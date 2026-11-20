package auth

import (
	"bytes"
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"testing"
)

func TestOfflineSyncPreferencesAndManualSyncLifecycle(t *testing.T) {
	_, mux, tokens := newTestMuxAndTokens()

	userID := "usr_sync_1"
	deviceFP := "fp_device_sync_1"
	accessToken, _, _, err := tokens.IssueSessionTokens(userID, deviceFP)
	if err != nil {
		t.Fatalf("IssueSessionTokens failed: %v", err)
	}

	// Unauthenticated call must fail closed
	unauthReq := httptest.NewRequest(http.MethodPost, "/v1/sync/now", bytes.NewReader([]byte(`{}`)))
	unauthRec := httptest.NewRecorder()
	mux.ServeHTTP(unauthRec, unauthReq)
	if unauthRec.Code != http.StatusUnauthorized {
		t.Fatalf("expected 401 for unauthenticated /v1/sync/now, got %d", unauthRec.Code)
	}

	// Update sync preferences
	updateBody := map[string]any{
		"auto_sync_enabled":        true,
		"sync_over_wifi_only":      true,
		"background_sync_enabled":  true,
		"cache_feeds_offline":      false,
		"cache_chat_media_offline": true,
	}
	updateBytes, _ := json.Marshal(updateBody)
	putReq := httptest.NewRequest(http.MethodPut, "/v1/settings/sync", bytes.NewReader(updateBytes))
	putReq.Header.Set("Authorization", "Bearer "+accessToken)
	putReq.Header.Set("X-Telefam-Device-FP", deviceFP)
	putRec := httptest.NewRecorder()
	mux.ServeHTTP(putRec, putReq)
	if putRec.Code != http.StatusOK {
		t.Fatalf("expected 200 on PUT /v1/settings/sync, got %d (%s)", putRec.Code, putRec.Body.String())
	}

	// Run /v1/sync/now with offline chat privacy + operations payload
	syncPayload := map[string]any{
		"client_sync_id": "sync_batch_42",
		"operations": []map[string]any{
			{"client_op_id": "op_1", "room_id": "c1", "kind": "msg", "payload": "{}", "created_at": 1000},
			{"client_op_id": "op_2", "room_id": "c2", "kind": "msg", "payload": "{}", "created_at": 1001},
		},
		"chat_privacy": map[string]any{
			"chat_theme_id":   "cosmic_nebula",
			"bubble_color_id": "emerald",
			"read_receipts":   "friends",
		},
	}
	syncBytes, _ := json.Marshal(syncPayload)
	syncReq := httptest.NewRequest(http.MethodPost, "/v1/sync/now", bytes.NewReader(syncBytes))
	syncReq.Header.Set("Authorization", "Bearer "+accessToken)
	syncReq.Header.Set("X-Telefam-Device-FP", deviceFP)
	syncRec := httptest.NewRecorder()
	mux.ServeHTTP(syncRec, syncReq)
	if syncRec.Code != http.StatusOK {
		t.Fatalf("expected 200 on /v1/sync/now, got %d (%s)", syncRec.Code, syncRec.Body.String())
	}

	var resp struct {
		Success         bool                   `json:"success"`
		SyncedOpsCount  int                    `json:"synced_ops_count"`
		AckedOpIDs      []string               `json:"acked_op_ids"`
		SyncPreferences SyncPreferencesDTO     `json:"sync_preferences"`
		ChatPrivacy     ChatPrivacySettingsDTO `json:"chat_privacy"`
	}
	if err := json.Unmarshal(syncRec.Body.Bytes(), &resp); err != nil {
		t.Fatalf("decode /v1/sync/now resp failed: %v", err)
	}
	if !resp.Success || len(resp.AckedOpIDs) != 2 || resp.SyncedOpsCount != 3 {
		t.Fatalf("unexpected sync ack counts: %+v", resp)
	}
	if resp.SyncPreferences.LastSyncStatus != "synced" || resp.SyncPreferences.LastSyncedAtMs <= 0 || !resp.SyncPreferences.SyncOverWifiOnly {
		t.Fatalf("expected updated sync_preferences with status synced, got %+v", resp.SyncPreferences)
	}
	if resp.ChatPrivacy.ChatThemeID != "cosmic_nebula" || resp.ChatPrivacy.BubbleColorID != "emerald" {
		t.Fatalf("expected reconciled chat_privacy theme/bubble, got %+v", resp.ChatPrivacy)
	}
}
