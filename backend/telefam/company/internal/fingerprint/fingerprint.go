package fingerprint

import (
	"crypto/hmac"
	"crypto/sha256"
	"encoding/hex"
	"errors"
	"net"
	"net/http"
	"strings"
)

var ErrInvalidDeviceFingerprint = errors.New("invalid device fingerprint")

type Context struct {
	AccountHash       string
	DeviceFingerprint string
	IPFingerprint     string
}

type Service struct {
	hmacSecret []byte
}

func NewService(secret []byte) *Service {
	copied := make([]byte, len(secret))
	copy(copied, secret)
	return &Service{hmacSecret: copied}
}

func (s *Service) HashAccount(email string) string {
	normalized := strings.ToLower(strings.TrimSpace(email))
	return s.hmacHex("acct:" + normalized)
}

func (s *Service) ExtractDeviceFingerprint(r *http.Request) (string, error) {
	raw := strings.TrimSpace(r.Header.Get("X-Telefam-Device-FP"))
	if len(raw) < 16 || len(raw) > 256 {
		return "", ErrInvalidDeviceFingerprint
	}
	for _, c := range raw {
		if !((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '-' || c == '_') {
			return "", ErrInvalidDeviceFingerprint
		}
	}
	return s.hmacHex("dev:" + raw), nil
}

func (s *Service) ExtractIPFingerprint(r *http.Request) string {
	ipStr := parseClientIP(r)
	ip := net.ParseIP(ipStr)
	if ip == nil {
		return s.hmacHex("ip:unknown")
	}
	if v4 := ip.To4(); v4 != nil {
		return s.hmacHex("ip4:" + v4.String())
	}
	// Privacy-preserving /64 subnet fingerprint for IPv6
	masked := ip.Mask(net.CIDRMask(64, 128))
	return s.hmacHex("ip6:" + masked.String())
}

func (s *Service) BuildContext(r *http.Request, email string) (Context, error) {
	deviceFP, err := s.ExtractDeviceFingerprint(r)
	if err != nil {
		return Context{}, err
	}
	return Context{
		AccountHash:       s.HashAccount(email),
		DeviceFingerprint: deviceFP,
		IPFingerprint:     s.ExtractIPFingerprint(r),
	}, nil
}

func parseClientIP(r *http.Request) string {
	if xff := r.Header.Get("X-Forwarded-For"); xff != "" {
		parts := strings.Split(xff, ",")
		if len(parts) > 0 {
			candidate := strings.TrimSpace(parts[0])
			if net.ParseIP(candidate) != nil {
				return candidate
			}
		}
	}
	if xri := strings.TrimSpace(r.Header.Get("X-Real-IP")); xri != "" {
		if net.ParseIP(xri) != nil {
			return xri
		}
	}
	host, _, err := net.SplitHostPort(r.RemoteAddr)
	if err == nil && net.ParseIP(host) != nil {
		return host
	}
	return strings.TrimSpace(r.RemoteAddr)
}

func (s *Service) hmacHex(payload string) string {
	mac := hmac.New(sha256.New, s.hmacSecret)
	mac.Write([]byte(payload))
	return hex.EncodeToString(mac.Sum(nil))
}
