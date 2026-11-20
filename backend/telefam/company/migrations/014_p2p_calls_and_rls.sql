-- Telefam P2P Audio & Video Calls with Backend-Computed Durations & Strict RLS
-- Package: telefam.company

-- 1. P2P Calls Table (Durations computed and persisted exclusively on backend)
CREATE TABLE IF NOT EXISTS telefam_calls (
    call_id         TEXT PRIMARY KEY,
    conversation_id TEXT NOT NULL,
    caller_user_id  TEXT NOT NULL,
    callee_user_id  TEXT NOT NULL,
    call_type       TEXT NOT NULL CHECK (call_type IN ('audio', 'video')),
    state           TEXT NOT NULL CHECK (
        state IN (
            'initiated',
            'ringing',
            'connected',
            'ended',
            'missed',
            'declined',
            'busy',
            'cancelled',
            'not_allowed'
        )
    ),
    end_reason      TEXT NOT NULL DEFAULT '',
    video_upgraded  BOOLEAN NOT NULL DEFAULT FALSE,
    log_message_id  TEXT NOT NULL DEFAULT '',
    started_at_ms   BIGINT NOT NULL,
    ringing_at_ms   BIGINT NOT NULL DEFAULT 0,
    answered_at_ms  BIGINT NOT NULL DEFAULT 0,
    ended_at_ms     BIGINT NOT NULL DEFAULT 0,
    duration_sec    BIGINT NOT NULL DEFAULT 0 CHECK (duration_sec >= 0)
);

CREATE INDEX IF NOT EXISTS idx_telefam_calls_conversation_time
    ON telefam_calls (conversation_id, started_at_ms DESC);

CREATE INDEX IF NOT EXISTS idx_telefam_calls_participants
    ON telefam_calls (caller_user_id, callee_user_id, started_at_ms DESC);

-- 2. Extend chat message & notification type constraints for in-chat call logs & incoming call pushes
ALTER TABLE telefam_chat_messages
    DROP CONSTRAINT IF EXISTS telefam_chat_messages_message_type_check;
ALTER TABLE telefam_chat_messages
    ADD CONSTRAINT telefam_chat_messages_message_type_check
    CHECK (message_type IN ('text', 'sticker', 'gif', 'clip', 'voice', 'voice_once', 'call_log', 'call_request'));

ALTER TABLE telefam_notifications
    DROP CONSTRAINT IF EXISTS telefam_notifications_kind_check;
ALTER TABLE telefam_notifications
    ADD CONSTRAINT telefam_notifications_kind_check
    CHECK (kind IN ('chat_message', 'follow', 'like', 'mention', 'incoming_call', 'missed_call'));

-- ============================================================================
-- STRICT POSTGRESQL ROW-LEVEL SECURITY (RLS)
-- ============================================================================
ALTER TABLE telefam_calls ENABLE ROW LEVEL SECURITY;
ALTER TABLE telefam_calls FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS telefam_calls_participant_select ON telefam_calls;
CREATE POLICY telefam_calls_participant_select
    ON telefam_calls
    FOR SELECT
    USING (
        caller_user_id = telefam_current_user_id()
        OR callee_user_id = telefam_current_user_id()
    );

DROP POLICY IF EXISTS telefam_calls_caller_insert ON telefam_calls;
CREATE POLICY telefam_calls_caller_insert
    ON telefam_calls
    FOR INSERT
    WITH CHECK (
        telefam_current_user_id() IS NOT NULL
        AND caller_user_id = telefam_current_user_id()
        AND callee_user_id <> caller_user_id
    );

DROP POLICY IF EXISTS telefam_calls_participant_update ON telefam_calls;
CREATE POLICY telefam_calls_participant_update
    ON telefam_calls
    FOR UPDATE
    USING (
        caller_user_id = telefam_current_user_id()
        OR callee_user_id = telefam_current_user_id()
    )
    WITH CHECK (
        caller_user_id = telefam_current_user_id()
        OR callee_user_id = telefam_current_user_id()
    );
