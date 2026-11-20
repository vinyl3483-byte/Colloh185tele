package chat

import (
	"bufio"
	"crypto/sha1"
	"encoding/base64"
	"encoding/binary"
	"encoding/json"
	"errors"
	"io"
	"net"
	"net/http"
	"strings"
	"sync"
	"time"
)

const wsGUID = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11"

type wsClient struct {
	userID   string
	deviceFP string
	conn     net.Conn
	rw       *bufio.ReadWriter
	writeMu  sync.Mutex
	closed   bool
}

func (c *wsClient) sendJSON(v any) error {
	payload, err := json.Marshal(v)
	if err != nil {
		return err
	}
	return c.writeFrame(0x1, payload)
}

func (c *wsClient) writeFrame(opcode byte, payload []byte) error {
	c.writeMu.Lock()
	defer c.writeMu.Unlock()
	if c.closed {
		return errors.New("ws closed")
	}
	_ = c.conn.SetWriteDeadline(time.Now().Add(10 * time.Second))
	header := []byte{0x80 | (opcode & 0x0F)}
	n := len(payload)
	switch {
	case n < 126:
		header = append(header, byte(n))
	case n < 65536:
		header = append(header, 126, byte(n>>8), byte(n))
	default:
		ext := make([]byte, 9)
		ext[0] = 127
		binary.BigEndian.PutUint64(ext[1:], uint64(n))
		header = append(header, ext...)
	}
	if _, err := c.rw.Write(header); err != nil {
		c.closed = true
		return err
	}
	if n > 0 {
		if _, err := c.rw.Write(payload); err != nil {
			c.closed = true
			return err
		}
	}
	if err := c.rw.Flush(); err != nil {
		c.closed = true
		return err
	}
	return nil
}

func (c *wsClient) close() {
	c.writeMu.Lock()
	if !c.closed {
		c.closed = true
		_ = c.conn.Close()
	}
	c.writeMu.Unlock()
}

// upgradeWebSocket performs an RFC 6455 HTTP/1.1 WebSocket upgrade using stdlib only.
func upgradeWebSocket(w http.ResponseWriter, r *http.Request) (net.Conn, *bufio.ReadWriter, error) {
	if !strings.EqualFold(r.Header.Get("Upgrade"), "websocket") {
		return nil, nil, errors.New("missing websocket upgrade header")
	}
	key := strings.TrimSpace(r.Header.Get("Sec-WebSocket-Key"))
	if key == "" {
		return nil, nil, errors.New("missing websocket key")
	}
	hj, ok := w.(http.Hijacker)
	if !ok {
		return nil, nil, errors.New("hijacking unsupported")
	}
	conn, rw, err := hj.Hijack()
	if err != nil {
		return nil, nil, err
	}

	h := sha1.Sum([]byte(key + wsGUID))
	accept := base64.StdEncoding.EncodeToString(h[:])
	resp := "HTTP/1.1 101 Switching Protocols\r\n" +
		"Upgrade: websocket\r\n" +
		"Connection: Upgrade\r\n" +
		"Sec-WebSocket-Accept: " + accept + "\r\n\r\n"
	if _, err := rw.WriteString(resp); err != nil {
		_ = conn.Close()
		return nil, nil, err
	}
	if err := rw.Flush(); err != nil {
		_ = conn.Close()
		return nil, nil, err
	}
	return conn, rw, nil
}

func readWSFrame(r *bufio.Reader) (opcode byte, payload []byte, err error) {
	var b0, b1 byte
	b0, err = r.ReadByte()
	if err != nil {
		return 0, nil, err
	}
	b1, err = r.ReadByte()
	if err != nil {
		return 0, nil, err
	}
	opcode = b0 & 0x0F
	masked := (b1 & 0x80) != 0
	length := int64(b1 & 0x7F)
	switch length {
	case 126:
		var ext [2]byte
		if _, err = io.ReadFull(r, ext[:]); err != nil {
			return 0, nil, err
		}
		length = int64(binary.BigEndian.Uint16(ext[:]))
	case 127:
		var ext [8]byte
		if _, err = io.ReadFull(r, ext[:]); err != nil {
			return 0, nil, err
		}
		length = int64(binary.BigEndian.Uint64(ext[:]))
	}
	if length < 0 || length > 4*1024*1024 {
		return 0, nil, errors.New("ws frame too large")
	}
	var mask [4]byte
	if masked {
		if _, err = io.ReadFull(r, mask[:]); err != nil {
			return 0, nil, err
		}
	}
	payload = make([]byte, length)
	if length > 0 {
		if _, err = io.ReadFull(r, payload); err != nil {
			return 0, nil, err
		}
		if masked {
			for i := int64(0); i < length; i++ {
				payload[i] ^= mask[i%4]
			}
		}
	}
	return opcode, payload, nil
}

// ServeWebSocket upgrades and streams real-time chat events for an authenticated user.
func (s *Service) ServeWebSocket(w http.ResponseWriter, r *http.Request, userID, deviceFP string) {
	conn, rw, err := upgradeWebSocket(w, r)
	if err != nil {
		http.Error(w, "websocket upgrade failed", http.StatusBadRequest)
		return
	}
	client := &wsClient{
		userID:   userID,
		deviceFP: deviceFP,
		conn:     conn,
		rw:       rw,
	}
	s.registerWSClient(client)
	defer s.unregisterWSClient(client)

	_ = client.sendJSON(WebSocketEvent{
		Event:       "connected",
		UserID:      userID,
		TimestampMs: s.nowMs(),
	})

	type incomingWS struct {
		Event          string `json:"event"`
		ConversationID string `json:"conversation_id"`
		TypingMode     string `json:"typing_mode"`
		MessageID      string `json:"message_id"`
		Status         string `json:"status"`
	}

	for {
		_ = conn.SetReadDeadline(time.Now().Add(90 * time.Second))
		op, payload, readErr := readWSFrame(rw.Reader)
		if readErr != nil {
			return
		}
		switch op {
		case 0x8: // close
			_ = client.writeFrame(0x8, nil)
			return
		case 0x9: // ping -> pong
			_ = client.writeFrame(0xA, payload)
		case 0x1: // text JSON
			var msg incomingWS
			if json.Unmarshal(payload, &msg) != nil {
				continue
			}
			switch strings.ToLower(strings.TrimSpace(msg.Event)) {
			case "ping":
				_ = client.sendJSON(WebSocketEvent{
					Event:       "pong",
					TimestampMs: s.nowMs(),
				})
			case "typing":
				s.HandleTypingSignal(userID, msg.ConversationID, msg.TypingMode)
			case "receipt":
				_, _ = s.UpdateMessageReceipt(userID, msg.MessageID, msg.Status)
			case "call_signal":
				if s.hooks.OnCallSignalWS != nil {
					s.hooks.OnCallSignalWS(userID, payload)
				}
			}
		}
	}
}
