package auth

import (
	"crypto/hmac"
	"crypto/rand"
	"crypto/sha256"
	"crypto/subtle"
	"encoding/base64"
	"encoding/hex"
	"errors"
	"fmt"
	"math/big"
	"strings"
	"sync"
	"time"
)

var (
	ErrVerificationFailed = errors.New("verification failed")
	ErrMaxAttemptsReached = errors.New("max verification attempts exceeded")
)

type otpChallenge struct {
	Email            string
	AccountHash      string
	CodeHash         []byte
	MagicTokenHash   []byte
	DeviceFP         string
	ExpiresAt        time.Time
	RemainingAttempts int
}

type OtpService struct {
	mu            sync.Mutex
	hmacSecret    []byte
	verifyBaseURL string
	challenges    map[string]*otpChallenge
	linkIndex     map[string]string
	ttl           time.Duration
}

func NewOtpService(secret []byte, verifyBaseURL string) *OtpService {
	copied := make([]byte, len(secret))
	copy(copied, secret)
	base := strings.TrimRight(strings.TrimSpace(verifyBaseURL), "/")
	if base == "" {
		base = "https://telefam.company"
	}
	return &OtpService{
		hmacSecret:    copied,
		verifyBaseURL: base,
		challenges:    make(map[string]*otpChallenge),
		linkIndex:     make(map[string]string),
		ttl:           15 * time.Minute,
	}
}

type IssuedChallenge struct {
	OtpCode          string
	VerificationLink string
	ExpiresInSeconds int64
}

func (s *OtpService) CancelChallenge(accountHash string) {
	s.mu.Lock()
	defer s.mu.Unlock()
	if old, exists := s.challenges[accountHash]; exists {
		delete(s.linkIndex, hex.EncodeToString(old.MagicTokenHash))
		delete(s.challenges, accountHash)
	}
}

func (s *OtpService) IssueChallenge(email, accountHash, deviceFP string) (IssuedChallenge, error) {
	code, err := generateSixDigitCode()
	if err != nil {
		return IssuedChallenge{}, err
	}
	linkSecretBytes := make([]byte, 24)
	if _, err := rand.Read(linkSecretBytes); err != nil {
		return IssuedChallenge{}, err
	}
	rawLinkToken := base64.RawURLEncoding.EncodeToString(linkSecretBytes)

	codeDigest := s.digest("otp:" + accountHash + ":" + code)
	linkDigest := s.digest("link:" + rawLinkToken)
	linkKey := hex.EncodeToString(linkDigest)

	s.mu.Lock()
	defer s.mu.Unlock()

	if old, exists := s.challenges[accountHash]; exists {
		delete(s.linkIndex, hex.EncodeToString(old.MagicTokenHash))
	}

	s.challenges[accountHash] = &otpChallenge{
		Email:             strings.ToLower(strings.TrimSpace(email)),
		AccountHash:       accountHash,
		CodeHash:          codeDigest,
		MagicTokenHash:    linkDigest,
		DeviceFP:          deviceFP,
		ExpiresAt:         time.Now().Add(s.ttl),
		RemainingAttempts: 5,
	}
	s.linkIndex[linkKey] = accountHash

	return IssuedChallenge{
		OtpCode:          code,
		VerificationLink: fmt.Sprintf("%s/verify?token=%s", s.verifyBaseURL, rawLinkToken),
		ExpiresInSeconds: int64(s.ttl.Seconds()),
	}, nil
}

func (s *OtpService) VerifyCode(accountHash, code string) (string, error) {
	s.mu.Lock()
	defer s.mu.Unlock()

	ch, ok := s.challenges[accountHash]
	if !ok || time.Now().After(ch.ExpiresAt) {
		delete(s.challenges, accountHash)
		return "", ErrVerificationFailed
	}
	if ch.RemainingAttempts <= 0 {
		delete(s.challenges, accountHash)
		return "", ErrMaxAttemptsReached
	}

	candidate := s.digest("otp:" + accountHash + ":" + strings.TrimSpace(code))
	if subtle.ConstantTimeCompare(candidate, ch.CodeHash) != 1 {
		ch.RemainingAttempts--
		return "", ErrVerificationFailed
	}

	email := ch.Email
	delete(s.linkIndex, hex.EncodeToString(ch.MagicTokenHash))
	delete(s.challenges, accountHash)
	return email, nil
}

func (s *OtpService) VerifyMagicLink(rawToken string) (string, string, error) {
	linkDigest := s.digest("link:" + strings.TrimSpace(rawToken))
	linkKey := hex.EncodeToString(linkDigest)

	s.mu.Lock()
	defer s.mu.Unlock()

	accountHash, ok := s.linkIndex[linkKey]
	if !ok {
		return "", "", ErrVerificationFailed
	}
	ch, ok := s.challenges[accountHash]
	if !ok || time.Now().After(ch.ExpiresAt) {
		delete(s.linkIndex, linkKey)
		delete(s.challenges, accountHash)
		return "", "", ErrVerificationFailed
	}

	email := ch.Email
	delete(s.linkIndex, linkKey)
	delete(s.challenges, accountHash)
	return email, accountHash, nil
}

func (s *OtpService) digest(payload string) []byte {
	mac := hmac.New(sha256.New, s.hmacSecret)
	mac.Write([]byte(payload))
	return mac.Sum(nil)
}

func generateSixDigitCode() (string, error) {
	n, err := rand.Int(rand.Reader, big.NewInt(1000000))
	if err != nil {
		return "", err
	}
	return fmt.Sprintf("%06d", n.Int64()), nil
}
