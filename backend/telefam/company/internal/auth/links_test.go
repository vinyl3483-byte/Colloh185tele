package auth

import (
	"encoding/json"
	"errors"
	"io"
	"net/http"
	"net/http/httptest"
	"net/url"
	"strings"
	"sync/atomic"
	"testing"

	"telefam.company/backend/internal/links"
)

type roundTripFunc func(*http.Request) (*http.Response, error)

func (f roundTripFunc) RoundTrip(r *http.Request) (*http.Response, error) {
	return f(r)
}

func TestLinksPreviewSSRFProtectionAndMetadataParsing(t *testing.T) {
	h, mux, tokens := newTestMuxAndTokens()
	dfp := "dfp-link-test"
	pair, _, err := tokens.IssueInitialPair("u_links_a", "linksa@telefam.company", dfp)
	if err != nil {
		t.Fatalf("IssueInitialPair failed: %v", err)
	}

	var fetchCalls atomic.Int32
	mockTransport := roundTripFunc(func(req *http.Request) (*http.Response, error) {
		fetchCalls.Add(1)
		if req.URL.String() == "https://redirect.example.com/hop" {
			return &http.Response{
				StatusCode: http.StatusFound,
				Header:     http.Header{"Location": []string{"http://127.0.0.1:8080/internal"}},
				Body:       io.NopCloser(strings.NewReader("")),
				Request:    req,
			}, nil
		}
		if req.URL.Host == "broken.example.com" {
			return &http.Response{
				StatusCode: http.StatusBadGateway,
				Header:     http.Header{"Content-Type": []string{"text/html"}},
				Body:       io.NopCloser(strings.NewReader("")),
				Request:    req,
			}, nil
		}
		htmlDoc := `<!doctype html><html><head>
			<title>Fallback Doc Title</title>
			<meta property="og:title" content="Telefam Engineering Blog">
			<meta property="og:description" content="How smart text &amp; E2EE chat work reliably.">
			<meta property="og:image" content="/assets/hero-banner.png">
			<link rel="icon" href="/favicon-32x32.png">
		</head><body><p>Hello world</p></body></html>`
		return &http.Response{
			StatusCode: http.StatusOK,
			Header:     http.Header{"Content-Type": []string{"text/html; charset=utf-8"}},
			Body:       io.NopCloser(strings.NewReader(htmlDoc)),
			Request:    req,
		}, nil
	})

	customClient := &http.Client{
		Transport: mockTransport,
		CheckRedirect: func(req *http.Request, via []*http.Request) error {
			if len(via) >= 5 {
				return errors.New("too many redirects")
			}
			if _, vErr := links.NormalizeAndValidatePublicURL(req.URL.String()); vErr != nil {
				return vErr
			}
			return nil
		},
	}
	h.SetLinkService(links.NewServiceWithHTTPClient(customClient))

	// 1. Blocked unsafe schemes and private/SSRF targets
	blockedTargets := []string{
		"http://127.0.0.1:8080/admin",
		"http://169.254.169.254/latest/meta-data/",
		"http://10.0.0.12/status",
		"http://192.168.1.1/",
		"http://localhost:3000",
		"http://metadata.google.internal/computeMetadata/v1/",
		"javascript:alert(1)",
		"file:///etc/passwd",
		"https://redirect.example.com/hop",
	}
	for _, target := range blockedTargets {
		w := httptest.NewRecorder()
		req := authReq("GET", "/v1/links/preview?url="+url.QueryEscape(target), nil, pair.AccessToken, dfp)
		mux.ServeHTTP(w, req)
		if w.Code != http.StatusBadRequest {
			t.Fatalf("expected 400 BadRequest for unsafe target %q, got %d (%s)", target, w.Code, w.Body.String())
		}
	}

	// 2. Valid public URL extracts OpenGraph metadata, resolves relative image/favicon, and caches result
	wOk := httptest.NewRecorder()
	mux.ServeHTTP(wOk, authReq("GET", "/v1/links/preview?url="+url.QueryEscape("https://telefam.company/blog/release"), nil, pair.AccessToken, dfp))
	if wOk.Code != http.StatusOK {
		t.Fatalf("expected 200 OK for public URL, got %d (%s)", wOk.Code, wOk.Body.String())
	}
	var previewResp struct {
		Success bool                  `json:"success"`
		Preview links.PreviewMetadata `json:"preview"`
	}
	if err := json.Unmarshal(wOk.Body.Bytes(), &previewResp); err != nil {
		t.Fatalf("unmarshal preview response: %v", err)
	}
	if !previewResp.Success || previewResp.Preview.Status != "ok" {
		t.Fatalf("unexpected preview status: %+v", previewResp.Preview)
	}
	if previewResp.Preview.Title != "Telefam Engineering Blog" {
		t.Fatalf("unexpected title: %q", previewResp.Preview.Title)
	}
	if previewResp.Preview.Description != "How smart text & E2EE chat work reliably." {
		t.Fatalf("unexpected description: %q", previewResp.Preview.Description)
	}
	if previewResp.Preview.ImageURL != "https://telefam.company/assets/hero-banner.png" {
		t.Fatalf("unexpected image URL: %q", previewResp.Preview.ImageURL)
	}
	if previewResp.Preview.FaviconURL != "https://telefam.company/favicon-32x32.png" {
		t.Fatalf("unexpected favicon URL: %q", previewResp.Preview.FaviconURL)
	}
	callsBeforeSecond := fetchCalls.Load()

	// Second request hits cache without external fetch
	wCached := httptest.NewRecorder()
	mux.ServeHTTP(wCached, authReq("GET", "/v1/links/preview?url="+url.QueryEscape("https://telefam.company/blog/release"), nil, pair.AccessToken, dfp))
	if wCached.Code != http.StatusOK {
		t.Fatalf("cached preview failed: %d", wCached.Code)
	}
	if fetchCalls.Load() != callsBeforeSecond {
		t.Fatalf("expected cache hit on second fetch, calls went from %d to %d", callsBeforeSecond, fetchCalls.Load())
	}

	// 3. Upstream HTTP error returns graceful fallback metadata instead of crashing
	wFallback := httptest.NewRecorder()
	mux.ServeHTTP(wFallback, authReq("GET", "/v1/links/preview?url="+url.QueryEscape("https://broken.example.com/page"), nil, pair.AccessToken, dfp))
	if wFallback.Code != http.StatusOK {
		t.Fatalf("expected 200 graceful fallback, got %d", wFallback.Code)
	}
	var fbResp struct {
		Preview links.PreviewMetadata `json:"preview"`
	}
	_ = json.Unmarshal(wFallback.Body.Bytes(), &fbResp)
	if fbResp.Preview.Status != "fallback" || fbResp.Preview.Domain != "broken.example.com" {
		t.Fatalf("unexpected fallback payload: %+v", fbResp.Preview)
	}
}

func TestSavedLinksCRUDAndPerUserIsolation(t *testing.T) {
	_, mux, tokens := newTestMuxAndTokens()
	dfpA := "dfp-user-a"
	dfpB := "dfp-user-b"
	pairA, _, err := tokens.IssueInitialPair("u_saved_a", "a@telefam.company", dfpA)
	if err != nil {
		t.Fatalf("IssueInitialPair A: %v", err)
	}
	pairB, _, err := tokens.IssueInitialPair("u_saved_b", "b@telefam.company", dfpB)
	if err != nil {
		t.Fatalf("IssueInitialPair B: %v", err)
	}

	// Reject unsafe saved link URLs
	wBad := httptest.NewRecorder()
	mux.ServeHTTP(wBad, authReq("POST", "/v1/links/saved", SaveLinkPayload{
		URL:    "javascript:alert(document.cookie)",
		Title:  "Evil",
		Source: "chat",
	}, pairA.AccessToken, dfpA))
	if wBad.Code != http.StatusBadRequest {
		t.Fatalf("expected 400 on unsafe save link URL, got %d", wBad.Code)
	}

	// User A saves a valid link
	wSave := httptest.NewRecorder()
	mux.ServeHTTP(wSave, authReq("POST", "/v1/links/saved", SaveLinkPayload{
		URL:         "youtube.com/watch?v=dQw4w9WgXcQ",
		Title:       "YouTube Video",
		Description: "Saved from chat",
		Source:      "chat",
	}, pairA.AccessToken, dfpA))
	if wSave.Code != http.StatusOK {
		t.Fatalf("save link failed: %d %s", wSave.Code, wSave.Body.String())
	}
	var saveResp struct {
		Success bool                `json:"success"`
		Item    links.SavedLinkItem `json:"item"`
	}
	_ = json.Unmarshal(wSave.Body.Bytes(), &saveResp)
	if saveResp.Item.ID == "" || saveResp.Item.Domain != "youtube.com" || saveResp.Item.Source != "chat" {
		t.Fatalf("unexpected saved item: %+v", saveResp.Item)
	}

	// User B cannot see User A's saved links or delete them
	wListB := httptest.NewRecorder()
	mux.ServeHTTP(wListB, authReq("GET", "/v1/links/saved", nil, pairB.AccessToken, dfpB))
	if wListB.Code != http.StatusOK {
		t.Fatalf("list user B failed: %d", wListB.Code)
	}
	var listB struct {
		Items []links.SavedLinkItem `json:"items"`
	}
	_ = json.Unmarshal(wListB.Body.Bytes(), &listB)
	if len(listB.Items) != 0 {
		t.Fatalf("expected 0 saved links for user B, got %d", len(listB.Items))
	}

	wDelB := httptest.NewRecorder()
	mux.ServeHTTP(wDelB, authReq("DELETE", "/v1/links/saved/"+saveResp.Item.ID, nil, pairB.AccessToken, dfpB))
	if wDelB.Code != http.StatusNotFound {
		t.Fatalf("expected 404 when user B deletes user A link, got %d", wDelB.Code)
	}

	// User A lists with query filter & deletes own link
	wListA := httptest.NewRecorder()
	mux.ServeHTTP(wListA, authReq("GET", "/v1/links/saved?q=youtube", nil, pairA.AccessToken, dfpA))
	var listA struct {
		Items []links.SavedLinkItem `json:"items"`
	}
	_ = json.Unmarshal(wListA.Body.Bytes(), &listA)
	if len(listA.Items) != 1 || listA.Items[0].ID != saveResp.Item.ID {
		t.Fatalf("expected 1 filtered item for user A, got %+v", listA.Items)
	}

	wDelA := httptest.NewRecorder()
	mux.ServeHTTP(wDelA, authReq("DELETE", "/v1/links/saved/"+saveResp.Item.ID, nil, pairA.AccessToken, dfpA))
	if wDelA.Code != http.StatusOK {
		t.Fatalf("expected 200 when user A deletes own link, got %d", wDelA.Code)
	}
}
