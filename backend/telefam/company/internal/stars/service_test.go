package stars

import (
	"context"
	"crypto/hmac"
	"crypto/sha512"
	"encoding/hex"
	"encoding/json"
	"errors"
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"
)

func TestPaystackInitVerifyWebhookAndIdempotency(t *testing.T) {
	paystackSecret := "sk_test_secret_telefam_12345"
	mux := http.NewServeMux()
	mux.HandleFunc("POST /transaction/initialize", func(w http.ResponseWriter, r *http.Request) {
		if r.Header.Get("Authorization") != "Bearer "+paystackSecret {
			w.WriteHeader(http.StatusUnauthorized)
			return
		}
		var req map[string]any
		_ = json.NewDecoder(r.Body).Decode(&req)
		_ = json.NewEncoder(w).Encode(map[string]any{
			"status": true,
			"data": map[string]any{
				"authorization_url": "https://checkout.paystack.com/test_auth_url",
				"reference":         req["reference"],
			},
		})
	})
	mux.HandleFunc("GET /transaction/verify/", func(w http.ResponseWriter, r *http.Request) {
		ref := strings.TrimPrefix(r.URL.Path, "/transaction/verify/")
		_ = json.NewEncoder(w).Encode(map[string]any{
			"status": true,
			"data": map[string]any{
				"status":    "success",
				"reference": ref,
				"amount":    10000, // 100 KES = 10000 minor
				"currency":  "KES",
			},
		})
	})
	srv := httptest.NewServer(mux)
	defer srv.Close()

	var wsEvents []string
	svc := NewService(HostHooks{
		ResolveUser: func(userID string) (UserInfo, bool) {
			return UserInfo{UserID: userID, FullName: "Alice", Username: "alice", Email: "alice@example.com", CountryISO: "KE"}, true
		},
		SendWSEvent: func(targetUserID string, event map[string]any) {
			if ev, ok := event["event"].(string); ok {
				wsEvents = append(wsEvents, ev)
			}
		},
	})
	svc.ConfigureGatewaysForTest(srv.URL, paystackSecret, "", "", "", "", "https://telefam.company")

	init1, err := svc.InitCheckout(context.Background(), "u1", "dfp1", "ip1", CheckoutInitPayload{
		Provider:       "paystack",
		PackageStars:   100,
		CountryISO:     "KE",
		IdempotencyKey: "idemp_buy_100_stars_1",
	})
	if err != nil {
		t.Fatalf("InitCheckout failed: %v", err)
	}
	// Replay same idempotency key returns same payment
	init2, err := svc.InitCheckout(context.Background(), "u1", "dfp1", "ip1", CheckoutInitPayload{
		Provider:       "paystack",
		PackageStars:   100,
		CountryISO:     "KE",
		IdempotencyKey: "idemp_buy_100_stars_1",
	})
	if err != nil || init2.Payment.PaymentID != init1.Payment.PaymentID {
		t.Fatalf("expected idempotent replay, got err=%v", err)
	}
	// Conflict with different amount on same key
	if _, err := svc.InitCheckout(context.Background(), "u1", "dfp1", "ip1", CheckoutInitPayload{
		Provider:       "paystack",
		PackageStars:   200,
		CountryISO:     "KE",
		IdempotencyKey: "idemp_buy_100_stars_1",
	}); !errors.Is(err, ErrIdempotencyConflict) {
		t.Fatalf("expected ErrIdempotencyConflict, got %v", err)
	}

	ver, err := svc.VerifyCheckout(context.Background(), "u1", "dfp1", "ip1", CheckoutVerifyPayload{
		PaymentID:      init1.Payment.PaymentID,
		IdempotencyKey: "idemp_verify_100_stars_1",
	})
	if err != nil || !ver.Credited || ver.Wallet.BalanceStars != 100 {
		t.Fatalf("VerifyCheckout failed: %+v err=%v", ver, err)
	}

	// Webhook replay must verify HMAC signature and be idempotent (no double credit!)
	webhookPayload, _ := json.Marshal(map[string]any{
		"event": "charge.success",
		"data": map[string]any{
			"status":    "success",
			"reference": init1.Payment.ProviderReference,
			"amount":    10000,
			"currency":  "KES",
		},
	})
	mac := hmac.New(sha512.New, []byte(paystackSecret))
	_, _ = mac.Write(webhookPayload)
	sig := hex.EncodeToString(mac.Sum(nil))

	if err := svc.HandlePaystackWebhook(context.Background(), webhookPayload, "bad_sig"); !errors.Is(err, ErrForbidden) {
		t.Fatalf("expected ErrForbidden on bad Paystack signature, got %v", err)
	}
	if err := svc.HandlePaystackWebhook(context.Background(), webhookPayload, sig); err != nil {
		t.Fatalf("valid webhook should succeed idempotently: %v", err)
	}

	summary, _ := svc.GetSummary("u1", "KE")
	if summary.Wallet.BalanceStars != 100 {
		t.Fatalf("expected balance 100 without double credit, got %d", summary.Wallet.BalanceStars)
	}
	if len(wsEvents) == 0 || wsEvents[0] != "stars.updated" {
		t.Fatalf("expected real-time stars.updated event, got %+v", wsEvents)
	}
}

func TestSendStarsInChatAmlPushAndInsufficientBalance(t *testing.T) {
	var sentPushStars int64
	svc := NewService(HostHooks{
		ResolveUser: func(userID string) (UserInfo, bool) {
			return UserInfo{UserID: userID, FullName: userID, Username: userID, CountryISO: "KE"}, true
		},
		OpenDirectConv: func(callerID, peerID string) (string, error) {
			return "conv_u1_u2", nil
		},
		AppendStarChatMsg: func(convID, senderID, peerID, metaJSON, clientOpID string) (string, error) {
			return "msg_star_1", nil
		},
		NotifyStarReceived: func(ctx context.Context, senderID, recipientID, convID, messageID string, stars int64) {
			sentPushStars = stars
		},
	})

	// 0 balance returns ErrInsufficientBalance so UI redirects to Buy Stars
	_, err := svc.SendStarsInChat(context.Background(), "u1", "dfp1", "ip1", SendStarsPayload{
		RecipientUserID: "u2",
		Stars:           50,
		IdempotencyKey:  "send_stars_zero_bal_1",
	})
	if !errors.Is(err, ErrInsufficientBalance) {
		t.Fatalf("expected ErrInsufficientBalance, got %v", err)
	}

	// Credit u1 with 500 stars via internal wallet setup
	svc.mu.Lock()
	svc.getOrCreateWalletLocked("u1").BalanceStars = 500
	svc.mu.Unlock()

	res, err := svc.SendStarsInChat(context.Background(), "u1", "dfp1", "ip1", SendStarsPayload{
		RecipientUserID: "u2",
		Stars:           100,
		IdempotencyKey:  "send_stars_real_1",
	})
	if err != nil || res.SenderWallet.BalanceStars != 400 || sentPushStars != 100 {
		t.Fatalf("expected 100 stars sent & push fired, got res=%+v err=%v push=%d", res, err, sentPushStars)
	}

	// Anti-money-laundering round-trip guard: u2 cannot immediately ping-pong >= 50 back to u1
	_, roundTripErr := svc.SendStarsInChat(context.Background(), "u2", "dfp2", "ip2", SendStarsPayload{
		RecipientUserID: "u1",
		Stars:           100,
		IdempotencyKey:  "wash_ping_pong_1",
	})
	if !errors.Is(roundTripErr, ErrRateLimited) {
		t.Fatalf("expected ErrRateLimited for round-trip AML protection, got %v", roundTripErr)
	}
}
