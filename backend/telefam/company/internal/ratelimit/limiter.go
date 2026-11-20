package ratelimit

import (
	"sync"
	"time"
)

// Escalating resend cooldown schedule:
// Resends 1, 2, 3 -> 1 minute (60s)
// Resend 4        -> 15 minutes (900s)
// Resend 5        -> 1 hour (3600s)
// Resend 6+       -> 1 day (86400s)
var EscalatingCooldownSchedule = []time.Duration{
	1 * time.Minute,
	1 * time.Minute,
	1 * time.Minute,
	15 * time.Minute,
	1 * time.Hour,
	24 * time.Hour,
}

type WindowPolicy struct {
	MaxRequests int
	Window      time.Duration
}

type Decision struct {
	Allowed         bool
	CooldownSeconds int64
}

type accountCooldownState struct {
	SendCount   int
	NextAllowed time.Time
	LastUpdated time.Time
}

type slidingBucket struct {
	Timestamps []time.Time
}

type Limiter struct {
	mu            sync.Mutex
	accountPolicy WindowPolicy
	devicePolicy  WindowPolicy
	ipPolicy      WindowPolicy
	accountHits   map[string]*slidingBucket
	deviceHits    map[string]*slidingBucket
	ipHits        map[string]*slidingBucket
	cooldowns     map[string]*accountCooldownState
	nowFunc       func() time.Time
}

func NewLimiter() *Limiter {
	return &Limiter{
		accountPolicy: WindowPolicy{MaxRequests: 12, Window: 24 * time.Hour},
		devicePolicy:  WindowPolicy{MaxRequests: 20, Window: 24 * time.Hour},
		ipPolicy:      WindowPolicy{MaxRequests: 40, Window: 24 * time.Hour},
		accountHits:   make(map[string]*slidingBucket),
		deviceHits:    make(map[string]*slidingBucket),
		ipHits:        make(map[string]*slidingBucket),
		cooldowns:     make(map[string]*accountCooldownState),
		nowFunc:       time.Now,
	}
}

func CooldownDurationForCount(sendCountAfterThisRequest int) time.Duration {
	if sendCountAfterThisRequest <= 0 {
		return EscalatingCooldownSchedule[0]
	}
	idx := sendCountAfterThisRequest - 1
	if idx >= len(EscalatingCooldownSchedule) {
		idx = len(EscalatingCooldownSchedule) - 1
	}
	return EscalatingCooldownSchedule[idx]
}

// CheckAndRecordOtpRequest enforces rate limits across account, device fingerprint,
// and IP fingerprint simultaneously, plus the escalating account/device cooldown.
func (l *Limiter) CheckAndRecordOtpRequest(accountHash, deviceFP, ipFP string) Decision {
	d := l.CheckOtpAllowed(accountHash, deviceFP, ipFP)
	if !d.Allowed {
		return d
	}
	return l.CommitOtpSent(accountHash, deviceFP, ipFP)
}

// CheckOtpAllowed verifies cooldown and sliding-window quotas without mutating send count.
func (l *Limiter) CheckOtpAllowed(accountHash, deviceFP, ipFP string) Decision {
	l.mu.Lock()
	defer l.mu.Unlock()

	now := l.nowFunc()
	cooldownKey := accountHash + ":" + deviceFP
	if st, ok := l.cooldowns[cooldownKey]; ok {
		if now.Sub(st.LastUpdated) >= 24*time.Hour {
			delete(l.cooldowns, cooldownKey)
		} else if now.Before(st.NextAllowed) {
			remaining := int64(st.NextAllowed.Sub(now).Seconds())
			if remaining < 1 {
				remaining = 1
			}
			return Decision{Allowed: false, CooldownSeconds: remaining}
		}
	}

	if !l.withinQuota(l.accountHits, accountHash, l.accountPolicy, now) ||
		!l.withinQuota(l.deviceHits, deviceFP, l.devicePolicy, now) ||
		!l.withinQuota(l.ipHits, ipFP, l.ipPolicy, now) {
		return Decision{Allowed: false, CooldownSeconds: 0}
	}

	nextSendCount := 1
	if st, ok := l.cooldowns[cooldownKey]; ok {
		nextSendCount = st.SendCount + 1
	}
	cooldown := CooldownDurationForCount(nextSendCount)
	return Decision{
		Allowed:         true,
		CooldownSeconds: int64(cooldown.Seconds()),
	}
}

// CommitOtpSent records hits across account/device/IP and advances the escalating cooldown.
func (l *Limiter) CommitOtpSent(accountHash, deviceFP, ipFP string) Decision {
	l.mu.Lock()
	defer l.mu.Unlock()

	now := l.nowFunc()
	cooldownKey := accountHash + ":" + deviceFP

	l.recordHit(l.accountHits, accountHash, now)
	l.recordHit(l.deviceHits, deviceFP, now)
	l.recordHit(l.ipHits, ipFP, now)

	st, ok := l.cooldowns[cooldownKey]
	if !ok {
		st = &accountCooldownState{}
		l.cooldowns[cooldownKey] = st
	}
	st.SendCount++
	cooldown := CooldownDurationForCount(st.SendCount)
	st.NextAllowed = now.Add(cooldown)
	st.LastUpdated = now

	return Decision{
		Allowed:         true,
		CooldownSeconds: int64(cooldown.Seconds()),
	}
}

func (l *Limiter) CheckVerifyAttempt(accountHash, deviceFP, ipFP string) bool {
	l.mu.Lock()
	defer l.mu.Unlock()

	now := l.nowFunc()
	verifyPolicy := WindowPolicy{MaxRequests: 15, Window: 15 * time.Minute}
	if !l.withinQuota(l.accountHits, "v:"+accountHash, verifyPolicy, now) ||
		!l.withinQuota(l.deviceHits, "v:"+deviceFP, verifyPolicy, now) ||
		!l.withinQuota(l.ipHits, "v:"+ipFP, verifyPolicy, now) {
		return false
	}
	l.recordHit(l.accountHits, "v:"+accountHash, now)
	l.recordHit(l.deviceHits, "v:"+deviceFP, now)
	l.recordHit(l.ipHits, "v:"+ipFP, now)
	return true
}

func (l *Limiter) AllowLinkPreview(userID, deviceFP, ipFP string) bool {
	l.mu.Lock()
	defer l.mu.Unlock()

	now := l.nowFunc()
	previewPolicy := WindowPolicy{MaxRequests: 60, Window: 1 * time.Minute}
	if !l.withinQuota(l.accountHits, "lp:"+userID, previewPolicy, now) ||
		!l.withinQuota(l.deviceHits, "lp:"+deviceFP, previewPolicy, now) ||
		!l.withinQuota(l.ipHits, "lp:"+ipFP, previewPolicy, now) {
		return false
	}
	l.recordHit(l.accountHits, "lp:"+userID, now)
	l.recordHit(l.deviceHits, "lp:"+deviceFP, now)
	l.recordHit(l.ipHits, "lp:"+ipFP, now)
	return true
}

func (l *Limiter) ResetAccountCooldown(accountHash, deviceFP string) {
	l.mu.Lock()
	defer l.mu.Unlock()
	delete(l.cooldowns, accountHash+":"+deviceFP)
}

func (l *Limiter) withinQuota(store map[string]*slidingBucket, key string, policy WindowPolicy, now time.Time) bool {
	bucket, ok := store[key]
	if !ok {
		return true
	}
	cutoff := now.Add(-policy.Window)
	pruned := bucket.Timestamps[:0]
	for _, ts := range bucket.Timestamps {
		if ts.After(cutoff) {
			pruned = append(pruned, ts)
		}
	}
	bucket.Timestamps = pruned
	return len(bucket.Timestamps) < policy.MaxRequests
}

func (l *Limiter) recordHit(store map[string]*slidingBucket, key string, now time.Time) {
	bucket, ok := store[key]
	if !ok {
		bucket = &slidingBucket{}
		store[key] = bucket
	}
	bucket.Timestamps = append(bucket.Timestamps, now)
}
