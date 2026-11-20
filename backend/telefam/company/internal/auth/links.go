package auth

import (
	"encoding/json"
	"errors"
	"net/http"
	"strings"

	sanitizer "telefam.company/backend/internal/errors"
	"telefam.company/backend/internal/links"
)

type SaveLinkPayload struct {
	URL         string `json:"url"`
	Title       string `json:"title,omitempty"`
	Description string `json:"description,omitempty"`
	ImageURL    string `json:"image_url,omitempty"`
	FaviconURL  string `json:"favicon_url,omitempty"`
	Domain      string `json:"domain,omitempty"`
	Source      string `json:"source,omitempty"`
}

func (h *Handler) ensureLinkService() *links.Service {
	h.mu.Lock()
	defer h.mu.Unlock()
	if h.linkSvc == nil {
		h.linkSvc = links.NewService()
	}
	return h.linkSvc
}

func (h *Handler) SetLinkService(svc *links.Service) {
	if svc != nil {
		h.mu.Lock()
		h.linkSvc = svc
		h.mu.Unlock()
	}
}

func (h *Handler) handleGetLinkPreview(w http.ResponseWriter, r *http.Request) {
	claims, deviceFP, ipFP, ok := h.requireAuth(w, r)
	if !ok {
		return
	}
	if h.limiter != nil && !h.limiter.AllowLinkPreview(claims.UserID, deviceFP, ipFP) {
		h.audit.Record(claims.UserID, "link_preview_rate_limited", deviceFP, ipFP, nil)
		sanitizer.WriteSanitizedError(w, http.StatusTooManyRequests, "link_preview_rate_limited", nil, "Too many link preview requests.")
		return
	}

	rawURL := strings.TrimSpace(r.URL.Query().Get("url"))
	if rawURL == "" {
		sanitizer.WriteSanitizedError(w, http.StatusBadRequest, "missing_url", nil, "URL is required.")
		return
	}

	preview, err := h.ensureLinkService().FetchPreview(r.Context(), rawURL)
	if err != nil {
		if errors.Is(err, links.ErrBlockedPrivateIP) || errors.Is(err, links.ErrInvalidOrUnsafeURL) {
			h.audit.Record(claims.UserID, "link_preview_blocked_ssrf", deviceFP, ipFP, map[string]string{
				"reason": err.Error(),
			})
			sanitizer.WriteSanitizedError(w, http.StatusBadRequest, "unsafe_url", err, "URL is invalid or not permitted.")
			return
		}
		sanitizer.WriteSanitizedError(w, http.StatusBadGateway, "link_preview_failed", err, sanitizer.GenericErrorMessage)
		return
	}

	writeJSON(w, http.StatusOK, map[string]any{
		"success": true,
		"preview": preview,
	})
}

func (h *Handler) handleListSavedLinks(w http.ResponseWriter, r *http.Request) {
	claims, _, _, ok := h.requireAuth(w, r)
	if !ok {
		return
	}
	query := strings.TrimSpace(r.URL.Query().Get("q"))
	items := h.ensureLinkService().ListSavedLinks(claims.UserID, query)
	writeJSON(w, http.StatusOK, map[string]any{
		"success": true,
		"items":   items,
	})
}

func (h *Handler) handleSaveLink(w http.ResponseWriter, r *http.Request) {
	claims, deviceFP, ipFP, ok := h.requireAuth(w, r)
	if !ok {
		return
	}
	r.Body = http.MaxBytesReader(w, r.Body, 64*1024)
	var req SaveLinkPayload
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		sanitizer.WriteSanitizedError(w, http.StatusBadRequest, "save_link_bad_json", err, "Invalid request body.")
		return
	}

	item, err := h.ensureLinkService().SaveLink(claims.UserID, links.SaveLinkInput{
		URL:         req.URL,
		Title:       req.Title,
		Description: req.Description,
		ImageURL:    req.ImageURL,
		FaviconURL:  req.FaviconURL,
		Domain:      req.Domain,
		Source:      req.Source,
	})
	if err != nil {
		sanitizer.WriteSanitizedError(w, http.StatusBadRequest, "save_link_unsafe_url", err, "Invalid or unsafe URL.")
		return
	}

	h.audit.Record(claims.UserID, "saved_link_created", deviceFP, ipFP, map[string]string{
		"domain": item.Domain,
		"source": item.Source,
	})
	writeJSON(w, http.StatusOK, map[string]any{
		"success": true,
		"item":    item,
	})
}

func (h *Handler) handleDeleteSavedLink(w http.ResponseWriter, r *http.Request) {
	claims, deviceFP, ipFP, ok := h.requireAuth(w, r)
	if !ok {
		return
	}
	id := strings.TrimSpace(r.PathValue("id"))
	if id == "" {
		sanitizer.WriteSanitizedError(w, http.StatusBadRequest, "missing_link_id", nil, "Saved link ID is required.")
		return
	}
	if err := h.ensureLinkService().DeleteSavedLink(claims.UserID, id); err != nil {
		if errors.Is(err, links.ErrSavedLinkNotFound) {
			sanitizer.WriteSanitizedError(w, http.StatusNotFound, "saved_link_not_found", err, "Saved link not found.")
			return
		}
		sanitizer.WriteSanitizedError(w, http.StatusInternalServerError, "delete_saved_link_failed", err, sanitizer.GenericErrorMessage)
		return
	}
	h.audit.Record(claims.UserID, "saved_link_deleted", deviceFP, ipFP, map[string]string{"id": id})
	writeJSON(w, http.StatusOK, map[string]any{
		"success": true,
	})
}
