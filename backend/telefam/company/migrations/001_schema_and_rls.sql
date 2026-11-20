-- Telefam PostgreSQL Schema & Row-Level Security (RLS) Policies
-- Package: telefam.company

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- 1. Core Users Table
CREATE TABLE IF NOT EXISTS telefam_users (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email           TEXT NOT NULL,
    email_hash      TEXT NOT NULL UNIQUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    last_login_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 2. User Profiles Table (with server/DB age >= 13 & length invariants)
CREATE TABLE IF NOT EXISTS telefam_profiles (
    user_id         UUID PRIMARY KEY REFERENCES telefam_users(id) ON DELETE CASCADE,
    full_name       TEXT NOT NULL CHECK (char_length(full_name) BETWEEN 1 AND 64),
    username        TEXT NOT NULL CHECK (username ~ '^[a-z0-9_]{3,24}$'),
    phone_e164      TEXT NOT NULL CHECK (phone_e164 ~ '^\+[1-9][0-9]{5,14}$'),
    country_iso     CHAR(2) NOT NULL CHECK (country_iso ~ '^[A-Z]{2}$'),
    gender          TEXT NOT NULL CHECK (gender IN ('Male', 'Female', 'Non-binary', 'Prefer not to say')),
    date_of_birth   DATE NOT NULL CHECK (date_of_birth <= (CURRENT_DATE - INTERVAL '13 years')),
    location        TEXT NOT NULL CHECK (char_length(location) BETWEEN 1 AND 80),
    bio             TEXT NOT NULL DEFAULT '' CHECK (char_length(bio) <= 150),
    avatar_media_id UUID NULL,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_telefam_profiles_username_lower
    ON telefam_profiles (LOWER(username));

-- 3. Media Uploads Table (renamed strictly by UUID, never user-supplied filename)
CREATE TABLE IF NOT EXISTS telefam_media_uploads (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_user_id       UUID NOT NULL REFERENCES telefam_users(id) ON DELETE CASCADE,
    storage_object_key  TEXT NOT NULL UNIQUE CHECK (storage_object_key ~ '^media/[0-9a-f-]{36}\.jpg$'),
    mime_type           TEXT NOT NULL CHECK (mime_type IN ('image/jpeg', 'image/png', 'image/webp')),
    sha256_fingerprint  CHAR(64) NOT NULL,
    dhash_fingerprint   CHAR(16) NOT NULL,
    width_px            INTEGER NOT NULL CHECK (width_px BETWEEN 64 AND 1024),
    height_px           INTEGER NOT NULL CHECK (height_px BETWEEN 64 AND 1024),
    byte_size           INTEGER NOT NULL CHECK (byte_size > 0 AND byte_size <= 524288),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_telefam_media_fingerprint
    ON telefam_media_uploads (sha256_fingerprint, dhash_fingerprint);

ALTER TABLE telefam_profiles
    ADD CONSTRAINT fk_telefam_profiles_avatar
    FOREIGN KEY (avatar_media_id) REFERENCES telefam_media_uploads(id) ON DELETE SET NULL;

-- 4. Rotating Refresh Token Families (Reuse Detection & Revocation)
CREATE TABLE IF NOT EXISTS telefam_refresh_tokens (
    token_id        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    family_id       UUID NOT NULL,
    user_id         UUID NOT NULL REFERENCES telefam_users(id) ON DELETE CASCADE,
    device_fp       CHAR(64) NOT NULL,
    token_hash      CHAR(64) NOT NULL UNIQUE,
    expires_at      TIMESTAMPTZ NOT NULL,
    revoked_at      TIMESTAMPTZ NULL,
    replaced_by     UUID NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_telefam_refresh_family
    ON telefam_refresh_tokens (family_id, user_id);

-- 5. Immutable User Activity & Security Audit Logs
CREATE TABLE IF NOT EXISTS telefam_audit_logs (
    id              BIGSERIAL PRIMARY KEY,
    user_id         UUID NULL REFERENCES telefam_users(id) ON DELETE SET NULL,
    action          TEXT NOT NULL CHECK (char_length(action) BETWEEN 2 AND 64),
    device_fp       CHAR(64) NOT NULL,
    ip_fp           CHAR(64) NOT NULL,
    metadata_json   JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_telefam_audit_user_time
    ON telefam_audit_logs (user_id, created_at DESC);

-- ============================================================================
-- ROW-LEVEL SECURITY (RLS) POLICIES
-- ============================================================================
ALTER TABLE telefam_users           ENABLE ROW LEVEL SECURITY;
ALTER TABLE telefam_profiles        ENABLE ROW LEVEL SECURITY;
ALTER TABLE telefam_media_uploads   ENABLE ROW LEVEL SECURITY;
ALTER TABLE telefam_refresh_tokens  ENABLE ROW LEVEL SECURITY;
ALTER TABLE telefam_audit_logs      ENABLE ROW LEVEL SECURITY;

-- Force RLS even for table owners in non-superuser roles
ALTER TABLE telefam_users           FORCE ROW LEVEL SECURITY;
ALTER TABLE telefam_profiles        FORCE ROW LEVEL SECURITY;
ALTER TABLE telefam_media_uploads   FORCE ROW LEVEL SECURITY;
ALTER TABLE telefam_refresh_tokens  FORCE ROW LEVEL SECURITY;
ALTER TABLE telefam_audit_logs      FORCE ROW LEVEL SECURITY;

-- Users: self-only read
CREATE POLICY users_self_select ON telefam_users
    FOR SELECT
    USING (id = NULLIF(current_setting('app.current_user_id', true), '')::UUID);

-- Profiles: owner can select/insert/update own profile; authenticated users can discover handles
CREATE POLICY profiles_owner_insert ON telefam_profiles
    FOR INSERT
    WITH CHECK (
        user_id = NULLIF(current_setting('app.current_user_id', true), '')::UUID
        AND date_of_birth <= (CURRENT_DATE - INTERVAL '13 years')
    );

CREATE POLICY profiles_owner_update ON telefam_profiles
    FOR UPDATE
    USING (user_id = NULLIF(current_setting('app.current_user_id', true), '')::UUID)
    WITH CHECK (
        user_id = NULLIF(current_setting('app.current_user_id', true), '')::UUID
        AND date_of_birth <= (CURRENT_DATE - INTERVAL '13 years')
    );

CREATE POLICY profiles_authenticated_select ON telefam_profiles
    FOR SELECT
    USING (NULLIF(current_setting('app.current_user_id', true), '') IS NOT NULL);

-- Media uploads: strictly owner read/insert; never arbitrary delete/overwrite
CREATE POLICY media_owner_select ON telefam_media_uploads
    FOR SELECT
    USING (owner_user_id = NULLIF(current_setting('app.current_user_id', true), '')::UUID);

CREATE POLICY media_owner_insert ON telefam_media_uploads
    FOR INSERT
    WITH CHECK (owner_user_id = NULLIF(current_setting('app.current_user_id', true), '')::UUID);

-- Refresh tokens: completely blocked from client-role access (service role only)
CREATE POLICY refresh_tokens_deny_all ON telefam_refresh_tokens
    FOR ALL
    USING (false)
    WITH CHECK (false);

-- Audit logs: append-only for current user, owner can read own logs, no updates/deletes
CREATE POLICY audit_logs_owner_select ON telefam_audit_logs
    FOR SELECT
    USING (user_id = NULLIF(current_setting('app.current_user_id', true), '')::UUID);

CREATE POLICY audit_logs_append_only ON telefam_audit_logs
    FOR INSERT
    WITH CHECK (
        user_id IS NULL
        OR user_id = NULLIF(current_setting('app.current_user_id', true), '')::UUID
    );
