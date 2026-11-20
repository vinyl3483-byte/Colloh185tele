package auth

import (
	"crypto/hmac"
	"crypto/rand"
	"crypto/sha256"
	"encoding/base64"
	"encoding/hex"
	"encoding/json"
	"errors"
	"fmt"
	"strings"
	"sync"
	"time"
)

const (
	AccessTokenTTL  = 15 * time.Minute
	RefreshTokenTTL = 30 * 24 * time.Hour
)

var (
	ErrTokenInvalid     = errors.New("token invalid")
	ErrTokenReuseAttack = errors.New("refresh token reuse detected")
)

type AccessClaims struct {
	UserID     string `json:"uid"`
	Email      string `json:"eml"`
	DeviceFP   string `json:"dfp"`
	FamilyID   string `json:"fid"`
	IssuedAt   int64  `json:"iat"`
	IssuedAtMs int64  `json:"iat_ms,omitempty"`
	ExpiresAt  int64  `json:"exp"`
}

type TokenPair struct {
	AccessToken         string `json:"access_token"`
	RefreshToken        string `json:"refresh_token"`
	AccessExpiresInSec  int64  `json:"access_expires_in_seconds"`
	RefreshExpiresInSec int64  `json:"refresh_expires_in_seconds"`
}

type refreshRecord struct {
	TokenID    string
	FamilyID   string
	UserID     string
	Email      string
	DeviceFP   string
	CreatedAt  time.Time
	ExpiresAt  time.Time
	Revoked    bool
	ReplacedBy string
}

type TokenService struct {
	mu                  sync.Mutex
	signingSecret       []byte
	refreshStore        map[string]*refreshRecord // key: sha256(refreshToken)
	familyRevoked       map[string]bool
	userRevokedBeforeMs map[string]int64
}

func NewTokenService(secret []byte) *TokenService {
	copied := make([]byte, len(secret))
	copy(copied, secret)
	return &TokenService{
		signingSecret:       copied,
		refreshStore:        make(map[string]*refreshRecord),
		familyRevoked:       make(map[string]bool),
		userRevokedBeforeMs: make(map[string]int64),
	}
}

func (ts *TokenService) IssueInitialPair(userID, email, deviceFP string) (TokenPair, string, error) {
	familyID := randomHex(16)
	return ts.issueForFamily(userID, email, deviceFP, familyID)
}

// RevokeFamily invalidates all tokens in a single device's refresh family.
func (ts *TokenService) RevokeFamily(familyID string) {
	if familyID == "" {
		return
	}
	ts.mu.Lock()
	defer ts.mu.Unlock()
	ts.familyRevoked[familyID] = true
	for _, item := range ts.refreshStore {
		if item.FamilyID == familyID {
			item.Revoked = true
		}
	}
}

// RevokeAllUserSessions immediately revokes every device session for the user.
func (ts *TokenService) RevokeAllUserSessions(userID string) {
	if userID == "" {
		return
	}
	nowMs := time.Now().UnixMilli()
	ts.mu.Lock()
	defer ts.mu.Unlock()
	if prev := ts.userRevokedBeforeMs[userID]; nowMs <= prev {
		nowMs = prev + 1
	}
	ts.userRevokedBeforeMs[userID] = nowMs
	for _, item := range ts.refreshStore {
		if item.UserID == userID {
			ts.familyRevoked[item.FamilyID] = true
			item.Revoked = true
		}
	}
}

// RotateRefreshToken enforces single-use rotating refresh tokens.
// If a previously revoked/rotated refresh token is presented, the entire token
// family is revoked immediately to prevent replay/theft.
func (ts *TokenService) RotateRefreshToken(rawRefreshToken, deviceFP string) (TokenPair, string, string, error) {
	hash := hashToken(rawRefreshToken)

	ts.mu.Lock()
	rec, ok := ts.refreshStore[hash]
	if !ok {
		ts.mu.Unlock()
		return TokenPair{}, "", "", ErrTokenInvalid
	}

	if cutoffMs := ts.userRevokedBeforeMs[rec.UserID]; cutoffMs > 0 && rec.CreatedAt.UnixMilli() <= cutoffMs {
		rec.Revoked = true
		ts.familyRevoked[rec.FamilyID] = true
		ts.mu.Unlock()
		return TokenPair{}, rec.UserID, rec.FamilyID, ErrTokenInvalid
	}

	if rec.Revoked || ts.familyRevoked[rec.FamilyID] {
		// Reuse attack detected -> revoke entire token family
		ts.familyRevoked[rec.FamilyID] = true
		for _, item := range ts.refreshStore {
			if item.FamilyID == rec.FamilyID {
				item.Revoked = true
			}
		}
		ts.mu.Unlock()
		return TokenPair{}, rec.UserID, rec.FamilyID, ErrTokenReuseAttack
	}

	if time.Now().After(rec.ExpiresAt) || rec.DeviceFP != deviceFP {
		rec.Revoked = true
		ts.mu.Unlock()
		return TokenPair{}, rec.UserID, rec.FamilyID, ErrTokenInvalid
	}

	rec.Revoked = true
	userID := rec.UserID
	email := rec.Email
	familyID := rec.FamilyID
	ts.mu.Unlock()

	pair, _, err := ts.issueForFamily(userID, email, deviceFP, familyID)
	return pair, userID, familyID, err
}

func (ts *TokenService) VerifyAccessToken(token string, expectedDeviceFP string) (AccessClaims, error) {
	parts := strings.Split(token, ".")
	if len(parts) != 2 {
		return AccessClaims{}, ErrTokenInvalid
	}
	payloadBytes, err := base64.RawURLEncoding.DecodeString(parts[0])
	if err != nil {
		return AccessClaims{}, ErrTokenInvalid
	}
	sigBytes, err := base64.RawURLEncoding.DecodeString(parts[1])
	if err != nil {
		return AccessClaims{}, ErrTokenInvalid
	}
	mac := hmac.New(sha256.New, ts.signingSecret)
	mac.Write(payloadBytes)
	if !hmac.Equal(sigBytes, mac.Sum(nil)) {
		return AccessClaims{}, ErrTokenInvalid
	}
	var claims AccessClaims
	if err := json.Unmarshal(payloadBytes, &claims); err != nil {
		return AccessClaims{}, ErrTokenInvalid
	}
	if time.Now().Unix() > claims.ExpiresAt || claims.DeviceFP != expectedDeviceFP {
		return AccessClaims{}, ErrTokenInvalid
	}

	ts.mu.Lock()
	revoked := ts.familyRevoked[claims.FamilyID]
	cutoffMs := ts.userRevokedBeforeMs[claims.UserID]
	ts.mu.Unlock()
	if revoked {
		return AccessClaims{}, ErrTokenInvalid
	}
	if cutoffMs > 0 {
		if claims.IssuedAtMs > 0 {
			if claims.IssuedAtMs <= cutoffMs {
				return AccessClaims{}, ErrTokenInvalid
			}
		} else if claims.IssuedAt <= cutoffMs/1000 {
			return AccessClaims{}, ErrTokenInvalid
		}
	}
	return claims, nil
}

func (ts *TokenService) issueForFamily(userID, email, deviceFP, familyID string) (TokenPair, string, error) {
	now := time.Now()
	nowMs := now.UnixMilli()
	ts.mu.Lock()
	if cutoffMs := ts.userRevokedBeforeMs[userID]; cutoffMs > 0 && nowMs <= cutoffMs {
		nowMs = cutoffMs + 1
		now = time.UnixMilli(nowMs)
	}
	ts.mu.Unlock()

	claims := AccessClaims{
		UserID:     userID,
		Email:      email,
		DeviceFP:   deviceFP,
		FamilyID:   familyID,
		IssuedAt:   now.Unix(),
		IssuedAtMs: nowMs,
		ExpiresAt:  now.Add(AccessTokenTTL).Unix(),
	}
	payload, err := json.Marshal(claims)
	if err != nil {
		return TokenPair{}, "", err
	}
	mac := hmac.New(sha256.New, ts.signingSecret)
	mac.Write(payload)
	accessToken := base64.RawURLEncoding.EncodeToString(payload) + "." +
		base64.RawURLEncoding.EncodeToString(mac.Sum(nil))

	rawRefresh := "tfr_" + randomHex(32)
	refreshHash := hashToken(rawRefresh)

	ts.mu.Lock()
	ts.refreshStore[refreshHash] = &refreshRecord{
		TokenID:   randomHex(12),
		FamilyID:  familyID,
		UserID:    userID,
		Email:     email,
		DeviceFP:  deviceFP,
		CreatedAt: now,
		ExpiresAt: now.Add(RefreshTokenTTL),
		Revoked:   false,
	}
	ts.mu.Unlock()

	return TokenPair{
		AccessToken:         accessToken,
		RefreshToken:        rawRefresh,
		AccessExpiresInSec:  int64(AccessTokenTTL.Seconds()),
		RefreshExpiresInSec: int64(RefreshTokenTTL.Seconds()),
	}, familyID, nil
}

func hashToken(raw string) string {
	sum := sha256.Sum256([]byte(raw))
	return hex.EncodeToString(sum[:])
}

func randomHex(nBytes int) string {
	b := make([]byte, nBytes)
	if _, err := rand.Read(b); err != nil {
		return fmt.Sprintf("%d", time.Now().UnixNano())
	}
	return hex.EncodeToString(b)
}
