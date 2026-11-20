package main

import (
	"compress/gzip"
	"context"
	"crypto/rand"
	"io"
	"log"
	"net/http"
	"os"
	"os/signal"
	"strings"
	"syscall"
	"time"

	"telefam.company/backend/internal/audit"
	"telefam.company/backend/internal/auth"
	"telefam.company/backend/internal/config"
	"telefam.company/backend/internal/contacts"
	"telefam.company/backend/internal/fingerprint"
	"telefam.company/backend/internal/mailer"
	"telefam.company/backend/internal/ratelimit"
)

func main() {
	for _, candidate := range []string{".env", "../../.env", "backend/telefam/company/.env"} {
		config.LoadDotEnv(candidate)
	}
	secret := loadHMACSecret()
	fpService := fingerprint.NewService(secret)
	rateLimiter := ratelimit.NewLimiter()
	otpService := auth.NewOtpService(secret, os.Getenv("TELEFAM_PUBLIC_BASE_URL"))
	tokenService := auth.NewTokenService(secret)
	auditLogger := audit.NewLogger()
	resendMailer := mailer.NewResendClientFromEnv()
	contactsService := contacts.NewService()
	log.Printf("Telefam Resend mailer configured=%t", resendMailer.IsConfigured())
	handler := auth.NewHandler(fpService, rateLimiter, otpService, tokenService, auditLogger, resendMailer, contactsService)

	mux := http.NewServeMux()
	handler.RegisterRoutes(mux)

	port := os.Getenv("TELEFAM_PORT")
	if port == "" {
		port = "8080"
	}

	srv := &http.Server{
		Addr:              ":" + port,
		Handler:           gzipMiddleware(securityHeadersMiddleware(mux)),
		ReadHeaderTimeout: 10 * time.Second,
		ReadTimeout:       300 * time.Second,
		WriteTimeout:      300 * time.Second,
		IdleTimeout:       120 * time.Second,
	}

	go func() {
		log.Printf("Telefam Go backend listening on :%s", port)
		if err := srv.ListenAndServe(); err != nil && err != http.ErrServerClosed {
			log.Fatalf("server failed: %v", err)
		}
	}()

	stop := make(chan os.Signal, 1)
	signal.Notify(stop, syscall.SIGINT, syscall.SIGTERM)
	<-stop

	ctx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
	defer cancel()
	_ = srv.Shutdown(ctx)
}

func loadHMACSecret() []byte {
	if envSecret := os.Getenv("TELEFAM_HMAC_SECRET"); len(envSecret) >= 32 {
		return []byte(envSecret)
	}
	buf := make([]byte, 32)
	_, _ = rand.Read(buf)
	return buf
}

type gzipResponseWriter struct {
	http.ResponseWriter
	gw          *gzip.Writer
	wroteHeader bool
}

func (g *gzipResponseWriter) WriteHeader(statusCode int) {
	if !g.wroteHeader {
		g.wroteHeader = true
		g.Header().Del("Content-Length")
		g.ResponseWriter.WriteHeader(statusCode)
	}
}

func (g *gzipResponseWriter) Write(p []byte) (int, error) {
	if !g.wroteHeader {
		g.WriteHeader(http.StatusOK)
	}
	return g.gw.Write(p)
}

func (g *gzipResponseWriter) Flush() {
	_ = g.gw.Flush()
	if f, ok := g.ResponseWriter.(http.Flusher); ok {
		f.Flush()
	}
}

func gzipMiddleware(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		// Never alter WebSocket upgrades (preserves http.Hijacker)
		if strings.EqualFold(r.Header.Get("Upgrade"), "websocket") || r.URL.Path == "/v1/chat/ws" {
			next.ServeHTTP(w, r)
			return
		}

		// Transparently decompress gzip request bodies from mobile/desktop clients
		if strings.EqualFold(r.Header.Get("Content-Encoding"), "gzip") && r.Body != nil {
			gr, err := gzip.NewReader(io.LimitReader(r.Body, 4<<20))
			if err != nil {
				http.Error(w, "invalid gzip body", http.StatusBadRequest)
				return
			}
			defer gr.Close()
			r.Body = gr
		}

		// Skip double-compressing already-encoded binary audio/video/image downloads
		if !strings.Contains(strings.ToLower(r.Header.Get("Accept-Encoding")), "gzip") ||
			strings.HasPrefix(r.URL.Path, "/v1/media/") ||
			strings.HasPrefix(r.URL.Path, "/v1/chat/media/") ||
			strings.Contains(r.URL.Path, "/download") {
			next.ServeHTTP(w, r)
			return
		}

		gw, err := gzip.NewWriterLevel(w, gzip.BestCompression)
		if err != nil {
			next.ServeHTTP(w, r)
			return
		}
		defer gw.Close()

		w.Header().Set("Content-Encoding", "gzip")
		w.Header().Add("Vary", "Accept-Encoding")
		next.ServeHTTP(&gzipResponseWriter{ResponseWriter: w, gw: gw}, r)
	})
}

func securityHeadersMiddleware(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("X-Content-Type-Options", "nosniff")
		w.Header().Set("X-Frame-Options", "DENY")
		w.Header().Set("Referrer-Policy", "no-referrer")
		next.ServeHTTP(w, r)
	})
}
