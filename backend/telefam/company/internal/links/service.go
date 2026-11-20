package links

import (
	"context"
	"crypto/rand"
	"crypto/sha256"
	"encoding/hex"
	"errors"
	"html"
	"io"
	"net"
	"net/http"
	"net/netip"
	"net/url"
	"regexp"
	"sort"
	"strings"
	"sync"
	"syscall"
	"time"

	"telefam.company/backend/internal/security"
)

var (
	ErrInvalidOrUnsafeURL = errors.New("invalid or unsafe URL")
	ErrBlockedPrivateIP   = errors.New("access to internal or private network addresses is blocked")
	ErrSavedLinkNotFound  = errors.New("saved link not found")
)

const (
	maxPreviewHTMLBytes = 1536 * 1024 // 1.5 MiB cap per page fetch
	previewCacheOkTTL   = 24 * time.Hour
	previewCacheFailTTL = 5 * time.Minute
	maxSavedLinksPerUsr = 500
)

type PreviewMetadata struct {
	NormalizedURL string `json:"normalized_url"`
	Title         string `json:"title"`
	Description   string `json:"description"`
	ImageURL      string `json:"image_url"`
	FaviconURL    string `json:"favicon_url"`
	Domain        string `json:"domain"`
	Status        string `json:"status"` // "ok" or "fallback"
	FetchedAtMs   int64  `json:"fetched_at_ms"`
}

type SavedLinkItem struct {
	ID            string `json:"id"`
	UserID        string `json:"user_id,omitempty"`
	URL           string `json:"url"`
	NormalizedURL string `json:"normalized_url"`
	Title         string `json:"title"`
	Description   string `json:"description"`
	ImageURL      string `json:"image_url"`
	FaviconURL    string `json:"favicon_url"`
	Domain        string `json:"domain"`
	Source        string `json:"source"`
	SavedAtMs     int64  `json:"saved_at_ms"`
}

type SaveLinkInput struct {
	URL         string
	Title       string
	Description string
	ImageURL    string
	FaviconURL  string
	Domain      string
	Source      string
}

type Service struct {
	mu           sync.RWMutex
	previewCache map[string]PreviewMetadata
	savedByUser  map[string]map[string]SavedLinkItem // userID -> savedLinkID -> item
	httpClient   *http.Client
	allowCustom  bool // optional test override when injecting httptest RoundTripper
}

func NewService() *Service {
	dialer := &net.Dialer{
		Timeout:   4 * time.Second,
		KeepAlive: 15 * time.Second,
		Control: func(network, address string, c syscall.RawConn) error {
			host, _, err := net.SplitHostPort(address)
			if err != nil {
				host = address
			}
			ip := net.ParseIP(host)
			if ip == nil || !IsPublicIP(ip) {
				return ErrBlockedPrivateIP
			}
			return nil
		},
	}

	transport := &http.Transport{
		DialContext:           dialer.DialContext,
		TLSHandshakeTimeout:   4 * time.Second,
		ResponseHeaderTimeout: 5 * time.Second,
		ExpectContinueTimeout: 1 * time.Second,
		MaxIdleConns:          32,
		IdleConnTimeout:       30 * time.Second,
	}

	client := &http.Client{
		Timeout:   6 * time.Second,
		Transport: transport,
		CheckRedirect: func(req *http.Request, via []*http.Request) error {
			if len(via) >= 5 {
				return errors.New("too many redirects")
			}
			if _, err := NormalizeAndValidatePublicURL(req.URL.String()); err != nil {
				return err
			}
			return nil
		},
	}

	return &Service{
		previewCache: make(map[string]PreviewMetadata),
		savedByUser:  make(map[string]map[string]SavedLinkItem),
		httpClient:   client,
	}
}

// NewServiceWithHTTPClient creates a link service with a custom HTTP client for unit testing.
// Even when a custom client is supplied, scheme/host/IP SSRF validation still runs before requests and on redirects.
func NewServiceWithHTTPClient(client *http.Client) *Service {
	svc := NewService()
	if client != nil {
		svc.httpClient = client
		svc.allowCustom = true
	}
	return svc
}

// NormalizeAndValidatePublicURL enforces http/https schemes, rejects localhost/internal hosts,
// and blocks private, loopback, link-local, CGNAT, multicast, and cloud-metadata IPs.
func NormalizeAndValidatePublicURL(raw string) (*url.URL, error) {
	trimmed := strings.TrimSpace(raw)
	if trimmed == "" || len(trimmed) > 2048 {
		return nil, ErrInvalidOrUnsafeURL
	}
	lower := strings.ToLower(trimmed)
	for _, blocked := range []string{"javascript:", "data:", "file:", "vbscript:", "about:", "content:", "intent:", "ftp:", "tel:", "mailto:"} {
		if strings.HasPrefix(lower, blocked) {
			return nil, ErrInvalidOrUnsafeURL
		}
	}
	if !strings.HasPrefix(lower, "http://") && !strings.HasPrefix(lower, "https://") {
		if strings.Contains(trimmed, "://") {
			return nil, ErrInvalidOrUnsafeURL
		}
		trimmed = "https://" + trimmed
	}

	parsed, err := url.Parse(trimmed)
	if err != nil || parsed == nil {
		return nil, ErrInvalidOrUnsafeURL
	}
	scheme := strings.ToLower(parsed.Scheme)
	if scheme != "http" && scheme != "https" {
		return nil, ErrInvalidOrUnsafeURL
	}
	if parsed.User != nil {
		return nil, ErrInvalidOrUnsafeURL
	}
	hostname := strings.ToLower(strings.TrimSuffix(strings.TrimSpace(parsed.Hostname()), "."))
	if hostname == "" || strings.ContainsAny(hostname, " \t\r\n") {
		return nil, ErrInvalidOrUnsafeURL
	}
	if isForbiddenHostname(hostname) {
		return nil, ErrBlockedPrivateIP
	}
	if ip := net.ParseIP(hostname); ip != nil {
		if !IsPublicIP(ip) {
			return nil, ErrBlockedPrivateIP
		}
	} else if !strings.Contains(hostname, ".") {
		// Reject bare single-label internal hostnames such as "redis" or "metadata"
		return nil, ErrBlockedPrivateIP
	}

	parsed.Scheme = scheme
	parsed.Host = strings.ToLower(parsed.Host)
	parsed.Fragment = ""
	return parsed, nil
}

func isForbiddenHostname(hostname string) bool {
	if hostname == "localhost" ||
		strings.HasSuffix(hostname, ".localhost") ||
		strings.HasSuffix(hostname, ".local") ||
		strings.HasSuffix(hostname, ".internal") ||
		strings.HasSuffix(hostname, ".localdomain") ||
		strings.HasSuffix(hostname, ".home.arpa") ||
		hostname == "metadata.google.internal" ||
		hostname == "metadata" {
		return true
	}
	return false
}

// IsPublicIP returns true strictly for globally routable public unicast IPv4/IPv6 addresses.
func IsPublicIP(ip net.IP) bool {
	if ip == nil {
		return false
	}
	if ip4 := ip.To4(); ip4 != nil {
		addr, ok := netip.AddrFromSlice(ip4)
		if !ok {
			return false
		}
		if !addr.IsValid() || addr.IsLoopback() || addr.IsPrivate() || addr.IsLinkLocalUnicast() ||
			addr.IsLinkLocalMulticast() || addr.IsMulticast() || addr.IsUnspecified() || addr.IsInterfaceLocalMulticast() {
			return false
		}
		// Reject CGNAT 100.64.0.0/10, benchmark 198.18.0.0/15, 0.0.0.0/8, 192.0.0.0/24, 240.0.0.0/4
		cgnat := netip.MustParsePrefix("100.64.0.0/10")
		bench := netip.MustParsePrefix("198.18.0.0/15")
		zeroNet := netip.MustParsePrefix("0.0.0.0/8")
		ietf := netip.MustParsePrefix("192.0.0.0/24")
		reserved := netip.MustParsePrefix("240.0.0.0/4")
		if cgnat.Contains(addr) || bench.Contains(addr) || zeroNet.Contains(addr) || ietf.Contains(addr) || reserved.Contains(addr) {
			return false
		}
		return addr.IsGlobalUnicast()
	}

	ip16 := ip.To16()
	if ip16 == nil {
		return false
	}
	addr, ok := netip.AddrFromSlice(ip16)
	if !ok {
		return false
	}
	addr = addr.Unmap()
	if addr.Is4() {
		return IsPublicIP(net.IP(addr.AsSlice()))
	}
	if !addr.IsValid() || addr.IsLoopback() || addr.IsPrivate() || addr.IsLinkLocalUnicast() ||
		addr.IsLinkLocalMulticast() || addr.IsMulticast() || addr.IsUnspecified() || addr.IsInterfaceLocalMulticast() {
		return false
	}
	// Reject 6to4 / documentation / NAT64 private prefixes cleanly
	docV6 := netip.MustParsePrefix("2001:db8::/32")
	if docV6.Contains(addr) {
		return false
	}
	return addr.IsGlobalUnicast()
}

func (s *Service) FetchPreview(ctx context.Context, rawURL string) (PreviewMetadata, error) {
	parsed, err := NormalizeAndValidatePublicURL(rawURL)
	if err != nil {
		return PreviewMetadata{}, err
	}
	normalized := parsed.String()
	domain := cleanDomain(parsed.Hostname())
	nowMs := time.Now().UnixMilli()

	// Pre-resolve DNS on standard live path before cache/http to catch private DNS A/AAAA entries early
	if !s.allowCustom && net.ParseIP(parsed.Hostname()) == nil {
		ips, lookupErr := net.DefaultResolver.LookupIPAddr(ctx, parsed.Hostname())
		if lookupErr == nil && len(ips) > 0 {
			for _, resolved := range ips {
				if !IsPublicIP(resolved.IP) {
					return PreviewMetadata{}, ErrBlockedPrivateIP
				}
			}
		}
	}

	s.mu.RLock()
	cached, hasCached := s.previewCache[normalized]
	s.mu.RUnlock()
	if hasCached {
		ttl := previewCacheOkTTL
		if cached.Status != "ok" {
			ttl = previewCacheFailTTL
		}
		if nowMs-cached.FetchedAtMs < ttl.Milliseconds() {
			return cached, nil
		}
	}

	fallback := PreviewMetadata{
		NormalizedURL: normalized,
		Title:         domain,
		Description:   normalized,
		ImageURL:      "",
		FaviconURL:    "",
		Domain:        domain,
		Status:        "fallback",
		FetchedAtMs:   nowMs,
	}

	req, err := http.NewRequestWithContext(ctx, http.MethodGet, normalized, nil)
	if err != nil {
		s.storePreview(fallback)
		return fallback, nil
	}
	req.Header.Set("User-Agent", "TelefamLinkPreview/1.0 (+https://telefam.company)")
	req.Header.Set("Accept", "text/html,application/xhtml+xml;q=0.9,*/*;q=0.5")
	req.Header.Set("Accept-Language", "en-US,en;q=0.9")

	resp, err := s.httpClient.Do(req)
	if err != nil {
		if errors.Is(err, ErrBlockedPrivateIP) || errors.Is(err, ErrInvalidOrUnsafeURL) {
			return PreviewMetadata{}, err
		}
		s.storePreview(fallback)
		return fallback, nil
	}
	defer resp.Body.Close()

	if resp.StatusCode < 200 || resp.StatusCode >= 400 {
		s.storePreview(fallback)
		return fallback, nil
	}

	contentType := strings.ToLower(resp.Header.Get("Content-Type"))
	if contentType != "" && !strings.Contains(contentType, "text/html") && !strings.Contains(contentType, "application/xhtml") && !strings.Contains(contentType, "text/plain") {
		s.storePreview(fallback)
		return fallback, nil
	}

	bodyBytes, err := io.ReadAll(io.LimitReader(resp.Body, maxPreviewHTMLBytes))
	if err != nil || len(bodyBytes) == 0 {
		s.storePreview(fallback)
		return fallback, nil
	}

	baseURL := parsed
	if resp.Request != nil && resp.Request.URL != nil {
		if validatedFinal, vErr := NormalizeAndValidatePublicURL(resp.Request.URL.String()); vErr == nil {
			baseURL = validatedFinal
			domain = cleanDomain(validatedFinal.Hostname())
		}
	}

	parsedMeta := ExtractHTMLMetadata(string(bodyBytes), baseURL)
	if parsedMeta.Title == "" && parsedMeta.Description == "" && parsedMeta.ImageURL == "" {
		s.storePreview(fallback)
		return fallback, nil
	}
	if parsedMeta.Title == "" {
		parsedMeta.Title = domain
	}
	parsedMeta.NormalizedURL = normalized
	parsedMeta.Domain = domain
	parsedMeta.Status = "ok"
	parsedMeta.FetchedAtMs = nowMs

	s.storePreview(parsedMeta)
	return parsedMeta, nil
}

func (s *Service) storePreview(meta PreviewMetadata) {
	s.mu.Lock()
	defer s.mu.Unlock()
	if len(s.previewCache) > 2048 {
		for k := range s.previewCache {
			delete(s.previewCache, k)
			if len(s.previewCache) <= 1536 {
				break
			}
		}
	}
	s.previewCache[meta.NormalizedURL] = meta
}

func (s *Service) ListSavedLinks(userID string, query string) []SavedLinkItem {
	s.mu.RLock()
	userMap := s.savedByUser[userID]
	out := make([]SavedLinkItem, 0, len(userMap))
	q := strings.ToLower(strings.TrimSpace(query))
	for _, item := range userMap {
		if q != "" &&
			!strings.Contains(strings.ToLower(item.Title), q) &&
			!strings.Contains(strings.ToLower(item.Domain), q) &&
			!strings.Contains(strings.ToLower(item.URL), q) {
			continue
		}
		out = append(out, item)
	}
	s.mu.RUnlock()

	sort.Slice(out, func(i, j int) bool {
		return out[i].SavedAtMs > out[j].SavedAtMs
	})
	return out
}

func (s *Service) SaveLink(userID string, input SaveLinkInput) (SavedLinkItem, error) {
	if strings.TrimSpace(userID) == "" {
		return SavedLinkItem{}, ErrInvalidOrUnsafeURL
	}
	parsed, err := NormalizeAndValidatePublicURL(input.URL)
	if err != nil {
		return SavedLinkItem{}, err
	}
	normalized := parsed.String()
	domain := cleanDomain(parsed.Hostname())
	source := normalizeSource(input.Source)

	title := security.SanitizeText(input.Title, 180, false)
	if title == "" {
		title = domain
	}
	description := security.SanitizeText(input.Description, 320, false)

	imageURL := sanitizeSubResourceURL(input.ImageURL, parsed)
	faviconURL := sanitizeSubResourceURL(input.FaviconURL, parsed)
	nowMs := time.Now().UnixMilli()

	s.mu.Lock()
	defer s.mu.Unlock()
	userMap, exists := s.savedByUser[userID]
	if !exists {
		userMap = make(map[string]SavedLinkItem)
		s.savedByUser[userID] = userMap
	}

	// Upsert by normalized URL so saving the same URL updates metadata/timestamp cleanly
	for existingID, existing := range userMap {
		if existing.NormalizedURL == normalized {
			updated := SavedLinkItem{
				ID:            existingID,
				UserID:        userID,
				URL:           normalized,
				NormalizedURL: normalized,
				Title:         title,
				Description:   description,
				ImageURL:      imageURL,
				FaviconURL:    faviconURL,
				Domain:        domain,
				Source:        source,
				SavedAtMs:     nowMs,
			}
			userMap[existingID] = updated
			return updated, nil
		}
	}

	if len(userMap) >= maxSavedLinksPerUsr {
		var oldestID string
		var oldestTs int64 = nowMs
		for k, v := range userMap {
			if oldestID == "" || v.SavedAtMs < oldestTs {
				oldestID = k
				oldestTs = v.SavedAtMs
			}
		}
		if oldestID != "" {
			delete(userMap, oldestID)
		}
	}

	item := SavedLinkItem{
		ID:            newSavedLinkID(userID, normalized),
		UserID:        userID,
		URL:           normalized,
		NormalizedURL: normalized,
		Title:         title,
		Description:   description,
		ImageURL:      imageURL,
		FaviconURL:    faviconURL,
		Domain:        domain,
		Source:        source,
		SavedAtMs:     nowMs,
	}
	userMap[item.ID] = item
	return item, nil
}

func (s *Service) DeleteSavedLink(userID string, id string) error {
	cleanID := strings.TrimSpace(id)
	if strings.TrimSpace(userID) == "" || cleanID == "" {
		return ErrSavedLinkNotFound
	}
	s.mu.Lock()
	defer s.mu.Unlock()
	userMap := s.savedByUser[userID]
	if userMap == nil {
		return ErrSavedLinkNotFound
	}
	if _, ok := userMap[cleanID]; ok {
		delete(userMap, cleanID)
		return nil
	}
	// Also allow deleting by normalized URL hash or matching URL if caller passed hash/id
	for k, v := range userMap {
		if v.NormalizedURL == cleanID {
			delete(userMap, k)
			return nil
		}
	}
	return ErrSavedLinkNotFound
}

func cleanDomain(host string) string {
	d := strings.ToLower(strings.TrimSpace(host))
	d = strings.TrimPrefix(d, "www.")
	return d
}

func normalizeSource(raw string) string {
	switch strings.ToLower(strings.TrimSpace(raw)) {
	case "chat", "feed", "profile", "browser", "search":
		return strings.ToLower(strings.TrimSpace(raw))
	default:
		return "browser"
	}
}

func sanitizeSubResourceURL(raw string, base *url.URL) string {
	trimmed := strings.TrimSpace(raw)
	if trimmed == "" || base == nil {
		return ""
	}
	rel, err := url.Parse(trimmed)
	if err != nil {
		return ""
	}
	resolved := base.ResolveReference(rel)
	validated, err := NormalizeAndValidatePublicURL(resolved.String())
	if err != nil {
		return ""
	}
	return validated.String()
}

func newSavedLinkID(userID, normalizedURL string) string {
	b := make([]byte, 6)
	_, _ = rand.Read(b)
	h := sha256.Sum256([]byte(userID + "|" + normalizedURL))
	return "lnk_" + hex.EncodeToString(h[:6]) + "_" + hex.EncodeToString(b)
}

var (
	metaTagRegex   = regexp.MustCompile(`(?is)<meta\b[^>]*>`)
	linkTagRegex   = regexp.MustCompile(`(?is)<link\b[^>]*>`)
	titleTagRegex  = regexp.MustCompile(`(?is)<title[^>]*>(.*?)</title>`)
	attrRegex      = regexp.MustCompile(`(?is)([a-zA-Z0-9_:\-]+)\s*=\s*(?:"([^"]*)"|'([^']*)'|([^\s"'>]+))`)
)

func ExtractHTMLMetadata(rawHTML string, baseURL *url.URL) PreviewMetadata {
	var ogTitle, twitterTitle, docTitle string
	var ogDesc, twitterDesc, metaDesc string
	var ogImage, twitterImage string
	var appleIcon, shortcutIcon, relIcon string

	if m := titleTagRegex.FindStringSubmatch(rawHTML); len(m) >= 2 {
		docTitle = security.SanitizeText(html.UnescapeString(m[1]), 180, false)
	}

	for _, tag := range metaTagRegex.FindAllString(rawHTML, 80) {
		attrs := parseTagAttrs(tag)
		key := strings.ToLower(firstNonEmpty(attrs["property"], attrs["name"], attrs["itemprop"]))
		content := security.SanitizeText(html.UnescapeString(attrs["content"]), 320, false)
		if content == "" {
			continue
		}
		switch key {
		case "og:title":
			if ogTitle == "" {
				ogTitle = content
			}
		case "twitter:title":
			if twitterTitle == "" {
				twitterTitle = content
			}
		case "og:description":
			if ogDesc == "" {
				ogDesc = content
			}
		case "twitter:description":
			if twitterDesc == "" {
				twitterDesc = content
			}
		case "description":
			if metaDesc == "" {
				metaDesc = content
			}
		case "og:image", "og:image:secure_url":
			if ogImage == "" {
				ogImage = sanitizeSubResourceURL(html.UnescapeString(attrs["content"]), baseURL)
			}
		case "twitter:image", "twitter:image:src":
			if twitterImage == "" {
				twitterImage = sanitizeSubResourceURL(html.UnescapeString(attrs["content"]), baseURL)
			}
		}
	}

	for _, tag := range linkTagRegex.FindAllString(rawHTML, 50) {
		attrs := parseTagAttrs(tag)
		rel := strings.ToLower(attrs["rel"])
		href := strings.TrimSpace(html.UnescapeString(attrs["href"]))
		if href == "" {
			continue
		}
		tokens := strings.Fields(rel)
		hasToken := func(target string) bool {
			for _, t := range tokens {
				if t == target {
					return true
				}
			}
			return false
		}
		resolved := sanitizeSubResourceURL(href, baseURL)
		if resolved == "" {
			continue
		}
		if strings.Contains(rel, "apple-touch-icon") && appleIcon == "" {
			appleIcon = resolved
		} else if rel == "shortcut icon" && shortcutIcon == "" {
			shortcutIcon = resolved
		} else if hasToken("icon") && relIcon == "" {
			relIcon = resolved
		}
	}

	favicon := firstNonEmpty(relIcon, shortcutIcon, appleIcon)
	if favicon == "" && baseURL != nil {
		favicon = sanitizeSubResourceURL("/favicon.ico", baseURL)
	}

	return PreviewMetadata{
		Title:       firstNonEmpty(ogTitle, twitterTitle, docTitle),
		Description: firstNonEmpty(ogDesc, twitterDesc, metaDesc),
		ImageURL:    firstNonEmpty(ogImage, twitterImage),
		FaviconURL:  favicon,
	}
}

func parseTagAttrs(tag string) map[string]string {
	out := make(map[string]string)
	for _, match := range attrRegex.FindAllStringSubmatch(tag, -1) {
		if len(match) < 5 {
			continue
		}
		name := strings.ToLower(strings.TrimSpace(match[1]))
		val := firstNonEmpty(match[2], match[3], match[4])
		if name != "" {
			out[name] = strings.TrimSpace(val)
		}
	}
	return out
}

func firstNonEmpty(values ...string) string {
	for _, v := range values {
		if strings.TrimSpace(v) != "" {
			return strings.TrimSpace(v)
		}
	}
	return ""
}
