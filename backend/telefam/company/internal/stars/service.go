package stars

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
	ErrInsufficientBalance = errors.New("insufficient star balance")
	ErrIdempotencyConflict = errors.New("idempotency key conflict")
	ErrRateLimited         = errors.New("transfer rate limit exceeded")
	ErrGatewayUnconfigured = errors.New("payment gateway not configured")
	ErrPaymentFailed       = errors.New("payment verification failed")
)

var StandardStarPackages = []int64{
	5, 10, 20, 50, 100, 200, 500, 1000, 2000, 5000, 10000, 50000,
}

var idempotencyPattern = regexp.MustCompile(`^[A-Za-z0-9._:-]{8,128}$`)

type UserInfo struct {
	UserID     string
	FullName   string
	Username   string
	AvatarURL  string
	Email      string
	CountryISO string
}

type HostHooks struct {
	ResolveUser         func(userID string) (UserInfo, bool)
	IsBlocked           func(a, b string) bool
	OpenDirectConv      func(callerID, peerID string) (string, error)
	AppendStarChatMsg   func(convID, senderID, peerID, metaJSON, clientOpID string) (string, error)
	SendWSEvent         func(targetUserID string, event map[string]any)
	NotifyStarReceived  func(ctx context.Context, senderID, recipientID, convID, messageID string, stars int64)
	AuditEvent          func(userID, action, deviceFP, ipFP string, meta map[string]string)
}

type StarWalletDTO struct {
	UserID             string `json:"user_id"`
	BalanceStars       int64  `json:"balance_stars"`
	StarsReceivedTotal int64  `json:"stars_received_total"`
	StarsSentTotal     int64  `json:"stars_sent_total"`
	UpdatedAtMs        int64  `json:"updated_at_ms"`
}

type StarPackageDTO struct {
	Stars             int64  `json:"stars"`
	IsPopular         bool   `json:"is_popular"`
	DisplayAmount     string `json:"display_amount"`
	DisplayCurrency   string `json:"display_currency"`
	PaystackAmountMin int64  `json:"paystack_amount_minor"`
	PaystackCurrency  string `json:"paystack_currency"`
	PayPalAmountValue string `json:"paypal_amount_value"`
	PayPalCurrency    string `json:"paypal_currency"`
}

type StarSenderSummaryDTO struct {
	SenderUserID     string `json:"sender_user_id"`
	SenderFullName   string `json:"sender_full_name"`
	SenderUsername   string `json:"sender_username"`
	SenderAvatarURL  string `json:"sender_avatar_url,omitempty"`
	TotalStarsSent   int64  `json:"total_stars_sent"`
	GiftCount        int    `json:"gift_count"`
	LastSentAtMs     int64  `json:"last_sent_at_ms"`
}

type StarLedgerItemDTO struct {
	EntryID              string `json:"entry_id"`
	EntryType            string `json:"entry_type"` // purchase | sent | received
	DeltaStars           int64  `json:"delta_stars"`
	BalanceAfter         int64  `json:"balance_after"`
	CounterpartyUserID   string `json:"counterparty_user_id,omitempty"`
	CounterpartyName     string `json:"counterparty_name,omitempty"`
	CounterpartyUsername string `json:"counterparty_username,omitempty"`
	CounterpartyAvatar   string `json:"counterparty_avatar,omitempty"`
	Provider             string `json:"provider,omitempty"`
	AmountMinor          int64  `json:"amount_minor,omitempty"`
	Currency             string `json:"currency,omitempty"`
	PaymentID            string `json:"payment_id,omitempty"`
	MessageID            string `json:"message_id,omitempty"`
	ConversationID       string `json:"conversation_id,omitempty"`
	CreatedAtMs          int64  `json:"created_at_ms"`
}

type StarPaymentDTO struct {
	PaymentID           string `json:"payment_id"`
	Provider            string `json:"provider"`
	ProviderReference   string `json:"provider_reference"`
	ProviderOrderID     string `json:"provider_order_id,omitempty"`
	PackageStars        int64  `json:"package_stars"`
	ExpectedAmountMinor int64  `json:"expected_amount_minor"`
	ExpectedCurrency    string `json:"expected_currency"`
	PaidAmountMinor     int64  `json:"paid_amount_minor"`
	PaidCurrency        string `json:"paid_currency,omitempty"`
	Status              string `json:"status"`
	CheckoutURL         string `json:"checkout_url,omitempty"`
	Purpose             string `json:"purpose"`
	PeerUserID          string `json:"peer_user_id,omitempty"`
	ConversationID      string `json:"conversation_id,omitempty"`
	VerifiedVia         string `json:"verified_via,omitempty"`
	CreatedAtMs         int64  `json:"created_at_ms"`
	CompletedAtMs       int64  `json:"completed_at_ms,omitempty"`
}

type StarsSummaryResponse struct {
	Success        bool                   `json:"success"`
	Wallet         StarWalletDTO          `json:"wallet"`
	CountryISO     string                 `json:"country_iso"`
	DisplayRateKES float64                `json:"display_rate_kes"`
	Packages       []StarPackageDTO       `json:"packages"`
	Senders        []StarSenderSummaryDTO `json:"senders"`
	History        []StarLedgerItemDTO    `json:"history"`
	ServerEpochMs  int64                  `json:"server_epoch_ms"`
}

type CheckoutInitPayload struct {
	Provider       string `json:"provider"` // paystack | paypal
	PackageStars   int64  `json:"package_stars"`
	CountryISO     string `json:"country_iso"`
	IdempotencyKey string `json:"idempotency_key"`
	Purpose        string `json:"purpose,omitempty"` // topup | send_gift
	PeerUserID     string `json:"peer_user_id,omitempty"`
	ConversationID string `json:"conversation_id,omitempty"`
}

type CheckoutInitResponse struct {
	Success   bool           `json:"success"`
	Payment   StarPaymentDTO `json:"payment"`
	Wallet    StarWalletDTO  `json:"wallet"`
	ReturnURL string         `json:"return_url"`
}

type CheckoutVerifyPayload struct {
	PaymentID      string `json:"payment_id"`
	Reference      string `json:"reference,omitempty"`
	PayPalOrderID  string `json:"paypal_order_id,omitempty"`
	IdempotencyKey string `json:"idempotency_key"`
}

type CheckoutVerifyResponse struct {
	Success       bool           `json:"success"`
	Credited      bool           `json:"credited"`
	Payment       StarPaymentDTO `json:"payment"`
	Wallet        StarWalletDTO  `json:"wallet"`
	ServerEpochMs int64          `json:"server_epoch_ms"`
}

type SendStarsPayload struct {
	RecipientUserID string `json:"recipient_user_id"`
	ConversationID  string `json:"conversation_id,omitempty"`
	Stars           int64  `json:"stars"`
	IdempotencyKey  string `json:"idempotency_key"`
}

type SendStarsResponse struct {
	Success          bool          `json:"success"`
	MessageID        string        `json:"message_id"`
	ConversationID   string        `json:"conversation_id"`
	StarsSent        int64         `json:"stars_sent"`
	SenderWallet     StarWalletDTO `json:"sender_wallet"`
	InsufficientBal  bool          `json:"insufficient_balance,omitempty"`
	RequiredBuyStars int64         `json:"required_buy_stars,omitempty"`
	ServerEpochMs    int64         `json:"server_epoch_ms"`
}

type starGiftChatMeta struct {
	Type           string `json:"type"`
	Stars          int64  `json:"stars"`
	SenderUserID   string `json:"sender_user_id"`
	SenderName     string `json:"sender_name"`
	RecipientID    string `json:"recipient_user_id"`
	RecipientName  string `json:"recipient_name"`
	AnimationTier  int    `json:"animation_tier"`
	LedgerEntryID  string `json:"ledger_entry_id"`
	IdempotencyKey string `json:"idempotency_key"`
	ServerSentAtMs int64  `json:"server_sent_at_ms"`
}

type walletRecord struct {
	UserID             string
	BalanceStars       int64
	StarsReceivedTotal int64
	StarsSentTotal     int64
	UpdatedAtMs        int64
}

type paymentRecord struct {
	DTO            StarPaymentDTO
	UserID         string
	CountryISO     string
	IdempotencyKey string
}

type ledgerRecord struct {
	DTO            StarLedgerItemDTO
	UserID         string
	IdempotencyKey string
}

type sendVelocityEntry struct {
	RecipientUserID string
	DeviceFP        string
	IPFP            string
	Stars           int64
	TimestampMs     int64
}

type paymentAttemptEntry struct {
	Stars       int64
	TimestampMs int64
}

type amlViolation struct {
	UserID      string
	PeerUserID  string
	Rule        string
	StarsAmount int64
	Details     string
	TimestampMs int64
}

type Service struct {
	hooks          HostHooks
	mu             sync.Mutex
	wallets        map[string]*walletRecord
	payments       map[string]*paymentRecord
	payByIdempKey  map[string]*paymentRecord // userID + ":" + idempotencyKey
	payByRef       map[string]*paymentRecord // provider + ":" + reference
	payByOrderID   map[string]*paymentRecord // paypal orderID -> payment
	ledgerByUser   map[string][]ledgerRecord
	sendIdempCache map[string]SendStarsResponse // userID + ":" + idempotencyKey
	sendLogsByUser map[string][]sendVelocityEntry
	buyLogsByUser  map[string][]paymentAttemptEntry
	amlLog         []amlViolation

	httpClient     *http.Client
	paystackBase   string
	paystackSecret string
	paypalBase     string
	paypalClientID string
	paypalSecret   string
	paypalWebhook  string
	publicBaseURL  string

	paypalAccessToken string
	paypalTokenExpiry time.Time

	nowFunc func() time.Time
}

func NewService(hooks HostHooks) *Service {
	paypalBase := strings.TrimSpace(os.Getenv("PAYPAL_API_BASE"))
	if paypalBase == "" {
		paypalBase = "https://api-m.sandbox.paypal.com"
	}
	publicBase := strings.TrimSpace(os.Getenv("TELEFAM_PUBLIC_BASE_URL"))
	if publicBase == "" {
		publicBase = "https://telefam.company"
	}
	return &Service{
		hooks:          hooks,
		wallets:        make(map[string]*walletRecord),
		payments:       make(map[string]*paymentRecord),
		payByIdempKey:  make(map[string]*paymentRecord),
		payByRef:       make(map[string]*paymentRecord),
		payByOrderID:   make(map[string]*paymentRecord),
		ledgerByUser:   make(map[string][]ledgerRecord),
		sendIdempCache: make(map[string]SendStarsResponse),
		sendLogsByUser: make(map[string][]sendVelocityEntry),
		buyLogsByUser:  make(map[string][]paymentAttemptEntry),
		httpClient:     &http.Client{Timeout: 12 * time.Second},
		paystackBase:   "https://api.paystack.co",
		paystackSecret: strings.TrimSpace(os.Getenv("PAYSTACK_SECRET_KEY")),
		paypalBase:     strings.TrimRight(paypalBase, "/"),
		paypalClientID: strings.TrimSpace(os.Getenv("PAYPAL_CLIENT_ID")),
		paypalSecret:   strings.TrimSpace(os.Getenv("PAYPAL_CLIENT_SECRET")),
		paypalWebhook:  strings.TrimSpace(os.Getenv("PAYPAL_WEBHOOK_ID")),
		publicBaseURL:  strings.TrimRight(publicBase, "/"),
		nowFunc:        time.Now,
	}
}

func (s *Service) ConfigureGatewaysForTest(
	paystackBase, paystackSecret, paypalBase, paypalClientID, paypalSecret, paypalWebhook, publicBase string,
) {
	s.mu.Lock()
	defer s.mu.Unlock()
	if paystackBase != "" {
		s.paystackBase = strings.TrimRight(paystackBase, "/")
	}
	s.paystackSecret = paystackSecret
	if paypalBase != "" {
		s.paypalBase = strings.TrimRight(paypalBase, "/")
	}
	s.paypalClientID = paypalClientID
	s.paypalSecret = paypalSecret
	s.paypalWebhook = paypalWebhook
	if publicBase != "" {
		s.publicBaseURL = strings.TrimRight(publicBase, "/")
	}
	s.paypalAccessToken = ""
	s.paypalTokenExpiry = time.Time{}
}

func (s *Service) nowMs() int64 {
	return s.nowFunc().UTC().UnixMilli()
}

func newRandomID(prefix string) string {
	var b [12]byte
	_, _ = rand.Read(b[:])
	return prefix + "_" + hex.EncodeToString(b[:])
}

func IsValidPackageStars(stars int64) bool {
	for _, p := range StandardStarPackages {
		if p == stars {
			return true
		}
	}
	return false
}

func normalizeIdempotencyKey(raw string) (string, bool) {
	clean := strings.TrimSpace(raw)
	if !idempotencyPattern.MatchString(clean) {
		return "", false
	}
	return clean, true
}

func AnimationTierForStars(stars int64) int {
	switch {
	case stars >= 10000:
		return 5
	case stars >= 1000:
		return 4
	case stars >= 200:
		return 3
	case stars >= 50:
		return 2
	default:
		return 1
	}
}

type pricingSpec struct {
	PaystackCurrency string
	PaystackMinorPer float64 // minor units per star (100 = 1 unit)
	PayPalCurrency   string
	USDPerStar       float64
	DisplayCurrency  string
	DisplaySymbol    string
	DisplayPerStar   float64
}

func pricingForCountry(countryISO string) pricingSpec {
	iso := strings.ToUpper(strings.TrimSpace(countryISO))
	// Canonical value: ~ $0.008 USD per star (= 1.00 KES per star, matching screenshots cleanly)
	switch iso {
	case "KE":
		return pricingSpec{
			PaystackCurrency: "KES",
			PaystackMinorPer: 100.0,
			PayPalCurrency:   "USD",
			USDPerStar:       0.008,
			DisplayCurrency:  "KES",
			DisplaySymbol:    "Ksh ",
			DisplayPerStar:   1.0,
		}
	case "NG":
		return pricingSpec{
			PaystackCurrency: "NGN",
			PaystackMinorPer: 1200.0,
			PayPalCurrency:   "USD",
			USDPerStar:       0.008,
			DisplayCurrency:  "NGN",
			DisplaySymbol:    "NGN ",
			DisplayPerStar:   12.0,
		}
	case "GH":
		return pricingSpec{
			PaystackCurrency: "GHS",
			PaystackMinorPer: 12.0,
			PayPalCurrency:   "USD",
			USDPerStar:       0.008,
			DisplayCurrency:  "GHS",
			DisplaySymbol:    "GHc ",
			DisplayPerStar:   0.12,
		}
	case "ZA":
		return pricingSpec{
			PaystackCurrency: "ZAR",
			PaystackMinorPer: 15.0,
			PayPalCurrency:   "USD",
			USDPerStar:       0.008,
			DisplayCurrency:  "ZAR",
			DisplaySymbol:    "R ",
			DisplayPerStar:   0.15,
		}
	case "GB":
		return pricingSpec{
			PaystackCurrency: "USD",
			PaystackMinorPer: 0.8,
			PayPalCurrency:   "GBP",
			USDPerStar:       0.008,
			DisplayCurrency:  "GBP",
			DisplaySymbol:    "GBP ",
			DisplayPerStar:   0.0064,
		}
	case "DE", "FR", "ES", "IT", "NL", "IE", "BE", "PT", "AT", "FI":
		return pricingSpec{
			PaystackCurrency: "USD",
			PaystackMinorPer: 0.8,
			PayPalCurrency:   "EUR",
			USDPerStar:       0.008,
			DisplayCurrency:  "EUR",
			DisplaySymbol:    "EUR ",
			DisplayPerStar:   0.0074,
		}
	default:
		return pricingSpec{
			PaystackCurrency: "USD",
			PaystackMinorPer: 0.8,
			PayPalCurrency:   "USD",
			USDPerStar:       0.008,
			DisplayCurrency:  "USD",
			DisplaySymbol:    "$",
			DisplayPerStar:   0.008,
		}
	}
}

func computePackagePricing(stars int64, countryISO string) StarPackageDTO {
	spec := pricingForCountry(countryISO)

	paystackMinor := int64(math.Round(float64(stars) * spec.PaystackMinorPer))
	if paystackMinor < 5 {
		paystackMinor = 5
	}

	var ppFactor float64
	switch spec.PayPalCurrency {
	case "GBP":
		ppFactor = 0.0064
	case "EUR":
		ppFactor = 0.0074
	default:
		ppFactor = spec.USDPerStar
	}
	paypalMinor := int64(math.Round(float64(stars) * ppFactor * 100.0))
	if paypalMinor < 5 {
		paypalMinor = 5
	}

	dispRaw := float64(stars) * spec.DisplayPerStar
	var dispLabel string
	if spec.DisplayPerStar >= 1.0 && math.Abs(dispRaw-math.Round(dispRaw)) < 0.001 {
		dispLabel = spec.DisplaySymbol + formatWithCommas(int64(math.Round(dispRaw)))
	} else {
		dispLabel = fmt.Sprintf("%s%.2f", spec.DisplaySymbol, math.Max(0.05, dispRaw))
	}

	return StarPackageDTO{
		Stars:             stars,
		IsPopular:         stars == 5,
		DisplayAmount:     dispLabel,
		DisplayCurrency:   spec.DisplayCurrency,
		PaystackAmountMin: paystackMinor,
		PaystackCurrency:  spec.PaystackCurrency,
		PayPalAmountValue: fmt.Sprintf("%.2f", float64(paypalMinor)/100.0),
		PayPalCurrency:    spec.PayPalCurrency,
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

func (s *Service) getOrCreateWalletLocked(userID string) *walletRecord {
	w, ok := s.wallets[userID]
	if !ok {
		w = &walletRecord{
			UserID:      userID,
			UpdatedAtMs: s.nowMs(),
		}
		s.wallets[userID] = w
	}
	return w
}

func (w *walletRecord) toDTO() StarWalletDTO {
	return StarWalletDTO{
		UserID:             w.UserID,
		BalanceStars:       w.BalanceStars,
		StarsReceivedTotal: w.StarsReceivedTotal,
		StarsSentTotal:     w.StarsSentTotal,
		UpdatedAtMs:        w.UpdatedAtMs,
	}
}

func (s *Service) GetSummary(userID, countryOverride string) (StarsSummaryResponse, error) {
	if strings.TrimSpace(userID) == "" {
		return StarsSummaryResponse{}, ErrUnauthorized
	}
	country := strings.ToUpper(strings.TrimSpace(countryOverride))
	if country == "" && s.hooks.ResolveUser != nil {
		if info, ok := s.hooks.ResolveUser(userID); ok && strings.TrimSpace(info.CountryISO) != "" {
			country = strings.ToUpper(strings.TrimSpace(info.CountryISO))
		}
	}
	if country == "" {
		country = "US"
	}

	pkgs := make([]StarPackageDTO, 0, len(StandardStarPackages))
	for _, p := range StandardStarPackages {
		pkgs = append(pkgs, computePackagePricing(p, country))
	}

	s.mu.Lock()
	wallet := s.getOrCreateWalletLocked(userID).toDTO()
	ledger := append([]ledgerRecord(nil), s.ledgerByUser[userID]...)
	s.mu.Unlock()

	senderAgg := make(map[string]*StarSenderSummaryDTO)
	history := make([]StarLedgerItemDTO, 0, len(ledger))
	for i := len(ledger) - 1; i >= 0; i-- {
		rec := ledger[i].DTO
		if rec.CounterpartyUserID != "" && s.hooks.ResolveUser != nil {
			if info, ok := s.hooks.ResolveUser(rec.CounterpartyUserID); ok {
				rec.CounterpartyName = info.FullName
				rec.CounterpartyUsername = info.Username
				rec.CounterpartyAvatar = info.AvatarURL
			}
		}
		history = append(history, rec)
		if rec.EntryType == "received" && rec.CounterpartyUserID != "" {
			agg, exists := senderAgg[rec.CounterpartyUserID]
			if !exists {
				agg = &StarSenderSummaryDTO{
					SenderUserID:    rec.CounterpartyUserID,
					SenderFullName:  rec.CounterpartyName,
					SenderUsername:  rec.CounterpartyUsername,
					SenderAvatarURL: rec.CounterpartyAvatar,
					LastSentAtMs:    rec.CreatedAtMs,
				}
				if agg.SenderFullName == "" {
					agg.SenderFullName = rec.CounterpartyUserID
				}
				senderAgg[rec.CounterpartyUserID] = agg
			}
			agg.TotalStarsSent += rec.DeltaStars
			agg.GiftCount++
			if rec.CreatedAtMs > agg.LastSentAtMs {
				agg.LastSentAtMs = rec.CreatedAtMs
			}
		}
	}

	senders := make([]StarSenderSummaryDTO, 0, len(senderAgg))
	for _, a := range senderAgg {
		senders = append(senders, *a)
	}
	sort.Slice(senders, func(i, j int) bool {
		if senders[i].TotalStarsSent != senders[j].TotalStarsSent {
			return senders[i].TotalStarsSent > senders[j].TotalStarsSent
		}
		return senders[i].LastSentAtMs > senders[j].LastSentAtMs
	})

	return StarsSummaryResponse{
		Success:        true,
		Wallet:         wallet,
		CountryISO:     country,
		DisplayRateKES: 1.0,
		Packages:       pkgs,
		Senders:        senders,
		History:        history,
		ServerEpochMs:  s.nowMs(),
	}, nil
}

// InitCheckout creates a server-priced Paystack or PayPal payment session with strict idempotency & AML checks.
func (s *Service) InitCheckout(
	ctx context.Context,
	userID, deviceFP, ipFP string,
	payload CheckoutInitPayload,
) (CheckoutInitResponse, error) {
	if strings.TrimSpace(userID) == "" {
		return CheckoutInitResponse{}, ErrUnauthorized
	}
	provider := strings.ToLower(strings.TrimSpace(payload.Provider))
	if provider != "paystack" && provider != "paypal" {
		return CheckoutInitResponse{}, ErrBadRequest
	}
	if !IsValidPackageStars(payload.PackageStars) {
		return CheckoutInitResponse{}, ErrBadRequest
	}
	idempKey, ok := normalizeIdempotencyKey(payload.IdempotencyKey)
	if !ok {
		return CheckoutInitResponse{}, ErrBadRequest
	}

	country := strings.ToUpper(strings.TrimSpace(payload.CountryISO))
	var userEmail string
	if s.hooks.ResolveUser != nil {
		if info, found := s.hooks.ResolveUser(userID); found {
			userEmail = strings.TrimSpace(info.Email)
			if country == "" && strings.TrimSpace(info.CountryISO) != "" {
				country = strings.ToUpper(strings.TrimSpace(info.CountryISO))
			}
		}
	}
	if country == "" {
		country = "US"
	}
	if userEmail == "" {
		userEmail = userID + "@users.telefam.company"
	}

	idempComposite := userID + ":" + idempKey
	now := s.nowMs()

	s.mu.Lock()
	if existing, exists := s.payByIdempKey[idempComposite]; exists {
		if existing.DTO.Provider != provider || existing.DTO.PackageStars != payload.PackageStars {
			s.mu.Unlock()
			return CheckoutInitResponse{}, ErrIdempotencyConflict
		}
		w := s.getOrCreateWalletLocked(userID).toDTO()
		dto := existing.DTO
		s.mu.Unlock()
		return CheckoutInitResponse{
			Success:   true,
			Payment:   dto,
			Wallet:    w,
			ReturnURL: s.buildReturnURL(dto.Provider, dto.PaymentID, dto.ProviderReference),
		}, nil
	}

	// AML / velocity check for purchases: max 8 checkout inits per 10 min, max 100,000 purchased stars / 24h
	cutoff10m := now - int64(10*time.Minute/time.Millisecond)
	cutoff24h := now - int64(24*time.Hour/time.Millisecond)
	recentCount := 0
	var dailyPurchased int64
	pruned := make([]paymentAttemptEntry, 0, len(s.buyLogsByUser[userID]))
	for _, item := range s.buyLogsByUser[userID] {
		if item.TimestampMs >= cutoff24h {
			pruned = append(pruned, item)
			if item.TimestampMs >= cutoff10m {
				recentCount++
			}
		}
	}
	for _, rec := range s.ledgerByUser[userID] {
		if rec.DTO.EntryType == "purchase" && rec.DTO.CreatedAtMs >= cutoff24h {
			dailyPurchased += rec.DTO.DeltaStars
		}
	}
	s.buyLogsByUser[userID] = pruned

	if recentCount >= 8 {
		s.recordAMLViolationLocked(userID, "", "checkout_velocity_10m", payload.PackageStars, "Too many payment attempts in 10 minutes", deviceFP, ipFP)
		s.mu.Unlock()
		return CheckoutInitResponse{}, ErrRateLimited
	}
	if dailyPurchased+payload.PackageStars > 100000 {
		s.recordAMLViolationLocked(userID, "", "daily_purchase_cap_100k", payload.PackageStars, "Exceeded 100,000 daily Star purchase cap", deviceFP, ipFP)
		s.mu.Unlock()
		return CheckoutInitResponse{}, ErrRateLimited
	}

	pkg := computePackagePricing(payload.PackageStars, country)
	paymentID := newRandomID("starpay")
	reference := "TFST_" + strings.ToUpper(strings.TrimPrefix(paymentID, "starpay_"))
	returnURL := s.buildReturnURL(provider, paymentID, reference)

	var expectedMinor int64
	var expectedCurrency string
	if provider == "paystack" {
		expectedMinor = pkg.PaystackAmountMin
		expectedCurrency = pkg.PaystackCurrency
	} else {
		fVal, _ := strconv.ParseFloat(pkg.PayPalAmountValue, 64)
		expectedMinor = int64(math.Round(fVal * 100.0))
		expectedCurrency = pkg.PayPalCurrency
	}

	paystackSecret := s.paystackSecret
	paystackBase := s.paystackBase
	paypalClient := s.paypalClientID
	paypalSecret := s.paypalSecret
	s.buyLogsByUser[userID] = append(s.buyLogsByUser[userID], paymentAttemptEntry{
		Stars:       payload.PackageStars,
		TimestampMs: now,
	})
	s.mu.Unlock()

	var checkoutURL string
	var providerOrderID string
	var initErr error

	if provider == "paystack" {
		if paystackSecret == "" {
			return CheckoutInitResponse{}, ErrGatewayUnconfigured
		}
		checkoutURL, initErr = s.initPaystackTransaction(
			ctx, paystackBase, paystackSecret, userID, userEmail, paymentID, reference,
			payload.PackageStars, expectedMinor, expectedCurrency, idempKey, returnURL,
		)
	} else {
		if paypalClient == "" || paypalSecret == "" {
			return CheckoutInitResponse{}, ErrGatewayUnconfigured
		}
		providerOrderID, checkoutURL, initErr = s.initPayPalOrder(
			ctx, userID, paymentID, payload.PackageStars, pkg.PayPalAmountValue, expectedCurrency, idempKey, returnURL,
		)
	}
	if initErr != nil {
		return CheckoutInitResponse{}, initErr
	}

	purpose := strings.TrimSpace(payload.Purpose)
	if purpose != "send_gift" {
		purpose = "topup"
	}

	dto := StarPaymentDTO{
		PaymentID:           paymentID,
		Provider:            provider,
		ProviderReference:   reference,
		ProviderOrderID:     providerOrderID,
		PackageStars:        payload.PackageStars,
		ExpectedAmountMinor: expectedMinor,
		ExpectedCurrency:    expectedCurrency,
		Status:              "pending",
		CheckoutURL:         checkoutURL,
		Purpose:             purpose,
		PeerUserID:          strings.TrimSpace(payload.PeerUserID),
		ConversationID:      strings.TrimSpace(payload.ConversationID),
		CreatedAtMs:         now,
	}

	rec := &paymentRecord{
		DTO:            dto,
		UserID:         userID,
		CountryISO:     country,
		IdempotencyKey: idempKey,
	}

	s.mu.Lock()
	s.payments[paymentID] = rec
	s.payByIdempKey[idempComposite] = rec
	s.payByRef[provider+":"+reference] = rec
	if providerOrderID != "" {
		s.payByOrderID[providerOrderID] = rec
	}
	wallet := s.getOrCreateWalletLocked(userID).toDTO()
	s.mu.Unlock()

	if s.hooks.AuditEvent != nil {
		s.hooks.AuditEvent(userID, "stars_checkout_init", deviceFP, ipFP, map[string]string{
			"payment_id": paymentID,
			"provider":   provider,
			"stars":      strconv.FormatInt(payload.PackageStars, 10),
			"currency":   expectedCurrency,
		})
	}

	return CheckoutInitResponse{
		Success:   true,
		Payment:   dto,
		Wallet:    wallet,
		ReturnURL: returnURL,
	}, nil
}

func (s *Service) buildReturnURL(provider, paymentID, reference string) string {
	return fmt.Sprintf(
		"%s/v1/stars/payment-return?provider=%s&payment_id=%s&reference=%s",
		s.publicBaseURL,
		url.QueryEscape(provider),
		url.QueryEscape(paymentID),
		url.QueryEscape(reference),
	)
}

func (s *Service) initPaystackTransaction(
	ctx context.Context,
	baseURL, secret, userID, email, paymentID, reference string,
	stars, amountMinor int64, currency, idempKey, callbackURL string,
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
			"package_stars":   stars,
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
		Status  bool   `json:"status"`
		Message string `json:"message"`
		Data    struct {
			AuthorizationURL string `json:"authorization_url"`
			Reference        string `json:"reference"`
		} `json:"data"`
	}
	if err := json.NewDecoder(io.LimitReader(resp.Body, 1<<20)).Decode(&parsed); err != nil {
		return "", ErrPaymentFailed
	}
	if resp.StatusCode < 200 || resp.StatusCode >= 300 || !parsed.Status || strings.TrimSpace(parsed.Data.AuthorizationURL) == "" {
		return "", ErrPaymentFailed
	}
	return parsed.Data.AuthorizationURL, nil
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
	creds := base64.StdEncoding.EncodeToString([]byte(cid + ":" + sec))
	req.Header.Set("Authorization", "Basic "+creds)

	resp, err := s.httpClient.Do(req)
	if err != nil {
		return "", ErrPaymentFailed
	}
	defer resp.Body.Close()

	var parsed struct {
		AccessToken string `json:"access_token"`
		ExpiresIn   int64  `json:"expires_in"`
	}
	if err := json.NewDecoder(io.LimitReader(resp.Body, 1<<20)).Decode(&parsed); err != nil || parsed.AccessToken == "" {
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
	userID, paymentID string,
	stars int64, amountVal, currency, idempKey, returnURL string,
) (orderID string, approveURL string, err error) {
	token, err := s.getPayPalToken(ctx)
	if err != nil {
		return "", "", err
	}
	orderBody := map[string]any{
		"intent": "CAPTURE",
		"purchase_units": []map[string]any{
			{
				"reference_id": paymentID,
				"custom_id":    fmt.Sprintf("%s|%s|%d", userID, paymentID, stars),
				"description":  fmt.Sprintf("Telefam %d Stars", stars),
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
	req.Header.Set("Authorization", "Bearer "+token)
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
	if err := json.NewDecoder(io.LimitReader(resp.Body, 1<<20)).Decode(&parsed); err != nil {
		return "", "", ErrPaymentFailed
	}
	if resp.StatusCode < 200 || resp.StatusCode >= 300 || parsed.ID == "" {
		return "", "", ErrPaymentFailed
	}
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

// VerifyCheckout actively verifies payment status, exact amount, currency, and recipient metadata with Paystack or PayPal
// and credits the user's Star wallet idempotently.
func (s *Service) VerifyCheckout(
	ctx context.Context,
	userID, deviceFP, ipFP string,
	payload CheckoutVerifyPayload,
) (CheckoutVerifyResponse, error) {
	if strings.TrimSpace(userID) == "" {
		return CheckoutVerifyResponse{}, ErrUnauthorized
	}
	idempKey, ok := normalizeIdempotencyKey(payload.IdempotencyKey)
	if !ok {
		return CheckoutVerifyResponse{}, ErrBadRequest
	}

	s.mu.Lock()
	var rec *paymentRecord
	if pid := strings.TrimSpace(payload.PaymentID); pid != "" {
		rec = s.payments[pid]
	}
	if rec == nil && strings.TrimSpace(payload.Reference) != "" {
		rec = s.payByRef["paystack:"+strings.TrimSpace(payload.Reference)]
	}
	if rec == nil && strings.TrimSpace(payload.PayPalOrderID) != "" {
		rec = s.payByOrderID[strings.TrimSpace(payload.PayPalOrderID)]
	}
	if rec == nil {
		s.mu.Unlock()
		return CheckoutVerifyResponse{}, ErrNotFound
	}
	if rec.UserID != userID {
		s.mu.Unlock()
		return CheckoutVerifyResponse{}, ErrForbidden
	}
	if rec.DTO.Status == "completed" {
		w := s.getOrCreateWalletLocked(userID).toDTO()
		dto := rec.DTO
		s.mu.Unlock()
		return CheckoutVerifyResponse{
			Success:       true,
			Credited:      false,
			Payment:       dto,
			Wallet:        w,
			ServerEpochMs: s.nowMs(),
		}, nil
	}
	provider := rec.DTO.Provider
	ref := rec.DTO.ProviderReference
	orderID := rec.DTO.ProviderOrderID
	if strings.TrimSpace(payload.PayPalOrderID) != "" && orderID == "" {
		orderID = strings.TrimSpace(payload.PayPalOrderID)
	}
	expectedMinor := rec.DTO.ExpectedAmountMinor
	expectedCurrency := rec.DTO.ExpectedCurrency
	paymentID := rec.DTO.PaymentID
	paystackBase := s.paystackBase
	paystackSecret := s.paystackSecret
	s.mu.Unlock()

	var verifiedAmountMinor int64
	var verifiedCurrency string
	var verifyErr error

	if provider == "paystack" {
		if paystackSecret == "" {
			return CheckoutVerifyResponse{}, ErrGatewayUnconfigured
		}
		verifiedAmountMinor, verifiedCurrency, verifyErr = s.verifyPaystackTransaction(ctx, paystackBase, paystackSecret, ref, paymentID, userID)
	} else {
		if orderID == "" {
			return CheckoutVerifyResponse{}, ErrBadRequest
		}
		verifiedAmountMinor, verifiedCurrency, verifyErr = s.verifyAndCapturePayPalOrder(ctx, orderID, paymentID, userID, idempKey)
	}
	if verifyErr != nil {
		return CheckoutVerifyResponse{}, verifyErr
	}

	if verifiedAmountMinor < expectedMinor || !strings.EqualFold(verifiedCurrency, expectedCurrency) {
		s.mu.Lock()
		rec.DTO.Status = "fraud_blocked"
		s.recordAMLViolationLocked(userID, "", "payment_amount_mismatch", rec.DTO.PackageStars,
			fmt.Sprintf("expected %d %s, got %d %s", expectedMinor, expectedCurrency, verifiedAmountMinor, verifiedCurrency), deviceFP, ipFP)
		s.mu.Unlock()
		return CheckoutVerifyResponse{}, ErrPaymentFailed
	}

	credited, updatedDTO, wallet := s.creditVerifiedPayment(rec, verifiedAmountMinor, verifiedCurrency, "active_verify")

	if s.hooks.AuditEvent != nil {
		s.hooks.AuditEvent(userID, "stars_checkout_verified", deviceFP, ipFP, map[string]string{
			"payment_id": paymentID,
			"provider":   provider,
			"credited":   strconv.FormatBool(credited),
		})
	}

	return CheckoutVerifyResponse{
		Success:       true,
		Credited:      credited,
		Payment:       updatedDTO,
		Wallet:        wallet,
		ServerEpochMs: s.nowMs(),
	}, nil
}

func (s *Service) verifyPaystackTransaction(
	ctx context.Context,
	baseURL, secret, reference, expectedPaymentID, expectedUserID string,
) (int64, string, error) {
	req, err := http.NewRequestWithContext(ctx, http.MethodGet, baseURL+"/transaction/verify/"+url.PathEscape(reference), nil)
	if err != nil {
		return 0, "", err
	}
	req.Header.Set("Authorization", "Bearer "+secret)

	resp, err := s.httpClient.Do(req)
	if err != nil {
		return 0, "", ErrPaymentFailed
	}
	defer resp.Body.Close()

	var parsed struct {
		Status bool `json:"status"`
		Data   struct {
			Status    string         `json:"status"`
			Reference string         `json:"reference"`
			Amount    int64          `json:"amount"`
			Currency  string         `json:"currency"`
			Metadata  map[string]any `json:"metadata"`
		} `json:"data"`
	}
	if err := json.NewDecoder(io.LimitReader(resp.Body, 1<<20)).Decode(&parsed); err != nil {
		return 0, "", ErrPaymentFailed
	}
	if resp.StatusCode < 200 || resp.StatusCode >= 300 || !parsed.Status {
		return 0, "", ErrPaymentFailed
	}
	if !strings.EqualFold(parsed.Data.Status, "success") {
		return 0, "", ErrPaymentFailed
	}
	if parsed.Data.Reference != "" && parsed.Data.Reference != reference {
		return 0, "", ErrPaymentFailed
	}
	if parsed.Data.Metadata != nil {
		if metaPID, ok := parsed.Data.Metadata["payment_id"].(string); ok && metaPID != "" && metaPID != expectedPaymentID {
			return 0, "", ErrPaymentFailed
		}
		if metaUID, ok := parsed.Data.Metadata["user_id"].(string); ok && metaUID != "" && metaUID != expectedUserID {
			return 0, "", ErrPaymentFailed
		}
	}
	return parsed.Data.Amount, parsed.Data.Currency, nil
}

func (s *Service) verifyAndCapturePayPalOrder(
	ctx context.Context,
	orderID, expectedPaymentID, expectedUserID, idempKey string,
) (int64, string, error) {
	token, err := s.getPayPalToken(ctx)
	if err != nil {
		return 0, "", err
	}

	type ppOrderResp struct {
		ID            string `json:"id"`
		Status        string `json:"status"`
		PurchaseUnits []struct {
			ReferenceID string `json:"reference_id"`
			CustomID    string `json:"custom_id"`
			Amount      struct {
				CurrencyCode string `json:"currency_code"`
				Value        string `json:"value"`
			} `json:"amount"`
			Payments struct {
				Captures []struct {
					ID     string `json:"id"`
					Status string `json:"status"`
					Amount struct {
						CurrencyCode string `json:"currency_code"`
						Value        string `json:"value"`
					} `json:"amount"`
				} `json:"captures"`
			} `json:"payments"`
		} `json:"purchase_units"`
	}

	fetchOrder := func() (ppOrderResp, error) {
		req, _ := http.NewRequestWithContext(ctx, http.MethodGet, s.paypalBase+"/v2/checkout/orders/"+url.PathEscape(orderID), nil)
		req.Header.Set("Authorization", "Bearer "+token)
		resp, doErr := s.httpClient.Do(req)
		if doErr != nil {
			return ppOrderResp{}, ErrPaymentFailed
		}
		defer resp.Body.Close()
		var out ppOrderResp
		if json.NewDecoder(io.LimitReader(resp.Body, 1<<20)).Decode(&out) != nil || resp.StatusCode < 200 || resp.StatusCode >= 300 {
			return ppOrderResp{}, ErrPaymentFailed
		}
		return out, nil
	}

	order, err := fetchOrder()
	if err != nil {
		return 0, "", err
	}
	if strings.EqualFold(order.Status, "APPROVED") {
		capReq, _ := http.NewRequestWithContext(ctx, http.MethodPost, s.paypalBase+"/v2/checkout/orders/"+url.PathEscape(orderID)+"/capture", bytes.NewReader([]byte("{}")))
		capReq.Header.Set("Authorization", "Bearer "+token)
		capReq.Header.Set("Content-Type", "application/json")
		capReq.Header.Set("PayPal-Request-Id", "cap_"+idempKey)
		capResp, capErr := s.httpClient.Do(capReq)
		if capErr != nil {
			return 0, "", ErrPaymentFailed
		}
		defer capResp.Body.Close()
		var captured ppOrderResp
		if json.NewDecoder(io.LimitReader(capResp.Body, 1<<20)).Decode(&captured) != nil || capResp.StatusCode < 200 || capResp.StatusCode >= 300 {
			return 0, "", ErrPaymentFailed
		}
		order = captured
	}

	if !strings.EqualFold(order.Status, "COMPLETED") || len(order.PurchaseUnits) == 0 {
		return 0, "", ErrPaymentFailed
	}
	pu := order.PurchaseUnits[0]
	if pu.ReferenceID != "" && pu.ReferenceID != expectedPaymentID {
		return 0, "", ErrPaymentFailed
	}
	if pu.CustomID != "" {
		parts := strings.Split(pu.CustomID, "|")
		if len(parts) >= 2 && (parts[0] != expectedUserID || parts[1] != expectedPaymentID) {
			return 0, "", ErrPaymentFailed
		}
	}

	amtStr := pu.Amount.Value
	curr := pu.Amount.CurrencyCode
	if len(pu.Payments.Captures) > 0 {
		capItem := pu.Payments.Captures[0]
		if !strings.EqualFold(capItem.Status, "COMPLETED") {
			return 0, "", ErrPaymentFailed
		}
		amtStr = capItem.Amount.Value
		curr = capItem.Amount.CurrencyCode
	}
	fVal, err := strconv.ParseFloat(strings.TrimSpace(amtStr), 64)
	if err != nil {
		return 0, "", ErrPaymentFailed
	}
	return int64(math.Round(fVal * 100.0)), curr, nil
}

// HandlePaystackWebhook verifies HMAC-SHA512 over raw body bytes using PAYSTACK_SECRET_KEY
// and atomically credits charge.success events if not already credited.
func (s *Service) HandlePaystackWebhook(ctx context.Context, rawBody []byte, signatureHeader string) error {
	s.mu.Lock()
	secret := s.paystackSecret
	s.mu.Unlock()
	if secret == "" {
		return ErrGatewayUnconfigured
	}
	sigClean := strings.ToLower(strings.TrimSpace(signatureHeader))
	if sigClean == "" {
		return ErrForbidden
	}
	mac := hmac.New(sha512.New, []byte(secret))
	_, _ = mac.Write(rawBody)
	expectedHex := hex.EncodeToString(mac.Sum(nil))
	if !hmac.Equal([]byte(expectedHex), []byte(sigClean)) {
		return ErrForbidden
	}

	var event struct {
		Event string `json:"event"`
		Data  struct {
			Status    string         `json:"status"`
			Reference string         `json:"reference"`
			Amount    int64          `json:"amount"`
			Currency  string         `json:"currency"`
			Metadata  map[string]any `json:"metadata"`
		} `json:"data"`
	}
	if err := json.Unmarshal(rawBody, &event); err != nil {
		return ErrBadRequest
	}
	if !strings.EqualFold(event.Event, "charge.success") || !strings.EqualFold(event.Data.Status, "success") {
		return nil
	}

	ref := strings.TrimSpace(event.Data.Reference)
	if ref == "" {
		return ErrBadRequest
	}

	s.mu.Lock()
	rec := s.payByRef["paystack:"+ref]
	if rec == nil && event.Data.Metadata != nil {
		if pid, ok := event.Data.Metadata["payment_id"].(string); ok {
			rec = s.payments[pid]
		}
	}
	if rec == nil {
		s.mu.Unlock()
		return ErrNotFound
	}
	if event.Data.Amount < rec.DTO.ExpectedAmountMinor || !strings.EqualFold(event.Data.Currency, rec.DTO.ExpectedCurrency) {
		rec.DTO.Status = "fraud_blocked"
		s.recordAMLViolationLocked(rec.UserID, "", "webhook_paystack_amount_mismatch", rec.DTO.PackageStars,
			fmt.Sprintf("webhook %d %s vs expected %d %s", event.Data.Amount, event.Data.Currency, rec.DTO.ExpectedAmountMinor, rec.DTO.ExpectedCurrency), "", "")
		s.mu.Unlock()
		return ErrPaymentFailed
	}
	s.mu.Unlock()

	_, _, _ = s.creditVerifiedPayment(rec, event.Data.Amount, event.Data.Currency, "webhook_paystack")
	return nil
}

// HandlePayPalWebhook verifies the transmission headers and PayPal Cert URL strictly before verifying
// the signature through PayPal's /v1/notifications/verify-webhook-signature endpoint.
func (s *Service) HandlePayPalWebhook(ctx context.Context, headers http.Header, rawBody []byte) error {
	s.mu.Lock()
	webhookID := s.paypalWebhook
	base := s.paypalBase
	s.mu.Unlock()

	if webhookID == "" || s.paypalClientID == "" || s.paypalSecret == "" {
		return ErrGatewayUnconfigured
	}

	transmissionID := strings.TrimSpace(headers.Get("PAYPAL-TRANSMISSION-ID"))
	transmissionTime := strings.TrimSpace(headers.Get("PAYPAL-TRANSMISSION-TIME"))
	transmissionSig := strings.TrimSpace(headers.Get("PAYPAL-TRANSMISSION-SIG"))
	certURL := strings.TrimSpace(headers.Get("PAYPAL-CERT-URL"))
	authAlgo := strings.TrimSpace(headers.Get("PAYPAL-AUTH-ALGO"))

	if transmissionID == "" || transmissionTime == "" || transmissionSig == "" || certURL == "" || authAlgo == "" {
		return ErrForbidden
	}
	parsedCert, err := url.Parse(certURL)
	if err != nil || !strings.EqualFold(parsedCert.Scheme, "https") || !strings.HasSuffix(strings.ToLower(parsedCert.Hostname()), ".paypal.com") {
		return ErrForbidden
	}

	token, err := s.getPayPalToken(ctx)
	if err != nil {
		return err
	}

	verifyReqPayload := map[string]any{
		"transmission_id":   transmissionID,
		"transmission_time": transmissionTime,
		"cert_url":          certURL,
		"auth_algo":         authAlgo,
		"transmission_sig":  transmissionSig,
		"webhook_id":        webhookID,
		"webhook_event":     json.RawMessage(rawBody),
	}
	bodyBytes, _ := json.Marshal(verifyReqPayload)
	req, err := http.NewRequestWithContext(ctx, http.MethodPost, base+"/v1/notifications/verify-webhook-signature", bytes.NewReader(bodyBytes))
	if err != nil {
		return err
	}
	req.Header.Set("Authorization", "Bearer "+token)
	req.Header.Set("Content-Type", "application/json")

	resp, err := s.httpClient.Do(req)
	if err != nil {
		return ErrPaymentFailed
	}
	defer resp.Body.Close()

	var verifyOut struct {
		VerificationStatus string `json:"verification_status"`
	}
	if json.NewDecoder(io.LimitReader(resp.Body, 1<<20)).Decode(&verifyOut) != nil ||
		!strings.EqualFold(verifyOut.VerificationStatus, "SUCCESS") {
		return ErrForbidden
	}

	var event struct {
		EventType string `json:"event_type"`
		Resource  struct {
			ID            string `json:"id"`
			Status        string `json:"status"`
			CustomID      string `json:"custom_id"`
			Supplementary struct {
				RelatedIDs struct {
					OrderID string `json:"order_id"`
				} `json:"related_ids"`
			} `json:"supplementary_data"`
			Amount struct {
				CurrencyCode string `json:"currency_code"`
				Value        string `json:"value"`
			} `json:"amount"`
		} `json:"resource"`
	}
	if err := json.Unmarshal(rawBody, &event); err != nil {
		return ErrBadRequest
	}
	if event.EventType != "PAYMENT.CAPTURE.COMPLETED" && event.EventType != "CHECKOUT.ORDER.COMPLETED" {
		return nil
	}

	orderID := strings.TrimSpace(event.Resource.Supplementary.RelatedIDs.OrderID)
	if orderID == "" && strings.HasPrefix(event.EventType, "CHECKOUT.ORDER") {
		orderID = strings.TrimSpace(event.Resource.ID)
	}
	var paymentID string
	if event.Resource.CustomID != "" {
		parts := strings.Split(event.Resource.CustomID, "|")
		if len(parts) >= 2 {
			paymentID = parts[1]
		}
	}

	s.mu.Lock()
	var rec *paymentRecord
	if orderID != "" {
		rec = s.payByOrderID[orderID]
	}
	if rec == nil && paymentID != "" {
		rec = s.payments[paymentID]
	}
	if rec == nil {
		s.mu.Unlock()
		return ErrNotFound
	}
	fVal, parseErr := strconv.ParseFloat(strings.TrimSpace(event.Resource.Amount.Value), 64)
	if parseErr != nil {
		s.mu.Unlock()
		return ErrBadRequest
	}
	paidMinor := int64(math.Round(fVal * 100.0))
	paidCurr := event.Resource.Amount.CurrencyCode
	if paidMinor < rec.DTO.ExpectedAmountMinor || !strings.EqualFold(paidCurr, rec.DTO.ExpectedCurrency) {
		rec.DTO.Status = "fraud_blocked"
		s.recordAMLViolationLocked(rec.UserID, "", "webhook_paypal_amount_mismatch", rec.DTO.PackageStars,
			fmt.Sprintf("webhook %d %s vs expected %d %s", paidMinor, paidCurr, rec.DTO.ExpectedAmountMinor, rec.DTO.ExpectedCurrency), "", "")
		s.mu.Unlock()
		return ErrPaymentFailed
	}
	s.mu.Unlock()

	_, _, _ = s.creditVerifiedPayment(rec, paidMinor, paidCurr, "webhook_paypal")
	return nil
}

func (s *Service) creditVerifiedPayment(
	rec *paymentRecord,
	paidMinor int64,
	paidCurrency, verifiedVia string,
) (credited bool, paymentDTO StarPaymentDTO, walletDTO StarWalletDTO) {
	now := s.nowMs()
	s.mu.Lock()
	if rec.DTO.Status == "completed" {
		w := s.getOrCreateWalletLocked(rec.UserID).toDTO()
		dto := rec.DTO
		s.mu.Unlock()
		return false, dto, w
	}

	rec.DTO.Status = "completed"
	rec.DTO.PaidAmountMinor = paidMinor
	rec.DTO.PaidCurrency = paidCurrency
	rec.DTO.VerifiedVia = verifiedVia
	rec.DTO.CompletedAtMs = now

	w := s.getOrCreateWalletLocked(rec.UserID)
	w.BalanceStars += rec.DTO.PackageStars
	w.UpdatedAtMs = now
	walletDTO = w.toDTO()

	entry := ledgerRecord{
		UserID:         rec.UserID,
		IdempotencyKey: rec.IdempotencyKey,
		DTO: StarLedgerItemDTO{
			EntryID:      newRandomID("starled"),
			EntryType:    "purchase",
			DeltaStars:   rec.DTO.PackageStars,
			BalanceAfter: w.BalanceStars,
			Provider:     rec.DTO.Provider,
			AmountMinor:  paidMinor,
			Currency:     paidCurrency,
			PaymentID:    rec.DTO.PaymentID,
			CreatedAtMs:  now,
		},
	}
	s.ledgerByUser[rec.UserID] = append(s.ledgerByUser[rec.UserID], entry)
	paymentDTO = rec.DTO
	sendWS := s.hooks.SendWSEvent
	s.mu.Unlock()

	if sendWS != nil {
		sendWS(rec.UserID, map[string]any{
			"event":          "stars.updated",
			"user_id":        rec.UserID,
			"balance_stars":  walletDTO.BalanceStars,
			"stars_received": walletDTO.StarsReceivedTotal,
			"stars_sent":     walletDTO.StarsSentTotal,
			"delta":          rec.DTO.PackageStars,
			"entry_type":     "purchase",
			"history_item":   entry.DTO,
			"timestamp_ms":   now,
		})
	}
	return true, paymentDTO, walletDTO
}

// SendStarsInChat atomically debits sender balance, credits recipient wallet, enforces AML velocity/wash-trading limits,
// writes a server-verified star_gift chat message, broadcasts real-time stars.updated events, and triggers FCM push.
func (s *Service) SendStarsInChat(
	ctx context.Context,
	senderID, deviceFP, ipFP string,
	payload SendStarsPayload,
) (SendStarsResponse, error) {
	if strings.TrimSpace(senderID) == "" {
		return SendStarsResponse{}, ErrUnauthorized
	}
	recipientID := strings.TrimSpace(payload.RecipientUserID)
	if recipientID == "" || recipientID == senderID {
		return SendStarsResponse{}, ErrBadRequest
	}
	if !IsValidPackageStars(payload.Stars) {
		return SendStarsResponse{}, ErrBadRequest
	}
	idempKey, ok := normalizeIdempotencyKey(payload.IdempotencyKey)
	if !ok {
		return SendStarsResponse{}, ErrBadRequest
	}
	if s.hooks.IsBlocked != nil && s.hooks.IsBlocked(senderID, recipientID) {
		return SendStarsResponse{}, ErrForbidden
	}

	idempComposite := senderID + ":" + idempKey
	now := s.nowMs()

	s.mu.Lock()
	if cached, exists := s.sendIdempCache[idempComposite]; exists {
		if cached.StarsSent != payload.Stars || cached.ConversationID != strings.TrimSpace(payload.ConversationID) && strings.TrimSpace(payload.ConversationID) != "" {
			// Allow matching if same recipient/amount
			if cached.StarsSent != payload.Stars {
				s.mu.Unlock()
				return SendStarsResponse{}, ErrIdempotencyConflict
			}
		}
		s.mu.Unlock()
		return cached, nil
	}

	senderWallet := s.getOrCreateWalletLocked(senderID)
	if senderWallet.BalanceStars < payload.Stars {
		walletSnap := senderWallet.toDTO()
		s.mu.Unlock()
		return SendStarsResponse{
			Success:          false,
			SenderWallet:     walletSnap,
			InsufficientBal:  true,
			RequiredBuyStars: payload.Stars,
			ServerEpochMs:    now,
		}, ErrInsufficientBalance
	}

	// AML Velocity & Wash-Trading Checks
	cutoff1m := now - int64(time.Minute/time.Millisecond)
	cutoff30m := now - int64(30*time.Minute/time.Millisecond)
	cutoff1h := now - int64(time.Hour/time.Millisecond)
	cutoff24h := now - int64(24*time.Hour/time.Millisecond)

	sends1m := 0
	sends1h := 0
	var sent24h int64
	var sentToPair1h int64
	pruned := make([]sendVelocityEntry, 0, len(s.sendLogsByUser[senderID]))
	for _, entry := range s.sendLogsByUser[senderID] {
		if entry.TimestampMs >= cutoff24h {
			pruned = append(pruned, entry)
			sent24h += entry.Stars
			if entry.TimestampMs >= cutoff1h {
				sends1h++
				if entry.RecipientUserID == recipientID {
					sentToPair1h += entry.Stars
				}
			}
			if entry.TimestampMs >= cutoff1m {
				sends1m++
			}
		}
	}
	s.sendLogsByUser[senderID] = pruned

	if sends1m >= 5 {
		s.recordAMLViolationLocked(senderID, recipientID, "send_velocity_1m", payload.Stars, "Exceeded 5 Star sends per minute", deviceFP, ipFP)
		s.mu.Unlock()
		return SendStarsResponse{}, ErrRateLimited
	}
	if sends1h >= 25 {
		s.recordAMLViolationLocked(senderID, recipientID, "send_velocity_1h", payload.Stars, "Exceeded 25 Star sends per hour", deviceFP, ipFP)
		s.mu.Unlock()
		return SendStarsResponse{}, ErrRateLimited
	}
	if sent24h+payload.Stars > 100000 {
		s.recordAMLViolationLocked(senderID, recipientID, "daily_send_cap_100k", payload.Stars, "Exceeded 100,000 daily Star send limit", deviceFP, ipFP)
		s.mu.Unlock()
		return SendStarsResponse{}, ErrRateLimited
	}
	if sentToPair1h+payload.Stars > 50000 && sends1h >= 3 {
		s.recordAMLViolationLocked(senderID, recipientID, "pair_structuring_1h", payload.Stars, "Repeated high-value structuring to same recipient", deviceFP, ipFP)
		s.mu.Unlock()
		return SendStarsResponse{}, ErrRateLimited
	}

	// Wash-trading / round-trip check: block immediate ping-ponging B -> A after A -> B within 30 minutes
	var recentInboundFromPeer int64
	for _, peerSend := range s.sendLogsByUser[recipientID] {
		if peerSend.RecipientUserID == senderID && peerSend.TimestampMs >= cutoff30m {
			recentInboundFromPeer += peerSend.Stars
		}
	}
	if recentInboundFromPeer > 0 && payload.Stars >= recentInboundFromPeer/2 && recentInboundFromPeer >= 50 {
		s.recordAMLViolationLocked(senderID, recipientID, "wash_round_trip_30m", payload.Stars,
			fmt.Sprintf("Round-trip cooldown active (%d stars recently received from peer)", recentInboundFromPeer), deviceFP, ipFP)
		s.mu.Unlock()
		return SendStarsResponse{}, ErrRateLimited
	}
	s.mu.Unlock()

	convID := strings.TrimSpace(payload.ConversationID)
	if convID == "" && s.hooks.OpenDirectConv != nil {
		openedID, err := s.hooks.OpenDirectConv(senderID, recipientID)
		if err != nil {
			return SendStarsResponse{}, ErrBadRequest
		}
		convID = openedID
	}

	var senderName, recipientName, senderUser, senderAvatar string
	if s.hooks.ResolveUser != nil {
		if info, ok := s.hooks.ResolveUser(senderID); ok {
			senderName = info.FullName
			senderUser = info.Username
			senderAvatar = info.AvatarURL
		}
		if info, ok := s.hooks.ResolveUser(recipientID); ok {
			recipientName = info.FullName
		}
	}

	sentEntryID := newRandomID("starled")
	recvEntryID := newRandomID("starled")

	metaObj := starGiftChatMeta{
		Type:           "star_gift",
		Stars:          payload.Stars,
		SenderUserID:   senderID,
		SenderName:     senderName,
		RecipientID:    recipientID,
		RecipientName:  recipientName,
		AnimationTier:  AnimationTierForStars(payload.Stars),
		LedgerEntryID:  sentEntryID,
		IdempotencyKey: idempKey,
		ServerSentAtMs: now,
	}
	metaBytes, _ := json.Marshal(metaObj)

	var msgID string
	if s.hooks.AppendStarChatMsg != nil && convID != "" {
		createdMsgID, err := s.hooks.AppendStarChatMsg(convID, senderID, recipientID, string(metaBytes), "star_"+idempKey)
		if err != nil {
			return SendStarsResponse{}, err
		}
		msgID = createdMsgID
	} else {
		msgID = newRandomID("msg")
	}

	s.mu.Lock()
	// Re-check balance inside final commit lock to prevent concurrent double-spend
	senderWallet = s.getOrCreateWalletLocked(senderID)
	if senderWallet.BalanceStars < payload.Stars {
		walletSnap := senderWallet.toDTO()
		s.mu.Unlock()
		return SendStarsResponse{
			Success:          false,
			SenderWallet:     walletSnap,
			InsufficientBal:  true,
			RequiredBuyStars: payload.Stars,
			ServerEpochMs:    now,
		}, ErrInsufficientBalance
	}

	recipientWallet := s.getOrCreateWalletLocked(recipientID)
	senderWallet.BalanceStars -= payload.Stars
	senderWallet.StarsSentTotal += payload.Stars
	senderWallet.UpdatedAtMs = now

	recipientWallet.BalanceStars += payload.Stars
	recipientWallet.StarsReceivedTotal += payload.Stars
	recipientWallet.UpdatedAtMs = now

	senderWalletDTO := senderWallet.toDTO()
	recipientWalletDTO := recipientWallet.toDTO()

	sentLedger := ledgerRecord{
		UserID:         senderID,
		IdempotencyKey: idempKey,
		DTO: StarLedgerItemDTO{
			EntryID:            sentEntryID,
			EntryType:          "sent",
			DeltaStars:         -payload.Stars,
			BalanceAfter:       senderWallet.BalanceStars,
			CounterpartyUserID: recipientID,
			MessageID:          msgID,
			ConversationID:     convID,
			CreatedAtMs:        now,
		},
	}
	recvLedger := ledgerRecord{
		UserID:         recipientID,
		IdempotencyKey: idempKey,
		DTO: StarLedgerItemDTO{
			EntryID:            recvEntryID,
			EntryType:          "received",
			DeltaStars:         payload.Stars,
			BalanceAfter:       recipientWallet.BalanceStars,
			CounterpartyUserID: senderID,
			CounterpartyName:   senderName,
			CounterpartyUsername: senderUser,
			CounterpartyAvatar: senderAvatar,
			MessageID:          msgID,
			ConversationID:     convID,
			CreatedAtMs:        now,
		},
	}

	s.ledgerByUser[senderID] = append(s.ledgerByUser[senderID], sentLedger)
	s.ledgerByUser[recipientID] = append(s.ledgerByUser[recipientID], recvLedger)
	s.sendLogsByUser[senderID] = append(s.sendLogsByUser[senderID], sendVelocityEntry{
		RecipientUserID: recipientID,
		DeviceFP:        deviceFP,
		IPFP:            ipFP,
		Stars:           payload.Stars,
		TimestampMs:     now,
	})

	resp := SendStarsResponse{
		Success:        true,
		MessageID:      msgID,
		ConversationID: convID,
		StarsSent:      payload.Stars,
		SenderWallet:   senderWalletDTO,
		ServerEpochMs:  now,
	}
	s.sendIdempCache[idempComposite] = resp

	sendWS := s.hooks.SendWSEvent
	notifyStar := s.hooks.NotifyStarReceived
	auditFn := s.hooks.AuditEvent
	s.mu.Unlock()

	if sendWS != nil {
		sendWS(senderID, map[string]any{
			"event":          "stars.updated",
			"user_id":        senderID,
			"balance_stars":  senderWalletDTO.BalanceStars,
			"stars_received": senderWalletDTO.StarsReceivedTotal,
			"stars_sent":     senderWalletDTO.StarsSentTotal,
			"delta":          -payload.Stars,
			"entry_type":     "sent",
			"peer_user_id":   recipientID,
			"history_item":   sentLedger.DTO,
			"timestamp_ms":   now,
		})
		sendWS(recipientID, map[string]any{
			"event":          "stars.updated",
			"user_id":        recipientID,
			"balance_stars":  recipientWalletDTO.BalanceStars,
			"stars_received": recipientWalletDTO.StarsReceivedTotal,
			"stars_sent":     recipientWalletDTO.StarsSentTotal,
			"delta":          payload.Stars,
			"entry_type":     "received",
			"peer_user_id":   senderID,
			"history_item":   recvLedger.DTO,
			"timestamp_ms":   now,
		})
	}
	if notifyStar != nil {
		notifyStar(ctx, senderID, recipientID, convID, msgID, payload.Stars)
	}
	if auditFn != nil {
		auditFn(senderID, "stars_sent_in_chat", deviceFP, ipFP, map[string]string{
			"recipient_user_id": recipientID,
			"stars":             strconv.FormatInt(payload.Stars, 10),
			"message_id":        msgID,
		})
	}

	return resp, nil
}

func (s *Service) recordAMLViolationLocked(userID, peerID, rule string, stars int64, details, deviceFP, ipFP string) {
	s.amlLog = append(s.amlLog, amlViolation{
		UserID:      userID,
		PeerUserID:  peerID,
		Rule:        rule,
		StarsAmount: stars,
		Details:     details,
		TimestampMs: s.nowMs(),
	})
	if len(s.amlLog) > 500 {
		s.amlLog = s.amlLog[len(s.amlLog)-500:]
	}
	if s.hooks.AuditEvent != nil {
		s.hooks.AuditEvent(userID, "stars_aml_blocked_"+rule, deviceFP, ipFP, map[string]string{
			"peer_user_id": peerID,
			"stars":        strconv.FormatInt(stars, 10),
			"details":      details,
		})
	}
}
