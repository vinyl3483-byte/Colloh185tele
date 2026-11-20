-- Telefam FCM Push Tokens & Notifications Inbox with Strict PostgreSQL RLS
-- Package: telefam.company

-- 1. Device-bound FCM / APNs / Desktop Push Token Registry
CREATE TABLE IF NOT EXISTS telefam_push_tokens (
    id            TEXT PRIMARY KEY,
    user_id       TEXT NOT NULL,
    device_fp     TEXT NOT NULL,
    platform      TEXT NOT NULL CHECK (platform IN ('android', 'ios', 'desktop')),
    token         TEXT NOT NULL,
    app_bundle    TEXT NOT NULL DEFAULT 'com.telefam.company.app',
    updated_at_ms BIGINT NOT NULL,
    UNIQUE (user_id, device_fp)
);

CREATE INDEX IF NOT EXISTS idx_push_tokens_user
    ON telefam_push_tokens (user_id, updated_at_ms DESC);

CREATE INDEX IF NOT EXISTS idx_push_tokens_device_token
    ON telefam_push_tokens (token);

-- 2. User-Scoped Activity & Social Notifications Inbox
CREATE TABLE IF NOT EXISTS telefam_notifications (
    id                TEXT PRIMARY KEY,
    recipient_user_id TEXT NOT NULL,
    actor_user_id     TEXT NOT NULL,
    actor_name        TEXT NOT NULL DEFAULT '',
    actor_username    TEXT NOT NULL DEFAULT '',
    actor_avatar_url  TEXT NOT NULL DEFAULT '',
    kind              TEXT NOT NULL CHECK (kind IN ('chat_message', 'follow', 'like', 'mention')),
    title             TEXT NOT NULL DEFAULT '',
    body              TEXT NOT NULL DEFAULT '',
    conversation_id   TEXT NOT NULL DEFAULT '',
    message_id        TEXT NOT NULL DEFAULT '',
    post_id           TEXT NOT NULL DEFAULT '',
    target_user_id    TEXT NOT NULL DEFAULT '',
    is_view_once      BOOLEAN NOT NULL DEFAULT FALSE,
    is_read           BOOLEAN NOT NULL DEFAULT FALSE,
    created_at_ms     BIGINT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_notifications_recipient_time
    ON telefam_notifications (recipient_user_id, created_at_ms DESC);

CREATE INDEX IF NOT EXISTS idx_notifications_unread
    ON telefam_notifications (recipient_user_id, is_read)
    WHERE is_read = FALSE;

-- ============================================================================
-- STRICT POSTGRESQL ROW-LEVEL SECURITY (RLS)
-- ============================================================================
ALTER TABLE telefam_push_tokens ENABLE ROW LEVEL SECURITY;
ALTER TABLE telefam_push_tokens FORCE ROW LEVEL SECURITY;

ALTER TABLE telefam_notifications ENABLE ROW LEVEL SECURITY;
ALTER TABLE telefam_notifications FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS push_tokens_owner_all ON telefam_push_tokens;
CREATE POLICY push_tokens_owner_all
    ON telefam_push_tokens
    FOR ALL
    USING (user_id = telefam_current_user_id())
    WITH CHECK (user_id = telefam_current_user_id());

DROP POLICY IF EXISTS notifications_recipient_select ON telefam_notifications;
CREATE POLICY notifications_recipient_select
    ON telefam_notifications
    FOR SELECT
    USING (recipient_user_id = telefam_current_user_id());

DROP POLICY IF EXISTS notifications_recipient_update ON telefam_notifications;
CREATE POLICY notifications_recipient_update
    ON telefam_notifications
    FOR UPDATE
    USING (recipient_user_id = telefam_current_user_id())
    WITH CHECK (recipient_user_id = telefam_current_user_id());

DROP POLICY IF EXISTS notifications_recipient_delete ON telefam_notifications;
CREATE POLICY notifications_recipient_delete
    ON telefam_notifications
    FOR DELETE
    USING (recipient_user_id = telefam_current_user_id());

DROP POLICY IF EXISTS notifications_actor_insert ON telefam_notifications;
CREATE POLICY notifications_actor_insert
    ON telefam_notifications
    FOR INSERT
    WITH CHECK (
        telefam_current_user_id() IS NOT NULL
        AND actor_user_id = telefam_current_user_id()
        AND recipient_user_id <> actor_user_id
    );
