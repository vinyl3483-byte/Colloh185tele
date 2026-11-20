package security

import (
	"errors"
	"regexp"
	"strings"
	"time"
	"unicode"
)

var (
	ErrInvalidInput    = errors.New("invalid input")
	ErrUnderMinimumAge = errors.New("minimum age 13 required")

	usernameRegex = regexp.MustCompile(`^[a-z0-9_]{3,24}$`)
	phoneRegex    = regexp.MustCompile(`^\+[1-9][0-9]{5,14}$`)
	isoRegex      = regexp.MustCompile(`^[A-Z]{2}$`)
	htmlTagRegex  = regexp.MustCompile(`<[^>]*>`)
)

// SanitizeText strips control runes, null bytes, zero-width spoofing characters,
// and HTML tags while enforcing a maximum rune length.
func SanitizeText(raw string, maxRunes int, allowNewlines bool) string {
	cleaned := htmlTagRegex.ReplaceAllString(raw, "")
	var b strings.Builder
	b.Grow(len(cleaned))

	runeCount := 0
	for _, r := range cleaned {
		if r == 0 || r == '\u200B' || r == '\u200C' || r == '\u200D' || r == '\uFEFF' {
			continue
		}
		if unicode.IsControl(r) {
			if allowNewlines && r == '\n' {
				b.WriteRune(' ')
				runeCount++
			}
			continue
		}
		b.WriteRune(r)
		runeCount++
		if maxRunes > 0 && runeCount >= maxRunes {
			break
		}
	}
	return strings.TrimSpace(b.String())
}

func SanitizeUsername(raw string) (string, error) {
	u := strings.ToLower(strings.TrimSpace(raw))
	u = strings.TrimPrefix(u, "@")
	if !usernameRegex.MatchString(u) {
		return "", ErrInvalidInput
	}
	return u, nil
}

func SanitizePhoneE164(raw string, countryISO string) (string, string, error) {
	iso := strings.ToUpper(strings.TrimSpace(countryISO))
	if !isoRegex.MatchString(iso) {
		return "", "", ErrInvalidInput
	}
	var b strings.Builder
	for i, r := range strings.TrimSpace(raw) {
		if i == 0 && r == '+' {
			b.WriteRune(r)
		} else if r >= '0' && r <= '9' {
			b.WriteRune(r)
		}
	}
	normalized := b.String()
	if !phoneRegex.MatchString(normalized) {
		return "", "", ErrInvalidInput
	}
	return normalized, iso, nil
}

func ValidateGender(raw string) (string, error) {
	switch strings.TrimSpace(raw) {
	case "Male", "Female", "Non-binary", "Prefer not to say":
		return strings.TrimSpace(raw), nil
	default:
		return "", ErrInvalidInput
	}
}

// ValidateDateOfBirthAtLeast13 parses YYYY-MM-DD and rejects any DOB under 13 years old.
func ValidateDateOfBirthAtLeast13(dobStr string, now time.Time) (time.Time, error) {
	dob, err := time.Parse("2006-01-02", strings.TrimSpace(dobStr))
	if err != nil {
		return time.Time{}, ErrInvalidInput
	}
	utcNow := now.UTC()
	cutoff := time.Date(utcNow.Year()-13, utcNow.Month(), utcNow.Day(), 0, 0, 0, 0, time.UTC)
	if dob.After(cutoff) || dob.Year() < 1900 {
		return time.Time{}, ErrUnderMinimumAge
	}
	return dob, nil
}

func AutofillFromEmail(email string) (fullName string, username string) {
	parts := strings.SplitN(strings.TrimSpace(email), "@", 2)
	prefix := "telefam"
	if len(parts) > 0 && parts[0] != "" {
		prefix = parts[0]
	}

	var nameWords []string
	for _, token := range strings.FieldsFunc(prefix, func(r rune) bool {
		return r == '.' || r == '_' || r == '-' || r == '+'
	}) {
		if len(token) > 0 {
			nameWords = append(nameWords, strings.ToUpper(token[:1])+strings.ToLower(token[1:]))
		}
	}
	fullName = strings.Join(nameWords, " ")
	if fullName == "" {
		fullName = "Telefam Member"
	}

	var userBuilder strings.Builder
	for _, r := range strings.ToLower(prefix) {
		if (r >= 'a' && r <= 'z') || (r >= '0' && r <= '9') || r == '_' {
			userBuilder.WriteRune(r)
		} else if r == '.' || r == '-' {
			userBuilder.WriteRune('_')
		}
	}
	username = strings.Trim(userBuilder.String(), "_")
	for len(username) < 3 {
		username += "0"
	}
	if len(username) > 24 {
		username = username[:24]
	}
	return fullName, username
}
