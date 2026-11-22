package auth

import (
	"context"
	"encoding/json"
	"errors"
	"fmt"
	"html"
	"io"
	"net/http"
	"net/url"
	"strings"

	sanitizer "telefam.company/backend/internal/errors"
	"telefam.company/backend/internal/stars"
)

func (h *Handler) ensureStarsService() *stars.Service {
	chatSvc := h.ensureChatService()
	notifSvc := h.ensureNotificationsService()

	h.mu.Lock()
	if h.stars == nil {
		h.stars = stars.NewService(stars.HostHooks{
			ResolveUser: func(userID string) (stars.UserInfo, bool) {
				h.mu.Lock()
				prof, hasProf := h.profiles[userID]
				email := h.userEmails[userID]
				h.mu.Unlock()
				if hasProf {
					avatar := ""
					if prof.AvatarMediaID != "" {
						avatar = "/v1/media/avatar/" + prof.AvatarMediaID
					}
					return stars.UserInfo{
						UserID:     prof.UserID,
						FullName:   prof.FullName,
						Username:   prof.Username,
						AvatarURL:  avatar,
						Email:      email,
						CountryISO: prof.CountryISO,
					}, true
				}
				du, _, ok := h.contacts.GetUserForViewer("", userID)
				if ok {
					avatar := ""
					if du.AvatarMediaID != "" {
						avatar = "/v1/media/avatar/" + du.AvatarMediaID
					}
					return stars.UserInfo{
						UserID:    du.UserID,
						FullName:  du.FullName,
						Username:  du.Username,
						AvatarURL: avatar,
						Email:     email,
					}, true
				}
				return stars.UserInfo{}, false
			},
			IsBlocked: func(a, b string) bool {
				return h.feeds.IsBlocked(a, b) || h.feeds.IsBlocked(b, a)
			},
			OpenDirectConv: func(callerID, peerID string) (string, error) {
				conv, err := chatSvc.OpenOrCreateDirectConversation(callerID, peerID)
				if err != nil {
					return "", err
				}
				return conv.ConversationID, nil
			},
			AppendStarChatMsg: func(convID, senderID, peerID, metaJSON, clientOpID string) (string, error) {
				msg, err := chatSvc.AppendStarGiftMessage(convID, senderID, peerID, metaJSON, clientOpID)
				if err != nil {
					return "", err
				}
				return msg.MessageID, nil
			},
			SendWSEvent: func(targetUserID string, event map[string]any) {
				chatSvc.SendRawWSEvent(targetUserID, event)
			},
			NotifyStarReceived: func(ctx context.Context, senderID, recipientID, convID, messageID string, amount int64) {
				notifSvc.NotifyStarReceived(ctx, senderID, recipientID, convID, messageID, amount)
			},
			AuditEvent: func(userID, action, deviceFP, ipFP string, meta map[string]string) {
				h.audit.Record(userID, action, deviceFP, ipFP, meta)
			},
		})
	}
	svc := h.stars
	h.mu.Unlock()
	return svc
}

func (h *Handler) handleGetStarsSummary(w http.ResponseWriter, r *http.Request) {
	claims, _, _, ok := h.requireAuth(w, r)
	if !ok {
		return
	}
	countryOverride := strings.TrimSpace(r.URL.Query().Get("country_iso"))
	resp, err := h.ensureStarsService().GetSummary(claims.UserID, countryOverride)
	if err != nil {
		h.writeStarsError(w, err)
		return
	}
	writeJSON(w, http.StatusOK, resp)
}

func (h *Handler) handleInitStarsCheckout(w http.ResponseWriter, r *http.Request) {
	claims, deviceFP, ipFP, ok := h.requireAuth(w, r)
	if !ok {
		return
	}
	var payload stars.CheckoutInitPayload
	if err := json.NewDecoder(http.MaxBytesReader(w, r.Body, 8192)).Decode(&payload); err != nil {
		sanitizer.WriteSanitizedError(w, http.StatusBadRequest, "decode_stars_checkout_init", err, sanitizer.GenericErrorMessage)
		return
	}
	if headerKey := strings.TrimSpace(r.Header.Get("Idempotency-Key")); headerKey != "" && payload.IdempotencyKey == "" {
		payload.IdempotencyKey = headerKey
	}
	resp, err := h.ensureStarsService().InitCheckout(r.Context(), claims.UserID, deviceFP, ipFP, payload)
	if err != nil {
		h.writeStarsError(w, err)
		return
	}
	writeJSON(w, http.StatusOK, resp)
}

func (h *Handler) handleVerifyStarsCheckout(w http.ResponseWriter, r *http.Request) {
	claims, deviceFP, ipFP, ok := h.requireAuth(w, r)
	if !ok {
		return
	}
	var payload stars.CheckoutVerifyPayload
	if err := json.NewDecoder(http.MaxBytesReader(w, r.Body, 8192)).Decode(&payload); err != nil {
		sanitizer.WriteSanitizedError(w, http.StatusBadRequest, "decode_stars_checkout_verify", err, sanitizer.GenericErrorMessage)
		return
	}
	if headerKey := strings.TrimSpace(r.Header.Get("Idempotency-Key")); headerKey != "" && payload.IdempotencyKey == "" {
		payload.IdempotencyKey = headerKey
	}
	resp, err := h.ensureStarsService().VerifyCheckout(r.Context(), claims.UserID, deviceFP, ipFP, payload)
	if err != nil {
		h.writeStarsError(w, err)
		return
	}
	writeJSON(w, http.StatusOK, resp)
}

func (h *Handler) handleSendStarsInChat(w http.ResponseWriter, r *http.Request) {
	claims, deviceFP, ipFP, ok := h.requireAuth(w, r)
	if !ok {
		return
	}
	var payload stars.SendStarsPayload
	if err := json.NewDecoder(http.MaxBytesReader(w, r.Body, 8192)).Decode(&payload); err != nil {
		sanitizer.WriteSanitizedError(w, http.StatusBadRequest, "decode_stars_send", err, sanitizer.GenericErrorMessage)
		return
	}
	if headerKey := strings.TrimSpace(r.Header.Get("Idempotency-Key")); headerKey != "" && payload.IdempotencyKey == "" {
		payload.IdempotencyKey = headerKey
	}
	resp, err := h.ensureStarsService().SendStarsInChat(r.Context(), claims.UserID, deviceFP, ipFP, payload)
	if errors.Is(err, stars.ErrInsufficientBalance) {
		writeJSON(w, http.StatusPaymentRequired, resp)
		return
	}
	if err != nil {
		h.writeStarsError(w, err)
		return
	}
	writeJSON(w, http.StatusOK, resp)
}

func (h *Handler) handlePaystackWebhook(w http.ResponseWriter, r *http.Request) {
	rawBody, err := io.ReadAll(http.MaxBytesReader(w, r.Body, 1<<20))
	if err != nil {
		w.WriteHeader(http.StatusBadRequest)
		return
	}
	sig := r.Header.Get("X-Paystack-Signature")
	if strings.Contains(string(rawBody), `"TFVR_`) {
		if err := h.ensureVerificationService().HandlePaystackWebhook(r.Context(), rawBody, sig); err != nil {
			h.writeVerificationError(w, err)
			return
		}
		writeJSON(w, http.StatusOK, map[string]any{"received": true})
		return
	}
	if err := h.ensureStarsService().HandlePaystackWebhook(r.Context(), rawBody, sig); err != nil {
		if errors.Is(err, stars.ErrForbidden) {
			w.WriteHeader(http.StatusUnauthorized)
			return
		}
		if errors.Is(err, stars.ErrGatewayUnconfigured) {
			w.WriteHeader(http.StatusServiceUnavailable)
			return
		}
		w.WriteHeader(http.StatusBadRequest)
		return
	}
	writeJSON(w, http.StatusOK, map[string]any{"received": true})
}

func (h *Handler) handlePayPalWebhook(w http.ResponseWriter, r *http.Request) {
	rawBody, err := io.ReadAll(http.MaxBytesReader(w, r.Body, 1<<20))
	if err != nil {
		w.WriteHeader(http.StatusBadRequest)
		return
	}
	starsErr := h.ensureStarsService().HandlePayPalWebhook(r.Context(), r.Header, rawBody)
	if starsErr != nil {
		if errors.Is(starsErr, stars.ErrForbidden) {
			w.WriteHeader(http.StatusUnauthorized)
			return
		}
		if errors.Is(starsErr, stars.ErrGatewayUnconfigured) {
			w.WriteHeader(http.StatusServiceUnavailable)
			return
		}
		w.WriteHeader(http.StatusBadRequest)
		return
	}
	_ = h.ensureVerificationService().HandlePayPalWebhook(r.Context(), r.Header, rawBody)
	writeJSON(w, http.StatusOK, map[string]any{"received": true})
}

func (h *Handler) handleStarsPaymentReturn(w http.ResponseWriter, r *http.Request) {
	provider := strings.TrimSpace(r.URL.Query().Get("provider"))
	paymentID := strings.TrimSpace(r.URL.Query().Get("payment_id"))
	reference := strings.TrimSpace(r.URL.Query().Get("reference"))
	if reference == "" {
		reference = strings.TrimSpace(r.URL.Query().Get("trxref"))
	}
	paypalOrderID := strings.TrimSpace(r.URL.Query().Get("token"))
	status := strings.TrimSpace(r.URL.Query().Get("paypal_status"))

	deepLink := fmt.Sprintf(
		"telefam://stars/payment-return?provider=%s&payment_id=%s&reference=%s&paypal_order_id=%s&status=%s",
		url.QueryEscape(provider),
		url.QueryEscape(paymentID),
		url.QueryEscape(reference),
		url.QueryEscape(paypalOrderID),
		url.QueryEscape(status),
	)
	w.Header().Set("Content-Type", "text/html; charset=utf-8")
	w.Header().Set("Cache-Control", "no-store")
	w.Header().Set("Content-Security-Policy", "default-src 'none'; style-src 'unsafe-inline'")
	w.WriteHeader(http.StatusOK)
	_, _ = fmt.Fprintf(
		w,
		`<!doctype html><html><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">`+
			`<meta http-equiv="refresh" content="0;url=%s"><title>Telefam Stars</title></head>`+
			`<body style="font-family:Arial,sans-serif;text-align:center;padding:48px;color:#1A1C1E;">`+
			`<h2 style="color:#C80A0F;">Returning to Telefam</h2>`+
			`<p>Completing verification of your Telefam Stars payment...</p>`+
			`<a href="%s" style="display:inline-block;margin-top:16px;padding:12px 24px;background:#C80A0F;color:#fff;border-radius:999px;text-decoration:none;font-weight:bold;">Open Telefam</a>`+
			`</body></html>`,
		html.EscapeString(deepLink),
		html.EscapeString(deepLink),
	)
}

func (h *Handler) writeStarsError(w http.ResponseWriter, err error) {
	switch {
	case errors.Is(err, stars.ErrUnauthorized):
		sanitizer.WriteSanitizedError(w, http.StatusUnauthorized, "stars_unauthorized", err, "Session expired")
	case errors.Is(err, stars.ErrForbidden):
		sanitizer.WriteSanitizedError(w, http.StatusForbidden, "stars_forbidden", err, "Action not permitted")
	case errors.Is(err, stars.ErrNotFound):
		sanitizer.WriteSanitizedError(w, http.StatusNotFound, "stars_not_found", err, "Payment record not found")
	case errors.Is(err, stars.ErrIdempotencyConflict):
		sanitizer.WriteSanitizedError(w, http.StatusConflict, "stars_idempotency_conflict", err, "Idempotency key conflict")
	case errors.Is(err, stars.ErrRateLimited):
		sanitizer.WriteSanitizedError(w, http.StatusTooManyRequests, "stars_rate_limited", err, "Rate limit active for account security")
	case errors.Is(err, stars.ErrGatewayUnconfigured):
		sanitizer.WriteSanitizedError(w, http.StatusServiceUnavailable, "stars_gateway_unconfigured", err, "Payment gateway is not configured on the server")
	case errors.Is(err, stars.ErrPaymentFailed):
		sanitizer.WriteSanitizedError(w, http.StatusBadGateway, "stars_payment_failed", err, "Payment could not be verified")
	default:
		sanitizer.WriteSanitizedError(w, http.StatusBadRequest, "stars_bad_request", err, sanitizer.GenericErrorMessage)
	}
}
