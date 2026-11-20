-- Telefam Email Uniqueness & PostgreSQL Row-Level Security (RLS) Hardening
-- Package: telefam.company

-- 1. Enforce strict case-insensitive email uniqueness (One Email = One Account)
CREATE UNIQUE INDEX IF NOT EXISTS idx_telefam_users_email_lower
    ON telefam_users (LOWER(TRIM(email)));

-- 2. Unified zero-trust RLS user resolver supporting both app.current_user_id and telefam.current_user_id
CREATE OR REPLACE FUNCTION telefam_current_user_id()
RETURNS TEXT
LANGUAGE sql
STABLE
AS $$
    SELECT COALESCE(
        NULLIF(current_setting('app.current_user_id', true), ''),
        NULLIF(current_setting('telefam.current_user_id', true), '')
    );
$$;

-- 3. Harden trusted device & login approval RLS policies using unified resolver
DROP POLICY IF EXISTS trusted_devices_owner_select ON telefam_trusted_devices;
CREATE POLICY trusted_devices_owner_select
    ON telefam_trusted_devices
    FOR SELECT
    USING (user_id = telefam_current_user_id());

DROP POLICY IF EXISTS trusted_devices_owner_update ON telefam_trusted_devices;
CREATE POLICY trusted_devices_owner_update
    ON telefam_trusted_devices
    FOR UPDATE
    USING (user_id = telefam_current_user_id())
    WITH CHECK (user_id = telefam_current_user_id());

DROP POLICY IF EXISTS login_approval_owner_select ON telefam_login_approval_requests;
CREATE POLICY login_approval_owner_select
    ON telefam_login_approval_requests
    FOR SELECT
    USING (user_id = telefam_current_user_id());

DROP POLICY IF EXISTS login_approval_owner_update ON telefam_login_approval_requests;
CREATE POLICY login_approval_owner_update
    ON telefam_login_approval_requests
    FOR UPDATE
    USING (user_id = telefam_current_user_id())
    WITH CHECK (user_id = telefam_current_user_id());

-- 4. Enforce non-blocked check on chat message inserts at DB RLS layer
DROP POLICY IF EXISTS chat_messages_sender_insert ON telefam_chat_messages;
CREATE POLICY chat_messages_sender_insert
    ON telefam_chat_messages
    FOR INSERT
    WITH CHECK (
        telefam_current_user_id() = sender_user_id
        AND NOT EXISTS (
            SELECT 1 FROM telefam_user_blocks b
            WHERE (b.blocker_user_id = telefam_chat_messages.sender_user_id
                   AND b.blocked_user_id = telefam_chat_messages.recipient_user_id)
               OR (b.blocker_user_id = telefam_chat_messages.recipient_user_id
                   AND b.blocked_user_id = telefam_chat_messages.sender_user_id)
        )
    );

-- 5. Ensure chat voice media & conversations strictly enforce participant isolation via unified resolver
DROP POLICY IF EXISTS chat_conversations_participant_all ON telefam_chat_conversations;
CREATE POLICY chat_conversations_participant_all
    ON telefam_chat_conversations
    FOR ALL
    USING (telefam_current_user_id() IN (user_a, user_b))
    WITH CHECK (telefam_current_user_id() IN (user_a, user_b));

DROP POLICY IF EXISTS chat_voice_media_participant_all ON telefam_chat_voice_media;
CREATE POLICY chat_voice_media_participant_all
    ON telefam_chat_voice_media
    FOR ALL
    USING (telefam_current_user_id() IN (uploader_user_id, recipient_user_id))
    WITH CHECK (telefam_current_user_id() = uploader_user_id);

-- 6. Prevent deleting or updating immutable user/audit records under RLS
DROP POLICY IF EXISTS users_self_select ON telefam_users;
CREATE POLICY users_self_select
    ON telefam_users
    FOR SELECT
    USING (id::TEXT = telefam_current_user_id());
