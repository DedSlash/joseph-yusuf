ALTER TABLE joseph_subscriptions.transactions
    ADD COLUMN IF NOT EXISTS promo_code      VARCHAR(50),
    ADD COLUMN IF NOT EXISTS discount_percent INTEGER,
    ADD COLUMN IF NOT EXISTS original_amount  NUMERIC(15,2);
