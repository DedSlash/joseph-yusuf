CREATE SCHEMA IF NOT EXISTS joseph_subscriptions;

CREATE TABLE joseph_subscriptions.subscriptions (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id      UUID         NOT NULL UNIQUE,
    plan         VARCHAR(20)  NOT NULL,
    status       VARCHAR(20)  NOT NULL,
    provider     VARCHAR(20),
    stripe_subscription_id VARCHAR(255),
    started_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    expires_at   TIMESTAMPTZ,
    cancelled_at TIMESTAMPTZ,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_subscriptions_user ON joseph_subscriptions.subscriptions(user_id);
CREATE INDEX idx_subscriptions_status ON joseph_subscriptions.subscriptions(status);
