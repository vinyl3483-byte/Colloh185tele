package calls

import (
	"context"
	"crypto/hmac"
	"crypto/rand"
	"crypto/sha1"
	"encoding/base64"
	"encoding/hex"
	"encoding/json"
	"errors"
	"fmt"
	"os"
	"strconv"
	"strings"
	"sync"
	"time"
)

var (
	ErrForbidden     = errors.New("forbidden")
	ErrNotFound      = errors.New("not found")
	ErrBadRequest    = errors.New("bad request")
	ErrCallNotActive = errors.New("call already ended")
)

const DefaultRingTimeout = 25 * time.Second

type IceServerDTO struct {
	URLs       []string `json:"urls"`
	Username   string   `json:"username,omitempty"`
	Credential string   `json:"credential,omitempty"`
}

type IceConfigResponse struct {
	Success    bool           `json:"success"`
	IceServers []IceServerDTO `json:"ice_servers"`
	TimeoutSec int            `json:"timeout_sec"`
}

type ParticipantInfo struct {
	UserID    string `json:"user_id"`
	FullName  string `json:"full_name"`
	Username  string `json:"username"`
	AvatarURL string `json:"avatar_url,omitempty"`
}

type CallRecordDTO struct {
	CallID         string `json:"call_id"`
	ConversationID string `json:"conversation_id"`
	CallerUserID   string `json:"caller_user_id"`
	CalleeUserID   string `json:"callee_user_id"`
	CallerName     string `json:"caller_name,omitempty"`
	CallerUsername string `json:"caller_username,omitempty"`
	CallerAvatar   string `json:"caller_avatar,omitempty"`
	CalleeName     string `json:"callee_name,omitempty"`
	CalleeUsername string `json:"callee_username,omitempty"`
	CalleeAvatar   string `json:"callee_avatar,omitempty"`
	CallType       string `json:"call_type"` // audio | video
	State          string `json:"state"`     // initiated | ringing | connected | ended | missed | declined | busy | cancelled | not_allowed
	EndReason      string `json:"end_reason,omitempty"`
	VideoUpgraded  bool   `json:"video_upgraded"`
	LogMessageID   string `json:"log_message_id,omitempty"`
	StartedAtMs    int64  `json:"started_at_ms"`
	RingingAtMs    int64  `json:"ringing_at_ms,omitempty"`
	AnsweredAtMs   int64  `json:"answered_at_ms,omitempty"`
	EndedAtMs      int64  `json:"ended_at_ms,omitempty"`
	DurationSec    int64  `json:"duration_sec"`
	ExpiresAtMs    int64  `json:"expires_at_ms,omitempty"`
	ServerEpochMs  int64  `json:"server_epoch_ms"`
}

type StartCallPayload struct {
	PeerUserID     string `json:"peer_user_id"`
	ConversationID string `json:"conversation_id,omitempty"`
	CallType       string `json:"call_type"` // audio | video
	SDPOffer       string `json:"sdp_offer,omitempty"`
}

type StartCallResponse struct {
	Success         bool           `json:"success"`
	Allowed         bool           `json:"allowed"`
	Code            string         `json:"code,omitempty"`
	Message         string         `json:"message,omitempty"`
	PeerOnline      bool           `json:"peer_online"`
	AutoRequestSent bool           `json:"auto_request_sent,omitempty"`
	Call            CallRecordDTO  `json:"call"`
	IceServers      []IceServerDTO `json:"ice_servers,omitempty"`
	TimeoutSec      int            `json:"timeout_sec"`
}

type CallSignalPayload struct {
	Action         string `json:"action"` // ringing | accept | decline | end | offer | answer | ice | media_state | switch_video_request | switch_video_accept | switch_video_decline
	SDP            string `json:"sdp,omitempty"`
	SDPType        string `json:"sdp_type,omitempty"`
	Candidate      string `json:"candidate,omitempty"`
	SDPMid         string `json:"sdp_mid,omitempty"`
	SDPMLineIndex  int    `json:"sdp_mline_index,omitempty"`
	Reason         string `json:"reason,omitempty"`
	CamEnabled     *bool  `json:"cam_enabled,omitempty"`
	MicEnabled     *bool  `json:"mic_enabled,omitempty"`
	InAppActive    *bool  `json:"in_app_active,omitempty"`
	ScreenSharing  *bool  `json:"screen_sharing,omitempty"`
	VideoFilter    string `json:"video_filter,omitempty"`
	NetworkQuality string `json:"network_quality,omitempty"`
}

type BufferedSignalEvent struct {
	Seq            int64  `json:"seq"`
	FromUserID     string `json:"from_user_id"`
	Action         string `json:"action"`
	SDP            string `json:"sdp,omitempty"`
	SDPType        string `json:"sdp_type,omitempty"`
	Candidate      string `json:"candidate,omitempty"`
	SDPMid         string `json:"sdp_mid,omitempty"`
	SDPMLineIndex  int    `json:"sdp_mline_index,omitempty"`
	CamEnabled     *bool  `json:"cam_enabled,omitempty"`
	MicEnabled     *bool  `json:"mic_enabled,omitempty"`
	InAppActive    *bool  `json:"in_app_active,omitempty"`
	ScreenSharing  *bool  `json:"screen_sharing,omitempty"`
	VideoFilter    string `json:"video_filter,omitempty"`
	NetworkQuality string `json:"network_quality,omitempty"`
	TimestampMs    int64  `json:"timestamp_ms"`
}

type HostHooks struct {
	ResolveUser         func(userID string) (ParticipantInfo, bool)
	IsBlocked           func(a, b string) bool
	CanCall             func(callerID, calleeID string) bool
	OpenDirectConv      func(callerID, peerID string) (string, error)
	IsUserOnlineWS      func(userID string) bool
	SendWSEvent         func(targetUserID string, event map[string]any)
	AppendChatMessage   func(convID, senderID, peerID, messageType, metaJSON string) (string, error)
	NotifyIncomingCall  func(ctx context.Context, call CallRecordDTO)
	NotifyCallCancelled func(ctx context.Context, recipientID, callID, convID, reason string)
	NotifyMissedCall    func(ctx context.Context, call CallRecordDTO)
}

type storedCall struct {
	DTO       CallRecordDTO
	LastOffer string
	Signals   []BufferedSignalEvent
	Timer     *time.Timer
}

type Service struct {
	hooks       HostHooks
	mu          sync.Mutex
	calls       map[string]*storedCall
	activeByUsr map[string]string
	seqCounter  int64
	ringTimeout time.Duration
	nowFunc     func() time.Time
}

func NewService(hooks HostHooks) *Service {
	return &Service{
		hooks:       hooks,
		calls:       make(map[string]*storedCall),
		activeByUsr: make(map[string]string),
		ringTimeout: DefaultRingTimeout,
		nowFunc:     time.Now,
	}
}

func (s *Service) SetRingTimeoutForTest(d time.Duration) {
	s.mu.Lock()
	defer s.mu.Unlock()
	if d > 0 {
		s.ringTimeout = d
	}
}

func (s *Service) IceServersForUser(userID string) IceConfigResponse {
	servers := []IceServerDTO{
		{URLs: []string{"stun:stun.l.google.com:19302", "stun:stun1.l.google.com:19302"}},
	}
	turnURLsRaw := strings.TrimSpace(os.Getenv("TELEFAM_TURN_URLS"))
	turnSecret := strings.TrimSpace(os.Getenv("TELEFAM_TURN_SECRET"))
	if turnURLsRaw != "" {
		urls := make([]string, 0, 4)
		for _, item := range strings.Split(turnURLsRaw, ",") {
			if clean := strings.TrimSpace(item); clean != "" {
				urls = append(urls, clean)
			}
		}
		if len(urls) > 0 {
			entry := IceServerDTO{URLs: urls}
			if turnSecret != "" {
				exp := s.nowFunc().UTC().Unix() + 12*3600
				uname := fmt.Sprintf("%d:%s", exp, userID)
				mac := hmac.New(sha1.New, []byte(turnSecret))
				_, _ = mac.Write([]byte(uname))
				entry.Username = uname
				entry.Credential = base64.StdEncoding.EncodeToString(mac.Sum(nil))
			} else {
				entry.Username = strings.TrimSpace(os.Getenv("TELEFAM_TURN_USERNAME"))
				entry.Credential = strings.TrimSpace(os.Getenv("TELEFAM_TURN_CREDENTIAL"))
			}
			servers = append(servers, entry)
		}
	}
	s.mu.Lock()
	toSec := int(s.ringTimeout.Seconds())
	s.mu.Unlock()
	if toSec <= 0 {
		toSec = 25
	}
	return IceConfigResponse{
		Success:    true,
		IceServers: servers,
		TimeoutSec: toSec,
	}
}

func (s *Service) StartCall(ctx context.Context, callerID string, req StartCallPayload) (StartCallResponse, error) {
	peerID := strings.TrimSpace(req.PeerUserID)
	if callerID == "" || peerID == "" || callerID == peerID {
		return StartCallResponse{}, ErrBadRequest
	}
	callType := strings.ToLower(strings.TrimSpace(req.CallType))
	if callType != "video" {
		callType = "audio"
	}
	if s.hooks.IsBlocked != nil && s.hooks.IsBlocked(callerID, peerID) {
		return StartCallResponse{}, ErrForbidden
	}

	convID := strings.TrimSpace(req.ConversationID)
	if convID == "" && s.hooks.OpenDirectConv != nil {
		opened, err := s.hooks.OpenDirectConv(callerID, peerID)
		if err != nil {
			return StartCallResponse{}, err
		}
		convID = opened
	}
	if convID == "" {
		return StartCallResponse{}, ErrBadRequest
	}

	callerInfo := s.resolveInfo(callerID)
	calleeInfo := s.resolveInfo(peerID)
	now := s.nowMs()
	callID := randomCallID("call")

	// Check peer privacy ("Who can call me")
	allowed := true
	if s.hooks.CanCall != nil {
		allowed = s.hooks.CanCall(callerID, peerID)
	}
	if !allowed {
		dto := CallRecordDTO{
			CallID:         callID,
			ConversationID: convID,
			CallerUserID:   callerID,
			CalleeUserID:   peerID,
			CallerName:     callerInfo.FullName,
			CallerUsername: callerInfo.Username,
			CallerAvatar:   callerInfo.AvatarURL,
			CalleeName:     calleeInfo.FullName,
			CalleeUsername: calleeInfo.Username,
			CalleeAvatar:   calleeInfo.AvatarURL,
			CallType:       callType,
			State:          "not_allowed",
			EndReason:      "calls_not_allowed",
			StartedAtMs:    now,
			EndedAtMs:      now,
			DurationSec:    0,
			ServerEpochMs:  now,
		}
		// Auto-insert a call_log AND a "Call me" request message directly inside the 1-on-1 chat.
		if s.hooks.AppendChatMessage != nil {
			logMeta := buildCallMetaJSON(dto, "Calls are not allowed. I tried to call you.")
			if msgID, err := s.hooks.AppendChatMessage(convID, callerID, peerID, "call_log", logMeta); err == nil {
				dto.LogMessageID = msgID
			}
			reqText := "Calls are restricted on your privacy settings. Please call me back when you're free."
			if callType == "video" {
				reqText = "Video calls are restricted on your privacy settings. Please call me back when you're free."
			}
			reqMeta := buildCallRequestJSON(dto, reqText)
			_, _ = s.hooks.AppendChatMessage(convID, callerID, peerID, "call_request", reqMeta)
		}
		s.mu.Lock()
		s.calls[callID] = &storedCall{DTO: dto}
		s.mu.Unlock()
		return StartCallResponse{
			Success:         true,
			Allowed:         false,
			Code:            "calls_not_allowed",
			Message:         "Calls are not allowed by this user's privacy settings. Call-me request sent.",
			PeerOnline:      false,
			AutoRequestSent: true,
			Call:            dto,
			TimeoutSec:      25,
		}, nil
	}

	s.mu.Lock()
	timeout := s.ringTimeout
	timeoutMs := timeout.Milliseconds()
	// Check if callee is already on another active call -> busy
	if existingCallID, hasActive := s.activeByUsr[peerID]; hasActive && existingCallID != "" {
		dto := CallRecordDTO{
			CallID:         callID,
			ConversationID: convID,
			CallerUserID:   callerID,
			CalleeUserID:   peerID,
			CallerName:     callerInfo.FullName,
			CallerUsername: callerInfo.Username,
			CallerAvatar:   callerInfo.AvatarURL,
			CalleeName:     calleeInfo.FullName,
			CalleeUsername: calleeInfo.Username,
			CalleeAvatar:   calleeInfo.AvatarURL,
			CallType:       callType,
			State:          "busy",
			EndReason:      "peer_busy",
			StartedAtMs:    now,
			EndedAtMs:      now,
			DurationSec:    0,
			ServerEpochMs:  now,
		}
		s.calls[callID] = &storedCall{DTO: dto}
		s.mu.Unlock()
		s.appendCallLogChatMessage(&dto, "Busy — I tried to call you.")
		return StartCallResponse{
			Success:    true,
			Allowed:    true,
			Code:       "peer_busy",
			Message:    "User is on another call.",
			PeerOnline: true,
			Call:       dto,
			TimeoutSec: int(timeout.Seconds()),
		}, nil
	}

	peerOnline := s.hooks.IsUserOnlineWS != nil && s.hooks.IsUserOnlineWS(peerID)
	initialState := "initiated"
	ringAt := int64(0)
	if peerOnline {
		initialState = "ringing"
		ringAt = now
	}

	dto := CallRecordDTO{
		CallID:         callID,
		ConversationID: convID,
		CallerUserID:   callerID,
		CalleeUserID:   peerID,
		CallerName:     callerInfo.FullName,
		CallerUsername: callerInfo.Username,
		CallerAvatar:   callerInfo.AvatarURL,
		CalleeName:     calleeInfo.FullName,
		CalleeUsername: calleeInfo.Username,
		CalleeAvatar:   calleeInfo.AvatarURL,
		CallType:       callType,
		State:          initialState,
		StartedAtMs:    now,
		RingingAtMs:    ringAt,
		ExpiresAtMs:    now + timeoutMs,
		ServerEpochMs:  now,
	}

	sc := &storedCall{
		DTO:       dto,
		LastOffer: strings.TrimSpace(req.SDPOffer),
	}
	if sc.LastOffer != "" {
		s.seqCounter++
		sc.Signals = append(sc.Signals, BufferedSignalEvent{
			Seq:         s.seqCounter,
			FromUserID:  callerID,
			Action:      "offer",
			SDP:         sc.LastOffer,
			SDPType:     "offer",
			TimestampMs: now,
		})
	}
	sc.Timer = time.AfterFunc(timeout, func() {
		s.expireUnansweredCall(callID)
	})
	s.calls[callID] = sc
	s.activeByUsr[callerID] = callID
	s.activeByUsr[peerID] = callID
	s.mu.Unlock()

	iceCfg := s.IceServersForUser(callerID)

	if s.hooks.SendWSEvent != nil {
		s.hooks.SendWSEvent(peerID, map[string]any{
			"event":           "call_incoming",
			"conversation_id": convID,
			"user_id":         callerID,
			"call":            dto,
			"sdp_offer":       sc.LastOffer,
			"ice_servers":     iceCfg.IceServers,
			"timestamp_ms":    now,
		})
		if peerOnline {
			s.hooks.SendWSEvent(callerID, map[string]any{
				"event":           "call_ringing",
				"conversation_id": convID,
				"user_id":         peerID,
				"call":            dto,
				"timestamp_ms":    now,
			})
		}
	}

	if s.hooks.NotifyIncomingCall != nil {
		s.hooks.NotifyIncomingCall(ctx, dto)
	}

	return StartCallResponse{
		Success:    true,
		Allowed:    true,
		PeerOnline: peerOnline,
		Call:       dto,
		IceServers: iceCfg.IceServers,
		TimeoutSec: iceCfg.TimeoutSec,
	}, nil
}

func (s *Service) GetCallState(callerID, callID string, sinceSeq int64) (CallRecordDTO, string, []BufferedSignalEvent, error) {
	s.mu.Lock()
	defer s.mu.Unlock()
	sc, ok := s.calls[strings.TrimSpace(callID)]
	if !ok {
		return CallRecordDTO{}, "", nil, ErrNotFound
	}
	if sc.DTO.CallerUserID != callerID && sc.DTO.CalleeUserID != callerID {
		return CallRecordDTO{}, "", nil, ErrForbidden
	}
	dto := sc.DTO
	dto.ServerEpochMs = s.nowMs()
	if dto.State == "connected" && dto.AnsweredAtMs > 0 {
		dto.DurationSec = (dto.ServerEpochMs - dto.AnsweredAtMs) / 1000
		if dto.DurationSec < 0 {
			dto.DurationSec = 0
		}
	}
	signals := make([]BufferedSignalEvent, 0)
	for _, sig := range sc.Signals {
		if sig.Seq > sinceSeq && sig.FromUserID != callerID {
			signals = append(signals, sig)
		}
	}
	return dto, sc.LastOffer, signals, nil
}

func (s *Service) HandleSignal(ctx context.Context, actorID, callID string, req CallSignalPayload) (CallRecordDTO, error) {
	action := strings.ToLower(strings.TrimSpace(req.Action))
	if action == "" {
		return CallRecordDTO{}, ErrBadRequest
	}
	now := s.nowMs()

	s.mu.Lock()
	sc, ok := s.calls[strings.TrimSpace(callID)]
	if !ok {
		s.mu.Unlock()
		return CallRecordDTO{}, ErrNotFound
	}
	if sc.DTO.CallerUserID != actorID && sc.DTO.CalleeUserID != actorID {
		s.mu.Unlock()
		return CallRecordDTO{}, ErrForbidden
	}
	peerID := sc.DTO.CalleeUserID
	if actorID == sc.DTO.CalleeUserID {
		peerID = sc.DTO.CallerUserID
	}

	isTerminal := isTerminalCallState(sc.DTO.State)
	if isTerminal && action != "end" && action != "decline" {
		dto := sc.DTO
		s.mu.Unlock()
		return dto, ErrCallNotActive
	}

	var shouldLogSummary string
	var notifyCancel bool
	var wsEvents []targetedCallWSEvent

	switch action {
	case "ringing":
		if sc.DTO.State == "initiated" {
			sc.DTO.State = "ringing"
			sc.DTO.RingingAtMs = now
			wsEvents = append(wsEvents, targetedCallWSEvent{
				TargetUserID: peerID,
				Payload: map[string]any{
					"event":           "call_ringing",
					"conversation_id": sc.DTO.ConversationID,
					"user_id":         actorID,
					"call":            sc.DTO,
					"timestamp_ms":    now,
				},
			})
		}

	case "accept", "answer":
		if sc.Timer != nil {
			sc.Timer.Stop()
			sc.Timer = nil
		}
		wasConnected := sc.DTO.State == "connected"
		sc.DTO.State = "connected"
		if sc.DTO.AnsweredAtMs == 0 {
			sc.DTO.AnsweredAtMs = now
		}
		if sdp := strings.TrimSpace(req.SDP); sdp != "" {
			s.seqCounter++
			ev := BufferedSignalEvent{
				Seq:         s.seqCounter,
				FromUserID:  actorID,
				Action:      "answer",
				SDP:         sdp,
				SDPType:     "answer",
				TimestampMs: now,
			}
			sc.Signals = append(sc.Signals, ev)
			wsEvents = append(wsEvents, targetedCallWSEvent{
				TargetUserID: peerID,
				Payload: map[string]any{
					"event":           "call_signal",
					"conversation_id": sc.DTO.ConversationID,
					"user_id":         actorID,
					"call":            sc.DTO,
					"signal":          ev,
					"timestamp_ms":    now,
				},
			})
		}
		if !wasConnected {
			wsEvents = append(wsEvents, targetedCallWSEvent{
				TargetUserID: peerID,
				Payload: map[string]any{
					"event":           "call_accepted",
					"conversation_id": sc.DTO.ConversationID,
					"user_id":         actorID,
					"call":            sc.DTO,
					"timestamp_ms":    now,
				},
			})
			notifyCancel = true
		}

	case "decline":
		if !isTerminal {
			if sc.Timer != nil {
				sc.Timer.Stop()
				sc.Timer = nil
			}
			sc.DTO.State = "declined"
			sc.DTO.EndReason = strings.TrimSpace(req.Reason)
			if sc.DTO.EndReason == "" {
				sc.DTO.EndReason = "declined"
			}
			sc.DTO.EndedAtMs = now
			sc.DTO.DurationSec = 0
			s.clearActivePairLocked(sc.DTO.CallerUserID, sc.DTO.CalleeUserID, sc.DTO.CallID)
			shouldLogSummary = "Call declined — I tried to call you."
			notifyCancel = true
			wsEvents = append(wsEvents,
				targetedCallWSEvent{
					TargetUserID: peerID,
					Payload: map[string]any{
						"event":           "call_ended",
						"conversation_id": sc.DTO.ConversationID,
						"user_id":         actorID,
						"call":            sc.DTO,
						"timestamp_ms":    now,
					},
				},
				targetedCallWSEvent{
					TargetUserID: actorID,
					Payload: map[string]any{
						"event":           "call_ended",
						"conversation_id": sc.DTO.ConversationID,
						"user_id":         actorID,
						"call":            sc.DTO,
						"timestamp_ms":    now,
					},
				},
			)
		}

	case "end", "cancel":
		if !isTerminal {
			if sc.Timer != nil {
				sc.Timer.Stop()
				sc.Timer = nil
			}
			reason := strings.TrimSpace(req.Reason)
			if sc.DTO.State == "connected" && sc.DTO.AnsweredAtMs > 0 {
				sc.DTO.State = "ended"
				sc.DTO.EndedAtMs = now
				dur := (now - sc.DTO.AnsweredAtMs) / 1000
				if dur < 1 && (now-sc.DTO.AnsweredAtMs) > 200 {
					dur = 1
				}
				if dur < 0 {
					dur = 0
				}
				sc.DTO.DurationSec = dur
				if reason == "" {
					reason = "hangup"
				}
				sc.DTO.EndReason = reason
				shouldLogSummary = fmt.Sprintf("Call duration %s", formatSeconds(dur))
			} else {
				if actorID == sc.DTO.CallerUserID {
					sc.DTO.State = "cancelled"
					if reason == "" {
						reason = "caller_cancelled"
					}
					shouldLogSummary = "Missed call — I tried to call you."
				} else {
					sc.DTO.State = "missed"
					if reason == "" {
						reason = "no_answer"
					}
					shouldLogSummary = "Missed call — I tried to call you."
				}
				sc.DTO.EndReason = reason
				sc.DTO.EndedAtMs = now
				sc.DTO.DurationSec = 0
			}
			s.clearActivePairLocked(sc.DTO.CallerUserID, sc.DTO.CalleeUserID, sc.DTO.CallID)
			notifyCancel = true
			for _, uid := range []string{peerID, actorID} {
				wsEvents = append(wsEvents, targetedCallWSEvent{
					TargetUserID: uid,
					Payload: map[string]any{
						"event":           "call_ended",
						"conversation_id": sc.DTO.ConversationID,
						"user_id":         actorID,
						"call":            sc.DTO,
						"timestamp_ms":    now,
					},
				})
			}
		}

	case "offer", "ice", "media_state", "switch_video_request", "switch_video_accept", "switch_video_decline":
		if action == "offer" && strings.TrimSpace(req.SDP) != "" {
			sc.LastOffer = strings.TrimSpace(req.SDP)
		}
		if action == "switch_video_accept" {
			sc.DTO.CallType = "video"
			sc.DTO.VideoUpgraded = true
		}
		s.seqCounter++
		ev := BufferedSignalEvent{
			Seq:            s.seqCounter,
			FromUserID:     actorID,
			Action:         action,
			SDP:            strings.TrimSpace(req.SDP),
			SDPType:        strings.TrimSpace(req.SDPType),
			Candidate:      strings.TrimSpace(req.Candidate),
			SDPMid:         strings.TrimSpace(req.SDPMid),
			SDPMLineIndex:  req.SDPMLineIndex,
			CamEnabled:     req.CamEnabled,
			MicEnabled:     req.MicEnabled,
			InAppActive:    req.InAppActive,
			ScreenSharing:  req.ScreenSharing,
			VideoFilter:    strings.TrimSpace(req.VideoFilter),
			NetworkQuality: strings.TrimSpace(req.NetworkQuality),
			TimestampMs:    now,
		}
		sc.Signals = append(sc.Signals, ev)
		if len(sc.Signals) > 256 {
			sc.Signals = sc.Signals[len(sc.Signals)-256:]
		}
		wsEvents = append(wsEvents, targetedCallWSEvent{
			TargetUserID: peerID,
			Payload: map[string]any{
				"event":           "call_signal",
				"conversation_id": sc.DTO.ConversationID,
				"user_id":         actorID,
				"call":            sc.DTO,
				"signal":          ev,
				"timestamp_ms":    now,
			},
		})
	default:
		s.mu.Unlock()
		return CallRecordDTO{}, ErrBadRequest
	}

	dto := sc.DTO
	dto.ServerEpochMs = now
	s.mu.Unlock()

	if shouldLogSummary != "" {
		s.appendCallLogChatMessage(&dto, shouldLogSummary)
		if dto.State == "missed" || dto.State == "cancelled" {
			if s.hooks.NotifyMissedCall != nil {
				s.hooks.NotifyMissedCall(ctx, dto)
			}
		}
	}
	if notifyCancel && s.hooks.NotifyCallCancelled != nil {
		s.hooks.NotifyCallCancelled(ctx, dto.CalleeUserID, dto.CallID, dto.ConversationID, dto.State)
	}
	if s.hooks.SendWSEvent != nil {
		for _, we := range wsEvents {
			s.hooks.SendWSEvent(we.TargetUserID, we.Payload)
		}
	}
	return dto, nil
}

func (s *Service) expireUnansweredCall(callID string) {
	now := s.nowMs()
	s.mu.Lock()
	sc, ok := s.calls[callID]
	if !ok || (sc.DTO.State != "initiated" && sc.DTO.State != "ringing") {
		s.mu.Unlock()
		return
	}
	sc.Timer = nil
	sc.DTO.State = "missed"
	sc.DTO.EndReason = "timeout_25s"
	sc.DTO.EndedAtMs = now
	sc.DTO.DurationSec = 0
	s.clearActivePairLocked(sc.DTO.CallerUserID, sc.DTO.CalleeUserID, sc.DTO.CallID)
	dto := sc.DTO
	dto.ServerEpochMs = now
	s.mu.Unlock()

	summary := "No answer — I tried to call you."
	if dto.CallType == "video" {
		summary = "No answer — I tried to video call you."
	}
	s.appendCallLogChatMessage(&dto, summary)

	if s.hooks.NotifyCallCancelled != nil {
		s.hooks.NotifyCallCancelled(context.Background(), dto.CalleeUserID, dto.CallID, dto.ConversationID, "timeout_25s")
	}
	if s.hooks.NotifyMissedCall != nil {
		s.hooks.NotifyMissedCall(context.Background(), dto)
	}
	if s.hooks.SendWSEvent != nil {
		for _, uid := range []string{dto.CallerUserID, dto.CalleeUserID} {
			s.hooks.SendWSEvent(uid, map[string]any{
				"event":           "call_ended",
				"conversation_id": dto.ConversationID,
				"user_id":         dto.CalleeUserID,
				"call":            dto,
				"timestamp_ms":    now,
			})
		}
	}
}

func (s *Service) appendCallLogChatMessage(dto *CallRecordDTO, text string) {
	if s.hooks.AppendChatMessage == nil || dto == nil {
		return
	}
	meta := buildCallMetaJSON(*dto, text)
	msgID, err := s.hooks.AppendChatMessage(dto.ConversationID, dto.CallerUserID, dto.CalleeUserID, "call_log", meta)
	if err == nil && msgID != "" {
		s.mu.Lock()
		if sc, ok := s.calls[dto.CallID]; ok {
			sc.DTO.LogMessageID = msgID
		}
		dto.LogMessageID = msgID
		s.mu.Unlock()
	}
}

func (s *Service) clearActivePairLocked(callerID, calleeID, callID string) {
	if s.activeByUsr[callerID] == callID {
		delete(s.activeByUsr, callerID)
	}
	if s.activeByUsr[calleeID] == callID {
		delete(s.activeByUsr, calleeID)
	}
}

func (s *Service) resolveInfo(userID string) ParticipantInfo {
	if s.hooks.ResolveUser != nil {
		if info, ok := s.hooks.ResolveUser(userID); ok {
			if info.FullName == "" {
				info.FullName = userID
			}
			return info
		}
	}
	return ParticipantInfo{UserID: userID, FullName: userID}
}

func (s *Service) nowMs() int64 {
	return s.nowFunc().UTC().UnixMilli()
}

type targetedCallWSEvent struct {
	TargetUserID string
	Payload      map[string]any
}

func isTerminalCallState(st string) bool {
	switch st {
	case "ended", "missed", "declined", "busy", "cancelled", "not_allowed":
		return true
	default:
		return false
	}
}

func buildCallMetaJSON(dto CallRecordDTO, text string) string {
	obj := map[string]any{
		"call_id":        dto.CallID,
		"call_type":      dto.CallType,
		"outcome":        dto.State,
		"end_reason":     dto.EndReason,
		"caller_user_id": dto.CallerUserID,
		"callee_user_id": dto.CalleeUserID,
		"duration_sec":   dto.DurationSec,
		"video_upgraded": dto.VideoUpgraded,
		"text":           text,
		"timestamp_ms":   dto.EndedAtMs,
	}
	raw, _ := json.Marshal(obj)
	return string(raw)
}

func buildCallRequestJSON(dto CallRecordDTO, text string) string {
	obj := map[string]any{
		"call_id":        dto.CallID,
		"call_type":      dto.CallType,
		"caller_user_id": dto.CallerUserID,
		"callee_user_id": dto.CalleeUserID,
		"text":           text,
		"timestamp_ms":   dto.StartedAtMs,
	}
	raw, _ := json.Marshal(obj)
	return string(raw)
}

func formatSeconds(sec int64) string {
	if sec < 0 {
		sec = 0
	}
	m := sec / 60
	s := sec % 60
	if m >= 60 {
		h := m / 60
		m = m % 60
		return strconv.FormatInt(h, 10) + "h " + fmt.Sprintf("%02d:%02d", m, s)
	}
	return fmt.Sprintf("%02d:%02d", m, s)
}

func randomCallID(prefix string) string {
	var b [12]byte
	_, _ = rand.Read(b[:])
	return prefix + "_" + hex.EncodeToString(b[:])
}
