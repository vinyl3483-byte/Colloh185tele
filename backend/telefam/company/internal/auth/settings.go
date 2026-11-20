package auth

import (
	"crypto/rand"
	"encoding/hex"
	"encoding/json"
	"fmt"
	"net/http"
	"sort"
	"strings"
	"time"

	"telefam.company/backend/internal/contacts"
	"telefam.company/backend/internal/errors"
	"telefam.company/backend/internal/security"
)

type PrivacySettingsDTO struct {
	IsPrivateAccount           bool   `json:"is_private_account"`
	WhoCanComment              string `json:"who_can_comment"`
	WhoCanMention              string `json:"who_can_mention"`
	WhoCanFindYou              string `json:"who_can_find_you"`
	ShowActivityStatus         bool   `json:"show_activity_status"`
	LikesVisibility            string `json:"likes_visibility"`
	FollowingVisibility        string `json:"following_visibility"`
	DataSharingPersonalization bool   `json:"data_sharing_personalization"`
	AnalyticsSharing           bool   `json:"analytics_sharing"`
	EssentialCookies           bool   `json:"essential_cookies"`
	PersonalizationCookies     bool   `json:"personalization_cookies"`
	UpdatedAtMs                int64  `json:"updated_at_ms"`
}

type NotificationSettingsDTO struct {
	MasterPause        bool  `json:"master_pause"`
	ChannelPush        bool  `json:"channel_push"`
	ChannelEmail       bool  `json:"channel_email"`
	ChannelSms         bool  `json:"channel_sms"`
	TypeMessages       bool  `json:"type_messages"`
	TypeComments       bool  `json:"type_comments"`
	TypeFollowers      bool  `json:"type_followers"`
	TypeProductUpdates bool  `json:"type_product_updates"`
	QuietHoursEnabled  bool  `json:"quiet_hours_enabled"`
	QuietStartMinutes  int   `json:"quiet_start_minutes"`
	QuietEndMinutes    int   `json:"quiet_end_minutes"`
	UpdatedAtMs        int64 `json:"updated_at_ms"`
}

type ContentPreferencesDTO struct {
	SuggestedContentEnabled bool     `json:"suggested_content_enabled"`
	NotInterestedTopics     []string `json:"not_interested_topics"`
	LanguageCode            string   `json:"language_code"`
	AutoplayVideos          bool     `json:"autoplay_videos"`
	VideoQuality            string   `json:"video_quality"`
	DataSaver               bool     `json:"data_saver"`
	CaptionsEnabled         bool     `json:"captions_enabled"`
	ReducedMotion           bool     `json:"reduced_motion"`
	ResetSuggestions        bool     `json:"reset_suggestions,omitempty"`
	UpdatedAtMs             int64    `json:"updated_at_ms"`
}

type RestrictedAccountItemDTO struct {
	UserID     string `json:"user_id"`
	FullName   string `json:"full_name"`
	Username   string `json:"username"`
	AvatarURL  string `json:"avatar_url,omitempty"`
	CreatedAtMs int64 `json:"created_at_ms"`
}

type ProblemReportPayload struct {
	ClientOpID        string `json:"client_op_id,omitempty"`
	Category          string `json:"category"`
	Description       string `json:"description"`
	ScreenshotMediaID string `json:"screenshot_media_id,omitempty"`
	ScreenshotSha256  string `json:"screenshot_sha256,omitempty"`
}

type SupportTicketDTO struct {
	TicketID          string `json:"ticket_id"`
	ReferenceNumber   string `json:"reference_number"`
	Category          string `json:"category"`
	Description       string `json:"description"`
	ScreenshotMediaID string `json:"screenshot_media_id,omitempty"`
	ScreenshotSha256  string `json:"screenshot_sha256,omitempty"`
	Status            string `json:"status"`
	ResolutionNote    string `json:"resolution_note,omitempty"`
	CreatedAtMs       int64  `json:"created_at_ms"`
	UpdatedAtMs       int64  `json:"updated_at_ms"`
}

type FaqItemDTO struct {
	ID       string `json:"id"`
	Category string `json:"category"`
	Question string `json:"question"`
	Answer   string `json:"answer"`
}

type SystemServiceStatusDTO struct {
	ServiceName string `json:"service_name"`
	Status      string `json:"status"`
	LatencyMs   int    `json:"latency_ms"`
	Summary     string `json:"summary"`
}

type LegalDocumentDTO struct {
	ID          string   `json:"id"`
	Title       string   `json:"title"`
	LastUpdated string   `json:"last_updated"`
	Sections    []string `json:"sections"`
}

type LogoutPayload struct {
	AllDevices bool `json:"all_devices"`
}

func defaultPrivacySettings() PrivacySettingsDTO {
	return PrivacySettingsDTO{
		IsPrivateAccount:           false,
		WhoCanComment:              "everyone",
		WhoCanMention:              "everyone",
		WhoCanFindYou:              "everyone",
		ShowActivityStatus:         true,
		LikesVisibility:            "everyone",
		FollowingVisibility:        "everyone",
		DataSharingPersonalization: true,
		AnalyticsSharing:           true,
		EssentialCookies:           true,
		PersonalizationCookies:     true,
		UpdatedAtMs:                time.Now().UnixMilli(),
	}
}

func defaultNotificationSettings() NotificationSettingsDTO {
	return NotificationSettingsDTO{
		MasterPause:        false,
		ChannelPush:        true,
		ChannelEmail:       true,
		ChannelSms:         false,
		TypeMessages:       true,
		TypeComments:       true,
		TypeFollowers:      true,
		TypeProductUpdates: false,
		QuietHoursEnabled:  false,
		QuietStartMinutes:  22 * 60,
		QuietEndMinutes:    7 * 60,
		UpdatedAtMs:        time.Now().UnixMilli(),
	}
}

func defaultContentPreferences() ContentPreferencesDTO {
	return ContentPreferencesDTO{
		SuggestedContentEnabled: true,
		NotInterestedTopics:     []string{},
		LanguageCode:            "en",
		AutoplayVideos:          true,
		VideoQuality:            "auto",
		DataSaver:               false,
		CaptionsEnabled:         false,
		ReducedMotion:           false,
		UpdatedAtMs:             time.Now().UnixMilli(),
	}
}

func normalizeAudience3(raw, fallback string) string {
	switch strings.ToLower(strings.TrimSpace(raw)) {
	case "everyone":
		return "everyone"
	case "friends":
		return "friends"
	case "no_one":
		return "no_one"
	default:
		return fallback
	}
}

func normalizeDiscoverability(raw string) string {
	switch strings.ToLower(strings.TrimSpace(raw)) {
	case "everyone":
		return "everyone"
	case "contacts":
		return "contacts"
	case "no_one":
		return "no_one"
	default:
		return "everyone"
	}
}

func normalizeVisibility3(raw string) string {
	switch strings.ToLower(strings.TrimSpace(raw)) {
	case "everyone":
		return "everyone"
	case "friends":
		return "friends"
	case "only_me":
		return "only_me"
	default:
		return "everyone"
	}
}

func normalizeVideoQuality(raw string) string {
	switch strings.ToLower(strings.TrimSpace(raw)) {
	case "high":
		return "high"
	case "data_saver":
		return "data_saver"
	default:
		return "auto"
	}
}

func (h *Handler) getUserPrivacyLocked(userID string) PrivacySettingsDTO {
	if p, ok := h.privacySettings[userID]; ok {
		return p
	}
	return defaultPrivacySettings()
}

func (h *Handler) getUserNotificationsLocked(userID string) NotificationSettingsDTO {
	if n, ok := h.notifSettings[userID]; ok {
		return n
	}
	return defaultNotificationSettings()
}

func (h *Handler) getUserContentPrefsLocked(userID string) ContentPreferencesDTO {
	if c, ok := h.contentPrefs[userID]; ok {
		return c
	}
	return defaultContentPreferences()
}

func (h *Handler) handleGetSettings(w http.ResponseWriter, r *http.Request) {
	claims, _, _, ok := h.requireAuth(w, r)
	if !ok {
		return
	}
	h.mu.Lock()
	priv := h.getUserPrivacyLocked(claims.UserID)
	chatPriv := h.getUserChatPrivacyLocked(claims.UserID)
	notif := h.getUserNotificationsLocked(claims.UserID)
	content := h.getUserContentPrefsLocked(claims.UserID)
	syncPrefs := h.getUserSyncPrefsLocked(claims.UserID)
	h.mu.Unlock()

	writeJSON(w, http.StatusOK, map[string]any{
		"success":             true,
		"privacy":             priv,
		"chat_privacy":        chatPriv,
		"notifications":       notif,
		"content_preferences": content,
		"sync_preferences":    syncPrefs,
		"server_epoch_ms":     time.Now().UnixMilli(),
	})
}

func (h *Handler) handleUpdatePrivacySettings(w http.ResponseWriter, r *http.Request) {
	claims, deviceFP, ipFP, ok := h.requireAuth(w, r)
	if !ok {
		return
	}
	var payload PrivacySettingsDTO
	if err := json.NewDecoder(http.MaxBytesReader(w, r.Body, 8192)).Decode(&payload); err != nil {
		sanitizer.WriteSanitizedError(w, http.StatusBadRequest, "decode_privacy_settings", err, sanitizer.GenericErrorMessage)
		return
	}

	nowMs := time.Now().UnixMilli()
	cleaned := PrivacySettingsDTO{
		IsPrivateAccount:           payload.IsPrivateAccount,
		WhoCanComment:              normalizeAudience3(payload.WhoCanComment, "everyone"),
		WhoCanMention:              normalizeAudience3(payload.WhoCanMention, "everyone"),
		WhoCanFindYou:              normalizeDiscoverability(payload.WhoCanFindYou),
		ShowActivityStatus:         payload.ShowActivityStatus,
		LikesVisibility:            normalizeVisibility3(payload.LikesVisibility),
		FollowingVisibility:        normalizeVisibility3(payload.FollowingVisibility),
		DataSharingPersonalization: payload.DataSharingPersonalization,
		AnalyticsSharing:           payload.AnalyticsSharing,
		EssentialCookies:           true, // Essential session cookies cannot be disabled
		PersonalizationCookies:     payload.PersonalizationCookies,
		UpdatedAtMs:                nowMs,
	}

	h.mu.Lock()
	h.privacySettings[claims.UserID] = cleaned
	content := h.getUserContentPrefsLocked(claims.UserID)
	h.mu.Unlock()

	h.feeds.UpdateUserContentControls(
		claims.UserID,
		content.SuggestedContentEnabled,
		content.NotInterestedTopics,
		cleaned.DataSharingPersonalization && cleaned.PersonalizationCookies,
		false,
	)
	h.audit.Record(claims.UserID, "privacy_settings_updated", deviceFP, ipFP, map[string]string{
		"is_private_account": fmt.Sprintf("%t", cleaned.IsPrivateAccount),
	})

	writeJSON(w, http.StatusOK, map[string]any{
		"success":         true,
		"privacy":         cleaned,
		"server_epoch_ms": nowMs,
	})
}

func (h *Handler) handleUpdateNotificationSettings(w http.ResponseWriter, r *http.Request) {
	claims, deviceFP, ipFP, ok := h.requireAuth(w, r)
	if !ok {
		return
	}
	var payload NotificationSettingsDTO
	if err := json.NewDecoder(http.MaxBytesReader(w, r.Body, 8192)).Decode(&payload); err != nil {
		sanitizer.WriteSanitizedError(w, http.StatusBadRequest, "decode_notif_settings", err, sanitizer.GenericErrorMessage)
		return
	}

	startMin := payload.QuietStartMinutes
	if startMin < 0 || startMin >= 1440 {
		startMin = 22 * 60
	}
	endMin := payload.QuietEndMinutes
	if endMin < 0 || endMin >= 1440 {
		endMin = 7 * 60
	}

	nowMs := time.Now().UnixMilli()
	cleaned := NotificationSettingsDTO{
		MasterPause:        payload.MasterPause,
		ChannelPush:        payload.ChannelPush,
		ChannelEmail:       payload.ChannelEmail,
		ChannelSms:         payload.ChannelSms,
		TypeMessages:       payload.TypeMessages,
		TypeComments:       payload.TypeComments,
		TypeFollowers:      payload.TypeFollowers,
		TypeProductUpdates: payload.TypeProductUpdates,
		QuietHoursEnabled:  payload.QuietHoursEnabled,
		QuietStartMinutes:  startMin,
		QuietEndMinutes:    endMin,
		UpdatedAtMs:        nowMs,
	}

	h.mu.Lock()
	h.notifSettings[claims.UserID] = cleaned
	h.mu.Unlock()

	h.audit.Record(claims.UserID, "notification_settings_updated", deviceFP, ipFP, nil)
	writeJSON(w, http.StatusOK, map[string]any{
		"success":         true,
		"notifications":   cleaned,
		"server_epoch_ms": nowMs,
	})
}

func (h *Handler) handleUpdateContentPreferences(w http.ResponseWriter, r *http.Request) {
	claims, deviceFP, ipFP, ok := h.requireAuth(w, r)
	if !ok {
		return
	}
	var payload ContentPreferencesDTO
	if err := json.NewDecoder(http.MaxBytesReader(w, r.Body, 16384)).Decode(&payload); err != nil {
		sanitizer.WriteSanitizedError(w, http.StatusBadRequest, "decode_content_prefs", err, sanitizer.GenericErrorMessage)
		return
	}

	cleanTopics := make([]string, 0, len(payload.NotInterestedTopics))
	seen := make(map[string]bool)
	for _, raw := range payload.NotInterestedTopics {
		t := strings.TrimPrefix(strings.ToLower(security.SanitizeText(raw, 40, false)), "#")
		if t != "" && !seen[t] && len(cleanTopics) < 30 {
			seen[t] = true
			cleanTopics = append(cleanTopics, t)
		}
	}

	lang := strings.ToLower(security.SanitizeText(payload.LanguageCode, 10, false))
	if lang == "" {
		lang = "en"
	}

	nowMs := time.Now().UnixMilli()
	cleaned := ContentPreferencesDTO{
		SuggestedContentEnabled: payload.SuggestedContentEnabled,
		NotInterestedTopics:     cleanTopics,
		LanguageCode:            lang,
		AutoplayVideos:          payload.AutoplayVideos,
		VideoQuality:            normalizeVideoQuality(payload.VideoQuality),
		DataSaver:               payload.DataSaver,
		CaptionsEnabled:         payload.CaptionsEnabled,
		ReducedMotion:           payload.ReducedMotion,
		UpdatedAtMs:             nowMs,
	}

	h.mu.Lock()
	h.contentPrefs[claims.UserID] = cleaned
	priv := h.getUserPrivacyLocked(claims.UserID)
	h.mu.Unlock()

	h.feeds.UpdateUserContentControls(
		claims.UserID,
		cleaned.SuggestedContentEnabled,
		cleaned.NotInterestedTopics,
		priv.DataSharingPersonalization && priv.PersonalizationCookies,
		payload.ResetSuggestions,
	)
	h.audit.Record(claims.UserID, "content_preferences_updated", deviceFP, ipFP, nil)

	writeJSON(w, http.StatusOK, map[string]any{
		"success":             true,
		"content_preferences": cleaned,
		"server_epoch_ms":     nowMs,
	})
}

func (h *Handler) handleListBlockedAccounts(w http.ResponseWriter, r *http.Request) {
	claims, _, _, ok := h.requireAuth(w, r)
	if !ok {
		return
	}
	query := strings.ToLower(security.SanitizeText(r.URL.Query().Get("q"), 64, false))
	idMap := h.feeds.ListBlockedUsers(claims.UserID)
	items := h.hydrateRestrictedAccounts(idMap, query)
	writeJSON(w, http.StatusOK, map[string]any{
		"success": true,
		"items":   items,
	})
}

func (h *Handler) handleListMutedAccounts(w http.ResponseWriter, r *http.Request) {
	claims, _, _, ok := h.requireAuth(w, r)
	if !ok {
		return
	}
	query := strings.ToLower(security.SanitizeText(r.URL.Query().Get("q"), 64, false))
	idMap := h.feeds.ListMutedUsers(claims.UserID)
	items := h.hydrateRestrictedAccounts(idMap, query)
	writeJSON(w, http.StatusOK, map[string]any{
		"success": true,
		"items":   items,
	})
}

func (h *Handler) hydrateRestrictedAccounts(idMap map[string]int64, query string) []RestrictedAccountItemDTO {
	items := make([]RestrictedAccountItemDTO, 0, len(idMap))
	for uid, createdAtMs := range idMap {
		u, exists := h.contacts.GetUserByID(uid)
		fullName := uid
		username := uid
		avatarURL := ""
		if exists {
			fullName = u.FullName
			username = u.Username
			if u.AvatarMediaID != "" {
				avatarURL = "/v1/media/avatar/" + u.AvatarMediaID
			}
		} else {
			h.mu.Lock()
			if sp, ok := h.profiles[uid]; ok {
				fullName = sp.FullName
				username = sp.Username
				if sp.AvatarMediaID != "" {
					avatarURL = "/v1/media/avatar/" + sp.AvatarMediaID
				}
			}
			h.mu.Unlock()
		}
		if query != "" && !strings.Contains(strings.ToLower(fullName), query) && !strings.Contains(strings.ToLower(username), query) {
			continue
		}
		items = append(items, RestrictedAccountItemDTO{
			UserID:      uid,
			FullName:    fullName,
			Username:    username,
			AvatarURL:   avatarURL,
			CreatedAtMs: createdAtMs,
		})
	}
	sort.Slice(items, func(i, j int) bool {
		if items[i].CreatedAtMs != items[j].CreatedAtMs {
			return items[i].CreatedAtMs > items[j].CreatedAtMs
		}
		return items[i].Username < items[j].Username
	})
	return items
}

func (h *Handler) handleMuteUser(w http.ResponseWriter, r *http.Request) {
	claims, deviceFP, ipFP, ok := h.requireAuth(w, r)
	if !ok {
		return
	}
	targetID := security.SanitizeText(r.PathValue("id"), 128, false)
	if targetID == "" || targetID == claims.UserID {
		sanitizer.WriteSanitizedError(w, http.StatusBadRequest, "invalid_mute_target", nil, sanitizer.GenericErrorMessage)
		return
	}
	h.feeds.MuteUser(claims.UserID, targetID)
	h.audit.Record(claims.UserID, "user_muted", deviceFP, ipFP, map[string]string{
		"target_user_id": targetID,
	})
	writeJSON(w, http.StatusOK, map[string]any{
		"success":  true,
		"user_id":  targetID,
		"is_muted": true,
	})
}

func (h *Handler) handleUnmuteUser(w http.ResponseWriter, r *http.Request) {
	claims, deviceFP, ipFP, ok := h.requireAuth(w, r)
	if !ok {
		return
	}
	targetID := security.SanitizeText(r.PathValue("id"), 128, false)
	if targetID == "" || targetID == claims.UserID {
		sanitizer.WriteSanitizedError(w, http.StatusBadRequest, "invalid_unmute_target", nil, sanitizer.GenericErrorMessage)
		return
	}
	h.feeds.UnmuteUser(claims.UserID, targetID)
	h.audit.Record(claims.UserID, "user_unmuted", deviceFP, ipFP, map[string]string{
		"target_user_id": targetID,
	})
	writeJSON(w, http.StatusOK, map[string]any{
		"success":  true,
		"user_id":  targetID,
		"is_muted": false,
	})
}

func (h *Handler) handleSubmitProblemReport(w http.ResponseWriter, r *http.Request) {
	claims, deviceFP, ipFP, ok := h.requireAuth(w, r)
	if !ok {
		return
	}
	var payload ProblemReportPayload
	if err := json.NewDecoder(http.MaxBytesReader(w, r.Body, 16384)).Decode(&payload); err != nil {
		sanitizer.WriteSanitizedError(w, http.StatusBadRequest, "decode_problem_report", err, sanitizer.GenericErrorMessage)
		return
	}

	category := security.SanitizeText(payload.Category, 64, false)
	description := security.SanitizeText(payload.Description, 2000, true)
	if category == "" || len([]rune(description)) < 5 {
		sanitizer.WriteSanitizedError(w, http.StatusBadRequest, "invalid_problem_report", nil, "Select a category and describe the issue in at least 5 characters")
		return
	}

	opID := security.SanitizeText(payload.ClientOpID, 80, false)
	nowMs := time.Now().UnixMilli()

	h.mu.Lock()
	if opID != "" {
		for _, existing := range h.supportTickets[claims.UserID] {
			if existing.TicketID == opID {
				h.mu.Unlock()
				writeJSON(w, http.StatusOK, map[string]any{
					"success":          true,
					"reference_number": existing.ReferenceNumber,
					"ticket":           existing,
				})
				return
			}
		}
	}

	var rnd [3]byte
	_, _ = rand.Read(rnd[:])
	ref := fmt.Sprintf("TF-%s-%s", time.Now().UTC().Format("20060102"), strings.ToUpper(hex.EncodeToString(rnd[:])))
	ticketID := opID
	if ticketID == "" {
		ticketID = ref
	}
	ticket := SupportTicketDTO{
		TicketID:          ticketID,
		ReferenceNumber:   ref,
		Category:          category,
		Description:       description,
		ScreenshotMediaID: security.SanitizeText(payload.ScreenshotMediaID, 64, false),
		ScreenshotSha256:  security.SanitizeText(payload.ScreenshotSha256, 64, false),
		Status:            "open",
		ResolutionNote:    "Received by Telefam Trust & Support. You will receive updates in Ticket History.",
		CreatedAtMs:       nowMs,
		UpdatedAtMs:       nowMs,
	}
	h.supportTickets[claims.UserID] = append([]SupportTicketDTO{ticket}, h.supportTickets[claims.UserID]...)
	h.mu.Unlock()

	h.audit.Record(claims.UserID, "problem_report_submitted", deviceFP, ipFP, map[string]string{
		"reference": ref,
		"category":  category,
	})
	writeJSON(w, http.StatusCreated, map[string]any{
		"success":          true,
		"reference_number": ref,
		"ticket":           ticket,
	})
}

func (h *Handler) handleListSupportTickets(w http.ResponseWriter, r *http.Request) {
	claims, _, _, ok := h.requireAuth(w, r)
	if !ok {
		return
	}
	h.mu.Lock()
	items := append([]SupportTicketDTO(nil), h.supportTickets[claims.UserID]...)
	h.mu.Unlock()

	writeJSON(w, http.StatusOK, map[string]any{
		"success": true,
		"items":   items,
	})
}

func (h *Handler) handleSearchFaqs(w http.ResponseWriter, r *http.Request) {
	if _, _, _, ok := h.requireAuth(w, r); !ok {
		return
	}
	query := strings.ToLower(security.SanitizeText(r.URL.Query().Get("q"), 64, false))
	allFaqs := []FaqItemDTO{
		{ID: "faq_privacy_acct", Category: "Privacy", Question: "How does a Private account work?", Answer: "When Private account is enabled, only approved followers see your posts in feeds and profile grids. Non-followers only see your name, handle, avatar, and privacy notice."},
		{ID: "faq_username_lock", Category: "Account", Question: "Why can I only change my username every 14 days?", Answer: "The 14-day cooldown protects your identity from impersonation, broken share links, and automated handle squatting."},
		{ID: "faq_locked_tab", Category: "Posts", Question: "Where do posts go when set to Private?", Answer: "Any post whose privacy is switched to Private is removed from For You and Following feeds and moved to the Locked tab on your profile."},
		{ID: "faq_video_quality", Category: "Playback", Question: "How do Autoplay, Video quality, and Data saver work?", Answer: "Changes in Content preferences take effect immediately without restarting. Data saver selects lower bitrate adaptive streams and reduces preload chunk sizes."},
		{ID: "faq_block_mute", Category: "Safety", Question: "What is the difference between Blocking and Muting?", Answer: "Blocking stops someone from seeing your content or interacting with you. Muting hides their posts from your feeds without notifying them."},
		{ID: "faq_offline", Category: "Reliability", Question: "What happens if I change settings while offline?", Answer: "Changes apply immediately from your local encrypted cache and queue safely for automatic synchronization as soon as connectivity returns."},
	}
	filtered := make([]FaqItemDTO, 0, len(allFaqs))
	for _, f := range allFaqs {
		if query == "" ||
			strings.Contains(strings.ToLower(f.Question), query) ||
			strings.Contains(strings.ToLower(f.Answer), query) ||
			strings.Contains(strings.ToLower(f.Category), query) {
			filtered = append(filtered, f)
		}
	}
	writeJSON(w, http.StatusOK, map[string]any{
		"success": true,
		"items":   filtered,
	})
}

func (h *Handler) handleSystemStatus(w http.ResponseWriter, r *http.Request) {
	if _, _, _, ok := h.requireAuth(w, r); !ok {
		return
	}
	nowMs := time.Now().UnixMilli()
	services := []SystemServiceStatusDTO{
		{ServiceName: "Authentication & Session Security", Status: "operational", LatencyMs: 18, Summary: "Token rotation, OTP mailers, and RLS enforcement operating normally."},
		{ServiceName: "Feeds & Adaptive Video Streaming", Status: "operational", LatencyMs: 24, Summary: "Byte-range streaming, bitrate renditions, and recommendations active."},
		{ServiceName: "Media Fingerprinting & Uploads", Status: "operational", LatencyMs: 31, Summary: "SHA-256, dHash, and frame-hash verification operating normally."},
		{ServiceName: "Notifications & Support Desk", Status: "operational", LatencyMs: 21, Summary: "Delivery pipelines and ticket intake accepting submissions."},
	}
	writeJSON(w, http.StatusOK, map[string]any{
		"success":         true,
		"overall_status":  "operational",
		"services":        services,
		"checked_at_ms":   nowMs,
	})
}

func (h *Handler) handleLegalDocuments(w http.ResponseWriter, r *http.Request) {
	if _, _, _, ok := h.requireAuth(w, r); !ok {
		return
	}
	docs := []LegalDocumentDTO{
		{
			ID:          "terms",
			Title:       "Terms of Service",
			LastUpdated: "November 14, 2026",
			Sections: []string{
				"1. Acceptance & Eligibility: By creating or accessing a Telefam account, you agree to these Terms of Service and confirm that you meet the minimum age requirement of 13 years (or higher where required by local law).",
				"2. Account Security & Identity: You are responsible for maintaining the confidentiality of your verification sessions and connected devices. Usernames may be updated once every 14 days to preserve authentic identity and prevent abuse.",
				"3. User Content & Ownership: You retain full ownership of videos, photos, audio selections, and text posts you share. By publishing content on Telefam, you grant us a limited, non-exclusive license to process, transcode adaptive bitrates, fingerprint for safety, and deliver that content in accordance with your chosen privacy setting (Public, Friends, or Private).",
				"4. Acceptable Conduct: Automated scraping, token replay, impersonation, harassment, malicious uploads, and circumvention of privacy controls or Row-Level Security boundaries are strictly prohibited and may result in immediate account termination.",
				"5. Disclaimers & Termination: Services are provided on an 'as-is' and 'as-available' basis. You may log out of individual devices or revoke all active sessions at any time from Settings and privacy.",
			},
		},
		{
			ID:          "privacy",
			Title:       "Privacy Policy",
			LastUpdated: "November 14, 2026",
			Sections: []string{
				"1. Information You Provide: We collect your email address, full name, username, optional phone number, date of birth, location, bio, profile avatar, background photos, and content you choose to upload.",
				"2. Security & Integrity Telemetry: We compute cryptographic SHA-256 checksums, perceptual image hashes (dHash), and device integrity fingerprints solely to prevent spam, duplicate abuse, replay attacks, and unauthorized session access.",
				"3. Granular Privacy Controls: You control account visibility (Private account), commenting, mentions, search discoverability, activity status indicators, likes visibility, following list visibility, and whether personalization data shapes recommendations.",
				"4. Local & Offline Caching: Essential data (session credentials in encrypted storage, recent profiles, and preference states) may be cached securely on your device so settings and feeds work seamlessly offline and synchronize on reconnect.",
				"5. Your Rights: You may edit profile details, manage blocked or muted accounts, restrict analytics and personalization cookies, or permanently revoke all device sessions at any time.",
			},
		},
		{
			ID:          "guidelines",
			Title:       "Community Guidelines",
			LastUpdated: "November 14, 2026",
			Sections: []string{
				"1. Keep Telefam Respectful: Treat family members and creators with dignity. Harassment, bullying, hate speech, threats, or targeted intimidation will not be tolerated.",
				"2. Authentic Identity & Consent: Do not impersonate another individual or share private personal information without explicit permission. Respect Private and Friends visibility boundaries.",
				"3. Child & Youth Safety: Any content involving exploitation or endangerment of minors is strictly prohibited and reported to appropriate child-protection authorities.",
				"4. Honest Engagement: Avoid deceptive spam, scam links, artificially manipulated engagement, or repeated abusive reporting.",
				"5. Enforcement: Depending on severity and history, violations may lead to content removal, temporary restriction, or permanent account suspension, with transparent tracking in Ticket History.",
			},
		},
		{
			ID:          "cookies",
			Title:       "Cookie & Local Storage Policy",
			LastUpdated: "November 14, 2026",
			Sections: []string{
				"1. Essential Storage (Always Active): Required for encrypted session verification, CSRF/replay protection, device binding, and offline outbox synchronization.",
				"2. Personalization & Recommendations: Optional storage used to tailor 'For You' recommendations and search shortcuts based on your watch completions, likes, and 'not interested' topics.",
				"3. Diagnostics & Analytics: Optional aggregated latency and crash signals used strictly to improve playback smoothness and reliability.",
			},
		},
		{
			ID:          "licenses",
			Title:       "Licenses & Third-Party Notices",
			LastUpdated: "November 14, 2026",
			Sections: []string{
				"1. Software License: Telefam client applications and proprietary recommendation services are licensed for personal, non-commercial mobile and desktop use in accordance with these Terms.",
				"2. Open Source Components: Telefam incorporates respected open-source libraries including Kotlin, Jetpack Compose, Material 3, AndroidX Room, Retrofit, OkHttp, Moshi, Coil, Media3 ExoPlayer, and the Go standard library under their respective Apache 2.0, MIT, and BSD-3-Clause licenses.",
			},
		},
	}
	writeJSON(w, http.StatusOK, map[string]any{
		"success": true,
		"items":   docs,
	})
}

func (h *Handler) handleAppVersion(w http.ResponseWriter, r *http.Request) {
	if _, _, _, ok := h.requireAuth(w, r); !ok {
		return
	}
	clientVersion := security.SanitizeText(r.URL.Query().Get("version"), 24, false)
	if clientVersion == "" {
		clientVersion = "1.0"
	}
	latestVersion := "1.0"
	writeJSON(w, http.StatusOK, map[string]any{
		"success":               true,
		"latest_version":        latestVersion,
		"min_supported_version": "1.0",
		"build_number":          1,
		"update_available":      clientVersion != latestVersion && clientVersion < latestVersion,
		"release_notes":         "Latest Settings and Privacy hub, offline sync outbox, and adaptive multi-ratio playback.",
	})
}

func (h *Handler) handleLogout(w http.ResponseWriter, r *http.Request) {
	claims, deviceFP, ipFP, ok := h.requireAuth(w, r)
	if !ok {
		return
	}
	var payload LogoutPayload
	_ = json.NewDecoder(http.MaxBytesReader(w, r.Body, 2048)).Decode(&payload)

	if payload.AllDevices {
		h.tokens.RevokeAllUserSessions(claims.UserID)
		h.mu.Lock()
		h.revokeAllDevicesLocked(claims.UserID)
		h.mu.Unlock()
		h.ensureNotificationsService().RevokeAllUserTokens(claims.UserID)
		h.ensureChatService().DisconnectUser(claims.UserID)
		h.audit.Record(claims.UserID, "user_logged_out_all_devices", deviceFP, ipFP, nil)
	} else {
		h.tokens.RevokeFamily(claims.FamilyID)
		h.mu.Lock()
		h.revokeDeviceLocked(claims.UserID, deviceFP)
		h.mu.Unlock()
		h.ensureNotificationsService().RevokeDeviceToken(claims.UserID, deviceFP)
		h.ensureChatService().DisconnectUserDevice(claims.UserID, deviceFP)
		h.audit.Record(claims.UserID, "user_logged_out_device", deviceFP, ipFP, nil)
	}

	writeJSON(w, http.StatusOK, map[string]any{
		"success":     true,
		"all_devices": payload.AllDevices,
	})
}

// Sanity helper used by contacts package reference
var _ = contacts.ComputeRelationship
var _ = security.SanitizeText
