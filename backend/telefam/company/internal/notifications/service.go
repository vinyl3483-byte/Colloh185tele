package notifications

import (
	"bytes"
	"context"
	"crypto"
	"crypto/rand"
	"crypto/rsa"
	"crypto/sha256"
	"crypto/x509"
	"encoding/base64"
	"encoding/hex"
	"encoding/json"
	"encoding/pem"
	"fmt"
	"io"
	"net/http"
	"net/url"
	"os"
	"regexp"
	"sort"
	"strconv"
	"strings"
	"sync"
	"time"
)

type ActorInfo struct {
	UserID    string `json:"user_id"`
	FullName  string `json:"full_name"`
	Username  string `json:"username"`
	AvatarURL string `json:"avatar_url,omitempty"`
}

type Preferences struct {
	MasterPause       bool
	ChannelPush       bool
	TypeMessages      bool
	TypeComments      bool
	TypeFollowers     bool
	QuietHoursEnabled bool
	QuietStartMinutes int
	QuietEndMinutes   int
	WhoCanMention     string
}

type HostHooks struct {
	ResolveUser        func(userID string) (ActorInfo, bool)
	ResolveByUsername  func(username string) (ActorInfo, bool)
	GetPreferences     func(userID string) Preferences
	IsBlocked          func(recipientID, actorID string) bool
	IsMutedUser        func(recipientID, actorID string) bool
	AreFriends         func(ownerID, viewerID string) bool
}

type PushTokenRecord struct {
	ID          string `json:"id"`
	UserID      string `json:"user_id"`
	DeviceFP    string `json:"device_fp"`
	Platform    string `json:"platform"` // android | ios | desktop
	Token       string `json:"token"`
	AppBundle   string `json:"app_bundle"`
	UpdatedAtMs int64  `json:"updated_at_ms"`
}

type NotificationDTO struct {
	ID              string `json:"id"`
	RecipientUserID string `json:"recipient_user_id"`
	ActorUserID     string `json:"actor_user_id"`
	ActorName       string `json:"actor_name"`
	ActorUsername   string `json:"actor_username"`
	ActorAvatarURL  string `json:"actor_avatar_url,omitempty"`
	Kind            string `json:"kind"` // chat_message | follow | like | mention
	Title           string `json:"title"`
	Body            string `json:"body"`
	ConversationID  string `json:"conversation_id,omitempty"`
	MessageID       string `json:"message_id,omitempty"`
	PostID          string `json:"post_id,omitempty"`
	TargetUserID    string `json:"target_user_id,omitempty"`
	IsViewOnce      bool   `json:"is_view_once"`
	IsRead          bool   `json:"is_read"`
	CreatedAtMs     int64  `json:"created_at_ms"`
}

type ChatPushInput struct {
	ConversationID   string
	MessageID        string
	SenderUserID     string
	RecipientUserID  string
	MessageType      string
	CompressionCodec string
	EncryptionScheme string
	CiphertextB64    string
	SignalMsgType    int
	VoiceDurationMs  int64
	CreatedAtMs      int64
	ConvMuted        bool
}

type IncomingCallPushInput struct {
	CallID         string
	ConversationID string
	CallType       string // audio | video
	CallerUserID   string
	CalleeUserID   string
	ExpiresAtMs    int64
	CreatedAtMs    int64
}

type DeliveryLogEntry struct {
	RecipientUserID string            `json:"recipient_user_id"`
	Token           string            `json:"token"`
	Platform        string            `json:"platform"`
	Data            map[string]string `json:"data"`
	Title           string            `json:"title,omitempty"`
	Body            string            `json:"body,omitempty"`
	Status          string            `json:"status"` // sent | skipped_unconfigured | failed
	CreatedAtMs     int64             `json:"created_at_ms"`
}

type serviceAccountCreds struct {
	ProjectID   string `json:"project_id"`
	ClientEmail string `json:"client_email"`
	PrivateKey  string `json:"private_key"`
	TokenURI    string `json:"token_uri"`
}

type Service struct {
	hooks       HostHooks
	mu          sync.Mutex
	tokens      map[string]map[string]PushTokenRecord // userID -> deviceFP -> record
	inbox       map[string][]NotificationDTO          // recipientUserID -> sorted newest first
	deliveryLog []DeliveryLogEntry
	nowFunc     func() time.Time

	httpClient    *http.Client
	fcmProjectID  string
	saEmail       string
	saPrivKey     *rsa.PrivateKey
	oauthTokenURI string
	fcmEndpoint   string
	cachedOAuth   string
	oauthExpiry   time.Time
}

var mentionRegex = regexp.MustCompile(`(?:^|[^a-zA-Z0-9_.@])@([a-zA-Z0-9_.]{3,24})`)

func NewService(hooks HostHooks) *Service {
	s := &Service{
		hooks:        hooks,
		tokens:       make(map[string]map[string]PushTokenRecord),
		inbox:        make(map[string][]NotificationDTO),
		nowFunc:      time.Now,
		httpClient:   &http.Client{Timeout: 8 * time.Second},
		fcmProjectID: strings.TrimSpace(os.Getenv("FCM_PROJECT_ID")),
	}
	s.loadServiceAccountFromEnv()
	return s
}

func (s *Service) ConfigureFCMForTest(projectID, clientEmail, privateKeyPEM, oauthTokenURI, fcmBaseURL string, httpClient *http.Client) error {
	s.mu.Lock()
	defer s.mu.Unlock()
	if httpClient != nil {
		s.httpClient = httpClient
	}
	s.fcmProjectID = strings.TrimSpace(projectID)
	s.saEmail = strings.TrimSpace(clientEmail)
	s.oauthTokenURI = strings.TrimSpace(oauthTokenURI)
	if strings.TrimSpace(fcmBaseURL) != "" {
		s.fcmEndpoint = strings.TrimRight(fcmBaseURL, "/") + "/v1/projects/" + s.fcmProjectID + "/messages:send"
	}
	s.cachedOAuth = ""
	s.oauthExpiry = time.Time{}
	if strings.TrimSpace(privateKeyPEM) != "" {
		pk, err := parseRSAPrivateKey(privateKeyPEM)
		if err != nil {
			return err
		}
		s.saPrivKey = pk
	}
	return nil
}

func (s *Service) loadServiceAccountFromEnv() {
	rawJSON := strings.TrimSpace(os.Getenv("FCM_SERVICE_ACCOUNT_JSON"))
	if rawJSON == "" {
		if path := strings.TrimSpace(os.Getenv("GOOGLE_APPLICATION_CREDENTIALS")); path != "" {
			if data, err := os.ReadFile(path); err == nil {
				rawJSON = string(data)
			}
		}
	}
	if rawJSON == "" {
		return
	}
	var creds serviceAccountCreds
	if err := json.Unmarshal([]byte(rawJSON), &creds); err != nil {
		return
	}
	if s.fcmProjectID == "" {
		s.fcmProjectID = strings.TrimSpace(creds.ProjectID)
	}
	s.saEmail = strings.TrimSpace(creds.ClientEmail)
	s.oauthTokenURI = strings.TrimSpace(creds.TokenURI)
	if s.oauthTokenURI == "" {
		s.oauthTokenURI = "https://oauth2.googleapis.com/token"
	}
	if s.fcmProjectID != "" {
		s.fcmEndpoint = "https://fcm.googleapis.com/v1/projects/" + url.PathEscape(s.fcmProjectID) + "/messages:send"
	}
	if pk, err := parseRSAPrivateKey(creds.PrivateKey); err == nil {
		s.saPrivKey = pk
	}
}

func parseRSAPrivateKey(pemStr string) (*rsa.PrivateKey, error) {
	normalized := strings.ReplaceAll(strings.TrimSpace(pemStr), `\n`, "\n")
	block, _ := pem.Decode([]byte(normalized))
	if block == nil {
		return nil, fmt.Errorf("invalid private key pem")
	}
	if key, err := x509.ParsePKCS8PrivateKey(block.Bytes); err == nil {
		if rsaKey, ok := key.(*rsa.PrivateKey); ok {
			return rsaKey, nil
		}
		return nil, fmt.Errorf("non-rsa pkcs8 private key")
	}
	if rsaKey, err := x509.ParsePKCS1PrivateKey(block.Bytes); err == nil {
		return rsaKey, nil
	}
	return nil, fmt.Errorf("unsupported private key format")
}

func newRandomID(prefix string) string {
	var b [12]byte
	_, _ = rand.Read(b[:])
	return prefix + "_" + hex.EncodeToString(b[:])
}

func (s *Service) RegisterPushToken(userID, deviceFP, platform, token, appBundle string) (PushTokenRecord, bool) {
	cleanUID := strings.TrimSpace(userID)
	cleanDevice := strings.TrimSpace(deviceFP)
	cleanToken := strings.TrimSpace(token)
	if cleanUID == "" || cleanDevice == "" || cleanToken == "" || len(cleanToken) > 4096 {
		return PushTokenRecord{}, false
	}
	cleanPlatform := strings.ToLower(strings.TrimSpace(platform))
	switch cleanPlatform {
	case "android", "ios", "desktop":
	default:
		cleanPlatform = "android"
	}
	cleanBundle := strings.TrimSpace(appBundle)
	if cleanBundle == "" {
		cleanBundle = "com.telefam.company.app"
	}

	now := s.nowFunc().UTC().UnixMilli()
	s.mu.Lock()
	defer s.mu.Unlock()

	// Ensure a physical device token never lingers on a previously logged-in user on shared hardware.
	for uid, devMap := range s.tokens {
		if uid == cleanUID {
			continue
		}
		for fp, rec := range devMap {
			if rec.Token == cleanToken || fp == cleanDevice {
				delete(devMap, fp)
			}
		}
		if len(devMap) == 0 {
			delete(s.tokens, uid)
		}
	}

	userTokens := s.tokens[cleanUID]
	if userTokens == nil {
		userTokens = make(map[string]PushTokenRecord)
		s.tokens[cleanUID] = userTokens
	}
	rec := PushTokenRecord{
		ID:          newRandomID("ptk"),
		UserID:      cleanUID,
		DeviceFP:    cleanDevice,
		Platform:    cleanPlatform,
		Token:       cleanToken,
		AppBundle:   cleanBundle,
		UpdatedAtMs: now,
	}
	userTokens[cleanDevice] = rec
	return rec, true
}

func (s *Service) RevokeDeviceToken(userID, deviceFP string) int {
	cleanUID := strings.TrimSpace(userID)
	cleanDevice := strings.TrimSpace(deviceFP)
	if cleanUID == "" || cleanDevice == "" {
		return 0
	}
	s.mu.Lock()
	defer s.mu.Unlock()
	devMap := s.tokens[cleanUID]
	if devMap == nil {
		return 0
	}
	if _, exists := devMap[cleanDevice]; !exists {
		return 0
	}
	delete(devMap, cleanDevice)
	if len(devMap) == 0 {
		delete(s.tokens, cleanUID)
	}
	return 1
}

func (s *Service) RevokeAllUserTokens(userID string) int {
	cleanUID := strings.TrimSpace(userID)
	if cleanUID == "" {
		return 0
	}
	s.mu.Lock()
	defer s.mu.Unlock()
	devMap := s.tokens[cleanUID]
	count := len(devMap)
	delete(s.tokens, cleanUID)
	return count
}

func (s *Service) ListUserTokens(userID string) []PushTokenRecord {
	s.mu.Lock()
	defer s.mu.Unlock()
	devMap := s.tokens[strings.TrimSpace(userID)]
	out := make([]PushTokenRecord, 0, len(devMap))
	for _, r := range devMap {
		out = append(out, r)
	}
	return out
}

func (s *Service) ListNotifications(userID string, limit int) ([]NotificationDTO, int) {
	if limit <= 0 || limit > 100 {
		limit = 50
	}
	cleanUID := strings.TrimSpace(userID)
	s.mu.Lock()
	defer s.mu.Unlock()
	items := s.inbox[cleanUID]
	unread := 0
	for _, item := range items {
		if !item.IsRead {
			unread++
		}
	}
	if len(items) > limit {
		items = items[:limit]
	}
	copied := make([]NotificationDTO, len(items))
	copy(copied, items)
	return copied, unread
}

func (s *Service) MarkNotificationRead(userID, notificationID string) (int, bool) {
	cleanUID := strings.TrimSpace(userID)
	cleanID := strings.TrimSpace(notificationID)
	s.mu.Lock()
	defer s.mu.Unlock()
	items := s.inbox[cleanUID]
	found := false
	unread := 0
	for i := range items {
		if items[i].ID == cleanID {
			items[i].IsRead = true
			found = true
		}
		if !items[i].IsRead {
			unread++
		}
	}
	s.inbox[cleanUID] = items
	return unread, found
}

func (s *Service) MarkConversationNotificationsRead(userID, conversationID string) int {
	cleanUID := strings.TrimSpace(userID)
	cleanConv := strings.TrimSpace(conversationID)
	if cleanUID == "" || cleanConv == "" {
		return 0
	}
	s.mu.Lock()
	defer s.mu.Unlock()
	items := s.inbox[cleanUID]
	unread := 0
	for i := range items {
		if items[i].Kind == "chat_message" && items[i].ConversationID == cleanConv {
			items[i].IsRead = true
		}
		if !items[i].IsRead {
			unread++
		}
	}
	s.inbox[cleanUID] = items
	return unread
}

func (s *Service) MarkAllRead(userID string) {
	cleanUID := strings.TrimSpace(userID)
	s.mu.Lock()
	defer s.mu.Unlock()
	items := s.inbox[cleanUID]
	for i := range items {
		items[i].IsRead = true
	}
	s.inbox[cleanUID] = items
}

func (s *Service) DeliveryLogSnapshot() []DeliveryLogEntry {
	s.mu.Lock()
	defer s.mu.Unlock()
	out := make([]DeliveryLogEntry, len(s.deliveryLog))
	copy(out, s.deliveryLog)
	return out
}

func ExtractMentionUsernames(text string) []string {
	matches := mentionRegex.FindAllStringSubmatch(text, -1)
	if len(matches) == 0 {
		return nil
	}
	seen := make(map[string]struct{}, len(matches))
	out := make([]string, 0, len(matches))
	for _, m := range matches {
		if len(m) < 2 {
			continue
		}
		uname := strings.ToLower(strings.TrimSpace(m[1]))
		if uname == "" {
			continue
		}
		if _, dup := seen[uname]; dup {
			continue
		}
		seen[uname] = struct{}{}
		out = append(out, uname)
		if len(out) >= 20 {
			break
		}
	}
	return out
}

func (s *Service) resolveActor(actorID string) ActorInfo {
	if s.hooks.ResolveUser != nil {
		if u, ok := s.hooks.ResolveUser(actorID); ok {
			if u.FullName == "" {
				u.FullName = u.Username
			}
			return u
		}
	}
	return ActorInfo{UserID: actorID, FullName: actorID, Username: "member"}
}

func (s *Service) shouldAllowRecord(recipientID, actorID string) bool {
	if recipientID == "" || actorID == "" || recipientID == actorID {
		return false
	}
	if s.hooks.IsBlocked != nil && s.hooks.IsBlocked(recipientID, actorID) {
		return false
	}
	if s.hooks.IsMutedUser != nil && s.hooks.IsMutedUser(recipientID, actorID) {
		return false
	}
	return true
}

func (s *Service) shouldSendPush(recipientID, kind string, convMuted bool) bool {
	if convMuted {
		return false
	}
	if s.hooks.GetPreferences == nil {
		return true
	}
	prefs := s.hooks.GetPreferences(recipientID)
	if prefs.MasterPause || !prefs.ChannelPush {
		return false
	}
	switch kind {
	case "chat_message":
		if !prefs.TypeMessages {
			return false
		}
	case "follow":
		if !prefs.TypeFollowers {
			return false
		}
	case "like", "mention":
		if !prefs.TypeComments {
			return false
		}
	}
	if prefs.QuietHoursEnabled && isWithinQuietHours(s.nowFunc().UTC(), prefs.QuietStartMinutes, prefs.QuietEndMinutes) {
		return false
	}
	return true
}

func isWithinQuietHours(now time.Time, startMin, endMin int) bool {
	if startMin == endMin {
		return false
	}
	cur := now.Hour()*60 + now.Minute()
	if startMin < endMin {
		return cur >= startMin && cur < endMin
	}
	// Wrap around midnight (e.g., 22:00 to 07:00)
	return cur >= startMin || cur < endMin
}

func (s *Service) appendInboxLocked(dto NotificationDTO) {
	items := append([]NotificationDTO{dto}, s.inbox[dto.RecipientUserID]...)
	if len(items) > 120 {
		items = items[:120]
	}
	sort.SliceStable(items, func(i, j int) bool {
		return items[i].CreatedAtMs > items[j].CreatedAtMs
	})
	s.inbox[dto.RecipientUserID] = items
}

func (s *Service) NotifyChatMessage(ctx context.Context, input ChatPushInput) {
	if !s.shouldAllowRecord(input.RecipientUserID, input.SenderUserID) {
		return
	}
	actor := s.resolveActor(input.SenderUserID)
	isViewOnce := strings.EqualFold(input.MessageType, "voice_once")

	if !s.shouldSendPush(input.RecipientUserID, "chat_message", input.ConvMuted) {
		return
	}

	cipherForPush := input.CiphertextB64
	if isViewOnce {
		// Never expose ciphertext or preview hints on the notification wire for view-once voice notes.
		cipherForPush = ""
	}
	ts := input.CreatedAtMs
	if ts <= 0 {
		ts = s.nowFunc().UTC().UnixMilli()
	}
	sigType := input.SignalMsgType
	if sigType == 0 {
		sigType = 3
	}
	data := map[string]string{
		"type":              "chat_message",
		"conversation_id":   input.ConversationID,
		"message_id":        input.MessageID,
		"sender_user_id":    actor.UserID,
		"sender_name":       actor.FullName,
		"sender_username":   actor.Username,
		"sender_avatar_url": actor.AvatarURL,
		"message_type":      input.MessageType,
		"compression_codec": input.CompressionCodec,
		"encryption_scheme": input.EncryptionScheme,
		"ciphertext_b64":    cipherForPush,
		"signal_msg_type":   strconv.Itoa(sigType),
		"is_view_once":      strconv.FormatBool(isViewOnce),
		"created_at_ms":     strconv.FormatInt(ts, 10),
	}
	// Send high-priority FCM data message so the client decrypts locally via E2EE and builds Reply/Mark-Read actions.
	s.dispatchToUser(ctx, input.RecipientUserID, data, "", "", isViewOnce)
}

func (s *Service) NotifyChatMessageCancel(ctx context.Context, recipientUserID, conversationID, messageID, reason string) {
	if strings.TrimSpace(recipientUserID) == "" || strings.TrimSpace(messageID) == "" {
		return
	}
	data := map[string]string{
		"type":            "message_cancel",
		"conversation_id": conversationID,
		"message_id":      messageID,
		"reason":          reason,
		"created_at_ms":   strconv.FormatInt(s.nowFunc().UTC().UnixMilli(), 10),
	}
	s.dispatchToUser(ctx, recipientUserID, data, "", "", false)
}

func (s *Service) NotifyIncomingCall(ctx context.Context, input IncomingCallPushInput) {
	if !s.shouldAllowRecord(input.CalleeUserID, input.CallerUserID) || strings.TrimSpace(input.CallID) == "" {
		return
	}
	// Calls bypass quiet hours unless MasterPause or Push is explicitly off
	if s.hooks.GetPreferences != nil {
		prefs := s.hooks.GetPreferences(input.CalleeUserID)
		if prefs.MasterPause || !prefs.ChannelPush {
			return
		}
	}
	caller := s.resolveActor(input.CallerUserID)
	callType := strings.ToLower(strings.TrimSpace(input.CallType))
	if callType != "video" {
		callType = "audio"
	}
	ts := input.CreatedAtMs
	if ts <= 0 {
		ts = s.nowFunc().UTC().UnixMilli()
	}
	title := caller.FullName
	body := "Incoming Audio Call"
	if callType == "video" {
		body = "Incoming Video Call"
	}
	data := map[string]string{
		"type":              "incoming_call",
		"recipient_user_id": input.CalleeUserID,
		"call_id":           input.CallID,
		"conversation_id":   input.ConversationID,
		"call_type":         callType,
		"caller_user_id":    caller.UserID,
		"caller_name":       caller.FullName,
		"caller_username":   caller.Username,
		"caller_avatar_url": caller.AvatarURL,
		"expires_at_ms":     strconv.FormatInt(input.ExpiresAtMs, 10),
		"created_at_ms":     strconv.FormatInt(ts, 10),
	}
	s.dispatchToUser(ctx, input.CalleeUserID, data, title, body, false)
}

func (s *Service) NotifyCallCancelled(ctx context.Context, recipientUserID, callID, conversationID, reason string) {
	if strings.TrimSpace(recipientUserID) == "" || strings.TrimSpace(callID) == "" {
		return
	}
	data := map[string]string{
		"type":              "call_cancel",
		"recipient_user_id": recipientUserID,
		"call_id":           callID,
		"conversation_id":   conversationID,
		"reason":            reason,
		"created_at_ms":     strconv.FormatInt(s.nowFunc().UTC().UnixMilli(), 10),
	}
	s.dispatchToUser(ctx, recipientUserID, data, "", "", false)
}

func (s *Service) NotifyMissedCall(ctx context.Context, calleeUserID, callerUserID, callID, convID, callType string) *NotificationDTO {
	if !s.shouldAllowRecord(calleeUserID, callerUserID) {
		return nil
	}
	caller := s.resolveActor(callerUserID)
	now := s.nowFunc().UTC().UnixMilli()
	title := caller.FullName
	body := "Missed audio call"
	if strings.EqualFold(callType, "video") {
		body = "Missed video call"
	}
	dto := NotificationDTO{
		ID:              newRandomID("ntf"),
		RecipientUserID: calleeUserID,
		ActorUserID:     caller.UserID,
		ActorName:       caller.FullName,
		ActorUsername:   caller.Username,
		ActorAvatarURL:  caller.AvatarURL,
		Kind:            "missed_call",
		Title:           title,
		Body:            body,
		ConversationID:  convID,
		MessageID:       callID,
		TargetUserID:    caller.UserID,
		CreatedAtMs:     now,
	}
	s.mu.Lock()
	s.appendInboxLocked(dto)
	s.mu.Unlock()
	if s.shouldSendPush(calleeUserID, "chat_message", false) {
		s.dispatchToUser(ctx, calleeUserID, map[string]string{
			"type":              "missed_call",
			"recipient_user_id": calleeUserID,
			"notification_id":   dto.ID,
			"call_id":           callID,
			"conversation_id":   convID,
			"call_type":         callType,
			"actor_user_id":     caller.UserID,
			"actor_name":        caller.FullName,
			"actor_username":    caller.Username,
			"actor_avatar_url":  caller.AvatarURL,
			"title":             title,
			"body":              body,
			"created_at_ms":     strconv.FormatInt(now, 10),
		}, title, body, false)
	}
	return &dto
}

func (s *Service) NotifyFollow(ctx context.Context, actorUserID, targetUserID string) *NotificationDTO {
	if !s.shouldAllowRecord(targetUserID, actorUserID) {
		return nil
	}
	actor := s.resolveActor(actorUserID)
	now := s.nowFunc().UTC().UnixMilli()
	title := actor.FullName
	body := "started following you"
	dto := NotificationDTO{
		ID:              newRandomID("ntf"),
		RecipientUserID: targetUserID,
		ActorUserID:     actor.UserID,
		ActorName:       actor.FullName,
		ActorUsername:   actor.Username,
		ActorAvatarURL:  actor.AvatarURL,
		Kind:            "follow",
		Title:           title,
		Body:            body,
		TargetUserID:    actor.UserID,
		CreatedAtMs:     now,
	}
	s.mu.Lock()
	s.appendInboxLocked(dto)
	s.mu.Unlock()

	if s.shouldSendPush(targetUserID, "follow", false) {
		s.dispatchToUser(ctx, targetUserID, map[string]string{
			"type":              "follow",
			"notification_id":   dto.ID,
			"actor_user_id":     actor.UserID,
			"actor_name":        actor.FullName,
			"actor_username":    actor.Username,
			"actor_avatar_url":  actor.AvatarURL,
			"target_user_id":    actor.UserID,
			"title":             title,
			"body":              body,
			"created_at_ms":     strconv.FormatInt(now, 10),
		}, title, body, false)
	}
	return &dto
}

func (s *Service) NotifyLike(ctx context.Context, actorUserID, authorUserID, postID, postMode string) *NotificationDTO {
	if !s.shouldAllowRecord(authorUserID, actorUserID) || strings.TrimSpace(postID) == "" {
		return nil
	}
	actor := s.resolveActor(actorUserID)
	now := s.nowFunc().UTC().UnixMilli()
	targetNoun := "post"
	if strings.EqualFold(postMode, "video") {
		targetNoun = "video"
	} else if strings.EqualFold(postMode, "photos") {
		targetNoun = "photo"
	}
	title := actor.FullName
	body := "liked your " + targetNoun
	dto := NotificationDTO{
		ID:              newRandomID("ntf"),
		RecipientUserID: authorUserID,
		ActorUserID:     actor.UserID,
		ActorName:       actor.FullName,
		ActorUsername:   actor.Username,
		ActorAvatarURL:  actor.AvatarURL,
		Kind:            "like",
		Title:           title,
		Body:            body,
		PostID:          postID,
		TargetUserID:    actor.UserID,
		CreatedAtMs:     now,
	}
	s.mu.Lock()
	s.appendInboxLocked(dto)
	s.mu.Unlock()

	if s.shouldSendPush(authorUserID, "like", false) {
		s.dispatchToUser(ctx, authorUserID, map[string]string{
			"type":             "like",
			"notification_id":  dto.ID,
			"actor_user_id":    actor.UserID,
			"actor_name":       actor.FullName,
			"actor_username":   actor.Username,
			"actor_avatar_url": actor.AvatarURL,
			"post_id":          postID,
			"target_user_id":   actor.UserID,
			"title":            title,
			"body":             body,
			"created_at_ms":    strconv.FormatInt(now, 10),
		}, title, body, false)
	}
	return &dto
}

func (s *Service) NotifyMentions(ctx context.Context, actorUserID string, usernames []string, sourceKind, postID, convID, msgID string) []NotificationDTO {
	if len(usernames) == 0 || s.hooks.ResolveByUsername == nil {
		return nil
	}
	actor := s.resolveActor(actorUserID)
	now := s.nowFunc().UTC().UnixMilli()
	created := make([]NotificationDTO, 0, len(usernames))

	for _, uname := range usernames {
		target, ok := s.hooks.ResolveByUsername(uname)
		if !ok || !s.shouldAllowRecord(target.UserID, actorUserID) {
			continue
		}
		if s.hooks.GetPreferences != nil {
			prefs := s.hooks.GetPreferences(target.UserID)
			rule := strings.ToLower(strings.TrimSpace(prefs.WhoCanMention))
			if rule == "no_one" || rule == "nobody" {
				continue
			}
			if (rule == "friends" || rule == "followers") && s.hooks.AreFriends != nil {
				if !s.hooks.AreFriends(target.UserID, actorUserID) {
					continue
				}
			}
		}

		title := actor.FullName
		body := "mentioned you in a post"
		if sourceKind == "chat" {
			body = "mentioned you in a message"
		}
		dto := NotificationDTO{
			ID:              newRandomID("ntf"),
			RecipientUserID: target.UserID,
			ActorUserID:     actor.UserID,
			ActorName:       actor.FullName,
			ActorUsername:   actor.Username,
			ActorAvatarURL:  actor.AvatarURL,
			Kind:            "mention",
			Title:           title,
			Body:            body,
			ConversationID:  convID,
			MessageID:       msgID,
			PostID:          postID,
			TargetUserID:    actor.UserID,
			CreatedAtMs:     now,
		}
		s.mu.Lock()
		s.appendInboxLocked(dto)
		s.mu.Unlock()
		created = append(created, dto)

		if s.shouldSendPush(target.UserID, "mention", false) {
			s.dispatchToUser(ctx, target.UserID, map[string]string{
				"type":             "mention",
				"notification_id":  dto.ID,
				"actor_user_id":    actor.UserID,
				"actor_name":       actor.FullName,
				"actor_username":   actor.Username,
				"actor_avatar_url": actor.AvatarURL,
				"post_id":          postID,
				"conversation_id":  convID,
				"message_id":       msgID,
				"target_user_id":   actor.UserID,
				"title":            title,
				"body":             body,
				"created_at_ms":    strconv.FormatInt(now, 10),
			}, title, body, false)
		}
	}
	return created
}

func (s *Service) NotifyStarReceived(ctx context.Context, senderUserID, recipientUserID, convID, messageID string, stars int64) *NotificationDTO {
	if stars <= 0 || !s.shouldAllowRecord(recipientUserID, senderUserID) {
		return nil
	}
	actor := s.resolveActor(senderUserID)
	now := s.nowFunc().UTC().UnixMilli()
	title := actor.FullName
	if strings.TrimSpace(title) == "" {
		title = "Telefam"
	}
	body := fmt.Sprintf("Sent you %d Telefam Stars", stars)
	if stars == 1 {
		body = "Sent you 1 Telefam Star"
	}
	dto := NotificationDTO{
		ID:              newRandomID("ntf"),
		RecipientUserID: recipientUserID,
		ActorUserID:     actor.UserID,
		ActorName:       actor.FullName,
		ActorUsername:   actor.Username,
		ActorAvatarURL:  actor.AvatarURL,
		Kind:            "star_received",
		Title:           title,
		Body:            body,
		ConversationID:  convID,
		MessageID:       messageID,
		TargetUserID:    actor.UserID,
		CreatedAtMs:     now,
	}
	s.mu.Lock()
	s.appendInboxLocked(dto)
	s.mu.Unlock()

	if s.shouldSendPush(recipientUserID, "star_received", false) {
		s.dispatchToUser(ctx, recipientUserID, map[string]string{
			"type":              "star_received",
			"recipient_user_id": recipientUserID,
			"notification_id":   dto.ID,
			"actor_user_id":     actor.UserID,
			"actor_name":        actor.FullName,
			"actor_username":    actor.Username,
			"actor_avatar_url":  actor.AvatarURL,
			"conversation_id":   convID,
			"message_id":        messageID,
			"target_user_id":    actor.UserID,
			"stars_amount":      strconv.FormatInt(stars, 10),
			"title":             title,
			"body":              body,
			"created_at_ms":     strconv.FormatInt(now, 10),
		}, title, body, false)
	}
	return &dto
}

func (s *Service) NotifyVerificationUpdate(ctx context.Context, recipientUserID, status, tier, title, body string) *NotificationDTO {
	if strings.TrimSpace(recipientUserID) == "" {
		return nil
	}
	now := s.nowFunc().UTC().UnixMilli()
	cleanTitle := strings.TrimSpace(title)
	if cleanTitle == "" {
		cleanTitle = "Telefam Verification"
	}
	cleanBody := strings.TrimSpace(body)
	if cleanBody == "" {
		cleanBody = "Your verification status has been updated."
	}
	dto := NotificationDTO{
		ID:              newRandomID("ntf"),
		RecipientUserID: recipientUserID,
		ActorUserID:     "telefam_verification",
		ActorName:       "Telefam Verification",
		ActorUsername:   "telefam",
		Kind:            "verification_update",
		Title:           cleanTitle,
		Body:            cleanBody,
		TargetUserID:    recipientUserID,
		CreatedAtMs:     now,
	}
	s.mu.Lock()
	s.appendInboxLocked(dto)
	s.mu.Unlock()

	if s.shouldSendPush(recipientUserID, "verification_update", false) {
		s.dispatchToUser(ctx, recipientUserID, map[string]string{
			"type":              "verification_update",
			"recipient_user_id": recipientUserID,
			"notification_id":   dto.ID,
			"status":            status,
			"tier":              tier,
			"target_user_id":    recipientUserID,
			"title":             cleanTitle,
			"body":              cleanBody,
			"created_at_ms":     strconv.FormatInt(now, 10),
		}, cleanTitle, cleanBody, false)
	}
	return &dto
}

func (s *Service) NotifySubscriptionEvent(
	ctx context.Context,
	recipientUserID string,
	actor ActorInfo,
	kind string,
	title string,
	body string,
	billingCycle string,
) *NotificationDTO {
	if strings.TrimSpace(recipientUserID) == "" {
		return nil
	}
	cleanKind := strings.TrimSpace(kind)
	if cleanKind != "new_subscriber" && cleanKind != "subscription_update" {
		cleanKind = "subscription_update"
	}
	now := s.nowFunc().UTC().UnixMilli()
	cleanTitle := strings.TrimSpace(title)
	if cleanTitle == "" {
		cleanTitle = "Telefam Subscriptions"
	}
	cleanBody := strings.TrimSpace(body)
	if cleanBody == "" {
		cleanBody = "Your subscription status was updated."
	}
	dto := NotificationDTO{
		ID:              newRandomID("ntf"),
		RecipientUserID: recipientUserID,
		ActorUserID:     actor.UserID,
		ActorName:       actor.FullName,
		ActorUsername:   actor.Username,
		ActorAvatarURL:  actor.AvatarURL,
		Kind:            cleanKind,
		Title:           cleanTitle,
		Body:            cleanBody,
		TargetUserID:    actor.UserID,
		CreatedAtMs:     now,
	}
	s.mu.Lock()
	s.appendInboxLocked(dto)
	s.mu.Unlock()

	if s.shouldSendPush(recipientUserID, cleanKind, false) {
		s.dispatchToUser(ctx, recipientUserID, map[string]string{
			"type":              cleanKind,
			"recipient_user_id": recipientUserID,
			"notification_id":   dto.ID,
			"actor_user_id":     actor.UserID,
			"actor_name":        actor.FullName,
			"actor_username":    actor.Username,
			"actor_avatar_url":  actor.AvatarURL,
			"target_user_id":    actor.UserID,
			"billing_cycle":     billingCycle,
			"title":             cleanTitle,
			"body":              cleanBody,
			"created_at_ms":     strconv.FormatInt(now, 10),
		}, cleanTitle, cleanBody, false)
	}
	return &dto
}

func (s *Service) dispatchToUser(ctx context.Context, recipientID string, data map[string]string, title, body string, isViewOnce bool) {
	tokens := s.ListUserTokens(recipientID)
	if len(tokens) == 0 {
		return
	}
	if ctx == nil {
		ctx = context.Background()
	}

	for _, rec := range tokens {
		entry := DeliveryLogEntry{
			RecipientUserID: recipientID,
			Token:           rec.Token,
			Platform:        rec.Platform,
			Data:            cloneStringMap(data),
			Title:           title,
			Body:            body,
			CreatedAtMs:     s.nowFunc().UTC().UnixMilli(),
		}

		if s.isFCMConfigured() {
			if err := s.sendFCMv1(ctx, rec, data, title, body, isViewOnce); err != nil {
				entry.Status = "failed"
			} else {
				entry.Status = "sent"
			}
		} else {
			entry.Status = "skipped_unconfigured"
		}

		s.mu.Lock()
		s.deliveryLog = append(s.deliveryLog, entry)
		if len(s.deliveryLog) > 200 {
			s.deliveryLog = s.deliveryLog[len(s.deliveryLog)-200:]
		}
		s.mu.Unlock()
	}
}

func (s *Service) isFCMConfigured() bool {
	s.mu.Lock()
	defer s.mu.Unlock()
	return s.fcmProjectID != "" && s.saEmail != "" && s.saPrivKey != nil && s.fcmEndpoint != ""
}

func (s *Service) sendFCMv1(ctx context.Context, target PushTokenRecord, data map[string]string, title, body string, isViewOnce bool) error {
	accessToken, err := s.getOAuthAccessToken(ctx)
	if err != nil {
		return err
	}

	msg := map[string]any{
		"token": target.Token,
		"data":  data,
		"android": map[string]any{
			"priority": "HIGH",
		},
	}

	// Non-chat social notifications can include alert title/body for iOS APNs fallback;
	// E2EE chat_message relies on local client decryption (APNs mutable-content / Android FCM Service).
	if target.Platform == "ios" {
		aps := map[string]any{
			"sound": "default",
		}
		if data["type"] == "incoming_call" {
			aps["category"] = "TELEFAM_INCOMING_CALL"
			aps["interruption-level"] = "time-sensitive"
			aps["content-available"] = 1
			aps["alert"] = map[string]any{
				"title": title,
				"body":  body,
			}
		} else if title != "" && !isViewOnce {
			aps["alert"] = map[string]any{
				"title": title,
				"body":  body,
			}
		} else if data["type"] == "chat_message" {
			aps["mutable-content"] = 1
			aps["content-available"] = 1
			alertBody := "New encrypted message"
			if isViewOnce {
				alertBody = "Sent a view-once voice message"
			}
			aps["alert"] = map[string]any{
				"title": data["sender_name"],
				"body":  alertBody,
			}
			if !isViewOnce {
				aps["category"] = "TELEFAM_CHAT_MESSAGE"
			} else {
				aps["category"] = "TELEFAM_CHAT_VIEW_ONCE"
			}
		}
		msg["apns"] = map[string]any{
			"headers": map[string]string{
				"apns-priority": "10",
			},
			"payload": map[string]any{
				"aps": aps,
			},
		}
	}

	payloadBytes, err := json.Marshal(map[string]any{"message": msg})
	if err != nil {
		return err
	}

	s.mu.Lock()
	endpoint := s.fcmEndpoint
	client := s.httpClient
	s.mu.Unlock()

	req, err := http.NewRequestWithContext(ctx, http.MethodPost, endpoint, bytes.NewReader(payloadBytes))
	if err != nil {
		return err
	}
	req.Header.Set("Authorization", "Bearer "+accessToken)
	req.Header.Set("Content-Type", "application/json")

	resp, err := client.Do(req)
	if err != nil {
		return err
	}
	defer resp.Body.Close()
	_, _ = io.Copy(io.Discard, io.LimitReader(resp.Body, 4096))
	if resp.StatusCode < 200 || resp.StatusCode >= 300 {
		if resp.StatusCode == http.StatusNotFound || resp.StatusCode == http.StatusGone {
			s.RevokeDeviceToken(target.UserID, target.DeviceFP)
		}
		return fmt.Errorf("fcm returned status %d", resp.StatusCode)
	}
	return nil
}

func (s *Service) getOAuthAccessToken(ctx context.Context) (string, error) {
	s.mu.Lock()
	if s.cachedOAuth != "" && time.Now().Add(60*time.Second).Before(s.oauthExpiry) {
		tok := s.cachedOAuth
		s.mu.Unlock()
		return tok, nil
	}
	email := s.saEmail
	privKey := s.saPrivKey
	tokenURI := s.oauthTokenURI
	client := s.httpClient
	s.mu.Unlock()

	if privKey == nil || email == "" || tokenURI == "" {
		return "", fmt.Errorf("fcm credentials incomplete")
	}

	now := time.Now().UTC()
	headerB64 := b64URL([]byte(`{"alg":"RS256","typ":"JWT"}`))
	claimsObj := map[string]any{
		"iss":   email,
		"scope": "https://www.googleapis.com/auth/firebase.messaging",
		"aud":   tokenURI,
		"iat":   now.Unix(),
		"exp":   now.Add(55 * time.Minute).Unix(),
	}
	claimsBytes, _ := json.Marshal(claimsObj)
	signingInput := headerB64 + "." + b64URL(claimsBytes)
	digest := sha256.Sum256([]byte(signingInput))
	sig, err := rsa.SignPKCS1v15(rand.Reader, privKey, crypto.SHA256, digest[:])
	if err != nil {
		return "", err
	}
	assertion := signingInput + "." + b64URL(sig)

	form := url.Values{}
	form.Set("grant_type", "urn:ietf:params:oauth:grant-type:jwt-bearer")
	form.Set("assertion", assertion)

	req, err := http.NewRequestWithContext(ctx, http.MethodPost, tokenURI, strings.NewReader(form.Encode()))
	if err != nil {
		return "", err
	}
	req.Header.Set("Content-Type", "application/x-www-form-urlencoded")

	resp, err := client.Do(req)
	if err != nil {
		return "", err
	}
	defer resp.Body.Close()
	if resp.StatusCode < 200 || resp.StatusCode >= 300 {
		return "", fmt.Errorf("oauth token exchange status %d", resp.StatusCode)
	}
	var tokenResp struct {
		AccessToken string `json:"access_token"`
		ExpiresIn   int64  `json:"expires_in"`
	}
	if err := json.NewDecoder(io.LimitReader(resp.Body, 16384)).Decode(&tokenResp); err != nil {
		return "", err
	}
	if tokenResp.AccessToken == "" {
		return "", fmt.Errorf("empty oauth access token")
	}
	ttl := time.Duration(tokenResp.ExpiresIn) * time.Second
	if ttl <= 0 {
		ttl = 50 * time.Minute
	}
	s.mu.Lock()
	s.cachedOAuth = tokenResp.AccessToken
	s.oauthExpiry = time.Now().Add(ttl)
	s.mu.Unlock()
	return tokenResp.AccessToken, nil
}

func b64URL(raw []byte) string {
	return base64.RawURLEncoding.EncodeToString(raw)
}

func cloneStringMap(m map[string]string) map[string]string {
	out := make(map[string]string, len(m))
	for k, v := range m {
		out[k] = v
	}
	return out
}
