package subscriptions

import (
	"bytes"
	"context"
	"crypto/hmac"
	"crypto/rand"
	"crypto/sha512"
	"encoding/base64"
	"encoding/hex"
	"encoding/json"
	"errors"
	"fmt"
	"io"
	"math"
	"net/http"
	"net/url"
	"os"
	"regexp"
	"sort"
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
	ErrSelfSubscribe       = errors.New("cannot subscribe to your own profile")
	ErrPlanDisabled        = errors.New("creator subscriptions are currently paused")
	ErrIdempotencyConflict = errors.New("idempotency key conflict")
	ErrRateLimited         = errors.New("too many subscription payment attempts; please wait")
	ErrGatewayUnconfigured = errors.New("payment gateway not configured")
	ErrPaymentFailed       = errors.New("payment verification failed")
)

const (
	CycleMonthly = "monthly"
	CycleAnnual  = "annual"

	StatusActive    = "active"
	StatusCancelled = "cancelled"
	StatusExpired   = "expired"

	MinMonthlyUSDCents int64 = 50      // $0.50
	MaxMonthlyUSDCents int64 = 10000   // $100.00
	MinGoalUSDCents    int64 = 50      // $0.50
	MaxGoalUSDCents    int64 = 1000000 // $10,000.00 (presets span $0.50 - $100.00)
	DefaultDiscountPct int   = 20
)

var idempotencyPattern = regexp.MustCompile(`^[A-Za-z0-9._:-]{8,128}$`)

type UserInfo struct {
	UserID       string
	FullName     string
	Username     string
	AvatarURL    string
	VerifiedTier string
	Email        string
	CountryISO   string
}

type HostHooks struct {
	ResolveUser             func(userID string) (UserInfo, bool)
	SendWSEvent             func(targetUserID string, event map[string]any)
	NotifySubscriptionEvent func(ctx context.Context, recipientUserID string, actor UserInfo, kind, title, body, billingCycle string)
	AuditEvent              func(userID, action, deviceFP, ipFP string, meta map[string]string)
}

type PlanPricingDTO struct {
	MonthlyUSDCents      int64  `json:"monthly_usd_cents"`
	AnnualUSDCents       int64  `json:"annual_usd_cents"`
	AnnualDiscountPct    int    `json:"annual_discount_pct"`
	CurrencyCode         string `json:"currency_code"`
	CurrencySymbol       string `json:"currency_symbol"`
	MonthlyDisplay       string `json:"monthly_display"`
	AnnualDisplay        string `json:"annual_display"`
	AnnualOriginalDisp   string `json:"annual_original_display"`
	PaystackMonthlyMinor int64  `json:"paystack_monthly_minor"`
	PaystackAnnualMinor  int64  `json:"paystack_annual_minor"`
	PaystackCurrency     string `json:"paystack_currency"`
	PayPalMonthlyValue   string `json:"paypal_monthly_value"`
	PayPalAnnualValue    string `json:"paypal_annual_value"`
	PayPalCurrency       string `json:"paypal_currency"`
}

type GoalPresetDTO struct {
	AmountUSDCents int64  `json:"amount_usd_cents"`
	USDLabel       string `json:"usd_label"`
	LocalDisplay   string `json:"local_display"`
}

type CreatorPlanDTO struct {
	CreatorUserID          string         `json:"creator_user_id"`
	Enabled                bool           `json:"enabled"`
	MonthlyUSDCents        int64          `json:"monthly_usd_cents"`
	AnnualDiscountPct      int            `json:"annual_discount_pct"`
	MonthlyGoalUSDCents    int64          `json:"monthly_goal_usd_cents"`
	GoalUSDDisplay         string         `json:"goal_usd_display"`
	GoalLocalDisplay       string         `json:"goal_local_display"`
	CurrentMRRUSDCents     int64          `json:"current_mrr_usd_cents"`
	CurrentMRRUSDDisplay   string         `json:"current_mrr_usd_display"`
	CurrentMRRLocalDisplay string         `json:"current_mrr_local_display"`
	GoalProgressPct        float64        `json:"goal_progress_pct"`
	GoalReached            bool           `json:"goal_reached"`
	ActiveSubscriberCount  int            `json:"active_subscriber_count"`
	PerksSummary           string         `json:"perks_summary"`
	Pricing                PlanPricingDTO `json:"pricing"`
	UpdatedAtMs            int64          `json:"updated_at_ms"`
}

type SubscriptionRecordDTO struct {
	SubscriptionID      string `json:"subscription_id"`
	SubscriberUserID    string `json:"subscriber_user_id"`
	SubscriberName      string `json:"subscriber_name"`
	SubscriberUsername  string `json:"subscriber_username"`
	SubscriberAvatarURL string `json:"subscriber_avatar_url,omitempty"`
	SubscriberVerified  string `json:"subscriber_verified_tier"`
	CreatorUserID       string `json:"creator_user_id"`
	CreatorName         string `json:"creator_name"`
	CreatorUsername     string `json:"creator_username"`
	CreatorAvatarURL    string `json:"creator_avatar_url,omitempty"`
	CreatorVerified     string `json:"creator_verified_tier"`
	BillingCycle        string `json:"billing_cycle"`
	Status              string `json:"status"`
	AutoRenew           bool   `json:"auto_renew"`
	AmountUSDCents      int64  `json:"amount_usd_cents"`
	AmountDisplay       string `json:"amount_display"`
	Provider            string `json:"provider"`
	StartedAtMs         int64  `json:"started_at_ms"`
	CurrentPeriodEndMs  int64  `json:"current_period_end_ms"`
	CancelledAtMs       int64  `json:"cancelled_at_ms,omitempty"`
	CanCancel           bool   `json:"can_cancel"`
	CanRenew            bool   `json:"can_renew"`
}

type SubscriptionsHubResponse struct {
	Success             bool                    `json:"success"`
	UserID              string                  `json:"user_id"`
	CountryISO          string                  `json:"country_iso"`
	CurrencyCode        string                  `json:"currency_code"`
	MyPlan              CreatorPlanDTO          `json:"my_plan"`
	Subscribers         []SubscriptionRecordDTO `json:"subscribers"`
	ActiveSubscriptions []SubscriptionRecordDTO `json:"active_subscriptions"`
	GoalPresets         []GoalPresetDTO         `json:"goal_presets"`
	ServerEpochMs       int64                   `json:"server_epoch_ms"`
}

type CreatorOfferingResponse struct {
	Success             bool                   `json:"success"`
	CreatorUserID       string                 `json:"creator_user_id"`
	CreatorName         string                 `json:"creator_name"`
	CreatorUsername     string                 `json:"creator_username"`
	CreatorAvatarURL    string                 `json:"creator_avatar_url,omitempty"`
	CreatorVerifiedTier string                 `json:"creator_verified_tier"`
	CountryISO          string                 `json:"country_iso"`
	Plan                CreatorPlanDTO         `json:"plan"`
	ViewerSubscription  *SubscriptionRecordDTO `json:"viewer_subscription,omitempty"`
	IsOwner             bool                   `json:"is_owner"`
	ServerEpochMs       int64                  `json:"server_epoch_ms"`
}

type SavePlanPayload struct {
	Enabled             *bool  `json:"enabled,omitempty"`
	MonthlyUSDCents     int64  `json:"monthly_usd_cents"`
	AnnualDiscountPct   *int   `json:"annual_discount_pct,omitempty"`
	MonthlyGoalUSDCents int64  `json:"monthly_goal_usd_cents"`
	PerksSummary        string `json:"perks_summary,omitempty"`
	CountryISO          string `json:"country_iso,omitempty"`
}

type SubscriptionPaymentDTO struct {
	PaymentID           string `json:"payment_id"`
	CreatorUserID       string `json:"creator_user_id"`
	BillingCycle        string `json:"billing_cycle"`
	Provider            string `json:"provider"`
	ProviderReference   string `json:"provider_reference"`
	ProviderOrderID     string `json:"provider_order_id,omitempty"`
	AmountUSDCents      int64  `json:"amount_usd_cents"`
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

type CheckoutInitPayload struct {
	CreatorUserID  string `json:"creator_user_id"`
	BillingCycle   string `json:"billing_cycle"`
	Provider       string `json:"provider"`
	CountryISO     string `json:"country_iso"`
	IdempotencyKey string `json:"idempotency_key"`
}

type CheckoutInitResponse struct {
	Success   bool                   `json:"success"`
	Payment   SubscriptionPaymentDTO `json:"payment"`
	Offering  CreatorOfferingResponse `json:"offering"`
	ReturnURL string                 `json:"return_url"`
}

type CheckoutVerifyPayload struct {
	PaymentID      string `json:"payment_id"`
	Reference      string `json:"reference,omitempty"`
	PayPalOrderID  string `json:"paypal_order_id,omitempty"`
	IdempotencyKey string `json:"idempotency_key"`
}

type CheckoutVerifyResponse struct {
	Success      bool                    `json:"success"`
	Payment      SubscriptionPaymentDTO  `json:"payment"`
	Subscription *SubscriptionRecordDTO  `json:"subscription,omitempty"`
	Offering     CreatorOfferingResponse `json:"offering"`
}

type CancelPayload struct {
	CreatorUserID string `json:"creator_user_id"`
	CountryISO    string `json:"country_iso,omitempty"`
}

type RenewPayload struct {
	CreatorUserID  string `json:"creator_user_id"`
	BillingCycle   string `json:"billing_cycle,omitempty"`
	Provider       string `json:"provider,omitempty"`
	CountryISO     string `json:"country_iso,omitempty"`
	IdempotencyKey string `json:"idempotency_key,omitempty"`
}

type MutateSubscriptionResponse struct {
	Success          bool                     `json:"success"`
	RequiresCheckout bool                     `json:"requires_checkout,omitempty"`
	Subscription     *SubscriptionRecordDTO   `json:"subscription,omitempty"`
	Checkout         *CheckoutInitResponse    `json:"checkout,omitempty"`
	Message          string                   `json:"message"`
	ServerEpochMs    int64                    `json:"server_epoch_ms"`
}

type planRecord struct {
	CreatorUserID       string
	Enabled             bool
	MonthlyUSDCents     int64
	AnnualDiscountPct   int
	MonthlyGoalUSDCents int64
	PerksSummary        string
	UpdatedAtMs         int64
}

type subscriptionRecord struct {
	SubscriptionID     string
	SubscriberUserID   string
	CreatorUserID      string
	BillingCycle       string
	Status             string
	AutoRenew          bool
	AmountUSDCents     int64
	DisplayAmountMinor int64
	DisplayCurrency    string
	CountryISO         string
	Provider           string
	StartedAtMs        int64
	CurrentPeriodEndMs int64
	CancelledAtMs      int64
	UpdatedAtMs        int64
}

type paymentRecord struct {
	DTO              SubscriptionPaymentDTO
	SubscriberUserID string
	CountryISO       string
	IdempotencyKey   string
}

type countrySpec struct {
	CurrencyCode   string
	Symbol         string
	RateFromUSD    float64
	ZeroDecimal    bool
	PaystackCurr   string
	PaystackMulUSD float64
	PayPalCurr     string
	PayPalMulUSD   float64
}

type Service struct {
	hooks             HostHooks
	mu                sync.Mutex
	plans             map[string]*planRecord
	subs              map[string]*subscriptionRecord // key: subscriberUserID + ":" + creatorUserID
	payments          map[string]*paymentRecord
	payByIdempKey     map[string]*paymentRecord
	payByRef          map[string]*paymentRecord
	payByOrderID      map[string]*paymentRecord
	attemptTimestamps map[string][]int64
	paystackSecret    string
	paystackBase      string
	paypalClientID    string
	paypalSecret      string
	paypalBase        string
	paypalWebhookID   string
	publicBaseURL     string
	paypalAccessToken string
	paypalTokenExpiry time.Time
	httpClient        *http.Client
	nowFunc           func() time.Time
}

func NewService(hooks HostHooks) *Service {
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
		plans:             make(map[string]*planRecord),
		subs:              make(map[string]*subscriptionRecord),
		payments:          make(map[string]*paymentRecord),
		payByIdempKey:     make(map[string]*paymentRecord),
		payByRef:          make(map[string]*paymentRecord),
		payByOrderID:      make(map[string]*paymentRecord),
		attemptTimestamps: make(map[string][]int64),
		paystackSecret:    strings.TrimSpace(os.Getenv("PAYSTACK_SECRET_KEY")),
		paystackBase:      "https://api.paystack.co",
		paypalClientID:    strings.TrimSpace(os.Getenv("PAYPAL_CLIENT_ID")),
		paypalSecret:      strings.TrimSpace(os.Getenv("PAYPAL_CLIENT_SECRET")),
		paypalBase:        strings.TrimRight(ppBase, "/"),
		paypalWebhookID:   strings.TrimSpace(os.Getenv("PAYPAL_WEBHOOK_ID")),
		publicBaseURL:     strings.TrimRight(pubBase, "/"),
		httpClient:        &http.Client{Timeout: 12 * time.Second},
		nowFunc:           time.Now,
	}
}

func (s *Service) ConfigureGatewaysForTest(
	paystackBase, paystackSecret, paypalBase, paypalClientID, paypalSecret, paypalWebhookID string,
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

func IsValidCycle(cycle string) bool {
	return cycle == CycleMonthly || cycle == CycleAnnual
}

func countryPricingSpec(countryISO string) countrySpec {
	switch strings.ToUpper(strings.TrimSpace(countryISO)) {
	case "KE":
		return countrySpec{"KES", "Ksh ", 129.0, false, "KES", 129.0, "USD", 1.0}
	case "NG":
		return countrySpec{"NGN", "NGN ", 1550.0, false, "NGN", 1550.0, "USD", 1.0}
	case "GH":
		return countrySpec{"GHS", "GHS ", 15.2, false, "GHS", 15.2, "USD", 1.0}
	case "ZA":
		return countrySpec{"ZAR", "R ", 18.2, false, "ZAR", 18.2, "USD", 1.0}
	case "UG":
		return countrySpec{"UGX", "UGX ", 3700.0, true, "KES", 129.0, "USD", 1.0}
	case "TZ":
		return countrySpec{"TZS", "TZS ", 2650.0, true, "KES", 129.0, "USD", 1.0}
	case "RW":
		return countrySpec{"RWF", "RWF ", 1350.0, true, "KES", 129.0, "USD", 1.0}
	case "GB":
		return countrySpec{"GBP", "GBP ", 0.79, false, "USD", 1.0, "GBP", 0.79}
	case "DE", "FR", "ES", "IT", "NL", "IE", "BE", "PT", "AT", "FI":
		return countrySpec{"EUR", "EUR ", 0.92, false, "USD", 1.0, "EUR", 0.92}
	case "CA":
		return countrySpec{"CAD", "CA$", 1.38, false, "USD", 1.0, "CAD", 1.38}
	case "AU":
		return countrySpec{"AUD", "A$", 1.52, false, "USD", 1.0, "AUD", 1.52}
	case "IN":
		return countrySpec{"INR", "INR ", 84.5, false, "USD", 1.0, "USD", 1.0}
	default:
		return countrySpec{"USD", "$", 1.0, false, "USD", 1.0, "USD", 1.0}
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

func formatUSDFromCents(usdCents int64) string {
	return fmt.Sprintf("$%.2f", float64(usdCents)/100.0)
}

func formatLocalFromUSDCents(usdCents int64, countryISO string) string {
	spec := countryPricingSpec(countryISO)
	usd := float64(usdCents) / 100.0
	raw := usd * spec.RateFromUSD
	if spec.ZeroDecimal {
		return spec.Symbol + formatWithCommas(int64(math.Round(raw)))
	}
	if spec.RateFromUSD >= 20.0 && raw >= 100.0 {
		return spec.Symbol + formatWithCommas(int64(math.Round(raw)))
	}
	return fmt.Sprintf("%s%.2f", spec.Symbol, math.Round(raw*100.0)/100.0)
}

func computeAnnualUSDCents(monthlyUSDCents int64, discountPct int) int64 {
	if discountPct < 0 {
		discountPct = 0
	}
	if discountPct > 50 {
		discountPct = 50
	}
	full := monthlyUSDCents * 12
	disc := int64(math.Round(float64(full) * float64(100-discountPct) / 100.0))
	if disc < MinMonthlyUSDCents {
		disc = MinMonthlyUSDCents
	}
	return disc
}

func ComputePlanPricing(monthlyUSDCents int64, discountPct int, countryISO string) PlanPricingDTO {
	if monthlyUSDCents < MinMonthlyUSDCents {
		monthlyUSDCents = MinMonthlyUSDCents
	}
	if monthlyUSDCents > MaxMonthlyUSDCents {
		monthlyUSDCents = MaxMonthlyUSDCents
	}
	spec := countryPricingSpec(countryISO)
	annualFullCents := monthlyUSDCents * 12
	annualDiscCents := computeAnnualUSDCents(monthlyUSDCents, discountPct)

	psMonthMinor := int64(math.Round(float64(monthlyUSDCents) * spec.PaystackMulUSD))
	if psMonthMinor < 50 {
		psMonthMinor = 50
	}
	psYearMinor := int64(math.Round(float64(annualDiscCents) * spec.PaystackMulUSD))
	if psYearMinor < 50 {
		psYearMinor = 50
	}

	ppMonthMinor := int64(math.Round(float64(monthlyUSDCents) * spec.PayPalMulUSD))
	if ppMonthMinor < 50 {
		ppMonthMinor = 50
	}
	ppYearMinor := int64(math.Round(float64(annualDiscCents) * spec.PayPalMulUSD))
	if ppYearMinor < 50 {
		ppYearMinor = 50
	}

	return PlanPricingDTO{
		MonthlyUSDCents:      monthlyUSDCents,
		AnnualUSDCents:       annualDiscCents,
		AnnualDiscountPct:    discountPct,
		CurrencyCode:         spec.CurrencyCode,
		CurrencySymbol:       spec.Symbol,
		MonthlyDisplay:       formatLocalFromUSDCents(monthlyUSDCents, countryISO),
		AnnualDisplay:        formatLocalFromUSDCents(annualDiscCents, countryISO),
		AnnualOriginalDisp:   formatLocalFromUSDCents(annualFullCents, countryISO),
		PaystackMonthlyMinor: psMonthMinor,
		PaystackAnnualMinor:  psYearMinor,
		PaystackCurrency:     spec.PaystackCurr,
		PayPalMonthlyValue:   fmt.Sprintf("%.2f", float64(ppMonthMinor)/100.0),
		PayPalAnnualValue:    fmt.Sprintf("%.2f", float64(ppYearMinor)/100.0),
		PayPalCurrency:       spec.PayPalCurr,
	}
}

func buildGoalPresets(countryISO string) []GoalPresetDTO {
	cents := []int64{50, 100, 200, 500, 1000, 2500, 5000, 10000}
	out := make([]GoalPresetDTO, 0, len(cents))
	for _, c := range cents {
		out = append(out, GoalPresetDTO{
			AmountUSDCents: c,
			USDLabel:       formatUSDFromCents(c),
			LocalDisplay:   formatLocalFromUSDCents(c, countryISO),
		})
	}
	return out
}

func newRandomID(prefix string) string {
	var b [12]byte
	_, _ = rand.Read(b[:])
	return prefix + "_" + hex.EncodeToString(b[:])
}

func (s *Service) nowMs() int64 {
	return s.nowFunc().UTC().UnixMilli()
}

func (s *Service) resolveUser(userID string) UserInfo {
	clean := strings.TrimSpace(userID)
	if clean == "" {
		return UserInfo{}
	}
	if s.hooks.ResolveUser != nil {
		if u, ok := s.hooks.ResolveUser(clean); ok {
			return u
		}
	}
	return UserInfo{
		UserID:   clean,
		FullName: clean,
		Username: clean,
		Email:    clean + "@users.telefam.company",
	}
}

func (s *Service) resolveCountry(userID, override string) string {
	country := strings.ToUpper(strings.TrimSpace(override))
	if len(country) == 2 {
		return country
	}
	u := s.resolveUser(userID)
	if len(strings.TrimSpace(u.CountryISO)) == 2 {
		return strings.ToUpper(strings.TrimSpace(u.CountryISO))
	}
	return "US"
}

func (s *Service) getOrCreatePlanLocked(creatorUserID string) *planRecord {
	p, ok := s.plans[creatorUserID]
	if !ok {
		p = &planRecord{
			CreatorUserID:       creatorUserID,
			Enabled:             true,
			MonthlyUSDCents:     500, // $5.00 default within $0.50 - $100.00 range
			AnnualDiscountPct:   DefaultDiscountPct,
			MonthlyGoalUSDCents: 10000, // $100.00 default monthly goal
			PerksSummary:        "Exclusive subscriber updates, priority inbox access, and supporter badge.",
			UpdatedAtMs:         s.nowMs(),
		}
		s.plans[creatorUserID] = p
	}
	return p
}

func (s *Service) refreshExpiryLocked(rec *subscriptionRecord, now int64) {
	if rec == nil {
		return
	}
	if (rec.Status == StatusActive || rec.Status == StatusCancelled) && now >= rec.CurrentPeriodEndMs {
		rec.Status = StatusExpired
		rec.AutoRenew = false
		rec.UpdatedAtMs = now
	}
}

func (s *Service) computeCreatorMetricsLocked(creatorUserID string, now int64) (mrrCents int64, activeCount int) {
	for _, rec := range s.subs {
		if rec.CreatorUserID != creatorUserID {
			continue
		}
		s.refreshExpiryLocked(rec, now)
		if rec.Status == StatusActive || (rec.Status == StatusCancelled && now < rec.CurrentPeriodEndMs) {
			activeCount++
			if rec.BillingCycle == CycleAnnual {
				mrrCents += rec.AmountUSDCents / 12
			} else {
				mrrCents += rec.AmountUSDCents
			}
		}
	}
	return mrrCents, activeCount
}

func (s *Service) buildPlanDTOLocked(p *planRecord, countryISO string, now int64) CreatorPlanDTO {
	mrrCents, activeCount := s.computeCreatorMetricsLocked(p.CreatorUserID, now)
	goal := p.MonthlyGoalUSDCents
	if goal < MinGoalUSDCents {
		goal = MinGoalUSDCents
	}
	pct := (float64(mrrCents) / float64(goal)) * 100.0
	if pct > 100.0 {
		pct = 100.0
	}
	if pct < 0 {
		pct = 0
	}
	pct = math.Round(pct*10.0) / 10.0

	return CreatorPlanDTO{
		CreatorUserID:          p.CreatorUserID,
		Enabled:                p.Enabled,
		MonthlyUSDCents:        p.MonthlyUSDCents,
		AnnualDiscountPct:      p.AnnualDiscountPct,
		MonthlyGoalUSDCents:    p.MonthlyGoalUSDCents,
		GoalUSDDisplay:         formatUSDFromCents(p.MonthlyGoalUSDCents),
		GoalLocalDisplay:       formatLocalFromUSDCents(p.MonthlyGoalUSDCents, countryISO),
		CurrentMRRUSDCents:     mrrCents,
		CurrentMRRUSDDisplay:   formatUSDFromCents(mrrCents),
		CurrentMRRLocalDisplay: formatLocalFromUSDCents(mrrCents, countryISO),
		GoalProgressPct:        pct,
		GoalReached:            mrrCents >= goal,
		ActiveSubscriberCount:  activeCount,
		PerksSummary:           p.PerksSummary,
		Pricing:                ComputePlanPricing(p.MonthlyUSDCents, p.AnnualDiscountPct, countryISO),
		UpdatedAtMs:            p.UpdatedAtMs,
	}
}

func (s *Service) buildSubDTOLocked(rec *subscriptionRecord, countryISO string, now int64) SubscriptionRecordDTO {
	s.refreshExpiryLocked(rec, now)
	subUser := s.resolveUser(rec.SubscriberUserID)
	creatorUser := s.resolveUser(rec.CreatorUserID)
	subVer := subUser.VerifiedTier
	if subVer == "" {
		subVer = "none"
	}
	creatorVer := creatorUser.VerifiedTier
	if creatorVer == "" {
		creatorVer = "none"
	}
	return SubscriptionRecordDTO{
		SubscriptionID:      rec.SubscriptionID,
		SubscriberUserID:    rec.SubscriberUserID,
		SubscriberName:      subUser.FullName,
		SubscriberUsername:  subUser.Username,
		SubscriberAvatarURL: subUser.AvatarURL,
		SubscriberVerified:  subVer,
		CreatorUserID:       rec.CreatorUserID,
		CreatorName:         creatorUser.FullName,
		CreatorUsername:     creatorUser.Username,
		CreatorAvatarURL:    creatorUser.AvatarURL,
		CreatorVerified:     creatorVer,
		BillingCycle:        rec.BillingCycle,
		Status:              rec.Status,
		AutoRenew:           rec.AutoRenew,
		AmountUSDCents:      rec.AmountUSDCents,
		AmountDisplay:       formatLocalFromUSDCents(rec.AmountUSDCents, countryISO),
		Provider:            rec.Provider,
		StartedAtMs:         rec.StartedAtMs,
		CurrentPeriodEndMs:  rec.CurrentPeriodEndMs,
		CancelledAtMs:       rec.CancelledAtMs,
		CanCancel:           rec.Status == StatusActive && rec.AutoRenew,
		CanRenew:            rec.Status == StatusCancelled || rec.Status == StatusExpired,
	}
}

func (s *Service) GetHub(userID, countryOverride string) (SubscriptionsHubResponse, error) {
	clean := strings.TrimSpace(userID)
	if clean == "" {
		return SubscriptionsHubResponse{}, ErrUnauthorized
	}
	country := s.resolveCountry(clean, countryOverride)
	now := s.nowMs()

	s.mu.Lock()
	defer s.mu.Unlock()

	myPlan := s.getOrCreatePlanLocked(clean)
	planDTO := s.buildPlanDTOLocked(myPlan, country, now)

	subscribers := make([]SubscriptionRecordDTO, 0)
	activeSubs := make([]SubscriptionRecordDTO, 0)

	for _, rec := range s.subs {
		if rec.CreatorUserID == clean {
			subscribers = append(subscribers, s.buildSubDTOLocked(rec, country, now))
		}
		if rec.SubscriberUserID == clean {
			activeSubs = append(activeSubs, s.buildSubDTOLocked(rec, country, now))
		}
	}

	sort.Slice(subscribers, func(i, j int) bool {
		return subscribers[i].StartedAtMs > subscribers[j].StartedAtMs
	})
	sort.Slice(activeSubs, func(i, j int) bool {
		return activeSubs[i].StartedAtMs > activeSubs[j].StartedAtMs
	})

	spec := countryPricingSpec(country)
	return SubscriptionsHubResponse{
		Success:             true,
		UserID:              clean,
		CountryISO:          country,
		CurrencyCode:        spec.CurrencyCode,
		MyPlan:              planDTO,
		Subscribers:         subscribers,
		ActiveSubscriptions: activeSubs,
		GoalPresets:         buildGoalPresets(country),
		ServerEpochMs:       now,
	}, nil
}

func (s *Service) SaveCreatorPlan(
	ctx context.Context,
	userID, deviceFP, ipFP string,
	payload SavePlanPayload,
) (SubscriptionsHubResponse, error) {
	clean := strings.TrimSpace(userID)
	if clean == "" {
		return SubscriptionsHubResponse{}, ErrUnauthorized
	}
	if payload.MonthlyUSDCents < MinMonthlyUSDCents || payload.MonthlyUSDCents > MaxMonthlyUSDCents {
		return SubscriptionsHubResponse{}, ErrBadRequest
	}
	goalCents := payload.MonthlyGoalUSDCents
	if goalCents == 0 {
		goalCents = MaxMonthlyUSDCents
	}
	if goalCents < MinGoalUSDCents || goalCents > MaxGoalUSDCents {
		return SubscriptionsHubResponse{}, ErrBadRequest
	}
	discount := DefaultDiscountPct
	if payload.AnnualDiscountPct != nil {
		if *payload.AnnualDiscountPct < 0 || *payload.AnnualDiscountPct > 50 {
			return SubscriptionsHubResponse{}, ErrBadRequest
		}
		discount = *payload.AnnualDiscountPct
	}
	country := s.resolveCountry(clean, payload.CountryISO)
	now := s.nowMs()

	s.mu.Lock()
	p := s.getOrCreatePlanLocked(clean)
	if payload.Enabled != nil {
		p.Enabled = *payload.Enabled
	} else {
		p.Enabled = true
	}
	p.MonthlyUSDCents = payload.MonthlyUSDCents
	p.AnnualDiscountPct = discount
	p.MonthlyGoalUSDCents = goalCents
	cleanPerks := strings.TrimSpace(payload.PerksSummary)
	if cleanPerks != "" {
		if len(cleanPerks) > 220 {
			cleanPerks = cleanPerks[:220]
		}
		p.PerksSummary = cleanPerks
	}
	p.UpdatedAtMs = now
	s.mu.Unlock()

	if s.hooks.AuditEvent != nil {
		s.hooks.AuditEvent(clean, "creator_subscription_plan_updated", deviceFP, ipFP, map[string]string{
			"monthly_usd_cents":      strconv.FormatInt(payload.MonthlyUSDCents, 10),
			"monthly_goal_usd_cents": strconv.FormatInt(goalCents, 10),
		})
	}
	if s.hooks.SendWSEvent != nil {
		s.hooks.SendWSEvent(clean, map[string]any{
			"type": "subscription.updated",
			"data": map[string]any{
				"creator_user_id": clean,
				"action":          "plan_updated",
			},
		})
	}

	return s.GetHub(clean, country)
}

func (s *Service) GetCreatorOffering(viewerUserID, creatorUserID, countryOverride string) (CreatorOfferingResponse, error) {
	viewer := strings.TrimSpace(viewerUserID)
	if viewer == "" {
		return CreatorOfferingResponse{}, ErrUnauthorized
	}
	creator := strings.TrimSpace(creatorUserID)
	if creator == "" {
		return CreatorOfferingResponse{}, ErrBadRequest
	}
	country := s.resolveCountry(viewer, countryOverride)
	creatorInfo := s.resolveUser(creator)
	now := s.nowMs()

	s.mu.Lock()
	defer s.mu.Unlock()

	plan := s.getOrCreatePlanLocked(creator)
	planDTO := s.buildPlanDTOLocked(plan, country, now)
	var subDTO *SubscriptionRecordDTO
	if existing, ok := s.subs[viewer+":"+creator]; ok {
		d := s.buildSubDTOLocked(existing, country, now)
		subDTO = &d
	}
	verTier := creatorInfo.VerifiedTier
	if verTier == "" {
		verTier = "none"
	}

	return CreatorOfferingResponse{
		Success:             true,
		CreatorUserID:       creator,
		CreatorName:         creatorInfo.FullName,
		CreatorUsername:     creatorInfo.Username,
		CreatorAvatarURL:    creatorInfo.AvatarURL,
		CreatorVerifiedTier: verTier,
		CountryISO:          country,
		Plan:                planDTO,
		ViewerSubscription:  subDTO,
		IsOwner:             viewer == creator,
		ServerEpochMs:       now,
	}, nil
}

func (s *Service) checkRateLimitLocked(userID string, now int64) bool {
	cutoff := now - int64(time.Hour/time.Millisecond)
	var kept []int64
	for _, t := range s.attemptTimestamps[userID] {
		if t >= cutoff {
			kept = append(kept, t)
		}
	}
	s.attemptTimestamps[userID] = kept
	if len(kept) >= 20 {
		return false
	}
	s.attemptTimestamps[userID] = append(kept, now)
	return true
}

func (s *Service) InitCheckout(
	ctx context.Context,
	subscriberUserID, deviceFP, ipFP string,
	payload CheckoutInitPayload,
) (CheckoutInitResponse, error) {
	subscriber := strings.TrimSpace(subscriberUserID)
	creator := strings.TrimSpace(payload.CreatorUserID)
	if subscriber == "" {
		return CheckoutInitResponse{}, ErrUnauthorized
	}
	if creator == "" {
		return CheckoutInitResponse{}, ErrBadRequest
	}
	if subscriber == creator {
		return CheckoutInitResponse{}, ErrSelfSubscribe
	}
	cycle := strings.ToLower(strings.TrimSpace(payload.BillingCycle))
	if cycle == "" {
		cycle = CycleMonthly
	}
	provider := strings.ToLower(strings.TrimSpace(payload.Provider))
	if !IsValidCycle(cycle) || (provider != "paystack" && provider != "paypal") {
		return CheckoutInitResponse{}, ErrBadRequest
	}
	idempKey := strings.TrimSpace(payload.IdempotencyKey)
	if !idempotencyPattern.MatchString(idempKey) {
		return CheckoutInitResponse{}, ErrBadRequest
	}

	country := s.resolveCountry(subscriber, payload.CountryISO)
	subUser := s.resolveUser(subscriber)
	creatorUser := s.resolveUser(creator)
	userEmail := strings.TrimSpace(subUser.Email)
	if userEmail == "" {
		userEmail = subscriber + "@users.telefam.company"
	}

	compKey := subscriber + ":" + idempKey
	now := s.nowMs()

	s.mu.Lock()
	if existing, ok := s.payByIdempKey[compKey]; ok {
		if existing.DTO.CreatorUserID != creator || existing.DTO.Provider != provider || existing.DTO.BillingCycle != cycle {
			s.mu.Unlock()
			return CheckoutInitResponse{}, ErrIdempotencyConflict
		}
		dto := existing.DTO
		s.mu.Unlock()
		offering, _ := s.GetCreatorOffering(subscriber, creator, country)
		return CheckoutInitResponse{
			Success:   true,
			Payment:   dto,
			Offering:  offering,
			ReturnURL: s.buildReturnURL(dto.Provider, dto.PaymentID, dto.ProviderReference),
		}, nil
	}

	if !s.checkRateLimitLocked(subscriber, now) {
		s.mu.Unlock()
		return CheckoutInitResponse{}, ErrRateLimited
	}
	plan := s.getOrCreatePlanLocked(creator)
	if !plan.Enabled {
		s.mu.Unlock()
		return CheckoutInitResponse{}, ErrPlanDisabled
	}

	pricing := ComputePlanPricing(plan.MonthlyUSDCents, plan.AnnualDiscountPct, country)
	amountUSDCents := pricing.MonthlyUSDCents
	if cycle == CycleAnnual {
		amountUSDCents = pricing.AnnualUSDCents
	}

	paymentID := newRandomID("subpay")
	reference := "TFSP_" + strings.ToUpper(strings.TrimPrefix(paymentID, "subpay_"))
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
	creatorTitle := creatorUser.FullName
	if strings.TrimSpace(creatorTitle) == "" {
		creatorTitle = creator
	}
	if provider == "paystack" {
		if psSecret == "" {
			return CheckoutInitResponse{}, ErrGatewayUnconfigured
		}
		checkoutURL, initErr = s.initPaystackTransaction(
			ctx, psBase, psSecret, subscriber, creator, userEmail, paymentID, reference, cycle, expectedMinor, expectedCurrency, idempKey, returnURL,
		)
	} else {
		if ppCID == "" || ppSec == "" {
			return CheckoutInitResponse{}, ErrGatewayUnconfigured
		}
		orderID, checkoutURL, initErr = s.initPayPalOrder(
			ctx, subscriber, creator, creatorTitle, paymentID, cycle, paypalValue, expectedCurrency, idempKey, returnURL,
		)
	}
	if initErr != nil {
		return CheckoutInitResponse{}, initErr
	}

	payDTO := SubscriptionPaymentDTO{
		PaymentID:           paymentID,
		CreatorUserID:       creator,
		BillingCycle:        cycle,
		Provider:            provider,
		ProviderReference:   reference,
		ProviderOrderID:     orderID,
		AmountUSDCents:      amountUSDCents,
		ExpectedAmountMinor: expectedMinor,
		ExpectedCurrency:    expectedCurrency,
		Status:              "pending",
		CheckoutURL:         checkoutURL,
		CreatedAtMs:         now,
	}
	payRec := &paymentRecord{
		DTO:              payDTO,
		SubscriberUserID: subscriber,
		CountryISO:       country,
		IdempotencyKey:   idempKey,
	}

	s.mu.Lock()
	s.payments[paymentID] = payRec
	s.payByIdempKey[compKey] = payRec
	s.payByRef[provider+":"+reference] = payRec
	if orderID != "" {
		s.payByOrderID[orderID] = payRec
	}
	s.mu.Unlock()

	offering, _ := s.GetCreatorOffering(subscriber, creator, country)
	return CheckoutInitResponse{
		Success:   true,
		Payment:   payDTO,
		Offering:  offering,
		ReturnURL: returnURL,
	}, nil
}

func (s *Service) VerifyCheckout(
	ctx context.Context,
	subscriberUserID, deviceFP, ipFP string,
	payload CheckoutVerifyPayload,
) (CheckoutVerifyResponse, error) {
	subscriber := strings.TrimSpace(subscriberUserID)
	if subscriber == "" {
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
	if payRec.SubscriberUserID != subscriber {
		s.mu.Unlock()
		return CheckoutVerifyResponse{}, ErrForbidden
	}
	creator := payRec.DTO.CreatorUserID
	country := payRec.CountryISO
	if payRec.DTO.Status == "completed" {
		subRec := s.subs[subscriber+":"+creator]
		var subDTO *SubscriptionRecordDTO
		if subRec != nil {
			d := s.buildSubDTOLocked(subRec, country, s.nowMs())
			subDTO = &d
		}
		dto := payRec.DTO
		s.mu.Unlock()
		offering, _ := s.GetCreatorOffering(subscriber, creator, country)
		return CheckoutVerifyResponse{
			Success:      true,
			Payment:      dto,
			Subscription: subDTO,
			Offering:     offering,
		}, nil
	}

	provider := payRec.DTO.Provider
	ref := payRec.DTO.ProviderReference
	orderID := payRec.DTO.ProviderOrderID
	if orderID == "" {
		orderID = strings.TrimSpace(payload.PayPalOrderID)
	}
	expectedMinor := payRec.DTO.ExpectedAmountMinor
	expectedCurrency := payRec.DTO.ExpectedCurrency
	paymentID := payRec.DTO.PaymentID
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

	payDTO, subDTO, newlyCompleted := s.finalizePayment(paymentID, paidMinor, paidCurr, "api_verify")
	if s.hooks.AuditEvent != nil {
		s.hooks.AuditEvent(subscriber, "creator_subscription_payment_verified", deviceFP, ipFP, map[string]string{
			"payment_id":      payDTO.PaymentID,
			"creator_user_id": creator,
			"billing_cycle":   payDTO.BillingCycle,
			"provider":        provider,
		})
	}
	if newlyCompleted {
		s.dispatchSubscriptionNotificationsAndWS(ctx, subscriber, creator, payDTO.BillingCycle, "activated")
	}
	offering, _ := s.GetCreatorOffering(subscriber, creator, country)
	return CheckoutVerifyResponse{
		Success:      true,
		Payment:      payDTO,
		Subscription: &subDTO,
		Offering:     offering,
	}, nil
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
	payDTO, _, newlyCompleted := s.finalizePayment(payRec.DTO.PaymentID, ev.Data.Amount, ev.Data.Currency, "webhook_paystack")
	if newlyCompleted {
		s.dispatchSubscriptionNotificationsAndWS(ctx, payRec.SubscriberUserID, payDTO.CreatorUserID, payDTO.BillingCycle, "activated")
	}
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
	payDTO, _, newlyCompleted := s.finalizePayment(payRec.DTO.PaymentID, payRec.DTO.ExpectedAmountMinor, payRec.DTO.ExpectedCurrency, "webhook_paypal")
	if newlyCompleted {
		s.dispatchSubscriptionNotificationsAndWS(ctx, payRec.SubscriberUserID, payDTO.CreatorUserID, payDTO.BillingCycle, "activated")
	}
	return nil
}

func (s *Service) finalizePayment(paymentID string, paidMinor int64, paidCurrency, via string) (SubscriptionPaymentDTO, SubscriptionRecordDTO, bool) {
	now := s.nowMs()
	s.mu.Lock()
	defer s.mu.Unlock()

	payRec := s.payments[paymentID]
	subKey := payRec.SubscriberUserID + ":" + payRec.DTO.CreatorUserID
	existingSub := s.subs[subKey]
	newlyCompleted := false

	durationMs := int64(30 * 24 * time.Hour / time.Millisecond)
	if payRec.DTO.BillingCycle == CycleAnnual {
		durationMs = int64(365 * 24 * time.Hour / time.Millisecond)
	}

	if payRec.DTO.Status != "completed" {
		newlyCompleted = true
		payRec.DTO.Status = "completed"
		payRec.DTO.PaidAmountMinor = paidMinor
		payRec.DTO.PaidCurrency = paidCurrency
		payRec.DTO.VerifiedVia = via
		payRec.DTO.CompletedAtMs = now

		if existingSub == nil {
			existingSub = &subscriptionRecord{
				SubscriptionID:     newRandomID("csub"),
				SubscriberUserID:   payRec.SubscriberUserID,
				CreatorUserID:      payRec.DTO.CreatorUserID,
				BillingCycle:       payRec.DTO.BillingCycle,
				Status:             StatusActive,
				AutoRenew:          true,
				AmountUSDCents:     payRec.DTO.AmountUSDCents,
				DisplayAmountMinor: paidMinor,
				DisplayCurrency:    payRec.DTO.ExpectedCurrency,
				CountryISO:         payRec.CountryISO,
				Provider:           payRec.DTO.Provider,
				StartedAtMs:        now,
				CurrentPeriodEndMs: now + durationMs,
				UpdatedAtMs:        now,
			}
			s.subs[subKey] = existingSub
		} else {
			baseEnd := now
			if existingSub.CurrentPeriodEndMs > now && (existingSub.Status == StatusActive || existingSub.Status == StatusCancelled) {
				baseEnd = existingSub.CurrentPeriodEndMs
			} else {
				existingSub.StartedAtMs = now
			}
			existingSub.BillingCycle = payRec.DTO.BillingCycle
			existingSub.Status = StatusActive
			existingSub.AutoRenew = true
			existingSub.CancelledAtMs = 0
			existingSub.AmountUSDCents = payRec.DTO.AmountUSDCents
			existingSub.DisplayAmountMinor = paidMinor
			existingSub.DisplayCurrency = payRec.DTO.ExpectedCurrency
			existingSub.CountryISO = payRec.CountryISO
			existingSub.Provider = payRec.DTO.Provider
			existingSub.CurrentPeriodEndMs = baseEnd + durationMs
			existingSub.UpdatedAtMs = now
		}
	}

	return payRec.DTO, s.buildSubDTOLocked(existingSub, payRec.CountryISO, now), newlyCompleted
}

func (s *Service) CancelSubscription(
	ctx context.Context,
	subscriberUserID, deviceFP, ipFP string,
	payload CancelPayload,
) (MutateSubscriptionResponse, error) {
	subscriber := strings.TrimSpace(subscriberUserID)
	creator := strings.TrimSpace(payload.CreatorUserID)
	if subscriber == "" {
		return MutateSubscriptionResponse{}, ErrUnauthorized
	}
	if creator == "" {
		return MutateSubscriptionResponse{}, ErrBadRequest
	}
	country := s.resolveCountry(subscriber, payload.CountryISO)
	now := s.nowMs()

	s.mu.Lock()
	subRec, ok := s.subs[subscriber+":"+creator]
	if !ok {
		s.mu.Unlock()
		return MutateSubscriptionResponse{}, ErrNotFound
	}
	s.refreshExpiryLocked(subRec, now)
	if subRec.Status == StatusExpired {
		dto := s.buildSubDTOLocked(subRec, country, now)
		s.mu.Unlock()
		return MutateSubscriptionResponse{
			Success:       true,
			Subscription:  &dto,
			Message:       "This subscription has already expired.",
			ServerEpochMs: now,
		}, nil
	}
	subRec.Status = StatusCancelled
	subRec.AutoRenew = false
	subRec.CancelledAtMs = now
	subRec.UpdatedAtMs = now
	dto := s.buildSubDTOLocked(subRec, country, now)
	cycle := subRec.BillingCycle
	s.mu.Unlock()

	if s.hooks.AuditEvent != nil {
		s.hooks.AuditEvent(subscriber, "creator_subscription_cancelled", deviceFP, ipFP, map[string]string{
			"creator_user_id": creator,
			"subscription_id": dto.SubscriptionID,
		})
	}
	s.dispatchSubscriptionNotificationsAndWS(ctx, subscriber, creator, cycle, "cancelled")

	return MutateSubscriptionResponse{
		Success:       true,
		Subscription:  &dto,
		Message:       "Auto-renewal cancelled. Access remains active until the end of the current period.",
		ServerEpochMs: now,
	}, nil
}

func (s *Service) RenewSubscription(
	ctx context.Context,
	subscriberUserID, deviceFP, ipFP string,
	payload RenewPayload,
) (MutateSubscriptionResponse, error) {
	subscriber := strings.TrimSpace(subscriberUserID)
	creator := strings.TrimSpace(payload.CreatorUserID)
	if subscriber == "" {
		return MutateSubscriptionResponse{}, ErrUnauthorized
	}
	if creator == "" || subscriber == creator {
		return MutateSubscriptionResponse{}, ErrBadRequest
	}
	country := s.resolveCountry(subscriber, payload.CountryISO)
	now := s.nowMs()
	reqCycle := strings.ToLower(strings.TrimSpace(payload.BillingCycle))

	s.mu.Lock()
	subRec := s.subs[subscriber+":"+creator]
	if subRec != nil {
		s.refreshExpiryLocked(subRec, now)
	}
	canReactivateInPlace := subRec != nil &&
		subRec.Status == StatusCancelled &&
		now < subRec.CurrentPeriodEndMs &&
		(reqCycle == "" || reqCycle == subRec.BillingCycle)

	if canReactivateInPlace {
		subRec.Status = StatusActive
		subRec.AutoRenew = true
		subRec.CancelledAtMs = 0
		subRec.UpdatedAtMs = now
		dto := s.buildSubDTOLocked(subRec, country, now)
		cycle := subRec.BillingCycle
		s.mu.Unlock()

		if s.hooks.AuditEvent != nil {
			s.hooks.AuditEvent(subscriber, "creator_subscription_autorenew_restored", deviceFP, ipFP, map[string]string{
				"creator_user_id": creator,
				"subscription_id": dto.SubscriptionID,
			})
		}
		s.dispatchSubscriptionNotificationsAndWS(ctx, subscriber, creator, cycle, "renewed")
		return MutateSubscriptionResponse{
			Success:          true,
			RequiresCheckout: false,
			Subscription:     &dto,
			Message:          "Auto-renewal restored for your subscription.",
			ServerEpochMs:    now,
		}, nil
	}

	defaultProvider := strings.ToLower(strings.TrimSpace(payload.Provider))
	if defaultProvider == "" && subRec != nil {
		defaultProvider = subRec.Provider
	}
	if defaultProvider == "" {
		defaultProvider = "paystack"
	}
	if reqCycle == "" && subRec != nil {
		reqCycle = subRec.BillingCycle
	}
	if reqCycle == "" {
		reqCycle = CycleMonthly
	}
	s.mu.Unlock()

	idempKey := strings.TrimSpace(payload.IdempotencyKey)
	if idempKey == "" {
		return MutateSubscriptionResponse{}, ErrBadRequest
	}

	initResp, err := s.InitCheckout(ctx, subscriber, deviceFP, ipFP, CheckoutInitPayload{
		CreatorUserID:  creator,
		BillingCycle:   reqCycle,
		Provider:       defaultProvider,
		CountryISO:     country,
		IdempotencyKey: idempKey,
	})
	if err != nil {
		return MutateSubscriptionResponse{}, err
	}

	return MutateSubscriptionResponse{
		Success:          true,
		RequiresCheckout: true,
		Checkout:         &initResp,
		Message:          "Complete checkout to renew your subscription.",
		ServerEpochMs:    s.nowMs(),
	}, nil
}

func (s *Service) dispatchSubscriptionNotificationsAndWS(ctx context.Context, subscriberUserID, creatorUserID, cycle, action string) {
	subUser := s.resolveUser(subscriberUserID)
	creatorUser := s.resolveUser(creatorUserID)

	if s.hooks.SendWSEvent != nil {
		ev := map[string]any{
			"type": "subscription.updated",
			"data": map[string]any{
				"subscriber_user_id": subscriberUserID,
				"creator_user_id":    creatorUserID,
				"billing_cycle":      cycle,
				"action":             action,
			},
		}
		s.hooks.SendWSEvent(subscriberUserID, ev)
		s.hooks.SendWSEvent(creatorUserID, ev)
	}

	if s.hooks.NotifySubscriptionEvent != nil {
		creatorName := strings.TrimSpace(creatorUser.FullName)
		if creatorName == "" {
			creatorName = "@" + creatorUser.Username
		}
		subName := strings.TrimSpace(subUser.FullName)
		if subName == "" {
			subName = "@" + subUser.Username
		}
		switch action {
		case "activated":
			s.hooks.NotifySubscriptionEvent(
				ctx,
				creatorUserID,
				subUser,
				"new_subscriber",
				"New subscriber",
				fmt.Sprintf("%s subscribed to your %s plan.", subName, cycle),
				cycle,
			)
			s.hooks.NotifySubscriptionEvent(
				ctx,
				subscriberUserID,
				creatorUser,
				"subscription_update",
				"Subscription active",
				fmt.Sprintf("Your %s subscription to %s is active.", cycle, creatorName),
				cycle,
			)
		case "cancelled":
			s.hooks.NotifySubscriptionEvent(
				ctx,
				subscriberUserID,
				creatorUser,
				"subscription_update",
				"Subscription updated",
				fmt.Sprintf("Auto-renewal cancelled for %s.", creatorName),
				cycle,
			)
		case "renewed":
			s.hooks.NotifySubscriptionEvent(
				ctx,
				subscriberUserID,
				creatorUser,
				"subscription_update",
				"Subscription renewed",
				fmt.Sprintf("Auto-renewal is active again for %s.", creatorName),
				cycle,
			)
		}
	}
}

func (s *Service) buildReturnURL(provider, paymentID, reference string) string {
	return fmt.Sprintf(
		"%s/v1/subscriptions/payment-return?provider=%s&payment_id=%s&reference=%s",
		s.publicBaseURL,
		url.QueryEscape(provider),
		url.QueryEscape(paymentID),
		url.QueryEscape(reference),
	)
}

func (s *Service) initPaystackTransaction(
	ctx context.Context,
	baseURL, secret, subscriberID, creatorID, email, paymentID, reference, cycle string,
	amountMinor int64, currency, idempKey, callbackURL string,
) (string, error) {
	bodyMap := map[string]any{
		"email":        email,
		"amount":       amountMinor,
		"currency":     currency,
		"reference":    reference,
		"callback_url": callbackURL,
		"metadata": map[string]any{
			"subscriber_user_id": subscriberID,
			"creator_user_id":    creatorID,
			"payment_id":         paymentID,
			"billing_cycle":      cycle,
			"idempotency_key":    idempKey,
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
	subscriberID, creatorID, creatorName, paymentID, cycle, amountVal, currency, idempKey, returnURL string,
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
				"custom_id":    fmt.Sprintf("%s|%s|%s|%s", subscriberID, creatorID, paymentID, cycle),
				"description":  fmt.Sprintf("Telefam Subscription to %s (%s)", creatorName, cycle),
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
