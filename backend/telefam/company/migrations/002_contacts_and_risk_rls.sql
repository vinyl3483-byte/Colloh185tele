-- Telefam Contacts, Follow Graph, Risk Scoring & RLS Policies
-- Package: telefam.company

-- 1. Directed Follow Graph Edges (Follower -> Followee)
CREATE TABLE IF NOT EXISTS telefam_contact_edges (
    follower_id UUID NOT NULL REFERENCES telefam_users(id) ON DELETE CASCADE,
    followee_id UUID NOT NULL REFERENCES telefam_users(id) ON DELETE CASCADE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (follower_id, followee_id),
    CHECK (follower_id <> followee_id)
);

CREATE INDEX IF NOT EXISTS idx_telefam_contact_edges_followee
    ON telefam_contact_edges (followee_id, follower_id);

-- 2. Server-Computed Account Risk Scores (never client-editable)
CREATE TABLE IF NOT EXISTS telefam_user_risk_scores (
    user_id            UUID PRIMARY KEY REFERENCES telefam_users(id) ON DELETE CASCADE,
    risk_score         DOUBLE PRECISION NOT NULL DEFAULT 0.0 CHECK (risk_score >= 0.0 AND risk_score <= 100.0),
    action_burst_count INTEGER NOT NULL DEFAULT 0,
    device_churn_count INTEGER NOT NULL DEFAULT 0,
    spam_flag_count    INTEGER NOT NULL DEFAULT 0,
    last_evaluated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ============================================================================
-- ROW-LEVEL SECURITY (RLS) POLICIES
-- ============================================================================
ALTER TABLE telefam_contact_edges    ENABLE ROW LEVEL SECURITY;
ALTER TABLE telefam_user_risk_scores ENABLE ROW LEVEL SECURITY;

ALTER TABLE telefam_contact_edges    FORCE ROW LEVEL SECURITY;
ALTER TABLE telefam_user_risk_scores FORCE ROW LEVEL SECURITY;

-- Contact edges: authenticated users can inspect edges involving themselves,
-- and can only insert/delete edges where they are the follower_id.
CREATE POLICY contact_edges_select_self ON telefam_contact_edges
    FOR SELECT
    USING (
        follower_id = NULLIF(current_setting('app.current_user_id', true), '')::UUID
        OR followee_id = NULLIF(current_setting('app.current_user_id', true), '')::UUID
    );

CREATE POLICY contact_edges_insert_self ON telefam_contact_edges
    FOR INSERT
    WITH CHECK (
        follower_id = NULLIF(current_setting('app.current_user_id', true), '')::UUID
        AND follower_id <> followee_id
    );

CREATE POLICY contact_edges_delete_self ON telefam_contact_edges
    FOR DELETE
    USING (
        follower_id = NULLIF(current_setting('app.current_user_id', true), '')::UUID
    );

-- Risk scores: completely denied for direct client roles; server-service role only
CREATE POLICY risk_scores_deny_client_all ON telefam_user_risk_scores
    FOR ALL
    USING (false)
    WITH CHECK (false);
