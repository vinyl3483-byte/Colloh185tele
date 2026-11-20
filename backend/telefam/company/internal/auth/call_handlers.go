package auth

import (
	"context"
	"encoding/json"
	"errors"
	"net/http"
	"strconv"
	"strings"

	"telefam.company/backend/internal/calls"
	"telefam.company/backend/internal/chat"
	sanitizer "telefam.company/backend/internal/errors"
	"telefam.company/backend/internal/notifications"
	"telefam.company/backend/internal/security"
)

func (h *Handler) ensureCallsService() *calls.Service {
	chatSvc := h.ensureChatService()
	notifSvc := h.ensureNotificationsService()

	h.mu.Lock()
	if h.calls == nil {
		h.calls = calls.NewService(calls.HostHooks{
			ResolveUser: func(userID string) (calls.ParticipantInfo, bool) {
				h.mu.Lock()
				prof, hasProf := h.profiles[userID]
				h.mu.Unlock()
				if hasProf {
					avatar := ""
					if prof.AvatarMediaID != "" {
						avatar = "/v1/media/avatar/" + prof.AvatarMediaID
					}
					return calls.ParticipantInfo{
						UserID:    prof.UserID,
						FullName:  prof.FullName,
						Username:  prof.Username,
						AvatarURL: avatar,
					}, true
				}
				du, _, ok := h.contacts.GetUserForViewer("", userID)
				if ok {
					avatar := ""
					if du.AvatarMediaID != "" {
						avatar = "/v1/media/avatar/" + du.AvatarMediaID
					}
					return calls.ParticipantInfo{
						UserID:    du.UserID,
						FullName:  du.FullName,
						Username:  du.Username,
						AvatarURL: avatar,
					}, true
				}
				return calls.ParticipantInfo{}, false
			},
			IsBlocked: func(a, b string) bool {
				return h.feeds.IsBlocked(a, b) || h.feeds.IsBlocked(b, a)
			},
			CanCall: func(callerID, calleeID string) bool {
				return chatSvc.CanCall(callerID, calleeID)
			},
			OpenDirectConv: func(callerID, peerID string) (string, error) {
				conv, err := chatSvc.OpenOrCreateDirectConversation(callerID, peerID)
				if err != nil {
					return "", err
				}
				return conv.ConversationID, nil
			},
			IsUserOnlineWS: func(userID string) bool {
				return chatSvc.IsUserOnline(userID)
			},
			SendWSEvent: func(targetUserID string, event map[string]any) {
				chatSvc.SendRawWSEvent(targetUserID, event)
			},
			AppendChatMessage: func(convID, senderID, peerID, messageType, metaJSON string) (string, error) {
				msg, err := chatSvc.AppendSystemCallMessage(convID, senderID, peerID, messageType, metaJSON)
				if err != nil {
					return "", err
				}
				return msg.MessageID, nil
			},
			NotifyIncomingCall: func(ctx context.Context, call calls.CallRecordDTO) {
				notifSvc.NotifyIncomingCall(ctx, notifications.IncomingCallPushInput{
					CallID:         call.CallID,
					ConversationID: call.ConversationID,
					CallType:       call.CallType,
					CallerUserID:   call.CallerUserID,
					CalleeUserID:   call.CalleeUserID,
					ExpiresAtMs:    call.ExpiresAtMs,
					CreatedAtMs:    call.StartedAtMs,
				})
			},
			NotifyCallCancelled: func(ctx context.Context, recipientID, callID, convID, reason string) {
				notifSvc.NotifyCallCancelled(ctx, recipientID, callID, convID, reason)
			},
			NotifyMissedCall: func(ctx context.Context, call calls.CallRecordDTO) {
				notifSvc.NotifyMissedCall(
					ctx,
					call.CalleeUserID,
					call.CallerUserID,
					call.CallID,
					call.ConversationID,
					call.CallType,
				)
			},
		})
	}
	callSvc := h.calls
	h.mu.Unlock()

	chatSvc.SetCallSignalHook(func(callerID string, rawPayload []byte) {
		type wsCallSignalEnvelope struct {
			CallID         string `json:"call_id"`
			Action         string `json:"action"`
			SDP            string `json:"sdp"`
			SDPType        string `json:"sdp_type"`
			Candidate      string `json:"candidate"`
			SDPMid         string `json:"sdp_mid"`
			SDPMLineIndex  int    `json:"sdp_mline_index"`
			Reason         string `json:"reason"`
			CamEnabled     *bool  `json:"cam_enabled"`
			MicEnabled     *bool  `json:"mic_enabled"`
			InAppActive    *bool  `json:"in_app_active"`
			ScreenSharing  *bool  `json:"screen_sharing"`
			VideoFilter    string `json:"video_filter"`
			NetworkQuality string `json:"network_quality"`
		}
		var env wsCallSignalEnvelope
		if json.Unmarshal(rawPayload, &env) != nil || strings.TrimSpace(env.CallID) == "" {
			return
		}
		_, _ = callSvc.HandleSignal(context.Background(), callerID, env.CallID, calls.CallSignalPayload{
			Action:         env.Action,
			SDP:            env.SDP,
			SDPType:        env.SDPType,
			Candidate:      env.Candidate,
			SDPMid:         env.SDPMid,
			SDPMLineIndex:  env.SDPMLineIndex,
			Reason:         env.Reason,
			CamEnabled:     env.CamEnabled,
			MicEnabled:     env.MicEnabled,
			InAppActive:    env.InAppActive,
			ScreenSharing:  env.ScreenSharing,
			VideoFilter:    env.VideoFilter,
			NetworkQuality: env.NetworkQuality,
		})
	})

	return callSvc
}

func (h *Handler) handleGetCallIceConfig(w http.ResponseWriter, r *http.Request) {
	claims, _, _, ok := h.requireAuth(w, r)
	if !ok {
		return
	}
	cfg := h.ensureCallsService().IceServersForUser(claims.UserID)
	writeJSON(w, http.StatusOK, cfg)
}

func (h *Handler) handleStartCall(w http.ResponseWriter, r *http.Request) {
	claims, deviceFP, ipFP, ok := h.requireAuth(w, r)
	if !ok {
		return
	}
	var payload calls.StartCallPayload
	if err := json.NewDecoder(http.MaxBytesReader(w, r.Body, 131072)).Decode(&payload); err != nil {
		sanitizer.WriteSanitizedError(w, http.StatusBadRequest, "decode_start_call", err, sanitizer.GenericErrorMessage)
		return
	}
	resp, err := h.ensureCallsService().StartCall(r.Context(), claims.UserID, payload)
	if err != nil {
		writeCallErr(w, err)
		return
	}
	h.audit.Record(claims.UserID, "call_start_"+resp.Call.CallType, deviceFP, ipFP, map[string]string{
		"call_id":         resp.Call.CallID,
		"conversation_id": resp.Call.ConversationID,
		"state":           resp.Call.State,
	})
	writeJSON(w, http.StatusOK, resp)
}

func (h *Handler) handleGetCallState(w http.ResponseWriter, r *http.Request) {
	claims, _, _, ok := h.requireAuth(w, r)
	if !ok {
		return
	}
	callID := security.SanitizeText(r.PathValue("callId"), 64, false)
	sinceSeq, _ := strconv.ParseInt(strings.TrimSpace(r.URL.Query().Get("since_seq")), 10, 64)
	dto, offer, signals, err := h.ensureCallsService().GetCallState(claims.UserID, callID, sinceSeq)
	if err != nil {
		writeCallErr(w, err)
		return
	}
	writeJSON(w, http.StatusOK, map[string]any{
		"success":   true,
		"call":      dto,
		"sdp_offer": offer,
		"signals":   signals,
	})
}

func (h *Handler) handleCallSignal(w http.ResponseWriter, r *http.Request) {
	claims, deviceFP, ipFP, ok := h.requireAuth(w, r)
	if !ok {
		return
	}
	callID := security.SanitizeText(r.PathValue("callId"), 64, false)
	var payload calls.CallSignalPayload
	if err := json.NewDecoder(http.MaxBytesReader(w, r.Body, 131072)).Decode(&payload); err != nil {
		sanitizer.WriteSanitizedError(w, http.StatusBadRequest, "decode_call_signal", err, sanitizer.GenericErrorMessage)
		return
	}
	dto, err := h.ensureCallsService().HandleSignal(r.Context(), claims.UserID, callID, payload)
	if err != nil {
		if errors.Is(err, calls.ErrCallNotActive) {
			writeJSON(w, http.StatusOK, map[string]any{
				"success": true,
				"call":    dto,
			})
			return
		}
		writeCallErr(w, err)
		return
	}
	action := strings.ToLower(strings.TrimSpace(payload.Action))
	if action == "accept" || action == "decline" || action == "end" || action == "cancel" {
		h.audit.Record(claims.UserID, "call_signal_"+action, deviceFP, ipFP, map[string]string{
			"call_id": callID,
			"state":   dto.State,
		})
	}
	writeJSON(w, http.StatusOK, map[string]any{
		"success": true,
		"call":    dto,
	})
}

func writeCallErr(w http.ResponseWriter, err error) {
	switch {
	case errors.Is(err, calls.ErrForbidden), errors.Is(err, chat.ErrForbidden):
		sanitizer.WriteSanitizedError(w, http.StatusForbidden, "call_forbidden", err, "Action not permitted by user privacy or block settings.")
	case errors.Is(err, calls.ErrNotFound), errors.Is(err, chat.ErrNotFound):
		sanitizer.WriteSanitizedError(w, http.StatusNotFound, "call_not_found", err, "Call session not found.")
	default:
		sanitizer.WriteSanitizedError(w, http.StatusBadRequest, "call_bad_request", err, "Invalid call request.")
	}
}
