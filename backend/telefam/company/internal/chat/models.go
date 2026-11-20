package chat

type OneTimePreKeyDTO struct {
	KeyID        int    `json:"key_id"`
	PublicKeyB64 string `json:"public_key_b64"`
}

type E2eeKeyBundleUploadPayload struct {
	RegistrationID     int                `json:"registration_id"`
	IdentityPubB64     string             `json:"identity_pub_b64"`
	SignedPreKeyID     int                `json:"signed_prekey_id"`
	SignedPreKeyPubB64 string             `json:"signed_prekey_pub_b64"`
	SignedPreKeySigB64 string             `json:"signed_prekey_sig_b64"`
	KyberPreKeyID      int                `json:"kyber_prekey_id"`
	KyberPreKeyPubB64  string             `json:"kyber_prekey_pub_b64"`
	KyberPreKeySigB64  string             `json:"kyber_prekey_sig_b64"`
	OneTimePreKeys     []OneTimePreKeyDTO `json:"one_time_prekeys"`
}

type E2eeKeyBundleResponse struct {
	Success            bool   `json:"success"`
	UserID             string `json:"user_id"`
	DeviceID           int    `json:"device_id"`
	RegistrationID     int    `json:"registration_id"`
	IdentityPubB64     string `json:"identity_pub_b64"`
	SignedPreKeyID     int    `json:"signed_prekey_id"`
	SignedPreKeyPubB64 string `json:"signed_prekey_pub_b64"`
	SignedPreKeySigB64 string `json:"signed_prekey_sig_b64"`
	KyberPreKeyID      int    `json:"kyber_prekey_id"`
	KyberPreKeyPubB64  string `json:"kyber_prekey_pub_b64"`
	KyberPreKeySigB64  string `json:"kyber_prekey_sig_b64"`
	PreKeyID           int    `json:"prekey_id,omitempty"`
	PreKeyPubB64       string `json:"prekey_pub_b64,omitempty"`
	ServerEpochMs      int64  `json:"server_epoch_ms"`
}

type ChatPeerCapabilitiesDTO struct {
	CanCall             bool `json:"can_call"`
	CanScreenshot       bool `json:"can_screenshot"`
	CanShareChats       bool `json:"can_share_chats"`
	CanCopyMessages     bool `json:"can_copy_messages"`
	CanDownloadMedia    bool `json:"can_download_media"`
	ShowReadReceipts    bool `json:"show_read_receipts"`
	ShowTypingIndicator bool `json:"show_typing_indicator"`
	ShowOnlineIndicator bool `json:"show_online_indicator"`
	IsBlocked           bool `json:"is_blocked"`
	IsMuted             bool `json:"is_muted"`
}

type ConversationDTO struct {
	ConversationID        string                  `json:"conversation_id"`
	PeerUserID            string                  `json:"peer_user_id"`
	PeerFullName          string                  `json:"peer_full_name"`
	PeerUsername          string                  `json:"peer_username"`
	PeerAvatarURL         string                  `json:"peer_avatar_url,omitempty"`
	LastMessageID         string                  `json:"last_message_id,omitempty"`
	LastMessageType       string                  `json:"last_message_type,omitempty"`
	LastSenderID          string                  `json:"last_sender_id,omitempty"`
	LastCiphertextB64     string                  `json:"last_ciphertext_b64,omitempty"`
	LastSenderCipherB64   string                  `json:"last_sender_ciphertext_b64,omitempty"`
	LastEncryptionScheme  string                  `json:"last_encryption_scheme,omitempty"`
	LastSignalMsgType     int                     `json:"last_signal_msg_type,omitempty"`
	LastDeliveryStatus    string                  `json:"last_delivery_status,omitempty"`
	LastMessageAtMs       int64                   `json:"last_message_at_ms"`
	UnreadCount           int                     `json:"unread_count"`
	IsArchived            bool                    `json:"is_archived"`
	IsMuted               bool                    `json:"is_muted"`
	IsOnline              bool                    `json:"is_online"`
	LastSeenMs            int64                   `json:"last_seen_ms"`
	LastSeenLabel         string                  `json:"last_seen_label"`
	Permissions           ChatPeerCapabilitiesDTO `json:"permissions"`
	CreatedAtMs           int64                   `json:"created_at_ms"`
	UpdatedAtMs           int64                   `json:"updated_at_ms"`
}

type MessageDTO struct {
	MessageID            string `json:"message_id"`
	ConversationID       string `json:"conversation_id"`
	SenderUserID         string `json:"sender_user_id"`
	RecipientUserID      string `json:"recipient_user_id"`
	ClientOpID           string `json:"client_op_id"`
	MessageType          string `json:"message_type"` // text | sticker | gif | clip | voice | voice_once | call_log | call_request
	CompressionCodec     string `json:"compression_codec"`
	EncryptionScheme     string `json:"encryption_scheme"`
	CiphertextB64        string `json:"ciphertext_b64"`
	SenderCiphertextB64  string `json:"sender_ciphertext_b64"`
	SignalMsgType        int    `json:"signal_msg_type"`
	MediaURL             string `json:"media_url,omitempty"`
	MediaID              string `json:"media_id,omitempty"`
	VoiceDurationMs      int64  `json:"voice_duration_ms,omitempty"`
	ViewOnceTTLSec       int    `json:"view_once_ttl_sec,omitempty"`
	ViewOnceOpenedAtMs   int64  `json:"view_once_opened_at_ms,omitempty"`
	ViewOnceExpiresAtMs  int64  `json:"view_once_expires_at_ms,omitempty"`
	ForwardedFromUserID  string `json:"forwarded_from_user_id,omitempty"`
	DeliveryStatus       string `json:"delivery_status"` // sent | delivered | seen
	EditedAtMs           int64  `json:"edited_at_ms,omitempty"`
	CreatedAtMs          int64  `json:"created_at_ms"`
}

type SendMessagePayload struct {
	ClientOpID          string `json:"client_op_id"`
	MessageType         string `json:"message_type"`
	CompressionCodec    string `json:"compression_codec"`
	EncryptionScheme    string `json:"encryption_scheme"`
	CiphertextB64       string `json:"ciphertext_b64"`
	SenderCiphertextB64 string `json:"sender_ciphertext_b64"`
	SignalMsgType       int    `json:"signal_msg_type"`
	MediaURL            string `json:"media_url,omitempty"`
	MediaID             string `json:"media_id,omitempty"`
	VoiceDurationMs     int64  `json:"voice_duration_ms,omitempty"`
	ViewOnceTTLSec      int    `json:"view_once_ttl_sec,omitempty"`
	ForwardedFromUserID string `json:"forwarded_from_user_id,omitempty"`
}

type EditMessagePayload struct {
	CiphertextB64       string `json:"ciphertext_b64"`
	SenderCiphertextB64 string `json:"sender_ciphertext_b64"`
	SignalMsgType       int    `json:"signal_msg_type"`
}

type ReceiptPayload struct {
	Status string `json:"status"` // delivered | seen
}

type BatchConversationActionPayload struct {
	ConversationIDs []string `json:"conversation_ids"`
	Action          string   `json:"action"` // delete | archive | unarchive | mute | unmute | block | mark_read | mark_unread
}

type SecurityFlagPayload struct {
	MessageID string `json:"message_id,omitempty"`
	FlagType  string `json:"flag_type"` // screenshot_disallowed | view_once_opened | view_once_capture_attempt
	Details   string `json:"details,omitempty"`
}

type SecurityFlagDTO struct {
	FlagID         string `json:"flag_id"`
	ConversationID string `json:"conversation_id"`
	MessageID      string `json:"message_id,omitempty"`
	ActorUserID    string `json:"actor_user_id"`
	TargetUserID   string `json:"target_user_id"`
	FlagType       string `json:"flag_type"`
	Details        string `json:"details,omitempty"`
	CreatedAtMs    int64  `json:"created_at_ms"`
}

type GiflyMediaItem struct {
	ID          int64    `json:"id"`
	Slug        string   `json:"slug"`
	Title       string   `json:"title"`
	Type        string   `json:"type"`
	Tags        []string `json:"tags"`
	Preview     string   `json:"preview"`
	PreviewWebp string   `json:"preview_webp"`
	URL         string   `json:"url"`
	URLHD       string   `json:"url_hd"`
	MP4         string   `json:"mp4,omitempty"`
	Width       int      `json:"width"`
	Height      int      `json:"height"`
}

type GiflySearchResponse struct {
	Success bool             `json:"success"`
	Type    string           `json:"type"`
	Query   string           `json:"query"`
	Page    int              `json:"page"`
	PerPage int              `json:"per_page"`
	HasNext bool             `json:"has_next"`
	Count   int              `json:"count"`
	Items   []GiflyMediaItem `json:"items"`
}

type ChatSearchPersonDTO struct {
	UserID         string `json:"user_id"`
	FullName       string `json:"full_name"`
	Username       string `json:"username"`
	AvatarURL      string `json:"avatar_url,omitempty"`
	Location       string `json:"location,omitempty"`
	ConversationID string `json:"conversation_id,omitempty"`
}

type WebSocketEvent struct {
	Event          string           `json:"event"`
	ConversationID string           `json:"conversation_id,omitempty"`
	UserID         string           `json:"user_id,omitempty"`
	MessageID      string           `json:"message_id,omitempty"`
	Status         string           `json:"status,omitempty"`
	TypingMode     string           `json:"typing_mode,omitempty"` // typing | recording | idle
	IsOnline       bool             `json:"is_online,omitempty"`
	LastSeenMs     int64            `json:"last_seen_ms,omitempty"`
	LastSeenLabel  string           `json:"last_seen_label,omitempty"`
	Message        *MessageDTO      `json:"message,omitempty"`
	Flag           *SecurityFlagDTO `json:"flag,omitempty"`
	TimestampMs    int64            `json:"timestamp_ms"`
}
