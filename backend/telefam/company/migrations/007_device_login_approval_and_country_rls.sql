-- Telefam Cross-Device Login Approval, Device Trust/Block Controls,
-- and Silent Monetization Country Attribution with Strict PostgreSQL RLS.

-- 1. Trusted Devices per User
CREATE TABLE IF NOT EXISTS telefam_trusted_devices (
    user_id          TEXT NOT NULL,
    device_fp        TEXT NOT NULL,
    device_name      TEXT NOT NULL CHECK (char_length(device_name) BETWEEN 1 AND 128),
    platform         TEXT NOT NULL DEFAULT 'android',
    last_ip_masked   TEXT NOT NULL DEFAULT '',
    last_location    TEXT NOT NULL DEFAULT '',
    country_iso      TEXT NOT NULL DEFAULT '',
    last_seen_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    revoked_at       TIMESTAMPTZ,
    PRIMARY KEY (user_id, device_fp)
);

CREATE INDEX IF NOT EXISTS idx_telefam_trusted_devices_user_active
    ON telefam_trusted_devices (user_id, last_seen_at DESC)
    WHERE revoked_at IS NULL;

-- 2. Cross-Device Login Approval Requests (60s window, single-use poll token bound to requesting_device_fp)
CREATE TABLE IF NOT EXISTS telefam_login_approval_requests (
    request_id             TEXT PRIMARY KEY,
    user_id                TEXT NOT NULL,
    account_hash           TEXT NOT NULL,
    requesting_device_fp   TEXT NOT NULL,
    requesting_device_name TEXT NOT NULL,
    requesting_ip_masked   TEXT NOT NULL DEFAULT '',
    requesting_location    TEXT NOT NULL DEFAULT '',
    target_device_fp       TEXT NOT NULL,
    target_device_name     TEXT NOT NULL,
    target_location        TEXT NOT NULL DEFAULT '',
    poll_token_hash        TEXT NOT NULL,
    status                 TEXT NOT NULL DEFAULT 'pending'
                           CHECK (status IN ('pending', 'approved', 'denied', 'expired', 'consumed')),
    expires_at             TIMESTAMPTZ NOT NULL,
    responded_at           TIMESTAMPTZ,
    responded_by_device_fp TEXT NOT NULL DEFAULT '',
    consumed_at            TIMESTAMPTZ,
    created_at             TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_login_approval_distinct_devices CHECK (requesting_device_fp <> target_device_fp)
);

CREATE INDEX IF NOT EXISTS idx_login_approval_user_pending
    ON telefam_login_approval_requests (user_id, expires_at DESC)
    WHERE status = 'pending';

CREATE INDEX IF NOT EXISTS idx_login_approval_requester
    ON telefam_login_approval_requests (account_hash, requesting_device_fp, created_at DESC);

-- 3. Denied Device Blocks
CREATE TABLE IF NOT EXISTS telefam_device_blocks (
    account_hash  TEXT NOT NULL,
    device_fp     TEXT NOT NULL,
    blocked_until TIMESTAMPTZ NOT NULL,
    reason        TEXT NOT NULL DEFAULT 'denied_by_trusted_device',
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (account_hash, device_fp)
);

-- 4. Silent Hardware / Carrier / GPS / IP Country Signals & Per-User CPM Attribution
CREATE TABLE IF NOT EXISTS telefam_device_country_signals (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id                 TEXT NOT NULL,
    device_fp               TEXT NOT NULL,
    hardware_summary        TEXT NOT NULL DEFAULT '',
    carrier_country_iso     TEXT NOT NULL DEFAULT '',
    sim_country_iso         TEXT NOT NULL DEFAULT '',
    gps_country_iso         TEXT NOT NULL DEFAULT '',
    ip_country_iso          TEXT NOT NULL DEFAULT '',
    locale_country_iso      TEXT NOT NULL DEFAULT '',
    timezone                TEXT NOT NULL DEFAULT '',
    effective_country_iso   TEXT NOT NULL DEFAULT '',
    monetization_cpm_country TEXT NOT NULL DEFAULT '',
    gps_lat_bucket          DOUBLE PRECISION,
    gps_lon_bucket          DOUBLE PRECISION,
    signal_source           TEXT NOT NULL DEFAULT 'carrier',
    captured_at_ms          BIGINT NOT NULL DEFAULT 0,
    synced_at               TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_device_country_signals_user_time
    ON telefam_device_country_signals (user_id, synced_at DESC);

CREATE TABLE IF NOT EXISTS telefam_user_country_attribution (
    user_id                  TEXT PRIMARY KEY,
    phone_country_iso        TEXT NOT NULL DEFAULT '',
    verified_country_iso     TEXT NOT NULL DEFAULT '',
    monetization_cpm_country TEXT NOT NULL DEFAULT '',
    attribution_source       TEXT NOT NULL DEFAULT '',
    updated_at               TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Enable & Force Strict PostgreSQL Row-Level Security (RLS)
ALTER TABLE telefam_trusted_devices ENABLE ROW LEVEL SECURITY;
ALTER TABLE telefam_trusted_devices FORCE ROW LEVEL SECURITY;

ALTER TABLE telefam_login_approval_requests ENABLE ROW LEVEL SECURITY;
ALTER TABLE telefam_login_approval_requests FORCE ROW LEVEL SECURITY;

ALTER TABLE telefam_device_blocks ENABLE ROW LEVEL SECURITY;
ALTER TABLE telefam_device_blocks FORCE ROW LEVEL SECURITY;

ALTER TABLE telefam_device_country_signals ENABLE ROW LEVEL SECURITY;
ALTER TABLE telefam_device_country_signals FORCE ROW LEVEL SECURITY;

ALTER TABLE telefam_user_country_attribution ENABLE ROW LEVEL SECURITY;
ALTER TABLE telefam_user_country_attribution FORCE ROW LEVEL SECURITY;

-- Policies for Trusted Devices (user reads/revokes only own devices; backend service manages registration)
DROP POLICY IF EXISTS trusted_devices_owner_select ON telefam_trusted_devices;
CREATE POLICY trusted_devices_owner_select
    ON telefam_trusted_devices
    FOR SELECT
    USING (user_id = current_setting('app.current_user_id', true));

DROP POLICY IF EXISTS trusted_devices_owner_update ON telefam_trusted_devices;
CREATE POLICY trusted_devices_owner_update
    ON telefam_trusted_devices
    FOR UPDATE
    USING (user_id = current_setting('app.current_user_id', true))
    WITH CHECK (user_id = current_setting('app.current_user_id', true));

-- Policies for Login Approval Requests (only owning user on trusted device can view or respond)
DROP POLICY IF EXISTS login_approval_owner_select ON telefam_login_approval_requests;
CREATE POLICY login_approval_owner_select
    ON telefam_login_approval_requests
    FOR SELECT
    USING (user_id = current_setting('app.current_user_id', true));

DROP POLICY IF EXISTS login_approval_owner_respond ON telefam_login_approval_requests;
CREATE POLICY login_approval_owner_respond
    ON telefam_login_approval_requests
    FOR UPDATE
    USING (
        user_id = current_setting('app.current_user_id', true)
        AND requesting_device_fp <> current_setting('app.current_device_fp', true)
    )
    WITH CHECK (
        user_id = current_setting('app.current_user_id', true)
        AND requesting_device_fp <> current_setting('app.current_device_fp', true)
        AND status IN ('approved', 'denied')
    );

-- Device Blocks are strictly server-controlled (no direct client reads/writes when app role is active)
DROP POLICY IF EXISTS device_blocks_server_only ON telefam_device_blocks;
CREATE POLICY device_blocks_server_only
    ON telefam_device_blocks
    FOR ALL
    USING (current_setting('app.is_service_role', true) = 'true')
    WITH CHECK (current_setting('app.is_service_role', true) = 'true');

-- Country Signals & Attribution (owner can append own signals; updates governed by server/service role)
DROP POLICY IF EXISTS country_signals_owner_insert ON telefam_device_country_signals;
CREATE POLICY country_signals_owner_insert
    ON telefam_device_country_signals
    FOR INSERT
    WITH CHECK (user_id = current_setting('app.current_user_id', true));

DROP POLICY IF EXISTS country_signals_service_select ON telefam_device_country_signals;
CREATE POLICY country_signals_service_select
    ON telefam_device_country_signals
    FOR SELECT
    USING (
        user_id = current_setting('app.current_user_id', true)
        OR current_setting('app.is_service_role', true) = 'true'
    );

DROP POLICY IF EXISTS country_attribution_service_all ON telefam_user_country_attribution;
CREATE POLICY country_attribution_service_all
    ON telefam_user_country_attribution
    FOR ALL
    USING (
        user_id = current_setting('app.current_user_id', true)
        OR current_setting('app.is_service_role', true) = 'true'
    )
    WITH CHECK (current_setting('app.is_service_role', true) = 'true');
