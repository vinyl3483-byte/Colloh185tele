package audit

import (
	"log"
	"sync"
	"time"
)

type Event struct {
	ID        int64             `json:"id"`
	UserID    string            `json:"user_id"`
	Action    string            `json:"action"`
	DeviceFP  string            `json:"device_fp"`
	IPFP      string            `json:"ip_fp"`
	Metadata  map[string]string `json:"metadata,omitempty"`
	CreatedAt time.Time         `json:"created_at"`
}

type Logger struct {
	mu     sync.Mutex
	nextID int64
	events []Event
}

func NewLogger() *Logger {
	return &Logger{nextID: 1}
}

func (l *Logger) Record(userID, action, deviceFP, ipFP string, metadata map[string]string) {
	l.mu.Lock()
	evt := Event{
		ID:        l.nextID,
		UserID:    userID,
		Action:    action,
		DeviceFP:  deviceFP,
		IPFP:      ipFP,
		Metadata:  metadata,
		CreatedAt: time.Now().UTC(),
	}
	l.nextID++
	l.events = append(l.events, evt)
	if len(l.events) > 10_000 {
		l.events = l.events[len(l.events)-5_000:]
	}
	l.mu.Unlock()

	log.Printf("[telefam-audit] id=%d action=%s user=%s device=%s", evt.ID, evt.Action, evt.UserID, safePrefix(evt.DeviceFP))
}

func safePrefix(s string) string {
	if len(s) <= 12 {
		return s
	}
	return s[:12]
}
