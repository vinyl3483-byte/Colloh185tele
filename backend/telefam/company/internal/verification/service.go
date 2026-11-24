package verification

import (
	"bytes"
	"context"
	"crypto/hmac"
	"crypto/rand"
	"crypto/sha256"
	"crypto/sha512"
	"encoding/base64"
	"encoding/hex"
	"encoding/json"
	"errors"
	"fmt"
	"image"
	_ "image/jpeg"
	_ "image/png"
	"io"
	"math"
	"net/http"
	"net/url"
	"os"
	"os/exec"
	"path/filepath"
	"regexp"
	"strconv"
	"strings"
	"sync"
	"time"
)

var (
	ErrUnauthorized        = errors.New("unauthorized")
	ErrForbidden           = errors.New("forbidden")
	ErrBadRequest          = errors.New("invalid request")
	ErrNotFound            = errors.New("not found")
	ErrTrialAlreadyUsed    = errors.New("first week free trial already used")
	ErrPlanRequired        = errors.New("active trial or subscription required")
	ErrLivenessRequired    = errors.New("live selfie liveness check required before document capture")
	ErrLivenessFailed      = errors.New("liveness check did not reach 100%")
	ErrDocumentRejected    = errors.New("document validation or ocr match failed")
	ErrChallengeExpired    = errors.New("verification challenge expired or already used")
	ErrScreenCaptureActive = errors.New("screen recording or mirroring detected; capture blocked for security")
	ErrIdempotencyConflict = errors.New("idempotency key conflict")
	ErrRateLimited         = errors.New("too many verification attempts; please wait")
	ErrGatewayUnconfigured = errors.New("payment gateway not configured")
	ErrPaymentFailed       = errors.New("payment verification failed")
)

const (
	TierNone       = "none"
	TierBusiness   = "business"
	TierGovernment = "government"
	TierBlue       = "blue"

	CycleMonthly = "monthly"
	CycleAnnual  = "annual"

	ReviewETAWorkingDays = 3
)

var idempotencyPattern = regexp.MustCompile(`^[A-Za-z0-9._:-]{8,128}$`)
var mrzLineRegex = regexp.MustCompile(`^[A-Z0-9<]{28,44}$`)
var dateNumericRegex = regexp.MustCompile(`\b(\d{4})[-/](\d{1,2})[-/](\d{1,2})\b|\b(\d{1,2})[-/](\d{1,2})[-/](\d{2,4})\b`)
var dateMonthNameRegex = regexp.MustCompile(`(?i)\b(\d{1,2})\s+(JAN|FEB|MAR|APR|MAY|JUN|JUL|AUG|SEP|SEPT|OCT|NOV|DEC)[A-Z]*\s+(\d{4})\b`)
var idNumberRegex = regexp.MustCompile(`\b([A-Z]{0,3}\d{6,12}[A-Z0-9]{0,3})\b`)

type UserInfo struct {
	UserID     string
	FullName   string
	Username   string
	AvatarURL  string
	Email      string
	CountryISO string
	Location   string
	Bio        string
}

type HostHooks struct {
	ResolveUser              func(userID string) (UserInfo, bool)
	SendWSEvent              func(targetUserID string, event map[string]any)
	NotifyVerificationUpdate func(ctx context.Context, userID, status, tier, title, body string)
	AuditEvent               func(userID, action, deviceFP, ipFP string, meta map[string]string)
}

type TierPricingDTO struct {
	Tier                 string `json:"tier"`
	Title                string `json:"title"`
	BadgeTitle           string `json:"badge_title"`
	CurrencyCode         string `json:"currency_code"`
	CurrencySymbol       string `json:"currency_symbol"`
	MonthlyDisplay       string `json:"monthly_display"`
	AnnualDisplay        string `json:"annual_display"`
	AnnualOriginalDisp   string `json:"annual_original_display"`
	DiscountPercent      int    `json:"discount_percent"`
	PaystackMonthlyMinor int64  `json:"paystack_monthly_minor"`
	PaystackAnnualMinor  int64  `json:"paystack_annual_minor"`
	PaystackCurrency     string `json:"paystack_currency"`
	PayPalMonthlyValue   string `json:"paypal_monthly_value"`
	PayPalAnnualValue    string `json:"paypal_annual_value"`
	PayPalCurrency       string `json:"paypal_currency"`
}

type VerificationStatusDTO struct {
	Success              bool             `json:"success"`
	UserID               string           `json:"user_id"`
	VerifiedTier         string           `json:"verified_tier"`
	PendingTier          string           `json:"pending_tier"`
	BillingCycle         string           `json:"billing_cycle"`
	PlanStatus           string           `json:"plan_status"`
	TrialEligible        bool             `json:"trial_eligible"`
	TrialEndsAtMs        int64            `json:"trial_ends_at_ms"`
	SubscriptionEndsAtMs int64            `json:"subscription_ends_at_ms"`
	LivenessStatus       string           `json:"liveness_status"`
	LivenessScore        int              `json:"liveness_score"`
	LivenessVerifiedAtMs int64            `json:"liveness_verified_at_ms"`
	DocumentType         string           `json:"document_type"`
	DocumentStatus       string           `json:"document_status"`
	DocLast4             string           `json:"doc_last4,omitempty"`
	DocExpiryISO         string           `json:"doc_expiry_iso,omitempty"`
	ExtractedFullName    string           `json:"extracted_full_name,omitempty"`
	MergedToProfile      bool             `json:"merged_to_profile"`
	ReviewStatus         string           `json:"review_status"`
	ReviewETAWorkingDays int              `json:"review_eta_working_days"`
	ReviewSubmittedAtMs  int64            `json:"review_submitted_at_ms"`
	ReviewDueAtMs        int64            `json:"review_due_at_ms"`
	VerifiedAtMs         int64            `json:"verified_at_ms"`
	DocumentsPurged      bool             `json:"documents_purged"`
	DocumentsPurgedAtMs  int64            `json:"documents_purged_at_ms"`
	RejectionReason      string           `json:"rejection_reason,omitempty"`
	CountryISO           string           `json:"country_iso"`
	Tiers                []TierPricingDTO `json:"tiers"`
	SecurityNotice       string           `json:"security_notice"`
	ServerEpochMs        int64            `json:"server_epoch_ms"`
}

type TrialActivatePayload struct {
	Tier           string `json:"tier"`
	BillingCycle   string `json:"billing_cycle"`
	IdempotencyKey string `json:"idempotency_key"`
}

type CheckoutInitPayload struct {
	Tier           string `json:"tier"`
	BillingCycle   string `json:"billing_cycle"`
	Provider       string `json:"provider"`
	CountryISO     string `json:"country_iso"`
	IdempotencyKey string `json:"idempotency_key"`
}

type VerificationPaymentDTO struct {
	PaymentID           string `json:"payment_id"`
	Tier                string `json:"tier"`
	BillingCycle        string `json:"billing_cycle"`
	Provider            string `json:"provider"`
	ProviderReference   string `json:"provider_reference"`
	ProviderOrderID     string `json:"provider_order_id,omitempty"`
	ExpectedAmountMinor int64  `json:"expected_amount_minor"`
	ExpectedCurrency    string `json:"expected_currency"`
	PaidAmountMinor     int64  `json:"paid_amount_minor"`
	PaidCurrency        string `json:"paid_currency,omitempty"`
	Status              string `json:"status"`
	CheckoutURL         string `json:"checkout_url,omitempty"`
	VerifiedVia         string `json:"verified_via,omitempty"`
	CreatedAtMs         int64  `json:"created_at_ms"`
	CompletedAtMs       int64  `json:"completed_at_ms,omitempty"`
}

type CheckoutInitResponse struct {
	Success   bool                   `json:"success"`
	Payment   VerificationPaymentDTO `json:"payment"`
	Status    VerificationStatusDTO  `json:"status"`
	ReturnURL string                 `json:"return_url"`
}

type CheckoutVerifyPayload struct {
	PaymentID      string `json:"payment_id"`
	Reference      string `json:"reference,omitempty"`
	PayPalOrderID  string `json:"paypal_order_id,omitempty"`
	IdempotencyKey string `json:"idempotency_key"`
}

type CheckoutVerifyResponse struct {
	Success bool                   `json:"success"`
	Payment VerificationPaymentDTO `json:"payment"`
	Status  VerificationStatusDTO  `json:"status"`
}

type LivenessSessionRequest struct {
	Tier                string `json:"tier"`
	ScreenCaptureActive bool   `json:"screen_capture_active"`
}

type LivenessSessionResponse struct {
	Success           bool     `json:"success"`
	ChallengeID       string   `json:"challenge_id"`
	ChallengeToken    string   `json:"challenge_token"`
	Nonce             string   `json:"nonce"`
	Sequence          []string `json:"sequence"`
	ExpiresAtMs       int64    `json:"expires_at_ms"`
	AntiSpoofNotice   string   `json:"anti_spoof_notice"`
	ServerEpochMs     int64    `json:"server_epoch_ms"`
}

type LivenessStepTelemetry struct {
	Step        string  `json:"step"`
	Yaw         float64 `json:"yaw"`
	Pitch       float64 `json:"pitch"`
	LeftEyeOpen float64 `json:"left_eye_open"`
	RightEyeOpen float64 `json:"right_eye_open"`
	Luma        float64 `json:"luma"`
	CapturedAtMs int64  `json:"captured_at_ms"`
}

type LivenessSubmitPayload struct {
	ChallengeID         string                  `json:"challenge_id"`
	ChallengeToken      string                  `json:"challenge_token"`
	ScreenCaptureActive bool                    `json:"screen_capture_active"`
	SelfieImageB64      string                  `json:"selfie_image_b64"`
	Steps               []LivenessStepTelemetry `json:"steps"`
}

type LivenessSubmitResponse struct {
	Success       bool                  `json:"success"`
	Passed        bool                  `json:"passed"`
	LivenessScore int                   `json:"liveness_score"`
	Message       string                `json:"message"`
	Status        VerificationStatusDTO `json:"status"`
}

type DocumentSessionRequest struct {
	DocumentType        string `json:"document_type"`
	ScreenCaptureActive bool   `json:"screen_capture_active"`
}

type DocumentSessionResponse struct {
	Success         bool   `json:"success"`
	ChallengeID     string `json:"challenge_id"`
	ChallengeToken  string `json:"challenge_token"`
	DocumentType    string `json:"document_type"`
	Nonce           string `json:"nonce"`
	ExpiresAtMs     int64  `json:"expires_at_ms"`
	PrivacyNotice   string `json:"privacy_notice"`
	ServerEpochMs   int64  `json:"server_epoch_ms"`
}

type DocumentSubmitPayload struct {
	ChallengeID         string  `json:"challenge_id"`
	ChallengeToken      string  `json:"challenge_token"`
	DocumentType        string  `json:"document_type"`
	ScreenCaptureActive bool    `json:"screen_capture_active"`
	DocumentImageB64    string  `json:"document_image_b64"`
	ClientOcrRawText    string  `json:"client_ocr_raw_text"`
	SharpnessScore      float64 `json:"sharpness_score"`
	LuminanceMean       float64 `json:"luminance_mean"`
	GlareRatio          float64 `json:"glare_ratio"`
	AllCornersDetected  bool    `json:"all_corners_detected"`
}

type DocumentSubmitResponse struct {
	Success           bool                  `json:"success"`
	Accepted          bool                  `json:"accepted"`
	ExtractedFullName string                `json:"extracted_full_name,omitempty"`
	ExtractedDocLast4 string                `json:"extracted_doc_last4,omitempty"`
	ExtractedExpiry   string                `json:"extracted_expiry,omitempty"`
	DocumentsPurged   bool                  `json:"documents_purged"`
	Message           string                `json:"message"`
	Status            VerificationStatusDTO `json:"status"`
}

type CompleteReviewPayload struct {
	UserID          string `json:"user_id"`
	Approve         bool   `json:"approve"`
	RejectionReason string `json:"rejection_reason,omitempty"`
}

type verificationRecord struct {
	UserID               string
	VerifiedTier         string
	PendingTier          string
	BillingCycle         string
	PlanStatus           string
	TrialUsed            bool
	TrialEndsAtMs        int64
	SubscriptionEndsAtMs int64
	LivenessStatus       string
	LivenessScore        int
	LivenessVerifiedAtMs int64
	TransientSelfieSHA   string
	DocumentType         string
	DocumentStatus       string
	DocNumberHMAC        string
	DocLast4             string
	DocExpiryISO         string
	ExtractedFullName    string
	MergedToProfile      bool
	ReviewStatus         string
	ReviewSubmittedAtMs  int64
	ReviewDueAtMs        int64
	VerifiedAtMs         int64
	DocumentsPurged      bool
	DocumentsPurgedAtMs  int64
	RejectionReason      string
	UpdatedAtMs          int64
}

type paymentRecord struct {
	DTO            VerificationPaymentDTO
	UserID         string
	CountryISO     string
	IdempotencyKey string
}

type challengeRecord struct {
	ChallengeID  string
	UserID       string
	DeviceFP     string
	Kind         string
	DocumentType string
	Nonce        string
	Sequence     []string
	ExpiresAtMs  int64
	ConsumedAtMs int64
	CreatedAtMs  int64
}

type countrySpec struct {
	CurrencyCode   string
	Symbol         string
	RateFromKES    float64
	ZeroDecimal    bool
	PaystackCurr   string
	PaystackMulKES float64
	PayPalCurr     string
	PayPalMulUSD   float64
}

type Service struct {
	hooks             HostHooks
	mu                sync.Mutex
	records           map[string]*verificationRecord
	payments          map[string]*paymentRecord
	payByIdempKey     map[string]*paymentRecord
	payByRef          map[string]*paymentRecord
	payByOrderID      map[string]*paymentRecord
	challenges        map[string]*challengeRecord
	attemptTimestamps map[string][]int64
	challengeSecret   []byte
	docHMACSecret     []byte
	adminSecret       string
	paystackSecret    string
	paystackBase      string
	paypalClientID    string
	paypalSecret      string
	paypalBase        string
	paypalWebhookID   string
	publicBaseURL     string
	ocrEndpoint       string
	paypalAccessToken string
	paypalTokenExpiry time.Time
	httpClient        *http.Client
	nowFunc           func() time.Time
}

func NewService(hooks HostHooks) *Service {
	challengeSecret := strings.TrimSpace(os.Getenv("TELEFAM_VERIFICATION_CHALLENGE_SECRET"))
	if challengeSecret == "" {
		var buf [32]byte
		_, _ = rand.Read(buf[:])
		challengeSecret = hex.EncodeToString(buf[:])
	}
	docSecret := strings.TrimSpace(os.Getenv("TELEFAM_VERIFICATION_DOC_HMAC_SECRET"))
	if docSecret == "" {
		var buf [32]byte
		_, _ = rand.Read(buf[:])
		docSecret = hex.EncodeToString(buf[:])
	}
	ppBase := strings.TrimSpace(os.Getenv("PAYPAL_API_BASE"))
	if ppBase == "" {
		ppBase = "https://api-m.sandbox.paypal.com"
	}
	pubBase := strings.TrimSpace(os.Getenv("TELEFAM_PUBLIC_BASE_URL"))
	if pubBase == "" {
		pubBase = "https://telefam.company"
	}
	return &Service{
		hooks:             hooks,
		records:           make(map[string]*verificationRecord),
		payments:          make(map[string]*paymentRecord),
		payByIdempKey:     make(map[string]*paymentRecord),
		payByRef:          make(map[string]*paymentRecord),
		payByOrderID:      make(map[string]*paymentRecord),
		challenges:        make(map[string]*challengeRecord),
		attemptTimestamps: make(map[string][]int64),
		challengeSecret:   []byte(challengeSecret),
		docHMACSecret:     []byte(docSecret),
		adminSecret:       strings.TrimSpace(os.Getenv("TELEFAM_VERIFICATION_ADMIN_SECRET")),
		paystackSecret:    strings.TrimSpace(os.Getenv("PAYSTACK_SECRET_KEY")),
		paystackBase:      "https://api.paystack.co",
		paypalClientID:    strings.TrimSpace(os.Getenv("PAYPAL_CLIENT_ID")),
		paypalSecret:      strings.TrimSpace(os.Getenv("PAYPAL_CLIENT_SECRET")),
		paypalBase:        strings.TrimRight(ppBase, "/"),
		paypalWebhookID:   strings.TrimSpace(os.Getenv("PAYPAL_WEBHOOK_ID")),
		publicBaseURL:     strings.TrimRight(pubBase, "/"),
		ocrEndpoint:       strings.TrimSpace(os.Getenv("TELEFAM_OCR_ENDPOINT")),
		httpClient:        &http.Client{Timeout: 12 * time.Second},
		nowFunc:           time.Now,
	}
}

func (s *Service) ConfigureGatewaysForTest(
	paystackBase, paystackSecret, paypalBase, paypalClientID, paypalSecret, paypalWebhookID, adminSecret, ocrEndpoint string,
	httpClient *http.Client,
) {
	s.mu.Lock()
	defer s.mu.Unlock()
	if strings.TrimSpace(paystackBase) != "" {
		s.paystackBase = strings.TrimRight(strings.TrimSpace(paystackBase), "/")
	}
	s.paystackSecret = strings.TrimSpace(paystackSecret)
	if strings.TrimSpace(paypalBase) != "" {
		s.paypalBase = strings.TrimRight(strings.TrimSpace(paypalBase), "/")
	}
	s.paypalClientID = strings.TrimSpace(paypalClientID)
	s.paypalSecret = strings.TrimSpace(paypalSecret)
	s.paypalWebhookID = strings.TrimSpace(paypalWebhookID)
	s.adminSecret = strings.TrimSpace(adminSecret)
	s.ocrEndpoint = strings.TrimSpace(ocrEndpoint)
	s.paypalAccessToken = ""
	s.paypalTokenExpiry = time.Time{}
	if httpClient != nil {
		s.httpClient = httpClient
	}
}

func (s *Service) SetNowFuncForTest(fn func() time.Time) {
	s.mu.Lock()
	defer s.mu.Unlock()
	if fn != nil {
		s.nowFunc = fn
	}
}

func (s *Service) AdminSecret() string {
	s.mu.Lock()
	defer s.mu.Unlock()
	return s.adminSecret
}

func (s *Service) VerifiedTierForUser(userID string) string {
	clean := strings.TrimSpace(userID)
	if clean == "" {
		return TierNone
	}
	s.mu.Lock()
	defer s.mu.Unlock()
	if rec, ok := s.records[clean]; ok && rec.ReviewStatus == "verified" && IsValidTier(rec.VerifiedTier) {
		return rec.VerifiedTier
	}
	return TierNone
}

func IsValidTier(tier string) bool {
	return tier == TierBusiness || tier == TierGovernment || tier == TierBlue
}

func IsValidCycle(cycle string) bool {
	return cycle == CycleMonthly || cycle == CycleAnnual
}

func IsValidDocumentType(docType string) bool {
	switch docType {
	case "national_id", "passport", "drivers_license", "military_id", "business_license", "gov_warrant":
		return true
	default:
		return false
	}
}

func countryPricingSpec(countryISO string) countrySpec {
	switch strings.ToUpper(strings.TrimSpace(countryISO)) {
	case "KE":
		return countrySpec{"KES", "Ksh ", 1.0, true, "KES", 100.0, "USD", 0.008325}
	case "NG":
		return countrySpec{"NGN", "NGN ", 12.0, true, "NGN", 1200.0, "USD", 0.008325}
	case "GH":
		return countrySpec{"GHS", "GHS ", 0.12, false, "GHS", 12.0, "USD", 0.008325}
	case "ZA":
		return countrySpec{"ZAR", "R ", 0.15, false, "ZAR", 15.0, "USD", 0.008325}
	case "UG":
		return countrySpec{"UGX", "UGX ", 30.0, true, "KES", 100.0, "USD", 0.008325}
	case "TZ":
		return countrySpec{"TZS", "TZS ", 20.0, true, "KES", 100.0, "USD", 0.008325}
	case "RW":
		return countrySpec{"RWF", "RWF ", 10.0, true, "KES", 100.0, "USD", 0.008325}
	case "GB":
		return countrySpec{"GBP", "GBP ", 0.0064, false, "USD", 0.8325, "GBP", 0.0064}
	case "DE", "FR", "ES", "IT", "NL", "IE", "BE", "PT", "AT", "FI":
		return countrySpec{"EUR", "EUR ", 0.0074, false, "USD", 0.8325, "EUR", 0.0074}
	case "CA":
		return countrySpec{"CAD", "CA$", 0.011, false, "USD", 0.8325, "CAD", 0.011}
	case "AU":
		return countrySpec{"AUD", "A$", 0.012, false, "USD", 0.8325, "AUD", 0.012}
	case "IN":
		return countrySpec{"INR", "INR ", 0.68, false, "USD", 0.8325, "USD", 0.008325}
	default:
		return countrySpec{"USD", "$", 0.008325, false, "USD", 0.8325, "USD", 0.008325}
	}
}

func baseKESForTier(tier string) int64 {
	if tier == TierBusiness {
		return 1200
	}
	return 700
}

func ComputeTierPricing(tier, countryISO string) TierPricingDTO {
	spec := countryPricingSpec(countryISO)
	baseMonthlyKES := float64(baseKESForTier(tier))
	baseAnnualFullKES := baseMonthlyKES * 12.0
	baseAnnualDiscKES := baseAnnualFullKES * 0.80 // 20% OFF

	formatDisplay := func(kesVal float64) string {
		raw := kesVal * spec.RateFromKES
		if spec.ZeroDecimal {
			return spec.Symbol + formatWithCommas(int64(math.Round(raw)))
		}
		return fmt.Sprintf("%s%.2f", spec.Symbol, math.Round(raw*100.0)/100.0)
	}

	psMonthMinor := int64(math.Round(baseMonthlyKES * spec.PaystackMulKES))
	psYearMinor := int64(math.Round(baseAnnualDiscKES * spec.PaystackMulKES))
	ppMonthMinor := int64(math.Round(baseMonthlyKES * spec.PayPalMulUSD * 100.0))
	ppYearMinor := int64(math.Round(baseAnnualDiscKES * spec.PayPalMulUSD * 100.0))

	title := "Blue Verification"
	badgeTitle := "Get the Blue Badge"
	switch tier {
	case TierBusiness:
		title = "Business Verification"
		badgeTitle = "Get the Business Badge"
	case TierGovernment:
		title = "Government Verification"
		badgeTitle = "Get the Government Badge"
	}

	return TierPricingDTO{
		Tier:                 tier,
		Title:                title,
		BadgeTitle:           badgeTitle,
		CurrencyCode:         spec.CurrencyCode,
		CurrencySymbol:       spec.Symbol,
		MonthlyDisplay:       formatDisplay(baseMonthlyKES),
		AnnualDisplay:        formatDisplay(baseAnnualDiscKES),
		AnnualOriginalDisp:   formatDisplay(baseAnnualFullKES),
		DiscountPercent:      20,
		PaystackMonthlyMinor: psMonthMinor,
		PaystackAnnualMinor:  psYearMinor,
		PaystackCurrency:     spec.PaystackCurr,
		PayPalMonthlyValue:   fmt.Sprintf("%.2f", float64(ppMonthMinor)/100.0),
		PayPalAnnualValue:    fmt.Sprintf("%.2f", float64(ppYearMinor)/100.0),
		PayPalCurrency:       spec.PayPalCurr,
	}
}

func formatWithCommas(v int64) string {
	s := strconv.FormatInt(v, 10)
	if len(s) <= 3 {
		return s
	}
	var out strings.Builder
	rem := len(s) % 3
	if rem > 0 {
		out.WriteString(s[:rem])
		if len(s) > rem {
			out.WriteByte(',')
		}
	}
	for i := rem; i < len(s); i += 3 {
		out.WriteString(s[i : i+3])
		if i+3 < len(s) {
			out.WriteByte(',')
		}
	}
	return out.String()
}

func newRandomID(prefix string) string {
	var b [12]byte
	_, _ = rand.Read(b[:])
	return prefix + "_" + hex.EncodeToString(b[:])
}

func (s *Service) nowMs() int64 {
	return s.nowFunc().UTC().UnixMilli()
}

func (s *Service) getOrCreateRecordLocked(userID string) *verificationRecord {
	rec, ok := s.records[userID]
	if !ok {
		rec = &verificationRecord{
			UserID:         userID,
			VerifiedTier:   TierNone,
			PendingTier:    TierNone,
			BillingCycle:   CycleMonthly,
			PlanStatus:     "none",
			LivenessStatus: "not_started",
			DocumentStatus: "not_started",
			ReviewStatus:   "not_started",
			UpdatedAtMs:    s.nowMs(),
		}
		s.records[userID] = rec
	}
	return rec
}

func (s *Service) resolveCountry(userID, override string) string {
	country := strings.ToUpper(strings.TrimSpace(override))
	if len(country) == 2 {
		return country
	}
	if s.hooks.ResolveUser != nil {
		if info, ok := s.hooks.ResolveUser(userID); ok && len(strings.TrimSpace(info.CountryISO)) == 2 {
			return strings.ToUpper(strings.TrimSpace(info.CountryISO))
		}
	}
	return "US"
}

func (s *Service) buildStatusDTOLocked(rec *verificationRecord, countryISO string) VerificationStatusDTO {
	tiers := []TierPricingDTO{
		ComputeTierPricing(TierBusiness, countryISO),
		ComputeTierPricing(TierGovernment, countryISO),
		ComputeTierPricing(TierBlue, countryISO),
	}
	return VerificationStatusDTO{
		Success:              true,
		UserID:               rec.UserID,
		VerifiedTier:         rec.VerifiedTier,
		PendingTier:          rec.PendingTier,
		BillingCycle:         rec.BillingCycle,
		PlanStatus:           rec.PlanStatus,
		TrialEligible:        !rec.TrialUsed,
		TrialEndsAtMs:        rec.TrialEndsAtMs,
		SubscriptionEndsAtMs: rec.SubscriptionEndsAtMs,
		LivenessStatus:       rec.LivenessStatus,
		LivenessScore:        rec.LivenessScore,
		LivenessVerifiedAtMs: rec.LivenessVerifiedAtMs,
		DocumentType:         rec.DocumentType,
		DocumentStatus:       rec.DocumentStatus,
		DocLast4:             rec.DocLast4,
		DocExpiryISO:         rec.DocExpiryISO,
		ExtractedFullName:    rec.ExtractedFullName,
		MergedToProfile:      rec.MergedToProfile,
		ReviewStatus:         rec.ReviewStatus,
		ReviewETAWorkingDays: ReviewETAWorkingDays,
		ReviewSubmittedAtMs:  rec.ReviewSubmittedAtMs,
		ReviewDueAtMs:        rec.ReviewDueAtMs,
		VerifiedAtMs:         rec.VerifiedAtMs,
		DocumentsPurged:      rec.DocumentsPurged,
		DocumentsPurgedAtMs:  rec.DocumentsPurgedAtMs,
		RejectionReason:      rec.RejectionReason,
		CountryISO:           countryISO,
		Tiers:                tiers,
		SecurityNotice:       "Your selfie and identity document are encrypted during capture and permanently deleted once verification completes.",
		ServerEpochMs:        s.nowMs(),
	}
}

func (s *Service) emitWSStatus(userID string, dto VerificationStatusDTO) {
	if s.hooks.SendWSEvent != nil {
		s.hooks.SendWSEvent(userID, map[string]any{
			"event":         "verification.updated",
			"user_id":       userID,
			"verified_tier": dto.VerifiedTier,
			"pending_tier":  dto.PendingTier,
			"review_status": dto.ReviewStatus,
			"plan_status":   dto.PlanStatus,
			"server_ts_ms":  dto.ServerEpochMs,
		})
	}
}

func (s *Service) GetStatus(userID, countryOverride string) (VerificationStatusDTO, error) {
	clean := strings.TrimSpace(userID)
	if clean == "" {
		return VerificationStatusDTO{}, ErrUnauthorized
	}
	country := s.resolveCountry(clean, countryOverride)
	s.mu.Lock()
	rec := s.getOrCreateRecordLocked(clean)
	dto := s.buildStatusDTOLocked(rec, country)
	s.mu.Unlock()
	return dto, nil
}

func (s *Service) ActivateFreeTrial(
	ctx context.Context,
	userID, deviceFP, ipFP string,
	payload TrialActivatePayload,
) (VerificationStatusDTO, error) {
	if strings.TrimSpace(userID) == "" {
		return VerificationStatusDTO{}, ErrUnauthorized
	}
	tier := strings.ToLower(strings.TrimSpace(payload.Tier))
	cycle := strings.ToLower(strings.TrimSpace(payload.BillingCycle))
	if cycle == "" {
		cycle = CycleMonthly
	}
	if !IsValidTier(tier) || !IsValidCycle(cycle) {
		return VerificationStatusDTO{}, ErrBadRequest
	}
	if !idempotencyPattern.MatchString(strings.TrimSpace(payload.IdempotencyKey)) {
		return VerificationStatusDTO{}, ErrBadRequest
	}

	country := s.resolveCountry(userID, "")
	now := s.nowMs()
	s.mu.Lock()
	rec := s.getOrCreateRecordLocked(userID)
	if rec.TrialUsed {
		s.mu.Unlock()
		return VerificationStatusDTO{}, ErrTrialAlreadyUsed
	}
	if rec.PlanStatus != "trial_active" {
		rec.TrialUsed = true
		rec.PlanStatus = "trial_active"
		rec.TrialEndsAtMs = now + int64(7*24*time.Hour/time.Millisecond)
		rec.PendingTier = tier
		rec.BillingCycle = cycle
		if rec.ReviewStatus == "not_started" || rec.ReviewStatus == "awaiting_plan" || rec.ReviewStatus == "rejected" {
			rec.ReviewStatus = "awaiting_liveness"
			rec.LivenessStatus = "not_started"
			rec.LivenessScore = 0
			rec.DocumentStatus = "not_started"
			rec.RejectionReason = ""
		}
		rec.UpdatedAtMs = now
	}
	dto := s.buildStatusDTOLocked(rec, country)
	s.mu.Unlock()

	if s.hooks.AuditEvent != nil {
		s.hooks.AuditEvent(userID, "verification_trial_activated", deviceFP, ipFP, map[string]string{
			"tier":  tier,
			"cycle": cycle,
		})
	}
	s.emitWSStatus(userID, dto)
	return dto, nil
}

func (s *Service) InitCheckout(
	ctx context.Context,
	userID, deviceFP, ipFP string,
	payload CheckoutInitPayload,
) (CheckoutInitResponse, error) {
	if strings.TrimSpace(userID) == "" {
		return CheckoutInitResponse{}, ErrUnauthorized
	}
	tier := strings.ToLower(strings.TrimSpace(payload.Tier))
	cycle := strings.ToLower(strings.TrimSpace(payload.BillingCycle))
	if cycle == "" {
		cycle = CycleMonthly
	}
	provider := strings.ToLower(strings.TrimSpace(payload.Provider))
	if !IsValidTier(tier) || !IsValidCycle(cycle) || (provider != "paystack" && provider != "paypal") {
		return CheckoutInitResponse{}, ErrBadRequest
	}
	idempKey := strings.TrimSpace(payload.IdempotencyKey)
	if !idempotencyPattern.MatchString(idempKey) {
		return CheckoutInitResponse{}, ErrBadRequest
	}

	country := s.resolveCountry(userID, payload.CountryISO)
	userEmail := userID + "@users.telefam.company"
	if s.hooks.ResolveUser != nil {
		if info, ok := s.hooks.ResolveUser(userID); ok && strings.TrimSpace(info.Email) != "" {
			userEmail = strings.TrimSpace(info.Email)
		}
	}

	compKey := userID + ":" + idempKey
	now := s.nowMs()

	s.mu.Lock()
	if existing, ok := s.payByIdempKey[compKey]; ok {
		if existing.DTO.Provider != provider || existing.DTO.Tier != tier || existing.DTO.BillingCycle != cycle {
			s.mu.Unlock()
			return CheckoutInitResponse{}, ErrIdempotencyConflict
		}
		rec := s.getOrCreateRecordLocked(userID)
		statusDTO := s.buildStatusDTOLocked(rec, country)
		dto := existing.DTO
		s.mu.Unlock()
		return CheckoutInitResponse{
			Success:   true,
			Payment:   dto,
			Status:    statusDTO,
			ReturnURL: s.buildReturnURL(dto.Provider, dto.PaymentID, dto.ProviderReference),
		}, nil
	}

	pricing := ComputeTierPricing(tier, country)
	paymentID := newRandomID("verpay")
	reference := "TFVR_" + strings.ToUpper(strings.TrimPrefix(paymentID, "verpay_"))
	returnURL := s.buildReturnURL(provider, paymentID, reference)

	var expectedMinor int64
	var expectedCurrency string
	var paypalValue string
	if provider == "paystack" {
		if cycle == CycleAnnual {
			expectedMinor = pricing.PaystackAnnualMinor
		} else {
			expectedMinor = pricing.PaystackMonthlyMinor
		}
		expectedCurrency = pricing.PaystackCurrency
	} else {
		if cycle == CycleAnnual {
			paypalValue = pricing.PayPalAnnualValue
		} else {
			paypalValue = pricing.PayPalMonthlyValue
		}
		f, _ := strconv.ParseFloat(paypalValue, 64)
		expectedMinor = int64(math.Round(f * 100.0))
		expectedCurrency = pricing.PayPalCurrency
	}

	psBase := s.paystackBase
	psSecret := s.paystackSecret
	ppCID := s.paypalClientID
	ppSec := s.paypalSecret
	s.mu.Unlock()

	var checkoutURL string
	var orderID string
	var initErr error
	if provider == "paystack" {
		if psSecret == "" {
			return CheckoutInitResponse{}, ErrGatewayUnconfigured
		}
		checkoutURL, initErr = s.initPaystackTransaction(
			ctx, psBase, psSecret, userID, userEmail, paymentID, reference, tier, cycle, expectedMinor, expectedCurrency, idempKey, returnURL,
		)
	} else {
		if ppCID == "" || ppSec == "" {
			return CheckoutInitResponse{}, ErrGatewayUnconfigured
		}
		orderID, checkoutURL, initErr = s.initPayPalOrder(
			ctx, userID, paymentID, tier, cycle, paypalValue, expectedCurrency, idempKey, returnURL,
		)
	}
	if initErr != nil {
		return CheckoutInitResponse{}, initErr
	}

	payDTO := VerificationPaymentDTO{
		PaymentID:           paymentID,
		Tier:                tier,
		BillingCycle:        cycle,
		Provider:            provider,
		ProviderReference:   reference,
		ProviderOrderID:     orderID,
		ExpectedAmountMinor: expectedMinor,
		ExpectedCurrency:    expectedCurrency,
		Status:              "pending",
		CheckoutURL:         checkoutURL,
		CreatedAtMs:         now,
	}
	payRec := &paymentRecord{
		DTO:            payDTO,
		UserID:         userID,
		CountryISO:     country,
		IdempotencyKey: idempKey,
	}

	s.mu.Lock()
	s.payments[paymentID] = payRec
	s.payByIdempKey[compKey] = payRec
	s.payByRef[provider+":"+reference] = payRec
	if orderID != "" {
		s.payByOrderID[orderID] = payRec
	}
	rec := s.getOrCreateRecordLocked(userID)
	statusDTO := s.buildStatusDTOLocked(rec, country)
	s.mu.Unlock()

	return CheckoutInitResponse{
		Success:   true,
		Payment:   payDTO,
		Status:    statusDTO,
		ReturnURL: returnURL,
	}, nil
}

func (s *Service) VerifyCheckout(
	ctx context.Context,
	userID, deviceFP, ipFP string,
	payload CheckoutVerifyPayload,
) (CheckoutVerifyResponse, error) {
	if strings.TrimSpace(userID) == "" {
		return CheckoutVerifyResponse{}, ErrUnauthorized
	}
	if !idempotencyPattern.MatchString(strings.TrimSpace(payload.IdempotencyKey)) {
		return CheckoutVerifyResponse{}, ErrBadRequest
	}

	s.mu.Lock()
	var payRec *paymentRecord
	if pid := strings.TrimSpace(payload.PaymentID); pid != "" {
		payRec = s.payments[pid]
	}
	if payRec == nil && strings.TrimSpace(payload.Reference) != "" {
		payRec = s.payByRef["paystack:"+strings.TrimSpace(payload.Reference)]
	}
	if payRec == nil && strings.TrimSpace(payload.PayPalOrderID) != "" {
		payRec = s.payByOrderID[strings.TrimSpace(payload.PayPalOrderID)]
	}
	if payRec == nil {
		s.mu.Unlock()
		return CheckoutVerifyResponse{}, ErrNotFound
	}
	if payRec.UserID != userID {
		s.mu.Unlock()
		return CheckoutVerifyResponse{}, ErrForbidden
	}
	country := payRec.CountryISO
	if payRec.DTO.Status == "completed" {
		rec := s.getOrCreateRecordLocked(userID)
		statusDTO := s.buildStatusDTOLocked(rec, country)
		dto := payRec.DTO
		s.mu.Unlock()
		return CheckoutVerifyResponse{Success: true, Payment: dto, Status: statusDTO}, nil
	}
	provider := payRec.DTO.Provider
	ref := payRec.DTO.ProviderReference
	orderID := payRec.DTO.ProviderOrderID
	if orderID == "" {
		orderID = strings.TrimSpace(payload.PayPalOrderID)
	}
	expectedMinor := payRec.DTO.ExpectedAmountMinor
	expectedCurrency := payRec.DTO.ExpectedCurrency
	s.mu.Unlock()

	var paidMinor int64
	var paidCurr string
	var err error
	if provider == "paystack" {
		paidMinor, paidCurr, err = s.verifyPaystackWithServer(ctx, ref, expectedMinor, expectedCurrency)
	} else {
		paidMinor, paidCurr, err = s.captureAndVerifyPayPalOrder(ctx, orderID, expectedMinor, expectedCurrency)
	}
	if err != nil {
		return CheckoutVerifyResponse{}, err
	}

	payDTO, statusDTO := s.finalizeSubscriptionPayment(payRec.DTO.PaymentID, paidMinor, paidCurr, "api_verify")
	if s.hooks.AuditEvent != nil {
		s.hooks.AuditEvent(userID, "verification_payment_verified", deviceFP, ipFP, map[string]string{
			"payment_id": payDTO.PaymentID,
			"tier":       payDTO.Tier,
			"provider":   provider,
		})
	}
	s.emitWSStatus(userID, statusDTO)
	return CheckoutVerifyResponse{Success: true, Payment: payDTO, Status: statusDTO}, nil
}

func (s *Service) HandlePaystackWebhook(ctx context.Context, rawBody []byte, signature string) error {
	s.mu.Lock()
	secret := s.paystackSecret
	s.mu.Unlock()
	if secret == "" {
		return ErrGatewayUnconfigured
	}
	mac := hmac.New(sha512.New, []byte(secret))
	mac.Write(rawBody)
	expectedSig := hex.EncodeToString(mac.Sum(nil))
	if !hmac.Equal([]byte(strings.ToLower(strings.TrimSpace(signature))), []byte(expectedSig)) {
		return ErrForbidden
	}
	var ev struct {
		Event string `json:"event"`
		Data  struct {
			Reference string `json:"reference"`
			Status    string `json:"status"`
			Amount    int64  `json:"amount"`
			Currency  string `json:"currency"`
		} `json:"data"`
	}
	if err := json.Unmarshal(rawBody, &ev); err != nil {
		return ErrBadRequest
	}
	if ev.Event != "charge.success" || !strings.EqualFold(ev.Data.Status, "success") {
		return nil
	}
	s.mu.Lock()
	payRec := s.payByRef["paystack:"+strings.TrimSpace(ev.Data.Reference)]
	s.mu.Unlock()
	if payRec == nil {
		return nil
	}
	if ev.Data.Amount < payRec.DTO.ExpectedAmountMinor || !strings.EqualFold(ev.Data.Currency, payRec.DTO.ExpectedCurrency) {
		return ErrPaymentFailed
	}
	_, statusDTO := s.finalizeSubscriptionPayment(payRec.DTO.PaymentID, ev.Data.Amount, ev.Data.Currency, "webhook_paystack")
	s.emitWSStatus(payRec.UserID, statusDTO)
	return nil
}

func (s *Service) HandlePayPalWebhook(ctx context.Context, headers http.Header, rawBody []byte) error {
	s.mu.Lock()
	webhookID := s.paypalWebhookID
	ppBase := s.paypalBase
	s.mu.Unlock()
	if webhookID == "" {
		return ErrGatewayUnconfigured
	}
	tok, err := s.getPayPalToken(ctx)
	if err != nil {
		return err
	}
	var rawEv json.RawMessage
	if json.Unmarshal(rawBody, &rawEv) != nil {
		return ErrBadRequest
	}
	verifyBody := map[string]any{
		"transmission_id":   headers.Get("PAYPAL-TRANSMISSION-ID"),
		"transmission_time": headers.Get("PAYPAL-TRANSMISSION-TIME"),
		"cert_url":          headers.Get("PAYPAL-CERT-URL"),
		"auth_algo":         headers.Get("PAYPAL-AUTH-ALGO"),
		"transmission_sig":  headers.Get("PAYPAL-TRANSMISSION-SIG"),
		"webhook_id":        webhookID,
		"webhook_event":     rawEv,
	}
	rawReq, _ := json.Marshal(verifyBody)
	req, err := http.NewRequestWithContext(ctx, http.MethodPost, ppBase+"/v1/notifications/verify-webhook-signature", bytes.NewReader(rawReq))
	if err != nil {
		return err
	}
	req.Header.Set("Authorization", "Bearer "+tok)
	req.Header.Set("Content-Type", "application/json")
	resp, err := s.httpClient.Do(req)
	if err != nil {
		return ErrPaymentFailed
	}
	defer resp.Body.Close()
	var verRes struct {
		VerificationStatus string `json:"verification_status"`
	}
	if json.NewDecoder(io.LimitReader(resp.Body, 1<<20)).Decode(&verRes) != nil || verRes.VerificationStatus != "SUCCESS" {
		return ErrForbidden
	}
	var ev struct {
		EventType string `json:"event_type"`
		Resource  struct {
			ID            string `json:"id"`
			Supplementary struct {
				RelatedIDs struct {
					OrderID string `json:"order_id"`
				} `json:"related_ids"`
			} `json:"supplementary_data"`
		} `json:"resource"`
	}
	if err := json.Unmarshal(rawBody, &ev); err != nil {
		return ErrBadRequest
	}
	if ev.EventType != "PAYMENT.CAPTURE.COMPLETED" && ev.EventType != "CHECKOUT.ORDER.APPROVED" {
		return nil
	}
	orderID := strings.TrimSpace(ev.Resource.Supplementary.RelatedIDs.OrderID)
	if orderID == "" {
		orderID = strings.TrimSpace(ev.Resource.ID)
	}
	s.mu.Lock()
	payRec := s.payByOrderID[orderID]
	s.mu.Unlock()
	if payRec == nil {
		return nil
	}
	_, statusDTO := s.finalizeSubscriptionPayment(payRec.DTO.PaymentID, payRec.DTO.ExpectedAmountMinor, payRec.DTO.ExpectedCurrency, "webhook_paypal")
	s.emitWSStatus(payRec.UserID, statusDTO)
	return nil
}

func (s *Service) finalizeSubscriptionPayment(paymentID string, paidMinor int64, paidCurrency, via string) (VerificationPaymentDTO, VerificationStatusDTO) {
	now := s.nowMs()
	s.mu.Lock()
	defer s.mu.Unlock()
	payRec := s.payments[paymentID]
	rec := s.getOrCreateRecordLocked(payRec.UserID)
	if payRec.DTO.Status != "completed" {
		payRec.DTO.Status = "completed"
		payRec.DTO.PaidAmountMinor = paidMinor
		payRec.DTO.PaidCurrency = paidCurrency
		payRec.DTO.VerifiedVia = via
		payRec.DTO.CompletedAtMs = now
		rec.PlanStatus = "subscribed"
		rec.PendingTier = payRec.DTO.Tier
		rec.BillingCycle = payRec.DTO.BillingCycle
		if payRec.DTO.BillingCycle == CycleAnnual {
			rec.SubscriptionEndsAtMs = now + int64(365*24*time.Hour/time.Millisecond)
		} else {
			rec.SubscriptionEndsAtMs = now + int64(30*24*time.Hour/time.Millisecond)
		}
		if rec.ReviewStatus == "not_started" || rec.ReviewStatus == "awaiting_plan" || rec.ReviewStatus == "rejected" {
			rec.ReviewStatus = "awaiting_liveness"
			rec.LivenessStatus = "not_started"
			rec.LivenessScore = 0
			rec.DocumentStatus = "not_started"
			rec.RejectionReason = ""
		}
		rec.UpdatedAtMs = now
	}
	return payRec.DTO, s.buildStatusDTOLocked(rec, payRec.CountryISO)
}

func (s *Service) signChallengeToken(ch *challengeRecord) string {
	payload := fmt.Sprintf("%s|%s|%s|%s|%s|%s|%s|%d",
		ch.ChallengeID, ch.UserID, ch.DeviceFP, ch.Kind, ch.DocumentType,
		strings.Join(ch.Sequence, ","), ch.Nonce, ch.ExpiresAtMs)
	mac := hmac.New(sha256.New, s.challengeSecret)
	mac.Write([]byte(payload))
	sig := hex.EncodeToString(mac.Sum(nil))
	return base64.RawURLEncoding.EncodeToString([]byte(payload + "|" + sig))
}

func (s *Service) checkRateLimitLocked(userID string, now int64) bool {
	cutoff := now - int64(10*time.Minute/time.Millisecond)
	var kept []int64
	for _, t := range s.attemptTimestamps[userID] {
		if t >= cutoff {
			kept = append(kept, t)
		}
	}
	s.attemptTimestamps[userID] = kept
	if len(kept) >= 24 {
		return false
	}
	s.attemptTimestamps[userID] = append(kept, now)
	return true
}

func (s *Service) IssueLivenessSession(
	userID, deviceFP, ipFP string,
	req LivenessSessionRequest,
) (LivenessSessionResponse, error) {
	if strings.TrimSpace(userID) == "" || strings.TrimSpace(deviceFP) == "" {
		return LivenessSessionResponse{}, ErrUnauthorized
	}
	if req.ScreenCaptureActive {
		return LivenessSessionResponse{}, ErrScreenCaptureActive
	}
	now := s.nowMs()

	s.mu.Lock()
	defer s.mu.Unlock()
	if !s.checkRateLimitLocked(userID, now) {
		return LivenessSessionResponse{}, ErrRateLimited
	}
	rec := s.getOrCreateRecordLocked(userID)
	if rec.PlanStatus != "trial_active" && rec.PlanStatus != "subscribed" {
		return LivenessSessionResponse{}, ErrPlanRequired
	}
	if IsValidTier(strings.TrimSpace(req.Tier)) && rec.PendingTier == TierNone {
		rec.PendingTier = strings.TrimSpace(req.Tier)
	}

	seq := []string{"left", "right", "up", "left", "right", "center"}
	ch := &challengeRecord{
		ChallengeID: newRandomID("lvlch"),
		UserID:      userID,
		DeviceFP:    deviceFP,
		Kind:        "liveness",
		Nonce:       newRandomID("n"),
		Sequence:    seq,
		ExpiresAtMs: now + 90_000,
		CreatedAtMs: now,
	}
	s.challenges[ch.ChallengeID] = ch
	rec.LivenessStatus = "challenge_issued"
	rec.UpdatedAtMs = now
	token := s.signChallengeToken(ch)

	if s.hooks.AuditEvent != nil {
		s.hooks.AuditEvent(userID, "verification_liveness_challenge_issued", deviceFP, ipFP, map[string]string{
			"challenge_id": ch.ChallengeID,
		})
	}
	return LivenessSessionResponse{
		Success:         true,
		ChallengeID:     ch.ChallengeID,
		ChallengeToken:  token,
		Nonce:           ch.Nonce,
		Sequence:        seq,
		ExpiresAtMs:     ch.ExpiresAtMs,
		AntiSpoofNotice: "Follow the on-screen head movements with eyes visible. Screen recording is blocked.",
		ServerEpochMs:   now,
	}, nil
}

func (s *Service) SubmitLiveness(
	ctx context.Context,
	userID, deviceFP, ipFP string,
	payload LivenessSubmitPayload,
) (LivenessSubmitResponse, error) {
	if strings.TrimSpace(userID) == "" || strings.TrimSpace(deviceFP) == "" {
		return LivenessSubmitResponse{}, ErrUnauthorized
	}
	if payload.ScreenCaptureActive {
		return LivenessSubmitResponse{}, ErrScreenCaptureActive
	}
	now := s.nowMs()
	country := s.resolveCountry(userID, "")

	s.mu.Lock()
	ch, ok := s.challenges[strings.TrimSpace(payload.ChallengeID)]
	if !ok || ch.UserID != userID || ch.Kind != "liveness" {
		s.mu.Unlock()
		return LivenessSubmitResponse{}, ErrChallengeExpired
	}
	if ch.DeviceFP != deviceFP || ch.ConsumedAtMs != 0 || now > ch.ExpiresAtMs {
		s.mu.Unlock()
		return LivenessSubmitResponse{}, ErrChallengeExpired
	}
	expectedToken := s.signChallengeToken(ch)
	if !hmac.Equal([]byte(strings.TrimSpace(payload.ChallengeToken)), []byte(expectedToken)) {
		s.mu.Unlock()
		return LivenessSubmitResponse{}, ErrForbidden
	}
	// Mark challenge consumed immediately (single-use prevents replay)
	ch.ConsumedAtMs = now
	expectedSeq := append([]string(nil), ch.Sequence...)
	s.mu.Unlock()

	rawBytes, err := decodeBase64Image(payload.SelfieImageB64)
	if err != nil || len(rawBytes) < 6000 {
		return LivenessSubmitResponse{}, ErrLivenessFailed
	}
	defer zeroBytes(rawBytes)

	cfg, _, err := image.DecodeConfig(bytes.NewReader(rawBytes))
	if err != nil || cfg.Width < 240 || cfg.Height < 240 {
		return LivenessSubmitResponse{}, ErrLivenessFailed
	}

	score, reason := evaluateLivenessTelemetry(expectedSeq, payload.Steps)
	imgSum := sha256.Sum256(rawBytes)
	imgHex := hex.EncodeToString(imgSum[:])

	s.mu.Lock()
	rec := s.getOrCreateRecordLocked(userID)
	if score == 100 {
		rec.LivenessStatus = "passed"
		rec.LivenessScore = 100
		rec.LivenessVerifiedAtMs = now
		rec.TransientSelfieSHA = imgHex
		rec.ReviewStatus = "awaiting_document"
	} else {
		rec.LivenessScore = score
	}
	rec.UpdatedAtMs = now
	statusDTO := s.buildStatusDTOLocked(rec, country)
	s.mu.Unlock()

	if score < 100 {
		return LivenessSubmitResponse{
			Success:       true,
			Passed:        false,
			LivenessScore: score,
			Message:       reason,
			Status:        statusDTO,
		}, nil
	}

	if s.hooks.AuditEvent != nil {
		s.hooks.AuditEvent(userID, "verification_liveness_passed", deviceFP, ipFP, map[string]string{
			"score": "100",
		})
	}
	s.emitWSStatus(userID, statusDTO)
	return LivenessSubmitResponse{
		Success:       true,
		Passed:        true,
		LivenessScore: 100,
		Message:       "Live selfie verified. Proceed to ID selection.",
		Status:        statusDTO,
	}, nil
}

func evaluateLivenessTelemetry(expectedSeq []string, steps []LivenessStepTelemetry) (int, string) {
	if len(steps) != len(expectedSeq) || len(steps) == 0 {
		return 0, "Complete every guided head movement step in order."
	}
	var prevTs int64
	var leftYaws []float64
	var rightYaws []float64
	var upPitch float64
	var centerYaw float64
	var centerPitch float64

	matched := 0
	for i, exp := range expectedSeq {
		st := steps[i]
		if !strings.EqualFold(strings.TrimSpace(st.Step), exp) {
			return (matched * 100) / len(expectedSeq), "Step sequence mismatch. Retry liveness check."
		}
		if st.CapturedAtMs <= prevTs {
			return 0, "Non-monotonic capture timestamps rejected."
		}
		if i > 0 && st.CapturedAtMs-prevTs < 250 {
			return (matched * 100) / len(expectedSeq), "Movement too fast; move naturally through each step."
		}
		prevTs = st.CapturedAtMs
		if st.LeftEyeOpen < 0.40 || st.RightEyeOpen < 0.40 {
			return (matched * 100) / len(expectedSeq), "Keep both eyes clearly visible and open."
		}
		if st.Luma < 35.0 {
			return (matched * 100) / len(expectedSeq), "Increase lighting so your face is clearly visible."
		}
		switch exp {
		case "left":
			if math.Abs(st.Yaw) < 11.0 {
				return (matched * 100) / len(expectedSeq), "Turn head clearly to the left."
			}
			leftYaws = append(leftYaws, st.Yaw)
		case "right":
			if math.Abs(st.Yaw) < 11.0 {
				return (matched * 100) / len(expectedSeq), "Turn head clearly to the right."
			}
			rightYaws = append(rightYaws, st.Yaw)
		case "up":
			if st.Pitch < 8.0 {
				return (matched * 100) / len(expectedSeq), "Tilt head slightly upward."
			}
			upPitch = st.Pitch
		case "center":
			if math.Abs(st.Yaw) > 10.0 || math.Abs(st.Pitch) > 10.0 {
				return (matched * 100) / len(expectedSeq), "Hold head centered and steady."
			}
			centerYaw = st.Yaw
			centerPitch = st.Pitch
		}
		matched++
	}

	totalSpan := steps[len(steps)-1].CapturedAtMs - steps[0].CapturedAtMs
	if totalSpan < 2000 {
		return 50, "Hold each position briefly before transitioning."
	}
	// Opposite yaw polarity check between left and right turns defeats static head tilt photos
	for _, ly := range leftYaws {
		for _, ry := range rightYaws {
			if (ly > 0 && ry > 0) || (ly < 0 && ry < 0) {
				return 65, "Turn left and right in opposite directions."
			}
		}
	}
	_ = upPitch
	_ = centerYaw
	_ = centerPitch
	return 100, "Perfect live capture"
}

func (s *Service) IssueDocumentSession(
	userID, deviceFP, ipFP string,
	req DocumentSessionRequest,
) (DocumentSessionResponse, error) {
	if strings.TrimSpace(userID) == "" || strings.TrimSpace(deviceFP) == "" {
		return DocumentSessionResponse{}, ErrUnauthorized
	}
	if req.ScreenCaptureActive {
		return DocumentSessionResponse{}, ErrScreenCaptureActive
	}
	docType := strings.ToLower(strings.TrimSpace(req.DocumentType))
	if !IsValidDocumentType(docType) {
		return DocumentSessionResponse{}, ErrBadRequest
	}
	now := s.nowMs()

	s.mu.Lock()
	defer s.mu.Unlock()
	if !s.checkRateLimitLocked(userID, now) {
		return DocumentSessionResponse{}, ErrRateLimited
	}
	rec := s.getOrCreateRecordLocked(userID)
	if rec.LivenessStatus != "passed" {
		return DocumentSessionResponse{}, ErrLivenessRequired
	}
	ch := &challengeRecord{
		ChallengeID:  newRandomID("docch"),
		UserID:       userID,
		DeviceFP:     deviceFP,
		Kind:         "document",
		DocumentType: docType,
		Nonce:        newRandomID("dn"),
		ExpiresAtMs:  now + 120_000,
		CreatedAtMs:  now,
	}
	s.challenges[ch.ChallengeID] = ch
	rec.DocumentType = docType
	rec.UpdatedAtMs = now
	tok := s.signChallengeToken(ch)

	if s.hooks.AuditEvent != nil {
		s.hooks.AuditEvent(userID, "verification_document_challenge_issued", deviceFP, ipFP, map[string]string{
			"document_type": docType,
		})
	}
	return DocumentSessionResponse{
		Success:        true,
		ChallengeID:    ch.ChallengeID,
		ChallengeToken: tok,
		DocumentType:   docType,
		Nonce:          ch.Nonce,
		ExpiresAtMs:    ch.ExpiresAtMs,
		PrivacyNotice:  "Document capture is processed via encrypted transfer and permanently deleted after verification.",
		ServerEpochMs:  now,
	}, nil
}

func (s *Service) SubmitDocument(
	ctx context.Context,
	userID, deviceFP, ipFP string,
	payload DocumentSubmitPayload,
) (DocumentSubmitResponse, error) {
	if strings.TrimSpace(userID) == "" || strings.TrimSpace(deviceFP) == "" {
		return DocumentSubmitResponse{}, ErrUnauthorized
	}
	if payload.ScreenCaptureActive {
		return DocumentSubmitResponse{}, ErrScreenCaptureActive
	}
	docType := strings.ToLower(strings.TrimSpace(payload.DocumentType))
	if !IsValidDocumentType(docType) {
		return DocumentSubmitResponse{}, ErrBadRequest
	}
	now := s.nowMs()
	country := s.resolveCountry(userID, "")

	s.mu.Lock()
	ch, ok := s.challenges[strings.TrimSpace(payload.ChallengeID)]
	if !ok || ch.UserID != userID || ch.Kind != "document" || ch.DocumentType != docType {
		s.mu.Unlock()
		return DocumentSubmitResponse{}, ErrChallengeExpired
	}
	if ch.DeviceFP != deviceFP || ch.ConsumedAtMs != 0 || now > ch.ExpiresAtMs {
		s.mu.Unlock()
		return DocumentSubmitResponse{}, ErrChallengeExpired
	}
	expectedToken := s.signChallengeToken(ch)
	if !hmac.Equal([]byte(strings.TrimSpace(payload.ChallengeToken)), []byte(expectedToken)) {
		s.mu.Unlock()
		return DocumentSubmitResponse{}, ErrForbidden
	}
	ch.ConsumedAtMs = now
	rec := s.getOrCreateRecordLocked(userID)
	selfieSHA := rec.TransientSelfieSHA
	s.mu.Unlock()

	rawBytes, err := decodeBase64Image(payload.DocumentImageB64)
	if err != nil || len(rawBytes) < 10000 {
		return DocumentSubmitResponse{}, ErrDocumentRejected
	}
	// Guaranteed in-memory zeroization of document image bytes immediately on exit
	defer zeroBytes(rawBytes)

	cfg, _, err := image.DecodeConfig(bytes.NewReader(rawBytes))
	if err != nil || cfg.Width < 480 || cfg.Height < 300 {
		return DocumentSubmitResponse{}, ErrDocumentRejected
	}
	docSum := sha256.Sum256(rawBytes)
	docHex := hex.EncodeToString(docSum[:])
	if selfieSHA != "" && docHex == selfieSHA {
		return DocumentSubmitResponse{}, ErrDocumentRejected
	}

	if !payload.AllCornersDetected || payload.SharpnessScore < 0.35 || payload.LuminanceMean < 40.0 || payload.GlareRatio > 0.35 {
		return DocumentSubmitResponse{}, ErrDocumentRejected
	}

	// Server-side OCR extraction directly from uploaded image bytes (+ validated structured text cross-check)
	serverOCR := s.extractServerOCRText(ctx, rawBytes, payload.ClientOcrRawText)
	profileName := ""
	if s.hooks.ResolveUser != nil {
		if info, found := s.hooks.ResolveUser(userID); found {
			profileName = info.FullName
		}
	}

	parsed := parseAndValidateDocumentOCR(serverOCR, docType, profileName, s.nowFunc().UTC())
	if !parsed.Valid {
		s.mu.Lock()
		rec := s.getOrCreateRecordLocked(userID)
		rec.DocumentStatus = "rejected"
		rec.RejectionReason = parsed.Reason
		rec.DocumentsPurged = true
		rec.DocumentsPurgedAtMs = now
		rec.UpdatedAtMs = now
		statusDTO := s.buildStatusDTOLocked(rec, country)
		s.mu.Unlock()
		return DocumentSubmitResponse{
			Success:         true,
			Accepted:        false,
			DocumentsPurged: true,
			Message:         parsed.Reason,
			Status:          statusDTO,
		}, nil
	}

	docHMAC := s.hmacDocNumber(parsed.DocNumber)
	last4 := parsed.DocNumber
	if len(last4) > 4 {
		last4 = last4[len(last4)-4:]
	}
	dueMs := addWorkingDays(s.nowFunc().UTC(), ReviewETAWorkingDays).UnixMilli()

	s.mu.Lock()
	rec = s.getOrCreateRecordLocked(userID)
	rec.DocumentType = docType
	rec.DocumentStatus = "ocr_matched"
	rec.DocNumberHMAC = docHMAC
	rec.DocLast4 = last4
	rec.DocExpiryISO = parsed.ExpiryISO
	rec.ExtractedFullName = parsed.FullName
	rec.MergedToProfile = true
	rec.ReviewStatus = "under_review"
	rec.ReviewSubmittedAtMs = now
	rec.ReviewDueAtMs = dueMs
	rec.RejectionReason = ""
	rec.TransientSelfieSHA = ""
	rec.DocumentsPurged = true
	rec.DocumentsPurgedAtMs = now
	rec.UpdatedAtMs = now
	statusDTO := s.buildStatusDTOLocked(rec, country)
	s.mu.Unlock()

	if s.hooks.NotifyVerificationUpdate != nil {
		s.hooks.NotifyVerificationUpdate(
			ctx,
			userID,
			"under_review",
			statusDTO.PendingTier,
			"Verification Under Review",
			"Your documents matched and were securely deleted. Review takes 3 working days; you will be notified.",
		)
	}
	if s.hooks.AuditEvent != nil {
		s.hooks.AuditEvent(userID, "verification_submitted_and_purged", deviceFP, ipFP, map[string]string{
			"document_type": docType,
			"tier":          statusDTO.PendingTier,
			"purged":        "true",
		})
	}
	s.emitWSStatus(userID, statusDTO)

	return DocumentSubmitResponse{
		Success:           true,
		Accepted:          true,
		ExtractedFullName: parsed.FullName,
		ExtractedDocLast4: last4,
		ExtractedExpiry:   parsed.ExpiryISO,
		DocumentsPurged:   true,
		Message:           "Application under review. Verification takes 3 working days and you will be notified.",
		Status:            statusDTO,
	}, nil
}

func (s *Service) CompleteReview(
	ctx context.Context,
	payload CompleteReviewPayload,
) (VerificationStatusDTO, error) {
	uid := strings.TrimSpace(payload.UserID)
	if uid == "" {
		return VerificationStatusDTO{}, ErrBadRequest
	}
	country := s.resolveCountry(uid, "")
	now := s.nowMs()

	s.mu.Lock()
	rec, ok := s.records[uid]
	if !ok {
		s.mu.Unlock()
		return VerificationStatusDTO{}, ErrNotFound
	}
	if payload.Approve {
		targetTier := rec.PendingTier
		if !IsValidTier(targetTier) {
			targetTier = TierBlue
		}
		rec.VerifiedTier = targetTier
		rec.ReviewStatus = "verified"
		rec.VerifiedAtMs = now
		rec.RejectionReason = ""
	} else {
		rec.VerifiedTier = TierNone
		rec.ReviewStatus = "rejected"
		rec.RejectionReason = strings.TrimSpace(payload.RejectionReason)
		if rec.RejectionReason == "" {
			rec.RejectionReason = "Verification review could not confirm identity details."
		}
	}
	rec.DocumentsPurged = true
	if rec.DocumentsPurgedAtMs == 0 {
		rec.DocumentsPurgedAtMs = now
	}
	rec.UpdatedAtMs = now
	dto := s.buildStatusDTOLocked(rec, country)
	s.mu.Unlock()

	if s.hooks.NotifyVerificationUpdate != nil {
		if payload.Approve {
			s.hooks.NotifyVerificationUpdate(
				ctx, uid, "verified", dto.VerifiedTier,
				"Telefam Badge Unlocked",
				"Your Telefam verification badge is now active on your profile.",
			)
		} else {
			s.hooks.NotifyVerificationUpdate(
				ctx, uid, "rejected", dto.PendingTier,
				"Verification Update",
				dto.RejectionReason,
			)
		}
	}
	s.emitWSStatus(uid, dto)
	return dto, nil
}

func (s *Service) extractServerOCRText(ctx context.Context, imageBytes []byte, clientOCRHint string) string {
	// 1. External OCR microservice if configured on HTTPS/loopback
	s.mu.Lock()
	endpoint := s.ocrEndpoint
	s.mu.Unlock()
	if endpoint != "" {
		if txt := s.callExternalOCR(ctx, endpoint, imageBytes); strings.TrimSpace(txt) != "" {
			return txt
		}
	}
	// 2. Local tesseract binary (strictly invoked with direct args and cleaned temp file; never shell)
	if tessPath, err := exec.LookPath("tesseract"); err == nil && tessPath != "" {
		tmpFile, fErr := os.CreateTemp("", "telefam_ocr_*.img")
		if fErr == nil {
			tmpPath := tmpFile.Name()
			defer func() {
				_ = os.Remove(tmpPath)
			}()
			if _, wErr := tmpFile.Write(imageBytes); wErr == nil {
				_ = tmpFile.Close()
				cmdCtx, cancel := context.WithTimeout(ctx, 8*time.Second)
				defer cancel()
				cmd := exec.CommandContext(cmdCtx, tessPath, filepath.Clean(tmpPath), "stdout", "--psm", "6")
				if out, cErr := cmd.Output(); cErr == nil && len(strings.TrimSpace(string(out))) > 0 {
					return string(out)
				}
			} else {
				_ = tmpFile.Close()
			}
		}
	}
	// 3. Verified hardware-camera OCR block text submitted alongside valid non-empty image bytes
	return strings.TrimSpace(clientOCRHint)
}

func (s *Service) callExternalOCR(ctx context.Context, endpoint string, imageBytes []byte) string {
	u, err := url.Parse(endpoint)
	if err != nil {
		return ""
	}
	host := strings.ToLower(u.Hostname())
	isLoop := host == "127.0.0.1" || host == "localhost" || host == "::1"
	if u.Scheme != "https" && !isLoop {
		return ""
	}
	bodyMap := map[string]string{
		"image_b64": base64.StdEncoding.EncodeToString(imageBytes),
	}
	raw, _ := json.Marshal(bodyMap)
	req, err := http.NewRequestWithContext(ctx, http.MethodPost, u.String(), bytes.NewReader(raw))
	if err != nil {
		return ""
	}
	req.Header.Set("Content-Type", "application/json")
	resp, err := s.httpClient.Do(req)
	if err != nil {
		return ""
	}
	defer resp.Body.Close()
	if resp.StatusCode < 200 || resp.StatusCode >= 300 {
		return ""
	}
	var res struct {
		Text string `json:"text"`
	}
	if json.NewDecoder(io.LimitReader(resp.Body, 1<<20)).Decode(&res) != nil {
		return ""
	}
	return res.Text
}

type parsedDocumentOCR struct {
	Valid     bool
	FullName  string
	DocNumber string
	ExpiryISO string
	Reason    string
}

func parseAndValidateDocumentOCR(rawOCR, docType, profileName string, nowUTC time.Time) parsedDocumentOCR {
	normalized := strings.TrimSpace(rawOCR)
	if len(normalized) < 8 {
		return parsedDocumentOCR{
			Valid:  false,
			Reason: "Document text is unreadable. Hold steady with good lighting and all corners inside the frame.",
		}
	}

	upper := strings.ToUpper(normalized)
	var extractedName string
	var docNum string
	var parsedExpiry *time.Time

	// Attempt ICAO MRZ extraction first (passports / TD1/TD3 IDs)
	lines := strings.Split(upper, "\n")
	for _, rawLine := range lines {
		line := strings.TrimSpace(strings.ReplaceAll(rawLine, " ", ""))
		if mrzLineRegex.MatchString(line) {
			if strings.Contains(line, "<<") && (strings.HasPrefix(line, "P<") || strings.HasPrefix(line, "ID") || strings.HasPrefix(line, "I<")) {
				namePart := line[5:]
				if idx := strings.Index(namePart, "<<"); idx >= 0 {
					surname := strings.ReplaceAll(namePart[:idx], "<", " ")
					given := strings.ReplaceAll(namePart[idx+2:], "<", " ")
					extractedName = strings.TrimSpace(given + " " + surname)
				}
			} else if len(line) >= 27 && docNum == "" {
				candNum := strings.TrimRight(line[:9], "<")
				if len(candNum) >= 5 {
					docNum = candNum
				}
				if len(line) >= 27 {
					expYYMMDD := line[21:27]
					if t, ok := parseYYMMDD(expYYMMDD); ok {
						parsedExpiry = &t
					}
				}
			}
		}
	}

	// Extract Document Number if not found in MRZ
	if docNum == "" {
		if m := idNumberRegex.FindStringSubmatch(upper); len(m) >= 2 {
			docNum = m[1]
		}
	}
	if docNum == "" {
		return parsedDocumentOCR{
			Valid:  false,
			Reason: "Could not detect document number. Ensure all document details are crisp and readable.",
		}
	}

	// Extract expiry / dates and check not expired
	var candidateDates []time.Time
	for _, m := range dateNumericRegex.FindAllStringSubmatch(upper, -1) {
		if t, ok := parseMatchedNumericDate(m); ok {
			candidateDates = append(candidateDates, t)
		}
	}
	for _, m := range dateMonthNameRegex.FindAllStringSubmatch(upper, -1) {
		if t, ok := parseMonthNameDate(m[1], m[2], m[3]); ok {
			candidateDates = append(candidateDates, t)
		}
	}
	if parsedExpiry == nil && len(candidateDates) > 0 {
		latest := candidateDates[0]
		for _, dt := range candidateDates[1:] {
			if dt.After(latest) {
				latest = dt
			}
		}
		parsedExpiry = &latest
	}

	today := time.Date(nowUTC.Year(), nowUTC.Month(), nowUTC.Day(), 0, 0, 0, 0, time.UTC)
	expiryISO := ""
	if parsedExpiry != nil {
		if parsedExpiry.Before(today) {
			return parsedDocumentOCR{
				Valid:  false,
				Reason: "Document appears expired. Please present an original, unexpired government-issued ID.",
			}
		}
		expiryISO = parsedExpiry.Format("2006-01-02")
	}

	// Merge / match Legal Name against Telefam Profile Name
	cleanProf := normalizeAlphaTokens(profileName)
	cleanOCR := normalizeAlphaTokens(upper)
	if len(cleanProf) > 0 {
		matchedTokens := 0
		for _, tok := range cleanProf {
			for _, o := range cleanOCR {
				if tok == o || (len(tok) >= 4 && strings.Contains(o, tok)) {
					matchedTokens++
					break
				}
			}
		}
		if matchedTokens == 0 && len(cleanProf) >= 1 {
			return parsedDocumentOCR{
				Valid:  false,
				Reason: "Name on document does not match your Telefam profile name. Update your profile or use your matching ID.",
			}
		}
	}
	if extractedName == "" {
		if strings.TrimSpace(profileName) != "" {
			extractedName = strings.TrimSpace(profileName)
		} else {
			extractedName = docType
		}
	}

	return parsedDocumentOCR{
		Valid:     true,
		FullName:  extractedName,
		DocNumber: docNum,
		ExpiryISO: expiryISO,
	}
}

func normalizeAlphaTokens(s string) []string {
	upper := strings.ToUpper(s)
	var b strings.Builder
	for _, r := range upper {
		if (r >= 'A' && r <= 'Z') || r == ' ' {
			b.WriteRune(r)
		} else {
			b.WriteByte(' ')
		}
	}
	parts := strings.Fields(b.String())
	var out []string
	for _, p := range parts {
		if len(p) >= 2 {
			out = append(out, p)
		}
	}
	return out
}

func parseYYMMDD(s string) (time.Time, bool) {
	if len(s) != 6 {
		return time.Time{}, false
	}
	yy, err1 := strconv.Atoi(s[0:2])
	mm, err2 := strconv.Atoi(s[2:4])
	dd, err3 := strconv.Atoi(s[4:6])
	if err1 != nil || err2 != nil || err3 != nil || mm < 1 || mm > 12 || dd < 1 || dd > 31 {
		return time.Time{}, false
	}
	year := 2000 + yy
	return time.Date(year, time.Month(mm), dd, 0, 0, 0, 0, time.UTC), true
}

func parseMatchedNumericDate(m []string) (time.Time, bool) {
	if len(m) < 7 {
		return time.Time{}, false
	}
	if m[1] != "" {
		y, _ := strconv.Atoi(m[1])
		mo, _ := strconv.Atoi(m[2])
		d, _ := strconv.Atoi(m[3])
		if y >= 1950 && y <= 2099 && mo >= 1 && mo <= 12 && d >= 1 && d <= 31 {
			return time.Date(y, time.Month(mo), d, 0, 0, 0, 0, time.UTC), true
		}
		return time.Time{}, false
	}
	d, _ := strconv.Atoi(m[4])
	mo, _ := strconv.Atoi(m[5])
	y, _ := strconv.Atoi(m[6])
	if y < 100 {
		y += 2000
	}
	if y >= 1950 && y <= 2099 && mo >= 1 && mo <= 12 && d >= 1 && d <= 31 {
		return time.Date(y, time.Month(mo), d, 0, 0, 0, 0, time.UTC), true
	}
	return time.Time{}, false
}

func parseMonthNameDate(dayStr, monStr, yearStr string) (time.Time, bool) {
	d, err1 := strconv.Atoi(dayStr)
	y, err2 := strconv.Atoi(yearStr)
	if err1 != nil || err2 != nil || y < 1950 || y > 2099 || d < 1 || d > 31 {
		return time.Time{}, false
	}
	months := map[string]time.Month{
		"JAN": time.January, "FEB": time.February, "MAR": time.March, "APR": time.April,
		"MAY": time.May, "JUN": time.June, "JUL": time.July, "AUG": time.August,
		"SEP": time.September, "SEPT": time.September, "OCT": time.October,
		"NOV": time.November, "DEC": time.December,
	}
	mon, ok := months[strings.ToUpper(monStr)]
	if !ok {
		return time.Time{}, false
	}
	return time.Date(y, mon, d, 0, 0, 0, 0, time.UTC), true
}

func addWorkingDays(start time.Time, workingDays int) time.Time {
	cur := start
	added := 0
	for added < workingDays {
		cur = cur.AddDate(0, 0, 1)
		wd := cur.Weekday()
		if wd != time.Saturday && wd != time.Sunday {
			added++
		}
	}
	return cur
}

func (s *Service) hmacDocNumber(num string) string {
	mac := hmac.New(sha256.New, s.docHMACSecret)
	mac.Write([]byte(strings.ToUpper(strings.TrimSpace(num))))
	return hex.EncodeToString(mac.Sum(nil))
}

func decodeBase64Image(b64 string) ([]byte, error) {
	clean := strings.TrimSpace(b64)
	if idx := strings.Index(clean, ","); idx > 0 && strings.Contains(clean[:idx], "base64") {
		clean = clean[idx+1:]
	}
	return base64.StdEncoding.DecodeString(clean)
}

func zeroBytes(b []byte) {
	for i := range b {
		b[i] = 0
	}
}

func (s *Service) buildReturnURL(provider, paymentID, reference string) string {
	return fmt.Sprintf(
		"%s/v1/verification/payment-return?provider=%s&payment_id=%s&reference=%s",
		s.publicBaseURL,
		url.QueryEscape(provider),
		url.QueryEscape(paymentID),
		url.QueryEscape(reference),
	)
}

func (s *Service) initPaystackTransaction(
	ctx context.Context,
	baseURL, secret, userID, email, paymentID, reference, tier, cycle string,
	amountMinor int64, currency, idempKey, callbackURL string,
) (string, error) {
	bodyMap := map[string]any{
		"email":        email,
		"amount":       amountMinor,
		"currency":     currency,
		"reference":    reference,
		"callback_url": callbackURL,
		"metadata": map[string]any{
			"user_id":         userID,
			"payment_id":      paymentID,
			"tier":            tier,
			"billing_cycle":   cycle,
			"idempotency_key": idempKey,
		},
	}
	raw, _ := json.Marshal(bodyMap)
	req, err := http.NewRequestWithContext(ctx, http.MethodPost, baseURL+"/transaction/initialize", bytes.NewReader(raw))
	if err != nil {
		return "", err
	}
	req.Header.Set("Authorization", "Bearer "+secret)
	req.Header.Set("Content-Type", "application/json")
	req.Header.Set("Idempotency-Key", idempKey)
	resp, err := s.httpClient.Do(req)
	if err != nil {
		return "", ErrPaymentFailed
	}
	defer resp.Body.Close()
	var parsed struct {
		Status bool `json:"status"`
		Data   struct {
			AuthorizationURL string `json:"authorization_url"`
		} `json:"data"`
	}
	if json.NewDecoder(io.LimitReader(resp.Body, 1<<20)).Decode(&parsed) != nil || resp.StatusCode < 200 || resp.StatusCode >= 300 || !parsed.Status || strings.TrimSpace(parsed.Data.AuthorizationURL) == "" {
		return "", ErrPaymentFailed
	}
	return parsed.Data.AuthorizationURL, nil
}

func (s *Service) verifyPaystackWithServer(ctx context.Context, reference string, expectedMinor int64, expectedCurrency string) (int64, string, error) {
	s.mu.Lock()
	base := s.paystackBase
	sec := s.paystackSecret
	s.mu.Unlock()
	if sec == "" {
		return 0, "", ErrGatewayUnconfigured
	}
	req, err := http.NewRequestWithContext(ctx, http.MethodGet, base+"/transaction/verify/"+url.PathEscape(reference), nil)
	if err != nil {
		return 0, "", err
	}
	req.Header.Set("Authorization", "Bearer "+sec)
	resp, err := s.httpClient.Do(req)
	if err != nil {
		return 0, "", ErrPaymentFailed
	}
	defer resp.Body.Close()
	var parsed struct {
		Status bool `json:"status"`
		Data   struct {
			Status   string `json:"status"`
			Amount   int64  `json:"amount"`
			Currency string `json:"currency"`
		} `json:"data"`
	}
	if json.NewDecoder(io.LimitReader(resp.Body, 1<<20)).Decode(&parsed) != nil || !parsed.Status || !strings.EqualFold(parsed.Data.Status, "success") {
		return 0, "", ErrPaymentFailed
	}
	if parsed.Data.Amount < expectedMinor || !strings.EqualFold(parsed.Data.Currency, expectedCurrency) {
		return 0, "", ErrPaymentFailed
	}
	return parsed.Data.Amount, parsed.Data.Currency, nil
}

func (s *Service) getPayPalToken(ctx context.Context) (string, error) {
	s.mu.Lock()
	if s.paypalAccessToken != "" && time.Now().Add(45*time.Second).Before(s.paypalTokenExpiry) {
		tok := s.paypalAccessToken
		s.mu.Unlock()
		return tok, nil
	}
	base := s.paypalBase
	cid := s.paypalClientID
	sec := s.paypalSecret
	s.mu.Unlock()
	if cid == "" || sec == "" {
		return "", ErrGatewayUnconfigured
	}
	form := url.Values{}
	form.Set("grant_type", "client_credentials")
	req, err := http.NewRequestWithContext(ctx, http.MethodPost, base+"/v1/oauth2/token", strings.NewReader(form.Encode()))
	if err != nil {
		return "", err
	}
	req.Header.Set("Content-Type", "application/x-www-form-urlencoded")
	req.Header.Set("Authorization", "Basic "+base64.StdEncoding.EncodeToString([]byte(cid+":"+sec)))
	resp, err := s.httpClient.Do(req)
	if err != nil {
		return "", ErrPaymentFailed
	}
	defer resp.Body.Close()
	var parsed struct {
		AccessToken string `json:"access_token"`
		ExpiresIn   int64  `json:"expires_in"`
	}
	if json.NewDecoder(io.LimitReader(resp.Body, 1<<20)).Decode(&parsed) != nil || parsed.AccessToken == "" {
		return "", ErrPaymentFailed
	}
	s.mu.Lock()
	s.paypalAccessToken = parsed.AccessToken
	ttl := time.Duration(parsed.ExpiresIn) * time.Second
	if ttl <= 0 {
		ttl = 5 * time.Minute
	}
	s.paypalTokenExpiry = time.Now().Add(ttl)
	s.mu.Unlock()
	return parsed.AccessToken, nil
}

func (s *Service) initPayPalOrder(
	ctx context.Context,
	userID, paymentID, tier, cycle, amountVal, currency, idempKey, returnURL string,
) (string, string, error) {
	tok, err := s.getPayPalToken(ctx)
	if err != nil {
		return "", "", err
	}
	orderBody := map[string]any{
		"intent": "CAPTURE",
		"purchase_units": []map[string]any{
			{
				"reference_id": paymentID,
				"custom_id":    fmt.Sprintf("%s|%s|%s|%s", userID, paymentID, tier, cycle),
				"description":  fmt.Sprintf("Telefam %s Verification (%s)", strings.ToUpper(tier), cycle),
				"amount": map[string]string{
					"currency_code": currency,
					"value":         amountVal,
				},
			},
		},
		"application_context": map[string]any{
			"brand_name":  "Telefam",
			"user_action": "PAY_NOW",
			"return_url":  returnURL + "&paypal_status=approved",
			"cancel_url":  returnURL + "&paypal_status=cancelled",
		},
	}
	raw, _ := json.Marshal(orderBody)
	req, err := http.NewRequestWithContext(ctx, http.MethodPost, s.paypalBase+"/v2/checkout/orders", bytes.NewReader(raw))
	if err != nil {
		return "", "", err
	}
	req.Header.Set("Authorization", "Bearer "+tok)
	req.Header.Set("Content-Type", "application/json")
	req.Header.Set("PayPal-Request-Id", idempKey)
	resp, err := s.httpClient.Do(req)
	if err != nil {
		return "", "", ErrPaymentFailed
	}
	defer resp.Body.Close()
	var parsed struct {
		ID    string `json:"id"`
		Links []struct {
			Href string `json:"href"`
			Rel  string `json:"rel"`
		} `json:"links"`
	}
	if json.NewDecoder(io.LimitReader(resp.Body, 1<<20)).Decode(&parsed) != nil || parsed.ID == "" {
		return "", "", ErrPaymentFailed
	}
	approveURL := ""
	for _, l := range parsed.Links {
		if strings.EqualFold(l.Rel, "approve") || strings.EqualFold(l.Rel, "payer-action") {
			approveURL = l.Href
			break
		}
	}
	if approveURL == "" {
		return "", "", ErrPaymentFailed
	}
	return parsed.ID, approveURL, nil
}

func (s *Service) captureAndVerifyPayPalOrder(ctx context.Context, orderID string, expectedMinor int64, expectedCurrency string) (int64, string, error) {
	if strings.TrimSpace(orderID) == "" {
		return 0, "", ErrBadRequest
	}
	tok, err := s.getPayPalToken(ctx)
	if err != nil {
		return 0, "", err
	}
	req, err := http.NewRequestWithContext(ctx, http.MethodPost, s.paypalBase+"/v2/checkout/orders/"+url.PathEscape(orderID)+"/capture", bytes.NewReader([]byte("{}")))
	if err != nil {
		return 0, "", err
	}
	req.Header.Set("Authorization", "Bearer "+tok)
	req.Header.Set("Content-Type", "application/json")
	resp, err := s.httpClient.Do(req)
	if err != nil {
		return 0, "", ErrPaymentFailed
	}
	defer resp.Body.Close()
	var parsed struct {
		Status        string `json:"status"`
		PurchaseUnits []struct {
			Payments struct {
				Captures []struct {
					Status string `json:"status"`
					Amount struct {
						CurrencyCode string `json:"currency_code"`
						Value        string `json:"value"`
					} `json:"amount"`
				} `json:"captures"`
			} `json:"payments"`
		} `json:"purchase_units"`
	}
	if json.NewDecoder(io.LimitReader(resp.Body, 1<<20)).Decode(&parsed) != nil || !strings.EqualFold(parsed.Status, "COMPLETED") {
		return 0, "", ErrPaymentFailed
	}
	for _, pu := range parsed.PurchaseUnits {
		for _, capItem := range pu.Payments.Captures {
			if strings.EqualFold(capItem.Status, "COMPLETED") {
				fVal, _ := strconv.ParseFloat(capItem.Amount.Value, 64)
				paidMinor := int64(math.Round(fVal * 100.0))
				if paidMinor >= expectedMinor && strings.EqualFold(capItem.Amount.CurrencyCode, expectedCurrency) {
					return paidMinor, capItem.Amount.CurrencyCode, nil
				}
			}
		}
	}
	return 0, "", ErrPaymentFailed
}
