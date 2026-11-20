-- Telefam Settings and Privacy, Notification Settings, Content Preferences, Muted Accounts,
-- Problem Reports / Support Tickets, Session Revocations & Strict PostgreSQL Row-Level Security (RLS).

-- 1. User Privacy & Cookie Settings Table
CREATE TABLE IF NOT EXISTS telefam_user_privacy_settings (
    user_id                       TEXT PRIMARY KEY,
    is_private_account            BOOLEAN NOT NULL DEFAULT FALSE,
    who_can_comment               TEXT NOT NULL DEFAULT 'everyone' CHECK (who_can_comment IN ('everyone', 'friends', 'no_one')),
    who_can_mention               TEXT NOT NULL DEFAULT 'everyone' CHECK (who_can_mention IN ('everyone', 'friends', 'no_one')),
    who_can_find_you              TEXT NOT NULL DEFAULT 'everyone' CHECK (who_can_find_you IN ('everyone', 'contacts', 'no_one')),
    show_activity_status          BOOLEAN NOT NULL DEFAULT TRUE,
    likes_visibility              TEXT NOT NULL DEFAULT 'everyone' CHECK (likes_visibility IN ('everyone', 'friends', 'only_me')),
    following_visibility          TEXT NOT NULL DEFAULT 'everyone' CHECK (following_visibility IN ('everyone', 'friends', 'only_me')),
    data_sharing_personalization  BOOLEAN NOT NULL DEFAULT TRUE,
    analytics_sharing             BOOLEAN NOT NULL DEFAULT TRUE,
    essential_cookies             BOOLEAN NOT NULL DEFAULT TRUE,
    personalization_cookies       BOOLEAN NOT NULL DEFAULT TRUE,
    updated_at                    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 2. Notification Preferences Table
CREATE TABLE IF NOT EXISTS telefam_notification_settings (
    user_id               TEXT PRIMARY KEY,
    master_pause          BOOLEAN NOT NULL DEFAULT FALSE,
    channel_push          BOOLEAN NOT NULL DEFAULT TRUE,
    channel_email         BOOLEAN NOT NULL DEFAULT TRUE,
    channel_sms           BOOLEAN NOT NULL DEFAULT FALSE,
    type_messages         BOOLEAN NOT NULL DEFAULT TRUE,
    type_comments         BOOLEAN NOT NULL DEFAULT TRUE,
    type_followers        BOOLEAN NOT NULL DEFAULT TRUE,
    type_product_updates  BOOLEAN NOT NULL DEFAULT FALSE,
    quiet_hours_enabled   BOOLEAN NOT NULL DEFAULT FALSE,
    quiet_start_minutes   INT NOT NULL DEFAULT 1320 CHECK (quiet_start_minutes BETWEEN 0 AND 1439),
    quiet_end_minutes     INT NOT NULL DEFAULT 420 CHECK (quiet_end_minutes BETWEEN 0 AND 1439),
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 3. Content & Accessibility Preferences Table
CREATE TABLE IF NOT EXISTS telefam_content_preferences (
    user_id                    TEXT PRIMARY KEY,
    suggested_content_enabled  BOOLEAN NOT NULL DEFAULT TRUE,
    not_interested_topics      JSONB NOT NULL DEFAULT '[]'::jsonb,
    language_code              TEXT NOT NULL DEFAULT 'en' CHECK (char_length(language_code) BETWEEN 2 AND 12),
    autoplay_videos            BOOLEAN NOT NULL DEFAULT TRUE,
    video_quality              TEXT NOT NULL DEFAULT 'auto' CHECK (video_quality IN ('auto', 'high', 'data_saver')),
    data_saver                 BOOLEAN NOT NULL DEFAULT FALSE,
    captions_enabled           BOOLEAN NOT NULL DEFAULT FALSE,
    reduced_motion             BOOLEAN NOT NULL DEFAULT FALSE,
    updated_at                 TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 4. Muted Accounts Table
CREATE TABLE IF NOT EXISTS telefam_user_mutes (
    muter_user_id  TEXT NOT NULL,
    muted_user_id  TEXT NOT NULL CHECK (muter_user_id <> muted_user_id),
    created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (muter_user_id, muted_user_id)
);

CREATE INDEX IF NOT EXISTS idx_telefam_user_mutes_muter
    ON telefam_user_mutes (muter_user_id, created_at DESC);

-- 5. Problem Reports & Support Ticket History Table (with Fingerprinted Screenshot Reference)
CREATE TABLE IF NOT EXISTS telefam_problem_reports (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reference_number   TEXT NOT NULL UNIQUE,
    client_op_id       TEXT NOT NULL DEFAULT '',
    user_id            TEXT NOT NULL,
    category           TEXT NOT NULL CHECK (char_length(category) BETWEEN 2 AND 64),
    description        TEXT NOT NULL CHECK (char_length(description) BETWEEN 5 AND 2000),
    screenshot_media_id TEXT NOT NULL DEFAULT '',
    screenshot_sha256  TEXT NOT NULL DEFAULT '',
    status             TEXT NOT NULL DEFAULT 'open' CHECK (status IN ('open', 'in_review', 'resolved')),
    resolution_note    TEXT NOT NULL DEFAULT '',
    created_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_problem_reports_client_op
    ON telefam_problem_reports (user_id, client_op_id)
    WHERE client_op_id <> '';

CREATE INDEX IF NOT EXISTS idx_problem_reports_user_created
    ON telefam_problem_reports (user_id, created_at DESC);

-- 6. User-Wide Session Revocation Watermark (supports Log out of all devices)
CREATE TABLE IF NOT EXISTS telefam_session_revocations (
    user_id             TEXT PRIMARY KEY,
    revoked_all_before  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Enable & Force Strict PostgreSQL Row-Level Security (RLS) on every settings/safety table
ALTER TABLE telefam_user_privacy_settings ENABLE ROW LEVEL SECURITY;
ALTER TABLE telefam_user_privacy_settings FORCE ROW LEVEL SECURITY;

ALTER TABLE telefam_notification_settings ENABLE ROW LEVEL SECURITY;
ALTER TABLE telefam_notification_settings FORCE ROW LEVEL SECURITY;

ALTER TABLE telefam_content_preferences ENABLE ROW LEVEL SECURITY;
ALTER TABLE telefam_content_preferences FORCE ROW LEVEL SECURITY;

ALTER TABLE telefam_user_mutes ENABLE ROW LEVEL SECURITY;
ALTER TABLE telefam_user_mutes FORCE ROW LEVEL SECURITY;

ALTER TABLE telefam_problem_reports ENABLE ROW LEVEL SECURITY;
ALTER TABLE telefam_problem_reports FORCE ROW LEVEL SECURITY;

ALTER TABLE telefam_session_revocations ENABLE ROW LEVEL SECURITY;
ALTER TABLE telefam_session_revocations FORCE ROW LEVEL SECURITY;

-- Privacy settings RLS: owner can manage; non-blocked authenticated callers can read public privacy posture
DROP POLICY IF EXISTS privacy_owner_all ON telefam_user_privacy_settings;
CREATE POLICY privacy_owner_all ON telefam_user_privacy_settings
    FOR ALL
    USING (
        user_id = COALESCE(NULLIF(current_setting('app.current_user_id', true), ''), NULLIF(current_setting('telefam.current_user_id', true), ''))
    )
    WITH CHECK (
        user_id = COALESCE(NULLIF(current_setting('app.current_user_id', true), ''), NULLIF(current_setting('telefam.current_user_id', true), ''))
    );

DROP POLICY IF EXISTS privacy_authenticated_visibility_select ON telefam_user_privacy_settings;
CREATE POLICY privacy_authenticated_visibility_select ON telefam_user_privacy_settings
    FOR SELECT
    USING (
        COALESCE(NULLIF(current_setting('app.current_user_id', true), ''), NULLIF(current_setting('telefam.current_user_id', true), '')) IS NOT NULL
        AND NOT EXISTS (
            SELECT 1 FROM telefam_user_blocks b
            WHERE b.blocker_user_id = COALESCE(NULLIF(current_setting('app.current_user_id', true), ''), NULLIF(current_setting('telefam.current_user_id', true), ''))
              AND b.blocked_user_id = telefam_user_privacy_settings.user_id
        )
    );

-- Notification settings RLS: strictly owner-only
DROP POLICY IF EXISTS notification_settings_owner_all ON telefam_notification_settings;
CREATE POLICY notification_settings_owner_all ON telefam_notification_settings
    FOR ALL
    USING (
        user_id = COALESCE(NULLIF(current_setting('app.current_user_id', true), ''), NULLIF(current_setting('telefam.current_user_id', true), ''))
    )
    WITH CHECK (
        user_id = COALESCE(NULLIF(current_setting('app.current_user_id', true), ''), NULLIF(current_setting('telefam.current_user_id', true), ''))
    );

-- Content preferences RLS: strictly owner-only
DROP POLICY IF EXISTS content_preferences_owner_all ON telefam_content_preferences;
CREATE POLICY content_preferences_owner_all ON telefam_content_preferences
    FOR ALL
    USING (
        user_id = COALESCE(NULLIF(current_setting('app.current_user_id', true), ''), NULLIF(current_setting('telefam.current_user_id', true), ''))
    )
    WITH CHECK (
        user_id = COALESCE(NULLIF(current_setting('app.current_user_id', true), ''), NULLIF(current_setting('telefam.current_user_id', true), ''))
    );

-- Muted accounts RLS: strictly muter-only
DROP POLICY IF EXISTS user_mutes_owner_all ON telefam_user_mutes;
CREATE POLICY user_mutes_owner_all ON telefam_user_mutes
    FOR ALL
    USING (
        muter_user_id = COALESCE(NULLIF(current_setting('app.current_user_id', true), ''), NULLIF(current_setting('telefam.current_user_id', true), ''))
    )
    WITH CHECK (
        muter_user_id = COALESCE(NULLIF(current_setting('app.current_user_id', true), ''), NULLIF(current_setting('telefam.current_user_id', true), ''))
    );

-- Problem reports & tickets RLS: strictly reporter-only SELECT and INSERT
DROP POLICY IF EXISTS problem_reports_owner_select ON telefam_problem_reports;
CREATE POLICY problem_reports_owner_select ON telefam_problem_reports
    FOR SELECT
    USING (
        user_id = COALESCE(NULLIF(current_setting('app.current_user_id', true), ''), NULLIF(current_setting('telefam.current_user_id', true), ''))
    );

DROP POLICY IF EXISTS problem_reports_owner_insert ON telefam_problem_reports;
CREATE POLICY problem_reports_owner_insert ON telefam_problem_reports
    FOR INSERT
    WITH CHECK (
        user_id = COALESCE(NULLIF(current_setting('app.current_user_id', true), ''), NULLIF(current_setting('telefam.current_user_id', true), ''))
    );

-- Session revocations RLS: strictly owner-only
DROP POLICY IF EXISTS session_revocations_owner_all ON telefam_session_revocations;
CREATE POLICY session_revocations_owner_all ON telefam_session_revocations
    FOR ALL
    USING (
        user_id = COALESCE(NULLIF(current_setting('app.current_user_id', true), ''), NULLIF(current_setting('telefam.current_user_id', true), ''))
    )
    WITH CHECK (
        user_id = COALESCE(NULLIF(current_setting('app.current_user_id', true), ''), NULLIF(current_setting('telefam.current_user_id', true), ''))
    );

-- Enforce follower/private-account checks directly inside telefam_posts RLS as well
DROP POLICY IF EXISTS posts_authenticated_read_visible ON telefam_posts;
CREATE POLICY posts_authenticated_read_visible ON telefam_posts
    FOR SELECT
    USING (
        COALESCE(NULLIF(current_setting('app.current_user_id', true), ''), NULLIF(current_setting('telefam.current_user_id', true), '')) IS NOT NULL
        AND is_deleted = FALSE
        AND NOT EXISTS (
            SELECT 1 FROM telefam_user_blocks b
            WHERE b.blocker_user_id = COALESCE(NULLIF(current_setting('app.current_user_id', true), ''), NULLIF(current_setting('telefam.current_user_id', true), ''))
              AND b.blocked_user_id = telefam_posts.author_user_id
        )
        AND NOT EXISTS (
            SELECT 1 FROM telefam_user_mutes m
            WHERE m.muter_user_id = COALESCE(NULLIF(current_setting('app.current_user_id', true), ''), NULLIF(current_setting('telefam.current_user_id', true), ''))
              AND m.muted_user_id = telefam_posts.author_user_id
        )
        AND (
            telefam_posts.author_user_id = COALESCE(NULLIF(current_setting('app.current_user_id', true), ''), NULLIF(current_setting('telefam.current_user_id', true), ''))
            OR (
                (
                    NOT COALESCE((
                        SELECT p.is_private_account
                        FROM telefam_user_privacy_settings p
                        WHERE p.user_id = telefam_posts.author_user_id
                    ), FALSE)
                    OR EXISTS (
                        SELECT 1 FROM telefam_contact_edges ef
                        WHERE ef.follower_id::TEXT = COALESCE(NULLIF(current_setting('app.current_user_id', true), ''), NULLIF(current_setting('telefam.current_user_id', true), ''))
                          AND ef.followee_id::TEXT = telefam_posts.author_user_id
                    )
                )
                AND (
                    telefam_posts.privacy = 'public'
                    OR (
                        telefam_posts.privacy = 'friends'
                        AND EXISTS (
                            SELECT 1 FROM telefam_contact_edges e1
                            WHERE e1.follower_id::TEXT = telefam_posts.author_user_id
                              AND e1.followee_id::TEXT = COALESCE(NULLIF(current_setting('app.current_user_id', true), ''), NULLIF(current_setting('telefam.current_user_id', true), ''))
                        )
                        AND EXISTS (
                            SELECT 1 FROM telefam_contact_edges e2
                            WHERE e2.follower_id::TEXT = COALESCE(NULLIF(current_setting('app.current_user_id', true), ''), NULLIF(current_setting('telefam.current_user_id', true), ''))
                              AND e2.followee_id::TEXT = telefam_posts.author_user_id
                        )
                    )
                )
            )
        )
    );
