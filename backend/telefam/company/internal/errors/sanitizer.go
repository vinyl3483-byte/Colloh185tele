package sanitizer

import (
	"encoding/json"
	"log"
	"net/http"
)

const (
	GenericErrorMessage = "An error has occurred. Please try again."
	TryLaterMessage     = "An error has occurred. Please try again later."
)

type PublicErrorResponse struct {
	Success bool   `json:"success"`
	Message string `json:"message"`
}

// WriteSanitizedError logs the real internal error server-side only and returns
// a strictly sanitized, non-leaky public response to the client.
func WriteSanitizedError(w http.ResponseWriter, statusCode int, internalContext string, err error, publicMessage string) {
	if err != nil {
		log.Printf("[telefam-internal] context=%s err=%v", internalContext, err)
	} else {
		log.Printf("[telefam-internal] context=%s status=%d", internalContext, statusCode)
	}

	if publicMessage == "" {
		publicMessage = GenericErrorMessage
	}

	w.Header().Set("Content-Type", "application/json; charset=utf-8")
	w.Header().Set("Cache-Control", "no-store")
	w.WriteHeader(statusCode)
	_ = json.NewEncoder(w).Encode(PublicErrorResponse{
		Success: false,
		Message: publicMessage,
	})
}
