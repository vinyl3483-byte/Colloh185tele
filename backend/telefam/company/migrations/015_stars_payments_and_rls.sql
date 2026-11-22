-- Telefam Stars Economy, Verified Paystack/PayPal Payments, AML Velocity Controls & Strict RLS
-- Package: telefam.company

-- 1. User Star Wallets
CREATE TABLE IF NOT EXISTS telefam_star_wallets (
    user_id              TEXT PRIMARY KEY,
    balance_stars        BIGINT NOT NULL DEFAULT 0 CHECK (balance_stars >= 0),
    stars_received_total BIGINT NOT NULL DEFAULT 0 CHECK (stars_received_total >= 0),
    stars_sent_total     BIGINT NOT NULL DEFAULT 0 CHECK (stars_sent_total >= 0),
    updated_at_ms        BIGINT NOT NULL DEFAULT 0
);

-- 2. Verified Star Top-Up Payments (Paystack & PayPal with Idempotency Keys)
CREATE TABLE IF NOT EXISTS telefam_star_payments (
    payment_id             TEXT PRIMARY KEY,
    user_id                TEXT NOT NULL,
    idempotency_key        TEXT NOT NULL,
    provider               TEXT NOT NULL CHECK (provider IN ('paystack', 'paypal')),
    provider_reference     TEXT NOT NULL,
    provider_order_id      TEXT NOT NULL DEFAULT '',
    package_stars          BIGINT NOT NULL CHECK (package_stars > 0),
    expected_amount_minor  BIGINT NOT NULL CHECK (expected_amount_minor > 0),
    expected_currency      TEXT NOT NULL,
    paid_amount_minor      BIGINT NOT NULL DEFAULT 0 CHECK (paid_amount_minor >= 0),
    paid_currency          TEXT NOT NULL DEFAULT '',
    country_iso            TEXT NOT NULL DEFAULT 'US',
    status                 TEXT NOT NULL CHECK (
        status IN ('pending', 'completed', 'failed', 'cancelled', 'fraud_blocked')
    ),
    checkout_url           TEXT NOT NULL DEFAULT '',
    purpose                TEXT NOT NULL DEFAULT 'topup',
    peer_user_id           TEXT NOT NULL DEFAULT '',
    conversation_id        TEXT NOT NULL DEFAULT '',
    verified_via           TEXT NOT NULL DEFAULT '',
    created_at_ms          BIGINT NOT NULL,
    completed_at_ms        BIGINT NOT NULL DEFAULT 0,
    UNIQUE (user_id, idempotency_key),
    UNIQUE (provider, provider_reference)
);

CREATE INDEX IF NOT EXISTS idx_telefam_star_payments_user_time
    ON telefam_star_payments (user_id, created_at_ms DESC);

-- 3. Immutable Double-Entry Star Ledger
CREATE TABLE IF NOT EXISTS telefam_star_ledger (
    entry_id             TEXT PRIMARY KEY,
    user_id              TEXT NOT NULL,
    counterparty_user_id TEXT NOT NULL DEFAULT '',
    entry_type           TEXT NOT NULL CHECK (entry_type IN ('purchase', 'sent', 'received')),
    delta_stars          BIGINT NOT NULL,
    balance_after        BIGINT NOT NULL CHECK (balance_after >= 0),
    payment_id           TEXT NOT NULL DEFAULT '',
    message_id           TEXT NOT NULL DEFAULT '',
    conversation_id      TEXT NOT NULL DEFAULT '',
    idempotency_key      TEXT NOT NULL DEFAULT '',
    amount_minor         BIGINT NOT NULL DEFAULT 0,
    currency             TEXT NOT NULL DEFAULT '',
    created_at_ms        BIGINT NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_telefam_star_ledger_idempotency
    ON telefam_star_ledger (user_id, entry_type, idempotency_key)
    WHERE idempotency_key <> '';

CREATE INDEX IF NOT EXISTS idx_telefam_star_ledger_user_time
    ON telefam_star_ledger (user_id, created_at_ms DESC);

-- 4. Anti-Money Laundering (AML) & Velocity Event Audit Log
CREATE TABLE IF NOT EXISTS telefam_star_aml_events (
    event_id        TEXT PRIMARY KEY,
    user_id         TEXT NOT NULL,
    peer_user_id    TEXT NOT NULL DEFAULT '',
    device_fp       TEXT NOT NULL DEFAULT '',
    ip_fp           TEXT NOT NULL DEFAULT '',
    rule_triggered  TEXT NOT NULL,
    stars_amount    BIGINT NOT NULL DEFAULT 0,
    details         TEXT NOT NULL DEFAULT '',
    created_at_ms   BIGINT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_telefam_star_aml_user_time
    ON telefam_star_aml_events (user_id, created_at_ms DESC);

-- 5. Extend Chat Message & Notification Type Check Constraints for Star Gifting
ALTER TABLE telefam_chat_messages
    DROP CONSTRAINT IF EXISTS telefam_chat_messages_message_type_check;
ALTER TABLE telefam_chat_messages
    ADD CONSTRAINT telefam_chat_messages_message_type_check
    CHECK (message_type IN ('text', 'sticker', 'gif', 'clip', 'voice', 'voice_once', 'call_log', 'call_request', 'star_gift'));

ALTER TABLE telefam_notifications
    DROP CONSTRAINT IF EXISTS telefam_notifications_kind_check;
ALTER TABLE telefam_notifications
    ADD CONSTRAINT telefam_notifications_kind_check
    CHECK (kind IN ('chat_message', 'follow', 'like', 'mention', 'incoming_call', 'missed_call', 'star_received'));

-- ============================================================================
-- STRICT POSTGRESQL ROW-LEVEL SECURITY (RLS)
-- ============================================================================
ALTER TABLE telefam_star_wallets ENABLE ROW LEVEL SECURITY;
ALTER TABLE telefam_star_wallets FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS telefam_star_wallets_owner_select ON telefam_star_wallets;
CREATE POLICY telefam_star_wallets_owner_select
    ON telefam_star_wallets
    FOR SELECT
    USING (user_id = telefam_current_user_id());

DROP POLICY IF EXISTS telefam_star_wallets_owner_insert ON telefam_star_wallets;
CREATE POLICY telefam_star_wallets_owner_insert
    ON telefam_star_wallets
    FOR INSERT
    WITH CHECK (telefam_current_user_id() IS NOT NULL AND user_id = telefam_current_user_id());

DROP POLICY IF EXISTS telefam_star_wallets_owner_update ON telefam_star_wallets;
CREATE POLICY telefam_star_wallets_owner_update
    ON telefam_star_wallets
    FOR UPDATE
    USING (user_id = telefam_current_user_id())
    WITH CHECK (user_id = telefam_current_user_id());

ALTER TABLE telefam_star_payments ENABLE ROW LEVEL SECURITY;
ALTER TABLE telefam_star_payments FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS telefam_star_payments_owner_select ON telefam_star_payments;
CREATE POLICY telefam_star_payments_owner_select
    ON telefam_star_payments
    FOR SELECT
    USING (user_id = telefam_current_user_id());

DROP POLICY IF EXISTS telefam_star_payments_owner_insert ON telefam_star_payments;
CREATE POLICY telefam_star_payments_owner_insert
    ON telefam_star_payments
    FOR INSERT
    WITH CHECK (telefam_current_user_id() IS NOT NULL AND user_id = telefam_current_user_id());

DROP POLICY IF EXISTS telefam_star_payments_owner_update ON telefam_star_payments;
CREATE POLICY telefam_star_payments_owner_update
    ON telefam_star_payments
    FOR UPDATE
    USING (user_id = telefam_current_user_id())
    WITH CHECK (user_id = telefam_current_user_id());

ALTER TABLE telefam_star_ledger ENABLE ROW LEVEL SECURITY;
ALTER TABLE telefam_star_ledger FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS telefam_star_ledger_owner_select ON telefam_star_ledger;
CREATE POLICY telefam_star_ledger_owner_select
    ON telefam_star_ledger
    FOR SELECT
    USING (user_id = telefam_current_user_id());

DROP POLICY IF EXISTS telefam_star_ledger_owner_insert ON telefam_star_ledger;
CREATE POLICY telefam_star_ledger_owner_insert
    ON telefam_star_ledger
    FOR INSERT
    WITH CHECK (telefam_current_user_id() IS NOT NULL AND user_id = telefam_current_user_id());

ALTER TABLE telefam_star_aml_events ENABLE ROW LEVEL SECURITY;
ALTER TABLE telefam_star_aml_events FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS telefam_star_aml_events_owner_select ON telefam_star_aml_events;
CREATE POLICY telefam_star_aml_events_owner_select
    ON telefam_star_aml_events
    FOR SELECT
    USING (user_id = telefam_current_user_id());
