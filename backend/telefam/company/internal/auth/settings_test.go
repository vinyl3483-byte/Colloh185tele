package auth

import (
	"bytes"
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"testing"

	"telefam.company/backend/internal/audit"
	"telefam.company/backend/internal/contacts"
	"telefam.company/backend/internal/fingerprint"
	"telefam.company/backend/internal/mailer"
	"telefam.company/backend/internal/posts"
	"telefam.company/backend/internal/ratelimit"
)

func newTestMuxAndTokens() (*Handler, http.Handler, *TokenService) {
	secret := []byte("0123456789abcdef0123456789abcdef")
	fp := fingerprint.NewService(secret)
	limiter := ratelimit.NewLimiter()
	otp := NewOtpService(secret, "https://telefam.company")
	tokens := NewTokenService(secret)
	logger := audit.NewLogger()
	m := mailer.NewResendClient("", "noreply@telefam.company")
	c := contacts.NewService()
	h := NewHandler(fp, limiter, otp, tokens, logger, m, c)
	mux := http.NewServeMux()
	h.RegisterRoutes(mux)
	return h, mux, tokens
}

func authReq(method, path string, body any, token, deviceFP string) *http.Request {
	var buf bytes.Buffer
	if body != nil {
		_ = json.NewEncoder(&buf).Encode(body)
	}
	req := httptest.NewRequest(method, path, &buf)
	req.Header.Set("Authorization", "Bearer "+token)
	req.Header.Set("X-Telefam-Device-FP", deviceFP)
	req.Header.Set("Content-Type", "application/json")
	return req
}

func TestSettingsPrivacyMutesTicketsAndLogoutAllDevices(t *testing.T) {
	h, mux, tokens := newTestMuxAndTokens()
	dfp1 := "device-fp-1"
	dfp2 := "device-fp-2"
	pair1, _, err := tokens.IssueInitialPair("u_caller", "caller@telefam.company", dfp1)
	if err != nil {
		t.Fatalf("IssueInitialPair 1 failed: %v", err)
	}
	pair2, _, err := tokens.IssueInitialPair("u_caller", "caller@telefam.company", dfp2)
	if err != nil {
		t.Fatalf("IssueInitialPair 2 failed: %v", err)
	}
	targetPair, _, err := tokens.IssueInitialPair("u_target", "target@telefam.company", "device-target")
	if err != nil {
		t.Fatalf("IssueInitialPair target failed: %v", err)
	}

	h.mu.Lock()
	h.profiles["u_caller"] = storedProfile{UserID: "u_caller", FullName: "Caller", Username: "caller"}
	h.profiles["u_target"] = storedProfile{UserID: "u_target", FullName: "Target User", Username: "targetuser"}
	h.mu.Unlock()

	_, _ = h.posts.CreatePost("u_target", posts.CreatePostRequest{
		ClientOpID: "op-target-1",
		Mode:       posts.ModeText,
		Privacy:    posts.PrivacyPublic,
		TextContent: "Sports update tonight",
		Hashtags:   []string{"sports"},
	})

	// 1. Target turns on Private account & Only-Me following visibility
	w := httptest.NewRecorder()
	mux.ServeHTTP(w, authReq("PUT", "/v1/settings/privacy", PrivacySettingsDTO{
		IsPrivateAccount:    true,
		WhoCanComment:       "friends",
		WhoCanMention:       "no_one",
		WhoCanFindYou:       "contacts",
		ShowActivityStatus:  false,
		LikesVisibility:     "only_me",
		FollowingVisibility: "only_me",
	}, targetPair.AccessToken, "device-target"))
	if w.Code != http.StatusOK {
		t.Fatalf("privacy update failed: %d %s", w.Code, w.Body.String())
	}

	// Non-follower caller viewing target profile sees private status & 0 posts
	wProf := httptest.NewRecorder()
	mux.ServeHTTP(wProf, authReq("GET", "/v1/users/u_target/profile", nil, pair1.AccessToken, dfp1))
	if wProf.Code != http.StatusOK {
		t.Fatalf("get target profile failed: %d", wProf.Code)
	}
	var profResp UserProfileResponse
	_ = json.Unmarshal(wProf.Body.Bytes(), &profResp)
	if !profResp.IsPrivateAccount || len(profResp.Posts) != 0 {
		t.Fatalf("expected private profile with hidden posts for non-follower, got private=%v posts=%d", profResp.IsPrivateAccount, len(profResp.Posts))
	}

	// 2. Mute target & verify muted accounts list + feed suppression
	wMute := httptest.NewRecorder()
	mux.ServeHTTP(wMute, authReq("POST", "/v1/users/u_target/mute", nil, pair1.AccessToken, dfp1))
	if wMute.Code != http.StatusOK {
		t.Fatalf("mute failed: %d", wMute.Code)
	}
	wMutedList := httptest.NewRecorder()
	mux.ServeHTTP(wMutedList, authReq("GET", "/v1/settings/muted-accounts", nil, pair1.AccessToken, dfp1))
	if !bytes.Contains(wMutedList.Body.Bytes(), []byte("u_target")) {
		t.Fatalf("expected u_target in muted list: %s", wMutedList.Body.String())
	}

	// 3. Content preferences not-interested topic suppression
	wContent := httptest.NewRecorder()
	mux.ServeHTTP(wContent, authReq("PUT", "/v1/settings/content-preferences", ContentPreferencesDTO{
		SuggestedContentEnabled: true,
		NotInterestedTopics:     []string{"#Sports"},
		LanguageCode:            "en",
		AutoplayVideos:          false,
		VideoQuality:            "data_saver",
		DataSaver:               true,
		CaptionsEnabled:         true,
		ReducedMotion:           true,
	}, pair1.AccessToken, dfp1))
	if wContent.Code != http.StatusOK {
		t.Fatalf("content prefs update failed: %d", wContent.Code)
	}

	// 4. Problem report returns reference number & persists in ticket history
	wRep := httptest.NewRecorder()
	mux.ServeHTTP(wRep, authReq("POST", "/v1/support/reports", ProblemReportPayload{
		ClientOpID:  "rep-op-1",
		Category:    "Video playback",
		Description: "Buffer stalled on 4:3 clip",
	}, pair1.AccessToken, dfp1))
	if wRep.Code != http.StatusCreated {
		t.Fatalf("problem report failed: %d %s", wRep.Code, wRep.Body.String())
	}
	wTickets := httptest.NewRecorder()
	mux.ServeHTTP(wTickets, authReq("GET", "/v1/support/tickets", nil, pair1.AccessToken, dfp1))
	if !bytes.Contains(wTickets.Body.Bytes(), []byte("TF-")) {
		t.Fatalf("expected TF- reference number in tickets: %s", wTickets.Body.String())
	}

	// 5. Log out of ALL devices revokes both pair1 and pair2 immediately
	wLogout := httptest.NewRecorder()
	mux.ServeHTTP(wLogout, authReq("POST", "/v1/auth/logout", LogoutPayload{AllDevices: true}, pair1.AccessToken, dfp1))
	if wLogout.Code != http.StatusOK {
		t.Fatalf("logout failed: %d", wLogout.Code)
	}

	wCheck2 := httptest.NewRecorder()
	mux.ServeHTTP(wCheck2, authReq("GET", "/v1/settings", nil, pair2.AccessToken, dfp2))
	if wCheck2.Code != http.StatusUnauthorized {
		t.Fatalf("expected device 2 token to be revoked after all_devices logout, got %d", wCheck2.Code)
	}
}
