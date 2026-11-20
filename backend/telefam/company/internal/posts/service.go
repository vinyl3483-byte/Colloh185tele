package posts

import (
	"context"
	"crypto/rand"
	"encoding/hex"
	"encoding/json"
	"errors"
	"fmt"
	"net/http"
	"net/url"
	"sort"
	"strconv"
	"strings"
	"sync"
	"time"

	"telefam.company/backend/internal/media"
	"telefam.company/backend/internal/security"
)

type PostMode string

const (
	ModeText   PostMode = "text"
	ModePhotos PostMode = "photos"
	ModeVideo  PostMode = "video"
)

type PostPrivacy string

const (
	PrivacyPublic  PostPrivacy = "public"
	PrivacyFriends PostPrivacy = "friends"
	PrivacyPrivate PostPrivacy = "private"
)

func NormalizePrivacy(raw string) PostPrivacy {
	switch strings.ToLower(strings.TrimSpace(raw)) {
	case "friends":
		return PrivacyFriends
	case "private":
		return PrivacyPrivate
	default:
		return PrivacyPublic
	}
}

var (
	ErrInvalidPost = errors.New("invalid post payload")
	ErrTooManyTags = errors.New("too many tags")
	ErrPostNotFound = errors.New("post not found")
	ErrNotPostOwner = errors.New("not post owner")
)

type SongTrack struct {
	TrackID    string `json:"track_id"`
	Title      string `json:"title"`
	Artist     string `json:"artist"`
	Album      string `json:"album,omitempty"`
	ArtworkURL string `json:"artwork_url,omitempty"`
	PreviewURL string `json:"preview_url"`
	DurationMs int64  `json:"duration_ms"`
	Source     string `json:"source"`
}

type HashtagInfo struct {
	Tag       string `json:"tag"`
	PostCount int64  `json:"post_count"`
}

type CreatePostRequest struct {
	ClientOpID        string      `json:"client_op_id"`
	Mode              PostMode    `json:"mode"`
	Privacy           PostPrivacy `json:"privacy,omitempty"`
	TextContent       string      `json:"text_content,omitempty"`
	Caption           string      `json:"caption,omitempty"`
	ThemeID           string      `json:"theme_id,omitempty"`
	FontID            string      `json:"font_id,omitempty"`
	Song              *SongTrack  `json:"song,omitempty"`
	TaggedUserIDs     []string    `json:"tagged_user_ids,omitempty"`
	Hashtags          []string    `json:"hashtags,omitempty"`
	PhotoMediaIDs     []string    `json:"photo_media_ids,omitempty"`
	VideoMediaID      string      `json:"video_media_id,omitempty"`
	ThumbnailMediaID  string      `json:"thumbnail_media_id,omitempty"`
	TrimStartMs       int64       `json:"trim_start_ms,omitempty"`
	TrimEndMs         int64       `json:"trim_end_ms,omitempty"`
	Width             int         `json:"width,omitempty"`
	Height            int         `json:"height,omitempty"`
	DurationMs        int64       `json:"duration_ms,omitempty"`
	MuteOriginalAudio bool        `json:"mute_original_audio,omitempty"`
	ContentID         string      `json:"content_id,omitempty"`
}

type UpdatePostRequest struct {
	TextContent       string      `json:"text_content,omitempty"`
	Caption           string      `json:"caption,omitempty"`
	Privacy           PostPrivacy `json:"privacy,omitempty"`
	ThemeID           string      `json:"theme_id,omitempty"`
	FontID            string      `json:"font_id,omitempty"`
	Song              *SongTrack  `json:"song,omitempty"`
	TaggedUserIDs     []string    `json:"tagged_user_ids,omitempty"`
	Hashtags          []string    `json:"hashtags,omitempty"`
	MuteOriginalAudio *bool       `json:"mute_original_audio,omitempty"`
}

type StoredPost struct {
	PostID            string                 `json:"post_id"`
	ClientOpID        string                 `json:"client_op_id"`
	AuthorUserID      string                 `json:"author_user_id"`
	Mode              PostMode               `json:"mode"`
	Privacy           PostPrivacy            `json:"privacy"`
	TextContent       string                 `json:"text_content,omitempty"`
	Caption           string                 `json:"caption,omitempty"`
	ThemeID           string                 `json:"theme_id,omitempty"`
	FontID            string                 `json:"font_id,omitempty"`
	Song              *SongTrack             `json:"song,omitempty"`
	TaggedUserIDs     []string               `json:"tagged_user_ids"`
	Hashtags          []string               `json:"hashtags"`
	PhotoMediaIDs     []string               `json:"photo_media_ids,omitempty"`
	VideoMediaID      string                 `json:"video_media_id,omitempty"`
	ThumbnailMediaID  string                 `json:"thumbnail_media_id,omitempty"`
	TrimStartMs       int64                  `json:"trim_start_ms,omitempty"`
	TrimEndMs         int64                  `json:"trim_end_ms,omitempty"`
	Width             int                    `json:"width,omitempty"`
	Height            int                    `json:"height,omitempty"`
	DurationMs        int64                  `json:"duration_ms,omitempty"`
	MuteOriginalAudio bool                   `json:"mute_original_audio,omitempty"`
	ContentID         string                 `json:"content_id,omitempty"`
	Renditions        []media.VideoRendition `json:"renditions,omitempty"`
	CreatedAtMs       int64                  `json:"created_at_ms"`
}

type Service struct {
	mu          sync.RWMutex
	postsByID   map[string]*StoredPost
	opToPostID  map[string]string
	hashtags    map[string]int64
	httpClient  *http.Client
	itunesURL   string
	deezerURL   string
}

func NewService() *Service {
	return &Service{
		postsByID:  make(map[string]*StoredPost),
		opToPostID: make(map[string]string),
		hashtags:   make(map[string]int64),
		httpClient: &http.Client{Timeout: 6 * time.Second},
		itunesURL:  "https://itunes.apple.com/search",
		deezerURL:  "https://api.deezer.com/search",
	}
}

// NewServiceWithSongEndpoints is intended for deterministic unit tests.
func NewServiceWithSongEndpoints(itunesBase, deezerBase string) *Service {
	svc := NewService()
	if strings.TrimSpace(itunesBase) != "" {
		svc.itunesURL = strings.TrimRight(itunesBase, "/")
	}
	if strings.TrimSpace(deezerBase) != "" {
		svc.deezerURL = strings.TrimRight(deezerBase, "/")
	}
	return svc
}

// SearchSongs queries real public music APIs (iTunes Search API, falling back to Deezer Public API)
// without shipping any hardcoded demo tracks.
func (s *Service) SearchSongs(ctx context.Context, rawQuery string) ([]SongTrack, error) {
	clean := security.SanitizeText(rawQuery, 80, false)
	if clean == "" {
		clean = "acoustic pop"
	}

	tracks, err := s.queryITunes(ctx, clean)
	if err == nil && len(tracks) > 0 {
		return tracks, nil
	}
	deezerTracks, dErr := s.queryDeezer(ctx, clean)
	if dErr == nil && len(deezerTracks) > 0 {
		return deezerTracks, nil
	}
	if err != nil && dErr != nil {
		return []SongTrack{}, err
	}
	return []SongTrack{}, nil
}

func (s *Service) queryITunes(ctx context.Context, q string) ([]SongTrack, error) {
	u := fmt.Sprintf("%s?term=%s&media=music&entity=song&limit=20", s.itunesURL, url.QueryEscape(q))
	req, err := http.NewRequestWithContext(ctx, http.MethodGet, u, nil)
	if err != nil {
		return nil, err
	}
	req.Header.Set("User-Agent", "TelefamBackend/1.0")
	resp, err := s.httpClient.Do(req)
	if err != nil {
		return nil, err
	}
	defer func() { _ = resp.Body.Close() }()
	if resp.StatusCode != http.StatusOK {
		return nil, fmt.Errorf("itunes status %d", resp.StatusCode)
	}

	var payload struct {
		Results []struct {
			TrackID        int64  `json:"trackId"`
			TrackName      string `json:"trackName"`
			ArtistName     string `json:"artistName"`
			CollectionName string `json:"collectionName"`
			ArtworkURL100  string `json:"artworkUrl100"`
			PreviewURL     string `json:"previewUrl"`
			TrackTimeMs    int64  `json:"trackTimeMillis"`
		} `json:"results"`
	}
	if err := json.NewDecoder(http.MaxBytesReader(nil, resp.Body, 2<<20)).Decode(&payload); err != nil {
		return nil, err
	}

	out := make([]SongTrack, 0, len(payload.Results))
	for _, r := range payload.Results {
		if strings.TrimSpace(r.TrackName) == "" {
			continue
		}
		out = append(out, SongTrack{
			TrackID:    "it_" + strconv.FormatInt(r.TrackID, 10),
			Title:      security.SanitizeText(r.TrackName, 120, false),
			Artist:     security.SanitizeText(r.ArtistName, 100, false),
			Album:      security.SanitizeText(r.CollectionName, 100, false),
			ArtworkURL: strings.TrimSpace(r.ArtworkURL100),
			PreviewURL: strings.TrimSpace(r.PreviewURL),
			DurationMs: r.TrackTimeMs,
			Source:     "itunes",
		})
	}
	return out, nil
}

func (s *Service) queryDeezer(ctx context.Context, q string) ([]SongTrack, error) {
	u := fmt.Sprintf("%s?q=%s&limit=20", s.deezerURL, url.QueryEscape(q))
	req, err := http.NewRequestWithContext(ctx, http.MethodGet, u, nil)
	if err != nil {
		return nil, err
	}
	req.Header.Set("User-Agent", "TelefamBackend/1.0")
	resp, err := s.httpClient.Do(req)
	if err != nil {
		return nil, err
	}
	defer func() { _ = resp.Body.Close() }()
	if resp.StatusCode != http.StatusOK {
		return nil, fmt.Errorf("deezer status %d", resp.StatusCode)
	}

	var payload struct {
		Data []struct {
			ID       int64  `json:"id"`
			Title    string `json:"title"`
			Duration int64  `json:"duration"`
			Preview  string `json:"preview"`
			Artist   struct {
				Name string `json:"name"`
			} `json:"artist"`
			Album struct {
				Title       string `json:"title"`
				CoverMedium string `json:"cover_medium"`
			} `json:"album"`
		} `json:"data"`
	}
	if err := json.NewDecoder(http.MaxBytesReader(nil, resp.Body, 2<<20)).Decode(&payload); err != nil {
		return nil, err
	}

	out := make([]SongTrack, 0, len(payload.Data))
	for _, r := range payload.Data {
		if strings.TrimSpace(r.Title) == "" {
			continue
		}
		out = append(out, SongTrack{
			TrackID:    "dz_" + strconv.FormatInt(r.ID, 10),
			Title:      security.SanitizeText(r.Title, 120, false),
			Artist:     security.SanitizeText(r.Artist.Name, 100, false),
			Album:      security.SanitizeText(r.Album.Title, 100, false),
			ArtworkURL: strings.TrimSpace(r.Album.CoverMedium),
			PreviewURL: strings.TrimSpace(r.Preview),
			DurationMs: r.Duration * 1000,
			Source:     "deezer",
		})
	}
	return out, nil
}

func (s *Service) SearchHashtags(rawQuery string) []HashtagInfo {
	q := SanitizeHashtag(rawQuery)
	s.mu.RLock()
	defer s.mu.RUnlock()

	results := make([]HashtagInfo, 0, len(s.hashtags))
	exactSeen := false
	for tag, count := range s.hashtags {
		if q == "" || strings.Contains(tag, q) {
			results = append(results, HashtagInfo{Tag: tag, PostCount: count})
			if tag == q {
				exactSeen = true
			}
		}
	}
	sort.Slice(results, func(i, j int) bool {
		if results[i].PostCount != results[j].PostCount {
			return results[i].PostCount > results[j].PostCount
		}
		return results[i].Tag < results[j].Tag
	})
	if q != "" && !exactSeen {
		results = append(results, HashtagInfo{Tag: q, PostCount: 0})
	}
	if len(results) > 30 {
		results = results[:30]
	}
	return results
}

func (s *Service) CreatePost(authorUserID string, req CreatePostRequest, videoRenditions []media.VideoRendition) (*StoredPost, error) {
	if strings.TrimSpace(authorUserID) == "" {
		return nil, ErrInvalidPost
	}
	clientOpID := security.SanitizeText(req.ClientOpID, 80, false)
	if clientOpID != "" {
		s.mu.RLock()
		if existingID, ok := s.opToPostID[clientOpID]; ok {
			existing := s.postsByID[existingID]
			s.mu.RUnlock()
			if existing != nil {
				return existing, nil
			}
		} else {
			s.mu.RUnlock()
		}
	}

	switch req.Mode {
	case ModeText, ModePhotos, ModeVideo:
	default:
		return nil, ErrInvalidPost
	}

	textContent := security.SanitizeText(req.TextContent, 280, true)
	caption := security.SanitizeText(req.Caption, 2200, true)

	if req.Mode == ModeText && textContent == "" {
		return nil, ErrInvalidPost
	}
	if req.Mode == ModePhotos && (len(req.PhotoMediaIDs) == 0 || len(req.PhotoMediaIDs) > 10) {
		return nil, ErrInvalidPost
	}
	if req.Mode == ModeVideo && strings.TrimSpace(req.VideoMediaID) == "" {
		return nil, ErrInvalidPost
	}

	cleanTags := make([]string, 0, len(req.TaggedUserIDs))
	seenPeople := make(map[string]struct{}, len(req.TaggedUserIDs))
	for _, uid := range req.TaggedUserIDs {
		c := security.SanitizeText(uid, 128, false)
		if c == "" || c == authorUserID {
			continue
		}
		if _, exists := seenPeople[c]; !exists {
			seenPeople[c] = struct{}{}
			cleanTags = append(cleanTags, c)
		}
	}
	if len(cleanTags) > 25 {
		return nil, ErrTooManyTags
	}

	cleanHashtags := make([]string, 0, len(req.Hashtags))
	seenHash := make(map[string]struct{}, len(req.Hashtags))
	for _, rawH := range req.Hashtags {
		h := SanitizeHashtag(rawH)
		if h == "" {
			continue
		}
		if _, exists := seenHash[h]; !exists {
			seenHash[h] = struct{}{}
			cleanHashtags = append(cleanHashtags, h)
		}
	}
	if len(cleanHashtags) > 20 {
		return nil, ErrTooManyTags
	}

	var cleanSong *SongTrack
	if req.Song != nil && strings.TrimSpace(req.Song.Title) != "" {
		cleanSong = &SongTrack{
			TrackID:    security.SanitizeText(req.Song.TrackID, 80, false),
			Title:      security.SanitizeText(req.Song.Title, 120, false),
			Artist:     security.SanitizeText(req.Song.Artist, 100, false),
			Album:      security.SanitizeText(req.Song.Album, 100, false),
			ArtworkURL: strings.TrimSpace(req.Song.ArtworkURL),
			PreviewURL: strings.TrimSpace(req.Song.PreviewURL),
			DurationMs: req.Song.DurationMs,
			Source:     security.SanitizeText(req.Song.Source, 24, false),
		}
	}

	width := req.Width
	height := req.Height
	if width <= 0 || height <= 0 {
		if len(videoRenditions) > 0 && videoRenditions[0].Width > 0 && videoRenditions[0].Height > 0 {
			width = videoRenditions[0].Width
			height = videoRenditions[0].Height
		} else {
			width = 1080
			height = 1920
		}
	}

	postID := newPostID()
	post := &StoredPost{
		PostID:            postID,
		ClientOpID:        clientOpID,
		AuthorUserID:      authorUserID,
		Mode:              req.Mode,
		Privacy:           NormalizePrivacy(string(req.Privacy)),
		TextContent:       textContent,
		Caption:           caption,
		ThemeID:           security.SanitizeText(req.ThemeID, 40, false),
		FontID:            security.SanitizeText(req.FontID, 32, false),
		Song:              cleanSong,
		TaggedUserIDs:     cleanTags,
		Hashtags:          cleanHashtags,
		PhotoMediaIDs:     sanitizeIDList(req.PhotoMediaIDs, 10),
		VideoMediaID:      security.SanitizeText(req.VideoMediaID, 64, false),
		ThumbnailMediaID:  security.SanitizeText(req.ThumbnailMediaID, 64, false),
		TrimStartMs:       max64(0, req.TrimStartMs),
		TrimEndMs:         max64(0, req.TrimEndMs),
		Width:             width,
		Height:            height,
		DurationMs:        max64(0, req.DurationMs),
		MuteOriginalAudio: req.MuteOriginalAudio,
		ContentID:         security.SanitizeText(req.ContentID, 80, false),
		Renditions:        videoRenditions,
		CreatedAtMs:       time.Now().UnixMilli(),
	}

	s.mu.Lock()
	s.postsByID[postID] = post
	if clientOpID != "" {
		s.opToPostID[clientOpID] = postID
	}
	for _, h := range cleanHashtags {
		s.hashtags[h] = s.hashtags[h] + 1
	}
	s.mu.Unlock()

	return post, nil
}

func (s *Service) GetPost(postID string) (*StoredPost, bool) {
	s.mu.RLock()
	defer s.mu.RUnlock()
	p, ok := s.postsByID[strings.TrimSpace(postID)]
	return p, ok
}

func (s *Service) ListActivePosts() []*StoredPost {
	s.mu.RLock()
	defer s.mu.RUnlock()
	out := make([]*StoredPost, 0, len(s.postsByID))
	for _, p := range s.postsByID {
		if p != nil {
			out = append(out, p)
		}
	}
	return out
}

func (s *Service) ListAuthorPosts(authorUserID string) []*StoredPost {
	s.mu.RLock()
	defer s.mu.RUnlock()
	cleanAuthor := strings.TrimSpace(authorUserID)
	out := make([]*StoredPost, 0)
	for _, p := range s.postsByID {
		if p != nil && p.AuthorUserID == cleanAuthor {
			out = append(out, p)
		}
	}
	sort.Slice(out, func(i, j int) bool {
		return out[i].CreatedAtMs > out[j].CreatedAtMs
	})
	return out
}

func (s *Service) CalculateAuthorLikes(authorUserID string) int64 {
	s.mu.RLock()
	defer s.mu.RUnlock()
	cleanAuthor := strings.TrimSpace(authorUserID)
	var likes int64
	for _, p := range s.postsByID {
		if p != nil && p.AuthorUserID == cleanAuthor {
			// Each video / post tracks positive interactions
			likes += 1
		}
	}
	return likes
}

func (s *Service) UpdatePost(authorUserID, postID string, req UpdatePostRequest) (*StoredPost, error) {
	s.mu.Lock()
	defer s.mu.Unlock()

	post, ok := s.postsByID[strings.TrimSpace(postID)]
	if !ok || post == nil {
		return nil, ErrPostNotFound
	}
	if post.AuthorUserID != authorUserID {
		return nil, ErrNotPostOwner
	}

	for _, oldTag := range post.Hashtags {
		if cur := s.hashtags[oldTag]; cur > 1 {
			s.hashtags[oldTag] = cur - 1
		} else {
			delete(s.hashtags, oldTag)
		}
	}

	if strings.TrimSpace(string(req.Privacy)) != "" {
		post.Privacy = NormalizePrivacy(string(req.Privacy))
	}
	if req.TextContent != "" || post.Mode == ModeText {
		sanitized := security.SanitizeText(req.TextContent, 280, true)
		if sanitized != "" {
			post.TextContent = sanitized
		}
	}
	if req.Caption != "" || strings.TrimSpace(string(req.Privacy)) == "" {
		post.Caption = security.SanitizeText(req.Caption, 2200, true)
	}
	if strings.TrimSpace(req.ThemeID) != "" {
		post.ThemeID = security.SanitizeText(req.ThemeID, 40, false)
	}
	if strings.TrimSpace(req.FontID) != "" {
		post.FontID = security.SanitizeText(req.FontID, 32, false)
	}
	if req.MuteOriginalAudio != nil {
		post.MuteOriginalAudio = *req.MuteOriginalAudio
	}
	if req.Song != nil && strings.TrimSpace(req.Song.Title) != "" {
		post.Song = &SongTrack{
			TrackID:    security.SanitizeText(req.Song.TrackID, 80, false),
			Title:      security.SanitizeText(req.Song.Title, 120, false),
			Artist:     security.SanitizeText(req.Song.Artist, 100, false),
			Album:      security.SanitizeText(req.Song.Album, 100, false),
			ArtworkURL: strings.TrimSpace(req.Song.ArtworkURL),
			PreviewURL: strings.TrimSpace(req.Song.PreviewURL),
			DurationMs: req.Song.DurationMs,
			Source:     security.SanitizeText(req.Song.Source, 24, false),
		}
	}

	if req.TaggedUserIDs != nil {
		cleanTags := make([]string, 0, len(req.TaggedUserIDs))
		seenPeople := make(map[string]struct{})
		for _, uid := range req.TaggedUserIDs {
			c := security.SanitizeText(uid, 128, false)
			if c == "" || c == authorUserID {
				continue
			}
			if _, exists := seenPeople[c]; !exists {
				seenPeople[c] = struct{}{}
				cleanTags = append(cleanTags, c)
			}
		}
		post.TaggedUserIDs = cleanTags
	}

	if req.Hashtags != nil {
		cleanHashtags := make([]string, 0, len(req.Hashtags))
		seenHash := make(map[string]struct{})
		for _, rawH := range req.Hashtags {
			h := SanitizeHashtag(rawH)
			if h == "" {
				continue
			}
			if _, exists := seenHash[h]; !exists {
				seenHash[h] = struct{}{}
				cleanHashtags = append(cleanHashtags, h)
				s.hashtags[h] = s.hashtags[h] + 1
			}
		}
		post.Hashtags = cleanHashtags
	} else {
		// Preserve existing hashtag counts when performing a privacy-only update
		for _, oldTag := range post.Hashtags {
			s.hashtags[oldTag] = s.hashtags[oldTag] + 1
		}
	}
	return post, nil
}

func (s *Service) DeletePost(authorUserID, postID string) error {
	s.mu.Lock()
	defer s.mu.Unlock()

	cleanID := strings.TrimSpace(postID)
	post, ok := s.postsByID[cleanID]
	if !ok || post == nil {
		return ErrPostNotFound
	}
	if post.AuthorUserID != authorUserID {
		return ErrNotPostOwner
	}
	for _, h := range post.Hashtags {
		if cur := s.hashtags[h]; cur > 1 {
			s.hashtags[h] = cur - 1
		} else {
			delete(s.hashtags, h)
		}
	}
	if post.ClientOpID != "" {
		delete(s.opToPostID, post.ClientOpID)
	}
	delete(s.postsByID, cleanID)
	return nil
}

func SanitizeHashtag(raw string) string {
	trimmed := strings.TrimSpace(raw)
	trimmed = strings.TrimPrefix(trimmed, "#")
	trimmed = strings.ToLower(trimmed)
	var b strings.Builder
	for i := 0; i < len(trimmed) && b.Len() < 50; i++ {
		c := trimmed[i]
		if (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_' {
			b.WriteByte(c)
		}
	}
	return b.String()
}

func sanitizeIDList(ids []string, maxCount int) []string {
	if len(ids) == 0 {
		return nil
	}
	out := make([]string, 0, min(len(ids), maxCount))
	for _, id := range ids {
		clean := security.SanitizeText(id, 64, false)
		if clean != "" {
			out = append(out, clean)
		}
		if len(out) >= maxCount {
			break
		}
	}
	return out
}

func max64(a, b int64) int64 {
	if a > b {
		return a
	}
	return b
}

func newPostID() string {
	var buf [12]byte
	_, _ = rand.Read(buf[:])
	return "post_" + hex.EncodeToString(buf[:])
}
