-- Migration vers Stripe Subscriptions (paiements récurrents)
-- Remplace le flow PaymentIntent par Subscription + Invoice + Customer.

ALTER TABLE joseph_subscriptions.subscriptions
    ADD COLUMN IF NOT EXISTS stripe_customer_id      VARCHAR(255),
    ADD COLUMN IF NOT EXISTS stripe_price_id         VARCHAR(255),
    ADD COLUMN IF NOT EXISTS current_period_start    TIMESTAMP,
    ADD COLUMN IF NOT EXISTS current_period_end      TIMESTAMP,
    ADD COLUMN IF NOT EXISTS cancel_at_period_end    BOOLEAN     DEFAULT FALSE NOT NULL,
    ADD COLUMN IF NOT EXISTS stripe_coupon_id        VARCHAR(255),
    ADD COLUMN IF NOT EXISTS coupon_duration         VARCHAR(20),
    ADD COLUMN IF NOT EXISTS currency                VARCHAR(10);
-- coupon_duration : ONCE | FOREVER | MONTHS

ALTER TABLE joseph_subscriptions.transactions
    ADD COLUMN IF NOT EXISTS stripe_invoice_id        VARCHAR(255),
    ADD COLUMN IF NOT EXISTS stripe_payment_intent_id VARCHAR(255);

CREATE INDEX IF NOT EXISTS idx_subscriptions_stripe_customer_id
    ON joseph_subscriptions.subscriptions (stripe_customer_id);

CREATE INDEX IF NOT EXISTS idx_subscriptions_stripe_subscription_id
    ON joseph_subscriptions.subscriptions (stripe_subscription_id);

CREATE INDEX IF NOT EXISTS idx_transactions_stripe_invoice_id
    ON joseph_subscriptions.transactions (stripe_invoice_id);
