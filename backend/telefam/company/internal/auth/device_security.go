package auth

import (
	"crypto/sha256"
	"crypto/subtle"
	"encoding/base64"
	"encoding/hex"
	"encoding/json"
	"errors"
	"fmt"
	"math"
	"net"
	"net/http"
	"net/url"
	"os"
	"sort"
	"strings"
	"time"

	"telefam.company/backend/internal/contacts"
	sanitizer "telefam.company/backend/internal/errors"
	"telefam.company/backend/internal/security"
)

const (
	loginApprovalTTL     = 60 * time.Second
	deniedDeviceBlockTTL = 30 * time.Minute
)

type TrustedDeviceRecord struct {
	UserID       string    `json:"user_id"`
	DeviceFP     string    `json:"device_fp"`
	DeviceName   string    `json:"device_name"`
	Platform     string    `json:"platform"`
	LastIPMasked string    `json:"last_ip_masked"`
	LastLocation string    `json:"last_location"`
	CountryISO   string    `json:"country_iso"`
	LastSeenAt   time.Time `json:"last_seen_at"`
	CreatedAt    time.Time `json:"created_at"`
	Revoked      bool      `json:"revoked"`
}

type LoginApprovalRequest struct {
	RequestID            string    `json:"request_id"`
	UserID               string    `json:"user_id"`
	AccountHash          string    `json:"account_hash"`
	Email                string    `json:"-"`
	RequestingDeviceFP   string    `json:"-"`
	RequestingDeviceName string    `json:"device_name"`
	RequestingIPMasked   string    `json:"ip_address_masked"`
	RequestingLocation   string    `json:"location"`
	TargetDeviceFP       string    `json:"-"`
	TargetDeviceName     string    `json:"target_device_name"`
	TargetLocation       string    `json:"target_location"`
	PollTokenHash        []byte    `json:"-"`
	Status               string    `json:"status"` // "pending", "approved", "denied", "expired", "consumed"
	CreatedAt            time.Time `json:"created_at"`
	ExpiresAt            time.Time `json:"expires_at"`
	RespondedAt          time.Time `json:"responded_at,omitempty"`
	RespondedByDeviceFP  string    `json:"-"`
}

type PendingLoginApprovalItemDTO struct {
	RequestID        string `json:"request_id"`
	DeviceName       string `json:"device_name"`
	Location         string `json:"location"`
	IPAddressMasked  string `json:"ip_address_masked"`
	RequestedAtMs    int64  `json:"requested_at_ms"`
	ExpiresAtMs      int64  `json:"expires_at_ms"`
	RemainingSeconds int64  `json:"remaining_seconds"`
}

type PendingLoginApprovalsResponse struct {
	Success  bool                          `json:"success"`
	Requests []PendingLoginApprovalItemDTO `json:"requests"`
}

type RespondLoginApprovalPayload struct {
	Action string `json:"action"` // "accept" or "deny"
}

type RespondLoginApprovalResponse struct {
	Success       bool   `json:"success"`
	RequestID     string `json:"request_id"`
	Status        string `json:"status"`
	DeviceName    string `json:"device_name"`
	Location      string `json:"location"`
	RequestedAtMs int64  `json:"requested_at_ms"`
}

type LoginApprovalStatusResponse struct {
	Success             bool   `json:"success"`
	Status              string `json:"status"`
	Blocked             bool   `json:"blocked,omitempty"`
	RemainingSeconds    int64  `json:"remaining_seconds"`
	TargetDeviceName    string `json:"target_device_name,omitempty"`
	TargetLocation      string `json:"target_location,omitempty"`
	UserID              string `json:"user_id,omitempty"`
	SessionToken        string `json:"session_token,omitempty"`
	RefreshToken        string `json:"refresh_token,omitempty"`
	AccessExpiresInSec  int64  `json:"access_expires_in_seconds,omitempty"`
	RefreshExpiresInSec int64  `json:"refresh_expires_in_seconds,omitempty"`
	Email               string `json:"email,omitempty"`
	SuggestedFullName   string `json:"suggested_full_name,omitempty"`
	SuggestedUsername   string `json:"suggested_username,omitempty"`
	ProfileCompleted    bool   `json:"profile_completed,omitempty"`
}

type DeviceContextTelemetryPayload struct {
	DeviceName       string  `json:"device_name"`
	Platform         string  `json:"platform"`
	HardwareModel    string  `json:"hardware_model"`
	CarrierISO       string  `json:"carrier_country_iso"`
	SimISO           string  `json:"sim_country_iso"`
	GpsCountryISO    string  `json:"gps_country_iso"`
	GpsCity          string  `json:"gps_city"`
	LocaleCountryISO string  `json:"locale_country_iso"`
	Timezone         string  `json:"timezone"`
	LatitudeBucket   float64 `json:"lat_bucket,omitempty"`
	LongitudeBucket  float64 `json:"lon_bucket,omitempty"`
	SignalSource     string  `json:"signal_source"`
	CapturedAtMs     int64   `json:"captured_at_ms"`
}

type UserCountryAttribution struct {
	UserID                 string `json:"user_id"`
	PhoneCountryISO        string `json:"phone_country_iso"`
	CarrierCountryISO      string `json:"carrier_country_iso"`
	SimCountryISO          string `json:"sim_country_iso"`
	GpsCountryISO          string `json:"gps_country_iso"`
	IPCountryISO           string `json:"ip_country_iso"`
	LocaleCountryISO       string `json:"locale_country_iso"`
	EffectiveCountryISO    string `json:"effective_country_iso"`
	MonetizationCPMCountry string `json:"monetization_cpm_country"`
	AttributionSource      string `json:"attribution_source"`
	ResolvedLocation       string `json:"resolved_location"`
	UpdatedAtMs            int64  `json:"updated_at_ms"`
}

var isoCountryNames = map[string]string{
	"KE": "Kenya", "UG": "Uganda", "TZ": "Tanzania", "RW": "Rwanda", "BI": "Burundi",
	"ET": "Ethiopia", "SO": "Somalia", "SS": "South Sudan", "SD": "Sudan", "CD": "Congo (DRC)",
	"NG": "Nigeria", "GH": "Ghana", "ZA": "South Africa", "EG": "Egypt", "MA": "Morocco",
	"AO": "Angola", "ZM": "Zambia", "ZW": "Zimbabwe", "MW": "Malawi", "MZ": "Mozambique",
	"CM": "Cameroon", "SN": "Senegal", "CI": "Cote d'Ivoire", "US": "United States",
	"CA": "Canada", "GB": "United Kingdom", "DE": "Germany", "FR": "France", "IT": "Italy",
	"ES": "Spain", "NL": "Netherlands", "IN": "India", "AE": "United Arab Emirates",
	"SA": "Saudi Arabia", "QA": "Qatar", "JP": "Japan", "KR": "South Korea", "AU": "Australia",
	"BR": "Brazil", "MX": "Mexico",
}

func (h *Handler) recordTrustedDeviceAndEmailLocked(userID, email, deviceFP string, r *http.Request) {
	if userID == "" {
		return
	}
	if cleanEmail := strings.ToLower(strings.TrimSpace(email)); cleanEmail != "" {
		acctHash := h.fp.HashAccount(cleanEmail)
		h.emailToUserID[acctHash] = userID
		h.userEmails[userID] = cleanEmail
	}
	if deviceFP == "" {
		return
	}
	now := time.Now().UTC()
	devName, platform, loc, ipMasked, countryISO := extractRequestDeviceContext(r)
	devs := h.trustedDevices[userID]
	if devs == nil {
		devs = make(map[string]*TrustedDeviceRecord)
		h.trustedDevices[userID] = devs
	}
	existing := devs[deviceFP]
	if existing != nil {
		if devName != "" {
			existing.DeviceName = devName
		}
		if platform != "" {
			existing.Platform = platform
		}
		if loc != "" {
			existing.LastLocation = loc
		}
		if ipMasked != "" {
			existing.LastIPMasked = ipMasked
		}
		if countryISO != "" {
			existing.CountryISO = countryISO
		}
		existing.LastSeenAt = now
		existing.Revoked = false
	} else {
		if devName == "" {
			devName = "Telefam Device"
		}
		devs[deviceFP] = &TrustedDeviceRecord{
			UserID:       userID,
			DeviceFP:     deviceFP,
			DeviceName:   devName,
			Platform:     platform,
			LastIPMasked: ipMasked,
			LastLocation: loc,
			CountryISO:   countryISO,
			LastSeenAt:   now,
			CreatedAt:    now,
			Revoked:      false,
		}
	}
}

func (h *Handler) resolveCanonicalUserByEmailLocked(email string) (string, bool) {
	clean := strings.ToLower(strings.TrimSpace(email))
	if clean == "" {
		return "", false
	}
	acctHash := h.fp.HashAccount(clean)
	if uid, ok := h.emailToUserID[acctHash]; ok && uid != "" {
		return uid, true
	}
	if _, ok := h.profiles[acctHash]; ok {
		h.emailToUserID[acctHash] = acctHash
		h.userEmails[acctHash] = clean
		return acctHash, true
	}
	return acctHash, false
}

func (h *Handler) pickOtherTrustedDeviceLocked(userID, requestingDeviceFP string) *TrustedDeviceRecord {
	devs := h.trustedDevices[userID]
	if len(devs) == 0 {
		return nil
	}
	var candidates []*TrustedDeviceRecord
	for fpKey, rec := range devs {
		if fpKey == requestingDeviceFP || rec == nil || rec.Revoked {
			continue
		}
		candidates = append(candidates, rec)
	}
	if len(candidates) == 0 {
		return nil
	}
	sort.Slice(candidates, func(i, j int) bool {
		return candidates[i].LastSeenAt.After(candidates[j].LastSeenAt)
	})
	return candidates[0]
}

func (h *Handler) isDeviceTrustedForUserLocked(userID, deviceFP string) bool {
	devs := h.trustedDevices[userID]
	if len(devs) == 0 {
		return false
	}
	rec, ok := devs[deviceFP]
	return ok && rec != nil && !rec.Revoked
}

func (h *Handler) revokeDeviceLocked(userID, deviceFP string) {
	if devs := h.trustedDevices[userID]; devs != nil {
		if rec := devs[deviceFP]; rec != nil {
			rec.Revoked = true
		}
	}
}

func (h *Handler) revokeAllDevicesLocked(userID string) {
	for _, rec := range h.trustedDevices[userID] {
		if rec != nil {
			rec.Revoked = true
		}
	}
}

func (h *Handler) isDeviceBlockedLocked(accountHash, deviceFP string, now time.Time) bool {
	key := accountHash + ":" + deviceFP
	until, ok := h.deviceBlocks[key]
	if !ok {
		return false
	}
	if now.After(until) {
		delete(h.deviceBlocks, key)
		return false
	}
	return true
}

func (h *Handler) initiateCrossDeviceApprovalLocked(
	userID, accountHash, email, requestingDeviceFP string,
	target *TrustedDeviceRecord,
	r *http.Request,
) (*LoginApprovalRequest, string) {
	now := time.Now().UTC()
	reqID := "lreq_" + randomHex(12)
	pollToken := "lpt_" + randomHex(24)
	tokenHash := hashPollToken(pollToken, requestingDeviceFP)

	reqDevName, _, reqLocation, reqIPMasked, _ := extractRequestDeviceContext(r)
	if reqDevName == "" {
		reqDevName = "New Device"
	}
	if reqLocation == "" {
		reqLocation = "Unknown location"
	}
	targetName := target.DeviceName
	if targetName == "" {
		targetName = "Your trusted device"
	}
	targetLoc := target.LastLocation
	if targetLoc == "" {
		targetLoc = reqLocation
	}

	req := &LoginApprovalRequest{
		RequestID:            reqID,
		UserID:               userID,
		AccountHash:          accountHash,
		Email:                email,
		RequestingDeviceFP:   requestingDeviceFP,
		RequestingDeviceName: reqDevName,
		RequestingIPMasked:   reqIPMasked,
		RequestingLocation:   reqLocation,
		TargetDeviceFP:       target.DeviceFP,
		TargetDeviceName:     targetName,
		TargetLocation:       targetLoc,
		PollTokenHash:        tokenHash,
		Status:               "pending",
		CreatedAt:            now,
		ExpiresAt:            now.Add(loginApprovalTTL),
	}
	h.loginRequests[reqID] = req
	return req, pollToken
}

func (h *Handler) handleGetLoginRequestStatus(w http.ResponseWriter, r *http.Request) {
	requestID := security.SanitizeText(r.PathValue("id"), 80, false)
	if requestID == "" {
		sanitizer.WriteSanitizedError(w, http.StatusBadRequest, "missing_login_request_id", nil, sanitizer.GenericErrorMessage)
		return
	}
	deviceFP, err := h.fp.ExtractDeviceFingerprint(r)
	if err != nil {
		sanitizer.WriteSanitizedError(w, http.StatusUnauthorized, "login_status_fp", err, sanitizer.GenericErrorMessage)
		return
	}
	pollToken := strings.TrimSpace(r.Header.Get("X-Telefam-Poll-Token"))
	if pollToken == "" {
		sanitizer.WriteSanitizedError(w, http.StatusUnauthorized, "login_status_poll_token", nil, sanitizer.GenericErrorMessage)
		return
	}
	ipFP := h.fp.ExtractIPFingerprint(r)

	h.mu.Lock()
	req, ok := h.loginRequests[requestID]
	if !ok {
		h.mu.Unlock()
		sanitizer.WriteSanitizedError(w, http.StatusNotFound, "login_request_not_found", nil, sanitizer.GenericErrorMessage)
		return
	}
	candidateHash := hashPollToken(pollToken, deviceFP)
	if req.RequestingDeviceFP != deviceFP || subtle.ConstantTimeCompare(candidateHash, req.PollTokenHash) != 1 {
		h.mu.Unlock()
		h.audit.Record(req.UserID, "login_request_hijack_blocked", deviceFP, ipFP, map[string]string{"request_id": requestID})
		sanitizer.WriteSanitizedError(w, http.StatusUnauthorized, "login_request_token_mismatch", nil, sanitizer.GenericErrorMessage)
		return
	}

	now := time.Now().UTC()
	if req.Status == "pending" && now.After(req.ExpiresAt) {
		req.Status = "expired"
	}

	switch req.Status {
	case "pending":
		rem := int64(math.Ceil(req.ExpiresAt.Sub(now).Seconds()))
		if rem < 0 {
			rem = 0
		}
		targetName := req.TargetDeviceName
		targetLoc := req.TargetLocation
		h.mu.Unlock()
		writeJSON(w, http.StatusOK, LoginApprovalStatusResponse{
			Success:          true,
			Status:           "pending",
			RemainingSeconds: rem,
			TargetDeviceName: targetName,
			TargetLocation:   targetLoc,
		})
		return

	case "denied":
		h.mu.Unlock()
		writeJSON(w, http.StatusOK, LoginApprovalStatusResponse{
			Success:          true,
			Status:           "denied",
			Blocked:          true,
			RemainingSeconds: 0,
		})
		return

	case "expired":
		h.mu.Unlock()
		writeJSON(w, http.StatusOK, LoginApprovalStatusResponse{
			Success:          true,
			Status:           "expired",
			RemainingSeconds: 0,
		})
		return

	case "consumed":
		h.mu.Unlock()
		sanitizer.WriteSanitizedError(w, http.StatusConflict, "login_request_already_consumed", nil, sanitizer.GenericErrorMessage)
		return

	case "approved":
		req.Status = "consumed"
		userID := req.UserID
		email := req.Email
		targetName := req.TargetDeviceName
		targetLoc := req.TargetLocation
		h.recordTrustedDeviceAndEmailLocked(userID, email, deviceFP, r)
		delete(h.deviceBlocks, req.AccountHash+":"+deviceFP)
		_, profileDone := h.profiles[userID]
		storedProf := h.profiles[userID]
		h.mu.Unlock()

		h.limiter.ResetAccountCooldown(req.AccountHash, deviceFP)
		pair, _, err := h.tokens.IssueInitialPair(userID, email, deviceFP)
		if err != nil {
			sanitizer.WriteSanitizedError(w, http.StatusInternalServerError, "login_approval_issue_tokens", err, sanitizer.GenericErrorMessage)
			return
		}
		fullName, username := security.AutofillFromEmail(email)
		if storedProf.FullName != "" {
			fullName = storedProf.FullName
		}
		if storedProf.Username != "" {
			username = storedProf.Username
		}
		h.audit.Record(userID, "device_login_approval_completed", deviceFP, ipFP, map[string]string{
			"request_id":        requestID,
			"profile_completed": fmt.Sprintf("%t", profileDone),
		})
		writeJSON(w, http.StatusOK, LoginApprovalStatusResponse{
			Success:             true,
			Status:              "approved",
			RemainingSeconds:    0,
			TargetDeviceName:    targetName,
			TargetLocation:      targetLoc,
			UserID:              userID,
			SessionToken:        pair.AccessToken,
			RefreshToken:        pair.RefreshToken,
			AccessExpiresInSec:  pair.AccessExpiresInSec,
			RefreshExpiresInSec: pair.RefreshExpiresInSec,
			Email:               email,
			SuggestedFullName:   fullName,
			SuggestedUsername:   username,
			ProfileCompleted:    profileDone,
		})
		return
	default:
		h.mu.Unlock()
		sanitizer.WriteSanitizedError(w, http.StatusBadRequest, "login_request_unknown_state", nil, sanitizer.GenericErrorMessage)
	}
}

func (h *Handler) handleGetPendingLoginRequests(w http.ResponseWriter, r *http.Request) {
	claims, deviceFP, _, ok := h.requireAuth(w, r)
	if !ok {
		return
	}

	now := time.Now().UTC()
	h.mu.Lock()
	if !h.isDeviceTrustedForUserLocked(claims.UserID, deviceFP) {
		h.mu.Unlock()
		writeJSON(w, http.StatusOK, PendingLoginApprovalsResponse{Success: true, Requests: []PendingLoginApprovalItemDTO{}})
		return
	}
	// Keep trusted device metadata fresh
	h.recordTrustedDeviceAndEmailLocked(claims.UserID, claims.Email, deviceFP, r)

	items := make([]PendingLoginApprovalItemDTO, 0)
	for _, req := range h.loginRequests {
		if req.UserID != claims.UserID || req.RequestingDeviceFP == deviceFP {
			continue
		}
		if req.Status == "pending" && now.After(req.ExpiresAt) {
			req.Status = "expired"
			continue
		}
		if req.Status != "pending" {
			continue
		}
		rem := int64(math.Ceil(req.ExpiresAt.Sub(now).Seconds()))
		if rem < 0 {
			rem = 0
		}
		items = append(items, PendingLoginApprovalItemDTO{
			RequestID:        req.RequestID,
			DeviceName:       req.RequestingDeviceName,
			Location:         req.RequestingLocation,
			IPAddressMasked:  req.RequestingIPMasked,
			RequestedAtMs:    req.CreatedAt.UnixMilli(),
			ExpiresAtMs:      req.ExpiresAt.UnixMilli(),
			RemainingSeconds: rem,
		})
	}
	h.mu.Unlock()

	sort.Slice(items, func(i, j int) bool {
		return items[i].RequestedAtMs > items[j].RequestedAtMs
	})
	writeJSON(w, http.StatusOK, PendingLoginApprovalsResponse{
		Success:  true,
		Requests: items,
	})
}

func (h *Handler) handleRespondToLoginRequest(w http.ResponseWriter, r *http.Request) {
	claims, deviceFP, ipFP, ok := h.requireAuth(w, r)
	if !ok {
		return
	}
	requestID := security.SanitizeText(r.PathValue("id"), 80, false)
	if requestID == "" {
		sanitizer.WriteSanitizedError(w, http.StatusBadRequest, "missing_login_request_id", nil, sanitizer.GenericErrorMessage)
		return
	}
	var payload RespondLoginApprovalPayload
	if err := json.NewDecoder(http.MaxBytesReader(w, r.Body, 2048)).Decode(&payload); err != nil {
		sanitizer.WriteSanitizedError(w, http.StatusBadRequest, "decode_respond_login_req", err, sanitizer.GenericErrorMessage)
		return
	}
	action := strings.ToLower(strings.TrimSpace(payload.Action))
	if action != "accept" && action != "deny" {
		sanitizer.WriteSanitizedError(w, http.StatusBadRequest, "invalid_respond_action", nil, sanitizer.GenericErrorMessage)
		return
	}

	now := time.Now().UTC()
	h.mu.Lock()
	if !h.isDeviceTrustedForUserLocked(claims.UserID, deviceFP) {
		h.mu.Unlock()
		sanitizer.WriteSanitizedError(w, http.StatusForbidden, "responder_not_trusted_device", nil, "Only an existing trusted device can approve this request.")
		return
	}
	req, exists := h.loginRequests[requestID]
	if !exists || req.UserID != claims.UserID {
		h.mu.Unlock()
		sanitizer.WriteSanitizedError(w, http.StatusNotFound, "respond_login_request_missing", nil, "Login request not found.")
		return
	}
	if req.RequestingDeviceFP == deviceFP {
		h.mu.Unlock()
		sanitizer.WriteSanitizedError(w, http.StatusForbidden, "self_approval_forbidden", nil, "A device cannot approve its own login request.")
		return
	}
	if req.Status == "pending" && now.After(req.ExpiresAt) {
		req.Status = "expired"
	}
	if req.Status != "pending" {
		curStatus := req.Status
		h.mu.Unlock()
		sanitizer.WriteSanitizedError(w, http.StatusConflict, "login_request_not_pending_"+curStatus, nil, "This login request is no longer active.")
		return
	}

	req.RespondedAt = now
	req.RespondedByDeviceFP = deviceFP
	if action == "accept" {
		req.Status = "approved"
	} else {
		req.Status = "denied"
		h.deviceBlocks[req.AccountHash+":"+req.RequestingDeviceFP] = now.Add(deniedDeviceBlockTTL)
	}
	resp := RespondLoginApprovalResponse{
		Success:       true,
		RequestID:     req.RequestID,
		Status:        req.Status,
		DeviceName:    req.RequestingDeviceName,
		Location:      req.RequestingLocation,
		RequestedAtMs: req.CreatedAt.UnixMilli(),
	}
	h.mu.Unlock()

	if action == "accept" {
		h.audit.Record(claims.UserID, "device_login_request_accepted", deviceFP, ipFP, map[string]string{
			"request_id": requestID,
		})
	} else {
		h.audit.Record(claims.UserID, "device_login_request_denied", deviceFP, ipFP, map[string]string{
			"request_id": requestID,
		})
	}
	writeJSON(w, http.StatusOK, resp)
}

func (h *Handler) handleDeviceContextTelemetry(w http.ResponseWriter, r *http.Request) {
	claims, deviceFP, ipFP, ok := h.requireAuth(w, r)
	if !ok {
		return
	}
	var payload DeviceContextTelemetryPayload
	if err := json.NewDecoder(http.MaxBytesReader(w, r.Body, 8192)).Decode(&payload); err != nil {
		sanitizer.WriteSanitizedError(w, http.StatusBadRequest, "decode_device_context", err, sanitizer.GenericErrorMessage)
		return
	}

	devName, platform, locHeader, ipMasked, ipCountryHeader := extractRequestDeviceContext(r)
	if s := security.SanitizeText(payload.DeviceName, 80, false); s != "" {
		devName = s
	}
	if p := security.SanitizeText(payload.Platform, 24, false); p != "" {
		platform = p
	}
	carrierISO := sanitizeCountryISO(payload.CarrierISO)
	if carrierISO == "" {
		carrierISO = sanitizeCountryISO(r.Header.Get("X-Telefam-Carrier-ISO"))
	}
	simISO := sanitizeCountryISO(payload.SimISO)
	if simISO == "" {
		simISO = sanitizeCountryISO(r.Header.Get("X-Telefam-Sim-ISO"))
	}
	gpsISO := sanitizeCountryISO(payload.GpsCountryISO)
	if gpsISO == "" {
		gpsISO = sanitizeCountryISO(r.Header.Get("X-Telefam-Gps-Country"))
	}
	localeISO := sanitizeCountryISO(payload.LocaleCountryISO)
	if localeISO == "" {
		localeISO = sanitizeCountryISO(r.Header.Get("X-Telefam-Locale-Country"))
	}
	ipISO := ipCountryHeader
	gpsCity := security.SanitizeText(payload.GpsCity, 64, false)

	// Server-authoritative prioritization: GPS > Active Cellular Carrier > SIM > IP Geo > Locale
	authoritativeISO, source := selectAuthoritativeCountry(gpsISO, carrierISO, simISO, ipISO, localeISO, "")

	now := time.Now().UTC()
	h.mu.Lock()
	h.recordTrustedDeviceAndEmailLocked(claims.UserID, claims.Email, deviceFP, r)
	if devs := h.trustedDevices[claims.UserID]; devs != nil {
		if rec := devs[deviceFP]; rec != nil {
			if devName != "" {
				rec.DeviceName = devName
			}
			if platform != "" {
				rec.Platform = platform
			}
			if authoritativeISO != "" {
				rec.CountryISO = authoritativeISO
			}
			if gpsCity != "" {
				cName := isoCountryNames[authoritativeISO]
				if cName != "" {
					rec.LastLocation = gpsCity + ", " + cName
				} else {
					rec.LastLocation = gpsCity
				}
			} else if locHeader != "" {
				rec.LastLocation = locHeader
			}
			if ipMasked != "" {
				rec.LastIPMasked = ipMasked
			}
			rec.LastSeenAt = now
		}
	}

	prevAttr := h.countryAttributions[claims.UserID]
	phoneCountry := ""
	if prevAttr != nil {
		phoneCountry = prevAttr.PhoneCountryISO
	}
	if prof, hasProf := h.profiles[claims.UserID]; hasProf && phoneCountry == "" {
		phoneCountry = prof.CountryISO
	}

	effCountry := authoritativeISO
	if effCountry == "" && prevAttr != nil {
		effCountry = prevAttr.EffectiveCountryISO
		source = prevAttr.AttributionSource
	}

	resolvedLocation := ""
	if gpsCity != "" && effCountry != "" {
		if cname := isoCountryNames[effCountry]; cname != "" {
			resolvedLocation = gpsCity + ", " + cname
		} else {
			resolvedLocation = gpsCity + ", " + effCountry
		}
	} else if cname := isoCountryNames[effCountry]; cname != "" {
		resolvedLocation = cname
	}

	attr := &UserCountryAttribution{
		UserID:                 claims.UserID,
		PhoneCountryISO:        phoneCountry,
		CarrierCountryISO:      firstNonEmpty(carrierISO, safeAttrField(prevAttr, func(a *UserCountryAttribution) string { return a.CarrierCountryISO })),
		SimCountryISO:          firstNonEmpty(simISO, safeAttrField(prevAttr, func(a *UserCountryAttribution) string { return a.SimCountryISO })),
		GpsCountryISO:          firstNonEmpty(gpsISO, safeAttrField(prevAttr, func(a *UserCountryAttribution) string { return a.GpsCountryISO })),
		IPCountryISO:           firstNonEmpty(ipISO, safeAttrField(prevAttr, func(a *UserCountryAttribution) string { return a.IPCountryISO })),
		LocaleCountryISO:       firstNonEmpty(localeISO, safeAttrField(prevAttr, func(a *UserCountryAttribution) string { return a.LocaleCountryISO })),
		EffectiveCountryISO:    effCountry,
		MonetizationCPMCountry: effCountry,
		AttributionSource:      source,
		ResolvedLocation:       resolvedLocation,
		UpdatedAtMs:            now.UnixMilli(),
	}
	h.countryAttributions[claims.UserID] = attr
	h.countrySignalLog[claims.UserID] = append(h.countrySignalLog[claims.UserID], payload)

	// Silently keep stored profile & directory country_iso aligned with real carrier/GPS attribution
	if effCountry != "" {
		if prof, hasProf := h.profiles[claims.UserID]; hasProf {
			if prof.CountryISO != effCountry {
				prof.CountryISO = effCountry
			}
			if strings.TrimSpace(prof.Location) == "" && resolvedLocation != "" {
				prof.Location = resolvedLocation
			}
			h.profiles[claims.UserID] = prof
			h.contacts.RegisterOrUpdateUser(contacts.DirectoryUser{
				UserID:        claims.UserID,
				FullName:      prof.FullName,
				Username:      prof.Username,
				Country:       prof.Location,
				CountryISO:    prof.CountryISO,
				AvatarMediaID: prof.AvatarMediaID,
				CreatedAt:     now,
			}, deviceFP)
		}
	}
	h.mu.Unlock()

	h.audit.Record(claims.UserID, "device_context_synced", deviceFP, ipFP, map[string]string{
		"source":      source,
		"country_iso": effCountry,
	})

	writeJSON(w, http.StatusOK, map[string]any{
		"success":               true,
		"effective_country_iso": effCountry,
		"monetization_country":  effCountry,
		"server_epoch_ms":       now.UnixMilli(),
	})
}

func (h *Handler) resolveAuthoritativeCountryForProfileLocked(userID, submittedCountryISO string, r *http.Request) (string, string) {
	_, _, _, _, ipISO := extractRequestDeviceContext(r)
	carrierISO := sanitizeCountryISO(r.Header.Get("X-Telefam-Carrier-ISO"))
	simISO := sanitizeCountryISO(r.Header.Get("X-Telefam-Sim-ISO"))
	gpsISO := sanitizeCountryISO(r.Header.Get("X-Telefam-Gps-Country"))
	localeISO := sanitizeCountryISO(r.Header.Get("X-Telefam-Locale-Country"))
	prev := h.countryAttributions[userID]
	if prev != nil {
		if gpsISO == "" {
			gpsISO = prev.GpsCountryISO
		}
		if carrierISO == "" {
			carrierISO = prev.CarrierCountryISO
		}
		if simISO == "" {
			simISO = prev.SimCountryISO
		}
		if ipISO == "" {
			ipISO = prev.IPCountryISO
		}
		if localeISO == "" {
			localeISO = prev.LocaleCountryISO
		}
	}
	phoneCountry := sanitizeCountryISO(submittedCountryISO)
	eff, source := selectAuthoritativeCountry(gpsISO, carrierISO, simISO, ipISO, localeISO, phoneCountry)
	h.countryAttributions[userID] = &UserCountryAttribution{
		UserID:                 userID,
		PhoneCountryISO:        phoneCountry,
		CarrierCountryISO:      carrierISO,
		SimCountryISO:          simISO,
		GpsCountryISO:          gpsISO,
		IPCountryISO:           ipISO,
		LocaleCountryISO:       localeISO,
		EffectiveCountryISO:    eff,
		MonetizationCPMCountry: eff,
		AttributionSource:      source,
		UpdatedAtMs:            time.Now().UnixMilli(),
	}
	if eff == "" {
		return phoneCountry, "phone"
	}
	return eff, source
}

// verifyOAuthIdentity extracts verified email and display name from Google/Apple ID tokens.
// When TELEFAM_GOOGLE_CLIENT_ID is configured and the token is an RS256 Google ID token,
// it verifies the token against Google's tokeninfo endpoint.
func verifyOAuthIdentity(provider, identityToken string) (email string, fullName string, err error) {
	tok := strings.TrimSpace(identityToken)
	if tok == "" {
		return "", "", errors.New("empty identity token")
	}

	// Support standard 3-segment JWT (header.payload.signature)
	parts := strings.Split(tok, ".")
	if len(parts) == 3 {
		payloadBytes, decErr := base64.RawURLEncoding.DecodeString(parts[1])
		if decErr == nil {
			var claims struct {
				Email         string `json:"email"`
				EmailVerified any    `json:"email_verified"`
				Name          string `json:"name"`
				Sub           string `json:"sub"`
				Aud           string `json:"aud"`
				Iss           string `json:"iss"`
				Exp           int64  `json:"exp"`
			}
			if uErr := json.Unmarshal(payloadBytes, &claims); uErr == nil {
				if claims.Exp > 0 && time.Now().Unix() > claims.Exp {
					return "", "", errors.New("oauth token expired")
				}
				// For production RS256 Google tokens when client ID env is configured and header alg is RS256
				if provider == "google" && isRS256Header(parts[0]) {
					if allowedAud := strings.TrimSpace(os.Getenv("TELEFAM_GOOGLE_CLIENT_ID")); allowedAud != "" {
						verifiedEmail, verifiedName, vErr := verifyGoogleTokenOnline(tok, allowedAud)
						if vErr != nil {
							return "", "", vErr
						}
						return verifiedEmail, verifiedName, nil
					}
				}
				cleanEmail := strings.ToLower(strings.TrimSpace(claims.Email))
				if cleanEmail != "" {
					if !isEmailVerifiedClaim(claims.EmailVerified) {
						return "", "", errors.New("oauth email not verified")
					}
					return cleanEmail, security.SanitizeText(claims.Name, 64, false), nil
				}
			}
		}
	}

	// If token is opaque Google access/id token, verify via tokeninfo when available
	if provider == "google" {
		if allowedAud := strings.TrimSpace(os.Getenv("TELEFAM_GOOGLE_CLIENT_ID")); allowedAud != "" {
			if verifiedEmail, verifiedName, vErr := verifyGoogleTokenOnline(tok, allowedAud); vErr == nil {
				return verifiedEmail, verifiedName, nil
			}
		}
	}

	return "", "", nil
}

func isRS256Header(encodedHeader string) bool {
	hb, err := base64.RawURLEncoding.DecodeString(encodedHeader)
	if err != nil {
		return false
	}
	var hdr struct {
		Alg string `json:"alg"`
	}
	if err := json.Unmarshal(hb, &hdr); err != nil {
		return false
	}
	return strings.EqualFold(hdr.Alg, "RS256")
}

func isEmailVerifiedClaim(v any) bool {
	if v == nil {
		return true
	}
	switch typed := v.(type) {
	case bool:
		return typed
	case string:
		return strings.EqualFold(typed, "true") || typed == "1"
	default:
		return false
	}
}

func verifyGoogleTokenOnline(idToken, expectedAud string) (string, string, error) {
	client := &http.Client{Timeout: 5 * time.Second}
	endpoint := "https://oauth2.googleapis.com/tokeninfo?id_token=" + url.QueryEscape(idToken)
	resp, err := client.Get(endpoint)
	if err != nil {
		return "", "", err
	}
	defer resp.Body.Close()
	if resp.StatusCode != http.StatusOK {
		return "", "", errors.New("google tokeninfo rejected token")
	}
	var info struct {
		Email         string `json:"email"`
		EmailVerified string `json:"email_verified"`
		Name          string `json:"name"`
		Aud           string `json:"aud"`
		Azp           string `json:"azp"`
	}
	if err := json.NewDecoder(resp.Body).Decode(&info); err != nil {
		return "", "", err
	}
	if expectedAud != "" && info.Aud != expectedAud && info.Azp != expectedAud {
		return "", "", errors.New("google audience mismatch")
	}
	if strings.TrimSpace(info.Email) == "" || (info.EmailVerified != "" && info.EmailVerified != "true") {
		return "", "", errors.New("google email unverified")
	}
	return strings.ToLower(strings.TrimSpace(info.Email)), security.SanitizeText(info.Name, 64, false), nil
}

func hashPollToken(pollToken, deviceFP string) []byte {
	sum := sha256.Sum256([]byte("lpt:" + pollToken + ":" + deviceFP))
	return sum[:]
}

func extractRequestDeviceContext(r *http.Request) (deviceName, platform, location, ipMasked, ipCountryISO string) {
	if r == nil {
		return "", "unknown", "", "", ""
	}
	deviceName = security.SanitizeText(r.Header.Get("X-Telefam-Device-Name"), 80, false)
	platform = strings.ToLower(security.SanitizeText(r.Header.Get("X-Telefam-Platform"), 24, false))
	if deviceName == "" {
		deviceName, platform = parseDeviceFromUserAgent(r.UserAgent())
	} else if platform == "" {
		_, platform = parseDeviceFromUserAgent(r.UserAgent())
	}

	locHint := security.SanitizeText(r.Header.Get("X-Telefam-Location-Hint"), 96, false)
	ipCountryISO = sanitizeCountryISO(firstNonEmpty(
		r.Header.Get("X-Telefam-IP-Country"),
		r.Header.Get("CF-IPCountry"),
		r.Header.Get("X-AppEngine-Country"),
		r.Header.Get("X-Country-Code"),
	))
	if locHint != "" {
		location = locHint
	} else if ipCountryISO != "" {
		if cname := isoCountryNames[ipCountryISO]; cname != "" {
			location = cname
		} else {
			location = ipCountryISO
		}
	}

	rawIP := parseRequestClientIP(r)
	ipMasked = maskIPAddress(rawIP)
	return deviceName, platform, location, ipMasked, ipCountryISO
}

func parseDeviceFromUserAgent(ua string) (string, string) {
	low := strings.ToLower(ua)
	switch {
	case strings.Contains(low, "iphone"):
		return "iPhone", "ios"
	case strings.Contains(low, "ipad"):
		return "iPad", "ios"
	case strings.Contains(low, "android"):
		return "Android Device", "android"
	case strings.Contains(low, "mac os") || strings.Contains(low, "darwin"):
		return "Mac Desktop", "desktop"
	case strings.Contains(low, "windows"):
		return "Windows Desktop", "desktop"
	case strings.Contains(low, "linux"):
		return "Linux Desktop", "desktop"
	default:
		return "Telefam Device", "unknown"
	}
}

func parseRequestClientIP(r *http.Request) string {
	if xff := r.Header.Get("X-Forwarded-For"); xff != "" {
		parts := strings.Split(xff, ",")
		if len(parts) > 0 {
			cand := strings.TrimSpace(parts[0])
			if net.ParseIP(cand) != nil {
				return cand
			}
		}
	}
	if xri := strings.TrimSpace(r.Header.Get("X-Real-IP")); xri != "" && net.ParseIP(xri) != nil {
		return xri
	}
	host, _, err := net.SplitHostPort(r.RemoteAddr)
	if err == nil && net.ParseIP(host) != nil {
		return host
	}
	return strings.TrimSpace(r.RemoteAddr)
}

func maskIPAddress(raw string) string {
	ip := net.ParseIP(strings.TrimSpace(raw))
	if ip == nil {
		return "XX.XX.XXX.XX"
	}
	if v4 := ip.To4(); v4 != nil {
		return fmt.Sprintf("%d.%d.XXX.XX", v4[0], v4[1])
	}
	// Mask last 80 bits of IPv6 for privacy while retaining network prefix
	return fmt.Sprintf("%02x%02x:%02x%02x:XXXX::", ip[0], ip[1], ip[2], ip[3])
}

func sanitizeCountryISO(raw string) string {
	clean := strings.ToUpper(strings.TrimSpace(raw))
	if len(clean) != 2 || clean == "XX" || clean == "ZZ" {
		return ""
	}
	for i := 0; i < 2; i++ {
		if clean[i] < 'A' || clean[i] > 'Z' {
			return ""
		}
	}
	return clean
}

func selectAuthoritativeCountry(gpsISO, carrierISO, simISO, ipISO, localeISO, phoneISO string) (string, string) {
	if gpsISO != "" {
		return gpsISO, "gps"
	}
	if carrierISO != "" {
		return carrierISO, "carrier"
	}
	if simISO != "" {
		return simISO, "sim"
	}
	if ipISO != "" {
		return ipISO, "ip_geo"
	}
	if localeISO != "" {
		return localeISO, "locale"
	}
	if phoneISO != "" {
		return phoneISO, "phone"
	}
	return "", "unknown"
}

func firstNonEmpty(values ...string) string {
	for _, v := range values {
		if t := strings.TrimSpace(v); t != "" {
			return t
		}
	}
	return ""
}

func safeAttrField(a *UserCountryAttribution, fn func(*UserCountryAttribution) string) string {
	if a == nil {
		return ""
	}
	return fn(a)
}

func sha256HexString(raw string) string {
	sum := sha256.Sum256([]byte(raw))
	return hex.EncodeToString(sum[:])
}
