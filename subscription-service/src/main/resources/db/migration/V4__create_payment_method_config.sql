CREATE TABLE IF NOT EXISTS payment_method_config (
    provider        VARCHAR(50)  PRIMARY KEY,
    enabled         BOOLEAN      NOT NULL DEFAULT true,
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

INSERT INTO payment_method_config (provider, enabled) VALUES
    ('STRIPE',       true),
    ('WAVE',         false),
    ('ORANGE_MONEY', false)
ON CONFLICT (provider) DO NOTHING;
