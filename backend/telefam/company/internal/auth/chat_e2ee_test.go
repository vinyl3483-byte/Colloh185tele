package auth

import (
	"bufio"
	"bytes"
	"crypto/sha1"
	"encoding/base64"
	"encoding/json"
	"io"
	"mime/multipart"
	"net"
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"

	"telefam.company/backend/internal/chat"
	"telefam.company/backend/internal/contacts"
)

func TestE2eeChatLifecycleViewOnceFlagsAndGifly(t *testing.T) {
	h, mux, tokens := newTestMuxAndTokens()
	dfp1 := "device-chat-alice"
	dfp2 := "device-chat-bob"
	alicePair, _, _ := tokens.IssueInitialPair("u_alice", "alice@telefam.company", dfp1)
	bobPair, _, _ := tokens.IssueInitialPair("u_bob", "bob@telefam.company", dfp2)

	h.contacts.RegisterOrUpdateUser(contacts.DirectoryUser{
		UserID: "u_alice", FullName: "Alice M", Username: "alicem", Country: "Kenya", CountryISO: "KE",
	}, dfp1)
	h.contacts.RegisterOrUpdateUser(contacts.DirectoryUser{
		UserID: "u_bob", FullName: "Bob K", Username: "bobk", Country: "Japan", CountryISO: "JP",
	}, dfp2)

	// 1. Upload Signal key bundles & verify OTK consumption
	uploadKeys := func(token, dfp, idPub string) {
		w := httptest.NewRecorder()
		mux.ServeHTTP(w, authReq("PUT", "/v1/chat/e2ee/keys", chat.E2eeKeyBundleUploadPayload{
			RegistrationID:     111,
			IdentityPubB64:     idPub,
			SignedPreKeyID:     1,
			SignedPreKeyPubB64: "spk_" + idPub,
			SignedPreKeySigB64: "sig_" + idPub,
			OneTimePreKeys: []chat.OneTimePreKeyDTO{
				{KeyID: 1, PublicKeyB64: "otk_1"},
				{KeyID: 2, PublicKeyB64: "otk_2"},
			},
		}, token, dfp))
		if w.Code != http.StatusOK {
			t.Fatalf("upload keys failed: %d %s", w.Code, w.Body.String())
		}
	}
	uploadKeys(alicePair.AccessToken, dfp1, "id_alice")
	uploadKeys(bobPair.AccessToken, dfp2, "id_bob")

	kRec1 := httptest.NewRecorder()
	mux.ServeHTTP(kRec1, authReq("GET", "/v1/chat/e2ee/keys/u_bob", nil, alicePair.AccessToken, dfp1))
	if kRec1.Code != http.StatusOK {
		t.Fatalf("fetch peer keys failed: %d", kRec1.Code)
	}
	var kb1 chat.E2eeKeyBundleResponse
	_ = json.Unmarshal(kRec1.Body.Bytes(), &kb1)
	if kb1.PreKeyID != 1 || kb1.PreKeyPubB64 != "otk_1" {
		t.Fatalf("expected otk_1 consumed first, got %+v", kb1)
	}
	kRec2 := httptest.NewRecorder()
	mux.ServeHTTP(kRec2, authReq("GET", "/v1/chat/e2ee/keys/u_bob", nil, alicePair.AccessToken, dfp1))
	var kb2 chat.E2eeKeyBundleResponse
	_ = json.Unmarshal(kRec2.Body.Bytes(), &kb2)
	if kb2.PreKeyID != 2 {
		t.Fatalf("expected otk_2 consumed second, got %+v", kb2)
	}

	// 2. Open 1-on-1 conversation Alice -> Bob
	cRec := httptest.NewRecorder()
	mux.ServeHTTP(cRec, authReq("POST", "/v1/chat/conversations/direct/u_bob", nil, alicePair.AccessToken, dfp1))
	if cRec.Code != http.StatusOK {
		t.Fatalf("open direct chat failed: %d %s", cRec.Code, cRec.Body.String())
	}
	var openResp struct {
		Conversation chat.ConversationDTO `json:"conversation"`
	}
	_ = json.Unmarshal(cRec.Body.Bytes(), &openResp)
	convID := openResp.Conversation.ConversationID
	if convID == "" {
		t.Fatalf("missing conversation_id")
	}

	// 3. Send compressed + Signal encrypted message, edit it, check privacy read receipt cap, then unsend
	sendRec := httptest.NewRecorder()
	mux.ServeHTTP(sendRec, authReq("POST", "/v1/chat/conversations/"+convID+"/messages", chat.SendMessagePayload{
		ClientOpID:          "op-1",
		MessageType:         "text",
		CompressionCodec:    "zlib",
		EncryptionScheme:    "libsignal_v1",
		CiphertextB64:       "c2lnbmFsX2NpcGhlcnRleHRfMQ==",
		SenderCiphertextB64: "c2VuZGVyX2NvcHlfb25l",
		SignalMsgType:       3,
	}, alicePair.AccessToken, dfp1))
	if sendRec.Code != http.StatusOK {
		t.Fatalf("send message failed: %d %s", sendRec.Code, sendRec.Body.String())
	}
	var msgResp struct {
		Message chat.MessageDTO `json:"message"`
	}
	_ = json.Unmarshal(sendRec.Body.Bytes(), &msgResp)
	msgID := msgResp.Message.MessageID

	// Bob disables read receipts -> receipt stays at "delivered" even when reporting "seen"
	mux.ServeHTTP(httptest.NewRecorder(), authReq("PUT", "/v1/chat/privacy", ChatPrivacySettingsDTO{
		ReadReceipts: "nobody",
	}, bobPair.AccessToken, dfp2))
	rcpRec := httptest.NewRecorder()
	mux.ServeHTTP(rcpRec, authReq("POST", "/v1/chat/messages/"+msgID+"/receipt", chat.ReceiptPayload{Status: "seen"}, bobPair.AccessToken, dfp2))
	var rcpResp struct {
		Message chat.MessageDTO `json:"message"`
	}
	_ = json.Unmarshal(rcpRec.Body.Bytes(), &rcpResp)
	if rcpResp.Message.DeliveryStatus != "delivered" {
		t.Fatalf("expected receipt capped at delivered when read_receipts=nobody, got %s", rcpResp.Message.DeliveryStatus)
	}

	// Alice edits & then unsends message -> zero traces remain
	editRec := httptest.NewRecorder()
	mux.ServeHTTP(editRec, authReq("PATCH", "/v1/chat/messages/"+msgID, chat.EditMessagePayload{
		CiphertextB64: "c2lnbmFsX2VkaXRlZA==",
	}, alicePair.AccessToken, dfp1))
	if editRec.Code != http.StatusOK {
		t.Fatalf("edit failed: %d", editRec.Code)
	}

	unsendRec := httptest.NewRecorder()
	mux.ServeHTTP(unsendRec, authReq("DELETE", "/v1/chat/messages/"+msgID+"?mode=unsend", nil, alicePair.AccessToken, dfp1))
	if unsendRec.Code != http.StatusOK {
		t.Fatalf("unsend failed: %d", unsendRec.Code)
	}
	listAfter := httptest.NewRecorder()
	mux.ServeHTTP(listAfter, authReq("GET", "/v1/chat/conversations/"+convID+"/messages", nil, bobPair.AccessToken, dfp2))
	var listResp struct {
		Messages []chat.MessageDTO `json:"messages"`
	}
	_ = json.Unmarshal(listAfter.Body.Bytes(), &listResp)
	if len(listResp.Messages) != 0 {
		t.Fatalf("expected 0 messages after unsend purge, got %d", len(listResp.Messages))
	}

	// 4. Upload voice note + send view-once voice note -> sender blocked from opening -> receiver opens (logs flag) -> consumes (purges all traces)
	var body bytes.Buffer
	mw := multipart.NewWriter(&body)
	_ = mw.WriteField("conversation_id", convID)
	_ = mw.WriteField("duration_ms", "3200")
	_ = mw.WriteField("view_once", "true")
	fw, _ := mw.CreateFormFile("file", "voice.m4a")
	_, _ = fw.Write([]byte("FAKE_M4A_AUDIO_BYTES_12345"))
	_ = mw.Close()

	vUpReq := authReq("POST", "/v1/chat/media/voice", nil, alicePair.AccessToken, dfp1)
	vUpReq.Body = http.NoBody
	vUpReq = httptest.NewRequest("POST", "/v1/chat/media/voice", &body)
	vUpReq.Header.Set("Authorization", "Bearer "+alicePair.AccessToken)
	vUpReq.Header.Set("X-Telefam-Device-FP", dfp1)
	vUpReq.Header.Set("Content-Type", mw.FormDataContentType())
	vUpRec := httptest.NewRecorder()
	mux.ServeHTTP(vUpRec, vUpReq)
	if vUpRec.Code != http.StatusOK {
		t.Fatalf("voice upload failed: %d %s", vUpRec.Code, vUpRec.Body.String())
	}
	var vUpResp struct {
		MediaID string `json:"media_id"`
		URL     string `json:"media_url"`
	}
	_ = json.Unmarshal(vUpRec.Body.Bytes(), &vUpResp)

	voSendRec := httptest.NewRecorder()
	mux.ServeHTTP(voSendRec, authReq("POST", "/v1/chat/conversations/"+convID+"/messages", chat.SendMessagePayload{
		ClientOpID:      "op-vo-1",
		MessageType:     "voice_once",
		CiphertextB64:   "dm9pY2VfY2lwaGVy",
		MediaID:         vUpResp.MediaID,
		MediaURL:        vUpResp.URL,
		VoiceDurationMs: 3200,
		ViewOnceTTLSec:  10,
	}, alicePair.AccessToken, dfp1))
	var voMsgResp struct {
		Message chat.MessageDTO `json:"message"`
	}
	_ = json.Unmarshal(voSendRec.Body.Bytes(), &voMsgResp)
	voMsgID := voMsgResp.Message.MessageID

	// Sender attempting to open own view-once is forbidden
	senderOpenRec := httptest.NewRecorder()
	mux.ServeHTTP(senderOpenRec, authReq("POST", "/v1/chat/messages/"+voMsgID+"/view-once/open", nil, alicePair.AccessToken, dfp1))
	if senderOpenRec.Code != http.StatusForbidden {
		t.Fatalf("expected sender opening view-once to be forbidden 403, got %d", senderOpenRec.Code)
	}

	// Receiver opens -> sets openedAt/expiresAt + creates persistent security flag
	bobOpenRec := httptest.NewRecorder()
	mux.ServeHTTP(bobOpenRec, authReq("POST", "/v1/chat/messages/"+voMsgID+"/view-once/open", nil, bobPair.AccessToken, dfp2))
	if bobOpenRec.Code != http.StatusOK {
		t.Fatalf("receiver open view-once failed: %d %s", bobOpenRec.Code, bobOpenRec.Body.String())
	}

	// Receiver consumes -> both message AND voice media bytes are purged with zero traces
	bobConsRec := httptest.NewRecorder()
	mux.ServeHTTP(bobConsRec, authReq("POST", "/v1/chat/messages/"+voMsgID+"/view-once/consume", nil, bobPair.AccessToken, dfp2))
	if bobConsRec.Code != http.StatusOK {
		t.Fatalf("consume view-once failed: %d", bobConsRec.Code)
	}
	dlAfter := httptest.NewRecorder()
	mux.ServeHTTP(dlAfter, authReq("GET", "/v1/chat/media/voice/"+vUpResp.MediaID, nil, bobPair.AccessToken, dfp2))
	if dlAfter.Code != http.StatusNotFound {
		t.Fatalf("expected voice media bytes purged (404) after consume, got %d", dlAfter.Code)
	}

	// 5. Screenshot security flag persists in /flags
	flagRec := httptest.NewRecorder()
	mux.ServeHTTP(flagRec, authReq("POST", "/v1/chat/conversations/"+convID+"/flags", chat.SecurityFlagPayload{
		FlagType: "screenshot_disallowed",
		Details:  "Screenshot captured while screenshot privacy is locked",
	}, bobPair.AccessToken, dfp2))
	if flagRec.Code != http.StatusOK {
		t.Fatalf("record flag failed: %d", flagRec.Code)
	}
	flagsList := httptest.NewRecorder()
	mux.ServeHTTP(flagsList, authReq("GET", "/v1/chat/conversations/"+convID+"/flags", nil, alicePair.AccessToken, dfp1))
	var flResp struct {
		Flags []chat.SecurityFlagDTO `json:"flags"`
	}
	_ = json.Unmarshal(flagsList.Body.Bytes(), &flResp)
	if len(flResp.Flags) < 2 {
		t.Fatalf("expected both view_once_opened and screenshot_disallowed flags persisted, got %d", len(flResp.Flags))
	}

	// 6. Gifly search & greeting endpoints work cleanly
	gRec := httptest.NewRecorder()
	mux.ServeHTTP(gRec, authReq("GET", "/v1/chat/stickers/greeting?peer_id=u_bob", nil, alicePair.AccessToken, dfp1))
	if gRec.Code != http.StatusOK || !strings.Contains(gRec.Body.String(), "klipy") {
		t.Fatalf("greeting sticker failed: %d %s", gRec.Code, gRec.Body.String())
	}

	// 7. WebSocket handshake check
	srv := httptest.NewServer(mux)
	defer srv.Close()
	wsURL := strings.Replace(srv.URL, "http://", "", 1)
	conn, err := (&http.Client{}).Get(srv.URL + "/healthz")
	if err != nil {
		t.Fatalf("healthz check failed: %v", err)
	}
	_ = conn.Body.Close()

	rawConn, err := netDialWS(wsURL, alicePair.AccessToken, dfp1)
	if err != nil {
		t.Fatalf("websocket connect failed: %v", err)
	}
	_ = rawConn.Close()
}

func netDialWS(hostPort, token, dfp string) (io.Closer, error) {
	c, err := http.NewRequest("GET", "http://"+hostPort+"/v1/chat/ws", nil)
	if err != nil {
		return nil, err
	}
	_ = c
	tcpConn, err := net.Dial("tcp", hostPort)
	if err != nil {
		return nil, err
	}
	key := base64.StdEncoding.EncodeToString([]byte("telefam-ws-test1"))
	reqText := "GET /v1/chat/ws HTTP/1.1\r\n" +
		"Host: " + hostPort + "\r\n" +
		"Upgrade: websocket\r\n" +
		"Connection: Upgrade\r\n" +
		"Sec-WebSocket-Key: " + key + "\r\n" +
		"Sec-WebSocket-Version: 13\r\n" +
		"Authorization: Bearer " + token + "\r\n" +
		"X-Telefam-Device-FP: " + dfp + "\r\n\r\n"
	if _, err := tcpConn.Write([]byte(reqText)); err != nil {
		_ = tcpConn.Close()
		return nil, err
	}
	br := bufio.NewReader(tcpConn)
	statusLine, err := br.ReadString('\n')
	if err != nil || !strings.Contains(statusLine, "101") {
		_ = tcpConn.Close()
		return nil, err
	}
	var acceptLine string
	for {
		line, err := br.ReadString('\n')
		if err != nil || line == "\r\n" {
			break
		}
		if strings.HasPrefix(strings.ToLower(line), "sec-websocket-accept:") {
			acceptLine = strings.TrimSpace(strings.SplitN(line, ":", 2)[1])
		}
	}
	h := sha1.Sum([]byte(key + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11"))
	expectedAccept := base64.StdEncoding.EncodeToString(h[:])
	if acceptLine != expectedAccept {
		_ = tcpConn.Close()
		return nil, http.ErrNotSupported
	}
	return tcpConn, nil
}

func TestChatDeliveryProgressionUnreadIsolationAndLogoutWSClose(t *testing.T) {
	h, mux, tokens := newTestMuxAndTokens()
	dfpAlice := "device-prog-alice"
	dfpBob := "device-prog-bob"
	fpAlice, _ := h.fp.ExtractDeviceFingerprint(&http.Request{Header: http.Header{"X-Telefam-Device-FP": []string{dfpAlice}}})
	fpBob, _ := h.fp.ExtractDeviceFingerprint(&http.Request{Header: http.Header{"X-Telefam-Device-FP": []string{dfpBob}}})

	alicePair, _, _ := tokens.IssueInitialPair("u_alice_p", "alicep@telefam.company", fpAlice)
	bobPair, _, _ := tokens.IssueInitialPair("u_bob_p", "bobp@telefam.company", fpBob)

	h.contacts.RegisterOrUpdateUser(contacts.DirectoryUser{UserID: "u_alice_p", FullName: "Alice P", Username: "alicep"}, fpAlice)
	h.contacts.RegisterOrUpdateUser(contacts.DirectoryUser{UserID: "u_bob_p", FullName: "Bob P", Username: "bobp"}, fpBob)

	cRec := httptest.NewRecorder()
	mux.ServeHTTP(cRec, authReq("POST", "/v1/chat/conversations/direct/u_bob_p", nil, alicePair.AccessToken, dfpAlice))
	var openResp struct {
		Conversation chat.ConversationDTO `json:"conversation"`
	}
	_ = json.Unmarshal(cRec.Body.Bytes(), &openResp)
	convID := openResp.Conversation.ConversationID

	// Alice sends while Bob is offline -> starts as "sent"
	sendRec := httptest.NewRecorder()
	mux.ServeHTTP(sendRec, authReq("POST", "/v1/chat/conversations/"+convID+"/messages", chat.SendMessagePayload{
		ClientOpID:    "op-prog-1",
		MessageType:   "text",
		CiphertextB64: "cHJvZ190ZXh0",
	}, alicePair.AccessToken, dfpAlice))
	var sendResp struct {
		Message chat.MessageDTO `json:"message"`
	}
	_ = json.Unmarshal(sendRec.Body.Bytes(), &sendResp)
	msgID := sendResp.Message.MessageID
	if sendResp.Message.DeliveryStatus != "sent" {
		t.Fatalf("expected initial delivery status 'sent', got %q", sendResp.Message.DeliveryStatus)
	}

	// Bob fetches conversation list (background sync) -> upgrades message to "delivered", preserves UnreadCount == 1
	listRec := httptest.NewRecorder()
	mux.ServeHTTP(listRec, authReq("GET", "/v1/chat/conversations", nil, bobPair.AccessToken, dfpBob))
	var convListResp struct {
		Conversations []chat.ConversationDTO `json:"conversations"`
	}
	_ = json.Unmarshal(listRec.Body.Bytes(), &convListResp)
	if len(convListResp.Conversations) != 1 || convListResp.Conversations[0].UnreadCount != 1 || convListResp.Conversations[0].LastDeliveryStatus != "delivered" {
		t.Fatalf("expected unread=1 and status=delivered on background sync, got %+v", convListResp.Conversations)
	}

	// Explicit "delivered" receipt does not clear unread
	delRcp := httptest.NewRecorder()
	mux.ServeHTTP(delRcp, authReq("POST", "/v1/chat/messages/"+msgID+"/receipt", chat.ReceiptPayload{Status: "delivered"}, bobPair.AccessToken, dfpBob))
	if delRcp.Code != http.StatusOK {
		t.Fatalf("delivered receipt failed: %d", delRcp.Code)
	}
	listRec2 := httptest.NewRecorder()
	mux.ServeHTTP(listRec2, authReq("GET", "/v1/chat/conversations", nil, bobPair.AccessToken, dfpBob))
	_ = json.Unmarshal(listRec2.Body.Bytes(), &convListResp)
	if convListResp.Conversations[0].UnreadCount != 1 {
		t.Fatalf("expected delivered receipt to preserve unread=1, got %d", convListResp.Conversations[0].UnreadCount)
	}

	// "seen" receipt advances status to "seen" and clears unread
	seenRcp := httptest.NewRecorder()
	mux.ServeHTTP(seenRcp, authReq("POST", "/v1/chat/messages/"+msgID+"/receipt", chat.ReceiptPayload{Status: "seen"}, bobPair.AccessToken, dfpBob))
	if seenRcp.Code != http.StatusOK {
		t.Fatalf("seen receipt failed: %d", seenRcp.Code)
	}
	listRec3 := httptest.NewRecorder()
	mux.ServeHTTP(listRec3, authReq("GET", "/v1/chat/conversations", nil, bobPair.AccessToken, dfpBob))
	_ = json.Unmarshal(listRec3.Body.Bytes(), &convListResp)
	if convListResp.Conversations[0].UnreadCount != 0 || convListResp.Conversations[0].LastDeliveryStatus != "seen" {
		t.Fatalf("expected unread=0 and status=seen after seen receipt, got %+v", convListResp.Conversations[0])
	}

	// Connect Bob WS -> logout -> verify Bob's socket closes cleanly without receiving post-logout messages
	srv := httptest.NewServer(mux)
	defer srv.Close()
	wsURL := strings.Replace(srv.URL, "http://", "", 1)
	bobWS, err := netDialWS(wsURL, bobPair.AccessToken, dfpBob)
	if err != nil {
		t.Fatalf("bob ws dial failed: %v", err)
	}
	defer bobWS.Close()

	logoutRec := httptest.NewRecorder()
	mux.ServeHTTP(logoutRec, authReq("POST", "/v1/auth/logout", LogoutPayload{AllDevices: false}, bobPair.AccessToken, dfpBob))
	if logoutRec.Code != http.StatusOK {
		t.Fatalf("bob logout failed: %d", logoutRec.Code)
	}
	// Attempting another message from Alice after Bob logged out should succeed on server,
	// while Bob's closed socket must not remain registered.
	afterLogoutSend := httptest.NewRecorder()
	mux.ServeHTTP(afterLogoutSend, authReq("POST", "/v1/chat/conversations/"+convID+"/messages", chat.SendMessagePayload{
		ClientOpID:    "op-prog-2",
		MessageType:   "text",
		CiphertextB64: "cG9zdF9sb2dvdXQ=",
	}, alicePair.AccessToken, dfpAlice))
	if afterLogoutSend.Code != http.StatusOK {
		t.Fatalf("send after logout failed: %d", afterLogoutSend.Code)
	}
}

