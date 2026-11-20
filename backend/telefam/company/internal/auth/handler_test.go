package auth

import (
	"bytes"
	"encoding/json"
	"image"
	"image/color"
	"image/jpeg"
	"net/http"
	"net/http/httptest"
	"regexp"
	"strings"
	"testing"
	"time"

	"telefam.company/backend/internal/audit"
	"telefam.company/backend/internal/contacts"
	"telefam.company/backend/internal/feeds"
	"telefam.company/backend/internal/fingerprint"
	"telefam.company/backend/internal/mailer"
	"telefam.company/backend/internal/ratelimit"
)

func TestOtpSendVerifyAndRateLimitFlow(t *testing.T) {
	var sentHTML string
	var sentTo []string
	var authHeader string

	resendServer := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		authHeader = r.Header.Get("Authorization")
		var payload struct {
			To   []string `json:"to"`
			HTML string   `json:"html"`
		}
		if err := json.NewDecoder(r.Body).Decode(&payload); err != nil {
			t.Fatalf("failed to decode resend payload: %v", err)
		}
		sentTo = payload.To
		sentHTML = payload.HTML
		w.WriteHeader(http.StatusOK)
		_, _ = w.Write([]byte(`{"id":"test_msg_1"}`))
	}))
	defer resendServer.Close()

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

	deviceFP := "android_device_fp_1234567890"
	reqBody := bytes.NewBufferString(`{"email":"colloh185@gmail.com"}`)
	req := httptest.NewRequest(http.MethodPost, "/v1/auth/otp/request", reqBody)
	req.Header.Set("Content-Type", "application/json")
	req.Header.Set("X-Telefam-Device-FP", deviceFP)
	req.RemoteAddr = "203.0.113.10:1234"
	rec := httptest.NewRecorder()

	mux.ServeHTTP(rec, req)
	if rec.Code != http.StatusOK {
		t.Fatalf("expected 200 OK, got %d: %s", rec.Code, rec.Body.String())
	}
	if authHeader != "Bearer test_key_only" {
		t.Fatalf("unexpected authorization header: %q", authHeader)
	}
	if len(sentTo) != 1 || sentTo[0] != "colloh185@gmail.com" {
		t.Fatalf("unexpected recipients: %v", sentTo)
	}

	codeMatch := regexp.MustCompile(`>([0-9]{6})<`).FindStringSubmatch(sentHTML)
	if len(codeMatch) != 2 {
		t.Fatalf("expected 6-digit OTP inside email HTML, got: %s", sentHTML)
	}
	otpCode := codeMatch[1]

	// Immediate second request from same device/email must trigger sanitized 429 cooldown
	req2 := httptest.NewRequest(http.MethodPost, "/v1/auth/otp/request", bytes.NewBufferString(`{"email":"colloh185@gmail.com"}`))
	req2.Header.Set("Content-Type", "application/json")
	req2.Header.Set("X-Telefam-Device-FP", deviceFP)
	req2.RemoteAddr = "203.0.113.10:1234"
	rec2 := httptest.NewRecorder()
	mux.ServeHTTP(rec2, req2)
	if rec2.Code != http.StatusTooManyRequests {
		t.Fatalf("expected 429 during cooldown, got %d", rec2.Code)
	}
	if strings.Contains(rec2.Body.String(), "cooldown") {
		t.Fatalf("error message leaked internal details: %s", rec2.Body.String())
	}

	// Verify issued OTP
	verifyReq := httptest.NewRequest(
		http.MethodPost,
		"/v1/auth/otp/verify",
		bytes.NewBufferString(`{"email":"colloh185@gmail.com","otp":"`+otpCode+`"}`),
	)
	verifyReq.Header.Set("Content-Type", "application/json")
	verifyReq.Header.Set("X-Telefam-Device-FP", deviceFP)
	verifyReq.RemoteAddr = "203.0.113.10:1234"
	verifyRec := httptest.NewRecorder()
	mux.ServeHTTP(verifyRec, verifyReq)
	if verifyRec.Code != http.StatusOK {
		t.Fatalf("expected 200 OK verifying OTP, got %d: %s", verifyRec.Code, verifyRec.Body.String())
	}

	var session AuthSessionResponse
	if err := json.NewDecoder(verifyRec.Body).Decode(&session); err != nil {
		t.Fatalf("decode session failed: %v", err)
	}
	if !session.Success || session.SessionToken == "" || session.RefreshToken == "" {
		t.Fatalf("expected valid session pair, got: %+v", session)
	}
}

func TestContactsRelationshipsPaginationAndRateLimit(t *testing.T) {
	secret := []byte("0123456789abcdef0123456789abcdef")
	fp := fingerprint.NewService(secret)
	limiter := ratelimit.NewLimiter()
	otpSvc := NewOtpService(secret, "https://telefam.company")
	tokens := NewTokenService(secret)
	aud := audit.NewLogger()
	contactsSvc := contacts.NewService()
	h := NewHandler(fp, limiter, otpSvc, tokens, aud, nil, contactsSvc)

	deviceA := "android_device_a_123456"
	deviceFP, _ := fp.ExtractDeviceFingerprint(&http.Request{Header: http.Header{"X-Telefam-Device-FP": []string{deviceA}}})
	pairA, _, _ := tokens.IssueInitialPair("user_a", "a@telefam.company", deviceFP)

	contactsSvc.RegisterOrUpdateUser(contacts.DirectoryUser{UserID: "user_a", FullName: "Alpha User", Username: "alpha", Country: "Kenya", CountryISO: "KE"}, deviceFP)
	contactsSvc.RegisterOrUpdateUser(contacts.DirectoryUser{UserID: "user_b", FullName: "Amara Okafor", Username: "amara_reads", Country: "Nigeria", CountryISO: "NG"}, "dev_b")
	contactsSvc.RegisterOrUpdateUser(contacts.DirectoryUser{UserID: "user_c", FullName: "Brian Kiprop", Username: "brian_k", Country: "Kenya", CountryISO: "KE"}, "dev_c")

	// User B follows User A -> User A should see FOLLOW_BACK
	contactsSvc.SetFollowing("user_b", "user_a", "dev_b", true)

	mux := http.NewServeMux()
	h.RegisterRoutes(mux)

	listReq := httptest.NewRequest(http.MethodGet, "/v1/contacts?filter=all&page=1&page_size=1", nil)
	listReq.Header.Set("Authorization", "Bearer "+pairA.AccessToken)
	listReq.Header.Set("X-Telefam-Device-FP", deviceA)
	listRec := httptest.NewRecorder()
	mux.ServeHTTP(listRec, listReq)
	if listRec.Code != http.StatusOK {
		t.Fatalf("expected 200 OK listing contacts, got %d", listRec.Code)
	}
	var pageResp contacts.ContactsPageResponse
	_ = json.NewDecoder(listRec.Body).Decode(&pageResp)
	if !pageResp.HasMore || pageResp.TotalCount != 2 || pageResp.Items[0].RelationshipState != contacts.RelationFollowBack {
		t.Fatalf("unexpected contacts page: %+v", pageResp)
	}

	// Follow user_b -> mutual follow yields FRIENDS
	followReq := httptest.NewRequest(http.MethodPost, "/v1/contacts/follow", bytes.NewBufferString(`{"target_user_id":"user_b"}`))
	followReq.Header.Set("Authorization", "Bearer "+pairA.AccessToken)
	followReq.Header.Set("X-Telefam-Device-FP", deviceA)
	followRec := httptest.NewRecorder()
	mux.ServeHTTP(followRec, followReq)
	var mutResp contacts.FollowMutationResponse
	_ = json.NewDecoder(followRec.Body).Decode(&mutResp)
	if mutResp.RelationshipState != contacts.RelationFriends {
		t.Fatalf("expected FRIENDS after mutual follow, got %s", mutResp.RelationshipState)
	}
}

func TestPostMediaSongsHashtagsAndContentID(t *testing.T) {
	itunesMock := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		_, _ = w.Write([]byte(`{"results":[{"trackId":9988,"trackName":"Golden Hour","artistName":"JVKE","collectionName":"this is what ____ feels like","artworkUrl100":"https://example.com/art.jpg","previewUrl":"https://example.com/prev.m4a","trackTimeMillis":209000}]}`))
	}))
	defer itunesMock.Close()

	secret := []byte("0123456789abcdef0123456789abcdef")
	fp := fingerprint.NewService(secret)
	limiter := ratelimit.NewLimiter()
	otpSvc := NewOtpService(secret, "https://telefam.company")
	tokens := NewTokenService(secret)
	aud := audit.NewLogger()
	contactsSvc := contacts.NewService()
	h := NewHandler(fp, limiter, otpSvc, tokens, aud, nil, contactsSvc)

	h.SetPostsService(posts.NewServiceWithSongEndpoints(itunesMock.URL, itunesMock.URL))
	deviceA := "android_device_post_123"
	deviceFP, _ := fp.ExtractDeviceFingerprint(&http.Request{Header: http.Header{"X-Telefam-Device-FP": []string{deviceA}}})
	pairA, _, _ := tokens.IssueInitialPair("author_1", "author@telefam.company", deviceFP)

	mux := http.NewServeMux()
	h.RegisterRoutes(mux)

	// 1. Search songs via mocked public iTunes API endpoint
	songReq := httptest.NewRequest(http.MethodGet, "/v1/songs/search?q=golden", nil)
	songReq.Header.Set("Authorization", "Bearer "+pairA.AccessToken)
	songReq.Header.Set("X-Telefam-Device-FP", deviceA)
	songRec := httptest.NewRecorder()
	mux.ServeHTTP(songRec, songReq)
	if songRec.Code != http.StatusOK || !strings.Contains(songRec.Body.String(), "Golden Hour") {
		t.Fatalf("expected song search result Golden Hour, got %d: %s", songRec.Code, songRec.Body.String())
	}

	// 2. Upload valid MP4 header + per-frame client dHashes -> verifies deterministic vid_ content_id + renditions
	fakeMp4 := []byte{0x00, 0x00, 0x00, 0x18, 'f', 't', 'y', 'p', 'm', 'p', '4', '2', 0x00, 0x00, 0x00, 0x00, 'm', 'd', 'a', 't', 1, 2, 3, 4}
	vidReq := httptest.NewRequest(http.MethodPost, "/v1/media/video?duration_ms=12000&trim_start_ms=1000&trim_end_ms=9000&width=1280&height=720", bytes.NewReader(fakeMp4))
	vidReq.Header.Set("Authorization", "Bearer "+pairA.AccessToken)
	vidReq.Header.Set("X-Telefam-Device-FP", deviceA)
	vidReq.Header.Set("X-Telefam-Frame-Hashes", "0f0f0f0f0f0f0f0f,a1a1a1a1a1a1a1a1")
	vidRec := httptest.NewRecorder()
	mux.ServeHTTP(vidRec, vidReq)
	if vidRec.Code != http.StatusOK {
		t.Fatalf("expected 200 uploading mp4, got %d: %s", vidRec.Code, vidRec.Body.String())
	}
	var mediaResp MediaUploadResponse
	_ = json.NewDecoder(vidRec.Body).Decode(&mediaResp)
	if !strings.HasPrefix(mediaResp.ContentID, "vid_") || len(mediaResp.Renditions) < 2 {
		t.Fatalf("unexpected video upload response: %+v", mediaResp)
	}

	// 3. Create video post with hashtag -> verify hashtag post_count increments
	postBody := `{"client_op_id":"op_v1","mode":"video","caption":"Weekend trip","video_media_id":"` + mediaResp.MediaID + `","trim_start_ms":1000,"trim_end_ms":9000,"mute_original_audio":true,"content_id":"` + mediaResp.ContentID + `","hashtags":["telefam","travel"]}`
	postReq := httptest.NewRequest(http.MethodPost, "/v1/posts", bytes.NewBufferString(postBody))
	postReq.Header.Set("Authorization", "Bearer "+pairA.AccessToken)
	postReq.Header.Set("X-Telefam-Device-FP", deviceA)
	postRec := httptest.NewRecorder()
	mux.ServeHTTP(postRec, postReq)
	if postRec.Code != http.StatusOK {
		t.Fatalf("expected 200 creating post, got %d: %s", postRec.Code, postRec.Body.String())
	}

	tagReq := httptest.NewRequest(http.MethodGet, "/v1/hashtags/search?q=telefam", nil)
	tagReq.Header.Set("Authorization", "Bearer "+pairA.AccessToken)
	tagReq.Header.Set("X-Telefam-Device-FP", deviceA)
	tagRec := httptest.NewRecorder()
	mux.ServeHTTP(tagRec, tagReq)
	if tagRec.Code != http.StatusOK || !strings.Contains(tagRec.Body.String(), `"post_count":1`) {
		t.Fatalf("expected hashtag post_count 1, got %d: %s", tagRec.Code, tagRec.Body.String())
	}
}

func TestFeedsDynamicRecommendationsRangeStreamingRepostAndOwnerEditDelete(t *testing.T) {
	secret := []byte("0123456789abcdef0123456789abcdef")
	fp := fingerprint.NewService(secret)
	limiter := ratelimit.NewLimiter()
	otpSvc := NewOtpService(secret, "https://telefam.company")
	tokens := NewTokenService(secret)
	aud := audit.NewLogger()
	contactsSvc := contacts.NewService()
	h := NewHandler(fp, limiter, otpSvc, tokens, aud, nil, contactsSvc)

	deviceA := "android_device_feed_1"
	deviceB := "android_device_feed_2"
	fpA, _ := fp.ExtractDeviceFingerprint(&http.Request{Header: http.Header{"X-Telefam-Device-FP": []string{deviceA}}})
	fpB, _ := fp.ExtractDeviceFingerprint(&http.Request{Header: http.Header{"X-Telefam-Device-FP": []string{deviceB}}})

	contactsSvc.RegisterOrUpdateUser(contacts.DirectoryUser{UserID: "creator_cook", Username: "chef_amara", FullName: "Amara"}, fpA)
	contactsSvc.RegisterOrUpdateUser(contacts.DirectoryUser{UserID: "creator_night", Username: "rohan.verma", FullName: "Rohan"}, fpB)

	pairViewer, _, _ := tokens.IssueInitialPair("viewer_1", "viewer@telefam.company", fpA)
	pairCook, _, _ := tokens.IssueInitialPair("creator_cook", "cook@telefam.company", fpA)
	pairNight, _, _ := tokens.IssueInitialPair("creator_night", "night@telefam.company", fpB)

	mux := http.NewServeMux()
	h.RegisterRoutes(mux)

	uploadVideo := func(token, dev string) string {
		mp4 := []byte{0x00, 0x00, 0x00, 0x18, 'f', 't', 'y', 'p', 'm', 'p', '4', '2', 0, 0, 0, 0, 'm', 'd', 'a', 't', 1, 2, 3, 4}
		req := httptest.NewRequest(http.MethodPost, "/v1/media/video?duration_ms=10000&width=1080&height=1920", bytes.NewReader(mp4))
		req.Header.Set("Authorization", "Bearer "+token)
		req.Header.Set("X-Telefam-Device-FP", dev)
		rec := httptest.NewRecorder()
		mux.ServeHTTP(rec, req)
		var resp MediaUploadResponse
		_ = json.NewDecoder(rec.Body).Decode(&resp)
		return resp.MediaID
	}

	cookMedia := uploadVideo(pairCook.AccessToken, deviceA)
	nightMedia := uploadVideo(pairNight.AccessToken, deviceB)

	createPost := func(token, dev, body string) string {
		req := httptest.NewRequest(http.MethodPost, "/v1/posts", bytes.NewBufferString(body))
		req.Header.Set("Authorization", "Bearer "+token)
		req.Header.Set("X-Telefam-Device-FP", dev)
		rec := httptest.NewRecorder()
		mux.ServeHTTP(rec, req)
		var out struct {
			Post struct {
				PostID string `json:"post_id"`
			} `json:"post"`
		}
		_ = json.NewDecoder(rec.Body).Decode(&out)
		return out.Post.PostID
	}

	cookPostID := createPost(pairCook.AccessToken, deviceA, `{"client_op_id":"op_c1","mode":"video","caption":"Quick pasta","video_media_id":"`+cookMedia+`","hashtags":["cooking","pasta"]}`)
	nightPostID := createPost(pairNight.AccessToken, deviceB, `{"client_op_id":"op_n1","mode":"video","caption":"Beautiful nights","video_media_id":"`+nightMedia+`","hashtags":["nightvibes","citylights"]}`)

	// 1. Verify RFC 7233 HTTP 206 Partial Content range streaming works for Media3/AVPlayer preloading
	rangeReq := httptest.NewRequest(http.MethodGet, "/v1/media/asset/"+nightMedia, nil)
	rangeReq.Header.Set("Range", "bytes=0-11")
	rangeRec := httptest.NewRecorder()
	mux.ServeHTTP(rangeRec, rangeReq)
	if rangeRec.Code != http.StatusPartialContent || rangeRec.Body.Len() != 12 {
		t.Fatalf("expected 206 Partial Content 12 bytes, got %d (%d bytes)", rangeRec.Code, rangeRec.Body.Len())
	}

	// 2. Viewer likes nightPostID -> nightPostID ranks first on For You
	interact := func(body string) {
		req := httptest.NewRequest(http.MethodPost, "/v1/feeds/interact", bytes.NewBufferString(body))
		req.Header.Set("Authorization", "Bearer "+pairViewer.AccessToken)
		req.Header.Set("X-Telefam-Device-FP", deviceA)
		rec := httptest.NewRecorder()
		mux.ServeHTTP(rec, req)
		if rec.Code != http.StatusOK {
			t.Fatalf("interact failed: %d %s", rec.Code, rec.Body.String())
		}
	}
	interact(`{"post_id":"` + nightPostID + `","action":"like"}`)
	interact(`{"post_id":"` + nightPostID + `","action":"watch","watch_ms":9000,"duration_ms":10000}`)

	fetchFeeds := func(tab string) feeds.FeedPageResponse {
		req := httptest.NewRequest(http.MethodGet, "/v1/feeds?tab="+tab, nil)
		req.Header.Set("Authorization", "Bearer "+pairViewer.AccessToken)
		req.Header.Set("X-Telefam-Device-FP", deviceA)
		rec := httptest.NewRecorder()
		mux.ServeHTTP(rec, req)
		var resp feeds.FeedPageResponse
		_ = json.NewDecoder(rec.Body).Decode(&resp)
		return resp
	}
	feed1 := fetchFeeds("for_you")
	if len(feed1.Items) != 2 || feed1.Items[0].PostID != nightPostID {
		t.Fatalf("expected nightPostID ranked first after like+watch, got %+v", feed1.Items)
	}

	// 3. Viewer interests dynamically shift: favourite cooking + mark nightPost not_interested
	interact(`{"post_id":"` + nightPostID + `","action":"not_interested"}`)
	interact(`{"post_id":"` + cookPostID + `","action":"favourite"}`)
	feed2 := fetchFeeds("for_you")
	if len(feed2.Items) != 1 || feed2.Items[0].PostID != cookPostID {
		t.Fatalf("expected dynamic interest pivot to cookPostID only, got %+v", feed2.Items)
	}

	// 4. Repost makes cookPostID show as Reposted in feed
	interact(`{"post_id":"` + cookPostID + `","action":"repost"}`)
	feedRepost := fetchFeeds("following")
	if len(feedRepost.Items) != 1 || !feedRepost.Items[0].IsReposted {
		t.Fatalf("expected reposted item in following feed, got %+v", feedRepost.Items)
	}

	// 5. Search shortcut generation from live corpus hashtags
	searchReq := httptest.NewRequest(http.MethodGet, "/v1/feeds/search?q=pasta&shortcut=cooking", nil)
	searchReq.Header.Set("Authorization", "Bearer "+pairViewer.AccessToken)
	searchReq.Header.Set("X-Telefam-Device-FP", deviceA)
	searchRec := httptest.NewRecorder()
	mux.ServeHTTP(searchRec, searchReq)
	var searchResp feeds.FeedSearchResponse
	_ = json.NewDecoder(searchRec.Body).Decode(&searchResp)
	if len(searchResp.Items) != 1 || len(searchResp.Shortcuts) < 2 {
		t.Fatalf("unexpected search response: %+v", searchResp)
	}

	// 6. Owner enforcement: non-owner cannot edit/delete (403); owner can edit & delete (200)
	forbiddenEdit := httptest.NewRequest(http.MethodPatch, "/v1/posts/"+cookPostID, bytes.NewBufferString(`{"caption":"hijacked"}`))
	forbiddenEdit.Header.Set("Authorization", "Bearer "+pairViewer.AccessToken)
	forbiddenEdit.Header.Set("X-Telefam-Device-FP", deviceA)
	forbiddenRec := httptest.NewRecorder()
	mux.ServeHTTP(forbiddenRec, forbiddenEdit)
	if forbiddenRec.Code != http.StatusForbidden {
		t.Fatalf("expected 403 Forbidden for non-owner edit, got %d", forbiddenRec.Code)
	}

	ownerEdit := httptest.NewRequest(http.MethodPatch, "/v1/posts/"+cookPostID, bytes.NewBufferString(`{"caption":"Updated pasta recipe"}`))
	ownerEdit.Header.Set("Authorization", "Bearer "+pairCook.AccessToken)
	ownerEdit.Header.Set("X-Telefam-Device-FP", deviceA)
	ownerEditRec := httptest.NewRecorder()
	mux.ServeHTTP(ownerEditRec, ownerEdit)
	if ownerEditRec.Code != http.StatusOK {
		t.Fatalf("expected 200 OK for owner edit, got %d", ownerEditRec.Code)
	}

	ownerDel := httptest.NewRequest(http.MethodDelete, "/v1/posts/"+cookPostID, nil)
	ownerDel.Header.Set("Authorization", "Bearer "+pairCook.AccessToken)
	ownerDel.Header.Set("X-Telefam-Device-FP", deviceA)
	ownerDelRec := httptest.NewRecorder()
	mux.ServeHTTP(ownerDelRec, ownerDel)
	if ownerDelRec.Code != http.StatusOK {
		t.Fatalf("expected 200 OK for owner delete, got %d", ownerDelRec.Code)
	}
}

func TestUserProfileViewsConnectionsBlockAndReport(t *testing.T) {
	secret := []byte("0123456789abcdef0123456789abcdef")
	fp := fingerprint.NewService(secret)
	limiter := ratelimit.NewLimiter()
	otpSvc := NewOtpService(secret, "https://telefam.company")
	tokens := NewTokenService(secret)
	aud := audit.NewLogger()
	contactsSvc := contacts.NewService()
	h := NewHandler(fp, limiter, otpSvc, tokens, aud, nil, contactsSvc)

	deviceA := "android_device_prof_1"
	deviceB := "android_device_prof_2"
	fpA, _ := fp.ExtractDeviceFingerprint(&http.Request{Header: http.Header{"X-Telefam-Device-FP": []string{deviceA}}})
	fpB, _ := fp.ExtractDeviceFingerprint(&http.Request{Header: http.Header{"X-Telefam-Device-FP": []string{deviceB}}})

	contactsSvc.RegisterOrUpdateUser(contacts.DirectoryUser{UserID: "viewer_p", Username: "viewer_p", FullName: "Viewer User", Country: "Kenya", CountryISO: "KE"}, fpA)
	contactsSvc.RegisterOrUpdateUser(contacts.DirectoryUser{UserID: "alex_p", Username: "alexmwangi", FullName: "Alex Mwangi", Country: "Nairobi, Kenya", CountryISO: "KE"}, fpB)
	contactsSvc.RegisterOrUpdateUser(contacts.DirectoryUser{UserID: "friend_p", Username: "amara", FullName: "Amara", Country: "Nigeria", CountryISO: "NG"}, fpB)
	contactsSvc.SetFollowing("alex_p", "viewer_p", fpB, true)
	contactsSvc.SetFollowing("friend_p", "alex_p", fpB, true)

	pairViewer, _, _ := tokens.IssueInitialPair("viewer_p", "viewerp@telefam.company", fpA)
	pairAlex, _, _ := tokens.IssueInitialPair("alex_p", "alexp@telefam.company", fpB)

	mux := http.NewServeMux()
	h.RegisterRoutes(mux)

	mp4 := []byte{0x00, 0x00, 0x00, 0x18, 'f', 't', 'y', 'p', 'm', 'p', '4', '2', 0, 0, 0, 0, 'm', 'd', 'a', 't', 1, 2, 3, 4}
	upReq := httptest.NewRequest(http.MethodPost, "/v1/media/video?duration_ms=8000&width=1080&height=1920", bytes.NewReader(mp4))
	upReq.Header.Set("Authorization", "Bearer "+pairAlex.AccessToken)
	upReq.Header.Set("X-Telefam-Device-FP", deviceB)
	upRec := httptest.NewRecorder()
	mux.ServeHTTP(upRec, upReq)
	var mediaResp MediaUploadResponse
	_ = json.NewDecoder(upRec.Body).Decode(&mediaResp)

	postReq := httptest.NewRequest(http.MethodPost, "/v1/posts", bytes.NewBufferString(`{"client_op_id":"op_alex_1","mode":"video","caption":"Coffee day","video_media_id":"`+mediaResp.MediaID+`"}`))
	postReq.Header.Set("Authorization", "Bearer "+pairAlex.AccessToken)
	postReq.Header.Set("X-Telefam-Device-FP", deviceB)
	postRec := httptest.NewRecorder()
	mux.ServeHTTP(postRec, postReq)
	var createdOut struct {
		Post struct {
			PostID string `json:"post_id"`
		} `json:"post"`
	}
	_ = json.NewDecoder(postRec.Body).Decode(&createdOut)

	// Record view + like from viewer -> verifies real counts (not hardcoded)
	for _, actionBody := range []string{
		`{"post_id":"` + createdOut.Post.PostID + `","action":"view"}`,
		`{"post_id":"` + createdOut.Post.PostID + `","action":"like"}`,
	} {
		iReq := httptest.NewRequest(http.MethodPost, "/v1/feeds/interact", bytes.NewBufferString(actionBody))
		iReq.Header.Set("Authorization", "Bearer "+pairViewer.AccessToken)
		iReq.Header.Set("X-Telefam-Device-FP", deviceA)
		iRec := httptest.NewRecorder()
		mux.ServeHTTP(iRec, iReq)
		if iRec.Code != http.StatusOK {
			t.Fatalf("interact failed: %d", iRec.Code)
		}
	}

	profReq := httptest.NewRequest(http.MethodGet, "/v1/users/alex_p/profile", nil)
	profReq.Header.Set("Authorization", "Bearer "+pairViewer.AccessToken)
	profReq.Header.Set("X-Telefam-Device-FP", deviceA)
	profRec := httptest.NewRecorder()
	mux.ServeHTTP(profRec, profReq)
	if profRec.Code != http.StatusOK {
		t.Fatalf("expected 200 getting profile, got %d", profRec.Code)
	}
	var profResp UserProfileResponse
	_ = json.NewDecoder(profRec.Body).Decode(&profResp)
	if !profResp.FollowsYou || profResp.RelationshipState != "FOLLOW_BACK" || profResp.LikesCount != 1 || len(profResp.Posts) != 1 || profResp.Posts[0].ViewsCount != 1 {
		t.Fatalf("unexpected user profile response: %+v", profResp)
	}

	followersReq := httptest.NewRequest(http.MethodGet, "/v1/users/alex_p/followers?page=1&page_size=20", nil)
	followersReq.Header.Set("Authorization", "Bearer "+pairViewer.AccessToken)
	followersReq.Header.Set("X-Telefam-Device-FP", deviceA)
	followersRec := httptest.NewRecorder()
	mux.ServeHTTP(followersRec, followersReq)
	if followersRec.Code != http.StatusOK || !strings.Contains(followersRec.Body.String(), "amara") {
		t.Fatalf("expected followers list to include amara, got %d: %s", followersRec.Code, followersRec.Body.String())
	}

	repReq := httptest.NewRequest(http.MethodPost, "/v1/users/alex_p/report", bytes.NewBufferString(`{"reason":"Impersonation","details":"Fake clone"}`))
	repReq.Header.Set("Authorization", "Bearer "+pairViewer.AccessToken)
	repReq.Header.Set("X-Telefam-Device-FP", deviceA)
	repRec := httptest.NewRecorder()
	mux.ServeHTTP(repRec, repReq)
	if repRec.Code != http.StatusOK || !strings.Contains(repRec.Body.String(), `"reported_user_id":"alex_p"`) {
		t.Fatalf("expected report success, got %d: %s", repRec.Code, repRec.Body.String())
	}

	blkReq := httptest.NewRequest(http.MethodPost, "/v1/users/alex_p/block", nil)
	blkReq.Header.Set("Authorization", "Bearer "+pairViewer.AccessToken)
	blkReq.Header.Set("X-Telefam-Device-FP", deviceA)
	blkRec := httptest.NewRecorder()
	mux.ServeHTTP(blkRec, blkReq)
	if blkRec.Code != http.StatusOK || !strings.Contains(blkRec.Body.String(), `"is_blocked":true`) {
		t.Fatalf("expected block success, got %d: %s", blkRec.Code, blkRec.Body.String())
	}

	unblkReq := httptest.NewRequest(http.MethodPost, "/v1/users/alex_p/unblock", nil)
	unblkReq.Header.Set("Authorization", "Bearer "+pairViewer.AccessToken)
	unblkReq.Header.Set("X-Telefam-Device-FP", deviceA)
	unblkRec := httptest.NewRecorder()
	mux.ServeHTTP(unblkRec, unblkReq)
	if unblkRec.Code != http.StatusOK || !strings.Contains(unblkRec.Body.String(), `"is_blocked":false`) {
		t.Fatalf("expected unblock success, got %d: %s", unblkRec.Code, unblkRec.Body.String())
	}

	meReq := httptest.NewRequest(http.MethodGet, "/v1/users/me/profile", nil)
	meReq.Header.Set("Authorization", "Bearer "+pairAlex.AccessToken)
	meReq.Header.Set("X-Telefam-Device-FP", deviceB)
	meRec := httptest.NewRecorder()
	mux.ServeHTTP(meRec, meReq)
	if meRec.Code != http.StatusOK || !strings.Contains(meRec.Body.String(), `"is_owner":true`) {
		t.Fatalf("expected own profile via /me, got %d: %s", meRec.Code, meRec.Body.String())
	}
}

func makeTestJPEGBytes(seed byte) []byte {
	img := image.NewRGBA(image.Rect(0, 0, 120, 80))
	for y := 0; y < 80; y++ {
		for x := 0; x < 120; x++ {
			img.SetRGBA(x, y, color.RGBA{
				R: uint8((x*3 + int(seed)*17) % 256),
				G: uint8((y*5 + int(seed)*31) % 256),
				B: uint8((x + y + int(seed)*11) % 256),
				A: 255,
			})
		}
	}
	var buf bytes.Buffer
	_ = jpeg.Encode(&buf, img, &jpeg.Options{Quality: 85})
	return buf.Bytes()
}

func TestProfileBackgroundsEditingUsernameCooldownAndPostPrivacy(t *testing.T) {
	secret := []byte("0123456789abcdef0123456789abcdef")
	fp := fingerprint.NewService(secret)
	limiter := ratelimit.NewLimiter()
	otpSvc := NewOtpService(secret, "https://telefam.company")
	tokens := NewTokenService(secret)
	aud := audit.NewLogger()
	contactsSvc := contacts.NewService()
	h := NewHandler(fp, limiter, otpSvc, tokens, aud, nil, contactsSvc)

	devOwner := "android_owner_fp_001"
	devFriend := "android_friend_fp_002"
	devStranger := "android_stranger_fp_003"
	fpOwner, _ := fp.ExtractDeviceFingerprint(&http.Request{Header: http.Header{"X-Telefam-Device-FP": []string{devOwner}}})
	fpFriend, _ := fp.ExtractDeviceFingerprint(&http.Request{Header: http.Header{"X-Telefam-Device-FP": []string{devFriend}}})
	fpStranger, _ := fp.ExtractDeviceFingerprint(&http.Request{Header: http.Header{"X-Telefam-Device-FP": []string{devStranger}}})

	pairOwner, _, _ := tokens.IssueInitialPair("owner_u1", "felistar@telefam.company", fpOwner)
	pairFriend, _, _ := tokens.IssueInitialPair("friend_u2", "friend@telefam.company", fpFriend)
	pairStranger, _, _ := tokens.IssueInitialPair("stranger_u3", "stranger@telefam.company", fpStranger)

	mux := http.NewServeMux()
	h.RegisterRoutes(mux)

	setupPayload := `{"full_name":"Felistar","username":"colloh","phone_number":"+254712345678","country_iso":"KE","gender":"Female","date_of_birth":"2001-05-12","location":"Konoin, Kenya","bio":"Building memories"}`
	setupReq := httptest.NewRequest(http.MethodPost, "/v1/profile/setup", bytes.NewBufferString(setupPayload))
	setupReq.Header.Set("Authorization", "Bearer "+pairOwner.AccessToken)
	setupReq.Header.Set("X-Telefam-Device-FP", devOwner)
	setupRec := httptest.NewRecorder()
	mux.ServeHTTP(setupRec, setupReq)
	if setupRec.Code != http.StatusOK {
		t.Fatalf("expected 200 setup, got %d: %s", setupRec.Code, setupRec.Body.String())
	}

	contactsSvc.RegisterOrUpdateUser(contacts.DirectoryUser{UserID: "friend_u2", Username: "mutual_friend", FullName: "Mutual Friend", Country: "Kenya", CountryISO: "KE"}, fpFriend)
	contactsSvc.RegisterOrUpdateUser(contacts.DirectoryUser{UserID: "stranger_u3", Username: "stranger", FullName: "Stranger", Country: "Kenya", CountryISO: "KE"}, fpStranger)
	contactsSvc.SetFollowing("owner_u1", "friend_u2", fpOwner, true)
	contactsSvc.SetFollowing("friend_u2", "owner_u1", fpFriend, true)

	var uploadedBgIDs []string
	for i := 1; i <= 5; i++ {
		bgReq := httptest.NewRequest(http.MethodPost, "/v1/media/background", bytes.NewReader(makeTestJPEGBytes(byte(i))))
		bgReq.Header.Set("Authorization", "Bearer "+pairOwner.AccessToken)
		bgReq.Header.Set("X-Telefam-Device-FP", devOwner)
		bgRec := httptest.NewRecorder()
		mux.ServeHTTP(bgRec, bgReq)
		if bgRec.Code != http.StatusOK {
			t.Fatalf("expected 200 uploading bg %d, got %d: %s", i, bgRec.Code, bgRec.Body.String())
		}
		var out struct {
			MediaID string `json:"media_id"`
			Sha256  string `json:"sha256_fingerprint"`
			DHash   string `json:"dhash_fingerprint"`
		}
		_ = json.NewDecoder(bgRec.Body).Decode(&out)
		if len(out.Sha256) != 64 || len(out.DHash) != 16 {
			t.Fatalf("expected SHA-256 + 64-bit dHash fingerprints, got sha=%q dhash=%q", out.Sha256, out.DHash)
		}
		uploadedBgIDs = append(uploadedBgIDs, out.MediaID)
	}
	sixthReq := httptest.NewRequest(http.MethodPost, "/v1/media/background", bytes.NewReader(makeTestJPEGBytes(6)))
	sixthReq.Header.Set("Authorization", "Bearer "+pairOwner.AccessToken)
	sixthReq.Header.Set("X-Telefam-Device-FP", devOwner)
	sixthRec := httptest.NewRecorder()
	mux.ServeHTTP(sixthRec, sixthReq)
	if sixthRec.Code != http.StatusConflict {
		t.Fatalf("expected 409 when uploading 6th background, got %d: %s", sixthRec.Code, sixthRec.Body.String())
	}

	delBgReq := httptest.NewRequest(http.MethodDelete, "/v1/profile/backgrounds/"+uploadedBgIDs[0], nil)
	delBgReq.Header.Set("Authorization", "Bearer "+pairOwner.AccessToken)
	delBgReq.Header.Set("X-Telefam-Device-FP", devOwner)
	delBgRec := httptest.NewRecorder()
	mux.ServeHTTP(delBgRec, delBgReq)
	if delBgRec.Code != http.StatusOK {
		t.Fatalf("expected 200 deleting background, got %d", delBgRec.Code)
	}

	visitorProfReq := httptest.NewRequest(http.MethodGet, "/v1/users/owner_u1/profile", nil)
	visitorProfReq.Header.Set("Authorization", "Bearer "+pairStranger.AccessToken)
	visitorProfReq.Header.Set("X-Telefam-Device-FP", devStranger)
	visitorProfRec := httptest.NewRecorder()
	mux.ServeHTTP(visitorProfRec, visitorProfReq)
	var visitorProf UserProfileResponse
	_ = json.NewDecoder(visitorProfRec.Body).Decode(&visitorProf)
	if len(visitorProf.BackgroundURLs) != 4 || len(visitorProf.Backgrounds) != 4 {
		t.Fatalf("expected visitor to receive 4 hero backgrounds, got %d urls", len(visitorProf.BackgroundURLs))
	}

	earlyUserChange := httptest.NewRequest(http.MethodPatch, "/v1/profile", bytes.NewBufferString(`{"full_name":"Felistar K","username":"colloh_new","location":"Nairobi, Kenya","bio":"Updated bio"}`))
	earlyUserChange.Header.Set("Authorization", "Bearer "+pairOwner.AccessToken)
	earlyUserChange.Header.Set("X-Telefam-Device-FP", devOwner)
	earlyUserRec := httptest.NewRecorder()
	mux.ServeHTTP(earlyUserRec, earlyUserChange)
	if earlyUserRec.Code != http.StatusTooManyRequests {
		t.Fatalf("expected 429 on early username change, got %d: %s", earlyUserRec.Code, earlyUserRec.Body.String())
	}

	bioEditReq := httptest.NewRequest(http.MethodPatch, "/v1/profile", bytes.NewBufferString(`{"full_name":"Felistar K","username":"colloh","location":"Konoin, Kenya, Angola","bio":"Traveler","gender":"Female","date_of_birth":"2001-05-12","country_iso":"KE","phone_number":"+254712345678"}`))
	bioEditReq.Header.Set("Authorization", "Bearer "+pairOwner.AccessToken)
	bioEditReq.Header.Set("X-Telefam-Device-FP", devOwner)
	bioEditRec := httptest.NewRecorder()
	mux.ServeHTTP(bioEditRec, bioEditReq)
	if bioEditRec.Code != http.StatusOK {
		t.Fatalf("expected 200 editing non-username fields during cooldown, got %d: %s", bioEditRec.Code, bioEditRec.Body.String())
	}

	h.mu.Lock()
	sp := h.profiles["owner_u1"]
	sp.UsernameChangedAtMs = time.Now().Add(-15 * 24 * time.Hour).UnixMilli()
	h.profiles["owner_u1"] = sp
	h.mu.Unlock()

	lateUserChange := httptest.NewRequest(http.MethodPatch, "/v1/profile", bytes.NewBufferString(`{"full_name":"Felistar K","username":"felistar_k","location":"Konoin, Kenya","bio":"Traveler"}`))
	lateUserChange.Header.Set("Authorization", "Bearer "+pairOwner.AccessToken)
	lateUserChange.Header.Set("X-Telefam-Device-FP", devOwner)
	lateUserRec := httptest.NewRecorder()
	mux.ServeHTTP(lateUserRec, lateUserChange)
	if lateUserRec.Code != http.StatusOK || !strings.Contains(lateUserRec.Body.String(), `"username":"felistar_k"`) {
		t.Fatalf("expected 200 changing username after 14d, got %d: %s", lateUserRec.Code, lateUserRec.Body.String())
	}

	mp4 := []byte{0x00, 0x00, 0x00, 0x18, 'f', 't', 'y', 'p', 'm', 'p', '4', '2', 0, 0, 0, 0, 'm', 'd', 'a', 't', 1, 2, 3, 4}
	upReq := httptest.NewRequest(http.MethodPost, "/v1/media/video?duration_ms=7000&width=1080&height=1920", bytes.NewReader(mp4))
	upReq.Header.Set("Authorization", "Bearer "+pairOwner.AccessToken)
	upReq.Header.Set("X-Telefam-Device-FP", devOwner)
	upRec := httptest.NewRecorder()
	mux.ServeHTTP(upRec, upReq)
	var upOut MediaUploadResponse
	_ = json.NewDecoder(upRec.Body).Decode(&upOut)

	postReq := httptest.NewRequest(http.MethodPost, "/v1/posts", bytes.NewBufferString(`{"client_op_id":"op_priv_1","mode":"video","caption":"Secret sunset","video_media_id":"`+upOut.MediaID+`","hashtags":["sunset"]}`))
	postReq.Header.Set("Authorization", "Bearer "+pairOwner.AccessToken)
	postReq.Header.Set("X-Telefam-Device-FP", devOwner)
	postRec := httptest.NewRecorder()
	mux.ServeHTTP(postRec, postReq)
	var pOut struct {
		Post struct {
			PostID string `json:"post_id"`
		} `json:"post"`
	}
	_ = json.NewDecoder(postRec.Body).Decode(&pOut)
	postID := pOut.Post.PostID

	privFriendsReq := httptest.NewRequest(http.MethodPatch, "/v1/posts/"+postID, bytes.NewBufferString(`{"privacy":"friends"}`))
	privFriendsReq.Header.Set("Authorization", "Bearer "+pairOwner.AccessToken)
	privFriendsReq.Header.Set("X-Telefam-Device-FP", devOwner)
	privFriendsRec := httptest.NewRecorder()
	mux.ServeHTTP(privFriendsRec, privFriendsReq)
	if privFriendsRec.Code != http.StatusOK {
		t.Fatalf("expected 200 switching privacy to friends, got %d", privFriendsRec.Code)
	}

	friendFeedReq := httptest.NewRequest(http.MethodGet, "/v1/feeds?tab=for_you", nil)
	friendFeedReq.Header.Set("Authorization", "Bearer "+pairFriend.AccessToken)
	friendFeedReq.Header.Set("X-Telefam-Device-FP", devFriend)
	friendFeedRec := httptest.NewRecorder()
	mux.ServeHTTP(friendFeedRec, friendFeedReq)
	var friendFeed feeds.FeedPageResponse
	_ = json.NewDecoder(friendFeedRec.Body).Decode(&friendFeed)
	if len(friendFeed.Items) != 1 || friendFeed.Items[0].Privacy != "friends" {
		t.Fatalf("expected mutual friend to view friends-only post, got %+v", friendFeed.Items)
	}

	strangerFeedReq := httptest.NewRequest(http.MethodGet, "/v1/feeds?tab=for_you", nil)
	strangerFeedReq.Header.Set("Authorization", "Bearer "+pairStranger.AccessToken)
	strangerFeedReq.Header.Set("X-Telefam-Device-FP", devStranger)
	strangerFeedRec := httptest.NewRecorder()
	mux.ServeHTTP(strangerFeedRec, strangerFeedReq)
	var strangerFeed feeds.FeedPageResponse
	_ = json.NewDecoder(strangerFeedRec.Body).Decode(&strangerFeed)
	if len(strangerFeed.Items) != 0 {
		t.Fatalf("expected stranger to NOT see friends-only post, got %+v", strangerFeed.Items)
	}

	privLockedReq := httptest.NewRequest(http.MethodPatch, "/v1/posts/"+postID, bytes.NewBufferString(`{"privacy":"private"}`))
	privLockedReq.Header.Set("Authorization", "Bearer "+pairOwner.AccessToken)
	privLockedReq.Header.Set("X-Telefam-Device-FP", devOwner)
	mux.ServeHTTP(httptest.NewRecorder(), privLockedReq)

	ownerSaveReq := httptest.NewRequest(http.MethodPost, "/v1/feeds/interact", bytes.NewBufferString(`{"post_id":"`+postID+`","action":"favourite"}`))
	ownerSaveReq.Header.Set("Authorization", "Bearer "+pairOwner.AccessToken)
	ownerSaveReq.Header.Set("X-Telefam-Device-FP", devOwner)
	mux.ServeHTTP(httptest.NewRecorder(), ownerSaveReq)

	ownerProfReq := httptest.NewRequest(http.MethodGet, "/v1/users/me/profile", nil)
	ownerProfReq.Header.Set("Authorization", "Bearer "+pairOwner.AccessToken)
	ownerProfReq.Header.Set("X-Telefam-Device-FP", devOwner)
	ownerProfRec := httptest.NewRecorder()
	mux.ServeHTTP(ownerProfRec, ownerProfReq)
	var ownerProf UserProfileResponse
	_ = json.NewDecoder(ownerProfRec.Body).Decode(&ownerProf)
	if len(ownerProf.Posts) != 0 || len(ownerProf.LockedPosts) != 1 || len(ownerProf.SavedPosts) != 1 || ownerProf.LockedPosts[0].Privacy != "private" {
		t.Fatalf("expected private post moved to LockedPosts, got posts=%d locked=%d saved=%d", len(ownerProf.Posts), len(ownerProf.LockedPosts), len(ownerProf.SavedPosts))
	}

	friendFeedAfterReq := httptest.NewRequest(http.MethodGet, "/v1/feeds?tab=for_you", nil)
	friendFeedAfterReq.Header.Set("Authorization", "Bearer "+pairFriend.AccessToken)
	friendFeedAfterReq.Header.Set("X-Telefam-Device-FP", devFriend)
	friendFeedAfterRec := httptest.NewRecorder()
	mux.ServeHTTP(friendFeedAfterRec, friendFeedAfterReq)
	var friendFeedAfter feeds.FeedPageResponse
	_ = json.NewDecoder(friendFeedAfterRec.Body).Decode(&friendFeedAfter)
	if len(friendFeedAfter.Items) != 0 {
		t.Fatalf("expected private post excluded from friend's For You feed, got %+v", friendFeedAfter.Items)
	}
}

func TestCheckEmailUniquenessAndSameSecondLogoutReissue(t *testing.T) {
	h, mux, tokens := newTestMuxAndTokens()
	dfpOwner := "device_owner_email_1"
	dfpNew := "device_other_email_2"
	fpOwner, _ := h.fp.ExtractDeviceFingerprint(&http.Request{Header: http.Header{"X-Telefam-Device-FP": []string{dfpOwner}}})
	fpNew, _ := h.fp.ExtractDeviceFingerprint(&http.Request{Header: http.Header{"X-Telefam-Device-FP": []string{dfpNew}}})

	// Before registration -> email does not exist
	chk0 := httptest.NewRequest(http.MethodGet, "/v1/auth/check-email?email=Unique.User@telefam.company", nil)
	chk0.Header.Set("X-Telefam-Device-FP", dfpOwner)
	rec0 := httptest.NewRecorder()
	mux.ServeHTTP(rec0, chk0)
	if rec0.Code != http.StatusOK {
		t.Fatalf("expected 200 on check-email, got %d: %s", rec0.Code, rec0.Body.String())
	}
	if strings.Contains(rec0.Body.String(), `"exists":true`) {
		t.Fatalf("expected unregistered email not to exist, got %s", rec0.Body.String())
	}

	// Register canonical user + trusted device + completed profile
	pair1, _, _ := tokens.IssueInitialPair("user_unique_1", "unique.user@telefam.company", fpOwner)
	h.mu.Lock()
	h.recordTrustedDeviceAndEmailLocked("user_unique_1", "unique.user@telefam.company", fpOwner, httptest.NewRequest(http.MethodPost, "/", nil))
	h.mu.Unlock()
	setupReq := authReq("POST", "/v1/profile/setup", ProfileSetupPayload{
		FullName:    "Unique User",
		Username:    "unique_user",
		PhoneNumber: "+254712345678",
		CountryISO:  "KE",
		Gender:      "Female",
		DateOfBirth: "1998-04-04",
		Location:    "Nairobi",
		Bio:         "Verified profile",
	}, pair1.AccessToken, dfpOwner)
	setupRec := httptest.NewRecorder()
	mux.ServeHTTP(setupRec, setupReq)
	if setupRec.Code != http.StatusOK {
		t.Fatalf("expected setup 200, got %d: %s", setupRec.Code, setupRec.Body.String())
	}

	// Same device -> exists=true, profile_completed=true, requires_device_approval=false
	chkSame := httptest.NewRequest(http.MethodGet, "/v1/auth/check-email?email=unique.user@telefam.company", nil)
	chkSame.Header.Set("X-Telefam-Device-FP", dfpOwner)
	recSame := httptest.NewRecorder()
	mux.ServeHTTP(recSame, chkSame)
	var outSame struct {
		Exists                 bool `json:"exists"`
		ProfileCompleted       bool `json:"profile_completed"`
		RequiresDeviceApproval bool `json:"requires_device_approval"`
	}
	_ = json.NewDecoder(recSame.Body).Decode(&outSame)
	if !outSame.Exists || !outSame.ProfileCompleted || outSame.RequiresDeviceApproval {
		t.Fatalf("unexpected same-device check-email result: %+v", outSame)
	}

	// Different device -> exists=true, profile_completed=true, requires_device_approval=true
	chkOther := httptest.NewRequest(http.MethodGet, "/v1/auth/check-email?email=UNIQUE.USER@telefam.company", nil)
	chkOther.Header.Set("X-Telefam-Device-FP", dfpNew)
	recOther := httptest.NewRecorder()
	mux.ServeHTTP(recOther, chkOther)
	var outOther struct {
		Exists                 bool `json:"exists"`
		ProfileCompleted       bool `json:"profile_completed"`
		RequiresDeviceApproval bool `json:"requires_device_approval"`
	}
	_ = json.NewDecoder(recOther.Body).Decode(&outOther)
	if !outOther.Exists || !outOther.ProfileCompleted || !outOther.RequiresDeviceApproval {
		t.Fatalf("unexpected other-device check-email result: %+v", outOther)
	}

	// Logout revokes old token even if caller re-logs in within the same second
	logoutRec := httptest.NewRecorder()
	mux.ServeHTTP(logoutRec, authReq("POST", "/v1/auth/logout", LogoutPayload{AllDevices: true}, pair1.AccessToken, dfpOwner))
	if logoutRec.Code != http.StatusOK {
		t.Fatalf("logout failed: %d", logoutRec.Code)
	}
	if _, err := tokens.VerifyAccessToken(pair1.AccessToken, fpOwner); err == nil {
		t.Fatalf("expected old token rejected after logout")
	}

	pair2, _, err := tokens.IssueInitialPair("user_unique_1", "unique.user@telefam.company", fpNew)
	if err != nil {
		t.Fatalf("reissue after logout failed: %v", err)
	}
	if _, err := tokens.VerifyAccessToken(pair2.AccessToken, fpNew); err != nil {
		t.Fatalf("expected newly issued same-second token to validate, got: %v", err)
	}
	_ = fpNew
}
