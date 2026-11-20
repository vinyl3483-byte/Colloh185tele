-- Telefam Link Previews Cache & Per-User Saved Links with Strict PostgreSQL RLS
-- Package: telefam.company

-- 1. Cached Public URL Link Preview Metadata
CREATE TABLE IF NOT EXISTS telefam_link_previews (
    url_hash       CHAR(64) PRIMARY KEY,
    normalized_url TEXT NOT NULL UNIQUE,
    title          TEXT NOT NULL DEFAULT '',
    description    TEXT NOT NULL DEFAULT '',
    image_url      TEXT NOT NULL DEFAULT '',
    favicon_url    TEXT NOT NULL DEFAULT '',
    domain         TEXT NOT NULL DEFAULT '',
    status         TEXT NOT NULL DEFAULT 'ok' CHECK (status IN ('ok', 'fallback')),
    fetched_at_ms  BIGINT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_link_previews_fetched
    ON telefam_link_previews (fetched_at_ms DESC);

-- 2. User-Scoped Saved Links Bookmarks
CREATE TABLE IF NOT EXISTS telefam_saved_links (
    id             TEXT PRIMARY KEY,
    user_id        TEXT NOT NULL,
    url            TEXT NOT NULL,
    normalized_url TEXT NOT NULL,
    title          TEXT NOT NULL DEFAULT '',
    description    TEXT NOT NULL DEFAULT '',
    image_url      TEXT NOT NULL DEFAULT '',
    favicon_url    TEXT NOT NULL DEFAULT '',
    domain         TEXT NOT NULL DEFAULT '',
    source         TEXT NOT NULL DEFAULT 'browser' CHECK (source IN ('chat', 'feed', 'profile', 'browser', 'search')),
    saved_at_ms    BIGINT NOT NULL,
    UNIQUE (user_id, normalized_url)
);

CREATE INDEX IF NOT EXISTS idx_saved_links_user_time
    ON telefam_saved_links (user_id, saved_at_ms DESC);

-- ============================================================================
-- STRICT POSTGRESQL ROW-LEVEL SECURITY (RLS)
-- ============================================================================
ALTER TABLE telefam_link_previews ENABLE ROW LEVEL SECURITY;
ALTER TABLE telefam_link_previews FORCE ROW LEVEL SECURITY;

ALTER TABLE telefam_saved_links ENABLE ROW LEVEL SECURITY;
ALTER TABLE telefam_saved_links FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS link_previews_authenticated_read ON telefam_link_previews;
CREATE POLICY link_previews_authenticated_read
    ON telefam_link_previews
    FOR SELECT
    USING (telefam_current_user_id() IS NOT NULL);

DROP POLICY IF EXISTS link_previews_authenticated_upsert ON telefam_link_previews;
CREATE POLICY link_previews_authenticated_upsert
    ON telefam_link_previews
    FOR INSERT
    WITH CHECK (telefam_current_user_id() IS NOT NULL);

DROP POLICY IF EXISTS saved_links_owner_all ON telefam_saved_links;
CREATE POLICY saved_links_owner_all
    ON telefam_saved_links
    FOR ALL
    USING (user_id = telefam_current_user_id())
    WITH CHECK (user_id = telefam_current_user_id());
