package feeds

import (
	"fmt"
	"math"
	"sort"
	"strings"
	"sync"
	"time"

	"telefam.company/backend/internal/media"
	"telefam.company/backend/internal/posts"
	"telefam.company/backend/internal/security"
)

type AuthorSummary struct {
	UserID      string `json:"user_id"`
	Username    string `json:"username"`
	FullName    string `json:"full_name"`
	AvatarURL   string `json:"avatar_url,omitempty"`
	IsFollowing bool   `json:"is_following"`
	FollowsYou  bool   `json:"follows_you"`
}

type FeedVideoRendition struct {
	QualityLabel string `json:"quality_label"`
	Width        int    `json:"width"`
	Height       int    `json:"height"`
	BitrateKbps  int    `json:"bitrate_kbps"`
	URL          string `json:"url"`
	ByteSize     int    `json:"byte_size"`
}

type FeedItem struct {
	PostID              string               `json:"post_id"`
	AuthorUserID        string               `json:"author_user_id"`
	AuthorUsername      string               `json:"author_username"`
	AuthorFullName      string               `json:"author_full_name"`
	AuthorAvatarURL     string               `json:"author_avatar_url,omitempty"`
	IsFollowingAuthor   bool                 `json:"is_following_author"`
	IsOwner             bool                 `json:"is_owner"`
	Privacy             posts.PostPrivacy    `json:"privacy"`
	Mode                posts.PostMode       `json:"mode"`
	TextContent         string               `json:"text_content,omitempty"`
	Caption             string               `json:"caption,omitempty"`
	ThemeID             string               `json:"theme_id,omitempty"`
	FontID              string               `json:"font_id,omitempty"`
	Song                *posts.SongTrack     `json:"song,omitempty"`
	Hashtags            []string             `json:"hashtags"`
	PhotoURLs           []string             `json:"photo_urls,omitempty"`
	VideoURL            string               `json:"video_url,omitempty"`
	ThumbnailURL        string               `json:"thumbnail_url,omitempty"`
	Renditions          []FeedVideoRendition `json:"renditions,omitempty"`
	Width               int                  `json:"width"`
	Height              int                  `json:"height"`
	DurationMs          int64                `json:"duration_ms"`
	MuteOriginalAudio   bool                 `json:"mute_original_audio"`
	ContentID           string               `json:"content_id,omitempty"`
	ViewsCount          int64                `json:"views_count"`
	LikesCount          int64                `json:"likes_count"`
	CommentsCount       int64                `json:"comments_count"`
	SharesCount         int64                `json:"shares_count"`
	FavouritesCount     int64                `json:"favourites_count"`
	RepostsCount        int64                `json:"reposts_count"`
	IsLiked             bool                 `json:"is_liked"`
	IsFavourited        bool                 `json:"is_favourited"`
	IsReposted          bool                 `json:"is_reposted"`
	RepostedByUsername  string               `json:"reposted_by_username,omitempty"`
	RecommendationLabel string               `json:"recommendation_label,omitempty"`
	RelevanceScore      float64              `json:"relevance_score"`
	CreatedAtMs         int64                `json:"created_at_ms"`
}

type FeedPageResponse struct {
	Success       bool       `json:"success"`
	Tab           string     `json:"tab"`
	Items         []FeedItem `json:"items"`
	Page          int        `json:"page"`
	PageSize      int        `json:"page_size"`
	HasMore       bool       `json:"has_more"`
	ServerEpochMs int64      `json:"server_epoch_ms"`
}

type SearchShortcut struct {
	Key   string `json:"key"`
	Label string `json:"label"`
	Count int64  `json:"count"`
}

type FeedSearchResponse struct {
	Success       bool             `json:"success"`
	Query         string           `json:"query"`
	Shortcut      string           `json:"shortcut"`
	Shortcuts     []SearchShortcut `json:"shortcuts"`
	Items         []FeedItem       `json:"items"`
	Page          int              `json:"page"`
	PageSize      int              `json:"page_size"`
	HasMore       bool             `json:"has_more"`
	ServerEpochMs int64            `json:"server_epoch_ms"`
}

type InteractionRequest struct {
	ClientOpID string `json:"client_op_id,omitempty"`
	PostID     string `json:"post_id"`
	Action     string `json:"action"` // like, unlike, favourite, unfavourite, repost, unrepost, share, not_interested, report, block, watch, skip
	WatchMs    int64  `json:"watch_ms,omitempty"`
	DurationMs int64  `json:"duration_ms,omitempty"`
	Reason     string `json:"reason,omitempty"`
	Details    string `json:"details,omitempty"`
}

type InteractionResponse struct {
	Success         bool   `json:"success"`
	PostID          string `json:"post_id"`
	Action          string `json:"action"`
	ViewsCount      int64  `json:"views_count"`
	LikesCount      int64  `json:"likes_count"`
	FavouritesCount int64  `json:"favourites_count"`
	RepostsCount    int64  `json:"reposts_count"`
	SharesCount     int64  `json:"shares_count"`
	IsLiked         bool   `json:"is_liked"`
	IsFavourited    bool   `json:"is_favourited"`
	IsReposted      bool   `json:"is_reposted"`
	ServerEpochMs   int64  `json:"server_epoch_ms"`
}

type ReportRecord struct {
	ReporterUserID string
	PostID         string
	Reason         string
	Details        string
	CreatedAtMs    int64
}

type UserReportRecord struct {
	ReporterUserID string
	TargetUserID   string
	Reason         string
	Details        string
	CreatedAtMs    int64
}

// Engine dynamically ranks feeds and search results using real per-user affinity signals.
type Engine struct {
	mu                  sync.RWMutex
	viewsCount          map[string]int64              // postID -> view count
	likes               map[string]map[string]bool    // postID -> userID -> liked
	favourites          map[string]map[string]bool    // postID -> userID -> saved
	reposts             map[string]map[string]int64   // postID -> userID -> timestampMs
	sharesCount         map[string]int64              // postID -> count
	commentsCount       map[string]int64              // postID -> count
	notInterested       map[string]map[string]bool    // userID -> postID -> suppressed
	blockedUsers        map[string]map[string]bool    // blockerUserID -> blockedAuthorID -> true
	blockedAtMs         map[string]map[string]int64   // blockerUserID -> blockedAuthorID -> unix ms
	mutedUsers          map[string]map[string]int64   // muterUserID -> mutedAuthorID -> unix ms
	notInterestedTopics map[string]map[string]bool    // userID -> lowercase topic -> true
	personalizationOff  map[string]bool               // userID -> true when personalization disabled
	suggestedOff        map[string]bool               // userID -> true when suggested content disabled
	affinity            map[string]map[string]float64 // userID -> dimensionKey -> dynamic weight
	ackedOps            map[string]struct{}
	reports             []ReportRecord
	userReports         []UserReportRecord
}

func NewEngine() *Engine {
	return &Engine{
		viewsCount:          make(map[string]int64),
		likes:               make(map[string]map[string]bool),
		favourites:          make(map[string]map[string]bool),
		reposts:             make(map[string]map[string]int64),
		sharesCount:         make(map[string]int64),
		commentsCount:       make(map[string]int64),
		notInterested:       make(map[string]map[string]bool),
		blockedUsers:        make(map[string]map[string]bool),
		blockedAtMs:         make(map[string]map[string]int64),
		mutedUsers:          make(map[string]map[string]int64),
		notInterestedTopics: make(map[string]map[string]bool),
		personalizationOff:  make(map[string]bool),
		suggestedOff:        make(map[string]bool),
		affinity:            make(map[string]map[string]float64),
		ackedOps:            make(map[string]struct{}),
	}
}

func (e *Engine) RecordInteraction(
	viewerUserID string,
	post *posts.StoredPost,
	req InteractionRequest,
) InteractionResponse {
	nowMs := time.Now().UnixMilli()
	action := strings.ToLower(strings.TrimSpace(req.Action))
	postID := ""
	authorID := ""
	if post != nil {
		postID = post.PostID
		authorID = post.AuthorUserID
	} else {
		postID = security.SanitizeText(req.PostID, 80, false)
	}

	e.mu.Lock()
	defer e.mu.Unlock()

	if opID := security.SanitizeText(req.ClientOpID, 80, false); opID != "" {
		dedupeKey := viewerUserID + "|" + opID
		if _, seen := e.ackedOps[dedupeKey]; seen {
			return e.snapshotInteractionLocked(viewerUserID, postID, action, nowMs)
		}
		e.ackedOps[dedupeKey] = struct{}{}
	}

	switch action {
	case "like":
		m := e.ensurePostSet(e.likes, postID)
		if !m[viewerUserID] {
			m[viewerUserID] = true
			e.updateAffinityLocked(viewerUserID, post, 3.0)
		}
	case "unlike":
		if m := e.likes[postID]; m != nil && m[viewerUserID] {
			delete(m, viewerUserID)
			e.updateAffinityLocked(viewerUserID, post, -2.5)
		}
	case "favourite":
		m := e.ensurePostSet(e.favourites, postID)
		if !m[viewerUserID] {
			m[viewerUserID] = true
			e.updateAffinityLocked(viewerUserID, post, 4.0)
		}
	case "unfavourite":
		if m := e.favourites[postID]; m != nil && m[viewerUserID] {
			delete(m, viewerUserID)
			e.updateAffinityLocked(viewerUserID, post, -3.0)
		}
	case "repost":
		rm := e.reposts[postID]
		if rm == nil {
			rm = make(map[string]int64)
			e.reposts[postID] = rm
		}
		if _, exists := rm[viewerUserID]; !exists {
			rm[viewerUserID] = nowMs
			e.updateAffinityLocked(viewerUserID, post, 4.5)
		}
	case "unrepost":
		if rm := e.reposts[postID]; rm != nil {
			if _, exists := rm[viewerUserID]; exists {
				delete(rm, viewerUserID)
				e.updateAffinityLocked(viewerUserID, post, -3.5)
			}
		}
	case "view":
		e.viewsCount[postID] = e.viewsCount[postID] + 1
		e.updateAffinityLocked(viewerUserID, post, 0.5)
	case "share":
		e.sharesCount[postID] = e.sharesCount[postID] + 1
		e.updateAffinityLocked(viewerUserID, post, 3.5)
	case "not_interested":
		nm := e.ensurePostSet(e.notInterested, viewerUserID)
		nm[postID] = true
		e.updateAffinityLocked(viewerUserID, post, -5.0)
	case "skip":
		e.updateAffinityLocked(viewerUserID, post, -1.5)
	case "watch", "watch_progress":
		dur := req.DurationMs
		if dur <= 0 && post != nil {
			dur = post.DurationMs
		}
		if dur <= 0 {
			dur = 10_000
		}
		ratio := float64(req.WatchMs) / float64(dur)
		switch {
		case ratio >= 0.75:
			e.updateAffinityLocked(viewerUserID, post, 3.5)
		case ratio >= 0.35:
			e.updateAffinityLocked(viewerUserID, post, 1.5)
		case ratio > 0 && ratio < 0.15:
			e.updateAffinityLocked(viewerUserID, post, -1.2)
		}
	case "report":
		reason := security.SanitizeText(req.Reason, 80, false)
		if reason == "" {
			reason = "Other"
		}
		details := security.SanitizeText(req.Details, 300, true)
		e.reports = append(e.reports, ReportRecord{
			ReporterUserID: viewerUserID,
			PostID:         postID,
			Reason:         reason,
			Details:        details,
			CreatedAtMs:    nowMs,
		})
		nm := e.ensurePostSet(e.notInterested, viewerUserID)
		nm[postID] = true
		e.updateAffinityLocked(viewerUserID, post, -4.0)
	case "block":
		if authorID != "" && authorID != viewerUserID {
			bm := e.ensurePostSet(e.blockedUsers, viewerUserID)
			bm[authorID] = true
			e.updateAffinityLocked(viewerUserID, post, -10.0)
		}
	}

	return e.snapshotInteractionLocked(viewerUserID, postID, action, nowMs)
}

func (e *Engine) BlockUser(callerID, targetID string) {
	if callerID == "" || targetID == "" || callerID == targetID {
		return
	}
	e.mu.Lock()
	defer e.mu.Unlock()
	bm := e.ensurePostSet(e.blockedUsers, callerID)
	bm[targetID] = true
	bat := e.blockedAtMs[callerID]
	if bat == nil {
		bat = make(map[string]int64)
		e.blockedAtMs[callerID] = bat
	}
	bat[targetID] = time.Now().UnixMilli()
}

func (e *Engine) UnblockUser(callerID, targetID string) {
	e.mu.Lock()
	defer e.mu.Unlock()
	if bm := e.blockedUsers[callerID]; bm != nil {
		delete(bm, targetID)
	}
	if bat := e.blockedAtMs[callerID]; bat != nil {
		delete(bat, targetID)
	}
}

func (e *Engine) IsBlocked(callerID, targetID string) bool {
	e.mu.RLock()
	defer e.mu.RUnlock()
	bm := e.blockedUsers[callerID]
	return bm != nil && bm[targetID]
}

func (e *Engine) MuteUser(callerID, targetID string) {
	if callerID == "" || targetID == "" || callerID == targetID {
		return
	}
	e.mu.Lock()
	defer e.mu.Unlock()
	mm := e.mutedUsers[callerID]
	if mm == nil {
		mm = make(map[string]int64)
		e.mutedUsers[callerID] = mm
	}
	mm[targetID] = time.Now().UnixMilli()
}

func (e *Engine) UnmuteUser(callerID, targetID string) {
	e.mu.Lock()
	defer e.mu.Unlock()
	if mm := e.mutedUsers[callerID]; mm != nil {
		delete(mm, targetID)
	}
}

func (e *Engine) IsMuted(callerID, targetID string) bool {
	e.mu.RLock()
	defer e.mu.RUnlock()
	mm := e.mutedUsers[callerID]
	_, ok := mm[targetID]
	return ok
}

func (e *Engine) ListBlockedUsers(callerID string) map[string]int64 {
	e.mu.RLock()
	defer e.mu.RUnlock()
	out := make(map[string]int64)
	bm := e.blockedUsers[callerID]
	bat := e.blockedAtMs[callerID]
	for uid, active := range bm {
		if active {
			out[uid] = bat[uid]
		}
	}
	return out
}

func (e *Engine) ListMutedUsers(callerID string) map[string]int64 {
	e.mu.RLock()
	defer e.mu.RUnlock()
	out := make(map[string]int64)
	for uid, ts := range e.mutedUsers[callerID] {
		out[uid] = ts
	}
	return out
}

func (e *Engine) UpdateUserContentControls(
	userID string,
	suggestedEnabled bool,
	notInterestedTopics []string,
	personalizationEnabled bool,
	resetSuggestions bool,
) {
	if userID == "" {
		return
	}
	e.mu.Lock()
	defer e.mu.Unlock()
	e.suggestedOff[userID] = !suggestedEnabled
	e.personalizationOff[userID] = !personalizationEnabled

	topicSet := make(map[string]bool)
	for _, raw := range notInterestedTopics {
		clean := strings.TrimPrefix(strings.ToLower(strings.TrimSpace(raw)), "#")
		if clean != "" {
			topicSet[clean] = true
		}
	}
	e.notInterestedTopics[userID] = topicSet

	if resetSuggestions {
		delete(e.affinity, userID)
		delete(e.notInterested, userID)
	}
}

func postMatchesAnySuppressedTopic(p *posts.StoredPost, topics map[string]bool) bool {
	if p == nil || len(topics) == 0 {
		return false
	}
	lowerCap := strings.ToLower(p.Caption)
	lowerText := strings.ToLower(p.TextContent)
	for _, h := range p.Hashtags {
		if topics[strings.ToLower(h)] {
			return true
		}
	}
	for topic := range topics {
		if topic != "" && (strings.Contains(lowerCap, topic) || strings.Contains(lowerText, topic)) {
			return true
		}
	}
	return false
}

func (e *Engine) RecordUserReport(reporterID, targetID, reason, details string) {
	e.mu.Lock()
	defer e.mu.Unlock()
	cleanReason := security.SanitizeText(reason, 80, false)
	if cleanReason == "" {
		cleanReason = "Other"
	}
	cleanDetails := security.SanitizeText(details, 400, true)
	e.userReports = append(e.userReports, UserReportRecord{
		ReporterUserID: reporterID,
		TargetUserID:   targetID,
		Reason:         cleanReason,
		Details:        cleanDetails,
		CreatedAtMs:    time.Now().UnixMilli(),
	})
}

func CanViewPost(viewerUserID string, p *posts.StoredPost, author AuthorSummary) bool {
	if p == nil {
		return false
	}
	if p.AuthorUserID == viewerUserID {
		return true
	}
	switch posts.NormalizePrivacy(string(p.Privacy)) {
	case posts.PrivacyPrivate:
		return false
	case posts.PrivacyFriends:
		return author.IsFollowing && author.FollowsYou
	default:
		return true
	}
}

func (e *Engine) BuildAuthorFeedItems(
	viewerUserID string,
	authorPosts []*posts.StoredPost,
	author AuthorSummary,
) (publicAndFriends []FeedItem, lockedPrivate []FeedItem, totalLikes int64) {
	e.mu.RLock()
	defer e.mu.RUnlock()

	publicAndFriends = make([]FeedItem, 0, len(authorPosts))
	lockedPrivate = make([]FeedItem, 0)
	for _, p := range authorPosts {
		if p == nil {
			continue
		}
		if !CanViewPost(viewerUserID, p, author) {
			continue
		}
		item := e.buildFeedItemLocked(viewerUserID, p, author, "", float64(p.CreatedAtMs))
		totalLikes += item.LikesCount
		if p.AuthorUserID == viewerUserID && posts.NormalizePrivacy(string(p.Privacy)) == posts.PrivacyPrivate {
			lockedPrivate = append(lockedPrivate, item)
		} else {
			publicAndFriends = append(publicAndFriends, item)
		}
	}
	return publicAndFriends, lockedPrivate, totalLikes
}

func (e *Engine) BuildOwnerSavedAndRepostedItems(
	ownerUserID string,
	allPosts []*posts.StoredPost,
	authorResolver func(userID string) AuthorSummary,
) (saved []FeedItem, reposted []FeedItem) {
	e.mu.RLock()
	defer e.mu.RUnlock()

	saved = make([]FeedItem, 0)
	reposted = make([]FeedItem, 0)
	blocked := e.blockedUsers[ownerUserID]
	for _, p := range allPosts {
		if p == nil {
			continue
		}
		if blocked != nil && blocked[p.AuthorUserID] {
			continue
		}
		author := authorResolver(p.AuthorUserID)
		if !CanViewPost(ownerUserID, p, author) {
			continue
		}
		isFav := e.favourites[p.PostID] != nil && e.favourites[p.PostID][ownerUserID]
		_, isRep := e.reposts[p.PostID][ownerUserID]
		if !isFav && !isRep {
			continue
		}
		item := e.buildFeedItemLocked(ownerUserID, p, author, "", float64(p.CreatedAtMs))
		if isFav {
			saved = append(saved, item)
		}
		if isRep {
			reposted = append(reposted, item)
		}
	}
	sort.Slice(saved, func(i, j int) bool { return saved[i].CreatedAtMs > saved[j].CreatedAtMs })
	sort.Slice(reposted, func(i, j int) bool { return reposted[i].CreatedAtMs > reposted[j].CreatedAtMs })
	return saved, reposted
}

func (e *Engine) GetFeed(
	viewerUserID string,
	tab string,
	page int,
	pageSize int,
	allPosts []*posts.StoredPost,
	authorResolver func(userID string) AuthorSummary,
) FeedPageResponse {
	if page < 1 {
		page = 1
	}
	if pageSize <= 0 || pageSize > 40 {
		pageSize = 15
	}
	cleanTab := strings.ToLower(strings.TrimSpace(tab))
	if cleanTab != "following" {
		cleanTab = "for_you"
	}

	e.mu.RLock()
	defer e.mu.RUnlock()

	nowMs := time.Now().UnixMilli()
	userAffinity := e.affinity[viewerUserID]
	if e.personalizationOff[viewerUserID] || e.suggestedOff[viewerUserID] {
		userAffinity = nil
	}
	blocked := e.blockedUsers[viewerUserID]
	muted := e.mutedUsers[viewerUserID]
	suppressed := e.notInterested[viewerUserID]
	suppressedTopics := e.notInterestedTopics[viewerUserID]

	scored := make([]FeedItem, 0, len(allPosts))
	for _, p := range allPosts {
		if p == nil {
			continue
		}
		// Private posts only belong in the owner's Locked profile tab, never in For You or Following feeds
		if posts.NormalizePrivacy(string(p.Privacy)) == posts.PrivacyPrivate {
			continue
		}
		if suppressed != nil && suppressed[p.PostID] {
			continue
		}
		if blocked != nil && blocked[p.AuthorUserID] {
			continue
		}
		if muted != nil {
			if _, isMuted := muted[p.AuthorUserID]; isMuted {
				continue
			}
		}
		if postMatchesAnySuppressedTopic(p, suppressedTopics) {
			continue
		}

		author := authorResolver(p.AuthorUserID)
		if !CanViewPost(viewerUserID, p, author) {
			continue
		}
		repostUser := e.findVisibleRepostAuthorLocked(p.PostID, viewerUserID, blocked, authorResolver)
		isFollowingFeedEligible := author.IsFollowing || p.AuthorUserID == viewerUserID || repostUser != ""

		if cleanTab == "following" && !isFollowingFeedEligible {
			continue
		}

		score := e.computeDynamicScoreLocked(viewerUserID, p, author, userAffinity, repostUser != "", nowMs)
		item := e.buildFeedItemLocked(viewerUserID, p, author, repostUser, score)
		scored = append(scored, item)
	}

	sort.SliceStable(scored, func(i, j int) bool {
		if cleanTab == "following" {
			return scored[i].CreatedAtMs > scored[j].CreatedAtMs
		}
		if math.Abs(scored[i].RelevanceScore-scored[j].RelevanceScore) > 0.001 {
			return scored[i].RelevanceScore > scored[j].RelevanceScore
		}
		return scored[i].CreatedAtMs > scored[j].CreatedAtMs
	})

	start := (page - 1) * pageSize
	if start >= len(scored) {
		return FeedPageResponse{
			Success:       true,
			Tab:           cleanTab,
			Items:         []FeedItem{},
			Page:          page,
			PageSize:      pageSize,
			HasMore:       false,
			ServerEpochMs: nowMs,
		}
	}
	end := min(len(scored), start+pageSize)
	return FeedPageResponse{
		Success:       true,
		Tab:           cleanTab,
		Items:         scored[start:end],
		Page:          page,
		PageSize:      pageSize,
		HasMore:       end < len(scored),
		ServerEpochMs: nowMs,
	}
}

func (e *Engine) Search(
	viewerUserID string,
	rawQuery string,
	rawShortcut string,
	page int,
	pageSize int,
	allPosts []*posts.StoredPost,
	authorResolver func(userID string) AuthorSummary,
) FeedSearchResponse {
	if page < 1 {
		page = 1
	}
	if pageSize <= 0 || pageSize > 40 {
		pageSize = 24
	}
	query := strings.ToLower(security.SanitizeText(rawQuery, 80, false))
	shortcut := strings.ToLower(security.SanitizeText(rawShortcut, 50, false))
	shortcut = strings.TrimPrefix(shortcut, "#")

	e.mu.RLock()
	defer e.mu.RUnlock()

	nowMs := time.Now().UnixMilli()
	userAffinity := e.affinity[viewerUserID]
	if e.personalizationOff[viewerUserID] {
		userAffinity = nil
	}
	blocked := e.blockedUsers[viewerUserID]
	muted := e.mutedUsers[viewerUserID]

	tagCounts := make(map[string]int64)
	matched := make([]FeedItem, 0, len(allPosts))
	var visibleCorpusCount int64

	for _, p := range allPosts {
		if p == nil {
			continue
		}
		if blocked != nil && blocked[p.AuthorUserID] {
			continue
		}
		if muted != nil {
			if _, isMuted := muted[p.AuthorUserID]; isMuted {
				continue
			}
		}
		if posts.NormalizePrivacy(string(p.Privacy)) == posts.PrivacyPrivate {
			continue
		}
		author := authorResolver(p.AuthorUserID)
		if !CanViewPost(viewerUserID, p, author) {
			continue
		}
		visibleCorpusCount++
		for _, h := range p.Hashtags {
			tagCounts[h] = tagCounts[h] + 1
		}
		if shortcut != "" && shortcut != "all" {
			shortcutMatched := false
			for _, h := range p.Hashtags {
				if h == shortcut {
					shortcutMatched = true
					break
				}
			}
			if !shortcutMatched && p.Song != nil && strings.Contains(strings.ToLower(p.Song.Title), shortcut) {
				shortcutMatched = true
			}
			if !shortcutMatched {
				continue
			}
		}

		if query != "" && !matchesPostQuery(p, author, query) {
			continue
		}

		score := e.computeDynamicScoreLocked(viewerUserID, p, author, userAffinity, false, nowMs)
		matched = append(matched, e.buildFeedItemLocked(viewerUserID, p, author, "", score))
	}

	sort.SliceStable(matched, func(i, j int) bool {
		if math.Abs(matched[i].RelevanceScore-matched[j].RelevanceScore) > 0.001 {
			return matched[i].RelevanceScore > matched[j].RelevanceScore
		}
		return matched[i].CreatedAtMs > matched[j].CreatedAtMs
	})

	// Build dynamic shortcut pills strictly from actual corpus hashtags (zero hardcoded strings)
	type tagEntry struct {
		tag   string
		count int64
	}
	tagList := make([]tagEntry, 0, len(tagCounts))
	for k, v := range tagCounts {
		tagList = append(tagList, tagEntry{tag: k, count: v})
	}
	sort.Slice(tagList, func(i, j int) bool {
		if tagList[i].count != tagList[j].count {
			return tagList[i].count > tagList[j].count
		}
		return tagList[i].tag < tagList[j].tag
	})

	shortcuts := make([]SearchShortcut, 0, min(len(tagList), 10)+1)
	shortcuts = append(shortcuts, SearchShortcut{Key: "all", Label: "All", Count: visibleCorpusCount})
	for i := 0; i < len(tagList) && i < 10; i++ {
		shortcuts = append(shortcuts, SearchShortcut{
			Key:   tagList[i].tag,
			Label: "#" + tagList[i].tag,
			Count: tagList[i].count,
		})
	}

	start := (page - 1) * pageSize
	if start >= len(matched) {
		return FeedSearchResponse{
			Success:       true,
			Query:         query,
			Shortcut:      shortcut,
			Shortcuts:     shortcuts,
			Items:         []FeedItem{},
			Page:          page,
			PageSize:      pageSize,
			HasMore:       false,
			ServerEpochMs: nowMs,
		}
	}
	end := min(len(matched), start+pageSize)
	return FeedSearchResponse{
		Success:       true,
		Query:         query,
		Shortcut:      shortcut,
		Shortcuts:     shortcuts,
		Items:         matched[start:end],
		Page:          page,
		PageSize:      pageSize,
		HasMore:       end < len(matched),
		ServerEpochMs: nowMs,
	}
}

func (e *Engine) GetUserAffinitySnapshot(userID string) map[string]float64 {
	e.mu.RLock()
	defer e.mu.RUnlock()
	src := e.affinity[userID]
	out := make(map[string]float64, len(src))
	for k, v := range src {
		out[k] = v
	}
	return out
}

func (e *Engine) updateAffinityLocked(userID string, post *posts.StoredPost, delta float64) {
	if post == nil || userID == "" {
		return
	}
	am := e.affinity[userID]
	if am == nil {
		am = make(map[string]float64)
		e.affinity[userID] = am
	}

	// Apply light exponential decay across existing dimensions on positive engagement
	// so shifting user interests naturally overtake older habits without hardcoding.
	if delta > 0 {
		for k, v := range am {
			am[k] = v * 0.94
		}
	}

	for _, tag := range post.Hashtags {
		key := "hashtag:" + tag
		am[key] = clampScore(am[key] + delta)
	}
	if post.Song != nil && post.Song.TrackID != "" {
		key := "audio:" + strings.ToLower(post.Song.TrackID)
		am[key] = clampScore(am[key] + delta*0.8)
	}
	creatorKey := "creator:" + post.AuthorUserID
	am[creatorKey] = clampScore(am[creatorKey] + delta*0.9)
	modeKey := "mode:" + string(post.Mode)
	am[modeKey] = clampScore(am[modeKey] + delta*0.2)
}

func (e *Engine) computeDynamicScoreLocked(
	viewerUserID string,
	p *posts.StoredPost,
	author AuthorSummary,
	userAffinity map[string]float64,
	hasVisibleRepost bool,
	nowMs int64,
) float64 {
	score := 1.0

	if userAffinity != nil {
		for _, tag := range p.Hashtags {
			score += userAffinity["hashtag:"+tag]
		}
		if p.Song != nil && p.Song.TrackID != "" {
			score += userAffinity["audio:"+strings.ToLower(p.Song.TrackID)]
		}
		score += userAffinity["creator:"+p.AuthorUserID]
		score += userAffinity["mode:"+string(p.Mode)]
	}

	if author.IsFollowing {
		score += 4.0
	}
	if hasVisibleRepost {
		score += 2.5
	}

	likes := int64(len(e.likes[p.PostID]))
	favs := int64(len(e.favourites[p.PostID]))
	reposts := int64(len(e.reposts[p.PostID]))
	engagement := float64(likes + 2*reposts + 2*favs + e.sharesCount[p.PostID])
	score += math.Log1p(engagement) * 1.4

	ageHours := float64(max64(0, nowMs-p.CreatedAtMs)) / 3_600_000.0
	freshness := math.Exp(-ageHours / 48.0) * 3.0
	score += freshness

	if p.AuthorUserID == viewerUserID && hasVisibleRepost {
		score += 0.5
	}
	return score
}

func (e *Engine) buildFeedItemLocked(
	viewerUserID string,
	p *posts.StoredPost,
	author AuthorSummary,
	repostUser string,
	score float64,
) FeedItem {
	photoURLs := make([]string, 0, len(p.PhotoMediaIDs))
	for _, pid := range p.PhotoMediaIDs {
		photoURLs = append(photoURLs, "/v1/media/asset/"+pid)
	}
	videoURL := ""
	if p.VideoMediaID != "" {
		videoURL = "/v1/media/asset/" + p.VideoMediaID
	}
	thumbURL := ""
	if p.ThumbnailMediaID != "" {
		thumbURL = "/v1/media/asset/" + p.ThumbnailMediaID
	}

	renditions := make([]FeedVideoRendition, 0, len(p.Renditions))
	for _, r := range p.Renditions {
		qURL := ""
		if p.VideoMediaID != "" {
			qURL = fmt.Sprintf("/v1/media/asset/%s?quality=%s", p.VideoMediaID, r.QualityLabel)
		}
		renditions = append(renditions, FeedVideoRendition{
			QualityLabel: r.QualityLabel,
			Width:        r.Width,
			Height:       r.Height,
			BitrateKbps:  r.BitrateKbps,
			URL:          qURL,
			ByteSize:     r.ByteSize,
		})
	}

	isLiked := e.likes[p.PostID] != nil && e.likes[p.PostID][viewerUserID]
	isFav := e.favourites[p.PostID] != nil && e.favourites[p.PostID][viewerUserID]
	_, isReposted := e.reposts[p.PostID][viewerUserID]

	recLabel := "You might like this"
	if repostUser != "" {
		recLabel = "Reposted by @" + repostUser
	} else if isReposted {
		recLabel = "Reposted"
	}

	w := p.Width
	h := p.Height
	if w <= 0 {
		w = 1080
	}
	if h <= 0 {
		h = 1920
	}

	var rends []FeedVideoRendition
	if len(renditions) > 0 {
		rends = renditions
	} else if videoURL != "" {
		rends = []FeedVideoRendition{
			{QualityLabel: "720p", Width: w, Height: h, BitrateKbps: 2500, URL: videoURL, ByteSize: 0},
		}
	}

	_ = media.MaxPostVideoBytes
	return FeedItem{
		PostID:              p.PostID,
		AuthorUserID:        p.AuthorUserID,
		AuthorUsername:      author.Username,
		AuthorFullName:      author.FullName,
		AuthorAvatarURL:     author.AvatarURL,
		IsFollowingAuthor:   author.IsFollowing,
		IsOwner:             p.AuthorUserID == viewerUserID,
		Privacy:             posts.NormalizePrivacy(string(p.Privacy)),
		Mode:                p.Mode,
		TextContent:         p.TextContent,
		Caption:             p.Caption,
		ThemeID:             p.ThemeID,
		FontID:              p.FontID,
		Song:                p.Song,
		Hashtags:            p.Hashtags,
		PhotoURLs:           photoURLs,
		VideoURL:            videoURL,
		ThumbnailURL:        thumbURL,
		Renditions:          rends,
		Width:               w,
		Height:              h,
		DurationMs:          p.DurationMs,
		MuteOriginalAudio:   p.MuteOriginalAudio,
		ContentID:           p.ContentID,
		ViewsCount:          e.viewsCount[p.PostID],
		LikesCount:          int64(len(e.likes[p.PostID])),
		CommentsCount:       e.commentsCount[p.PostID],
		SharesCount:         e.sharesCount[p.PostID],
		FavouritesCount:     int64(len(e.favourites[p.PostID])),
		RepostsCount:        int64(len(e.reposts[p.PostID])),
		IsLiked:             isLiked,
		IsFavourited:        isFav,
		IsReposted:          isReposted,
		RepostedByUsername:  repostUser,
		RecommendationLabel: recLabel,
		RelevanceScore:      score,
		CreatedAtMs:         p.CreatedAtMs,
	}
}

func (e *Engine) findVisibleRepostAuthorLocked(
	postID, viewerUserID string,
	blocked map[string]bool,
	authorResolver func(userID string) AuthorSummary,
) string {
	rm := e.reposts[postID]
	if len(rm) == 0 {
		return ""
	}
	var latestTs int64
	var latestUsername string
	for reposterID, ts := range rm {
		if blocked != nil && blocked[reposterID] {
			continue
		}
		summary := authorResolver(reposterID)
		if reposterID == viewerUserID || summary.IsFollowing {
			if ts >= latestTs {
				latestTs = ts
				latestUsername = summary.Username
			}
		}
	}
	return latestUsername
}

func (e *Engine) snapshotInteractionLocked(viewerUserID, postID, action string, nowMs int64) InteractionResponse {
	isLiked := e.likes[postID] != nil && e.likes[postID][viewerUserID]
	isFav := e.favourites[postID] != nil && e.favourites[postID][viewerUserID]
	_, isReposted := e.reposts[postID][viewerUserID]
	return InteractionResponse{
		Success:         true,
		PostID:          postID,
		Action:          action,
		ViewsCount:      e.viewsCount[postID],
		LikesCount:      int64(len(e.likes[postID])),
		FavouritesCount: int64(len(e.favourites[postID])),
		RepostsCount:    int64(len(e.reposts[postID])),
		SharesCount:     e.sharesCount[postID],
		IsLiked:         isLiked,
		IsFavourited:    isFav,
		IsReposted:      isReposted,
		ServerEpochMs:   nowMs,
	}
}

func (e *Engine) ensurePostSet(root map[string]map[string]bool, key string) map[string]bool {
	m := root[key]
	if m == nil {
		m = make(map[string]bool)
		root[key] = m
	}
	return m
}

func matchesPostQuery(p *posts.StoredPost, author AuthorSummary, query string) bool {
	if strings.Contains(strings.ToLower(p.Caption), query) ||
		strings.Contains(strings.ToLower(p.TextContent), query) ||
		strings.Contains(strings.ToLower(author.Username), query) ||
		strings.Contains(strings.ToLower(author.FullName), query) {
		return true
	}
	for _, h := range p.Hashtags {
		if strings.Contains(strings.ToLower(h), query) {
			return true
		}
	}
	if p.Song != nil {
		if strings.Contains(strings.ToLower(p.Song.Title), query) ||
			strings.Contains(strings.ToLower(p.Song.Artist), query) {
			return true
		}
	}
	return false
}

func clampScore(v float64) float64 {
	if v > 40.0 {
		return 40.0
	}
	if v < -40.0 {
		return -40.0
	}
	return v
}

func max64(a, b int64) int64 {
	if a > b {
		return a
	}
	return b
}
