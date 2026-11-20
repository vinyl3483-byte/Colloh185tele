-- Telefam Owner Profile Backgrounds (up to 5), 2-Week Username Cooldown, Upload Fingerprinting,
-- Post Privacy ('public' | 'friends' | 'private') & Strict PostgreSQL Row-Level Security (RLS).

-- 1. Expand telefam_profiles for username change cooldown & background photo IDs
ALTER TABLE telefam_profiles
    ADD COLUMN IF NOT EXISTS username_changed_at TIMESTAMPTZ NULL,
    ADD COLUMN IF NOT EXISTS background_media_ids JSONB NOT NULL DEFAULT '[]'::jsonb;

-- 2. Expand telefam_media_uploads to support fingerprinted profile backgrounds alongside avatars
ALTER TABLE telefam_media_uploads
    ADD COLUMN IF NOT EXISTS media_role TEXT NOT NULL DEFAULT 'avatar'
        CHECK (media_role IN ('avatar', 'profile_background'));

ALTER TABLE telefam_media_uploads
    DROP CONSTRAINT IF EXISTS telefam_media_uploads_storage_object_key_check,
    DROP CONSTRAINT IF EXISTS telefam_media_uploads_width_px_check,
    DROP CONSTRAINT IF EXISTS telefam_media_uploads_height_px_check,
    DROP CONSTRAINT IF EXISTS telefam_media_uploads_byte_size_check;

ALTER TABLE telefam_media_uploads
    ADD CONSTRAINT telefam_media_uploads_storage_object_key_check
        CHECK (storage_object_key ~ '^media/(bg/)?[0-9a-f-]{36}\.jpg$'),
    ADD CONSTRAINT telefam_media_uploads_width_px_check
        CHECK (width_px BETWEEN 64 AND 1920),
    ADD CONSTRAINT telefam_media_uploads_height_px_check
        CHECK (height_px BETWEEN 64 AND 1920),
    ADD CONSTRAINT telefam_media_uploads_byte_size_check
        CHECK (byte_size > 0 AND byte_size <= 2097152);

-- Allow authenticated users to resolve avatar and profile background media records
DROP POLICY IF EXISTS media_owner_select ON telefam_media_uploads;
CREATE POLICY media_authenticated_select ON telefam_media_uploads
    FOR SELECT
    USING (
        COALESCE(NULLIF(current_setting('app.current_user_id', true), ''), NULLIF(current_setting('telefam.current_user_id', true), '')) IS NOT NULL
    );

-- 3. Profile Backgrounds Table (max 5 active hero backgrounds per user, full SHA-256 + dHash fingerprinting)
CREATE TABLE IF NOT EXISTS telefam_profile_backgrounds (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID NOT NULL REFERENCES telefam_users(id) ON DELETE CASCADE,
    media_id            UUID NOT NULL REFERENCES telefam_media_uploads(id) ON DELETE CASCADE,
    storage_object_key  TEXT NOT NULL UNIQUE CHECK (storage_object_key ~ '^media/bg/[0-9a-f-]{36}\.jpg$'),
    sha256_fingerprint  CHAR(64) NOT NULL,
    dhash_fingerprint   CHAR(16) NOT NULL,
    sort_order          INT NOT NULL DEFAULT 0 CHECK (sort_order BETWEEN 0 AND 4),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, media_id)
);

CREATE INDEX IF NOT EXISTS idx_telefam_profile_backgrounds_user_order
    ON telefam_profile_backgrounds (user_id, sort_order ASC, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_telefam_profile_backgrounds_fingerprints
    ON telefam_profile_backgrounds (sha256_fingerprint, dhash_fingerprint);

-- Enforce max 5 active hero background photos per profile at database level
CREATE OR REPLACE FUNCTION telefam_enforce_max_profile_backgrounds()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    IF (SELECT COUNT(*) FROM telefam_profile_backgrounds WHERE user_id = NEW.user_id) >= 5 THEN
        RAISE EXCEPTION 'telefam_profile_backgrounds: maximum 5 background photos allowed per profile';
    END IF;
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_enforce_max_5_profile_backgrounds ON telefam_profile_backgrounds;
CREATE TRIGGER trg_enforce_max_5_profile_backgrounds
    BEFORE INSERT ON telefam_profile_backgrounds
    FOR EACH ROW
    EXECUTE FUNCTION telefam_enforce_max_profile_backgrounds();

-- Enforce 2-week (14-day) username change cooldown at database level too
CREATE OR REPLACE FUNCTION telefam_enforce_username_cooldown()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    IF LOWER(NEW.username) IS DISTINCT FROM LOWER(OLD.username) THEN
        IF OLD.username_changed_at IS NOT NULL
           AND OLD.username_changed_at > (NOW() - INTERVAL '14 days') THEN
            RAISE EXCEPTION 'telefam_profiles: username can only be changed once every 2 weeks';
        END IF;
        NEW.username_changed_at := NOW();
    END IF;
    NEW.updated_at := NOW();
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_enforce_username_cooldown ON telefam_profiles;
CREATE TRIGGER trg_enforce_username_cooldown
    BEFORE UPDATE ON telefam_profiles
    FOR EACH ROW
    EXECUTE FUNCTION telefam_enforce_username_cooldown();

-- Strictly enable & force RLS on telefam_profile_backgrounds
ALTER TABLE telefam_profile_backgrounds ENABLE ROW LEVEL SECURITY;
ALTER TABLE telefam_profile_backgrounds FORCE ROW LEVEL SECURITY;

-- Anyone authenticated who isn't blocked can view another user's profile hero backgrounds
CREATE POLICY profile_backgrounds_authenticated_read ON telefam_profile_backgrounds
    FOR SELECT
    USING (
        COALESCE(NULLIF(current_setting('app.current_user_id', true), ''), NULLIF(current_setting('telefam.current_user_id', true), '')) IS NOT NULL
        AND NOT EXISTS (
            SELECT 1 FROM telefam_user_blocks b
            WHERE b.blocker_user_id = COALESCE(NULLIF(current_setting('app.current_user_id', true), ''), NULLIF(current_setting('telefam.current_user_id', true), ''))
              AND b.blocked_user_id = telefam_profile_backgrounds.user_id::TEXT
        )
    );

CREATE POLICY profile_backgrounds_owner_insert ON telefam_profile_backgrounds
    FOR INSERT
    WITH CHECK (
        user_id::TEXT = COALESCE(NULLIF(current_setting('app.current_user_id', true), ''), NULLIF(current_setting('telefam.current_user_id', true), ''))
    );

CREATE POLICY profile_backgrounds_owner_update ON telefam_profile_backgrounds
    FOR UPDATE
    USING (
        user_id::TEXT = COALESCE(NULLIF(current_setting('app.current_user_id', true), ''), NULLIF(current_setting('telefam.current_user_id', true), ''))
    )
    WITH CHECK (
        user_id::TEXT = COALESCE(NULLIF(current_setting('app.current_user_id', true), ''), NULLIF(current_setting('telefam.current_user_id', true), ''))
    );

CREATE POLICY profile_backgrounds_owner_delete ON telefam_profile_backgrounds
    FOR DELETE
    USING (
        user_id::TEXT = COALESCE(NULLIF(current_setting('app.current_user_id', true), ''), NULLIF(current_setting('telefam.current_user_id', true), ''))
    );

-- 4. Post Privacy Column ('public', 'friends', 'private') + Strict RLS Visibility
ALTER TABLE telefam_posts
    ADD COLUMN IF NOT EXISTS privacy TEXT NOT NULL DEFAULT 'public'
        CHECK (privacy IN ('public', 'friends', 'private'));

CREATE INDEX IF NOT EXISTS idx_telefam_posts_author_privacy_created
    ON telefam_posts (author_user_id, privacy, created_at DESC)
    WHERE is_deleted = FALSE;

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
        AND (
            telefam_posts.author_user_id = COALESCE(NULLIF(current_setting('app.current_user_id', true), ''), NULLIF(current_setting('telefam.current_user_id', true), ''))
            OR telefam_posts.privacy = 'public'
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
    );
