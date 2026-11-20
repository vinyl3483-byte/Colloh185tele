package auth

import (
	"bytes"
	"encoding/base64"
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"testing"
	"time"

	"telefam.company/backend/internal/calls"
)

func TestP2pCallsPrivacyRestrictionAutoCallMeAndDuration(t *testing.T) {
	h, mux := setupTestAuthMux(t)
	aliceTok, aliceID := issueTestUserToken(t, h, "alice.call@example.com", "alicecall", "Alice Call", "dev_alice_call")
	bobTok, bobID := issueTestUserToken(t, h, "bob.call@example.com", "bobcall", "Bob Call", "dev_bob_call")

	// 1. Bob sets who_can_call = "no_one" -> Alice receives calls_not_allowed + auto call_request in chat
	privBody, _ := json.Marshal(map[string]any{
		"message_requests":       "anyone",
		"who_can_call":           "no_one",
		"who_can_screenshot":     "anyone",
		"who_can_share_chats":    "anyone",
		"who_can_copy_messages":  "anyone",
		"who_can_download_media": "anyone",
		"chat_theme_id":          "default",
		"bubble_color_id":        "crimson",
		"read_receipts":          "anyone",
		"typing_indicators":      "anyone",
		"online_indicators":      "anyone",
	})
	rec := httptest.NewRecorder()
	req := httptest.NewRequest(http.MethodPut, "/v1/chat/privacy", bytes.NewReader(privBody))
	req.Header.Set("Authorization", "Bearer "+bobTok)
	req.Header.Set("X-Telefam-Device-FP", "dev_bob_call")
	mux.ServeHTTP(rec, req)
	if rec.Code != http.StatusOK {
		t.Fatalf("expected 200 setting chat privacy, got %d: %s", rec.Code, rec.Body.String())
	}

	startBody, _ := json.Marshal(calls.StartCallPayload{
		PeerUserID: bobID,
		CallType:   "video",
	})
	rec = httptest.NewRecorder()
	req = httptest.NewRequest(http.MethodPost, "/v1/calls", bytes.NewReader(startBody))
	req.Header.Set("Authorization", "Bearer "+aliceTok)
	req.Header.Set("X-Telefam-Device-FP", "dev_alice_call")
	mux.ServeHTTP(rec, req)
	if rec.Code != http.StatusOK {
		t.Fatalf("expected 200 on restricted start call, got %d: %s", rec.Code, rec.Body.String())
	}
	var restrictedResp calls.StartCallResponse
	if err := json.Unmarshal(rec.Body.Bytes(), &restrictedResp); err != nil {
		t.Fatal(err)
	}
	if restrictedResp.Allowed || restrictedResp.Code != "calls_not_allowed" || !restrictedResp.AutoRequestSent {
		t.Fatalf("unexpected restricted response: %+v", restrictedResp)
	}

	// Verify call_log + call_request messages were inserted inside the direct conversation
	convID := restrictedResp.Call.ConversationID
	rec = httptest.NewRecorder()
	req = httptest.NewRequest(http.MethodGet, "/v1/chat/conversations/"+convID+"/messages", nil)
	req.Header.Set("Authorization", "Bearer "+bobTok)
	req.Header.Set("X-Telefam-Device-FP", "dev_bob_call")
	mux.ServeHTTP(rec, req)
	if rec.Code != http.StatusOK {
		t.Fatalf("expected 200 listing messages, got %d", rec.Code)
	}
	var msgResp struct {
		Messages []struct {
			MessageType      string `json:"message_type"`
			EncryptionScheme string `json:"encryption_scheme"`
			CiphertextB64    string `json:"ciphertext_b64"`
		} `json:"messages"`
	}
	if err := json.Unmarshal(rec.Body.Bytes(), &msgResp); err != nil {
		t.Fatal(err)
	}
	if len(msgResp.Messages) < 2 {
		t.Fatalf("expected call_log and call_request messages in chat, got %d", len(msgResp.Messages))
	}
	foundRequest := false
	for _, m := range msgResp.Messages {
		if m.MessageType == "call_request" && m.EncryptionScheme == "telefam_call_meta_v1" {
			raw, _ := base64.StdEncoding.DecodeString(m.CiphertextB64)
			if len(raw) > 0 {
				foundRequest = true
			}
		}
	}
	if !foundRequest {
		t.Fatalf("expected auto-sent call_request message inside 1-on-1 chat")
	}

	// 2. Bob allows calls; Alice starts audio call, Bob accepts, switches to video, ends -> backend stores duration & logs chat entry
	privBody2, _ := json.Marshal(map[string]any{
		"message_requests":       "anyone",
		"who_can_call":           "anyone",
		"who_can_screenshot":     "anyone",
		"who_can_share_chats":    "anyone",
		"who_can_copy_messages":  "anyone",
		"who_can_download_media": "anyone",
		"chat_theme_id":          "default",
		"bubble_color_id":        "crimson",
		"read_receipts":          "anyone",
		"typing_indicators":      "anyone",
		"online_indicators":      "anyone",
	})
	rec = httptest.NewRecorder()
	req = httptest.NewRequest(http.MethodPut, "/v1/chat/privacy", bytes.NewReader(privBody2))
	req.Header.Set("Authorization", "Bearer "+bobTok)
	req.Header.Set("X-Telefam-Device-FP", "dev_bob_call")
	mux.ServeHTTP(rec, req)

	audioStart, _ := json.Marshal(calls.StartCallPayload{
		PeerUserID: bobID,
		CallType:   "audio",
		SDPOffer:   "v=0\r\no=- 1 1 IN IP4 127.0.0.1\r\n",
	})
	rec = httptest.NewRecorder()
	req = httptest.NewRequest(http.MethodPost, "/v1/calls", bytes.NewReader(audioStart))
	req.Header.Set("Authorization", "Bearer "+aliceTok)
	req.Header.Set("X-Telefam-Device-FP", "dev_alice_call")
	mux.ServeHTTP(rec, req)
	var startOK calls.StartCallResponse
	_ = json.Unmarshal(rec.Body.Bytes(), &startOK)
	if !startOK.Allowed || startOK.Call.CallerUserID != aliceID {
		t.Fatalf("expected allowed audio call start, got %+v", startOK)
	}
	callID := startOK.Call.CallID

	acceptBody, _ := json.Marshal(calls.CallSignalPayload{
		Action: "accept",
		SDP:    "v=0\r\no=- 2 2 IN IP4 127.0.0.1\r\n",
	})
	rec = httptest.NewRecorder()
	req = httptest.NewRequest(http.MethodPost, "/v1/calls/"+callID+"/signal", bytes.NewReader(acceptBody))
	req.Header.Set("Authorization", "Bearer "+bobTok)
	req.Header.Set("X-Telefam-Device-FP", "dev_bob_call")
	mux.ServeHTTP(rec, req)
	if rec.Code != http.StatusOK {
		t.Fatalf("accept failed: %d %s", rec.Code, rec.Body.String())
	}

	switchBody, _ := json.Marshal(calls.CallSignalPayload{Action: "switch_video_accept"})
	rec = httptest.NewRecorder()
	req = httptest.NewRequest(http.MethodPost, "/v1/calls/"+callID+"/signal", bytes.NewReader(switchBody))
	req.Header.Set("Authorization", "Bearer "+bobTok)
	req.Header.Set("X-Telefam-Device-FP", "dev_bob_call")
	mux.ServeHTTP(rec, req)

	time.Sleep(250 * time.Millisecond)

	endBody, _ := json.Marshal(calls.CallSignalPayload{Action: "end"})
	rec = httptest.NewRecorder()
	req = httptest.NewRequest(http.MethodPost, "/v1/calls/"+callID+"/signal", bytes.NewReader(endBody))
	req.Header.Set("Authorization", "Bearer "+aliceTok)
	req.Header.Set("X-Telefam-Device-FP", "dev_alice_call")
	mux.ServeHTTP(rec, req)
	var endResp struct {
		Call calls.CallRecordDTO `json:"call"`
	}
	_ = json.Unmarshal(rec.Body.Bytes(), &endResp)
	if endResp.Call.State != "ended" || !endResp.Call.VideoUpgraded || endResp.Call.DurationSec < 1 {
		t.Fatalf("expected connected ended call with video_upgraded and backend duration >= 1s, got %+v", endResp.Call)
	}
}

func TestP2pCall25SecondTimeoutExpiresAsMissed(t *testing.T) {
	h, mux := setupTestAuthMux(t)
	h.ensureCallsService().SetRingTimeoutForTest(60 * time.Millisecond)

	aliceTok, _ := issueTestUserToken(t, h, "alice.timeout@example.com", "alicetime", "Alice Time", "dev_alice_to")
	_, bobID := issueTestUserToken(t, h, "bob.timeout@example.com", "bobtime", "Bob Time", "dev_bob_to")

	startBody, _ := json.Marshal(calls.StartCallPayload{
		PeerUserID: bobID,
		CallType:   "video",
	})
	rec := httptest.NewRecorder()
	req := httptest.NewRequest(http.MethodPost, "/v1/calls", bytes.NewReader(startBody))
	req.Header.Set("Authorization", "Bearer "+aliceTok)
	req.Header.Set("X-Telefam-Device-FP", "dev_alice_to")
	mux.ServeHTTP(rec, req)
	var resp calls.StartCallResponse
	_ = json.Unmarshal(rec.Body.Bytes(), &resp)

	time.Sleep(150 * time.Millisecond)

	rec = httptest.NewRecorder()
	req = httptest.NewRequest(http.MethodGet, "/v1/calls/"+resp.Call.CallID, nil)
	req.Header.Set("Authorization", "Bearer "+aliceTok)
	req.Header.Set("X-Telefam-Device-FP", "dev_alice_to")
	mux.ServeHTTP(rec, req)
	var stateResp struct {
		Call calls.CallRecordDTO `json:"call"`
	}
	_ = json.Unmarshal(rec.Body.Bytes(), &stateResp)
	if stateResp.Call.State != "missed" || stateResp.Call.EndReason != "timeout_25s" {
		t.Fatalf("expected missed call with timeout_25s, got %+v", stateResp.Call)
	}
}
