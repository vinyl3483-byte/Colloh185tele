-- Telefam 1-on-1 End-to-End Encrypted Chat (Signal Protocol), Voice View-Once & Security Flags
-- Enforces Strict PostgreSQL Row-Level Security (RLS) on all tables.

-- 1. E2EE Signal Identity, Signed PreKey & Kyber PQXDH Device Keys
CREATE TABLE IF NOT EXISTS telefam_e2ee_device_keys (
    user_id               TEXT NOT NULL,
    device_fp             TEXT NOT NULL,
    registration_id       INTEGER NOT NULL,
    identity_pub_b64      TEXT NOT NULL,
    signed_prekey_id      INTEGER NOT NULL,
    signed_prekey_pub_b64 TEXT NOT NULL,
    signed_prekey_sig_b64 TEXT NOT NULL,
    kyber_prekey_id       INTEGER NOT NULL DEFAULT 0,
    kyber_prekey_pub_b64  TEXT NOT NULL DEFAULT '',
    kyber_prekey_sig_b64  TEXT NOT NULL DEFAULT '',
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_id, device_fp)
);

-- 2. Signal One-Time PreKeys (Atomically Consumed on X3DH Session Init)
CREATE TABLE IF NOT EXISTS telefam_e2ee_one_time_prekeys (
    id                  BIGSERIAL PRIMARY KEY,
    user_id             TEXT NOT NULL,
    device_fp           TEXT NOT NULL,
    key_id              INTEGER NOT NULL,
    public_key_b64      TEXT NOT NULL,
    consumed_by_user_id TEXT NULL,
    consumed_at         TIMESTAMPTZ NULL,
    UNIQUE (user_id, device_fp, key_id)
);

CREATE INDEX IF NOT EXISTS idx_e2ee_otk_available
    ON telefam_e2ee_one_time_prekeys (user_id, consumed_by_user_id)
    WHERE consumed_by_user_id IS NULL;

-- 3. Canonical 1-on-1 Chat Conversations (user_a < user_b)
CREATE TABLE IF NOT EXISTS telefam_chat_conversations (
    conversation_id      TEXT PRIMARY KEY,
    user_a               TEXT NOT NULL,
    user_b               TEXT NOT NULL,
    last_message_id      TEXT NOT NULL DEFAULT '',
    last_message_type    TEXT NOT NULL DEFAULT '',
    last_sender_id       TEXT NOT NULL DEFAULT '',
    last_message_at_ms   BIGINT NOT NULL DEFAULT 0,
    archived_a           BOOLEAN NOT NULL DEFAULT FALSE,
    archived_b           BOOLEAN NOT NULL DEFAULT FALSE,
    muted_a              BOOLEAN NOT NULL DEFAULT FALSE,
    muted_b              BOOLEAN NOT NULL DEFAULT FALSE,
    unread_a             INTEGER NOT NULL DEFAULT 0,
    unread_b             INTEGER NOT NULL DEFAULT 0,
    marked_unread_a      BOOLEAN NOT NULL DEFAULT FALSE,
    marked_unread_b      BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_before_a_ms  BIGINT NOT NULL DEFAULT 0,
    deleted_before_b_ms  BIGINT NOT NULL DEFAULT 0,
    created_at_ms        BIGINT NOT NULL,
    updated_at_ms        BIGINT NOT NULL,
    UNIQUE (user_a, user_b),
    CHECK (user_a < user_b)
);

-- 4. Voice Note Encrypted Media Blobs (Purged physically upon unsend or view-once expiry)
CREATE TABLE IF NOT EXISTS telefam_chat_voice_media (
    media_id           TEXT PRIMARY KEY,
    conversation_id    TEXT NOT NULL REFERENCES telefam_chat_conversations(conversation_id) ON DELETE CASCADE,
    uploader_user_id   TEXT NOT NULL,
    recipient_user_id  TEXT NOT NULL,
    mime_type          TEXT NOT NULL DEFAULT 'audio/mp4',
    sha256_fingerprint CHAR(64) NOT NULL,
    byte_size          INTEGER NOT NULL CHECK (byte_size > 0 AND byte_size <= 10485760),
    duration_ms        BIGINT NOT NULL DEFAULT 0,
    is_view_once       BOOLEAN NOT NULL DEFAULT FALSE,
    created_at_ms      BIGINT NOT NULL
);

-- 5. E2EE Compressed Chat Messages (Backend only stores ciphertext blobs, never plaintext)
CREATE TABLE IF NOT EXISTS telefam_chat_messages (
    message_id              TEXT PRIMARY KEY,
    conversation_id         TEXT NOT NULL REFERENCES telefam_chat_conversations(conversation_id) ON DELETE CASCADE,
    sender_user_id          TEXT NOT NULL,
    recipient_user_id       TEXT NOT NULL,
    client_op_id            TEXT NOT NULL UNIQUE,
    message_type            TEXT NOT NULL CHECK (message_type IN ('text', 'sticker', 'gif', 'clip', 'voice', 'voice_once')),
    compression_codec       TEXT NOT NULL DEFAULT 'zlib',
    encryption_scheme       TEXT NOT NULL DEFAULT 'libsignal_v1',
    ciphertext_b64          TEXT NOT NULL,
    sender_ciphertext_b64   TEXT NOT NULL DEFAULT '',
    signal_msg_type         INTEGER NOT NULL DEFAULT 3,
    media_url               TEXT NOT NULL DEFAULT '',
    media_id                TEXT NOT NULL DEFAULT '',
    voice_duration_ms       BIGINT NOT NULL DEFAULT 0,
    view_once_ttl_sec       INTEGER NOT NULL DEFAULT 0,
    view_once_opened_at_ms  BIGINT NOT NULL DEFAULT 0,
    view_once_expires_at_ms BIGINT NOT NULL DEFAULT 0,
    forwarded_from_user_id  TEXT NOT NULL DEFAULT '',
    delivery_status         TEXT NOT NULL DEFAULT 'sent' CHECK (delivery_status IN ('sent', 'delivered', 'seen')),
    edited_at_ms            BIGINT NOT NULL DEFAULT 0,
    deleted_for_a           BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_for_b           BOOLEAN NOT NULL DEFAULT FALSE,
    created_at_ms           BIGINT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_chat_messages_conv_time
    ON telefam_chat_messages (conversation_id, created_at_ms ASC);

-- 6. Persistent Screenshot & View-Once Security Flags
CREATE TABLE IF NOT EXISTS telefam_chat_security_flags (
    flag_id          TEXT PRIMARY KEY,
    conversation_id  TEXT NOT NULL REFERENCES telefam_chat_conversations(conversation_id) ON DELETE CASCADE,
    message_id       TEXT NOT NULL DEFAULT '',
    actor_user_id    TEXT NOT NULL,
    target_user_id   TEXT NOT NULL,
    flag_type        TEXT NOT NULL CHECK (flag_type IN ('screenshot_disallowed', 'view_once_opened', 'view_once_capture_attempt')),
    details          TEXT NOT NULL DEFAULT '',
    created_at_ms    BIGINT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_chat_flags_conv_time
    ON telefam_chat_security_flags (conversation_id, created_at_ms DESC);

-- ============================================================================
-- STRICT POSTGRESQL ROW-LEVEL SECURITY (RLS)
-- ============================================================================
ALTER TABLE telefam_e2ee_device_keys      ENABLE ROW LEVEL SECURITY;
ALTER TABLE telefam_e2ee_device_keys      FORCE ROW LEVEL SECURITY;

ALTER TABLE telefam_e2ee_one_time_prekeys ENABLE ROW LEVEL SECURITY;
ALTER TABLE telefam_e2ee_one_time_prekeys FORCE ROW LEVEL SECURITY;

ALTER TABLE telefam_chat_conversations    ENABLE ROW LEVEL SECURITY;
ALTER TABLE telefam_chat_conversations    FORCE ROW LEVEL SECURITY;

ALTER TABLE telefam_chat_voice_media      ENABLE ROW LEVEL SECURITY;
ALTER TABLE telefam_chat_voice_media      FORCE ROW LEVEL SECURITY;

ALTER TABLE telefam_chat_messages         ENABLE ROW LEVEL SECURITY;
ALTER TABLE telefam_chat_messages         FORCE ROW LEVEL SECURITY;

ALTER TABLE telefam_chat_security_flags   ENABLE ROW LEVEL SECURITY;
ALTER TABLE telefam_chat_security_flags   FORCE ROW LEVEL SECURITY;

-- E2EE keys: owner full access; non-blocked authenticated peers read public keys
DROP POLICY IF EXISTS e2ee_keys_owner_all ON telefam_e2ee_device_keys;
CREATE POLICY e2ee_keys_owner_all ON telefam_e2ee_device_keys
    FOR ALL
    USING (
        user_id = COALESCE(NULLIF(current_setting('app.current_user_id', true), ''), NULLIF(current_setting('telefam.current_user_id', true), ''))
    )
    WITH CHECK (
        user_id = COALESCE(NULLIF(current_setting('app.current_user_id', true), ''), NULLIF(current_setting('telefam.current_user_id', true), ''))
    );

DROP POLICY IF EXISTS e2ee_keys_peer_read ON telefam_e2ee_device_keys;
CREATE POLICY e2ee_keys_peer_read ON telefam_e2ee_device_keys
    FOR SELECT
    USING (
        COALESCE(NULLIF(current_setting('app.current_user_id', true), ''), NULLIF(current_setting('telefam.current_user_id', true), '')) IS NOT NULL
        AND NOT EXISTS (
            SELECT 1 FROM telefam_user_blocks b
            WHERE (b.blocker_user_id = COALESCE(NULLIF(current_setting('app.current_user_id', true), ''), NULLIF(current_setting('telefam.current_user_id', true), ''))
                   AND b.blocked_user_id = telefam_e2ee_device_keys.user_id)
               OR (b.blocker_user_id = telefam_e2ee_device_keys.user_id
                   AND b.blocked_user_id = COALESCE(NULLIF(current_setting('app.current_user_id', true), ''), NULLIF(current_setting('telefam.current_user_id', true), '')))
        )
    );

-- One-time prekeys: owner manages own keys; authenticated peer may claim 1 unconsumed OTK
DROP POLICY IF EXISTS e2ee_otk_owner_all ON telefam_e2ee_one_time_prekeys;
CREATE POLICY e2ee_otk_owner_all ON telefam_e2ee_one_time_prekeys
    FOR ALL
    USING (
        user_id = COALESCE(NULLIF(current_setting('app.current_user_id', true), ''), NULLIF(current_setting('telefam.current_user_id', true), ''))
    )
    WITH CHECK (
        user_id = COALESCE(NULLIF(current_setting('app.current_user_id', true), ''), NULLIF(current_setting('telefam.current_user_id', true), ''))
    );

DROP POLICY IF EXISTS e2ee_otk_peer_consume ON telefam_e2ee_one_time_prekeys;
CREATE POLICY e2ee_otk_peer_consume ON telefam_e2ee_one_time_prekeys
    FOR SELECT
    USING (
        COALESCE(NULLIF(current_setting('app.current_user_id', true), ''), NULLIF(current_setting('telefam.current_user_id', true), '')) IS NOT NULL
        AND consumed_by_user_id IS NULL
    );

-- Conversations: strict participant isolation
DROP POLICY IF EXISTS chat_conversations_participant_all ON telefam_chat_conversations;
CREATE POLICY chat_conversations_participant_all ON telefam_chat_conversations
    FOR ALL
    USING (
        COALESCE(NULLIF(current_setting('app.current_user_id', true), ''), NULLIF(current_setting('telefam.current_user_id', true), '')) IN (user_a, user_b)
    )
    WITH CHECK (
        COALESCE(NULLIF(current_setting('app.current_user_id', true), ''), NULLIF(current_setting('telefam.current_user_id', true), '')) IN (user_a, user_b)
    );

-- Voice media: strict uploader/recipient isolation
DROP POLICY IF EXISTS chat_voice_media_participant_all ON telefam_chat_voice_media;
CREATE POLICY chat_voice_media_participant_all ON telefam_chat_voice_media
    FOR ALL
    USING (
        COALESCE(NULLIF(current_setting('app.current_user_id', true), ''), NULLIF(current_setting('telefam.current_user_id', true), '')) IN (uploader_user_id, recipient_user_id)
    )
    WITH CHECK (
        COALESCE(NULLIF(current_setting('app.current_user_id', true), ''), NULLIF(current_setting('telefam.current_user_id', true), '')) = uploader_user_id
    );

-- Messages: strict sender/recipient isolation
DROP POLICY IF EXISTS chat_messages_participant_select ON telefam_chat_messages;
CREATE POLICY chat_messages_participant_select ON telefam_chat_messages
    FOR SELECT
    USING (
        COALESCE(NULLIF(current_setting('app.current_user_id', true), ''), NULLIF(current_setting('telefam.current_user_id', true), '')) IN (sender_user_id, recipient_user_id)
    );

DROP POLICY IF EXISTS chat_messages_sender_insert ON telefam_chat_messages;
CREATE POLICY chat_messages_sender_insert ON telefam_chat_messages
    FOR INSERT
    WITH CHECK (
        COALESCE(NULLIF(current_setting('app.current_user_id', true), ''), NULLIF(current_setting('telefam.current_user_id', true), '')) = sender_user_id
    );

DROP POLICY IF EXISTS chat_messages_participant_update ON telefam_chat_messages;
CREATE POLICY chat_messages_participant_update ON telefam_chat_messages
    FOR UPDATE
    USING (
        COALESCE(NULLIF(current_setting('app.current_user_id', true), ''), NULLIF(current_setting('telefam.current_user_id', true), '')) IN (sender_user_id, recipient_user_id)
    )
    WITH CHECK (
        COALESCE(NULLIF(current_setting('app.current_user_id', true), ''), NULLIF(current_setting('telefam.current_user_id', true), '')) IN (sender_user_id, recipient_user_id)
    );

DROP POLICY IF EXISTS chat_messages_sender_delete ON telefam_chat_messages;
CREATE POLICY chat_messages_sender_delete ON telefam_chat_messages
    FOR DELETE
    USING (
        COALESCE(NULLIF(current_setting('app.current_user_id', true), ''), NULLIF(current_setting('telefam.current_user_id', true), '')) IN (sender_user_id, recipient_user_id)
    );

-- Security Flags: strict actor/target isolation
DROP POLICY IF EXISTS chat_flags_participant_all ON telefam_chat_security_flags;
CREATE POLICY chat_flags_participant_all ON telefam_chat_security_flags
    FOR ALL
    USING (
        COALESCE(NULLIF(current_setting('app.current_user_id', true), ''), NULLIF(current_setting('telefam.current_user_id', true), '')) IN (actor_user_id, target_user_id)
    )
    WITH CHECK (
        COALESCE(NULLIF(current_setting('app.current_user_id', true), ''), NULLIF(current_setting('telefam.current_user_id', true), '')) IN (actor_user_id, target_user_id)
    );
