package auth

import (
	"context"
	"encoding/json"
	"errors"
	"fmt"
	"html"
	"net/http"
	"net/url"
	"strings"

	sanitizer "telefam.company/backend/internal/errors"
	"telefam.company/backend/internal/notifications"
	"telefam.company/backend/internal/subscriptions"
)

func (h *Handler) ensureSubscriptionsService() *subscriptions.Service {
	chatSvc := h.ensureChatService()
	notifSvc := h.ensureNotificationsService()

	h.mu.Lock()
	if h.subs == nil {
		h.subs = subscriptions.NewService(subscriptions.HostHooks{
			ResolveUser: func(userID string) (subscriptions.UserInfo, bool) {
				verTier := "none"
				h.mu.Lock()
				prof, hasProf := h.profiles[userID]
				email := h.userEmails[userID]
				verSvc := h.verSvc
				h.mu.Unlock()
				if verSvc != nil {
					verTier = verSvc.VerifiedTierForUser(userID)
				}
				if hasProf {
					avatar := ""
					if prof.AvatarMediaID != "" {
						avatar = "/v1/media/avatar/" + prof.AvatarMediaID
					}
					return subscriptions.UserInfo{
						UserID:       prof.UserID,
						FullName:     prof.FullName,
						Username:     prof.Username,
						AvatarURL:    avatar,
						VerifiedTier: verTier,
						Email:        email,
						CountryISO:   prof.CountryISO,
					}, true
				}
				du, _, ok := h.contacts.GetUserForViewer("", userID)
				if ok {
					avatar := ""
					if du.AvatarMediaID != "" {
						avatar = "/v1/media/avatar/" + du.AvatarMediaID
					}
					return subscriptions.UserInfo{
						UserID:       du.UserID,
						FullName:     du.FullName,
						Username:     du.Username,
						AvatarURL:    avatar,
						VerifiedTier: verTier,
						Email:        email,
						CountryISO:   du.CountryISO,
					}, true
				}
				return subscriptions.UserInfo{}, false
			},
			SendWSEvent: func(targetUserID string, event map[string]any) {
				chatSvc.SendRawWSEvent(targetUserID, event)
			},
			NotifySubscriptionEvent: func(
				ctx context.Context,
				recipientUserID string,
				actor subscriptions.UserInfo,
				kind, title, body, billingCycle string,
			) {
				notifSvc.NotifySubscriptionEvent(
					ctx,
					recipientUserID,
					notifications.ActorInfo{
						UserID:    actor.UserID,
						FullName:  actor.FullName,
						Username:  actor.Username,
						AvatarURL: actor.AvatarURL,
					},
					kind,
					title,
					body,
					billingCycle,
				)
			},
			AuditEvent: func(userID, action, deviceFP, ipFP string, meta map[string]string) {
				h.audit.Record(userID, action, deviceFP, ipFP, meta)
			},
		})
	}
	svc := h.subs
	h.mu.Unlock()
	return svc
}

func (h *Handler) handleGetSubscriptionsHub(w http.ResponseWriter, r *http.Request) {
	claims, _, _, ok := h.requireAuth(w, r)
	if !ok {
		return
	}
	countryOverride := strings.TrimSpace(r.URL.Query().Get("country_iso"))
	resp, err := h.ensureSubscriptionsService().GetHub(claims.UserID, countryOverride)
	if err != nil {
		h.writeSubscriptionsError(w, err)
		return
	}
	writeJSON(w, http.StatusOK, resp)
}

func (h *Handler) handleSaveCreatorSubscriptionPlan(w http.ResponseWriter, r *http.Request) {
	claims, deviceFP, ipFP, ok := h.requireAuth(w, r)
	if !ok {
		return
	}
	var payload subscriptions.SavePlanPayload
	if err := json.NewDecoder(http.MaxBytesReader(w, r.Body, 8192)).Decode(&payload); err != nil {
		sanitizer.WriteSanitizedError(w, http.StatusBadRequest, "decode_sub_plan", err, sanitizer.GenericErrorMessage)
		return
	}
	resp, err := h.ensureSubscriptionsService().SaveCreatorPlan(r.Context(), claims.UserID, deviceFP, ipFP, payload)
	if err != nil {
		h.writeSubscriptionsError(w, err)
		return
	}
	writeJSON(w, http.StatusOK, resp)
}

func (h *Handler) handleGetCreatorSubscriptionOffering(w http.ResponseWriter, r *http.Request) {
	claims, _, _, ok := h.requireAuth(w, r)
	if !ok {
		return
	}
	creatorID := strings.TrimSpace(r.URL.Query().Get("creator_user_id"))
	if creatorID == "" {
		creatorID = strings.TrimSpace(r.PathValue("creatorId"))
	}
	countryOverride := strings.TrimSpace(r.URL.Query().Get("country_iso"))
	resp, err := h.ensureSubscriptionsService().GetCreatorOffering(claims.UserID, creatorID, countryOverride)
	if err != nil {
		h.writeSubscriptionsError(w, err)
		return
	}
	writeJSON(w, http.StatusOK, resp)
}

func (h *Handler) handleInitSubscriptionCheckout(w http.ResponseWriter, r *http.Request) {
	claims, deviceFP, ipFP, ok := h.requireAuth(w, r)
	if !ok {
		return
	}
	var payload subscriptions.CheckoutInitPayload
	if err := json.NewDecoder(http.MaxBytesReader(w, r.Body, 8192)).Decode(&payload); err != nil {
		sanitizer.WriteSanitizedError(w, http.StatusBadRequest, "decode_sub_checkout_init", err, sanitizer.GenericErrorMessage)
		return
	}
	if headerKey := strings.TrimSpace(r.Header.Get("Idempotency-Key")); headerKey != "" && payload.IdempotencyKey == "" {
		payload.IdempotencyKey = headerKey
	}
	resp, err := h.ensureSubscriptionsService().InitCheckout(r.Context(), claims.UserID, deviceFP, ipFP, payload)
	if err != nil {
		h.writeSubscriptionsError(w, err)
		return
	}
	writeJSON(w, http.StatusOK, resp)
}

func (h *Handler) handleVerifySubscriptionCheckout(w http.ResponseWriter, r *http.Request) {
	claims, deviceFP, ipFP, ok := h.requireAuth(w, r)
	if !ok {
		return
	}
	var payload subscriptions.CheckoutVerifyPayload
	if err := json.NewDecoder(http.MaxBytesReader(w, r.Body, 8192)).Decode(&payload); err != nil {
		sanitizer.WriteSanitizedError(w, http.StatusBadRequest, "decode_sub_checkout_verify", err, sanitizer.GenericErrorMessage)
		return
	}
	if headerKey := strings.TrimSpace(r.Header.Get("Idempotency-Key")); headerKey != "" && payload.IdempotencyKey == "" {
		payload.IdempotencyKey = headerKey
	}
	resp, err := h.ensureSubscriptionsService().VerifyCheckout(r.Context(), claims.UserID, deviceFP, ipFP, payload)
	if err != nil {
		h.writeSubscriptionsError(w, err)
		return
	}
	writeJSON(w, http.StatusOK, resp)
}

func (h *Handler) handleCancelSubscription(w http.ResponseWriter, r *http.Request) {
	claims, deviceFP, ipFP, ok := h.requireAuth(w, r)
	if !ok {
		return
	}
	var payload subscriptions.CancelPayload
	if err := json.NewDecoder(http.MaxBytesReader(w, r.Body, 4096)).Decode(&payload); err != nil {
		sanitizer.WriteSanitizedError(w, http.StatusBadRequest, "decode_sub_cancel", err, sanitizer.GenericErrorMessage)
		return
	}
	resp, err := h.ensureSubscriptionsService().CancelSubscription(r.Context(), claims.UserID, deviceFP, ipFP, payload)
	if err != nil {
		h.writeSubscriptionsError(w, err)
		return
	}
	writeJSON(w, http.StatusOK, resp)
}

func (h *Handler) handleRenewSubscription(w http.ResponseWriter, r *http.Request) {
	claims, deviceFP, ipFP, ok := h.requireAuth(w, r)
	if !ok {
		return
	}
	var payload subscriptions.RenewPayload
	if err := json.NewDecoder(http.MaxBytesReader(w, r.Body, 4096)).Decode(&payload); err != nil {
		sanitizer.WriteSanitizedError(w, http.StatusBadRequest, "decode_sub_renew", err, sanitizer.GenericErrorMessage)
		return
	}
	if headerKey := strings.TrimSpace(r.Header.Get("Idempotency-Key")); headerKey != "" && payload.IdempotencyKey == "" {
		payload.IdempotencyKey = headerKey
	}
	resp, err := h.ensureSubscriptionsService().RenewSubscription(r.Context(), claims.UserID, deviceFP, ipFP, payload)
	if err != nil {
		h.writeSubscriptionsError(w, err)
		return
	}
	writeJSON(w, http.StatusOK, resp)
}

func (h *Handler) handleSubscriptionsPaymentReturn(w http.ResponseWriter, r *http.Request) {
	provider := strings.TrimSpace(r.URL.Query().Get("provider"))
	paymentID := strings.TrimSpace(r.URL.Query().Get("payment_id"))
	reference := strings.TrimSpace(r.URL.Query().Get("reference"))
	if reference == "" {
		reference = strings.TrimSpace(r.URL.Query().Get("trxref"))
	}
	paypalOrderID := strings.TrimSpace(r.URL.Query().Get("token"))
	status := strings.TrimSpace(r.URL.Query().Get("paypal_status"))

	deepLink := fmt.Sprintf(
		"telefam://subscriptions/payment-return?provider=%s&payment_id=%s&reference=%s&paypal_order_id=%s&status=%s",
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
			`<meta http-equiv="refresh" content="0;url=%s"><title>Telefam Subscriptions</title></head>`+
			`<body style="font-family:Arial,sans-serif;text-align:center;padding:48px;color:#1A1C1E;">`+
			`<h2 style="color:#C80A0F;">Returning to Telefam</h2>`+
			`<p>Completing verification of your creator subscription...</p>`+
			`<a href="%s" style="display:inline-block;margin-top:16px;padding:12px 24px;background:#C80A0F;color:#fff;border-radius:999px;text-decoration:none;font-weight:bold;">Open Telefam</a>`+
			`</body></html>`,
		html.EscapeString(deepLink),
		html.EscapeString(deepLink),
	)
}

func (h *Handler) writeSubscriptionsError(w http.ResponseWriter, err error) {
	switch {
	case errors.Is(err, subscriptions.ErrUnauthorized):
		sanitizer.WriteSanitizedError(w, http.StatusUnauthorized, "sub_unauthorized", err, "Authentication required.")
	case errors.Is(err, subscriptions.ErrForbidden):
		sanitizer.WriteSanitizedError(w, http.StatusForbidden, "sub_forbidden", err, "Access denied.")
	case errors.Is(err, subscriptions.ErrNotFound):
		sanitizer.WriteSanitizedError(w, http.StatusNotFound, "sub_not_found", err, "Subscription record not found.")
	case errors.Is(err, subscriptions.ErrSelfSubscribe):
		sanitizer.WriteSanitizedError(w, http.StatusBadRequest, "sub_self_not_allowed", err, "You cannot subscribe to your own creator profile.")
	case errors.Is(err, subscriptions.ErrPlanDisabled):
		sanitizer.WriteSanitizedError(w, http.StatusConflict, "sub_plan_disabled", err, "This creator's subscription plan is currently paused.")
	case errors.Is(err, subscriptions.ErrIdempotencyConflict):
		sanitizer.WriteSanitizedError(w, http.StatusConflict, "sub_idemp_conflict", err, "Idempotency key conflict.")
	case errors.Is(err, subscriptions.ErrRateLimited):
		sanitizer.WriteSanitizedError(w, http.StatusTooManyRequests, "sub_rate_limited", err, "Too many payment attempts. Please wait a few minutes.")
	case errors.Is(err, subscriptions.ErrGatewayUnconfigured):
		sanitizer.WriteSanitizedError(w, http.StatusServiceUnavailable, "sub_gateway_unconfigured", err, "Payment gateway credentials are not configured on this server.")
	case errors.Is(err, subscriptions.ErrPaymentFailed):
		sanitizer.WriteSanitizedError(w, http.StatusPaymentRequired, "sub_payment_failed", err, "Subscription payment could not be verified.")
	default:
		sanitizer.WriteSanitizedError(w, http.StatusBadRequest, "sub_bad_request", err, "Invalid subscription request.")
	}
}
