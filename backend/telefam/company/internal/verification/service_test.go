package verification

import (
	"bytes"
	"context"
	"crypto/hmac"
	"crypto/sha512"
	"encoding/base64"
	"encoding/hex"
	"encoding/json"
	"image"
	"image/color"
	"image/png"
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"
	"time"
)

func encodeTestPNG(w, h int, seed byte) string {
	img := image.NewRGBA(image.Rect(0, 0, w, h))
	for y := 0; y < h; y++ {
		for x := 0; x < w; x++ {
			img.Set(x, y, color.RGBA{
				R: uint8((x + int(seed)) % 255),
				G: uint8((y + int(seed*3)) % 255),
				B: uint8((x ^ y + int(seed)) % 255),
				A: 255,
			})
		}
	}
	var buf bytes.Buffer
	_ = png.Encode(&buf, img)
	return base64.StdEncoding.EncodeToString(buf.Bytes())
}

func TestVerificationPricingAndEndToEndFlowWithBadgeUnlock(t *testing.T) {
	keBiz := ComputeTierPricing(TierBusiness, "KE")
	if keBiz.MonthlyDisplay != "Ksh 1,200" || keBiz.AnnualDisplay != "Ksh 11,520" || keBiz.AnnualOriginalDisp != "Ksh 14,400" {
		t.Fatalf("unexpected KE business pricing: %+v", keBiz)
	}
	keGov := ComputeTierPricing(TierGovernment, "KE")
	if keGov.MonthlyDisplay != "Ksh 700" || keGov.AnnualDisplay != "Ksh 6,720" || keGov.AnnualOriginalDisp != "Ksh 8,400" {
		t.Fatalf("unexpected KE gov pricing: %+v", keGov)
	}

	var pushed []string
	svc := NewService(HostHooks{
		ResolveUser: func(userID string) (UserInfo, bool) {
			return UserInfo{
				UserID:     userID,
				FullName:   "John Michael Doe",
				Username:   "johndoe",
				Email:      "john@telefam.test",
				CountryISO: "KE",
			}, true
		},
		NotifyVerificationUpdate: func(ctx context.Context, userID, status, tier, title, body string) {
			pushed = append(pushed, status+":"+tier)
		},
	})

	ctx := context.Background()
	status, err := svc.ActivateFreeTrial(ctx, "user_1", "fp_1", "ip_1", TrialActivatePayload{
		Tier:           TierBusiness,
		BillingCycle:   CycleAnnual,
		IdempotencyKey: "trial-key-0001",
	})
	if err != nil {
		t.Fatalf("trial activation failed: %v", err)
	}
	if status.TrialEligible || status.PlanStatus != "trial_active" || status.ReviewStatus != "awaiting_liveness" {
		t.Fatalf("unexpected status after trial: %+v", status)
	}

	// Second free trial attempt must fail
	if _, err := svc.ActivateFreeTrial(ctx, "user_1", "fp_1", "ip_1", TrialActivatePayload{
		Tier:           TierBlue,
		BillingCycle:   CycleMonthly,
		IdempotencyKey: "trial-key-0002",
	}); err != ErrTrialAlreadyUsed {
		t.Fatalf("expected ErrTrialAlreadyUsed, got %v", err)
	}

	// Liveness blocked if screen capture active
	if _, err := svc.IssueLivenessSession("user_1", "fp_1", "ip_1", LivenessSessionRequest{
		Tier:                TierBusiness,
		ScreenCaptureActive: true,
	}); err != ErrScreenCaptureActive {
		t.Fatalf("expected screen capture block, got %v", err)
	}

	sess, err := svc.IssueLivenessSession("user_1", "fp_1", "ip_1", LivenessSessionRequest{
		Tier: TierBusiness,
	})
	if err != nil || len(sess.Sequence) != 6 {
		t.Fatalf("unexpected liveness session: %+v err=%v", sess, err)
	}

	selfieB64 := encodeTestPNG(320, 320, 7)
	t0 := time.Now().UnixMilli() - 5000
	steps := []LivenessStepTelemetry{
		{Step: "left", Yaw: -18, Pitch: 1, LeftEyeOpen: 0.92, RightEyeOpen: 0.91, Luma: 110, CapturedAtMs: t0},
		{Step: "right", Yaw: 19, Pitch: 2, LeftEyeOpen: 0.90, RightEyeOpen: 0.90, Luma: 112, CapturedAtMs: t0 + 700},
		{Step: "up", Yaw: 1, Pitch: 15, LeftEyeOpen: 0.88, RightEyeOpen: 0.89, Luma: 109, CapturedAtMs: t0 + 1400},
		{Step: "left", Yaw: -16, Pitch: 0, LeftEyeOpen: 0.93, RightEyeOpen: 0.92, Luma: 115, CapturedAtMs: t0 + 2100},
		{Step: "right", Yaw: 17, Pitch: 1, LeftEyeOpen: 0.91, RightEyeOpen: 0.92, Luma: 114, CapturedAtMs: t0 + 2800},
		{Step: "center", Yaw: 0.5, Pitch: -1.0, LeftEyeOpen: 0.95, RightEyeOpen: 0.94, Luma: 118, CapturedAtMs: t0 + 3500},
	}

	lvRes, err := svc.SubmitLiveness(ctx, "user_1", "fp_1", "ip_1", LivenessSubmitPayload{
		ChallengeID:    sess.ChallengeID,
		ChallengeToken: sess.ChallengeToken,
		SelfieImageB64: selfieB64,
		Steps:          steps,
	})
	if err != nil || !lvRes.Passed || lvRes.LivenessScore != 100 {
		t.Fatalf("expected liveness score 100 pass, got %+v err=%v", lvRes, err)
	}

	// Replaying the same liveness challenge token must fail
	if _, err := svc.SubmitLiveness(ctx, "user_1", "fp_1", "ip_1", LivenessSubmitPayload{
		ChallengeID:    sess.ChallengeID,
		ChallengeToken: sess.ChallengeToken,
		SelfieImageB64: selfieB64,
		Steps:          steps,
	}); err != ErrChallengeExpired {
		t.Fatalf("expected replay rejection, got %v", err)
	}

	docSess, err := svc.IssueDocumentSession("user_1", "fp_1", "ip_1", DocumentSessionRequest{
		DocumentType: "national_id",
	})
	if err != nil {
		t.Fatalf("document session failed: %v", err)
	}

	docB64 := encodeTestPNG(640, 420, 42)
	docRes, err := svc.SubmitDocument(ctx, "user_1", "fp_1", "ip_1", DocumentSubmitPayload{
		ChallengeID:        docSess.ChallengeID,
		ChallengeToken:     docSess.ChallengeToken,
		DocumentType:       "national_id",
		DocumentImageB64:   docB64,
		ClientOcrRawText:   "REPUBLIC OF EXAMPLELAND\nNATIONAL IDENTITY CARD\nID NUMBER EX123456789\nFULL NAME JOHN MICHAEL DOE\nDATE OF EXPIRY 15 MAY 2030",
		SharpnessScore:     0.85,
		LuminanceMean:      120.0,
		GlareRatio:         0.04,
		AllCornersDetected: true,
	})
	if err != nil || !docRes.Accepted || !docRes.DocumentsPurged || docRes.Status.ReviewStatus != "under_review" {
		t.Fatalf("expected document under_review & purged, got %+v err=%v", docRes, err)
	}
	if docRes.Status.ReviewETAWorkingDays != 3 {
		t.Fatalf("expected 3 working days SLA, got %d", docRes.Status.ReviewETAWorkingDays)
	}
	if !strings.HasSuffix(docRes.ExtractedDocLast4, "6789") {
		t.Fatalf("expected redacted last4 6789, got %s", docRes.ExtractedDocLast4)
	}

	finalStatus, err := svc.CompleteReview(ctx, CompleteReviewPayload{
		UserID:  "user_1",
		Approve: true,
	})
	if err != nil || finalStatus.ReviewStatus != "verified" || finalStatus.VerifiedTier != TierBusiness {
		t.Fatalf("expected unlocked business badge, got %+v err=%v", finalStatus, err)
	}
	if len(pushed) < 2 || pushed[len(pushed)-1] != "verified:business" {
		t.Fatalf("expected under_review & verified push notifications, got %+v", pushed)
	}
}

func TestVerificationPaystackCheckoutAndWebhook(t *testing.T) {
	psSecret := "sk_test_ver_secret"
	srv := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		switch {
		case r.Method == http.MethodPost && r.URL.Path == "/transaction/initialize":
			_ = json.NewEncoder(w).Encode(map[string]any{
				"status": true,
				"data": map[string]string{
					"authorization_url": "https://checkout.paystack.com/ver_test_123",
					"reference":         "TFVR_TEST",
				},
			})
		default:
			w.WriteHeader(http.StatusNotFound)
		}
	}))
	defer srv.Close()

	svc := NewService(HostHooks{
		ResolveUser: func(userID string) (UserInfo, bool) {
			return UserInfo{UserID: userID, Email: "gov@telefam.test", CountryISO: "KE"}, true
		},
	})
	svc.ConfigureGatewaysForTest(srv.URL, psSecret, "", "", "", "", "", "", srv.Client())

	ctx := context.Background()
	initRes, err := svc.InitCheckout(ctx, "user_gov", "fp_g", "ip_g", CheckoutInitPayload{
		Tier:           TierGovernment,
		BillingCycle:   CycleMonthly,
		Provider:       "paystack",
		CountryISO:     "KE",
		IdempotencyKey: "ver-pay-0001",
	})
	if err != nil || initRes.Payment.ExpectedAmountMinor != 70000 {
		t.Fatalf("expected Ksh 700 (70000 minor), got %+v err=%v", initRes, err)
	}

	webhookBody, _ := json.Marshal(map[string]any{
		"event": "charge.success",
		"data": map[string]any{
			"reference": initRes.Payment.ProviderReference,
			"status":    "success",
			"amount":    70000,
			"currency":  "KES",
		},
	})
	mac := hmac.New(sha512.New, []byte(psSecret))
	mac.Write(webhookBody)
	sig := hex.EncodeToString(mac.Sum(nil))

	if err := svc.HandlePaystackWebhook(ctx, webhookBody, sig); err != nil {
		t.Fatalf("paystack webhook failed: %v", err)
	}
	st, _ := svc.GetStatus("user_gov", "KE")
	if st.PlanStatus != "subscribed" || st.PendingTier != TierGovernment || st.ReviewStatus != "awaiting_liveness" {
		t.Fatalf("expected active subscription awaiting liveness, got %+v", st)
	}
}
