package auth

import (
	"context"
	"encoding/json"
	"errors"
	"io"
	"net/http"
	"strconv"
	"strings"
	"time"

	"telefam.company/backend/internal/chat"
	"telefam.company/backend/internal/contacts"
	sanitizer "telefam.company/backend/internal/errors"
	"telefam.company/backend/internal/notifications"
	"telefam.company/backend/internal/security"
)

func (h *Handler) ensureChatService() *chat.Service {
	h.mu.Lock()
	if h.chat == nil {
		h.chat = chat.NewService(chat.HostHooks{
			ResolveUser: func(userID string) (chat.UserInfo, bool) {
				h.mu.Lock()
				prof, hasProf := h.profiles[userID]
				h.mu.Unlock()
				if hasProf {
					avatar := ""
					if prof.AvatarMediaID != "" {
						avatar = "/v1/media/avatar/" + prof.AvatarMediaID
					}
					return chat.UserInfo{
						UserID:    prof.UserID,
						FullName:  prof.FullName,
						Username:  prof.Username,
						AvatarURL: avatar,
						Location:  prof.Location,
					}, true
				}
				du, _, ok := h.contacts.GetUserForViewer("", userID)
				if ok {
					avatar := ""
					if du.AvatarMediaID != "" {
						avatar = "/v1/media/avatar/" + du.AvatarMediaID
					}
					return chat.UserInfo{
						UserID:    du.UserID,
						FullName:  du.FullName,
						Username:  du.Username,
						AvatarURL: avatar,
						Location:  du.Country,
					}, true
				}
				return chat.UserInfo{}, false
			},
			ListAllUsers: func(query string) []chat.UserInfo {
				resp := h.contacts.ListContacts("", contacts.FilterAll, query, 1, 40)
				out := make([]chat.UserInfo, 0, len(resp.Items)+4)
				seen := make(map[string]struct{})
				for _, item := range resp.Items {
					seen[item.UserID] = struct{}{}
					out = append(out, chat.UserInfo{
						UserID:    item.UserID,
						FullName:  item.FullName,
						Username:  item.Username,
						AvatarURL: item.AvatarURL,
						Location:  item.Location,
					})
				}
				clean := strings.ToLower(strings.TrimSpace(query))
				h.mu.Lock()
				for _, p := range h.profiles {
					if _, ok := seen[p.UserID]; ok {
						continue
					}
					if clean == "" ||
						strings.Contains(strings.ToLower(p.FullName), clean) ||
						strings.Contains(strings.ToLower(p.Username), clean) {
						avatar := ""
						if p.AvatarMediaID != "" {
							avatar = "/v1/media/avatar/" + p.AvatarMediaID
						}
						out = append(out, chat.UserInfo{
							UserID:    p.UserID,
							FullName:  p.FullName,
							Username:  p.Username,
							AvatarURL: avatar,
							Location:  p.Location,
						})
					}
				}
				h.mu.Unlock()
				return out
			},
			GetPrivacy: func(userID string) chat.PrivacyView {
				h.mu.Lock()
				cp := h.getUserChatPrivacyLocked(userID)
				h.mu.Unlock()
				return chat.PrivacyView{
					MessageRequests:     cp.MessageRequests,
					WhoCanCall:          cp.WhoCanCall,
					WhoCanScreenshot:    cp.WhoCanScreenshot,
					WhoCanShareChats:    cp.WhoCanShareChats,
					WhoCanCopyMessages:  cp.WhoCanCopyMessages,
					WhoCanDownloadMedia: cp.WhoCanDownloadMedia,
					ReadReceipts:        cp.ReadReceipts,
					TypingIndicators:    cp.TypingIndicators,
					OnlineIndicators:    cp.OnlineIndicators,
				}
			},
			IsBlocked: func(a, b string) bool {
				return h.feeds.IsBlocked(a, b) || h.feeds.IsBlocked(b, a)
			},
			BlockUser: func(caller, target string) {
				h.feeds.BlockUser(caller, target)
			},
			IsFriendOrFollow: func(owner, viewer string) (bool, bool) {
				_, isFollowing, followsYou, ok := h.contacts.GetUserRelationshipForViewer(viewer, owner)
				if !ok {
					return false, false
				}
				return isFollowing, followsYou
			},
			OnMessageSent: func(msg chat.MessageDTO, recipientConvMuted bool) {
				svc := h.ensureNotificationsService()
				svc.NotifyChatMessage(context.Background(), notifications.ChatPushInput{
					ConversationID:   msg.ConversationID,
					MessageID:        msg.MessageID,
					SenderUserID:     msg.SenderUserID,
					RecipientUserID:  msg.RecipientUserID,
					MessageType:      msg.MessageType,
					CompressionCodec: msg.CompressionCodec,
					EncryptionScheme: msg.EncryptionScheme,
					CiphertextB64:    msg.CiphertextB64,
					SignalMsgType:    msg.SignalMsgType,
					VoiceDurationMs:  msg.VoiceDurationMs,
					CreatedAtMs:      msg.CreatedAtMs,
					ConvMuted:        recipientConvMuted,
				})
			},
			OnMessageCancelled: func(recipientUserID, convID, messageID, reason string) {
				h.ensureNotificationsService().NotifyChatMessageCancel(
					context.Background(),
					recipientUserID,
					convID,
					messageID,
					reason,
				)
			},
		})
		h.mu.Unlock()
		return h.chat
	}
	c := h.chat
	h.mu.Unlock()
	return c
}

func (h *Handler) handleUploadE2eeKeys(w http.ResponseWriter, r *http.Request) {
	claims, deviceFP, ipFP, ok := h.requireAuth(w, r)
	if !ok {
		return
	}
	var payload chat.E2eeKeyBundleUploadPayload
	if err := json.NewDecoder(http.MaxBytesReader(w, r.Body, 128*1024)).Decode(&payload); err != nil {
		sanitizer.WriteSanitizedError(w, http.StatusBadRequest, "decode_e2ee_keys", err, sanitizer.GenericErrorMessage)
		return
	}
	if err := h.ensureChatService().UploadKeyBundle(claims.UserID, payload); err != nil {
		sanitizer.WriteSanitizedError(w, http.StatusBadRequest, "upload_e2ee_keys", err, "Invalid encryption key bundle.")
		return
	}
	h.audit.Record(claims.UserID, "chat_e2ee_keys_uploaded", deviceFP, ipFP, map[string]string{
		"otk_count": strconv.Itoa(len(payload.OneTimePreKeys)),
	})
	writeJSON(w, http.StatusOK, map[string]any{
		"success":         true,
		"server_epoch_ms": time.Now().UnixMilli(),
	})
}

func (h *Handler) handleFetchPeerE2eeKeys(w http.ResponseWriter, r *http.Request) {
	claims, _, _, ok := h.requireAuth(w, r)
	if !ok {
		return
	}
	targetID := security.SanitizeText(r.PathValue("userId"), 64, false)
	bundle, err := h.ensureChatService().FetchPeerKeyBundle(claims.UserID, targetID)
	if err != nil {
		writeChatErr(w, err)
		return
	}
	writeJSON(w, http.StatusOK, bundle)
}

func (h *Handler) handleListChatConversations(w http.ResponseWriter, r *http.Request) {
	claims, _, _, ok := h.requireAuth(w, r)
	if !ok {
		return
	}
	includeArchived := strings.EqualFold(r.URL.Query().Get("archived"), "true")
	items := h.ensureChatService().ListConversations(claims.UserID, includeArchived)
	writeJSON(w, http.StatusOK, map[string]any{
		"success":         true,
		"conversations":   items,
		"server_epoch_ms": time.Now().UnixMilli(),
	})
}

func (h *Handler) handleOpenDirectConversation(w http.ResponseWriter, r *http.Request) {
	claims, deviceFP, ipFP, ok := h.requireAuth(w, r)
	if !ok {
		return
	}
	targetID := security.SanitizeText(r.PathValue("userId"), 64, false)
	conv, err := h.ensureChatService().OpenOrCreateDirectConversation(claims.UserID, targetID)
	if err != nil {
		writeChatErr(w, err)
		return
	}
	h.audit.Record(claims.UserID, "chat_conversation_open", deviceFP, ipFP, map[string]string{
		"conversation_id": conv.ConversationID,
		"peer_user_id":    targetID,
	})
	writeJSON(w, http.StatusOK, map[string]any{
		"success":         true,
		"conversation":    conv,
		"server_epoch_ms": time.Now().UnixMilli(),
	})
}

func (h *Handler) handleBatchConversationAction(w http.ResponseWriter, r *http.Request) {
	claims, deviceFP, ipFP, ok := h.requireAuth(w, r)
	if !ok {
		return
	}
	var payload chat.BatchConversationActionPayload
	if err := json.NewDecoder(http.MaxBytesReader(w, r.Body, 16384)).Decode(&payload); err != nil {
		sanitizer.WriteSanitizedError(w, http.StatusBadRequest, "decode_batch_conv", err, sanitizer.GenericErrorMessage)
		return
	}
	convs, err := h.ensureChatService().BatchConversationAction(claims.UserID, payload)
	if err != nil {
		writeChatErr(w, err)
		return
	}
	h.audit.Record(claims.UserID, "chat_conversations_batch_"+payload.Action, deviceFP, ipFP, map[string]string{
		"count": strconv.Itoa(len(payload.ConversationIDs)),
	})
	writeJSON(w, http.StatusOK, map[string]any{
		"success":         true,
		"conversations":   convs,
		"server_epoch_ms": time.Now().UnixMilli(),
	})
}

func (h *Handler) handleListChatMessages(w http.ResponseWriter, r *http.Request) {
	claims, _, _, ok := h.requireAuth(w, r)
	if !ok {
		return
	}
	convID := security.SanitizeText(r.PathValue("id"), 64, false)
	sinceMs, _ := strconv.ParseInt(strings.TrimSpace(r.URL.Query().Get("since_ms")), 10, 64)
	msgs, conv, err := h.ensureChatService().ListMessagesDelta(claims.UserID, convID, sinceMs)
	if err != nil {
		writeChatErr(w, err)
		return
	}
	flags, _ := h.ensureChatService().ListSecurityFlags(claims.UserID, convID)
	writeJSON(w, http.StatusOK, map[string]any{
		"success":         true,
		"conversation":    conv,
		"messages":        msgs,
		"security_flags":  flags,
		"is_delta":        sinceMs > 0,
		"server_epoch_ms": time.Now().UnixMilli(),
	})
}

func (h *Handler) handleSendChatMessage(w http.ResponseWriter, r *http.Request) {
	claims, deviceFP, ipFP, ok := h.requireAuth(w, r)
	if !ok {
		return
	}
	convID := security.SanitizeText(r.PathValue("id"), 64, false)
	var payload chat.SendMessagePayload
	if err := json.NewDecoder(http.MaxBytesReader(w, r.Body, 512*1024)).Decode(&payload); err != nil {
		sanitizer.WriteSanitizedError(w, http.StatusBadRequest, "decode_send_chat", err, sanitizer.GenericErrorMessage)
		return
	}
	msg, err := h.ensureChatService().SendMessage(claims.UserID, convID, payload)
	if err != nil {
		writeChatErr(w, err)
		return
	}
	h.audit.Record(claims.UserID, "chat_message_sent", deviceFP, ipFP, map[string]string{
		"conversation_id": convID,
		"type":            msg.MessageType,
	})
	writeJSON(w, http.StatusOK, map[string]any{
		"success":         true,
		"message":         msg,
		"server_epoch_ms": time.Now().UnixMilli(),
	})
}

func (h *Handler) handleEditChatMessage(w http.ResponseWriter, r *http.Request) {
	claims, _, _, ok := h.requireAuth(w, r)
	if !ok {
		return
	}
	msgID := security.SanitizeText(r.PathValue("id"), 64, false)
	var payload chat.EditMessagePayload
	if err := json.NewDecoder(http.MaxBytesReader(w, r.Body, 512*1024)).Decode(&payload); err != nil {
		sanitizer.WriteSanitizedError(w, http.StatusBadRequest, "decode_edit_chat", err, sanitizer.GenericErrorMessage)
		return
	}
	msg, err := h.ensureChatService().EditMessage(claims.UserID, msgID, payload)
	if err != nil {
		writeChatErr(w, err)
		return
	}
	writeJSON(w, http.StatusOK, map[string]any{
		"success": true,
		"message": msg,
	})
}

func (h *Handler) handleDeleteChatMessage(w http.ResponseWriter, r *http.Request) {
	claims, _, _, ok := h.requireAuth(w, r)
	if !ok {
		return
	}
	msgID := security.SanitizeText(r.PathValue("id"), 64, false)
	mode := r.URL.Query().Get("mode")
	convID, err := h.ensureChatService().DeleteMessage(claims.UserID, msgID, mode)
	if err != nil {
		writeChatErr(w, err)
		return
	}
	writeJSON(w, http.StatusOK, map[string]any{
		"success":         true,
		"conversation_id": convID,
		"message_id":      msgID,
	})
}

func (h *Handler) handleChatMessageReceipt(w http.ResponseWriter, r *http.Request) {
	claims, _, _, ok := h.requireAuth(w, r)
	if !ok {
		return
	}
	msgID := security.SanitizeText(r.PathValue("id"), 64, false)
	var payload chat.ReceiptPayload
	if err := json.NewDecoder(http.MaxBytesReader(w, r.Body, 4096)).Decode(&payload); err != nil {
		sanitizer.WriteSanitizedError(w, http.StatusBadRequest, "decode_receipt", err, sanitizer.GenericErrorMessage)
		return
	}
	msg, err := h.ensureChatService().UpdateMessageReceipt(claims.UserID, msgID, payload.Status)
	if err != nil {
		writeChatErr(w, err)
		return
	}
	writeJSON(w, http.StatusOK, map[string]any{
		"success": true,
		"message": msg,
	})
}

func (h *Handler) handleOpenViewOnceVoice(w http.ResponseWriter, r *http.Request) {
	claims, deviceFP, ipFP, ok := h.requireAuth(w, r)
	if !ok {
		return
	}
	msgID := security.SanitizeText(r.PathValue("id"), 64, false)
	msg, flag, err := h.ensureChatService().OpenViewOnceMessage(claims.UserID, msgID)
	if err != nil {
		writeChatErr(w, err)
		return
	}
	h.audit.Record(claims.UserID, "chat_view_once_opened", deviceFP, ipFP, map[string]string{
		"message_id": msgID,
	})
	writeJSON(w, http.StatusOK, map[string]any{
		"success":       true,
		"message":       msg,
		"security_flag": flag,
	})
}

func (h *Handler) handleConsumeViewOnceVoice(w http.ResponseWriter, r *http.Request) {
	claims, deviceFP, ipFP, ok := h.requireAuth(w, r)
	if !ok {
		return
	}
	msgID := security.SanitizeText(r.PathValue("id"), 64, false)
	convID, err := h.ensureChatService().ConsumeViewOnceMessage(claims.UserID, msgID)
	if err != nil {
		writeChatErr(w, err)
		return
	}
	h.audit.Record(claims.UserID, "chat_view_once_consumed", deviceFP, ipFP, map[string]string{
		"message_id": msgID,
	})
	writeJSON(w, http.StatusOK, map[string]any{
		"success":         true,
		"conversation_id": convID,
		"message_id":      msgID,
	})
}

func (h *Handler) handleUploadVoiceMedia(w http.ResponseWriter, r *http.Request) {
	claims, _, _, ok := h.requireAuth(w, r)
	if !ok {
		return
	}
	if err := r.ParseMultipartForm(12 << 20); err != nil {
		sanitizer.WriteSanitizedError(w, http.StatusBadRequest, "parse_voice_multipart", err, "Invalid voice upload.")
		return
	}
	convID := security.SanitizeText(r.FormValue("conversation_id"), 64, false)
	durMs, _ := strconv.ParseInt(r.FormValue("duration_ms"), 10, 64)
	viewOnce := strings.EqualFold(r.FormValue("view_once"), "true")
	file, header, err := r.FormFile("file")
	if err != nil {
		sanitizer.WriteSanitizedError(w, http.StatusBadRequest, "missing_voice_file", err, "Voice recording required.")
		return
	}
	defer file.Close()
	raw, err := io.ReadAll(io.LimitReader(file, 10<<20))
	if err != nil {
		sanitizer.WriteSanitizedError(w, http.StatusBadRequest, "read_voice_file", err, "Unable to read voice recording.")
		return
	}
	mimeType := "audio/mp4"
	if header != nil && strings.HasPrefix(header.Header.Get("Content-Type"), "audio/") {
		mimeType = header.Header.Get("Content-Type")
	}
	mediaID, fp, err := h.ensureChatService().StoreVoiceMedia(claims.UserID, convID, mimeType, raw, durMs, viewOnce)
	if err != nil {
		writeChatErr(w, err)
		return
	}
	writeJSON(w, http.StatusOK, map[string]any{
		"success":            true,
		"media_id":           mediaID,
		"media_url":          "/v1/chat/media/voice/" + mediaID,
		"sha256_fingerprint": fp,
		"byte_size":          len(raw),
	})
}

func (h *Handler) handleDownloadVoiceMedia(w http.ResponseWriter, r *http.Request) {
	claims, _, _, ok := h.requireAuth(w, r)
	if !ok {
		return
	}
	mediaID := security.SanitizeText(r.PathValue("mediaId"), 64, false)
	mimeType, raw, err := h.ensureChatService().GetVoiceMedia(claims.UserID, mediaID)
	if err != nil {
		writeChatErr(w, err)
		return
	}
	w.Header().Set("Content-Type", mimeType)
	w.Header().Set("Cache-Control", "no-store")
	w.Header().Set("X-Content-Type-Options", "nosniff")
	w.WriteHeader(http.StatusOK)
	_, _ = w.Write(raw)
}

func (h *Handler) handleRecordSecurityFlag(w http.ResponseWriter, r *http.Request) {
	claims, deviceFP, ipFP, ok := h.requireAuth(w, r)
	if !ok {
		return
	}
	convID := security.SanitizeText(r.PathValue("id"), 64, false)
	var payload chat.SecurityFlagPayload
	if err := json.NewDecoder(http.MaxBytesReader(w, r.Body, 8192)).Decode(&payload); err != nil {
		sanitizer.WriteSanitizedError(w, http.StatusBadRequest, "decode_security_flag", err, sanitizer.GenericErrorMessage)
		return
	}
	flag, err := h.ensureChatService().RecordSecurityFlag(claims.UserID, convID, payload)
	if err != nil {
		writeChatErr(w, err)
		return
	}
	h.audit.Record(claims.UserID, "chat_security_flag_"+flag.FlagType, deviceFP, ipFP, map[string]string{
		"conversation_id": convID,
		"flag_id":         flag.FlagID,
	})
	writeJSON(w, http.StatusOK, map[string]any{
		"success": true,
		"flag":    flag,
	})
}

func (h *Handler) handleListSecurityFlags(w http.ResponseWriter, r *http.Request) {
	claims, _, _, ok := h.requireAuth(w, r)
	if !ok {
		return
	}
	convID := security.SanitizeText(r.PathValue("id"), 64, false)
	flags, err := h.ensureChatService().ListSecurityFlags(claims.UserID, convID)
	if err != nil {
		writeChatErr(w, err)
		return
	}
	writeJSON(w, http.StatusOK, map[string]any{
		"success": true,
		"flags":   flags,
	})
}

func (h *Handler) handleGiflyGreetingSticker(w http.ResponseWriter, r *http.Request) {
	claims, _, _, ok := h.requireAuth(w, r)
	if !ok {
		return
	}
	seed := r.URL.Query().Get("peer_id") + claims.UserID
	item := h.ensureChatService().Gifly.GreetingSticker(seed)
	writeJSON(w, http.StatusOK, map[string]any{
		"success": true,
		"item":    item,
	})
}

func (h *Handler) handleGiflySearch(w http.ResponseWriter, r *http.Request) {
	_, _, _, ok := h.requireAuth(w, r)
	if !ok {
		return
	}
	mediaType := r.URL.Query().Get("type")
	query := r.URL.Query().Get("q")
	page, _ := strconv.Atoi(r.URL.Query().Get("page"))
	perPage, _ := strconv.Atoi(r.URL.Query().Get("per_page"))
	resp := h.ensureChatService().Gifly.SearchMedia(mediaType, query, page, perPage)
	writeJSON(w, http.StatusOK, resp)
}

func (h *Handler) handleChatGlobalSearch(w http.ResponseWriter, r *http.Request) {
	claims, _, _, ok := h.requireAuth(w, r)
	if !ok {
		return
	}
	query := r.URL.Query().Get("q")
	convs, people := h.ensureChatService().SearchDirectoryAndConversations(claims.UserID, query)
	writeJSON(w, http.StatusOK, map[string]any{
		"success":       true,
		"conversations": convs,
		"people":        people,
	})
}

func (h *Handler) handleChatWebSocket(w http.ResponseWriter, r *http.Request) {
	deviceFP := strings.TrimSpace(r.Header.Get("X-Telefam-Device-FP"))
	if deviceFP == "" {
		deviceFP = strings.TrimSpace(r.URL.Query().Get("device_fp"))
	}
	token := strings.TrimSpace(strings.TrimPrefix(r.Header.Get("Authorization"), "Bearer "))
	if token == "" {
		token = strings.TrimSpace(r.URL.Query().Get("token"))
	}
	if deviceFP == "" || token == "" {
		sanitizer.WriteSanitizedError(w, http.StatusUnauthorized, "ws_missing_auth", nil, sanitizer.GenericErrorMessage)
		return
	}
	claims, err := h.tokens.VerifyAccessToken(token, deviceFP)
	if err != nil {
		sanitizer.WriteSanitizedError(w, http.StatusUnauthorized, "ws_invalid_token", err, sanitizer.GenericErrorMessage)
		return
	}
	h.ensureCallsService()
	h.ensureChatService().ServeWebSocket(w, r, claims.UserID, deviceFP)
}

func writeChatErr(w http.ResponseWriter, err error) {
	switch {
	case errors.Is(err, chat.ErrViewOnceGuard):
		sanitizer.WriteSanitizedError(w, http.StatusForbidden, "chat_view_once_guard", err, "Only the recipient can open a view-once voice note.")
	case errors.Is(err, chat.ErrForbidden):
		sanitizer.WriteSanitizedError(w, http.StatusForbidden, "chat_forbidden", err, "Action not permitted by user privacy or block settings.")
	case errors.Is(err, chat.ErrNotFound):
		sanitizer.WriteSanitizedError(w, http.StatusNotFound, "chat_not_found", err, "Chat item not found.")
	default:
		sanitizer.WriteSanitizedError(w, http.StatusBadRequest, "chat_bad_request", err, "Invalid chat request.")
	}
}
