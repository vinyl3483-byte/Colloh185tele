package auth

import (
	"bytes"
	"crypto/rand"
	"crypto/rsa"
	"crypto/x509"
	"encoding/json"
	"encoding/pem"
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"
	"time"

	"telefam.company/backend/internal/audit"
	"telefam.company/backend/internal/chat"
	"telefam.company/backend/internal/contacts"
	"telefam.company/backend/internal/fingerprint"
	"telefam.company/backend/internal/notifications"
	"telefam.company/backend/internal/posts"
	"telefam.company/backend/internal/ratelimit"
)

func newTestAuthHandlerForNotifications(t *testing.T) *Handler {
	t.Helper()
	fp := fingerprint.NewService("test-fp-secret")
	limiter := ratelimit.NewLimiter()
	otp := NewOtpService(time.Minute, 5)
	tokens := NewTokenService("test-jwt-secret", 15*time.Minute, 24*time.Hour)
	auditLogger := audit.NewLogger()
	contactsSvc := contacts.NewService()
	return NewHandler(fp, limiter, otp, tokens, auditLogger, nil, contactsSvc)
}

func registerTestUserWithProfile(t *testing.T, h *Handler, userID, email, deviceFP, fullName, username string) {
	t.Helper()
	h.mu.Lock()
	h.emailToUserID[email] = userID
	h.userEmails[userID] = email
	h.profiles[userID] = storedProfile{
		UserID:   userID,
		FullName: fullName,
		Username: username,
	}
	h.usernames[strings.ToLower(username)] = userID
	h.recordTrustedDeviceAndEmailLocked(userID, email, deviceFP, nil)
	h.mu.Unlock()
}

func makeBearerHeaders(t *testing.T, h *Handler, userID, email, deviceFP string) http.Header {
	t.Helper()
	pair, _, err := h.tokens.IssueInitialPair(userID, email, deviceFP)
	if err != nil {
		t.Fatalf("IssueInitialPair: %v", err)
	}
	hdr := make(http.Header)
	hdr.Set("Authorization", "Bearer "+pair.AccessToken)
	hdr.Set("X-Telefam-Device-FP", deviceFP)
	hdr.Set("Content-Type", "application/json")
	return hdr
}

func TestNotificationsFCMDispatchViewOnceFollowLikeMentionAndLogout(t *testing.T) {
	h := newTestAuthHandlerForNotifications(t)
	mux := http.NewServeMux()
	h.RegisterRoutes(mux)

	const (
		aliceID     = "usr_alice"
		aliceEmail  = "alice@telefam.test"
		aliceDevice = "fp_alice_android"
		bobID       = "usr_bob"
		bobEmail    = "bob@telefam.test"
		bobDevice   = "fp_bob_android"
	)
	registerTestUserWithProfile(t, h, aliceID, aliceEmail, aliceDevice, "Alice Kim", "alicekim")
	registerTestUserWithProfile(t, h, bobID, bobEmail, bobDevice, "Bob Ochieng", "bobochieng")

	// Mock Google OAuth2 + FCM HTTP v1 server
	var fcmRequests []map[string]any
	mockFCMServer := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		switch {
		case r.URL.Path == "/token":
			w.Header().Set("Content-Type", "application/json")
			_ = json.NewEncoder(w).Encode(map[string]any{
				"access_token": "mock_fcm_oauth_token",
				"expires_in":   3600,
			})
		case strings.HasSuffix(r.URL.Path, "/messages:send"):
			var body map[string]any
			_ = json.NewDecoder(r.Body).Decode(&body)
			fcmRequests = append(fcmRequests, body)
			w.Header().Set("Content-Type", "application/json")
			_ = json.NewEncoder(w).Encode(map[string]string{"name": "projects/p/messages/1"})
		default:
			w.WriteHeader(http.StatusNotFound)
		}
	}))
	defer mockFCMServer.Close()

	privKey, err := rsa.GenerateKey(rand.Reader, 2048)
	if err != nil {
		t.Fatalf("rsa.GenerateKey: %v", err)
	}
	der, err := x509.MarshalPKCS8PrivateKey(privKey)
	if err != nil {
		t.Fatalf("MarshalPKCS8PrivateKey: %v", err)
	}
	pemStr := string(pem.EncodeToMemory(&pem.Block{Type: "PRIVATE KEY", Bytes: der}))

	notifSvc := h.ensureNotificationsService()
	if err := notifSvc.ConfigureFCMForTest(
		"telefam-prod",
		"fcm-sender@telefam-prod.iam.gserviceaccount.com",
		pemStr,
		mockFCMServer.URL+"/token",
		mockFCMServer.URL,
		mockFCMServer.Client(),
	); err != nil {
		t.Fatalf("ConfigureFCMForTest: %v", err)
	}

	aliceHdr := makeBearerHeaders(t, h, aliceID, aliceEmail, aliceDevice)
	bobHdr := makeBearerHeaders(t, h, bobID, bobEmail, bobDevice)

	// 1. Register FCM token for Bob
	regReq := httptest.NewRequest(http.MethodPost, "/v1/notifications/push-token", strings.NewReader(`{"token":"fcm_bob_tok_1","platform":"android"}`))
	regReq.Header = bobHdr.Clone()
	regRec := httptest.NewRecorder()
	mux.ServeHTTP(regRec, regReq)
	if regRec.Code != http.StatusOK {
		t.Fatalf("register push token failed: %d %s", regRec.Code, regRec.Body.String())
	}

	// 2. Alice follows Bob -> triggers inbox + FCM follow push
	followReq := httptest.NewRequest(http.MethodPost, "/v1/contacts/follow", strings.NewReader(`{"target_user_id":"usr_bob"}`))
	followReq.Header = aliceHdr.Clone()
	followRec := httptest.NewRecorder()
	mux.ServeHTTP(followRec, followReq)
	if followRec.Code != http.StatusOK {
		t.Fatalf("follow failed: %d %s", followRec.Code, followRec.Body.String())
	}

	// 3. Bob creates a post mentioning @alicekim -> Alice has no token yet, inbox records mention;
	// then Alice creates a post mentioning @bobochieng -> triggers FCM mention push to Bob.
	postReq := httptest.NewRequest(http.MethodPost, "/v1/posts", strings.NewReader(`{"client_op_id":"op_p1","mode":"text","privacy":"public","text_content":"Hello @bobochieng check this out!"}`))
	postReq.Header = aliceHdr.Clone()
	postRec := httptest.NewRecorder()
	mux.ServeHTTP(postRec, postReq)
	if postRec.Code != http.StatusOK {
		t.Fatalf("create post failed: %d %s", postRec.Code, postRec.Body.String())
	}
	var postResp struct {
		Post posts.StoredPost `json:"post"`
	}
	_ = json.Unmarshal(postRec.Body.Bytes(), &postResp)

	// 4. Bob likes Alice's post? Let's have Alice register token and Bob like Alice's post -> follow/like/mention all tested!
	regAliceReq := httptest.NewRequest(http.MethodPost, "/v1/notifications/push-token", strings.NewReader(`{"token":"fcm_alice_tok_1","platform":"android"}`))
	regAliceReq.Header = aliceHdr.Clone()
	mux.ServeHTTP(httptest.NewRecorder(), regAliceReq)

	likeReq := httptest.NewRequest(http.MethodPost, "/v1/feeds/interact", bytes.NewBufferString(`{"post_id":"`+postResp.Post.PostID+`","action":"like"}`))
	likeReq.Header = bobHdr.Clone()
	likeRec := httptest.NewRecorder()
	mux.ServeHTTP(likeRec, likeReq)
	if likeRec.Code != http.StatusOK {
		t.Fatalf("like failed: %d %s", likeRec.Code, likeRec.Body.String())
	}

	// 5. Alice sends Bob an E2EE text chat message & an E2EE view-once voice note
	chatSvc := h.ensureChatService()
	conv, err := chatSvc.GetOrCreateDirectConversation(aliceID, bobID)
	if err != nil {
		t.Fatalf("GetOrCreateDirectConversation: %v", err)
	}
	_, err = chatSvc.SendMessage(aliceID, conv.ConversationID, chat.SendMessagePayload{
		ClientOpID:          "op_m1",
		MessageType:         "text",
		CompressionCodec:    "zlib",
		EncryptionScheme:    "libsignal_v1",
		CiphertextB64:       "c2lnbmFsX2NpcGhlcnRleHQ=",
		SenderCiphertextB64: "c2VsZl9jaXBoZXJ0ZXh0",
		SignalMsgType:       3,
	})
	if err != nil {
		t.Fatalf("SendMessage text: %v", err)
	}

	voMsg, err := chatSvc.SendMessage(aliceID, conv.ConversationID, chat.SendMessagePayload{
		ClientOpID:          "op_m2",
		MessageType:         "voice_once",
		CompressionCodec:    "zlib",
		EncryptionScheme:    "libsignal_v1",
		CiphertextB64:       "c2VjcmV0X3ZvaWNlX29uY2U=",
		SenderCiphertextB64: "c2VsZl92bw==",
		SignalMsgType:       3,
		VoiceDurationMs:     4500,
		ViewOnceTTLSec:      10,
	})
	if err != nil {
		t.Fatalf("SendMessage voice_once: %v", err)
	}

	// Verify FCM payloads delivered: follow, mention, like, text chat_message, voice_once chat_message
	if len(fcmRequests) != 5 {
		t.Fatalf("expected 5 fcm requests, got %d", len(fcmRequests))
	}
	voPayload := fcmRequests[4]["message"].(map[string]any)["data"].(map[string]any)
	if voPayload["is_view_once"] != "true" {
		t.Fatalf("expected voice_once FCM data is_view_once=true, got %v", voPayload["is_view_once"])
	}
	if voPayload["ciphertext_b64"] != "" {
		t.Fatalf("voice_once FCM payload must mask ciphertext_b64, got %v", voPayload["ciphertext_b64"])
	}

	// 6. Opening view-once sends cancel notification to recipient device
	if _, _, err := chatSvc.OpenViewOnceMessage(bobID, voMsg.MessageID); err != nil {
		t.Fatalf("OpenViewOnceMessage: %v", err)
	}
	if len(fcmRequests) != 6 {
		t.Fatalf("expected 6th fcm cancel request after view_once open, got %d", len(fcmRequests))
	}
	cancelData := fcmRequests[5]["message"].(map[string]any)["data"].(map[string]any)
	if cancelData["type"] != "message_cancel" || cancelData["message_id"] != voMsg.MessageID {
		t.Fatalf("unexpected cancel payload: %+v", cancelData)
	}

	// 7. Bob lists notifications & marks all read
	listReq := httptest.NewRequest(http.MethodGet, "/v1/notifications", nil)
	listReq.Header = bobHdr.Clone()
	listRec := httptest.NewRecorder()
	mux.ServeHTTP(listRec, listReq)
	if listRec.Code != http.StatusOK {
		t.Fatalf("list notifications failed: %d", listRec.Code)
	}
	var listResp struct {
		UnreadCount int `json:"unread_count"`
		Items       []notifications.NotificationDTO `json:"items"`
	}
	_ = json.Unmarshal(listRec.Body.Bytes(), &listResp)
	if listResp.UnreadCount != 2 || len(listResp.Items) != 2 {
		t.Fatalf("expected 2 unread social notifications for bob, got unread=%d len=%d", listResp.UnreadCount, len(listResp.Items))
	}

	// 8. Logout revokes Bob's push token so no more pushes reach the logged-out device
	logoutReq := httptest.NewRequest(http.MethodPost, "/v1/auth/logout", strings.NewReader(`{"all_devices":false}`))
	logoutReq.Header = bobHdr.Clone()
	logoutRec := httptest.NewRecorder()
	mux.ServeHTTP(logoutRec, logoutReq)
	if logoutRec.Code != http.StatusOK {
		t.Fatalf("logout failed: %d %s", logoutRec.Code, logoutRec.Body.String())
	}
	if gotTokens := notifSvc.ListUserTokens(bobID); len(gotTokens) != 0 {
		t.Fatalf("expected 0 push tokens after logout, got %d", len(gotTokens))
	}
}

func TestExtractMentionUsernamesIgnoresEmails(t *testing.T) {
	mentions := notifications.ExtractMentionUsernames("Contact bob@example.com or ping @alice.k and @charlie_99 and @alice.k")
	if len(mentions) != 2 || mentions[0] != "alice.k" || mentions[1] != "charlie_99" {
		t.Fatalf("unexpected mentions parsed: %#v", mentions)
	}
}
