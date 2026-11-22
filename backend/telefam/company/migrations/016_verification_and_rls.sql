-- Telefam Verified Badges (Business, Government, Blue), Trials, Subscriptions, Anti-Replay Liveness, Document Purge & Strict RLS
-- Package: telefam.company

-- 1. User Verification State & Unlocked Badge Tier
CREATE TABLE IF NOT EXISTS telefam_verifications (
    user_id                TEXT PRIMARY KEY,
    verified_tier          TEXT NOT NULL DEFAULT 'none'
        CHECK (verified_tier IN ('none', 'business', 'government', 'blue')),
    pending_tier           TEXT NOT NULL DEFAULT 'none'
        CHECK (pending_tier IN ('none', 'business', 'government', 'blue')),
    billing_cycle          TEXT NOT NULL DEFAULT 'monthly'
        CHECK (billing_cycle IN ('monthly', 'annual')),
    plan_status            TEXT NOT NULL DEFAULT 'none'
        CHECK (plan_status IN ('none', 'trial_active', 'subscribed', 'cancelled')),
    trial_used             BOOLEAN NOT NULL DEFAULT FALSE,
    trial_ends_at_ms       BIGINT NOT NULL DEFAULT 0,
    subscription_ends_at_ms BIGINT NOT NULL DEFAULT 0,
    liveness_status        TEXT NOT NULL DEFAULT 'not_started'
        CHECK (liveness_status IN ('not_started', 'challenge_issued', 'passed')),
    liveness_score         INT NOT NULL DEFAULT 0 CHECK (liveness_score BETWEEN 0 AND 100),
    liveness_verified_at_ms BIGINT NOT NULL DEFAULT 0,
    document_type          TEXT NOT NULL DEFAULT ''
        CHECK (document_type IN ('', 'national_id', 'passport', 'drivers_license', 'military_id', 'business_license', 'gov_warrant')),
    document_status        TEXT NOT NULL DEFAULT 'not_started'
        CHECK (document_status IN ('not_started', 'captured', 'ocr_matched', 'rejected')),
    doc_number_hmac        TEXT NOT NULL DEFAULT '',
    doc_last4              TEXT NOT NULL DEFAULT '',
    doc_expiry_iso         TEXT NOT NULL DEFAULT '',
    extracted_full_name    TEXT NOT NULL DEFAULT '',
    merged_to_profile      BOOLEAN NOT NULL DEFAULT FALSE,
    review_status          TEXT NOT NULL DEFAULT 'not_started'
        CHECK (review_status IN ('not_started', 'awaiting_plan', 'awaiting_liveness', 'awaiting_document', 'under_review', 'verified', 'rejected')),
    review_submitted_at_ms BIGINT NOT NULL DEFAULT 0,
    review_due_at_ms       BIGINT NOT NULL DEFAULT 0,
    verified_at_ms         BIGINT NOT NULL DEFAULT 0,
    documents_purged       BOOLEAN NOT NULL DEFAULT FALSE,
    documents_purged_at_ms BIGINT NOT NULL DEFAULT 0,
    rejection_reason       TEXT NOT NULL DEFAULT '',
    updated_at_ms          BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_telefam_verifications_status
    ON telefam_verifications (review_status, updated_at_ms DESC);

-- 2. Verification Subscription Payments (Paystack & PayPal with Idempotency Keys)
CREATE TABLE IF NOT EXISTS telefam_verification_payments (
    payment_id             TEXT PRIMARY KEY,
    user_id                TEXT NOT NULL,
    idempotency_key        TEXT NOT NULL,
    tier                   TEXT NOT NULL CHECK (tier IN ('business', 'government', 'blue')),
    billing_cycle          TEXT NOT NULL CHECK (billing_cycle IN ('monthly', 'annual')),
    provider               TEXT NOT NULL CHECK (provider IN ('paystack', 'paypal')),
    provider_reference     TEXT NOT NULL,
    provider_order_id      TEXT NOT NULL DEFAULT '',
    expected_amount_minor  BIGINT NOT NULL CHECK (expected_amount_minor > 0),
    expected_currency      TEXT NOT NULL,
    paid_amount_minor      BIGINT NOT NULL DEFAULT 0 CHECK (paid_amount_minor >= 0),
    paid_currency          TEXT NOT NULL DEFAULT '',
    country_iso            TEXT NOT NULL DEFAULT 'US',
    status                 TEXT NOT NULL CHECK (status IN ('pending', 'completed', 'failed', 'cancelled')),
    checkout_url           TEXT NOT NULL DEFAULT '',
    verified_via           TEXT NOT NULL DEFAULT '',
    created_at_ms          BIGINT NOT NULL,
    completed_at_ms        BIGINT NOT NULL DEFAULT 0,
    UNIQUE (user_id, idempotency_key),
    UNIQUE (provider, provider_reference)
);

CREATE INDEX IF NOT EXISTS idx_telefam_ver_payments_user
    ON telefam_verification_payments (user_id, created_at_ms DESC);

-- 3. Single-Use Signed Liveness & Document Capture Challenges (Anti-Replay & Anti-Spoof)
CREATE TABLE IF NOT EXISTS telefam_verification_challenges (
    challenge_id           TEXT PRIMARY KEY,
    user_id                TEXT NOT NULL,
    device_fp              TEXT NOT NULL,
    kind                   TEXT NOT NULL CHECK (kind IN ('liveness', 'document')),
    document_type          TEXT NOT NULL DEFAULT '',
    nonce_hmac             TEXT NOT NULL,
    expected_sequence      TEXT NOT NULL DEFAULT '',
    expires_at_ms          BIGINT NOT NULL,
    consumed_at_ms         BIGINT NOT NULL DEFAULT 0,
    created_at_ms          BIGINT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_telefam_ver_challenges_user
    ON telefam_verification_challenges (user_id, expires_at_ms DESC);

-- 4. Propagate Verified Tier & Notification Type Check Constraints
ALTER TABLE telefam_notifications
    DROP CONSTRAINT IF EXISTS telefam_notifications_kind_check;
ALTER TABLE telefam_notifications
    ADD CONSTRAINT telefam_notifications_kind_check
    CHECK (kind IN ('chat_message', 'follow', 'like', 'mention', 'incoming_call', 'missed_call', 'star_received', 'verification_update'));

-- ============================================================================
-- STRICT POSTGRESQL ROW-LEVEL SECURITY (RLS)
-- ============================================================================
ALTER TABLE telefam_verifications ENABLE ROW LEVEL SECURITY;
ALTER TABLE telefam_verifications FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS telefam_verifications_owner_select ON telefam_verifications;
CREATE POLICY telefam_verifications_owner_select
    ON telefam_verifications
    FOR SELECT
    USING (user_id = telefam_current_user_id());

DROP POLICY IF EXISTS telefam_verifications_owner_insert ON telefam_verifications;
CREATE POLICY telefam_verifications_owner_insert
    ON telefam_verifications
    FOR INSERT
    WITH CHECK (
        telefam_current_user_id() IS NOT NULL
        AND user_id = telefam_current_user_id()
        AND verified_tier = 'none'
    );

DROP POLICY IF EXISTS telefam_verifications_owner_update ON telefam_verifications;
CREATE POLICY telefam_verifications_owner_update
    ON telefam_verifications
    FOR UPDATE
    USING (user_id = telefam_current_user_id())
    WITH CHECK (user_id = telefam_current_user_id());

ALTER TABLE telefam_verification_payments ENABLE ROW LEVEL SECURITY;
ALTER TABLE telefam_verification_payments FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS telefam_ver_payments_owner_select ON telefam_verification_payments;
CREATE POLICY telefam_ver_payments_owner_select
    ON telefam_verification_payments
    FOR SELECT
    USING (user_id = telefam_current_user_id());

DROP POLICY IF EXISTS telefam_ver_payments_owner_insert ON telefam_verification_payments;
CREATE POLICY telefam_ver_payments_owner_insert
    ON telefam_verification_payments
    FOR INSERT
    WITH CHECK (telefam_current_user_id() IS NOT NULL AND user_id = telefam_current_user_id());

ALTER TABLE telefam_verification_challenges ENABLE ROW LEVEL SECURITY;
ALTER TABLE telefam_verification_challenges FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS telefam_ver_challenges_owner_select ON telefam_verification_challenges;
CREATE POLICY telefam_ver_challenges_owner_select
    ON telefam_verification_challenges
    FOR SELECT
    USING (user_id = telefam_current_user_id());

DROP POLICY IF EXISTS telefam_ver_challenges_owner_insert ON telefam_verification_challenges;
CREATE POLICY telefam_ver_challenges_owner_insert
    ON telefam_verification_challenges
    FOR INSERT
    WITH CHECK (telefam_current_user_id() IS NOT NULL AND user_id = telefam_current_user_id());
