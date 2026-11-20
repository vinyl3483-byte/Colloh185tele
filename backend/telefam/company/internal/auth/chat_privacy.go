package auth

import (
	"encoding/json"
	"net/http"
	"strings"
	"time"

	"telefam.company/backend/internal/errors"
)

type ChatPrivacySettingsDTO struct {
	MessageRequests     string `json:"message_requests"`
	WhoCanCall          string `json:"who_can_call"`
	WhoCanScreenshot    string `json:"who_can_screenshot"`
	WhoCanShareChats    string `json:"who_can_share_chats"`
	WhoCanCopyMessages  string `json:"who_can_copy_messages"`
	WhoCanDownloadMedia string `json:"who_can_download_media"`
	ChatThemeID         string `json:"chat_theme_id"`
	BubbleColorID       string `json:"bubble_color_id"`
	ReadReceipts        string `json:"read_receipts"`
	TypingIndicators    string `json:"typing_indicators"`
	OnlineIndicators    string `json:"online_indicators"`
	UpdatedAtMs         int64  `json:"updated_at_ms"`
}

type ChatThemeCatalogItemDTO struct {
	ThemeID        string `json:"theme_id"`
	Title          string `json:"title"`
	PatternKind    string `json:"pattern_kind"`
	LightBgHex     string `json:"light_bg_hex"`
	DarkBgHex      string `json:"dark_bg_hex"`
	AccentStartHex string `json:"accent_start_hex"`
	AccentEndHex   string `json:"accent_end_hex"`
	SelectionHex   string `json:"selection_hex"`
}

type ChatBubbleColorDTO struct {
	ColorID       string `json:"color_id"`
	Label         string `json:"label"`
	LightStartHex string `json:"light_start_hex"`
	LightEndHex   string `json:"light_end_hex"`
	DarkStartHex  string `json:"dark_start_hex"`
	DarkEndHex    string `json:"dark_end_hex"`
}

type UpdateChatThemePayload struct {
	ChatThemeID   string `json:"chat_theme_id,omitempty"`
	BubbleColorID string `json:"bubble_color_id,omitempty"`
}

func defaultChatPrivacySettings() ChatPrivacySettingsDTO {
	return ChatPrivacySettingsDTO{
		MessageRequests:     "anyone",
		WhoCanCall:          "anyone",
		WhoCanScreenshot:    "nobody",
		WhoCanShareChats:    "nobody",
		WhoCanCopyMessages:  "nobody",
		WhoCanDownloadMedia: "anyone",
		ChatThemeID:         "none",
		BubbleColorID:       "cyan",
		ReadReceipts:        "anyone",
		TypingIndicators:    "nobody",
		OnlineIndicators:    "nobody",
		UpdatedAtMs:         time.Now().UnixMilli(),
	}
}

func chatThemeCatalog() []ChatThemeCatalogItemDTO {
	return []ChatThemeCatalogItemDTO{
		{ThemeID: "none", Title: "Default", PatternKind: "clean", LightBgHex: "#F9FAFB", DarkBgHex: "#0E1014", AccentStartHex: "#D91E2A", AccentEndHex: "#E11319", SelectionHex: "#D91E2A"},
		{ThemeID: "classic_home", Title: "Cozy Home", PatternKind: "home", LightBgHex: "#EAF3FC", DarkBgHex: "#0C1219", AccentStartHex: "#0EA5E9", AccentEndHex: "#0284C7", SelectionHex: "#38BDF8"},
		{ThemeID: "spring_meadow", Title: "Spring Meadow", PatternKind: "meadow", LightBgHex: "#EAF5EE", DarkBgHex: "#0C140E", AccentStartHex: "#16A34A", AccentEndHex: "#22C55E", SelectionHex: "#7ED957"},
		{ThemeID: "arctic_snow", Title: "Arctic Frost", PatternKind: "winter", LightBgHex: "#EBF3F9", DarkBgHex: "#0B131A", AccentStartHex: "#2B7A9E", AccentEndHex: "#5BA4CF", SelectionHex: "#60A5FA"},
		{ThemeID: "deep_sea", Title: "Crystal Ocean", PatternKind: "ocean", LightBgHex: "#E9F2F8", DarkBgHex: "#09121C", AccentStartHex: "#3B82F6", AccentEndHex: "#06B6D4", SelectionHex: "#22D3EE"},
		{ThemeID: "academia_math", Title: "Academia", PatternKind: "math", LightBgHex: "#EBF5F2", DarkBgHex: "#0B1412", AccentStartHex: "#0D9488", AccentEndHex: "#2DD4BF", SelectionHex: "#2DD4BF"},
		{ThemeID: "botanical_tulip", Title: "Botanical", PatternKind: "botanical", LightBgHex: "#FAF0F3", DarkBgHex: "#150C10", AccentStartHex: "#E14D6A", AccentEndHex: "#A855F7", SelectionHex: "#F472B6"},
		{ThemeID: "neon_romance", Title: "Velvet Heart", PatternKind: "romance", LightBgHex: "#F5EFFA", DarkBgHex: "#130C18", AccentStartHex: "#7C3AED", AccentEndHex: "#EC4899", SelectionHex: "#C084FC"},
		{ThemeID: "festive_fireside", Title: "Warm Hearth", PatternKind: "fireside", LightBgHex: "#FBF2EB", DarkBgHex: "#15100B", AccentStartHex: "#D97706", AccentEndHex: "#EA580C", SelectionHex: "#FB923C"},
		{ThemeID: "retro_arcade", Title: "Retro Arcade", PatternKind: "arcade", LightBgHex: "#F3EFFB", DarkBgHex: "#0D0B18", AccentStartHex: "#A855F7", AccentEndHex: "#F59E0B", SelectionHex: "#F472B6"},
		{ThemeID: "cosmic_nebula", Title: "Cosmic Orbit", PatternKind: "cosmic", LightBgHex: "#EEF1FA", DarkBgHex: "#0A0D18", AccentStartHex: "#4F46E5", AccentEndHex: "#06B6D4", SelectionHex: "#818CF8"},
		{ThemeID: "safari_sunset", Title: "Savanna Dusk", PatternKind: "safari", LightBgHex: "#FBF1EC", DarkBgHex: "#170E0B", AccentStartHex: "#B45309", AccentEndHex: "#E11D48", SelectionHex: "#F97316"},
		{ThemeID: "telefam_crimson", Title: "Telefam Ruby", PatternKind: "telefam", LightBgHex: "#FDF0F1", DarkBgHex: "#14090B", AccentStartHex: "#C80A0F", AccentEndHex: "#EF233C", SelectionHex: "#EF4444"},
	}
}

func chatBubbleColorCatalog() []ChatBubbleColorDTO {
	return []ChatBubbleColorDTO{
		{ColorID: "cyan", Label: "Lagoon Cyan", LightStartHex: "#0097A7", LightEndHex: "#00BCD4", DarkStartHex: "#00ACC1", DarkEndHex: "#26C6DA"},
		{ColorID: "crimson", Label: "Telefam Crimson", LightStartHex: "#C80A0F", LightEndHex: "#E11319", DarkStartHex: "#D91E2A", DarkEndHex: "#EF4444"},
		{ColorID: "emerald", Label: "Emerald Spring", LightStartHex: "#15803D", LightEndHex: "#22C55E", DarkStartHex: "#1B8E4B", DarkEndHex: "#2DB866"},
		{ColorID: "azure", Label: "Ocean Azure", LightStartHex: "#0284C7", LightEndHex: "#38BDF8", DarkStartHex: "#1D9BF0", DarkEndHex: "#0EA5E9"},
		{ColorID: "violet", Label: "Royal Violet", LightStartHex: "#6D28D9", LightEndHex: "#8B5CF6", DarkStartHex: "#7C3AED", DarkEndHex: "#A78BFA"},
		{ColorID: "amber", Label: "Sunset Amber", LightStartHex: "#C2410C", LightEndHex: "#F97316", DarkStartHex: "#D97706", DarkEndHex: "#F59E0B"},
	}
}

func normalizeChatAudience4(raw, fallback string) string {
	switch strings.ToLower(strings.TrimSpace(raw)) {
	case "anyone", "everyone":
		return "anyone"
	case "friends":
		return "friends"
	case "contacts", "contacts_only":
		return "contacts"
	case "nobody", "no_one", "only_me":
		return "nobody"
	default:
		return fallback
	}
}

func normalizeChatAudience3(raw, fallback string) string {
	switch strings.ToLower(strings.TrimSpace(raw)) {
	case "anyone", "everyone":
		return "anyone"
	case "friends":
		return "friends"
	case "nobody", "no_one", "only_me":
		return "nobody"
	default:
		return fallback
	}
}

func normalizeChatThemeID(raw, fallback string) string {
	clean := strings.ToLower(strings.TrimSpace(raw))
	for _, t := range chatThemeCatalog() {
		if t.ThemeID == clean {
			return t.ThemeID
		}
	}
	return fallback
}

func normalizeBubbleColorID(raw, fallback string) string {
	clean := strings.ToLower(strings.TrimSpace(raw))
	for _, b := range chatBubbleColorCatalog() {
		if b.ColorID == clean {
			return b.ColorID
		}
	}
	return fallback
}

func (h *Handler) getUserChatPrivacyLocked(userID string) ChatPrivacySettingsDTO {
	if p, ok := h.chatPrivacySettings[userID]; ok {
		return p
	}
	return defaultChatPrivacySettings()
}

func (h *Handler) handleGetChatPrivacy(w http.ResponseWriter, r *http.Request) {
	claims, _, _, ok := h.requireAuth(w, r)
	if !ok {
		return
	}
	h.mu.Lock()
	cp := h.getUserChatPrivacyLocked(claims.UserID)
	h.mu.Unlock()

	writeJSON(w, http.StatusOK, map[string]any{
		"success":         true,
		"chat_privacy":    cp,
		"themes":          chatThemeCatalog(),
		"bubble_colors":   chatBubbleColorCatalog(),
		"server_epoch_ms": time.Now().UnixMilli(),
	})
}

func (h *Handler) handleUpdateChatPrivacy(w http.ResponseWriter, r *http.Request) {
	claims, deviceFP, ipFP, ok := h.requireAuth(w, r)
	if !ok {
		return
	}
	var payload ChatPrivacySettingsDTO
	if err := json.NewDecoder(http.MaxBytesReader(w, r.Body, 8192)).Decode(&payload); err != nil {
		sanitizer.WriteSanitizedError(w, http.StatusBadRequest, "decode_chat_privacy", err, sanitizer.GenericErrorMessage)
		return
	}

	nowMs := time.Now().UnixMilli()
	h.mu.Lock()
	existing := h.getUserChatPrivacyLocked(claims.UserID)
	cleaned := ChatPrivacySettingsDTO{
		MessageRequests:     normalizeChatAudience4(payload.MessageRequests, existing.MessageRequests),
		WhoCanCall:          normalizeChatAudience4(payload.WhoCanCall, existing.WhoCanCall),
		WhoCanScreenshot:    normalizeChatAudience3(payload.WhoCanScreenshot, existing.WhoCanScreenshot),
		WhoCanShareChats:    normalizeChatAudience3(payload.WhoCanShareChats, existing.WhoCanShareChats),
		WhoCanCopyMessages:  normalizeChatAudience3(payload.WhoCanCopyMessages, existing.WhoCanCopyMessages),
		WhoCanDownloadMedia: normalizeChatAudience3(payload.WhoCanDownloadMedia, existing.WhoCanDownloadMedia),
		ChatThemeID:         normalizeChatThemeID(payload.ChatThemeID, existing.ChatThemeID),
		BubbleColorID:       normalizeBubbleColorID(payload.BubbleColorID, existing.BubbleColorID),
		ReadReceipts:        normalizeChatAudience3(payload.ReadReceipts, existing.ReadReceipts),
		TypingIndicators:    normalizeChatAudience3(payload.TypingIndicators, existing.TypingIndicators),
		OnlineIndicators:    normalizeChatAudience3(payload.OnlineIndicators, existing.OnlineIndicators),
		UpdatedAtMs:         nowMs,
	}
	h.chatPrivacySettings[claims.UserID] = cleaned
	h.mu.Unlock()

	h.audit.Record(claims.UserID, "chat_privacy_updated", deviceFP, ipFP, map[string]string{
		"theme_id":     cleaned.ChatThemeID,
		"bubble_color": cleaned.BubbleColorID,
	})

	writeJSON(w, http.StatusOK, map[string]any{
		"success":         true,
		"chat_privacy":    cleaned,
		"themes":          chatThemeCatalog(),
		"bubble_colors":   chatBubbleColorCatalog(),
		"server_epoch_ms": nowMs,
	})
}

func (h *Handler) handleUpdateChatTheme(w http.ResponseWriter, r *http.Request) {
	claims, deviceFP, ipFP, ok := h.requireAuth(w, r)
	if !ok {
		return
	}
	var payload UpdateChatThemePayload
	if err := json.NewDecoder(http.MaxBytesReader(w, r.Body, 4096)).Decode(&payload); err != nil {
		sanitizer.WriteSanitizedError(w, http.StatusBadRequest, "decode_chat_theme", err, sanitizer.GenericErrorMessage)
		return
	}

	nowMs := time.Now().UnixMilli()
	h.mu.Lock()
	cur := h.getUserChatPrivacyLocked(claims.UserID)
	if strings.TrimSpace(payload.ChatThemeID) != "" {
		cur.ChatThemeID = normalizeChatThemeID(payload.ChatThemeID, cur.ChatThemeID)
	}
	if strings.TrimSpace(payload.BubbleColorID) != "" {
		cur.BubbleColorID = normalizeBubbleColorID(payload.BubbleColorID, cur.BubbleColorID)
	}
	cur.UpdatedAtMs = nowMs
	h.chatPrivacySettings[claims.UserID] = cur
	h.mu.Unlock()

	h.audit.Record(claims.UserID, "chat_theme_updated", deviceFP, ipFP, map[string]string{
		"theme_id":     cur.ChatThemeID,
		"bubble_color": cur.BubbleColorID,
	})

	writeJSON(w, http.StatusOK, map[string]any{
		"success":         true,
		"chat_privacy":    cur,
		"server_epoch_ms": nowMs,
	})
}
