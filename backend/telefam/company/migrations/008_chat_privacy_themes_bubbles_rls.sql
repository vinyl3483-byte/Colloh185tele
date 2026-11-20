-- Telefam Chat Privacy, Telegram-style Adaptive Themes & 6 Message Bubble Colours
-- Tables, Catalog Seeds, and Strict PostgreSQL Row-Level Security (RLS).

-- 1. Chat & Messaging Privacy Settings Table (all 11 controls)
CREATE TABLE IF NOT EXISTS telefam_chat_privacy_settings (
    user_id                 TEXT PRIMARY KEY,
    message_requests        TEXT NOT NULL DEFAULT 'anyone' CHECK (message_requests IN ('anyone', 'friends', 'contacts', 'nobody')),
    who_can_call            TEXT NOT NULL DEFAULT 'anyone' CHECK (who_can_call IN ('anyone', 'friends', 'contacts', 'nobody')),
    who_can_screenshot      TEXT NOT NULL DEFAULT 'nobody' CHECK (who_can_screenshot IN ('anyone', 'friends', 'nobody')),
    who_can_share_chats     TEXT NOT NULL DEFAULT 'nobody' CHECK (who_can_share_chats IN ('anyone', 'friends', 'nobody')),
    who_can_copy_messages   TEXT NOT NULL DEFAULT 'nobody' CHECK (who_can_copy_messages IN ('anyone', 'friends', 'nobody')),
    who_can_download_media  TEXT NOT NULL DEFAULT 'anyone' CHECK (who_can_download_media IN ('anyone', 'friends', 'nobody')),
    chat_theme_id           TEXT NOT NULL DEFAULT 'none',
    bubble_color_id         TEXT NOT NULL DEFAULT 'cyan',
    read_receipts           TEXT NOT NULL DEFAULT 'anyone' CHECK (read_receipts IN ('anyone', 'friends', 'nobody')),
    typing_indicators       TEXT NOT NULL DEFAULT 'nobody' CHECK (typing_indicators IN ('anyone', 'friends', 'nobody')),
    online_indicators       TEXT NOT NULL DEFAULT 'nobody' CHECK (online_indicators IN ('anyone', 'friends', 'nobody')),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 2. Adaptive Chat Themes Catalog (Light + Dark Wallpapers & Gradient Bubbles)
CREATE TABLE IF NOT EXISTS telefam_chat_theme_catalog (
    theme_id          TEXT PRIMARY KEY,
    title             TEXT NOT NULL,
    sort_order        INT NOT NULL,
    pattern_kind      TEXT NOT NULL,
    light_bg_hex      TEXT NOT NULL,
    dark_bg_hex       TEXT NOT NULL,
    accent_start_hex  TEXT NOT NULL,
    accent_end_hex    TEXT NOT NULL,
    selection_hex     TEXT NOT NULL
);

INSERT INTO telefam_chat_theme_catalog (
    theme_id, title, sort_order, pattern_kind, light_bg_hex, dark_bg_hex, accent_start_hex, accent_end_hex, selection_hex
) VALUES
    ('none', 'Default', 0, 'clean', '#F9FAFB', '#0E1014', '#D91E2A', '#E11319', '#D91E2A'),
    ('classic_home', 'Cozy Home', 1, 'home', '#EAF3FC', '#0C1219', '#0EA5E9', '#0284C7', '#38BDF8'),
    ('spring_meadow', 'Spring Meadow', 2, 'meadow', '#EAF5EE', '#0C140E', '#16A34A', '#22C55E', '#7ED957'),
    ('arctic_snow', 'Arctic Frost', 3, 'winter', '#EBF3F9', '#0B131A', '#2B7A9E', '#5BA4CF', '#60A5FA'),
    ('deep_sea', 'Crystal Ocean', 4, 'ocean', '#E9F2F8', '#09121C', '#3B82F6', '#06B6D4', '#22D3EE'),
    ('academia_math', 'Academia', 5, 'math', '#EBF5F2', '#0B1412', '#0D9488', '#2DD4BF', '#2DD4BF'),
    ('botanical_tulip', 'Botanical', 6, 'botanical', '#FAF0F3', '#150C10', '#E14D6A', '#A855F7', '#F472B6'),
    ('neon_romance', 'Velvet Heart', 7, 'romance', '#F5EFFA', '#130C18', '#7C3AED', '#EC4899', '#C084FC'),
    ('festive_fireside', 'Warm Hearth', 8, 'fireside', '#FBF2EB', '#15100B', '#D97706', '#EA580C', '#FB923C'),
    ('retro_arcade', 'Retro Arcade', 9, 'arcade', '#F3EFFB', '#0D0B18', '#A855F7', '#F59E0B', '#F472B6'),
    ('cosmic_nebula', 'Cosmic Orbit', 10, 'cosmic', '#EEF1FA', '#0A0D18', '#4F46E5', '#06B6D4', '#818CF8'),
    ('safari_sunset', 'Savanna Dusk', 11, 'safari', '#FBF1EC', '#170E0B', '#B45309', '#E11D48', '#F97316'),
    ('telefam_crimson', 'Telefam Ruby', 12, 'telefam', '#FDF0F1', '#14090B', '#C80A0F', '#EF233C', '#EF4444')
ON CONFLICT (theme_id) DO NOTHING;

-- 3. Six Curated Message Bubble Colour Palettes (Adaptive Light/Dark)
CREATE TABLE IF NOT EXISTS telefam_chat_bubble_palettes (
    color_id          TEXT PRIMARY KEY,
    label             TEXT NOT NULL,
    sort_order        INT NOT NULL,
    light_start_hex   TEXT NOT NULL,
    light_end_hex     TEXT NOT NULL,
    dark_start_hex    TEXT NOT NULL,
    dark_end_hex      TEXT NOT NULL
);

INSERT INTO telefam_chat_bubble_palettes (
    color_id, label, sort_order, light_start_hex, light_end_hex, dark_start_hex, dark_end_hex
) VALUES
    ('cyan', 'Lagoon Cyan', 1, '#0097A7', '#00BCD4', '#00ACC1', '#26C6DA'),
    ('crimson', 'Telefam Crimson', 2, '#C80A0F', '#E11319', '#D91E2A', '#EF4444'),
    ('emerald', 'Emerald Spring', 3, '#15803D', '#22C55E', '#1B8E4B', '#2DB866'),
    ('azure', 'Ocean Azure', 4, '#0284C7', '#38BDF8', '#1D9BF0', '#0EA5E9'),
    ('violet', 'Royal Violet', 5, '#6D28D9', '#8B5CF6', '#7C3AED', '#A78BFA'),
    ('amber', 'Sunset Amber', 6, '#C2410C', '#F97316', '#D97706', '#F59E0B')
ON CONFLICT (color_id) DO NOTHING;

-- Enable & Force Strict PostgreSQL Row-Level Security (RLS)
ALTER TABLE telefam_chat_privacy_settings ENABLE ROW LEVEL SECURITY;
ALTER TABLE telefam_chat_privacy_settings FORCE ROW LEVEL SECURITY;

ALTER TABLE telefam_chat_theme_catalog ENABLE ROW LEVEL SECURITY;
ALTER TABLE telefam_chat_theme_catalog FORCE ROW LEVEL SECURITY;

ALTER TABLE telefam_chat_bubble_palettes ENABLE ROW LEVEL SECURITY;
ALTER TABLE telefam_chat_bubble_palettes FORCE ROW LEVEL SECURITY;

-- Owner full access on own chat privacy settings
DROP POLICY IF EXISTS chat_privacy_owner_all ON telefam_chat_privacy_settings;
CREATE POLICY chat_privacy_owner_all ON telefam_chat_privacy_settings
    FOR ALL
    USING (
        user_id = COALESCE(NULLIF(current_setting('app.current_user_id', true), ''), NULLIF(current_setting('telefam.current_user_id', true), ''))
    )
    WITH CHECK (
        user_id = COALESCE(NULLIF(current_setting('app.current_user_id', true), ''), NULLIF(current_setting('telefam.current_user_id', true), ''))
    );

-- Non-blocked authenticated peers may read interaction posture (calls, screenshots, receipts)
DROP POLICY IF EXISTS chat_privacy_authenticated_peer_read ON telefam_chat_privacy_settings;
CREATE POLICY chat_privacy_authenticated_peer_read ON telefam_chat_privacy_settings
    FOR SELECT
    USING (
        COALESCE(NULLIF(current_setting('app.current_user_id', true), ''), NULLIF(current_setting('telefam.current_user_id', true), '')) IS NOT NULL
        AND NOT EXISTS (
            SELECT 1 FROM telefam_user_blocks b
            WHERE (b.blocker_user_id = COALESCE(NULLIF(current_setting('app.current_user_id', true), ''), NULLIF(current_setting('telefam.current_user_id', true), ''))
                   AND b.blocked_user_id = telefam_chat_privacy_settings.user_id)
               OR (b.blocker_user_id = telefam_chat_privacy_settings.user_id
                   AND b.blocked_user_id = COALESCE(NULLIF(current_setting('app.current_user_id', true), ''), NULLIF(current_setting('telefam.current_user_id', true), '')))
        )
    );

-- Catalogs: read-only for authenticated users; writes denied via RLS
DROP POLICY IF EXISTS chat_theme_catalog_read ON telefam_chat_theme_catalog;
CREATE POLICY chat_theme_catalog_read ON telefam_chat_theme_catalog
    FOR SELECT
    USING (
        COALESCE(NULLIF(current_setting('app.current_user_id', true), ''), NULLIF(current_setting('telefam.current_user_id', true), '')) IS NOT NULL
    );

DROP POLICY IF EXISTS chat_bubble_palettes_read ON telefam_chat_bubble_palettes;
CREATE POLICY chat_bubble_palettes_read ON telefam_chat_bubble_palettes
    FOR SELECT
    USING (
        COALESCE(NULLIF(current_setting('app.current_user_id', true), ''), NULLIF(current_setting('telefam.current_user_id', true), '')) IS NOT NULL
    );
