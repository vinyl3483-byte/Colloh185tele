-- Telefam Posts, Multi-Quality Video Renditions, Media Fingerprints & Hashtag Counts with RLS
-- All user-authored tables enforce PostgreSQL Row-Level Security (RLS).

CREATE TABLE IF NOT EXISTS telefam_post_media (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_user_id       TEXT NOT NULL,
    media_kind          TEXT NOT NULL CHECK (media_kind IN ('photo', 'video', 'thumbnail')),
    storage_object_key  TEXT NOT NULL UNIQUE,
    mime_type           TEXT NOT NULL,
    byte_size           BIGINT NOT NULL CHECK (byte_size > 0 AND byte_size <= 209715200),
    width               INT NOT NULL DEFAULT 0,
    height              INT NOT NULL DEFAULT 0,
    duration_ms         BIGINT NOT NULL DEFAULT 0,
    sha256_fingerprint  TEXT NOT NULL,
    dhash_fingerprint   TEXT NOT NULL DEFAULT '',
    per_frame_hashes    JSONB NOT NULL DEFAULT '[]'::jsonb,
    content_id          TEXT NOT NULL DEFAULT '',
    thumbnail_media_id  UUID NULL REFERENCES telefam_post_media(id) ON DELETE SET NULL,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_telefam_post_media_owner
    ON telefam_post_media (owner_user_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_telefam_post_media_content_id
    ON telefam_post_media (content_id)
    WHERE content_id <> '';

CREATE TABLE IF NOT EXISTS telefam_video_renditions (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    media_id            UUID NOT NULL REFERENCES telefam_post_media(id) ON DELETE CASCADE,
    owner_user_id       TEXT NOT NULL,
    quality_label       TEXT NOT NULL CHECK (quality_label IN ('360p', '480p', '720p', '1080p')),
    width               INT NOT NULL,
    height              INT NOT NULL,
    bitrate_kbps        INT NOT NULL,
    storage_object_key  TEXT NOT NULL UNIQUE,
    sha256_fingerprint  TEXT NOT NULL,
    byte_size           BIGINT NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (media_id, quality_label)
);

CREATE TABLE IF NOT EXISTS telefam_hashtags (
    tag                 TEXT PRIMARY KEY CHECK (tag ~ '^[a-z0-9_]{1,50}$'),
    post_count          BIGINT NOT NULL DEFAULT 0 CHECK (post_count >= 0),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS telefam_posts (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    client_op_id        TEXT NOT NULL UNIQUE,
    author_user_id      TEXT NOT NULL,
    mode                TEXT NOT NULL CHECK (mode IN ('text', 'photos', 'video')),
    text_content        TEXT NOT NULL DEFAULT '',
    caption             TEXT NOT NULL DEFAULT '',
    theme_id            TEXT NOT NULL DEFAULT 'crimson',
    font_id             TEXT NOT NULL DEFAULT 'classic',
    song_json           JSONB NULL,
    tagged_user_ids     JSONB NOT NULL DEFAULT '[]'::jsonb,
    hashtags            JSONB NOT NULL DEFAULT '[]'::jsonb,
    photo_media_ids     JSONB NOT NULL DEFAULT '[]'::jsonb,
    video_media_id      UUID NULL REFERENCES telefam_post_media(id) ON DELETE SET NULL,
    thumbnail_media_id  UUID NULL REFERENCES telefam_post_media(id) ON DELETE SET NULL,
    trim_start_ms       BIGINT NOT NULL DEFAULT 0,
    trim_end_ms         BIGINT NOT NULL DEFAULT 0,
    mute_original_audio BOOLEAN NOT NULL DEFAULT FALSE,
    content_id          TEXT NOT NULL DEFAULT '',
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_telefam_posts_author_created
    ON telefam_posts (author_user_id, created_at DESC);

CREATE TABLE IF NOT EXISTS telefam_post_hashtags (
    post_id             UUID NOT NULL REFERENCES telefam_posts(id) ON DELETE CASCADE,
    tag                 TEXT NOT NULL REFERENCES telefam_hashtags(tag) ON DELETE CASCADE,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (post_id, tag)
);

-- Strictly enable & force RLS on every post & media table
ALTER TABLE telefam_post_media ENABLE ROW LEVEL SECURITY;
ALTER TABLE telefam_post_media FORCE ROW LEVEL SECURITY;

ALTER TABLE telefam_video_renditions ENABLE ROW LEVEL SECURITY;
ALTER TABLE telefam_video_renditions FORCE ROW LEVEL SECURITY;

ALTER TABLE telefam_hashtags ENABLE ROW LEVEL SECURITY;
ALTER TABLE telefam_hashtags FORCE ROW LEVEL SECURITY;

ALTER TABLE telefam_posts ENABLE ROW LEVEL SECURITY;
ALTER TABLE telefam_posts FORCE ROW LEVEL SECURITY;

ALTER TABLE telefam_post_hashtags ENABLE ROW LEVEL SECURITY;
ALTER TABLE telefam_post_hashtags FORCE ROW LEVEL SECURITY;

-- Post media policies: authenticated users can read media referenced by visible posts; owners write/delete
CREATE POLICY post_media_owner_write ON telefam_post_media
    FOR ALL
    USING (owner_user_id = current_setting('telefam.current_user_id', TRUE))
    WITH CHECK (owner_user_id = current_setting('telefam.current_user_id', TRUE));

CREATE POLICY post_media_authenticated_read ON telefam_post_media
    FOR SELECT
    USING (current_setting('telefam.current_user_id', TRUE) <> '');

-- Video rendition policies
CREATE POLICY video_renditions_owner_write ON telefam_video_renditions
    FOR ALL
    USING (owner_user_id = current_setting('telefam.current_user_id', TRUE))
    WITH CHECK (owner_user_id = current_setting('telefam.current_user_id', TRUE));

CREATE POLICY video_renditions_authenticated_read ON telefam_video_renditions
    FOR SELECT
    USING (current_setting('telefam.current_user_id', TRUE) <> '');

-- Hashtags: readable by all authenticated users; mutations via server RPC/service role
CREATE POLICY hashtags_authenticated_read ON telefam_hashtags
    FOR SELECT
    USING (current_setting('telefam.current_user_id', TRUE) <> '');

-- Posts policies: authenticated read, strict owner insert/update/delete
CREATE POLICY posts_authenticated_read ON telefam_posts
    FOR SELECT
    USING (current_setting('telefam.current_user_id', TRUE) <> '');

CREATE POLICY posts_owner_insert ON telefam_posts
    FOR INSERT
    WITH CHECK (author_user_id = current_setting('telefam.current_user_id', TRUE));

CREATE POLICY posts_owner_modify ON telefam_posts
    FOR UPDATE
    USING (author_user_id = current_setting('telefam.current_user_id', TRUE))
    WITH CHECK (author_user_id = current_setting('telefam.current_user_id', TRUE));

CREATE POLICY posts_owner_delete ON telefam_posts
    FOR DELETE
    USING (author_user_id = current_setting('telefam.current_user_id', TRUE));

-- Post hashtags join policies
CREATE POLICY post_hashtags_authenticated_read ON telefam_post_hashtags
    FOR SELECT
    USING (current_setting('telefam.current_user_id', TRUE) <> '');
