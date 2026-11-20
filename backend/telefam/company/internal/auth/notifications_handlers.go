package auth

import (
	"encoding/json"
	"net/http"
	"strconv"
	"strings"

	"telefam.company/backend/internal/contacts"
	sanitizer "telefam.company/backend/internal/errors"
	"telefam.company/backend/internal/notifications"
	"telefam.company/backend/internal/security"
)

type RegisterPushTokenPayload struct {
	Token     string `json:"token"`
	Platform  string `json:"platform"`
	AppBundle string `json:"app_bundle,omitempty"`
}

type MarkNotificationReadPayload struct {
	NotificationID string `json:"notification_id,omitempty"`
	ConversationID string `json:"conversation_id,omitempty"`
}

func (h *Handler) ensureNotificationsService() *notifications.Service {
	h.mu.Lock()
	if h.notifs == nil {
		h.notifs = notifications.NewService(notifications.HostHooks{
			ResolveUser: func(userID string) (notifications.ActorInfo, bool) {
				h.mu.Lock()
				prof, hasProf := h.profiles[userID]
				h.mu.Unlock()
				if hasProf {
					avatar := ""
					if prof.AvatarMediaID != "" {
						avatar = "/v1/media/avatar/" + prof.AvatarMediaID
					}
					return notifications.ActorInfo{
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
					return notifications.ActorInfo{
						UserID:    du.UserID,
						FullName:  du.FullName,
						Username:  du.Username,
						AvatarURL: avatar,
					}, true
				}
				return notifications.ActorInfo{}, false
			},
			ResolveByUsername: func(username string) (notifications.ActorInfo, bool) {
				clean := strings.ToLower(strings.TrimPrefix(strings.TrimSpace(username), "@"))
				if clean == "" {
					return notifications.ActorInfo{}, false
				}
				h.mu.Lock()
				uid, ok := h.usernames[clean]
				prof, hasProf := h.profiles[uid]
				h.mu.Unlock()
				if ok && hasProf {
					avatar := ""
					if prof.AvatarMediaID != "" {
						avatar = "/v1/media/avatar/" + prof.AvatarMediaID
					}
					return notifications.ActorInfo{
						UserID:    prof.UserID,
						FullName:  prof.FullName,
						Username:  prof.Username,
						AvatarURL: avatar,
					}, true
				}
				resp := h.contacts.ListContacts("", contacts.FilterAll, clean, 1, 25)
				for _, user := range resp.Items {
					if strings.EqualFold(user.Username, clean) {
						return notifications.ActorInfo{
							UserID:    user.UserID,
							FullName:  user.FullName,
							Username:  user.Username,
							AvatarURL: user.AvatarURL,
						}, true
					}
				}
				return notifications.ActorInfo{}, false
			},
			GetPreferences: func(userID string) notifications.Preferences {
				h.mu.Lock()
				ns := h.getUserNotificationsLocked(userID)
				pv := h.getUserPrivacyLocked(userID)
				h.mu.Unlock()
				return notifications.Preferences{
					MasterPause:       ns.MasterPause,
					ChannelPush:       ns.ChannelPush,
					TypeMessages:      ns.TypeMessages,
					TypeComments:      ns.TypeComments,
					TypeFollowers:     ns.TypeFollowers,
					QuietHoursEnabled: ns.QuietHoursEnabled,
					QuietStartMinutes: ns.QuietStartMinutes,
					QuietEndMinutes:   ns.QuietEndMinutes,
					WhoCanMention:     pv.WhoCanMention,
				}
			},
			IsBlocked: func(recipientID, actorID string) bool {
				return h.feeds.IsBlocked(recipientID, actorID) || h.feeds.IsBlocked(actorID, recipientID)
			},
			IsMutedUser: func(recipientID, actorID string) bool {
				return h.feeds.IsMuted(recipientID, actorID)
			},
			AreFriends: func(ownerID, viewerID string) bool {
				_, isFollowing, followsYou, ok := h.contacts.GetUserRelationshipForViewer(viewerID, ownerID)
				return ok && (isFollowing || followsYou)
			},
		})
	}
	svc := h.notifs
	h.mu.Unlock()
	return svc
}

func (h *Handler) handleRegisterPushToken(w http.ResponseWriter, r *http.Request) {
	claims, deviceFP, ipFP, ok := h.requireAuth(w, r)
	if !ok {
		return
	}
	var payload RegisterPushTokenPayload
	if err := json.NewDecoder(http.MaxBytesReader(w, r.Body, 8192)).Decode(&payload); err != nil {
		sanitizer.WriteSanitizedError(w, http.StatusBadRequest, "decode_push_token", err, sanitizer.GenericErrorMessage)
		return
	}
	rec, saved := h.ensureNotificationsService().RegisterPushToken(
		claims.UserID,
		deviceFP,
		payload.Platform,
		payload.Token,
		payload.AppBundle,
	)
	if !saved {
		sanitizer.WriteSanitizedError(w, http.StatusBadRequest, "invalid_push_token", nil, sanitizer.GenericErrorMessage)
		return
	}
	h.audit.Record(claims.UserID, "push_token_registered", deviceFP, ipFP, map[string]string{
		"platform": rec.Platform,
	})
	writeJSON(w, http.StatusOK, map[string]any{
		"success":       true,
		"platform":      rec.Platform,
		"updated_at_ms": rec.UpdatedAtMs,
	})
}

func (h *Handler) handleUnregisterPushToken(w http.ResponseWriter, r *http.Request) {
	claims, deviceFP, ipFP, ok := h.requireAuth(w, r)
	if !ok {
		return
	}
	allDevices := strings.EqualFold(strings.TrimSpace(r.URL.Query().Get("all_devices")), "true")
	removed := 0
	if allDevices {
		removed = h.ensureNotificationsService().RevokeAllUserTokens(claims.UserID)
	} else {
		removed = h.ensureNotificationsService().RevokeDeviceToken(claims.UserID, deviceFP)
	}
	h.audit.Record(claims.UserID, "push_token_unregistered", deviceFP, ipFP, map[string]string{
		"removed":     strconv.Itoa(removed),
		"all_devices": strconv.FormatBool(allDevices),
	})
	writeJSON(w, http.StatusOK, map[string]any{
		"success": true,
		"removed": removed,
	})
}

func (h *Handler) handleListNotifications(w http.ResponseWriter, r *http.Request) {
	claims, _, _, ok := h.requireAuth(w, r)
	if !ok {
		return
	}
	limit, _ := strconv.Atoi(r.URL.Query().Get("limit"))
	items, unread := h.ensureNotificationsService().ListNotifications(claims.UserID, limit)
	writeJSON(w, http.StatusOK, map[string]any{
		"success":      true,
		"items":        items,
		"unread_count": unread,
	})
}

func (h *Handler) handleMarkNotificationRead(w http.ResponseWriter, r *http.Request) {
	claims, _, _, ok := h.requireAuth(w, r)
	if !ok {
		return
	}
	var payload MarkNotificationReadPayload
	if err := json.NewDecoder(http.MaxBytesReader(w, r.Body, 4096)).Decode(&payload); err != nil {
		sanitizer.WriteSanitizedError(w, http.StatusBadRequest, "decode_mark_notif_read", err, sanitizer.GenericErrorMessage)
		return
	}
	svc := h.ensureNotificationsService()
	var unread int
	if strings.TrimSpace(payload.ConversationID) != "" {
		unread = svc.MarkConversationNotificationsRead(claims.UserID, security.SanitizeText(payload.ConversationID, 96, false))
	} else {
		unread, _ = svc.MarkNotificationRead(claims.UserID, security.SanitizeText(payload.NotificationID, 96, false))
	}
	writeJSON(w, http.StatusOK, map[string]any{
		"success":      true,
		"unread_count": unread,
	})
}

func (h *Handler) handleMarkAllNotificationsRead(w http.ResponseWriter, r *http.Request) {
	claims, _, _, ok := h.requireAuth(w, r)
	if !ok {
		return
	}
	h.ensureNotificationsService().MarkAllRead(claims.UserID)
	writeJSON(w, http.StatusOK, map[string]any{
		"success":      true,
		"unread_count": 0,
	})
}
