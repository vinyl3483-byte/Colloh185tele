package chat

import (
	"crypto/rand"
	"crypto/sha256"
	"encoding/base64"
	"encoding/hex"
	"errors"
	"fmt"
	"sort"
	"strings"
	"sync"
	"time"
)

var (
	ErrForbidden     = errors.New("forbidden")
	ErrNotFound      = errors.New("not found")
	ErrBadRequest    = errors.New("bad request")
	ErrViewOnceGuard = errors.New("only recipient can open view once voice note")
)

type UserInfo struct {
	UserID    string
	FullName  string
	Username  string
	AvatarURL string
	Location  string
}

type PrivacyView struct {
	MessageRequests     string
	WhoCanCall          string
	WhoCanScreenshot    string
	WhoCanShareChats    string
	WhoCanCopyMessages  string
	WhoCanDownloadMedia string
	ReadReceipts        string
	TypingIndicators    string
	OnlineIndicators    string
}

// HostHooks decouples chat from auth/contacts/feeds internals cleanly.
type HostHooks struct {
	ResolveUser      func(userID string) (UserInfo, bool)
	ListAllUsers     func(query string) []UserInfo
	GetPrivacy       func(userID string) PrivacyView
	IsBlocked        func(a, b string) bool
	BlockUser        func(caller, target string)
	IsFriendOrFollow   func(owner, viewer string) (isFollowing, followsYou bool)
	UpdateAvatarURL    func(mediaID string) string
	OnMessageSent      func(msg MessageDTO, recipientConvMuted bool)
	OnMessageCancelled func(recipientUserID, convID, messageID, reason string)
	OnCallSignalWS     func(callerID string, rawPayload []byte)
}

type storedKeyBundle struct {
	UserID             string
	DeviceID           int
	RegistrationID     int
	IdentityPubB64     string
	SignedPreKeyID     int
	SignedPreKeyPubB64 string
	SignedPreKeySigB64 string
	KyberPreKeyID      int
	KyberPreKeyPubB64  string
	KyberPreKeySigB64  string
	OneTimePreKeys     []OneTimePreKeyDTO
	UpdatedAtMs        int64
}

type storedConversation struct {
	ConversationID    string
	UserA             string
	UserB             string
	LastMessageID     string
	LastMessageType   string
	LastSenderID      string
	LastCiphertext       string
	LastSenderCipher     string
	LastEncryptionScheme string
	LastSignalMsgType    int
	LastDelivery         string
	LastMessageAtMs   int64
	ArchivedA         bool
	ArchivedB         bool
	MutedA            bool
	MutedB            bool
	UnreadA           int
	UnreadB           int
	DeletedBeforeAMs  int64
	DeletedBeforeBMs  int64
	CreatedAtMs       int64
	UpdatedAtMs       int64
}

type storedMessage struct {
	DTO         MessageDTO
	DeletedForA bool
	DeletedForB bool
}

type storedVoiceMedia struct {
	MediaID         string
	ConversationID  string
	UploaderUserID  string
	RecipientUserID string
	MimeType        string
	SHA256          string
	DurationMs      int64
	IsViewOnce      bool
	Bytes           []byte
	CreatedAtMs     int64
}

type Service struct {
	hooks         HostHooks
	Gifly         *GiflyClient
	mu            sync.Mutex
	keyBundles    map[string]*storedKeyBundle
	conversations map[string]*storedConversation
	pairIndex     map[string]string // sorted "a|b" -> conversation_id
	messages      map[string][]*storedMessage
	messageIndex  map[string]*storedMessage
	clientOpIndex map[string]*storedMessage
	voiceMedia    map[string]*storedVoiceMedia
	flags         map[string][]SecurityFlagDTO
	wsClients     map[string]map[*wsClient]struct{}
	lastSeenMs    map[string]int64
	nowFunc       func() time.Time
}

func NewService(hooks HostHooks) *Service {
	return &Service{
		hooks:         hooks,
		Gifly:         NewGiflyClient(""),
		keyBundles:    make(map[string]*storedKeyBundle),
		conversations: make(map[string]*storedConversation),
		pairIndex:     make(map[string]string),
		messages:      make(map[string][]*storedMessage),
		messageIndex:  make(map[string]*storedMessage),
		clientOpIndex: make(map[string]*storedMessage),
		voiceMedia:    make(map[string]*storedVoiceMedia),
		flags:         make(map[string][]SecurityFlagDTO),
		wsClients:     make(map[string]map[*wsClient]struct{}),
		lastSeenMs:    make(map[string]int64),
		nowFunc:       time.Now,
	}
}

func (s *Service) nowMs() int64 {
	return s.nowFunc().UTC().UnixMilli()
}

func canonicalPair(a, b string) (string, string) {
	if a < b {
		return a, b
	}
	return b, a
}

func newRandomID(prefix string) string {
	var b [12]byte
	_, _ = rand.Read(b[:])
	return prefix + "_" + hex.EncodeToString(b[:])
}

func (s *Service) allowsRule(owner, viewer, rule string) bool {
	if owner == viewer {
		return true
	}
	if s.hooks.IsBlocked != nil && s.hooks.IsBlocked(owner, viewer) {
		return false
	}
	switch strings.ToLower(strings.TrimSpace(rule)) {
	case "anyone", "everyone", "":
		return true
	case "nobody", "no_one", "only_me":
		return false
	case "friends":
		if s.hooks.IsFriendOrFollow == nil {
			return false
		}
		a, b := s.hooks.IsFriendOrFollow(owner, viewer)
		return a && b
	case "contacts", "contacts_only":
		if s.hooks.IsFriendOrFollow == nil {
			return false
		}
		a, b := s.hooks.IsFriendOrFollow(owner, viewer)
		return a || b
	default:
		return false
	}
}

func (s *Service) UploadKeyBundle(userID string, payload E2eeKeyBundleUploadPayload) error {
	if strings.TrimSpace(payload.IdentityPubB64) == "" || strings.TrimSpace(payload.SignedPreKeyPubB64) == "" {
		return ErrBadRequest
	}
	otks := make([]OneTimePreKeyDTO, 0, len(payload.OneTimePreKeys))
	for _, k := range payload.OneTimePreKeys {
		if strings.TrimSpace(k.PublicKeyB64) != "" {
			otks = append(otks, k)
		}
	}
	s.mu.Lock()
	s.keyBundles[userID] = &storedKeyBundle{
		UserID:             userID,
		DeviceID:           1,
		RegistrationID:     payload.RegistrationID,
		IdentityPubB64:     payload.IdentityPubB64,
		SignedPreKeyID:     payload.SignedPreKeyID,
		SignedPreKeyPubB64: payload.SignedPreKeyPubB64,
		SignedPreKeySigB64: payload.SignedPreKeySigB64,
		KyberPreKeyID:      payload.KyberPreKeyID,
		KyberPreKeyPubB64:  payload.KyberPreKeyPubB64,
		KyberPreKeySigB64:  payload.KyberPreKeySigB64,
		OneTimePreKeys:     otks,
		UpdatedAtMs:        s.nowMs(),
	}
	s.mu.Unlock()
	return nil
}

func (s *Service) FetchPeerKeyBundle(callerID, peerID string) (E2eeKeyBundleResponse, error) {
	if s.hooks.IsBlocked != nil && s.hooks.IsBlocked(callerID, peerID) {
		return E2eeKeyBundleResponse{}, ErrForbidden
	}
	s.mu.Lock()
	defer s.mu.Unlock()
	b, ok := s.keyBundles[peerID]
	if !ok {
		return E2eeKeyBundleResponse{}, ErrNotFound
	}
	resp := E2eeKeyBundleResponse{
		Success:            true,
		UserID:             b.UserID,
		DeviceID:           b.DeviceID,
		RegistrationID:     b.RegistrationID,
		IdentityPubB64:     b.IdentityPubB64,
		SignedPreKeyID:     b.SignedPreKeyID,
		SignedPreKeyPubB64: b.SignedPreKeyPubB64,
		SignedPreKeySigB64: b.SignedPreKeySigB64,
		KyberPreKeyID:      b.KyberPreKeyID,
		KyberPreKeyPubB64:  b.KyberPreKeyPubB64,
		KyberPreKeySigB64:  b.KyberPreKeySigB64,
		ServerEpochMs:      s.nowMs(),
	}
	if len(b.OneTimePreKeys) > 0 {
		consumed := b.OneTimePreKeys[0]
		b.OneTimePreKeys = b.OneTimePreKeys[1:]
		resp.PreKeyID = consumed.KeyID
		resp.PreKeyPubB64 = consumed.PublicKeyB64
	}
	return resp, nil
}

func (s *Service) OpenOrCreateDirectConversation(callerID, targetUserID string) (ConversationDTO, error) {
	targetUserID = strings.TrimSpace(targetUserID)
	if targetUserID == "" || targetUserID == callerID {
		return ConversationDTO{}, ErrBadRequest
	}
	userA, userB := canonicalPair(callerID, targetUserID)
	pairKey := userA + "|" + userB

	s.mu.Lock()
	if convID, exists := s.pairIndex[pairKey]; exists {
		if existingConv := s.conversations[convID]; existingConv != nil {
			dto := s.toConversationDTOLocked(callerID, existingConv)
			s.mu.Unlock()
			return dto, nil
		}
	}
	s.mu.Unlock()

	if s.hooks.IsBlocked != nil && s.hooks.IsBlocked(callerID, targetUserID) {
		return ConversationDTO{}, ErrForbidden
	}
	if s.hooks.ResolveUser != nil {
		if _, ok := s.hooks.ResolveUser(targetUserID); !ok {
			return ConversationDTO{}, ErrNotFound
		}
	}
	if s.hooks.GetPrivacy != nil {
		pv := s.hooks.GetPrivacy(targetUserID)
		if !s.allowsRule(targetUserID, callerID, pv.MessageRequests) {
			return ConversationDTO{}, ErrForbidden
		}
	}

	now := s.nowMs()
	s.mu.Lock()
	convID, exists := s.pairIndex[pairKey]
	var c *storedConversation
	if exists {
		c = s.conversations[convID]
	} else {
		convID = newRandomID("conv")
		c = &storedConversation{
			ConversationID: convID,
			UserA:          userA,
			UserB:          userB,
			CreatedAtMs:    now,
			UpdatedAtMs:    now,
		}
		s.pairIndex[pairKey] = convID
		s.conversations[convID] = c
	}
	dto := s.toConversationDTOLocked(callerID, c)
	s.mu.Unlock()
	return dto, nil
}

func (s *Service) ListConversations(callerID string, includeArchived bool) []ConversationDTO {
	s.mu.Lock()
	s.lastSeenMs[callerID] = s.nowMs()
	receiptEvents := s.promotePendingDeliveredLocked(callerID)

	out := make([]ConversationDTO, 0)
	for _, c := range s.conversations {
		if c.UserA != callerID && c.UserB != callerID {
			continue
		}
		dto := s.toConversationDTOLocked(callerID, c)
		if dto.IsArchived && !includeArchived {
			continue
		}
		delBefore := c.DeletedBeforeAMs
		if callerID == c.UserB {
			delBefore = c.DeletedBeforeBMs
		}
		if delBefore > 0 && c.LastMessageAtMs <= delBefore {
			continue
		}
		out = append(out, dto)
	}
	sort.Slice(out, func(i, j int) bool {
		return out[i].UpdatedAtMs > out[j].UpdatedAtMs
	})
	s.mu.Unlock()

	for _, ev := range receiptEvents {
		s.pushWSEvent(ev.TargetUserID, ev.Event)
	}
	return out
}

func (s *Service) ListMessages(callerID, convID string) ([]MessageDTO, ConversationDTO, error) {
	return s.ListMessagesDelta(callerID, convID, 0)
}

func (s *Service) ListMessagesDelta(callerID, convID string, sinceMs int64) ([]MessageDTO, ConversationDTO, error) {
	s.mu.Lock()
	c, ok := s.conversations[convID]
	if !ok {
		s.mu.Unlock()
		return nil, ConversationDTO{}, ErrNotFound
	}
	if c.UserA != callerID && c.UserB != callerID {
		s.mu.Unlock()
		return nil, ConversationDTO{}, ErrForbidden
	}

	s.purgeExpiredViewOnceLocked(convID)
	receiptEvents := s.promotePendingDeliveredLocked(callerID)

	delBefore := c.DeletedBeforeAMs
	isA := callerID == c.UserA
	if !isA {
		delBefore = c.DeletedBeforeBMs
	}

	raw := s.messages[convID]
	items := make([]MessageDTO, 0, len(raw))
	for _, m := range raw {
		if m.DTO.CreatedAtMs <= delBefore {
			continue
		}
		if (isA && m.DeletedForA) || (!isA && m.DeletedForB) {
			continue
		}
		if sinceMs > 0 && m.DTO.CreatedAtMs <= sinceMs && m.DTO.EditedAtMs <= sinceMs {
			continue
		}
		items = append(items, m.DTO)
	}
	convDTO := s.toConversationDTOLocked(callerID, c)
	s.mu.Unlock()

	for _, ev := range receiptEvents {
		s.pushWSEvent(ev.TargetUserID, ev.Event)
	}
	return items, convDTO, nil
}

func (s *Service) SendMessage(callerID, convID string, req SendMessagePayload) (MessageDTO, error) {
	msgType := normalizeMsgType(req.MessageType)
	if strings.TrimSpace(req.CiphertextB64) == "" && strings.TrimSpace(req.MediaURL) == "" {
		return MessageDTO{}, ErrBadRequest
	}
	s.mu.Lock()
	c, ok := s.conversations[convID]
	if !ok {
		s.mu.Unlock()
		return MessageDTO{}, ErrNotFound
	}
	if c.UserA != callerID && c.UserB != callerID {
		s.mu.Unlock()
		return MessageDTO{}, ErrForbidden
	}
	peerID := c.UserB
	if callerID == c.UserB {
		peerID = c.UserA
	}
	if s.hooks.IsBlocked != nil && s.hooks.IsBlocked(callerID, peerID) {
		s.mu.Unlock()
		return MessageDTO{}, ErrForbidden
	}
	opID := strings.TrimSpace(req.ClientOpID)
	if opID != "" {
		if prev, exists := s.clientOpIndex[opID]; exists {
			dto := prev.DTO
			s.mu.Unlock()
			return dto, nil
		}
	}

	now := s.nowMs()
	codec := strings.TrimSpace(req.CompressionCodec)
	if codec == "" {
		codec = "zlib"
	}
	scheme := strings.TrimSpace(req.EncryptionScheme)
	if scheme == "" {
		scheme = "libsignal_v1"
	}
	sigType := req.SignalMsgType
	if sigType == 0 {
		sigType = 3
	}
	ttl := req.ViewOnceTTLSec
	if msgType == "voice_once" && ttl <= 0 {
		ttl = 10
	}
	initialStatus := "sent"
	if s.isOnlineLocked(peerID) {
		initialStatus = "delivered"
	}

	dto := MessageDTO{
		MessageID:           newRandomID("msg"),
		ConversationID:      convID,
		SenderUserID:        callerID,
		RecipientUserID:     peerID,
		ClientOpID:          opID,
		MessageType:         msgType,
		CompressionCodec:    codec,
		EncryptionScheme:    scheme,
		CiphertextB64:       req.CiphertextB64,
		SenderCiphertextB64: req.SenderCiphertextB64,
		SignalMsgType:       sigType,
		MediaURL:            strings.TrimSpace(req.MediaURL),
		MediaID:             strings.TrimSpace(req.MediaID),
		VoiceDurationMs:     req.VoiceDurationMs,
		ViewOnceTTLSec:      ttl,
		ForwardedFromUserID: strings.TrimSpace(req.ForwardedFromUserID),
		DeliveryStatus:      initialStatus,
		CreatedAtMs:         now,
	}
	rec := &storedMessage{DTO: dto}
	s.messages[convID] = append(s.messages[convID], rec)
	s.messageIndex[dto.MessageID] = rec
	if opID != "" {
		s.clientOpIndex[opID] = rec
	}

	c.LastMessageID = dto.MessageID
	c.LastMessageType = dto.MessageType
	c.LastSenderID = callerID
	c.LastCiphertext = dto.CiphertextB64
	c.LastSenderCipher = dto.SenderCiphertextB64
	c.LastEncryptionScheme = dto.EncryptionScheme
	c.LastSignalMsgType = dto.SignalMsgType
	c.LastDelivery = dto.DeliveryStatus
	c.LastMessageAtMs = now
	c.UpdatedAtMs = now
	recipientMuted := false
	if peerID == c.UserA {
		c.UnreadA++
		c.ArchivedA = false
		recipientMuted = c.MutedA
	} else {
		c.UnreadB++
		c.ArchivedB = false
		recipientMuted = c.MutedB
	}
	s.lastSeenMs[callerID] = now
	onSent := s.hooks.OnMessageSent
	s.mu.Unlock()

	s.pushWSEvent(peerID, WebSocketEvent{
		Event:          "message_new",
		ConversationID: convID,
		UserID:         callerID,
		MessageID:      dto.MessageID,
		Message:        &dto,
		TimestampMs:    now,
	})
	if onSent != nil {
		onSent(dto, recipientMuted)
	}
	return dto, nil
}

func (s *Service) EditMessage(callerID, messageID string, req EditMessagePayload) (MessageDTO, error) {
	if strings.TrimSpace(req.CiphertextB64) == "" {
		return MessageDTO{}, ErrBadRequest
	}
	s.mu.Lock()
	m, ok := s.messageIndex[messageID]
	if !ok {
		s.mu.Unlock()
		return MessageDTO{}, ErrNotFound
	}
	if m.DTO.SenderUserID != callerID {
		s.mu.Unlock()
		return MessageDTO{}, ErrForbidden
	}
	now := s.nowMs()
	m.DTO.CiphertextB64 = req.CiphertextB64
	if strings.TrimSpace(req.SenderCiphertextB64) != "" {
		m.DTO.SenderCiphertextB64 = req.SenderCiphertextB64
	}
	if req.SignalMsgType > 0 {
		m.DTO.SignalMsgType = req.SignalMsgType
	}
	m.DTO.EditedAtMs = now
	if c := s.conversations[m.DTO.ConversationID]; c != nil && c.LastMessageID == m.DTO.MessageID {
		c.LastCiphertext = m.DTO.CiphertextB64
		c.LastSenderCipher = m.DTO.SenderCiphertextB64
	}
	dto := m.DTO
	peerID := m.DTO.RecipientUserID
	s.mu.Unlock()

	s.pushWSEvent(peerID, WebSocketEvent{
		Event:          "message_edited",
		ConversationID: dto.ConversationID,
		UserID:         callerID,
		MessageID:      dto.MessageID,
		Message:        &dto,
		TimestampMs:    now,
	})
	return dto, nil
}

// DeleteMessage handles both "unsend" (erases all traces for both participants + media) and "self" delete.
func (s *Service) DeleteMessage(callerID, messageID, mode string) (string, error) {
	unsend := strings.EqualFold(strings.TrimSpace(mode), "unsend") || strings.TrimSpace(mode) == ""
	s.mu.Lock()
	m, ok := s.messageIndex[messageID]
	if !ok {
		s.mu.Unlock()
		return "", ErrNotFound
	}
	c := s.conversations[m.DTO.ConversationID]
	if c == nil || (c.UserA != callerID && c.UserB != callerID) {
		s.mu.Unlock()
		return "", ErrForbidden
	}
	if unsend && m.DTO.SenderUserID != callerID {
		s.mu.Unlock()
		return "", ErrForbidden
	}

	convID := m.DTO.ConversationID
	peerID := m.DTO.RecipientUserID
	if peerID == callerID {
		peerID = m.DTO.SenderUserID
	}

	if unsend {
		s.purgeMessageEverywhereLocked(m)
	} else {
		if callerID == c.UserA {
			m.DeletedForA = true
		} else {
			m.DeletedForB = true
		}
	}
	onCancel := s.hooks.OnMessageCancelled
	s.mu.Unlock()

	if unsend {
		s.pushWSEvent(peerID, WebSocketEvent{
			Event:          "message_unsent",
			ConversationID: convID,
			UserID:         callerID,
			MessageID:      messageID,
			TimestampMs:    s.nowMs(),
		})
		if onCancel != nil {
			onCancel(peerID, convID, messageID, "message_unsent")
		}
	}
	return convID, nil
}

func (s *Service) purgeMessageEverywhereLocked(m *storedMessage) {
	convID := m.DTO.ConversationID
	msgID := m.DTO.MessageID
	if m.DTO.MediaID != "" {
		delete(s.voiceMedia, m.DTO.MediaID)
	}
	delete(s.messageIndex, msgID)
	if m.DTO.ClientOpID != "" {
		delete(s.clientOpIndex, m.DTO.ClientOpID)
	}
	list := s.messages[convID]
	filtered := make([]*storedMessage, 0, len(list))
	for _, item := range list {
		if item.DTO.MessageID != msgID {
			filtered = append(filtered, item)
		}
	}
	s.messages[convID] = filtered

	if c := s.conversations[convID]; c != nil && c.LastMessageID == msgID {
		if len(filtered) > 0 {
			last := filtered[len(filtered)-1].DTO
			c.LastMessageID = last.MessageID
			c.LastMessageType = last.MessageType
			c.LastSenderID = last.SenderUserID
			c.LastCiphertext = last.CiphertextB64
			c.LastSenderCipher = last.SenderCiphertextB64
			c.LastEncryptionScheme = last.EncryptionScheme
			c.LastSignalMsgType = last.SignalMsgType
			c.LastDelivery = last.DeliveryStatus
			c.LastMessageAtMs = last.CreatedAtMs
		} else {
			c.LastMessageID = ""
			c.LastMessageType = ""
			c.LastSenderID = ""
			c.LastCiphertext = ""
			c.LastSenderCipher = ""
			c.LastEncryptionScheme = ""
			c.LastSignalMsgType = 0
			c.LastDelivery = ""
			c.LastMessageAtMs = 0
		}
	}
}

func (s *Service) UpdateMessageReceipt(callerID, messageID, status string) (MessageDTO, error) {
	clean := strings.ToLower(strings.TrimSpace(status))
	if clean != "delivered" && clean != "seen" {
		return MessageDTO{}, ErrBadRequest
	}
	s.mu.Lock()
	m, ok := s.messageIndex[messageID]
	if !ok {
		s.mu.Unlock()
		return MessageDTO{}, ErrNotFound
	}
	if m.DTO.RecipientUserID != callerID {
		s.mu.Unlock()
		return MessageDTO{}, ErrForbidden
	}

	effective := clean
	if clean == "seen" && s.hooks.GetPrivacy != nil {
		callerPv := s.hooks.GetPrivacy(callerID)
		if !s.allowsRule(callerID, m.DTO.SenderUserID, callerPv.ReadReceipts) {
			effective = "delivered"
		}
	}

	now := s.nowMs()
	extraEvents := make([]targetedWSEvent, 0)
	if effective == "seen" || (effective == "delivered" && m.DTO.DeliveryStatus == "sent") {
		m.DTO.DeliveryStatus = effective
	}
	if list := s.messages[m.DTO.ConversationID]; len(list) > 0 {
		for _, prev := range list {
			if prev.DTO.MessageID == m.DTO.MessageID || prev.DTO.RecipientUserID != callerID || prev.DTO.CreatedAtMs > m.DTO.CreatedAtMs {
				continue
			}
			upgraded := false
			if effective == "seen" && prev.DTO.DeliveryStatus != "seen" {
				prev.DTO.DeliveryStatus = "seen"
				upgraded = true
			} else if effective == "delivered" && prev.DTO.DeliveryStatus == "sent" {
				prev.DTO.DeliveryStatus = "delivered"
				upgraded = true
			}
			if upgraded {
				extraEvents = append(extraEvents, targetedWSEvent{
					TargetUserID: prev.DTO.SenderUserID,
					Event: WebSocketEvent{
						Event:          "message_receipt",
						ConversationID: prev.DTO.ConversationID,
						UserID:         callerID,
						MessageID:      prev.DTO.MessageID,
						Status:         prev.DTO.DeliveryStatus,
						TimestampMs:    now,
					},
				})
			}
		}
	}
	if c := s.conversations[m.DTO.ConversationID]; c != nil {
		// Unread counters belong strictly to whether the user has opened/seen messages;
		// delivering in background must NEVER clear the unread badge.
		if clean == "seen" {
			if callerID == c.UserA {
				c.UnreadA = 0
			} else {
				c.UnreadB = 0
			}
		}
		if c.LastMessageID == m.DTO.MessageID {
			c.LastDelivery = m.DTO.DeliveryStatus
		}
	}
	dto := m.DTO
	s.mu.Unlock()

	for _, ev := range extraEvents {
		s.pushWSEvent(ev.TargetUserID, ev.Event)
	}
	s.pushWSEvent(dto.SenderUserID, WebSocketEvent{
		Event:          "message_receipt",
		ConversationID: dto.ConversationID,
		UserID:         callerID,
		MessageID:      dto.MessageID,
		Status:         dto.DeliveryStatus,
		TimestampMs:    now,
	})
	return dto, nil
}

func (s *Service) StoreVoiceMedia(callerID, convID, mimeType string, raw []byte, durationMs int64, isViewOnce bool) (string, string, error) {
	if len(raw) == 0 || len(raw) > 10*1024*1024 {
		return "", "", ErrBadRequest
	}
	s.mu.Lock()
	c, ok := s.conversations[convID]
	if !ok {
		s.mu.Unlock()
		return "", "", ErrNotFound
	}
	if c.UserA != callerID && c.UserB != callerID {
		s.mu.Unlock()
		return "", "", ErrForbidden
	}
	peerID := c.UserB
	if callerID == c.UserB {
		peerID = c.UserA
	}
	sum := sha256.Sum256(raw)
	fp := hex.EncodeToString(sum[:])
	mediaID := newRandomID("vm")
	if strings.TrimSpace(mimeType) == "" {
		mimeType = "audio/mp4"
	}
	cp := make([]byte, len(raw))
	copy(cp, raw)
	s.voiceMedia[mediaID] = &storedVoiceMedia{
		MediaID:         mediaID,
		ConversationID:  convID,
		UploaderUserID:  callerID,
		RecipientUserID: peerID,
		MimeType:        mimeType,
		SHA256:          fp,
		DurationMs:      durationMs,
		IsViewOnce:      isViewOnce,
		Bytes:           cp,
		CreatedAtMs:     s.nowMs(),
	}
	s.mu.Unlock()
	return mediaID, fp, nil
}

func (s *Service) GetVoiceMedia(callerID, mediaID string) (string, []byte, error) {
	s.mu.Lock()
	defer s.mu.Unlock()
	vm, ok := s.voiceMedia[mediaID]
	if !ok {
		return "", nil, ErrNotFound
	}
	if vm.UploaderUserID != callerID && vm.RecipientUserID != callerID {
		return "", nil, ErrForbidden
	}
	return vm.MimeType, vm.Bytes, nil
}

func (s *Service) OpenViewOnceMessage(callerID, messageID string) (MessageDTO, SecurityFlagDTO, error) {
	s.mu.Lock()
	m, ok := s.messageIndex[messageID]
	if !ok {
		s.mu.Unlock()
		return MessageDTO{}, SecurityFlagDTO{}, ErrNotFound
	}
	if m.DTO.RecipientUserID != callerID {
		s.mu.Unlock()
		return MessageDTO{}, SecurityFlagDTO{}, ErrViewOnceGuard
	}
	now := s.nowMs()
	if m.DTO.ViewOnceOpenedAtMs == 0 {
		m.DTO.ViewOnceOpenedAtMs = now
		ttl := int64(m.DTO.ViewOnceTTLSec)
		if ttl <= 0 {
			ttl = 10
		}
		m.DTO.ViewOnceExpiresAtMs = now + ttl*1000
	}
	m.DTO.DeliveryStatus = "seen"
	flag := SecurityFlagDTO{
		FlagID:         newRandomID("flag"),
		ConversationID: m.DTO.ConversationID,
		MessageID:      m.DTO.MessageID,
		ActorUserID:    callerID,
		TargetUserID:   m.DTO.SenderUserID,
		FlagType:       "view_once_opened",
		Details:        "View-once voice note opened under protected mode",
		CreatedAtMs:    now,
	}
	s.flags[m.DTO.ConversationID] = append(s.flags[m.DTO.ConversationID], flag)
	dto := m.DTO
	senderID := m.DTO.SenderUserID
	onCancel := s.hooks.OnMessageCancelled
	s.mu.Unlock()

	s.pushWSEvent(senderID, WebSocketEvent{
		Event:          "view_once_opened",
		ConversationID: dto.ConversationID,
		UserID:         callerID,
		MessageID:      dto.MessageID,
		Message:        &dto,
		Flag:           &flag,
		TimestampMs:    now,
	})
	if onCancel != nil {
		onCancel(callerID, dto.ConversationID, dto.MessageID, "view_once_opened")
	}
	return dto, flag, nil
}

func (s *Service) ConsumeViewOnceMessage(callerID, messageID string) (string, error) {
	s.mu.Lock()
	m, ok := s.messageIndex[messageID]
	if !ok {
		s.mu.Unlock()
		return "", nil
	}
	if m.DTO.SenderUserID != callerID && m.DTO.RecipientUserID != callerID {
		s.mu.Unlock()
		return "", ErrForbidden
	}
	convID := m.DTO.ConversationID
	senderID := m.DTO.SenderUserID
	recipientID := m.DTO.RecipientUserID
	s.purgeMessageEverywhereLocked(m)
	onCancel := s.hooks.OnMessageCancelled
	s.mu.Unlock()

	ev := WebSocketEvent{
		Event:          "view_once_consumed",
		ConversationID: convID,
		UserID:         callerID,
		MessageID:      messageID,
		TimestampMs:    s.nowMs(),
	}
	s.pushWSEvent(senderID, ev)
	s.pushWSEvent(recipientID, ev)
	if onCancel != nil {
		onCancel(recipientID, convID, messageID, "view_once_consumed")
	}
	return convID, nil
}

func (s *Service) purgeExpiredViewOnceLocked(convID string) {
	now := s.nowMs()
	list := s.messages[convID]
	for _, m := range list {
		if m.DTO.MessageType == "voice_once" && m.DTO.ViewOnceExpiresAtMs > 0 && now >= m.DTO.ViewOnceExpiresAtMs {
			s.purgeMessageEverywhereLocked(m)
		}
	}
}

func (s *Service) BatchConversationAction(callerID string, payload BatchConversationActionPayload) ([]ConversationDTO, error) {
	action := strings.ToLower(strings.TrimSpace(payload.Action))
	if len(payload.ConversationIDs) == 0 {
		return nil, ErrBadRequest
	}
	now := s.nowMs()
	s.mu.Lock()
	for _, convID := range payload.ConversationIDs {
		c, ok := s.conversations[convID]
		if !ok || (c.UserA != callerID && c.UserB != callerID) {
			continue
		}
		isA := callerID == c.UserA
		peerID := c.UserB
		if !isA {
			peerID = c.UserA
		}
		switch action {
		case "archive":
			if isA {
				c.ArchivedA = true
			} else {
				c.ArchivedB = true
			}
		case "unarchive":
			if isA {
				c.ArchivedA = false
			} else {
				c.ArchivedB = false
			}
		case "mute":
			if isA {
				c.MutedA = true
			} else {
				c.MutedB = true
			}
		case "unmute":
			if isA {
				c.MutedA = false
			} else {
				c.MutedB = false
			}
		case "mark_read":
			if isA {
				c.UnreadA = 0
			} else {
				c.UnreadB = 0
			}
		case "mark_unread":
			if isA {
				if c.UnreadA == 0 {
					c.UnreadA = 1
				}
			} else {
				if c.UnreadB == 0 {
					c.UnreadB = 1
				}
			}
		case "delete":
			if isA {
				c.DeletedBeforeAMs = now
				c.UnreadA = 0
			} else {
				c.DeletedBeforeBMs = now
				c.UnreadB = 0
			}
		case "block":
			if s.hooks.BlockUser != nil {
				s.hooks.BlockUser(callerID, peerID)
			}
			s.pushWSEvent(peerID, WebSocketEvent{
				Event:          "conversation_updated",
				ConversationID: convID,
				UserID:         callerID,
				TimestampMs:    now,
			})
		}
	}
	s.mu.Unlock()
	return s.ListConversations(callerID, false), nil
}

func (s *Service) NotifyBlockStateChanged(callerID, targetUserID string) {
	userA, userB := canonicalPair(callerID, targetUserID)
	pairKey := userA + "|" + userB
	s.mu.Lock()
	convID := s.pairIndex[pairKey]
	s.mu.Unlock()
	if convID == "" {
		return
	}
	now := s.nowMs()
	s.pushWSEvent(targetUserID, WebSocketEvent{
		Event:          "conversation_updated",
		ConversationID: convID,
		UserID:         callerID,
		TimestampMs:    now,
	})
	s.pushWSEvent(callerID, WebSocketEvent{
		Event:          "conversation_updated",
		ConversationID: convID,
		UserID:         callerID,
		TimestampMs:    now,
	})
}

func (s *Service) RecordSecurityFlag(callerID, convID string, payload SecurityFlagPayload) (SecurityFlagDTO, error) {
	ft := strings.ToLower(strings.TrimSpace(payload.FlagType))
	if ft != "screenshot_disallowed" && ft != "view_once_opened" && ft != "view_once_capture_attempt" {
		ft = "screenshot_disallowed"
	}
	s.mu.Lock()
	c, ok := s.conversations[convID]
	if !ok {
		s.mu.Unlock()
		return SecurityFlagDTO{}, ErrNotFound
	}
	if c.UserA != callerID && c.UserB != callerID {
		s.mu.Unlock()
		return SecurityFlagDTO{}, ErrForbidden
	}
	peerID := c.UserB
	if callerID == c.UserB {
		peerID = c.UserA
	}
	now := s.nowMs()
	flag := SecurityFlagDTO{
		FlagID:         newRandomID("flag"),
		ConversationID: convID,
		MessageID:      strings.TrimSpace(payload.MessageID),
		ActorUserID:    callerID,
		TargetUserID:   peerID,
		FlagType:       ft,
		Details:        strings.TrimSpace(payload.Details),
		CreatedAtMs:    now,
	}
	s.flags[convID] = append(s.flags[convID], flag)
	s.mu.Unlock()

	s.pushWSEvent(peerID, WebSocketEvent{
		Event:          "security_flag",
		ConversationID: convID,
		UserID:         callerID,
		MessageID:      flag.MessageID,
		Flag:           &flag,
		TimestampMs:    now,
	})
	return flag, nil
}

func (s *Service) ListSecurityFlags(callerID, convID string) ([]SecurityFlagDTO, error) {
	s.mu.Lock()
	defer s.mu.Unlock()
	c, ok := s.conversations[convID]
	if !ok {
		return nil, ErrNotFound
	}
	if c.UserA != callerID && c.UserB != callerID {
		return nil, ErrForbidden
	}
	list := s.flags[convID]
	out := make([]SecurityFlagDTO, len(list))
	copy(out, list)
	return out, nil
}

func (s *Service) SearchDirectoryAndConversations(callerID, query string) ([]ConversationDTO, []ChatSearchPersonDTO) {
	clean := strings.ToLower(strings.TrimSpace(query))
	convs := s.ListConversations(callerID, true)
	matchedConvs := make([]ConversationDTO, 0)
	byUserConv := make(map[string]string)
	for _, c := range convs {
		byUserConv[c.PeerUserID] = c.ConversationID
		if clean == "" ||
			strings.Contains(strings.ToLower(c.PeerFullName), clean) ||
			strings.Contains(strings.ToLower(c.PeerUsername), clean) {
			matchedConvs = append(matchedConvs, c)
		}
	}
	people := make([]ChatSearchPersonDTO, 0)
	if s.hooks.ListAllUsers != nil {
		for _, u := range s.hooks.ListAllUsers(query) {
			if u.UserID == callerID {
				continue
			}
			if s.hooks.IsBlocked != nil && s.hooks.IsBlocked(callerID, u.UserID) {
				continue
			}
			people = append(people, ChatSearchPersonDTO{
				UserID:         u.UserID,
				FullName:       u.FullName,
				Username:       u.Username,
				AvatarURL:      u.AvatarURL,
				Location:       u.Location,
				ConversationID: byUserConv[u.UserID],
			})
		}
	}
	return matchedConvs, people
}

func (s *Service) HandleTypingSignal(callerID, convID, mode string) {
	s.mu.Lock()
	c, ok := s.conversations[convID]
	if !ok || (c.UserA != callerID && c.UserB != callerID) {
		s.mu.Unlock()
		return
	}
	peerID := c.UserB
	if callerID == c.UserB {
		peerID = c.UserA
	}
	allowTyping := true
	if s.hooks.GetPrivacy != nil {
		callerPv := s.hooks.GetPrivacy(callerID)
		allowTyping = s.allowsRule(callerID, peerID, callerPv.TypingIndicators)
	}
	s.mu.Unlock()
	if !allowTyping {
		return
	}
	cleanMode := strings.ToLower(strings.TrimSpace(mode))
	switch cleanMode {
	case "", "idle":
		cleanMode = ""
	case "recording", "recording_voice":
		cleanMode = "recording_voice"
	default:
		cleanMode = "typing"
	}
	s.pushWSEvent(peerID, WebSocketEvent{
		Event:          "typing",
		ConversationID: convID,
		UserID:         callerID,
		TypingMode:     cleanMode,
		TimestampMs:    s.nowMs(),
	})
}

type targetedWSEvent struct {
	TargetUserID string
	Event        WebSocketEvent
}

func (s *Service) promotePendingDeliveredLocked(recipientID string) []targetedWSEvent {
	if recipientID == "" {
		return nil
	}
	now := s.nowMs()
	events := make([]targetedWSEvent, 0)
	for convID, list := range s.messages {
		c := s.conversations[convID]
		for _, m := range list {
			if m.DTO.RecipientUserID != recipientID || m.DTO.DeliveryStatus != "sent" {
				continue
			}
			m.DTO.DeliveryStatus = "delivered"
			if c != nil && c.LastMessageID == m.DTO.MessageID {
				c.LastDelivery = "delivered"
			}
			events = append(events, targetedWSEvent{
				TargetUserID: m.DTO.SenderUserID,
				Event: WebSocketEvent{
					Event:          "message_receipt",
					ConversationID: m.DTO.ConversationID,
					UserID:         recipientID,
					MessageID:      m.DTO.MessageID,
					Status:         "delivered",
					TimestampMs:    now,
				},
			})
		}
	}
	return events
}

func (s *Service) registerWSClient(c *wsClient) {
	now := s.nowMs()
	s.mu.Lock()
	set := s.wsClients[c.userID]
	if set == nil {
		set = make(map[*wsClient]struct{})
		s.wsClients[c.userID] = set
	}
	set[c] = struct{}{}
	s.lastSeenMs[c.userID] = now
	receiptEvents := s.promotePendingDeliveredLocked(c.userID)
	peers := make([]string, 0)
	for _, conv := range s.conversations {
		if conv.UserA == c.userID {
			peers = append(peers, conv.UserB)
		} else if conv.UserB == c.userID {
			peers = append(peers, conv.UserA)
		}
	}
	s.mu.Unlock()

	for _, ev := range receiptEvents {
		s.pushWSEvent(ev.TargetUserID, ev.Event)
	}
	for _, peerID := range peers {
		if s.canSeePresence(c.userID, peerID) {
			s.pushWSEvent(peerID, WebSocketEvent{
				Event:         "presence",
				UserID:        c.userID,
				IsOnline:      true,
				LastSeenMs:    now,
				LastSeenLabel: "online",
				TimestampMs:   now,
			})
		}
	}
}

func (s *Service) DisconnectUser(userID string) {
	s.disconnectMatchingWS(userID, "")
}

func (s *Service) DisconnectUserDevice(userID, deviceFP string) {
	s.disconnectMatchingWS(userID, deviceFP)
}

func (s *Service) disconnectMatchingWS(userID, deviceFP string) {
	if userID == "" {
		return
	}
	s.mu.Lock()
	set := s.wsClients[userID]
	targets := make([]*wsClient, 0, len(set))
	for c := range set {
		if deviceFP == "" || c.deviceFP == deviceFP {
			targets = append(targets, c)
		}
	}
	s.mu.Unlock()

	for _, c := range targets {
		_ = c.writeFrame(0x8, nil)
		s.unregisterWSClient(c)
	}
}

func (s *Service) unregisterWSClient(c *wsClient) {
	c.close()
	now := s.nowMs()
	s.mu.Lock()
	if set := s.wsClients[c.userID]; set != nil {
		delete(set, c)
		if len(set) == 0 {
			delete(s.wsClients, c.userID)
		}
	}
	stillOnline := len(s.wsClients[c.userID]) > 0
	if !stillOnline {
		s.lastSeenMs[c.userID] = now
	}
	peers := make([]string, 0)
	if !stillOnline {
		for _, conv := range s.conversations {
			if conv.UserA == c.userID {
				peers = append(peers, conv.UserB)
			} else if conv.UserB == c.userID {
				peers = append(peers, conv.UserA)
			}
		}
	}
	s.mu.Unlock()

	for _, peerID := range peers {
		if s.canSeePresence(c.userID, peerID) {
			s.pushWSEvent(peerID, WebSocketEvent{
				Event:         "presence",
				UserID:        c.userID,
				IsOnline:      false,
				LastSeenMs:    now,
				LastSeenLabel: "last seen just now",
				TimestampMs:   now,
			})
		}
	}
}

func (s *Service) isOnlineLocked(userID string) bool {
	return len(s.wsClients[userID]) > 0
}

func (s *Service) canSeePresence(owner, viewer string) bool {
	if s.hooks.GetPrivacy == nil {
		return false
	}
	pv := s.hooks.GetPrivacy(owner)
	return s.allowsRule(owner, viewer, pv.OnlineIndicators)
}

func (s *Service) pushWSEvent(targetUserID string, ev WebSocketEvent) {
	s.mu.Lock()
	set := s.wsClients[targetUserID]
	clients := make([]*wsClient, 0, len(set))
	for c := range set {
		clients = append(clients, c)
	}
	s.mu.Unlock()

	for _, c := range clients {
		if err := c.sendJSON(ev); err != nil {
			s.unregisterWSClient(c)
		}
	}
}

func (s *Service) SendRawWSEvent(targetUserID string, payload any) {
	if strings.TrimSpace(targetUserID) == "" || payload == nil {
		return
	}
	s.mu.Lock()
	set := s.wsClients[targetUserID]
	clients := make([]*wsClient, 0, len(set))
	for c := range set {
		clients = append(clients, c)
	}
	s.mu.Unlock()

	for _, c := range clients {
		if err := c.sendJSON(payload); err != nil {
			s.unregisterWSClient(c)
		}
	}
}

func (s *Service) SetCallSignalHook(fn func(callerID string, rawPayload []byte)) {
	s.mu.Lock()
	defer s.mu.Unlock()
	s.hooks.OnCallSignalWS = fn
}

func (s *Service) IsUserOnline(userID string) bool {
	s.mu.Lock()
	defer s.mu.Unlock()
	return s.isOnlineLocked(strings.TrimSpace(userID))
}

func (s *Service) CanCall(callerID, peerID string) bool {
	if callerID == "" || peerID == "" || callerID == peerID {
		return false
	}
	if s.hooks.IsBlocked != nil && s.hooks.IsBlocked(callerID, peerID) {
		return false
	}
	if s.hooks.GetPrivacy == nil {
		return true
	}
	peerPv := s.hooks.GetPrivacy(peerID)
	return s.allowsRule(peerID, callerID, peerPv.WhoCanCall)
}

func (s *Service) AppendSystemCallMessage(convID, senderID, peerID, messageType, metaJSON string) (MessageDTO, error) {
	cleanType := normalizeMsgType(messageType)
	if cleanType != "call_log" && cleanType != "call_request" {
		cleanType = "call_log"
	}
	encoded := base64.StdEncoding.EncodeToString([]byte(metaJSON))
	now := s.nowMs()

	s.mu.Lock()
	c, ok := s.conversations[convID]
	if !ok {
		s.mu.Unlock()
		return MessageDTO{}, ErrNotFound
	}
	if peerID == "" {
		if senderID == c.UserA {
			peerID = c.UserB
		} else {
			peerID = c.UserA
		}
	}
	initialStatus := "delivered"
	if cleanType == "call_request" && !s.isOnlineLocked(peerID) {
		initialStatus = "sent"
	}
	dto := MessageDTO{
		MessageID:           newRandomID("msg"),
		ConversationID:      convID,
		SenderUserID:        senderID,
		RecipientUserID:     peerID,
		ClientOpID:          newRandomID("callop"),
		MessageType:         cleanType,
		CompressionCodec:    "none",
		EncryptionScheme:    "telefam_call_meta_v1",
		CiphertextB64:       encoded,
		SenderCiphertextB64: encoded,
		SignalMsgType:       1,
		DeliveryStatus:      initialStatus,
		CreatedAtMs:         now,
	}
	rec := &storedMessage{DTO: dto}
	s.messages[convID] = append(s.messages[convID], rec)
	s.messageIndex[dto.MessageID] = rec

	c.LastMessageID = dto.MessageID
	c.LastMessageType = dto.MessageType
	c.LastSenderID = senderID
	c.LastCiphertext = dto.CiphertextB64
	c.LastSenderCipher = dto.SenderCiphertextB64
	c.LastEncryptionScheme = dto.EncryptionScheme
	c.LastSignalMsgType = dto.SignalMsgType
	c.LastDelivery = dto.DeliveryStatus
	c.LastMessageAtMs = now
	c.UpdatedAtMs = now

	recipientMuted := false
	if peerID == c.UserA {
		c.UnreadA++
		c.ArchivedA = false
		recipientMuted = c.MutedA
	} else {
		c.UnreadB++
		c.ArchivedB = false
		recipientMuted = c.MutedB
	}
	onSent := s.hooks.OnMessageSent
	s.mu.Unlock()

	for _, uid := range []string{peerID, senderID} {
		s.pushWSEvent(uid, WebSocketEvent{
			Event:          "message_new",
			ConversationID: convID,
			UserID:         senderID,
			MessageID:      dto.MessageID,
			Message:        &dto,
			TimestampMs:    now,
		})
	}
	if cleanType == "call_request" && onSent != nil {
		onSent(dto, recipientMuted)
	}
	return dto, nil
}

// AppendStarGiftMessage persists a backend-verified animated star gift message in the 1:1 conversation
// and broadcasts message_new over WebSocket to both participants.
func (s *Service) AppendStarGiftMessage(convID, senderID, peerID, metaJSON, clientOpID string) (MessageDTO, error) {
	encoded := base64.StdEncoding.EncodeToString([]byte(metaJSON))
	now := s.nowMs()

	s.mu.Lock()
	c, ok := s.conversations[convID]
	if !ok {
		s.mu.Unlock()
		return MessageDTO{}, ErrNotFound
	}
	if peerID == "" {
		if senderID == c.UserA {
			peerID = c.UserB
		} else {
			peerID = c.UserA
		}
	}
	opID := strings.TrimSpace(clientOpID)
	if opID == "" {
		opID = newRandomID("starop")
	}
	if existing, exists := s.clientOpIndex[opID]; exists {
		dto := existing.DTO
		s.mu.Unlock()
		return dto, nil
	}

	initialStatus := "sent"
	if s.isOnlineLocked(peerID) {
		initialStatus = "delivered"
	}
	dto := MessageDTO{
		MessageID:           newRandomID("msg"),
		ConversationID:      convID,
		SenderUserID:        senderID,
		RecipientUserID:     peerID,
		ClientOpID:          opID,
		MessageType:         "star_gift",
		CompressionCodec:    "none",
		EncryptionScheme:    "telefam_star_meta_v1",
		CiphertextB64:       encoded,
		SenderCiphertextB64: encoded,
		SignalMsgType:       1,
		DeliveryStatus:      initialStatus,
		CreatedAtMs:         now,
	}
	rec := &storedMessage{DTO: dto}
	s.messages[convID] = append(s.messages[convID], rec)
	s.messageIndex[dto.MessageID] = rec
	s.clientOpIndex[opID] = rec

	c.LastMessageID = dto.MessageID
	c.LastMessageType = dto.MessageType
	c.LastSenderID = senderID
	c.LastCiphertext = dto.CiphertextB64
	c.LastSenderCipher = dto.SenderCiphertextB64
	c.LastEncryptionScheme = dto.EncryptionScheme
	c.LastSignalMsgType = dto.SignalMsgType
	c.LastDelivery = dto.DeliveryStatus
	c.LastMessageAtMs = now
	c.UpdatedAtMs = now

	if peerID == c.UserA {
		c.UnreadA++
		c.ArchivedA = false
	} else {
		c.UnreadB++
		c.ArchivedB = false
	}
	s.mu.Unlock()

	for _, uid := range []string{peerID, senderID} {
		s.pushWSEvent(uid, WebSocketEvent{
			Event:          "message_new",
			ConversationID: convID,
			UserID:         senderID,
			MessageID:      dto.MessageID,
			Message:        &dto,
			TimestampMs:    now,
		})
	}
	return dto, nil
}

func (s *Service) toConversationDTOLocked(callerID string, c *storedConversation) ConversationDTO {
	isA := callerID == c.UserA
	peerID := c.UserB
	archived := c.ArchivedA
	muted := c.MutedA
	unread := c.UnreadA
	if !isA {
		peerID = c.UserA
		archived = c.ArchivedB
		muted = c.MutedB
		unread = c.UnreadB
	}

	var peerInfo UserInfo
	if s.hooks.ResolveUser != nil {
		peerInfo, _ = s.hooks.ResolveUser(peerID)
	}
	if peerInfo.FullName == "" {
		peerInfo.FullName = peerID
	}

	caps := ChatPeerCapabilitiesDTO{
		CanCall:          true,
		CanScreenshot:    false,
		CanShareChats:    false,
		CanCopyMessages:  false,
		CanDownloadMedia: true,
		ShowReadReceipts: true,
	}
	if s.hooks.IsBlocked != nil {
		caps.IsBlocked = s.hooks.IsBlocked(callerID, peerID)
	}
	caps.IsMuted = muted

	showOnline := false
	if s.hooks.GetPrivacy != nil {
		peerPv := s.hooks.GetPrivacy(peerID)
		callerPv := s.hooks.GetPrivacy(callerID)
		caps.CanCall = s.allowsRule(peerID, callerID, peerPv.WhoCanCall)
		caps.CanScreenshot = s.allowsRule(peerID, callerID, peerPv.WhoCanScreenshot) &&
			s.allowsRule(callerID, peerID, callerPv.WhoCanScreenshot)
		caps.CanShareChats = s.allowsRule(peerID, callerID, peerPv.WhoCanShareChats) &&
			s.allowsRule(callerID, peerID, callerPv.WhoCanShareChats)
		caps.CanCopyMessages = s.allowsRule(peerID, callerID, peerPv.WhoCanCopyMessages) &&
			s.allowsRule(callerID, peerID, callerPv.WhoCanCopyMessages)
		caps.CanDownloadMedia = s.allowsRule(peerID, callerID, peerPv.WhoCanDownloadMedia)
		caps.ShowReadReceipts = s.allowsRule(peerID, callerID, peerPv.ReadReceipts) &&
			s.allowsRule(callerID, peerID, callerPv.ReadReceipts)
		caps.ShowTypingIndicator = s.allowsRule(peerID, callerID, peerPv.TypingIndicators)
		caps.ShowOnlineIndicator = s.allowsRule(peerID, callerID, peerPv.OnlineIndicators)
		showOnline = caps.ShowOnlineIndicator
	}

	online := false
	lastSeen := int64(0)
	lastSeenLabel := "last seen recently"
	if showOnline {
		online = s.isOnlineLocked(peerID)
		lastSeen = s.lastSeenMs[peerID]
		if online {
			lastSeenLabel = "online"
		} else if lastSeen > 0 {
			diff := time.Duration(s.nowMs()-lastSeen) * time.Millisecond
			switch {
			case diff < time.Minute:
				lastSeenLabel = "last seen just now"
			case diff < time.Hour:
				lastSeenLabel = fmt.Sprintf("last seen %dm ago", int(diff.Minutes()))
			default:
				lastSeenLabel = "last seen recently"
			}
		}
	}

	return ConversationDTO{
		ConversationID:      c.ConversationID,
		PeerUserID:          peerID,
		PeerFullName:        peerInfo.FullName,
		PeerUsername:        peerInfo.Username,
		PeerAvatarURL:       peerInfo.AvatarURL,
		LastMessageID:       c.LastMessageID,
		LastMessageType:     c.LastMessageType,
		LastSenderID:        c.LastSenderID,
		LastCiphertextB64:    c.LastCiphertext,
		LastSenderCipherB64:  c.LastSenderCipher,
		LastEncryptionScheme: c.LastEncryptionScheme,
		LastSignalMsgType:    c.LastSignalMsgType,
		LastDeliveryStatus:   c.LastDelivery,
		LastMessageAtMs:     c.LastMessageAtMs,
		UnreadCount:         unread,
		IsArchived:          archived,
		IsMuted:             muted,
		IsOnline:            online,
		LastSeenMs:          lastSeen,
		LastSeenLabel:       lastSeenLabel,
		Permissions:         caps,
		CreatedAtMs:         c.CreatedAtMs,
		UpdatedAtMs:         c.UpdatedAtMs,
	}
}

func normalizeMsgType(raw string) string {
	switch strings.ToLower(strings.TrimSpace(raw)) {
	case "sticker", "gif", "clip", "voice", "voice_once", "call_log", "call_request", "star_gift":
		return strings.ToLower(strings.TrimSpace(raw))
	default:
		return "text"
	}
}
