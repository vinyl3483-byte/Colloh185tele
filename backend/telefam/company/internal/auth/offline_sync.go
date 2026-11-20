package auth

import (
	"encoding/json"
	"net/http"
	"strings"
	"time"

	"telefam.company/backend/internal/errors"
	"telefam.company/backend/internal/security"
)

type SyncPreferencesDTO struct {
	AutoSyncEnabled       bool   `json:"auto_sync_enabled"`
	SyncOverWifiOnly      bool   `json:"sync_over_wifi_only"`
	BackgroundSyncEnabled bool   `json:"background_sync_enabled"`
	CacheFeedsOffline     bool   `json:"cache_feeds_offline"`
	CacheChatMediaOffline bool   `json:"cache_chat_media_offline"`
	LastSyncedAtMs        int64  `json:"last_synced_at_ms"`
	LastSyncStatus        string `json:"last_sync_status"`
	TotalSyncedOps        int64  `json:"total_synced_ops"`
	UpdatedAtMs           int64  `json:"updated_at_ms"`
}

type ManualSyncRequestDTO struct {
	ClientSyncID       string                   `json:"client_sync_id,omitempty"`
	Operations         []SyncOperation          `json:"operations,omitempty"`
	Privacy            *PrivacySettingsDTO      `json:"privacy,omitempty"`
	Notifications      *NotificationSettingsDTO `json:"notifications,omitempty"`
	ContentPreferences *ContentPreferencesDTO   `json:"content_preferences,omitempty"`
	ChatPrivacy        *ChatPrivacySettingsDTO  `json:"chat_privacy,omitempty"`
	SyncPreferences    *SyncPreferencesDTO      `json:"sync_preferences,omitempty"`
}

func defaultSyncPreferences() SyncPreferencesDTO {
	return SyncPreferencesDTO{
		AutoSyncEnabled:       true,
		SyncOverWifiOnly:      false,
		BackgroundSyncEnabled: true,
		CacheFeedsOffline:     true,
		CacheChatMediaOffline: true,
		LastSyncedAtMs:        0,
		LastSyncStatus:        "idle",
		TotalSyncedOps:        0,
		UpdatedAtMs:           time.Now().UnixMilli(),
	}
}

func (h *Handler) getUserSyncPrefsLocked(userID string) SyncPreferencesDTO {
	if s, ok := h.syncPrefs[userID]; ok {
		return s
	}
	return defaultSyncPreferences()
}

func sanitizeSyncPreferences(payload SyncPreferencesDTO, existing SyncPreferencesDTO, nowMs int64) SyncPreferencesDTO {
	status := strings.ToLower(strings.TrimSpace(payload.LastSyncStatus))
	switch status {
	case "idle", "synced", "queued", "error":
	default:
		status = existing.LastSyncStatus
		if status == "" {
			status = "idle"
		}
	}
	lastSynced := existing.LastSyncedAtMs
	if payload.LastSyncedAtMs > lastSynced {
		lastSynced = payload.LastSyncedAtMs
	}
	totalOps := existing.TotalSyncedOps
	if payload.TotalSyncedOps > totalOps {
		totalOps = payload.TotalSyncedOps
	}
	return SyncPreferencesDTO{
		AutoSyncEnabled:       payload.AutoSyncEnabled,
		SyncOverWifiOnly:      payload.SyncOverWifiOnly,
		BackgroundSyncEnabled: payload.BackgroundSyncEnabled,
		CacheFeedsOffline:     payload.CacheFeedsOffline,
		CacheChatMediaOffline: payload.CacheChatMediaOffline,
		LastSyncedAtMs:        lastSynced,
		LastSyncStatus:        status,
		TotalSyncedOps:        totalOps,
		UpdatedAtMs:           nowMs,
	}
}

func (h *Handler) handleGetSyncSettings(w http.ResponseWriter, r *http.Request) {
	claims, deviceFP, _, ok := h.requireAuth(w, r)
	if !ok {
		return
	}
	h.mu.Lock()
	sp := h.getUserSyncPrefsLocked(claims.UserID)
	checkpoint := h.syncCheckpoints[claims.UserID+"|"+deviceFP]
	h.mu.Unlock()

	writeJSON(w, http.StatusOK, map[string]any{
		"success":           true,
		"sync_preferences":  sp,
		"checkpoint_epoch":  checkpoint,
		"server_epoch_ms":   time.Now().UnixMilli(),
	})
}

func (h *Handler) handleUpdateSyncSettings(w http.ResponseWriter, r *http.Request) {
	claims, deviceFP, ipFP, ok := h.requireAuth(w, r)
	if !ok {
		return
	}
	var payload SyncPreferencesDTO
	if err := json.NewDecoder(http.MaxBytesReader(w, r.Body, 8192)).Decode(&payload); err != nil {
		sanitizer.WriteSanitizedError(w, http.StatusBadRequest, "decode_sync_prefs", err, sanitizer.GenericErrorMessage)
		return
	}

	nowMs := time.Now().UnixMilli()
	h.mu.Lock()
	existing := h.getUserSyncPrefsLocked(claims.UserID)
	cleaned := sanitizeSyncPreferences(payload, existing, nowMs)
	h.syncPrefs[claims.UserID] = cleaned
	h.mu.Unlock()

	h.audit.Record(claims.UserID, "offline_sync_prefs_updated", deviceFP, ipFP, nil)
	writeJSON(w, http.StatusOK, map[string]any{
		"success":          true,
		"sync_preferences": cleaned,
		"server_epoch_ms":  nowMs,
	})
}

func (h *Handler) handleSyncNow(w http.ResponseWriter, r *http.Request) {
	claims, deviceFP, ipFP, ok := h.requireAuth(w, r)
	if !ok {
		return
	}
	var req ManualSyncRequestDTO
	if err := json.NewDecoder(http.MaxBytesReader(w, r.Body, 1<<20)).Decode(&req); err != nil {
		sanitizer.WriteSanitizedError(w, http.StatusBadRequest, "decode_manual_sync", err, sanitizer.GenericErrorMessage)
		return
	}

	nowMs := time.Now().UnixMilli()
	ackedIDs := make([]string, 0, len(req.Operations))
	appliedSections := 0

	h.mu.Lock()
	for _, op := range req.Operations {
		cleanOpID := strings.TrimSpace(op.ClientOpID)
		if cleanOpID == "" {
			continue
		}
		h.ackedOps[cleanOpID] = struct{}{}
		ackedIDs = append(ackedIDs, cleanOpID)
	}

	if req.Privacy != nil {
		prev := h.getUserPrivacyLocked(claims.UserID)
		if req.Privacy.UpdatedAtMs == 0 || req.Privacy.UpdatedAtMs >= prev.UpdatedAtMs {
			h.privacySettings[claims.UserID] = PrivacySettingsDTO{
				IsPrivateAccount:           req.Privacy.IsPrivateAccount,
				WhoCanComment:              normalizeAudience3(req.Privacy.WhoCanComment, "everyone"),
				WhoCanMention:              normalizeAudience3(req.Privacy.WhoCanMention, "everyone"),
				WhoCanFindYou:              normalizeDiscoverability(req.Privacy.WhoCanFindYou),
				ShowActivityStatus:         req.Privacy.ShowActivityStatus,
				LikesVisibility:            normalizeVisibility3(req.Privacy.LikesVisibility),
				FollowingVisibility:        normalizeVisibility3(req.Privacy.FollowingVisibility),
				DataSharingPersonalization: req.Privacy.DataSharingPersonalization,
				AnalyticsSharing:           req.Privacy.AnalyticsSharing,
				EssentialCookies:           true,
				PersonalizationCookies:     req.Privacy.PersonalizationCookies,
				UpdatedAtMs:                nowMs,
			}
			appliedSections++
		}
	}

	if req.Notifications != nil {
		prev := h.getUserNotificationsLocked(claims.UserID)
		if req.Notifications.UpdatedAtMs == 0 || req.Notifications.UpdatedAtMs >= prev.UpdatedAtMs {
			startMin := req.Notifications.QuietStartMinutes
			if startMin < 0 || startMin >= 1440 {
				startMin = 22 * 60
			}
			endMin := req.Notifications.QuietEndMinutes
			if endMin < 0 || endMin >= 1440 {
				endMin = 7 * 60
			}
			h.notifSettings[claims.UserID] = NotificationSettingsDTO{
				MasterPause:        req.Notifications.MasterPause,
				ChannelPush:        req.Notifications.ChannelPush,
				ChannelEmail:       req.Notifications.ChannelEmail,
				ChannelSms:         req.Notifications.ChannelSms,
				TypeMessages:       req.Notifications.TypeMessages,
				TypeComments:       req.Notifications.TypeComments,
				TypeFollowers:      req.Notifications.TypeFollowers,
				TypeProductUpdates: req.Notifications.TypeProductUpdates,
				QuietHoursEnabled:  req.Notifications.QuietHoursEnabled,
				QuietStartMinutes:  startMin,
				QuietEndMinutes:    endMin,
				UpdatedAtMs:        nowMs,
			}
			appliedSections++
		}
	}

	if req.ContentPreferences != nil {
		prev := h.getUserContentPrefsLocked(claims.UserID)
		if req.ContentPreferences.UpdatedAtMs == 0 || req.ContentPreferences.UpdatedAtMs >= prev.UpdatedAtMs {
			cleanTopics := make([]string, 0, len(req.ContentPreferences.NotInterestedTopics))
			seen := make(map[string]bool)
			for _, raw := range req.ContentPreferences.NotInterestedTopics {
				t := strings.TrimPrefix(strings.ToLower(security.SanitizeText(raw, 40, false)), "#")
				if t != "" && !seen[t] && len(cleanTopics) < 30 {
					seen[t] = true
					cleanTopics = append(cleanTopics, t)
				}
			}
			lang := strings.ToLower(security.SanitizeText(req.ContentPreferences.LanguageCode, 10, false))
			if lang == "" {
				lang = "en"
			}
			h.contentPrefs[claims.UserID] = ContentPreferencesDTO{
				SuggestedContentEnabled: req.ContentPreferences.SuggestedContentEnabled,
				NotInterestedTopics:     cleanTopics,
				LanguageCode:            lang,
				AutoplayVideos:          req.ContentPreferences.AutoplayVideos,
				VideoQuality:            normalizeVideoQuality(req.ContentPreferences.VideoQuality),
				DataSaver:               req.ContentPreferences.DataSaver,
				CaptionsEnabled:         req.ContentPreferences.CaptionsEnabled,
				ReducedMotion:           req.ContentPreferences.ReducedMotion,
				UpdatedAtMs:             nowMs,
			}
			appliedSections++
		}
	}

	if req.ChatPrivacy != nil {
		prev := h.getUserChatPrivacyLocked(claims.UserID)
		if req.ChatPrivacy.UpdatedAtMs == 0 || req.ChatPrivacy.UpdatedAtMs >= prev.UpdatedAtMs {
			h.chatPrivacySettings[claims.UserID] = ChatPrivacySettingsDTO{
				MessageRequests:     normalizeChatAudience4(req.ChatPrivacy.MessageRequests, prev.MessageRequests),
				WhoCanCall:          normalizeChatAudience4(req.ChatPrivacy.WhoCanCall, prev.WhoCanCall),
				WhoCanScreenshot:    normalizeChatAudience3(req.ChatPrivacy.WhoCanScreenshot, prev.WhoCanScreenshot),
				WhoCanShareChats:    normalizeChatAudience3(req.ChatPrivacy.WhoCanShareChats, prev.WhoCanShareChats),
				WhoCanCopyMessages:  normalizeChatAudience3(req.ChatPrivacy.WhoCanCopyMessages, prev.WhoCanCopyMessages),
				WhoCanDownloadMedia: normalizeChatAudience3(req.ChatPrivacy.WhoCanDownloadMedia, prev.WhoCanDownloadMedia),
				ChatThemeID:         normalizeChatThemeID(req.ChatPrivacy.ChatThemeID),
				BubbleColorID:       normalizeBubbleColorID(req.ChatPrivacy.BubbleColorID),
				ReadReceipts:        normalizeChatAudience3(req.ChatPrivacy.ReadReceipts, prev.ReadReceipts),
				TypingIndicators:    normalizeChatAudience3(req.ChatPrivacy.TypingIndicators, prev.TypingIndicators),
				OnlineIndicators:    normalizeChatAudience3(req.ChatPrivacy.OnlineIndicators, prev.OnlineIndicators),
				UpdatedAtMs:         nowMs,
			}
			appliedSections++
		}
	}

	curSync := h.getUserSyncPrefsLocked(claims.UserID)
	if req.SyncPreferences != nil {
		curSync = sanitizeSyncPreferences(*req.SyncPreferences, curSync, nowMs)
	}
	syncedCount := len(ackedIDs) + appliedSections
	curSync.LastSyncedAtMs = nowMs
	curSync.LastSyncStatus = "synced"
	curSync.TotalSyncedOps += int64(syncedCount)
	curSync.UpdatedAtMs = nowMs
	h.syncPrefs[claims.UserID] = curSync
	h.syncCheckpoints[claims.UserID+"|"+deviceFP] = nowMs

	priv := h.getUserPrivacyLocked(claims.UserID)
	notif := h.getUserNotificationsLocked(claims.UserID)
	content := h.getUserContentPrefsLocked(claims.UserID)
	chatPriv := h.getUserChatPrivacyLocked(claims.UserID)
	h.mu.Unlock()

	h.feeds.UpdateUserContentControls(
		claims.UserID,
		content.SuggestedContentEnabled,
		content.NotInterestedTopics,
		priv.DataSharingPersonalization && priv.PersonalizationCookies,
		false,
	)
	h.audit.Record(claims.UserID, "manual_offline_sync", deviceFP, ipFP, map[string]string{
		"client_sync_id": strings.TrimSpace(req.ClientSyncID),
	})

	writeJSON(w, http.StatusOK, map[string]any{
		"success":             true,
		"synced_ops_count":    syncedCount,
		"acked_op_ids":        ackedIDs,
		"sync_preferences":    curSync,
		"privacy":             priv,
		"notifications":       notif,
		"content_preferences": content,
		"chat_privacy":        chatPriv,
		"server_epoch_ms":     nowMs,
	})
}
