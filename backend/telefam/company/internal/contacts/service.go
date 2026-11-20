package contacts

import (
	"sort"
	"strings"
	"sync"
	"time"

	"telefam.company/backend/internal/security"
)

type FilterTab string

const (
	FilterAll         FilterTab = "all"
	FilterSuggestions FilterTab = "suggestions"
	FilterFollowers   FilterTab = "followers"
	FilterFollowing   FilterTab = "following"
	FilterFriends     FilterTab = "friends"
)

type RelationshipState string

const (
	RelationFollow     RelationshipState = "FOLLOW"
	RelationFollowing  RelationshipState = "FOLLOWING"
	RelationFollowBack RelationshipState = "FOLLOW_BACK"
	RelationFriends    RelationshipState = "FRIENDS"
)

type DirectoryUser struct {
	UserID        string
	FullName      string
	Username      string
	Country       string
	CountryISO    string
	AvatarMediaID string
	CreatedAt     time.Time
}

type ContactItem struct {
	UserID            string            `json:"user_id"`
	FullName          string            `json:"full_name"`
	Username          string            `json:"username"`
	Location          string            `json:"location"`
	CountryISO        string            `json:"country_iso"`
	AvatarURL         string            `json:"avatar_url,omitempty"`
	IsFollowing       bool              `json:"is_following"`
	FollowsYou        bool              `json:"follows_you"`
	RelationshipState RelationshipState `json:"relationship_state"`
	RiskScore         float64           `json:"risk_score"`
	UpdatedAtEpochMs  int64             `json:"updated_at_ms"`
}

type ContactsPageResponse struct {
	Success       bool          `json:"success"`
	Items         []ContactItem `json:"items"`
	Page          int           `json:"page"`
	PageSize      int           `json:"page_size"`
	TotalCount    int           `json:"total_count"`
	HasMore       bool          `json:"has_more"`
	ServerEpochMs int64         `json:"server_epoch_ms"`
}

type FollowMutationResponse struct {
	Success           bool              `json:"success"`
	TargetUserID      string            `json:"target_user_id"`
	IsFollowing       bool              `json:"is_following"`
	FollowsYou        bool              `json:"follows_you"`
	RelationshipState RelationshipState `json:"relationship_state"`
	CallerRiskScore   float64           `json:"caller_risk_score"`
	ServerEpochMs     int64             `json:"server_epoch_ms"`
}

type riskProfile struct {
	Score           float64
	DevicesSeen     map[string]struct{}
	BurstViolations int
	RecentActions   []time.Time
	LastEvaluated   time.Time
}

type Service struct {
	mu           sync.Mutex
	users        map[string]DirectoryUser
	following    map[string]map[string]struct{} // follower -> set(followee)
	followers    map[string]map[string]struct{} // followee -> set(follower)
	actionTimes  map[string][]time.Time         // rateKey -> timestamps in last 60s
	riskScores   map[string]*riskProfile
	maxPerMinute int
	nowFunc      func() time.Time
}

func NewService() *Service {
	return &Service{
		users:        make(map[string]DirectoryUser),
		following:    make(map[string]map[string]struct{}),
		followers:    make(map[string]map[string]struct{}),
		actionTimes:  make(map[string][]time.Time),
		riskScores:   make(map[string]*riskProfile),
		maxPerMinute: 20,
		nowFunc:      time.Now,
	}
}

// RegisterOrUpdateUser adds or updates a verified logged-in user in the directory.
func (s *Service) RegisterOrUpdateUser(u DirectoryUser, deviceFP string) {
	s.mu.Lock()
	defer s.mu.Unlock()

	if strings.TrimSpace(u.UserID) == "" || strings.TrimSpace(u.Username) == "" {
		return
	}
	if u.CreatedAt.IsZero() {
		u.CreatedAt = s.nowFunc().UTC()
	}
	s.users[u.UserID] = u
	s.touchRiskDeviceLocked(u.UserID, deviceFP)
}

func (s *Service) GetUserForViewer(callerID, targetID string) (DirectoryUser, bool, bool) {
	u, ok, isFollowing, _ := s.GetUserRelationshipForViewer(callerID, targetID)
	return u, ok, isFollowing
}

func (s *Service) GetUserRelationshipForViewer(callerID, targetID string) (DirectoryUser, bool, bool, bool) {
	s.mu.Lock()
	defer s.mu.Unlock()

	u, ok := s.users[targetID]
	if !ok {
		return DirectoryUser{}, false, false, false
	}
	isFollowing := false
	if set := s.following[callerID]; set != nil {
		_, isFollowing = set[targetID]
	}
	followsYou := false
	if set := s.followers[callerID]; set != nil {
		_, followsYou = set[targetID]
	}
	return u, true, isFollowing, followsYou
}

func (s *Service) GetUserCounts(targetID string) (followingCount int64, followersCount int64) {
	s.mu.Lock()
	defer s.mu.Unlock()
	if set := s.following[targetID]; set != nil {
		followingCount = int64(len(set))
	}
	if set := s.followers[targetID]; set != nil {
		followersCount = int64(len(set))
	}
	return followingCount, followersCount
}

func (s *Service) ListUserConnections(targetID string, isFollowersList bool, callerID string) []ContactItem {
	resp := s.ListUserConnectionsPage(targetID, isFollowersList, callerID, 1, 200, nil)
	return resp.Items
}

func (s *Service) ListUserConnectionsPage(
	targetID string,
	isFollowersList bool,
	callerID string,
	page int,
	pageSize int,
	isBlocked func(userID string) bool,
) ContactsPageResponse {
	s.mu.Lock()
	defer s.mu.Unlock()

	if page < 1 {
		page = 1
	}
	if pageSize <= 0 || pageSize > 200 {
		pageSize = 30
	}

	var idSet map[string]struct{}
	if isFollowersList {
		idSet = s.followers[targetID]
	} else {
		idSet = s.following[targetID]
	}

	out := make([]ContactItem, 0, len(idSet))
	callerFollowing := s.following[callerID]
	callerFollowers := s.followers[callerID]

	for uid := range idSet {
		if isBlocked != nil && uid != callerID && isBlocked(uid) {
			continue
		}
		u, ok := s.users[uid]
		if !ok {
			continue
		}
		isFollowing := false
		if callerFollowing != nil {
			_, isFollowing = callerFollowing[uid]
		}
		followsYou := false
		if callerFollowers != nil {
			_, followsYou = callerFollowers[uid]
		}
		rel := ComputeRelationship(isFollowing, followsYou)
		avatarURL := ""
		if u.AvatarMediaID != "" {
			avatarURL = "/v1/media/avatar/" + u.AvatarMediaID
		}
		out = append(out, ContactItem{
			UserID:            u.UserID,
			FullName:          u.FullName,
			Username:          u.Username,
			Location:          u.Country,
			CountryISO:        u.CountryISO,
			AvatarURL:         avatarURL,
			IsFollowing:       isFollowing,
			FollowsYou:        followsYou,
			RelationshipState: rel,
			RiskScore:         s.getRiskScoreLocked(u.UserID),
			UpdatedAtEpochMs:  u.CreatedAt.UnixMilli(),
		})
	}

	sort.Slice(out, func(i, j int) bool {
		return strings.ToLower(out[i].FullName) < strings.ToLower(out[j].FullName)
	})

	total := len(out)
	start := (page - 1) * pageSize
	if start >= total {
		return ContactsPageResponse{
			Success:       true,
			Items:         []ContactItem{},
			Page:          page,
			PageSize:      pageSize,
			TotalCount:    total,
			HasMore:       false,
			ServerEpochMs: s.nowFunc().UnixMilli(),
		}
	}
	end := start + pageSize
	if end > total {
		end = total
	}
	return ContactsPageResponse{
		Success:       true,
		Items:         out[start:end],
		Page:          page,
		PageSize:      pageSize,
		TotalCount:    total,
		HasMore:       end < total,
		ServerEpochMs: s.nowFunc().UnixMilli(),
	}
}

func (s *Service) AddRiskScore(userID string, delta float64) float64 {
	s.mu.Lock()
	defer s.mu.Unlock()
	rp := s.ensureRiskLocked(userID)
	rp.Score += delta
	if rp.Score < 0.0 {
		rp.Score = 0.0
	}
	if rp.Score > 100.0 {
		rp.Score = 100.0
	}
	rp.LastEvaluated = s.nowFunc()
	return rp.Score
}

func ComputeRelationship(isFollowing, followsYou bool) RelationshipState {
	switch {
	case isFollowing && followsYou:
		return RelationFriends
	case isFollowing:
		return RelationFollowing
	case followsYou:
		return RelationFollowBack
	default:
		return RelationFollow
	}
}

// CheckActionRateLimit enforces per-minute action limits across user, device, and IP.
func (s *Service) CheckActionRateLimit(userID, deviceFP, ipFP string) bool {
	s.mu.Lock()
	defer s.mu.Unlock()

	now := s.nowFunc()
	if !s.allowWindowLocked("u:"+userID, now) ||
		!s.allowWindowLocked("d:"+deviceFP, now) ||
		!s.allowWindowLocked("i:"+ipFP, now) {
		s.bumpRiskForBurstLocked(userID, deviceFP, now)
		return false
	}
	s.recordWindowLocked("u:"+userID, now)
	s.recordWindowLocked("d:"+deviceFP, now)
	s.recordWindowLocked("i:"+ipFP, now)
	return true
}

func (s *Service) SetFollowing(callerID, targetID, deviceFP string, wantFollow bool) (FollowMutationResponse, bool) {
	s.mu.Lock()
	defer s.mu.Unlock()

	if callerID == "" || targetID == "" || callerID == targetID {
		return FollowMutationResponse{}, false
	}
	if _, ok := s.users[targetID]; !ok {
		return FollowMutationResponse{}, false
	}

	if wantFollow {
		if s.following[callerID] == nil {
			s.following[callerID] = make(map[string]struct{})
		}
		if s.followers[targetID] == nil {
			s.followers[targetID] = make(map[string]struct{})
		}
		s.following[callerID][targetID] = struct{}{}
		s.followers[targetID][callerID] = struct{}{}
	} else {
		if set := s.following[callerID]; set != nil {
			delete(set, targetID)
		}
		if set := s.followers[targetID]; set != nil {
			delete(set, callerID)
		}
	}

	score := s.evaluateRiskLocked(callerID, deviceFP, s.nowFunc())
	followsYou := false
	if set := s.followers[callerID]; set != nil {
		_, followsYou = set[targetID]
	}

	return FollowMutationResponse{
		Success:           true,
		TargetUserID:      targetID,
		IsFollowing:       wantFollow,
		FollowsYou:        followsYou,
		RelationshipState: ComputeRelationship(wantFollow, followsYou),
		CallerRiskScore:   score,
		ServerEpochMs:     s.nowFunc().UnixMilli(),
	}, true
}

func (s *Service) ListContacts(callerID string, filter FilterTab, query string, page, pageSize int) ContactsPageResponse {
	s.mu.Lock()
	defer s.mu.Unlock()

	if page < 1 {
		page = 1
	}
	if pageSize <= 0 || pageSize > 50 {
		pageSize = 20
	}

	cleanQuery := strings.ToLower(security.SanitizeText(query, 64, false))
	callerFollowing := s.following[callerID]
	callerFollowers := s.followers[callerID]

	matched := make([]ContactItem, 0, len(s.users))
	for _, u := range s.users {
		if u.UserID == callerID {
			continue
		}
		isFollowing := false
		if callerFollowing != nil {
			_, isFollowing = callerFollowing[u.UserID]
		}
		followsYou := false
		if callerFollowers != nil {
			_, followsYou = callerFollowers[u.UserID]
		}
		rel := ComputeRelationship(isFollowing, followsYou)

		if !matchesFilter(filter, isFollowing, followsYou, rel) {
			continue
		}
		if cleanQuery != "" {
			nameMatch := strings.Contains(strings.ToLower(u.FullName), cleanQuery)
			userMatch := strings.Contains(strings.ToLower(u.Username), cleanQuery)
			locMatch := strings.Contains(strings.ToLower(u.Country), cleanQuery)
			if !nameMatch && !userMatch && !locMatch {
				continue
			}
		}

		risk := s.getRiskScoreLocked(u.UserID)
		avatarURL := ""
		if u.AvatarMediaID != "" {
			avatarURL = "/v1/media/avatar/" + u.AvatarMediaID
		}
		matched = append(matched, ContactItem{
			UserID:            u.UserID,
			FullName:          u.FullName,
			Username:          u.Username,
			Location:          u.Country,
			CountryISO:        u.CountryISO,
			AvatarURL:         avatarURL,
			IsFollowing:       isFollowing,
			FollowsYou:        followsYou,
			RelationshipState: rel,
			RiskScore:         risk,
			UpdatedAtEpochMs:  u.CreatedAt.UnixMilli(),
		})
	}

	sort.Slice(matched, func(i, j int) bool {
		if filter == FilterSuggestions {
			if matched[i].RiskScore != matched[j].RiskScore {
				return matched[i].RiskScore < matched[j].RiskScore
			}
		}
		li := strings.ToLower(matched[i].FullName)
		lj := strings.ToLower(matched[j].FullName)
		if li != lj {
			return li < lj
		}
		return strings.ToLower(matched[i].Username) < strings.ToLower(matched[j].Username)
	})

	total := len(matched)
	start := (page - 1) * pageSize
	if start >= total {
		return ContactsPageResponse{
			Success:       true,
			Items:         []ContactItem{},
			Page:          page,
			PageSize:      pageSize,
			TotalCount:    total,
			HasMore:       false,
			ServerEpochMs: s.nowFunc().UnixMilli(),
		}
	}
	end := start + pageSize
	if end > total {
		end = total
	}
	return ContactsPageResponse{
		Success:       true,
		Items:         matched[start:end],
		Page:          page,
		PageSize:      pageSize,
		TotalCount:    total,
		HasMore:       end < total,
		ServerEpochMs: s.nowFunc().UnixMilli(),
	}
}

func (s *Service) GetRiskScore(userID string) float64 {
	s.mu.Lock()
	defer s.mu.Unlock()
	return s.getRiskScoreLocked(userID)
}

func matchesFilter(filter FilterTab, isFollowing, followsYou bool, rel RelationshipState) bool {
	switch filter {
	case FilterAll, "":
		return true
	case FilterSuggestions:
		return !isFollowing
	case FilterFollowers:
		return followsYou
	case FilterFollowing:
		return isFollowing
	case FilterFriends:
		return rel == RelationFriends
	default:
		return true
	}
}

func (s *Service) allowWindowLocked(key string, now time.Time) bool {
	cutoff := now.Add(-1 * time.Minute)
	list := s.actionTimes[key]
	pruned := list[:0]
	for _, ts := range list {
		if ts.After(cutoff) {
			pruned = append(pruned, ts)
		}
	}
	s.actionTimes[key] = pruned
	return len(pruned) < s.maxPerMinute
}

func (s *Service) recordWindowLocked(key string, now time.Time) {
	s.actionTimes[key] = append(s.actionTimes[key], now)
}

func (s *Service) touchRiskDeviceLocked(userID, deviceFP string) {
	rp := s.ensureRiskLocked(userID)
	if deviceFP != "" {
		rp.DevicesSeen[deviceFP] = struct{}{}
	}
	rp.Score = s.calculateScoreLocked(userID, rp, s.nowFunc())
	rp.LastEvaluated = s.nowFunc()
}

func (s *Service) bumpRiskForBurstLocked(userID, deviceFP string, now time.Time) {
	rp := s.ensureRiskLocked(userID)
	rp.BurstViolations++
	if deviceFP != "" {
		rp.DevicesSeen[deviceFP] = struct{}{}
	}
	rp.Score = s.calculateScoreLocked(userID, rp, now)
	rp.LastEvaluated = now
}

func (s *Service) evaluateRiskLocked(userID, deviceFP string, now time.Time) float64 {
	rp := s.ensureRiskLocked(userID)
	if deviceFP != "" {
		rp.DevicesSeen[deviceFP] = struct{}{}
	}
	cutoff := now.Add(-5 * time.Minute)
	pruned := rp.RecentActions[:0]
	for _, ts := range rp.RecentActions {
		if ts.After(cutoff) {
			pruned = append(pruned, ts)
		}
	}
	pruned = append(pruned, now)
	rp.RecentActions = pruned
	rp.Score = s.calculateScoreLocked(userID, rp, now)
	rp.LastEvaluated = now
	return rp.Score
}

func (s *Service) calculateScoreLocked(userID string, rp *riskProfile, now time.Time) float64 {
	score := 5.0
	// Rapid action velocity in the past 5 minutes raises risk
	score += float64(len(rp.RecentActions)) * 1.2
	// Rate-limit violations indicate automation/spam risk
	score += float64(rp.BurstViolations) * 14.0
	// Excessive device churn beyond 2 devices adds moderate risk
	if len(rp.DevicesSeen) > 2 {
		score += float64(len(rp.DevicesSeen)-2) * 8.0
	}
	// Follower reciprocity lowers risk
	if fSet := s.followers[userID]; len(fSet) > 0 {
		score -= minFloat(float64(len(fSet))*1.5, 10.0)
	}
	if u, ok := s.users[userID]; ok && now.Sub(u.CreatedAt) > 24*time.Hour {
		score -= 3.0
	}
	if score < 0.0 {
		return 0.0
	}
	if score > 100.0 {
		return 100.0
	}
	return score
}

func (s *Service) getRiskScoreLocked(userID string) float64 {
	if rp, ok := s.riskScores[userID]; ok {
		return rp.Score
	}
	return 5.0
}

func (s *Service) ensureRiskLocked(userID string) *riskProfile {
	rp, ok := s.riskScores[userID]
	if !ok {
		rp = &riskProfile{
			Score:       5.0,
			DevicesSeen: make(map[string]struct{}),
		}
		s.riskScores[userID] = rp
	}
	return rp
}

func minFloat(a, b float64) float64 {
	if a < b {
		return a
	}
	return b
}
