package auth

import (
	"bytes"
	"encoding/base64"
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"regexp"
	"strings"
	"testing"
	"time"

	"telefam.company/backend/internal/audit"
	"telefam.company/backend/internal/contacts"
	"telefam.company/backend/internal/fingerprint"
	"telefam.company/backend/internal/mailer"
	"telefam.company/backend/internal/ratelimit"
)

func buildTestMuxWithMailer(t *testing.T, onMail func(to []string, html string)) (*Handler, *http.ServeMux, func()) {
	t.Helper()
	resendServer := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		var payload struct {
			To   []string `json:"to"`
			HTML string   `json:"html"`
		}
		if err := json.NewDecoder(r.Body).Decode(&payload); err != nil {
			t.Fatalf("failed to decode resend payload: %v", err)
		}
		if onMail != nil {
			onMail(payload.To, payload.HTML)
		}
		w.WriteHeader(http.StatusOK)
		_, _ = w.Write([]byte(`{"id":"test_msg"}`))
	}))

	secret := []byte("0123456789abcdef0123456789abcdef")
	fp := fingerprint.NewService(secret)
	limiter := ratelimit.NewLimiter()
	otpSvc := NewOtpService(secret, "https://telefam.company")
	tokens := NewTokenService(secret)
	aud := audit.NewLogger()
	resendClient := mailer.NewResendClient("test_key_only", "Telefam <onboarding@resend.dev>", resendServer.URL)
	contactsSvc := contacts.NewService()
	h := NewHandler(fp, limiter, otpSvc, tokens, aud, resendClient, contactsSvc)
	mux := http.NewServeMux()
	h.RegisterRoutes(mux)
	return h, mux, resendServer.Close
}

func makeUnsignedTestJWT(claims map[string]any) string {
	hdr := base64.RawURLEncoding.EncodeToString([]byte(`{"alg":"HS256","typ":"JWT"}`))
	plBytes, _ := json.Marshal(claims)
	pl := base64.RawURLEncoding.EncodeToString(plBytes)
	sig := base64.RawURLEncoding.EncodeToString([]byte("test_signature"))
	return hdr + "." + pl + "." + sig
}

func TestCrossDeviceApprovalAcceptDenyTryAnotherWayAndProfilePreservation(t *testing.T) {
	mailCount := 0
	var lastHTML string
	_, mux, cleanup := buildTestMuxWithMailer(t, func(to []string, html string) {
		mailCount++
		lastHTML = html
	})
	defer cleanup()

	email := "alex.mwangi@telefam.company"
	devAFP := "samsung_s23_trusted_fp_0001"
	devBFP := "pixel_9_untrusted_fp_0002"
	devCFP := "rogue_untrusted_fp_0003"

	// 1. Register on Device A via OTP & setup profile
	reqOtpA := httptest.NewRequest(http.MethodPost, "/v1/auth/otp/request", bytes.NewBufferString(`{"email":"`+email+`"}`))
	reqOtpA.Header.Set("Content-Type", "application/json")
	reqOtpA.Header.Set("X-Telefam-Device-FP", devAFP)
	reqOtpA.Header.Set("X-Telefam-Device-Name", "Samsung Galaxy S23")
	reqOtpA.Header.Set("X-Telefam-Location-Hint", "Nairobi, Kenya")
	reqOtpA.RemoteAddr = "41.89.10.22:443"
	recOtpA := httptest.NewRecorder()
	mux.ServeHTTP(recOtpA, reqOtpA)
	if recOtpA.Code != http.StatusOK || mailCount != 1 {
		t.Fatalf("expected OTP mail sent for new account, code=%d mailCount=%d body=%s", recOtpA.Code, mailCount, recOtpA.Body.String())
	}
	var otpRespA RequestOtpResponse
	_ = json.NewDecoder(recOtpA.Body).Decode(&otpRespA)
	if otpRespA.RequiresDeviceApproval {
		t.Fatalf("new account should not require cross-device approval")
	}

	codeMatch := regexp.MustCompile(`>([0-9]{6})<`).FindStringSubmatch(lastHTML)
	if len(codeMatch) != 2 {
		t.Fatalf("expected 6-digit OTP, got html=%s", lastHTML)
	}
	verifyReqA := httptest.NewRequest(http.MethodPost, "/v1/auth/otp/verify", bytes.NewBufferString(`{"email":"`+email+`","otp":"`+codeMatch[1]+`"}`))
	verifyReqA.Header.Set("Content-Type", "application/json")
	verifyReqA.Header.Set("X-Telefam-Device-FP", devAFP)
	verifyReqA.Header.Set("X-Telefam-Device-Name", "Samsung Galaxy S23")
	verifyReqA.Header.Set("X-Telefam-Location-Hint", "Nairobi, Kenya")
	verifyRecA := httptest.NewRecorder()
	mux.ServeHTTP(verifyRecA, verifyReqA)
	if verifyRecA.Code != http.StatusOK {
		t.Fatalf("verify OTP A failed: %d %s", verifyRecA.Code, verifyRecA.Body.String())
	}
	var sessA AuthSessionResponse
	_ = json.NewDecoder(verifyRecA.Body).Decode(&sessA)
	if sessA.ProfileCompleted {
		t.Fatalf("expected profile_completed=false before profile setup")
	}

	// Setup profile on Device A
	setupBody := `{"full_name":"Alex Mwangi","username":"alexmwangi","phone_number":"+254712345678","country_iso":"KE","gender":"Male","date_of_birth":"1998-05-12","location":"Nairobi, Kenya","bio":"Traveller"}`
	setupReq := httptest.NewRequest(http.MethodPost, "/v1/profile/setup", bytes.NewBufferString(setupBody))
	setupReq.Header.Set("Content-Type", "application/json")
	setupReq.Header.Set("Authorization", "Bearer "+sessA.SessionToken)
	setupReq.Header.Set("X-Telefam-Device-FP", devAFP)
	setupReq.Header.Set("X-Telefam-Device-Name", "Samsung Galaxy S23")
	setupReq.Header.Set("X-Telefam-Location-Hint", "Nairobi, Kenya")
	setupRec := httptest.NewRecorder()
	mux.ServeHTTP(setupRec, setupReq)
	if setupRec.Code != http.StatusOK {
		t.Fatalf("profile setup failed: %d %s", setupRec.Code, setupRec.Body.String())
	}

	// 2. Request OTP for same email from NEW Device B -> must NOT send email OTP; must initiate cross-device approval
	reqOtpB := httptest.NewRequest(http.MethodPost, "/v1/auth/otp/request", bytes.NewBufferString(`{"email":"`+email+`"}`))
	reqOtpB.Header.Set("Content-Type", "application/json")
	reqOtpB.Header.Set("X-Telefam-Device-FP", devBFP)
	reqOtpB.Header.Set("X-Telefam-Device-Name", "Google Pixel 9")
	reqOtpB.Header.Set("X-Telefam-Location-Hint", "Mombasa, Kenya")
	reqOtpB.RemoteAddr = "41.89.77.88:51200"
	recOtpB := httptest.NewRecorder()
	mux.ServeHTTP(recOtpB, reqOtpB)
	if recOtpB.Code != http.StatusOK {
		t.Fatalf("expected 200 on device approval request, got %d: %s", recOtpB.Code, recOtpB.Body.String())
	}
	if mailCount != 1 {
		t.Fatalf("expected NO OTP email sent for untrusted new device; mailCount=%d", mailCount)
	}
	var otpRespB RequestOtpResponse
	_ = json.NewDecoder(recOtpB.Body).Decode(&otpRespB)
	if !otpRespB.RequiresDeviceApproval || otpRespB.RequestID == "" || otpRespB.PollToken == "" {
		t.Fatalf("expected requires_device_approval with request_id and poll_token, got %+v", otpRespB)
	}
	if otpRespB.TargetDeviceName != "Samsung Galaxy S23" || otpRespB.TargetLocation != "Nairobi, Kenya" {
		t.Fatalf("unexpected target device info: %+v", otpRespB)
	}
	if otpRespB.ExpiresInSeconds != 60 {
		t.Fatalf("expected 60s expiry, got %d", otpRespB.ExpiresInSeconds)
	}

	// 3. Trusted Device A polls pending requests -> sees Device B request
	pendReq := httptest.NewRequest(http.MethodGet, "/v1/auth/login-requests/pending", nil)
	pendReq.Header.Set("Authorization", "Bearer "+sessA.SessionToken)
	pendReq.Header.Set("X-Telefam-Device-FP", devAFP)
	pendRec := httptest.NewRecorder()
	mux.ServeHTTP(pendRec, pendReq)
	if pendRec.Code != http.StatusOK {
		t.Fatalf("pending poll failed: %d", pendRec.Code)
	}
	var pendResp PendingLoginApprovalsResponse
	_ = json.NewDecoder(pendRec.Body).Decode(&pendResp)
	if len(pendResp.Requests) != 1 {
		t.Fatalf("expected 1 pending request, got %d", len(pendResp.Requests))
	}
	if pendResp.Requests[0].DeviceName != "Google Pixel 9" || pendResp.Requests[0].IPAddressMasked != "41.89.XXX.XX" {
		t.Fatalf("unexpected pending request details: %+v", pendResp.Requests[0])
	}

	// Security checks: wrong poll_token or wrong deviceFP must get 401
	badStatusReq := httptest.NewRequest(http.MethodGet, "/v1/auth/login-requests/"+otpRespB.RequestID+"/status", nil)
	badStatusReq.Header.Set("X-Telefam-Device-FP", devCFP)
	badStatusReq.Header.Set("X-Telefam-Poll-Token", otpRespB.PollToken)
	badStatusRec := httptest.NewRecorder()
	mux.ServeHTTP(badStatusRec, badStatusReq)
	if badStatusRec.Code != http.StatusUnauthorized {
		t.Fatalf("expected 401 for mismatched deviceFP on status poll, got %d", badStatusRec.Code)
	}

	// 4. Device A accepts the login request
	accReq := httptest.NewRequest(http.MethodPost, "/v1/auth/login-requests/"+otpRespB.RequestID+"/respond", bytes.NewBufferString(`{"action":"accept"}`))
	accReq.Header.Set("Content-Type", "application/json")
	accReq.Header.Set("Authorization", "Bearer "+sessA.SessionToken)
	accReq.Header.Set("X-Telefam-Device-FP", devAFP)
	accRec := httptest.NewRecorder()
	mux.ServeHTTP(accRec, accReq)
	if accRec.Code != http.StatusOK {
		t.Fatalf("accept failed: %d %s", accRec.Code, accRec.Body.String())
	}

	// Device B polls status -> receives session + profile_completed=true + preserved userID
	statReq := httptest.NewRequest(http.MethodGet, "/v1/auth/login-requests/"+otpRespB.RequestID+"/status", nil)
	statReq.Header.Set("X-Telefam-Device-FP", devBFP)
	statReq.Header.Set("X-Telefam-Device-Name", "Google Pixel 9")
	statReq.Header.Set("X-Telefam-Poll-Token", otpRespB.PollToken)
	statRec := httptest.NewRecorder()
	mux.ServeHTTP(statRec, statReq)
	if statRec.Code != http.StatusOK {
		t.Fatalf("status poll after accept failed: %d %s", statRec.Code, statRec.Body.String())
	}
	var statResp LoginApprovalStatusResponse
	_ = json.NewDecoder(statRec.Body).Decode(&statResp)
	if statResp.Status != "approved" || statResp.SessionToken == "" || !statResp.ProfileCompleted {
		t.Fatalf("expected approved session with profile_completed=true, got %+v", statResp)
	}
	if statResp.UserID != sessA.UserID || statResp.SuggestedFullName != "Alex Mwangi" || statResp.SuggestedUsername != "alexmwangi" {
		t.Fatalf("expected canonical user profile preserved, got %+v vs sessA.UserID=%s", statResp, sessA.UserID)
	}

	// Replay status poll must return 409 conflict (single-use consumption)
	replayReq := httptest.NewRequest(http.MethodGet, "/v1/auth/login-requests/"+otpRespB.RequestID+"/status", nil)
	replayReq.Header.Set("X-Telefam-Device-FP", devBFP)
	replayReq.Header.Set("X-Telefam-Poll-Token", otpRespB.PollToken)
	replayRec := httptest.NewRecorder()
	mux.ServeHTTP(replayRec, replayReq)
	if replayRec.Code != http.StatusConflict {
		t.Fatalf("expected 409 on replay poll, got %d", replayRec.Code)
	}

	// 5. Device C requests login -> Device A DENIES -> Device C blocked (403 on subsequent requests)
	reqOtpC := httptest.NewRequest(http.MethodPost, "/v1/auth/otp/request", bytes.NewBufferString(`{"email":"`+email+`"}`))
	reqOtpC.Header.Set("Content-Type", "application/json")
	reqOtpC.Header.Set("X-Telefam-Device-FP", devCFP)
	reqOtpC.Header.Set("X-Telefam-Device-Name", "iPhone 15 Pro")
	recOtpC := httptest.NewRecorder()
	mux.ServeHTTP(recOtpC, reqOtpC)
	var otpRespC RequestOtpResponse
	_ = json.NewDecoder(recOtpC.Body).Decode(&otpRespC)

	denyReq := httptest.NewRequest(http.MethodPost, "/v1/auth/login-requests/"+otpRespC.RequestID+"/respond", bytes.NewBufferString(`{"action":"deny"}`))
	denyReq.Header.Set("Content-Type", "application/json")
	denyReq.Header.Set("Authorization", "Bearer "+sessA.SessionToken)
	denyReq.Header.Set("X-Telefam-Device-FP", devAFP)
	denyRec := httptest.NewRecorder()
	mux.ServeHTTP(denyRec, denyReq)
	if denyRec.Code != http.StatusOK {
		t.Fatalf("deny failed: %d", denyRec.Code)
	}

	statReqC := httptest.NewRequest(http.MethodGet, "/v1/auth/login-requests/"+otpRespC.RequestID+"/status", nil)
	statReqC.Header.Set("X-Telefam-Device-FP", devCFP)
	statReqC.Header.Set("X-Telefam-Poll-Token", otpRespC.PollToken)
	statRecC := httptest.NewRecorder()
	mux.ServeHTTP(statRecC, statReqC)
	var statRespC LoginApprovalStatusResponse
	_ = json.NewDecoder(statRecC.Body).Decode(&statRespC)
	if statRespC.Status != "denied" || !statRespC.Blocked {
		t.Fatalf("expected denied + blocked status, got %+v", statRespC)
	}

	// Retry OTP from denied device -> must return 403
	retryC := httptest.NewRequest(http.MethodPost, "/v1/auth/otp/request", bytes.NewBufferString(`{"email":"`+email+`"}`))
	retryC.Header.Set("Content-Type", "application/json")
	retryC.Header.Set("X-Telefam-Device-FP", devCFP)
	retryRecC := httptest.NewRecorder()
	mux.ServeHTTP(retryRecC, retryC)
	if retryRecC.Code != http.StatusForbidden {
		t.Fatalf("expected 403 for blocked denied device, got %d: %s", retryRecC.Code, retryRecC.Body.String())
	}

	// 6. "Try Another Way" Google OAuth -> wrong email rejected (403), matching email accepted & skips profile setup
	wrongJWT := makeUnsignedTestJWT(map[string]any{
		"email":          "someone.else@gmail.com",
		"email_verified": true,
		"name":           "Other Person",
		"exp":            time.Now().Add(time.Hour).Unix(),
	})
	wrongOAuthBody, _ := json.Marshal(OAuthPayload{
		Provider:          "google",
		IdentityToken:     wrongJWT,
		ExpectedEmail:     email,
		ApprovalRequestID: otpRespC.RequestID,
	})
	wrongOAuthReq := httptest.NewRequest(http.MethodPost, "/v1/auth/oauth/google", bytes.NewReader(wrongOAuthBody))
	wrongOAuthReq.Header.Set("Content-Type", "application/json")
	wrongOAuthReq.Header.Set("X-Telefam-Device-FP", devCFP)
	wrongOAuthRec := httptest.NewRecorder()
	mux.ServeHTTP(wrongOAuthRec, wrongOAuthReq)
	if wrongOAuthRec.Code != http.StatusForbidden {
		t.Fatalf("expected 403 for wrong Google email in Try Another Way, got %d", wrongOAuthRec.Code)
	}

	rightJWT := makeUnsignedTestJWT(map[string]any{
		"email":          email,
		"email_verified": true,
		"name":           "Alex Mwangi",
		"exp":            time.Now().Add(time.Hour).Unix(),
	})
	rightOAuthBody, _ := json.Marshal(OAuthPayload{
		Provider:          "google",
		IdentityToken:     rightJWT,
		ExpectedEmail:     email,
		ApprovalRequestID: otpRespC.RequestID,
	})
	rightOAuthReq := httptest.NewRequest(http.MethodPost, "/v1/auth/oauth/google", bytes.NewReader(rightOAuthBody))
	rightOAuthReq.Header.Set("Content-Type", "application/json")
	rightOAuthReq.Header.Set("X-Telefam-Device-FP", devCFP)
	rightOAuthReq.Header.Set("X-Telefam-Device-Name", "iPhone 15 Pro")
	rightOAuthRec := httptest.NewRecorder()
	mux.ServeHTTP(rightOAuthRec, rightOAuthReq)
	if rightOAuthRec.Code != http.StatusOK {
		t.Fatalf("expected 200 for matching Google OAuth recovery, got %d: %s", rightOAuthRec.Code, rightOAuthRec.Body.String())
	}
	var sessC AuthSessionResponse
	_ = json.NewDecoder(rightOAuthRec.Body).Decode(&sessC)
	if sessC.UserID != sessA.UserID || !sessC.ProfileCompleted || sessC.SuggestedUsername != "alexmwangi" {
		t.Fatalf("expected Google recovery to preserve canonical user & profile_completed=true, got %+v", sessC)
	}
}

func TestAuthoritativeCarrierAndGpsCountryAttributionForMonetization(t *testing.T) {
	h, mux, cleanup := buildTestMuxWithMailer(t, nil)
	defer cleanup()

	email := "cpm.user@telefam.company"
	devFP := "carrier_gps_device_fp_999"
	idToken := makeUnsignedTestJWT(map[string]any{
		"email":          email,
		"email_verified": true,
		"name":           "Grace Wanjiku",
		"exp":            time.Now().Add(time.Hour).Unix(),
	})
	oauthBody, _ := json.Marshal(OAuthPayload{Provider: "google", IdentityToken: idToken})
	oauthReq := httptest.NewRequest(http.MethodPost, "/v1/auth/oauth/google", bytes.NewReader(oauthBody))
	oauthReq.Header.Set("Content-Type", "application/json")
	oauthReq.Header.Set("X-Telefam-Device-FP", devFP)
	oauthRec := httptest.NewRecorder()
	mux.ServeHTTP(oauthRec, oauthReq)
	if oauthRec.Code != http.StatusOK {
		t.Fatalf("oauth failed: %d", oauthRec.Code)
	}
	var sess AuthSessionResponse
	_ = json.NewDecoder(oauthRec.Body).Decode(&sess)

	// Setup profile with KE phone (+254) while device carrier header says US -> server must persist US for monetization
	setupBody := `{"full_name":"Grace Wanjiku","username":"gracew","phone_number":"+254722111222","country_iso":"KE","gender":"Female","date_of_birth":"1996-02-20","location":"New York, United States","bio":"Creator"}`
	setupReq := httptest.NewRequest(http.MethodPost, "/v1/profile/setup", bytes.NewBufferString(setupBody))
	setupReq.Header.Set("Content-Type", "application/json")
	setupReq.Header.Set("Authorization", "Bearer "+sess.SessionToken)
	setupReq.Header.Set("X-Telefam-Device-FP", devFP)
	setupReq.Header.Set("X-Telefam-Carrier-ISO", "US")
	setupRec := httptest.NewRecorder()
	mux.ServeHTTP(setupRec, setupReq)
	if setupRec.Code != http.StatusOK {
		t.Fatalf("setup failed: %d %s", setupRec.Code, setupRec.Body.String())
	}

	h.mu.Lock()
	prof := h.profiles[sess.UserID]
	attr := h.countryAttributions[sess.UserID]
	h.mu.Unlock()
	if prof.CountryISO != "US" {
		t.Fatalf("expected carrier US to override KE phone dial country in stored profile, got %q", prof.CountryISO)
	}
	if attr == nil || attr.MonetizationCPMCountry != "US" || attr.PhoneCountryISO != "KE" || attr.AttributionSource != "carrier" {
		t.Fatalf("unexpected country attribution: %+v", attr)
	}

	// Simulate offline GPS capture synced when internet returns with GB country
	telemPayload := DeviceContextTelemetryPayload{
		DeviceName:       "Samsung Galaxy S24",
		Platform:         "android",
		HardwareModel:    "SM-S921B",
		CarrierISO:       "US",
		SimISO:           "KE",
		GpsCountryISO:    "GB",
		GpsCity:          "London",
		LocaleCountryISO: "KE",
		Timezone:         "Europe/London",
		SignalSource:     "gps_passive",
		CapturedAtMs:     time.Now().Add(-5 * time.Minute).UnixMilli(),
	}
	telemBytes, _ := json.Marshal(telemPayload)
	telemReq := httptest.NewRequest(http.MethodPost, "/v1/telemetry/device-context", bytes.NewReader(telemBytes))
	telemReq.Header.Set("Content-Type", "application/json")
	telemReq.Header.Set("Authorization", "Bearer "+sess.SessionToken)
	telemReq.Header.Set("X-Telefam-Device-FP", devFP)
	telemRec := httptest.NewRecorder()
	mux.ServeHTTP(telemRec, telemReq)
	if telemRec.Code != http.StatusOK {
		t.Fatalf("telemetry sync failed: %d %s", telemRec.Code, telemRec.Body.String())
	}

	h.mu.Lock()
	profAfter := h.profiles[sess.UserID]
	attrAfter := h.countryAttributions[sess.UserID]
	h.mu.Unlock()
	if profAfter.CountryISO != "GB" {
		t.Fatalf("expected GPS GB to update profile CountryISO, got %q", profAfter.CountryISO)
	}
	if attrAfter.MonetizationCPMCountry != "GB" || attrAfter.AttributionSource != "gps" || !strings.Contains(attrAfter.ResolvedLocation, "London") {
		t.Fatalf("unexpected GPS monetization attribution: %+v", attrAfter)
	}
}
