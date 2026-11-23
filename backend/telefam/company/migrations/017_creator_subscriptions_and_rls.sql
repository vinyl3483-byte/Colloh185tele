-- Telefam Creator Subscriptions, Revenue Goals ($0.50 - $100+), Subscribers, Paystack & PayPal Checkout, Strict RLS
-- Package: telefam.company

-- 1. Creator Subscription Plans & Monthly Goals
CREATE TABLE IF NOT EXISTS telefam_creator_subscription_plans (
    creator_user_id        TEXT PRIMARY KEY,
    enabled                BOOLEAN NOT NULL DEFAULT TRUE,
    monthly_usd_cents      BIGINT NOT NULL DEFAULT 499
        CHECK (monthly_usd_cents BETWEEN 50 AND 10000),
    annual_discount_pct    INT NOT NULL DEFAULT 20
        CHECK (annual_discount_pct BETWEEN 0 AND 50),
    monthly_goal_usd_cents BIGINT NOT NULL DEFAULT 10000
        CHECK (monthly_goal_usd_cents BETWEEN 50 AND 1000000),
    perks_summary          TEXT NOT NULL DEFAULT '',
    updated_at_ms          BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_telefam_creator_plans_enabled
    ON telefam_creator_subscription_plans (enabled, updated_at_ms DESC);

-- 2. Fan-to-Creator Subscriptions (Active / Cancelled / Expired)
CREATE TABLE IF NOT EXISTS telefam_creator_subscriptions (
    subscription_id        TEXT PRIMARY KEY,
    subscriber_user_id     TEXT NOT NULL,
    creator_user_id        TEXT NOT NULL,
    billing_cycle          TEXT NOT NULL CHECK (billing_cycle IN ('monthly', 'annual')),
    status                 TEXT NOT NULL CHECK (status IN ('active', 'cancelled', 'expired')),
    auto_renew             BOOLEAN NOT NULL DEFAULT TRUE,
    amount_usd_cents       BIGINT NOT NULL CHECK (amount_usd_cents BETWEEN 50 AND 120000),
    display_amount_minor   BIGINT NOT NULL CHECK (display_amount_minor > 0),
    display_currency       TEXT NOT NULL,
    provider               TEXT NOT NULL CHECK (provider IN ('paystack', 'paypal')),
    started_at_ms          BIGINT NOT NULL,
    current_period_end_ms  BIGINT NOT NULL,
    cancelled_at_ms        BIGINT NOT NULL DEFAULT 0,
    updated_at_ms          BIGINT NOT NULL,
    UNIQUE (subscriber_user_id, creator_user_id),
    CHECK (subscriber_user_id <> creator_user_id)
);

CREATE INDEX IF NOT EXISTS idx_telefam_creator_subs_creator
    ON telefam_creator_subscriptions (creator_user_id, status, updated_at_ms DESC);

CREATE INDEX IF NOT EXISTS idx_telefam_creator_subs_subscriber
    ON telefam_creator_subscriptions (subscriber_user_id, status, updated_at_ms DESC);

-- 3. Subscription Checkout & Renewal Payments (Paystack & PayPal with Idempotency Keys)
CREATE TABLE IF NOT EXISTS telefam_subscription_payments (
    payment_id             TEXT PRIMARY KEY,
    subscriber_user_id     TEXT NOT NULL,
    creator_user_id        TEXT NOT NULL,
    idempotency_key        TEXT NOT NULL,
    billing_cycle          TEXT NOT NULL CHECK (billing_cycle IN ('monthly', 'annual')),
    provider               TEXT NOT NULL CHECK (provider IN ('paystack', 'paypal')),
    provider_reference     TEXT NOT NULL,
    provider_order_id      TEXT NOT NULL DEFAULT '',
    expected_amount_minor  BIGINT NOT NULL CHECK (expected_amount_minor > 0),
    expected_currency      TEXT NOT NULL,
    amount_usd_cents       BIGINT NOT NULL CHECK (amount_usd_cents BETWEEN 50 AND 120000),
    paid_amount_minor      BIGINT NOT NULL DEFAULT 0 CHECK (paid_amount_minor >= 0),
    paid_currency          TEXT NOT NULL DEFAULT '',
    country_iso            TEXT NOT NULL DEFAULT 'US',
    status                 TEXT NOT NULL CHECK (status IN ('pending', 'completed', 'failed', 'cancelled')),
    checkout_url           TEXT NOT NULL DEFAULT '',
    verified_via           TEXT NOT NULL DEFAULT '',
    created_at_ms          BIGINT NOT NULL,
    completed_at_ms        BIGINT NOT NULL DEFAULT 0,
    UNIQUE (subscriber_user_id, idempotency_key),
    UNIQUE (provider, provider_reference),
    CHECK (subscriber_user_id <> creator_user_id)
);

CREATE INDEX IF NOT EXISTS idx_telefam_sub_payments_user
    ON telefam_subscription_payments (subscriber_user_id, created_at_ms DESC);

-- 4. Extend Notifications Kind Check Constraint
ALTER TABLE telefam_notifications
    DROP CONSTRAINT IF EXISTS telefam_notifications_kind_check;
ALTER TABLE telefam_notifications
    ADD CONSTRAINT telefam_notifications_kind_check
    CHECK (kind IN (
        'chat_message',
        'follow',
        'like',
        'mention',
        'incoming_call',
        'missed_call',
        'star_received',
        'verification_update',
        'new_subscriber',
        'subscription_update'
    ));

-- ============================================================================
-- STRICT POSTGRESQL ROW-LEVEL SECURITY (RLS)
-- ============================================================================
ALTER TABLE telefam_creator_subscription_plans ENABLE ROW LEVEL SECURITY;
ALTER TABLE telefam_creator_subscription_plans FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS telefam_creator_plans_auth_select ON telefam_creator_subscription_plans;
CREATE POLICY telefam_creator_plans_auth_select
    ON telefam_creator_subscription_plans
    FOR SELECT
    USING (telefam_current_user_id() IS NOT NULL);

DROP POLICY IF EXISTS telefam_creator_plans_owner_insert ON telefam_creator_subscription_plans;
CREATE POLICY telefam_creator_plans_owner_insert
    ON telefam_creator_subscription_plans
    FOR INSERT
    WITH CHECK (
        telefam_current_user_id() IS NOT NULL
        AND creator_user_id = telefam_current_user_id()
    );

DROP POLICY IF EXISTS telefam_creator_plans_owner_update ON telefam_creator_subscription_plans;
CREATE POLICY telefam_creator_plans_owner_update
    ON telefam_creator_subscription_plans
    FOR UPDATE
    USING (creator_user_id = telefam_current_user_id())
    WITH CHECK (creator_user_id = telefam_current_user_id());

ALTER TABLE telefam_creator_subscriptions ENABLE ROW LEVEL SECURITY;
ALTER TABLE telefam_creator_subscriptions FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS telefam_creator_subs_party_select ON telefam_creator_subscriptions;
CREATE POLICY telefam_creator_subs_party_select
    ON telefam_creator_subscriptions
    FOR SELECT
    USING (
        subscriber_user_id = telefam_current_user_id()
        OR creator_user_id = telefam_current_user_id()
    );

DROP POLICY IF EXISTS telefam_creator_subs_subscriber_update ON telefam_creator_subscriptions;
CREATE POLICY telefam_creator_subs_subscriber_update
    ON telefam_creator_subscriptions
    FOR UPDATE
    USING (subscriber_user_id = telefam_current_user_id())
    WITH CHECK (subscriber_user_id = telefam_current_user_id());

ALTER TABLE telefam_subscription_payments ENABLE ROW LEVEL SECURITY;
ALTER TABLE telefam_subscription_payments FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS telefam_sub_payments_owner_select ON telefam_subscription_payments;
CREATE POLICY telefam_sub_payments_owner_select
    ON telefam_subscription_payments
    FOR SELECT
    USING (subscriber_user_id = telefam_current_user_id());

DROP POLICY IF EXISTS telefam_sub_payments_owner_insert ON telefam_subscription_payments;
CREATE POLICY telefam_sub_payments_owner_insert
    ON telefam_subscription_payments
    FOR INSERT
    WITH CHECK (
        telefam_current_user_id() IS NOT NULL
        AND subscriber_user_id = telefam_current_user_id()
        AND status = 'pending'
    );
