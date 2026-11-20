package mailer

import (
	"bytes"
	"context"
	"encoding/json"
	"errors"
	"fmt"
	"html"
	"io"
	"net/http"
	"os"
	"strings"
	"time"
)

const defaultResendEndpoint = "https://api.resend.com/emails"

var ErrMailerUnavailable = errors.New("mailer unavailable")

type ResendClient struct {
	apiKey     string
	fromEmail  string
	endpoint   string
	httpClient *http.Client
}

type resendEmailPayload struct {
	From    string   `json:"from"`
	To      []string `json:"to"`
	Subject string   `json:"subject"`
	HTML    string   `json:"html"`
	Text    string   `json:"text"`
}

func NewResendClientFromEnv() *ResendClient {
	key := strings.TrimSpace(os.Getenv("RESEND_API_KEY"))
	from := strings.TrimSpace(os.Getenv("RESEND_FROM_EMAIL"))
	if from == "" {
		from = "Telefam <onboarding@resend.dev>"
	}
	return NewResendClient(key, from, defaultResendEndpoint)
}

func NewResendClient(apiKey, fromEmail, endpoint string) *ResendClient {
	if strings.TrimSpace(fromEmail) == "" {
		fromEmail = "Telefam <onboarding@resend.dev>"
	}
	if strings.TrimSpace(endpoint) == "" {
		endpoint = defaultResendEndpoint
	}
	return &ResendClient{
		apiKey:    strings.TrimSpace(apiKey),
		fromEmail: strings.TrimSpace(fromEmail),
		endpoint:  strings.TrimSpace(endpoint),
		httpClient: &http.Client{
			Timeout: 10 * time.Second,
		},
	}
}

func (c *ResendClient) IsConfigured() bool {
	return c != nil && c.apiKey != ""
}

func (c *ResendClient) SendOtpAndVerificationLink(ctx context.Context, toEmail, otpCode, verificationLink string) error {
	if !c.IsConfigured() {
		return ErrMailerUnavailable
	}

	safeEmail := html.EscapeString(toEmail)
	safeCode := html.EscapeString(otpCode)
	safeLink := html.EscapeString(verificationLink)

	htmlBody := fmt.Sprintf(
		`<div style="font-family:Arial,sans-serif;max-width:480px;margin:0 auto;padding:24px;color:#1A1C1E;">`+
			`<h2 style="color:#C80A0F;margin-bottom:8px;">Telefam Verification</h2>`+
			`<p style="font-size:14px;color:#4B5563;">Use the verification code below for %s:</p>`+
			`<div style="font-size:28px;font-weight:700;letter-spacing:6px;padding:16px;background:#FCECEC;color:#C80A0F;border-radius:12px;text-align:center;margin:16px 0;">%s</div>`+
			`<p style="font-size:14px;color:#4B5563;">Or click the link below to verify your account:</p>`+
			`<p style="margin:16px 0;"><a href="%s" style="display:inline-block;padding:12px 20px;background:#C80A0F;color:#FFFFFF;text-decoration:none;border-radius:10px;font-weight:600;">Verify Telefam Account</a></p>`+
			`<p style="font-size:12px;color:#6B7280;">This code expires in 15 minutes.</p>`+
			`</div>`,
		safeEmail, safeCode, safeLink,
	)

	textBody := fmt.Sprintf(
		"Your Telefam verification code is: %s\nOr open this link to verify your account: %s\nThis code expires in 15 minutes.",
		otpCode, verificationLink,
	)

	payload, err := json.Marshal(resendEmailPayload{
		From:    c.fromEmail,
		To:      []string{toEmail},
		Subject: "Your Telefam verification code",
		HTML:    htmlBody,
		Text:    textBody,
	})
	if err != nil {
		return err
	}

	req, err := http.NewRequestWithContext(ctx, http.MethodPost, c.endpoint, bytes.NewReader(payload))
	if err != nil {
		return err
	}
	req.Header.Set("Authorization", "Bearer "+c.apiKey)
	req.Header.Set("Content-Type", "application/json")
	req.Header.Set("User-Agent", "TelefamBackend/1.0")

	resp, err := c.httpClient.Do(req)
	if err != nil {
		return err
	}
	defer resp.Body.Close()
	_, _ = io.Copy(io.Discard, io.LimitReader(resp.Body, 4096))

	if resp.StatusCode < 200 || resp.StatusCode >= 300 {
		return fmt.Errorf("resend status %d", resp.StatusCode)
	}
	return nil
}
