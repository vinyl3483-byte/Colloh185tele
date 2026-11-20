-- Telefam Feeds, Dynamic Recommendations, Interactions, Reposts, Reports, Blocks & Strict RLS
-- Zero hardcoded interests; all affinity signals derived from verified user interactions.

ALTER TABLE telefam_posts
    ADD COLUMN IF NOT EXISTS width INT NOT NULL DEFAULT 1080,
    ADD COLUMN IF NOT EXISTS height INT NOT NULL DEFAULT 1920,
    ADD COLUMN IF NOT EXISTS duration_ms BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS likes_count BIGINT NOT NULL DEFAULT 0 CHECK (likes_count >= 0),
    ADD COLUMN IF NOT EXISTS comments_count BIGINT NOT NULL DEFAULT 0 CHECK (comments_count >= 0),
    ADD COLUMN IF NOT EXISTS shares_count BIGINT NOT NULL DEFAULT 0 CHECK (shares_count >= 0),
    ADD COLUMN IF NOT EXISTS favourites_count BIGINT NOT NULL DEFAULT 0 CHECK (favourites_count >= 0),
    ADD COLUMN IF NOT EXISTS reposts_count BIGINT NOT NULL DEFAULT 0 CHECK (reposts_count >= 0),
    ADD COLUMN IF NOT EXISTS is_deleted BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX IF NOT EXISTS idx_telefam_posts_active_created
    ON telefam_posts (created_at DESC)
    WHERE is_deleted = FALSE;

-- Per-user interaction log powering dynamic recommendation affinity
CREATE TABLE IF NOT EXISTS telefam_video_interactions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    client_op_id    TEXT NOT NULL DEFAULT '',
    user_id         TEXT NOT NULL,
    post_id         UUID NOT NULL REFERENCES telefam_posts(id) ON DELETE CASCADE,
    action          TEXT NOT NULL CHECK (action IN (
                        'like', 'unlike', 'favourite', 'unfavourite',
                        'repost', 'unrepost', 'share', 'not_interested',
                        'watch', 'skip'
                    )),
    watch_ms        BIGINT NOT NULL DEFAULT 0 CHECK (watch_ms >= 0),
    duration_ms     BIGINT NOT NULL DEFAULT 0 CHECK (duration_ms >= 0),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_video_interactions_user_post
    ON telefam_video_interactions (user_id, post_id, created_at DESC);

CREATE UNIQUE INDEX IF NOT EXISTS idx_video_interactions_client_op
    ON telefam_video_interactions (user_id, client_op_id)
    WHERE client_op_id <> '';

-- Dynamic learned interest weights per user (hashtags, audio tracks, creators)
CREATE TABLE IF NOT EXISTS telefam_user_affinity (
    user_id         TEXT NOT NULL,
    dimension_type  TEXT NOT NULL CHECK (dimension_type IN ('hashtag', 'audio', 'creator', 'mode')),
    dimension_key   TEXT NOT NULL,
    score           DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_id, dimension_type, dimension_key)
);

CREATE INDEX IF NOT EXISTS idx_user_affinity_lookup
    ON telefam_user_affinity (user_id, score DESC);

-- Active likes (one row per user per post)
CREATE TABLE IF NOT EXISTS telefam_video_likes (
    user_id         TEXT NOT NULL,
    post_id         UUID NOT NULL REFERENCES telefam_posts(id) ON DELETE CASCADE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_id, post_id)
);

-- Active favourites/bookmarks (one row per user per post)
CREATE TABLE IF NOT EXISTS telefam_video_favourites (
    user_id         TEXT NOT NULL,
    post_id         UUID NOT NULL REFERENCES telefam_posts(id) ON DELETE CASCADE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_id, post_id)
);

-- Active reposts (shown back in followers/For You feeds with reposted attribution)
CREATE TABLE IF NOT EXISTS telefam_video_reposts (
    user_id         TEXT NOT NULL,
    post_id         UUID NOT NULL REFERENCES telefam_posts(id) ON DELETE CASCADE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_id, post_id)
);

-- Not interested suppressions (per-user privacy scoped)
CREATE TABLE IF NOT EXISTS telefam_video_not_interested (
    user_id         TEXT NOT NULL,
    post_id         UUID NOT NULL REFERENCES telefam_posts(id) ON DELETE CASCADE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_id, post_id)
);

-- Content moderation reports (strict reporter-only RLS)
CREATE TABLE IF NOT EXISTS telefam_video_reports (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reporter_user_id    TEXT NOT NULL,
    post_id             UUID NOT NULL REFERENCES telefam_posts(id) ON DELETE CASCADE,
    reason              TEXT NOT NULL CHECK (char_length(reason) BETWEEN 2 AND 80),
    details             TEXT NOT NULL DEFAULT '',
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Blocked creators per user (enforced in both recommender queries and RLS)
CREATE TABLE IF NOT EXISTS telefam_user_blocks (
    blocker_user_id     TEXT NOT NULL,
    blocked_user_id     TEXT NOT NULL CHECK (blocker_user_id <> blocked_user_id),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (blocker_user_id, blocked_user_id)
);

-- Enable & force strict RLS on every new table
ALTER TABLE telefam_video_interactions ENABLE ROW LEVEL SECURITY;
ALTER TABLE telefam_video_interactions FORCE ROW LEVEL SECURITY;

ALTER TABLE telefam_user_affinity ENABLE ROW LEVEL SECURITY;
ALTER TABLE telefam_user_affinity FORCE ROW LEVEL SECURITY;

ALTER TABLE telefam_video_likes ENABLE ROW LEVEL SECURITY;
ALTER TABLE telefam_video_likes FORCE ROW LEVEL SECURITY;

ALTER TABLE telefam_video_favourites ENABLE ROW LEVEL SECURITY;
ALTER TABLE telefam_video_favourites FORCE ROW LEVEL SECURITY;

ALTER TABLE telefam_video_reposts ENABLE ROW LEVEL SECURITY;
ALTER TABLE telefam_video_reposts FORCE ROW LEVEL SECURITY;

ALTER TABLE telefam_video_not_interested ENABLE ROW LEVEL SECURITY;
ALTER TABLE telefam_video_not_interested FORCE ROW LEVEL SECURITY;

ALTER TABLE telefam_video_reports ENABLE ROW LEVEL SECURITY;
ALTER TABLE telefam_video_reports FORCE ROW LEVEL SECURITY;

ALTER TABLE telefam_user_blocks ENABLE ROW LEVEL SECURITY;
ALTER TABLE telefam_user_blocks FORCE ROW LEVEL SECURITY;

-- Owner-scoped RLS policies
CREATE POLICY interactions_owner_policy ON telefam_video_interactions
    FOR ALL
    USING (user_id = current_setting('telefam.current_user_id', TRUE))
    WITH CHECK (user_id = current_setting('telefam.current_user_id', TRUE));

CREATE POLICY affinity_owner_policy ON telefam_user_affinity
    FOR ALL
    USING (user_id = current_setting('telefam.current_user_id', TRUE))
    WITH CHECK (user_id = current_setting('telefam.current_user_id', TRUE));

CREATE POLICY likes_authenticated_read ON telefam_video_likes
    FOR SELECT
    USING (current_setting('telefam.current_user_id', TRUE) <> '');

CREATE POLICY likes_owner_write ON telefam_video_likes
    FOR ALL
    USING (user_id = current_setting('telefam.current_user_id', TRUE))
    WITH CHECK (user_id = current_setting('telefam.current_user_id', TRUE));

CREATE POLICY favourites_owner_policy ON telefam_video_favourites
    FOR ALL
    USING (user_id = current_setting('telefam.current_user_id', TRUE))
    WITH CHECK (user_id = current_setting('telefam.current_user_id', TRUE));

CREATE POLICY reposts_authenticated_read ON telefam_video_reposts
    FOR SELECT
    USING (current_setting('telefam.current_user_id', TRUE) <> '');

CREATE POLICY reposts_owner_write ON telefam_video_reposts
    FOR ALL
    USING (user_id = current_setting('telefam.current_user_id', TRUE))
    WITH CHECK (user_id = current_setting('telefam.current_user_id', TRUE));

CREATE POLICY not_interested_owner_policy ON telefam_video_not_interested
    FOR ALL
    USING (user_id = current_setting('telefam.current_user_id', TRUE))
    WITH CHECK (user_id = current_setting('telefam.current_user_id', TRUE));

CREATE POLICY reports_owner_policy ON telefam_video_reports
    FOR ALL
    USING (reporter_user_id = current_setting('telefam.current_user_id', TRUE))
    WITH CHECK (reporter_user_id = current_setting('telefam.current_user_id', TRUE));

CREATE POLICY blocks_owner_policy ON telefam_user_blocks
    FOR ALL
    USING (blocker_user_id = current_setting('telefam.current_user_id', TRUE))
    WITH CHECK (blocker_user_id = current_setting('telefam.current_user_id', TRUE));

-- Harden posts read policy so deleted posts & blocked authors are never exposed by DB queries
DROP POLICY IF EXISTS posts_authenticated_read ON telefam_posts;
CREATE POLICY posts_authenticated_read_visible ON telefam_posts
    FOR SELECT
    USING (
        current_setting('telefam.current_user_id', TRUE) <> ''
        AND is_deleted = FALSE
        AND NOT EXISTS (
            SELECT 1 FROM telefam_user_blocks b
            WHERE b.blocker_user_id = current_setting('telefam.current_user_id', TRUE)
              AND b.blocked_user_id = telefam_posts.author_user_id
        )
    );
