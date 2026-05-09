CREATE TABLE joseph_subscriptions.transactions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    subscription_id UUID         REFERENCES joseph_subscriptions.subscriptions(id) ON DELETE SET NULL,
    user_id         UUID         NOT NULL,
    plan            VARCHAR(20)  NOT NULL,
    provider        VARCHAR(20)  NOT NULL,
    transaction_id  VARCHAR(255) UNIQUE,
    amount          NUMERIC(15,2) NOT NULL,
    currency        VARCHAR(10)  NOT NULL,
    status          VARCHAR(20)  NOT NULL,
    failure_reason  VARCHAR(500),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_transactions_user ON joseph_subscriptions.transactions(user_id);
CREATE INDEX idx_transactions_subscription ON joseph_subscriptions.transactions(subscription_id);
CREATE INDEX idx_transactions_external ON joseph_subscriptions.transactions(transaction_id);
