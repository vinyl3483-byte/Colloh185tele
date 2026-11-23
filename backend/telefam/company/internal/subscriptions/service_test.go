package subscriptions

import (
	"context"
	"crypto/hmac"
	"crypto/sha512"
	"encoding/hex"
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"testing"
)

func TestPlanGoalBoundsAndCurrencyConversion(t *testing.T) {
	svc := NewService(HostHooks{
		ResolveUser: func(userID string) (UserInfo, bool) {
			return UserInfo{UserID: userID, FullName: "Alex", Username: "alex", CountryISO: "KE"}, true
		},
	})

	// Valid minimum $0.50 plan & $100.00 goal
	hub, err := svc.SaveCreatorPlan(context.Background(), "creator_1", "dev1", "ip1", SavePlanPayload{
		MonthlyUSDCents:     50,
		MonthlyGoalUSDCents: 10000,
		CountryISO:          "KE",
	})
	if err != nil {
		t.Fatalf("expected valid $0.50 plan to save, got %v", err)
	}
	if hub.MyPlan.MonthlyUSDCents != 50 || hub.MyPlan.MonthlyGoalUSDCents != 10000 {
		t.Fatalf("unexpected plan values: %+v", hub.MyPlan)
	}
	if hub.MyPlan.Pricing.CurrencyCode != "KES" {
		t.Fatalf("expected KES currency conversion, got %s", hub.MyPlan.Pricing.CurrencyCode)
	}
	if hub.MyPlan.Pricing.AnnualUSDCents != 480 { // 50 * 12 * 0.80 = 480
		t.Fatalf("expected 480 annual cents with 20%% discount, got %d", hub.MyPlan.Pricing.AnnualUSDCents)
	}

	// Out-of-bounds monthly plan (<$0.50 or >$100.00) must reject
	if _, err := svc.SaveCreatorPlan(context.Background(), "creator_1", "dev1", "ip1", SavePlanPayload{
		MonthlyUSDCents:     49,
		MonthlyGoalUSDCents: 10000,
	}); err == nil {
		t.Fatalf("expected rejection below $0.50")
	}
	if _, err := svc.SaveCreatorPlan(context.Background(), "creator_1", "dev1", "ip1", SavePlanPayload{
		MonthlyUSDCents:     10001,
		MonthlyGoalUSDCents: 10000,
	}); err == nil {
		t.Fatalf("expected rejection above $100.00")
	}
}

func TestCheckoutVerifyPaystackWebhookCancelAndRenew(t *testing.T) {
	var events []string
	var capturedRef string
	var capturedAmount int64

	psServer := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "application/json")
		if r.URL.Path == "/transaction/initialize" {
			var req struct {
				Reference string `json:"reference"`
				Amount    int64  `json:"amount"`
			}
			_ = json.NewDecoder(r.Body).Decode(&req)
			capturedRef = req.Reference
			capturedAmount = req.Amount
			_ = json.NewEncoder(w).Encode(map[string]any{
				"status": true,
				"data":   map[string]any{"authorization_url": "https://checkout.paystack.test/" + req.Reference},
			})
			return
		}
		if r.URL.Path == "/transaction/verify/"+capturedRef {
			_ = json.NewEncoder(w).Encode(map[string]any{
				"status": true,
				"data": map[string]any{
					"status":   "success",
					"amount":   capturedAmount,
					"currency": "KES",
				},
			})
			return
		}
		w.WriteHeader(http.StatusNotFound)
	}))
	defer psServer.Close()

	svc := NewService(HostHooks{
		ResolveUser: func(userID string) (UserInfo, bool) {
			return UserInfo{UserID: userID, FullName: userID, Username: userID, CountryISO: "KE"}, true
		},
		SendWSEvent: func(targetUserID string, event map[string]any) {
			events = append(events, targetUserID)
		},
	})
	svc.ConfigureGatewaysForTest(psServer.URL, "sk_test_123", "", "", "", "", psServer.Client())

	_, _ = svc.SaveCreatorPlan(context.Background(), "creator_1", "d1", "i1", SavePlanPayload{
		MonthlyUSDCents:     1000, // $10.00/mo
		MonthlyGoalUSDCents: 2000, // $20.00 goal -> 50% after 1 subscriber
		CountryISO:          "KE",
	})

	initResp, err := svc.InitCheckout(context.Background(), "fan_1", "d2", "i2", CheckoutInitPayload{
		CreatorUserID:  "creator_1",
		BillingCycle:   CycleMonthly,
		Provider:       "paystack",
		CountryISO:     "KE",
		IdempotencyKey: "sub_idemp_0001",
	})
	if err != nil {
		t.Fatalf("init checkout failed: %v", err)
	}
	if initResp.Payment.ProviderReference == "" || initResp.Payment.CheckoutURL == "" {
		t.Fatalf("missing checkout info: %+v", initResp.Payment)
	}

	// Idempotency conflict check with different cycle
	if _, err := svc.InitCheckout(context.Background(), "fan_1", "d2", "i2", CheckoutInitPayload{
		CreatorUserID:  "creator_1",
		BillingCycle:   CycleAnnual,
		Provider:       "paystack",
		CountryISO:     "KE",
		IdempotencyKey: "sub_idemp_0001",
	}); err != ErrIdempotencyConflict {
		t.Fatalf("expected ErrIdempotencyConflict, got %v", err)
	}

	// Verify via API
	verResp, err := svc.VerifyCheckout(context.Background(), "fan_1", "d2", "i2", CheckoutVerifyPayload{
		PaymentID:      initResp.Payment.PaymentID,
		Reference:      initResp.Payment.ProviderReference,
		IdempotencyKey: "sub_ver_0001",
	})
	if err != nil {
		t.Fatalf("verify checkout failed: %v", err)
	}
	if verResp.Subscription == nil || verResp.Subscription.Status != StatusActive {
		t.Fatalf("expected active subscription, got %+v", verResp.Subscription)
	}
	if verResp.Offering.Plan.GoalProgressPct != 50.0 {
		t.Fatalf("expected 50%% goal progress, got %v", verResp.Offering.Plan.GoalProgressPct)
	}

	// Cancel -> preserves active period with auto_renew=false
	cancelResp, err := svc.CancelSubscription(context.Background(), "fan_1", "d2", "i2", CancelPayload{CreatorUserID: "creator_1"})
	if err != nil || cancelResp.Subscription == nil || cancelResp.Subscription.Status != StatusCancelled || cancelResp.Subscription.AutoRenew {
		t.Fatalf("cancel failed: %v %+v", err, cancelResp)
	}

	// Restore / Renew in-place within active period
	renewResp, err := svc.RenewSubscription(context.Background(), "fan_1", "d2", "i2", RenewPayload{CreatorUserID: "creator_1"})
	if err != nil || renewResp.RequiresCheckout || renewResp.Subscription == nil || renewResp.Subscription.Status != StatusActive || !renewResp.Subscription.AutoRenew {
		t.Fatalf("renew failed: %v %+v", err, renewResp)
	}

	// Webhook flow for fan_2
	init2, err := svc.InitCheckout(context.Background(), "fan_2", "d3", "i3", CheckoutInitPayload{
		CreatorUserID:  "creator_1",
		BillingCycle:   CycleMonthly,
		Provider:       "paystack",
		CountryISO:     "KE",
		IdempotencyKey: "sub_idemp_0002",
	})
	if err != nil {
		t.Fatalf("init2 failed: %v", err)
	}
	whBody, _ := json.Marshal(map[string]any{
		"event": "charge.success",
		"data": map[string]any{
			"reference": init2.Payment.ProviderReference,
			"status":    "success",
			"amount":    init2.Payment.ExpectedAmountMinor,
			"currency":  init2.Payment.ExpectedCurrency,
		},
	})
	mac := hmac.New(sha512.New, []byte("sk_test_123"))
	mac.Write(whBody)
	sig := hex.EncodeToString(mac.Sum(nil))
	if err := svc.HandlePaystackWebhook(context.Background(), whBody, sig); err != nil {
		t.Fatalf("webhook failed: %v", err)
	}

	creatorHub, err := svc.GetHub("creator_1", "KE")
	if err != nil || len(creatorHub.Subscribers) != 2 || !creatorHub.MyPlan.GoalReached {
		t.Fatalf("expected 2 subscribers and goal reached, got %+v", creatorHub)
	}
	if len(events) == 0 {
		t.Fatalf("expected WS events emitted")
	}
}
