-- Telefam Offline Sync Preferences & Device Sync Checkpoints with Strict PostgreSQL RLS.

CREATE TABLE IF NOT EXISTS telefam_sync_preferences (
    user_id                  TEXT PRIMARY KEY,
    auto_sync_enabled        BOOLEAN NOT NULL DEFAULT TRUE,
    sync_over_wifi_only      BOOLEAN NOT NULL DEFAULT FALSE,
    background_sync_enabled  BOOLEAN NOT NULL DEFAULT TRUE,
    cache_feeds_offline      BOOLEAN NOT NULL DEFAULT TRUE,
    cache_chat_media_offline BOOLEAN NOT NULL DEFAULT TRUE,
    last_synced_at_ms        BIGINT NOT NULL DEFAULT 0,
    last_sync_status         TEXT NOT NULL DEFAULT 'idle' CHECK (last_sync_status IN ('idle', 'synced', 'queued', 'error')),
    total_synced_ops         BIGINT NOT NULL DEFAULT 0,
    updated_at               TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS telefam_sync_checkpoints (
    user_id           TEXT NOT NULL,
    device_fp         TEXT NOT NULL,
    synced_ops_count  INT NOT NULL DEFAULT 0,
    last_sync_epoch_ms BIGINT NOT NULL DEFAULT 0,
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_id, device_fp)
);

CREATE INDEX IF NOT EXISTS idx_telefam_sync_checkpoints_user
    ON telefam_sync_checkpoints (user_id, last_sync_epoch_ms DESC);

ALTER TABLE telefam_sync_preferences ENABLE ROW LEVEL SECURITY;
ALTER TABLE telefam_sync_preferences FORCE ROW LEVEL SECURITY;

ALTER TABLE telefam_sync_checkpoints ENABLE ROW LEVEL SECURITY;
ALTER TABLE telefam_sync_checkpoints FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS sync_preferences_owner_all ON telefam_sync_preferences;
CREATE POLICY sync_preferences_owner_all ON telefam_sync_preferences
    FOR ALL
    USING (
        user_id = COALESCE(NULLIF(current_setting('app.current_user_id', true), ''), NULLIF(current_setting('telefam.current_user_id', true), ''))
    )
    WITH CHECK (
        user_id = COALESCE(NULLIF(current_setting('app.current_user_id', true), ''), NULLIF(current_setting('telefam.current_user_id', true), ''))
    );

DROP POLICY IF EXISTS sync_checkpoints_owner_all ON telefam_sync_checkpoints;
CREATE POLICY sync_checkpoints_owner_all ON telefam_sync_checkpoints
    FOR ALL
    USING (
        user_id = COALESCE(NULLIF(current_setting('app.current_user_id', true), ''), NULLIF(current_setting('telefam.current_user_id', true), ''))
    )
    WITH CHECK (
        user_id = COALESCE(NULLIF(current_setting('app.current_user_id', true), ''), NULLIF(current_setting('telefam.current_user_id', true), ''))
    );
