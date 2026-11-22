package auth

import (
	"context"
	"crypto/subtle"
	"encoding/json"
	"errors"
	"fmt"
	"html"
	"net/http"
	"net/url"
	"strings"

	sanitizer "telefam.company/backend/internal/errors"
	"telefam.company/backend/internal/verification"
)

func (h *Handler) ensureVerificationService() *verification.Service {
	chatSvc := h.ensureChatService()
	notifSvc := h.ensureNotificationsService()

	h.mu.Lock()
	if h.verSvc == nil {
		h.verSvc = verification.NewService(verification.HostHooks{
			ResolveUser: func(userID string) (verification.UserInfo, bool) {
				h.mu.Lock()
				prof, hasProf := h.profiles[userID]
				email := h.userEmails[userID]
				h.mu.Unlock()
				if hasProf {
					avatar := ""
					if prof.AvatarMediaID != "" {
						avatar = "/v1/media/avatar/" + prof.AvatarMediaID
					}
					return verification.UserInfo{
						UserID:     prof.UserID,
						FullName:   prof.FullName,
						Username:   prof.Username,
						AvatarURL:  avatar,
						Email:      email,
						CountryISO: prof.CountryISO,
						Location:   prof.Location,
						Bio:        prof.Bio,
					}, true
				}
				du, _, ok := h.contacts.GetUserForViewer("", userID)
				if ok {
					avatar := ""
					if du.AvatarMediaID != "" {
						avatar = "/v1/media/avatar/" + du.AvatarMediaID
					}
					return verification.UserInfo{
						UserID:     du.UserID,
						FullName:   du.FullName,
						Username:   du.Username,
						AvatarURL:  avatar,
						Email:      email,
						CountryISO: du.CountryISO,
						Location:   du.Country,
					}, true
				}
				return verification.UserInfo{}, false
			},
			SendWSEvent: func(targetUserID string, event map[string]any) {
				chatSvc.SendRawWSEvent(targetUserID, event)
			},
			NotifyVerificationUpdate: func(ctx context.Context, userID, status, tier, title, body string) {
				notifSvc.NotifyVerificationUpdate(ctx, userID, status, tier, title, body)
			},
			AuditEvent: func(userID, action, deviceFP, ipFP string, meta map[string]string) {
				h.audit.Record(userID, action, deviceFP, ipFP, meta)
			},
		})
	}
	svc := h.verSvc
	h.mu.Unlock()
	return svc
}

func (h *Handler) handleGetVerificationStatus(w http.ResponseWriter, r *http.Request) {
	claims, _, _, ok := h.requireAuth(w, r)
	if !ok {
		return
	}
	countryOverride := strings.TrimSpace(r.URL.Query().Get("country_iso"))
	resp, err := h.ensureVerificationService().GetStatus(claims.UserID, countryOverride)
	if err != nil {
		h.writeVerificationError(w, err)
		return
	}
	writeJSON(w, http.StatusOK, resp)
}

func (h *Handler) handleActivateVerificationTrial(w http.ResponseWriter, r *http.Request) {
	claims, deviceFP, ipFP, ok := h.requireAuth(w, r)
	if !ok {
		return
	}
	var payload verification.TrialActivatePayload
	if err := json.NewDecoder(http.MaxBytesReader(w, r.Body, 8192)).Decode(&payload); err != nil {
		sanitizer.WriteSanitizedError(w, http.StatusBadRequest, "decode_ver_trial", err, sanitizer.GenericErrorMessage)
		return
	}
	if headerKey := strings.TrimSpace(r.Header.Get("Idempotency-Key")); headerKey != "" && payload.IdempotencyKey == "" {
		payload.IdempotencyKey = headerKey
	}
	resp, err := h.ensureVerificationService().ActivateFreeTrial(r.Context(), claims.UserID, deviceFP, ipFP, payload)
	if err != nil {
		h.writeVerificationError(w, err)
		return
	}
	writeJSON(w, http.StatusOK, resp)
}

func (h *Handler) handleInitVerificationCheckout(w http.ResponseWriter, r *http.Request) {
	claims, deviceFP, ipFP, ok := h.requireAuth(w, r)
	if !ok {
		return
	}
	var payload verification.CheckoutInitPayload
	if err := json.NewDecoder(http.MaxBytesReader(w, r.Body, 8192)).Decode(&payload); err != nil {
		sanitizer.WriteSanitizedError(w, http.StatusBadRequest, "decode_ver_checkout_init", err, sanitizer.GenericErrorMessage)
		return
	}
	if headerKey := strings.TrimSpace(r.Header.Get("Idempotency-Key")); headerKey != "" && payload.IdempotencyKey == "" {
		payload.IdempotencyKey = headerKey
	}
	resp, err := h.ensureVerificationService().InitCheckout(r.Context(), claims.UserID, deviceFP, ipFP, payload)
	if err != nil {
		h.writeVerificationError(w, err)
		return
	}
	writeJSON(w, http.StatusOK, resp)
}

func (h *Handler) handleVerifyVerificationCheckout(w http.ResponseWriter, r *http.Request) {
	claims, deviceFP, ipFP, ok := h.requireAuth(w, r)
	if !ok {
		return
	}
	var payload verification.CheckoutVerifyPayload
	if err := json.NewDecoder(http.MaxBytesReader(w, r.Body, 8192)).Decode(&payload); err != nil {
		sanitizer.WriteSanitizedError(w, http.StatusBadRequest, "decode_ver_checkout_verify", err, sanitizer.GenericErrorMessage)
		return
	}
	if headerKey := strings.TrimSpace(r.Header.Get("Idempotency-Key")); headerKey != "" && payload.IdempotencyKey == "" {
		payload.IdempotencyKey = headerKey
	}
	resp, err := h.ensureVerificationService().VerifyCheckout(r.Context(), claims.UserID, deviceFP, ipFP, payload)
	if err != nil {
		h.writeVerificationError(w, err)
		return
	}
	writeJSON(w, http.StatusOK, resp)
}

func (h *Handler) handleIssueLivenessSession(w http.ResponseWriter, r *http.Request) {
	claims, deviceFP, ipFP, ok := h.requireAuth(w, r)
	if !ok {
		return
	}
	var payload verification.LivenessSessionRequest
	if err := json.NewDecoder(http.MaxBytesReader(w, r.Body, 4096)).Decode(&payload); err != nil {
		sanitizer.WriteSanitizedError(w, http.StatusBadRequest, "decode_liveness_session", err, sanitizer.GenericErrorMessage)
		return
	}
	resp, err := h.ensureVerificationService().IssueLivenessSession(claims.UserID, deviceFP, ipFP, payload)
	if err != nil {
		h.writeVerificationError(w, err)
		return
	}
	writeJSON(w, http.StatusOK, resp)
}

func (h *Handler) handleSubmitLiveness(w http.ResponseWriter, r *http.Request) {
	claims, deviceFP, ipFP, ok := h.requireAuth(w, r)
	if !ok {
		return
	}
	var payload verification.LivenessSubmitPayload
	if err := json.NewDecoder(http.MaxBytesReader(w, r.Body, 6<<20)).Decode(&payload); err != nil {
		sanitizer.WriteSanitizedError(w, http.StatusBadRequest, "decode_liveness_submit", err, sanitizer.GenericErrorMessage)
		return
	}
	resp, err := h.ensureVerificationService().SubmitLiveness(r.Context(), claims.UserID, deviceFP, ipFP, payload)
	if err != nil {
		h.writeVerificationError(w, err)
		return
	}
	writeJSON(w, http.StatusOK, resp)
}

func (h *Handler) handleIssueDocumentSession(w http.ResponseWriter, r *http.Request) {
	claims, deviceFP, ipFP, ok := h.requireAuth(w, r)
	if !ok {
		return
	}
	var payload verification.DocumentSessionRequest
	if err := json.NewDecoder(http.MaxBytesReader(w, r.Body, 4096)).Decode(&payload); err != nil {
		sanitizer.WriteSanitizedError(w, http.StatusBadRequest, "decode_doc_session", err, sanitizer.GenericErrorMessage)
		return
	}
	resp, err := h.ensureVerificationService().IssueDocumentSession(claims.UserID, deviceFP, ipFP, payload)
	if err != nil {
		h.writeVerificationError(w, err)
		return
	}
	writeJSON(w, http.StatusOK, resp)
}

func (h *Handler) handleSubmitDocument(w http.ResponseWriter, r *http.Request) {
	claims, deviceFP, ipFP, ok := h.requireAuth(w, r)
	if !ok {
		return
	}
	var payload verification.DocumentSubmitPayload
	if err := json.NewDecoder(http.MaxBytesReader(w, r.Body, 8<<20)).Decode(&payload); err != nil {
		sanitizer.WriteSanitizedError(w, http.StatusBadRequest, "decode_doc_submit", err, sanitizer.GenericErrorMessage)
		return
	}
	resp, err := h.ensureVerificationService().SubmitDocument(r.Context(), claims.UserID, deviceFP, ipFP, payload)
	if err != nil {
		h.writeVerificationError(w, err)
		return
	}
	writeJSON(w, http.StatusOK, resp)
}

func (h *Handler) handleCompleteVerificationReview(w http.ResponseWriter, r *http.Request) {
	svc := h.ensureVerificationService()
	adminSec := svc.AdminSecret()
	provided := strings.TrimSpace(r.Header.Get("X-Telefam-Review-Secret"))
	if adminSec == "" || subtle.ConstantTimeCompare([]byte(provided), []byte(adminSec)) != 1 {
		sanitizer.WriteSanitizedError(w, http.StatusUnauthorized, "ver_review_unauthorized", nil, sanitizer.GenericErrorMessage)
		return
	}
	var payload verification.CompleteReviewPayload
	if err := json.NewDecoder(http.MaxBytesReader(w, r.Body, 8192)).Decode(&payload); err != nil {
		sanitizer.WriteSanitizedError(w, http.StatusBadRequest, "decode_ver_review", err, sanitizer.GenericErrorMessage)
		return
	}
	resp, err := svc.CompleteReview(r.Context(), payload)
	if err != nil {
		h.writeVerificationError(w, err)
		return
	}
	writeJSON(w, http.StatusOK, resp)
}

func (h *Handler) handleVerificationPaymentReturn(w http.ResponseWriter, r *http.Request) {
	provider := strings.TrimSpace(r.URL.Query().Get("provider"))
	paymentID := strings.TrimSpace(r.URL.Query().Get("payment_id"))
	reference := strings.TrimSpace(r.URL.Query().Get("reference"))
	if reference == "" {
		reference = strings.TrimSpace(r.URL.Query().Get("trxref"))
	}
	paypalOrderID := strings.TrimSpace(r.URL.Query().Get("token"))
	status := strings.TrimSpace(r.URL.Query().Get("paypal_status"))

	deepLink := fmt.Sprintf(
		"telefam://verification/payment-return?provider=%s&payment_id=%s&reference=%s&paypal_order_id=%s&status=%s",
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
			`<meta http-equiv="refresh" content="0;url=%s"><title>Telefam Verification</title></head>`+
			`<body style="font-family:Arial,sans-serif;text-align:center;padding:48px;color:#1A1C1E;">`+
			`<h2 style="color:#C80A0F;">Returning to Telefam</h2>`+
			`<p>Completing verification of your Telefam Verified subscription...</p>`+
			`<a href="%s" style="display:inline-block;margin-top:16px;padding:12px 24px;background:#C80A0F;color:#fff;border-radius:999px;text-decoration:none;font-weight:bold;">Open Telefam</a>`+
			`</body></html>`,
		html.EscapeString(deepLink),
		html.EscapeString(deepLink),
	)
}

func (h *Handler) writeVerificationError(w http.ResponseWriter, err error) {
	switch {
	case errors.Is(err, verification.ErrUnauthorized):
		sanitizer.WriteSanitizedError(w, http.StatusUnauthorized, "ver_unauthorized", err, "Authentication required.")
	case errors.Is(err, verification.ErrForbidden):
		sanitizer.WriteSanitizedError(w, http.StatusForbidden, "ver_forbidden", err, "Access denied.")
	case errors.Is(err, verification.ErrNotFound):
		sanitizer.WriteSanitizedError(w, http.StatusNotFound, "ver_not_found", err, "Verification record not found.")
	case errors.Is(err, verification.ErrTrialAlreadyUsed):
		sanitizer.WriteSanitizedError(w, http.StatusConflict, "ver_trial_used", err, "Your one-week free trial has already been used. Please choose Paystack or PayPal to subscribe.")
	case errors.Is(err, verification.ErrIdempotencyConflict):
		sanitizer.WriteSanitizedError(w, http.StatusConflict, "ver_idemp_conflict", err, "Idempotency key conflict.")
	case errors.Is(err, verification.ErrPlanRequired):
		sanitizer.WriteSanitizedError(w, http.StatusPaymentRequired, "ver_plan_required", err, "Activate your free trial or subscription before starting identity verification.")
	case errors.Is(err, verification.ErrLivenessRequired):
		sanitizer.WriteSanitizedError(w, http.StatusPreconditionRequired, "ver_liveness_required", err, "Complete the live selfie check first.")
	case errors.Is(err, verification.ErrScreenCaptureActive):
		sanitizer.WriteSanitizedError(w, http.StatusForbidden, "ver_screen_capture_blocked", err, "Screen recording or mirroring is blocked during identity verification.")
	case errors.Is(err, verification.ErrChallengeExpired):
		sanitizer.WriteSanitizedError(w, http.StatusGone, "ver_challenge_expired", err, "Verification challenge expired. Start a fresh capture session.")
	case errors.Is(err, verification.ErrRateLimited):
		sanitizer.WriteSanitizedError(w, http.StatusTooManyRequests, "ver_rate_limited", err, "Too many verification attempts. Please wait a few minutes.")
	case errors.Is(err, verification.ErrGatewayUnconfigured):
		sanitizer.WriteSanitizedError(w, http.StatusServiceUnavailable, "ver_gateway_unconfigured", err, "Payment gateway credentials are not configured on this server.")
	case errors.Is(err, verification.ErrPaymentFailed):
		sanitizer.WriteSanitizedError(w, http.StatusPaymentRequired, "ver_payment_failed", err, "Payment could not be verified.")
	default:
		sanitizer.WriteSanitizedError(w, http.StatusBadRequest, "ver_bad_request", err, "Invalid verification request.")
	}
}
