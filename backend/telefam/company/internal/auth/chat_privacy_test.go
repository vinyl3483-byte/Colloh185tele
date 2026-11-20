package auth

import (
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"testing"
)

func TestChatPrivacyThemesAndBubbleColorsEndpoints(t *testing.T) {
	_, mux, tokens := newTestMuxAndTokens()
	dfp := "chat-privacy-device-fp"
	pair, _, err := tokens.IssueInitialPair("u_chat_1", "chat1@telefam.company", dfp)
	if err != nil {
		t.Fatalf("IssueInitialPair failed: %v", err)
	}

	// Unauthenticated request rejected
	unauth := httptest.NewRecorder()
	mux.ServeHTTP(unauth, httptest.NewRequest("GET", "/v1/chat/privacy", nil))
	if unauth.Code != http.StatusUnauthorized {
		t.Fatalf("expected 401 unauthenticated, got %d", unauth.Code)
	}

	// Default fetch returns defaults + full theme catalog + 6 bubble colors
	w := httptest.NewRecorder()
	mux.ServeHTTP(w, authReq("GET", "/v1/chat/privacy", nil, pair.AccessToken, dfp))
	if w.Code != http.StatusOK {
		t.Fatalf("GET /v1/chat/privacy failed: %d %s", w.Code, w.Body.String())
	}
	var initial struct {
		Success      bool                     `json:"success"`
		ChatPrivacy  ChatPrivacySettingsDTO   `json:"chat_privacy"`
		Themes       []ChatThemeCatalogItemDTO `json:"themes"`
		BubbleColors []ChatBubbleColorDTO     `json:"bubble_colors"`
	}
	if err := json.Unmarshal(w.Body.Bytes(), &initial); err != nil {
		t.Fatalf("unmarshal initial failed: %v", err)
	}
	if !initial.Success || initial.ChatPrivacy.MessageRequests != "anyone" || initial.ChatPrivacy.WhoCanScreenshot != "nobody" {
		t.Fatalf("unexpected defaults: %+v", initial.ChatPrivacy)
	}
	if len(initial.Themes) < 13 || len(initial.BubbleColors) != 6 {
		t.Fatalf("expected >=13 theme options and 6 bubble colors, got themes=%d bubbles=%d", len(initial.Themes), len(initial.BubbleColors))
	}

	// Full 11-field privacy update persists across subsequent GET /v1/chat/privacy & GET /v1/settings
	upd := httptest.NewRecorder()
	mux.ServeHTTP(upd, authReq("PUT", "/v1/chat/privacy", ChatPrivacySettingsDTO{
		MessageRequests:     "friends",
		WhoCanCall:          "contacts",
		WhoCanScreenshot:    "friends",
		WhoCanShareChats:    "nobody",
		WhoCanCopyMessages:  "friends",
		WhoCanDownloadMedia: "nobody",
		ChatThemeID:         "spring_meadow",
		BubbleColorID:       "crimson",
		ReadReceipts:        "friends",
		TypingIndicators:    "anyone",
		OnlineIndicators:    "friends",
	}, pair.AccessToken, dfp))
	if upd.Code != http.StatusOK {
		t.Fatalf("PUT /v1/chat/privacy failed: %d %s", upd.Code, upd.Body.String())
	}

	// Quick theme & bubble color update validates inputs and rejects unknown IDs safely
	themeUpd := httptest.NewRecorder()
	mux.ServeHTTP(themeUpd, authReq("PUT", "/v1/chat/theme", UpdateChatThemePayload{
		ChatThemeID:   "academia_math",
		BubbleColorID: "emerald",
	}, pair.AccessToken, dfp))
	if themeUpd.Code != http.StatusOK {
		t.Fatalf("PUT /v1/chat/theme failed: %d %s", themeUpd.Code, themeUpd.Body.String())
	}

	bundleRec := httptest.NewRecorder()
	mux.ServeHTTP(bundleRec, authReq("GET", "/v1/settings", nil, pair.AccessToken, dfp))
	if bundleRec.Code != http.StatusOK {
		t.Fatalf("GET /v1/settings failed: %d", bundleRec.Code)
	}
	var bundle struct {
		ChatPrivacy ChatPrivacySettingsDTO `json:"chat_privacy"`
	}
	if err := json.Unmarshal(bundleRec.Body.Bytes(), &bundle); err != nil {
		t.Fatalf("unmarshal settings bundle failed: %v", err)
	}
	if bundle.ChatPrivacy.ChatThemeID != "academia_math" || bundle.ChatPrivacy.BubbleColorID != "emerald" || bundle.ChatPrivacy.WhoCanCall != "contacts" {
		t.Fatalf("unexpected persisted chat_privacy in settings bundle: %+v", bundle.ChatPrivacy)
	}
}
