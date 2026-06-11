-- ════════════════════════════════════════════════════════════════════
-- V8 : Moyens de paiement natifs via PayTech + retrait complet Stripe
-- ════════════════════════════════════════════════════════════════════

-- ───── Table payment_method_config : nouvelle structure ─────
ALTER TABLE payment_method_config
    ADD COLUMN IF NOT EXISTS display_name         VARCHAR(100) NULL,
    ADD COLUMN IF NOT EXISTS display_order        INTEGER     NOT NULL DEFAULT 99,
    ADD COLUMN IF NOT EXISTS paytech_method_code  VARCHAR(50)  NULL;

-- Suppression définitive de Stripe (retrait complet)
DELETE FROM payment_method_config WHERE provider = 'STRIPE';

-- Moyens natifs (routés via PayTech en interne)
UPDATE payment_method_config
   SET enabled = true, display_name = 'Wave', display_order = 1, paytech_method_code = 'wave'
 WHERE provider = 'WAVE';

UPDATE payment_method_config
   SET enabled = true, display_name = 'Orange Money', display_order = 2, paytech_method_code = 'orange_money'
 WHERE provider = 'ORANGE_MONEY';

INSERT INTO payment_method_config (provider, enabled, display_name, display_order, paytech_method_code, updated_at)
VALUES
    ('FREE_MONEY', true, 'Free Money',     3, 'free_money', NOW()),
    ('CARTE',      true, 'Carte bancaire', 4, 'card',       NOW())
ON CONFLICT (provider) DO UPDATE
    SET enabled             = EXCLUDED.enabled,
        display_name        = EXCLUDED.display_name,
        display_order       = EXCLUDED.display_order,
        paytech_method_code = EXCLUDED.paytech_method_code;

-- Agrégateurs internes : conservés mais display_order > 97 → jamais affichés
INSERT INTO payment_method_config (provider, enabled, display_name, display_order, paytech_method_code, updated_at)
VALUES
    ('PAYTECH',  false, 'PayTech',  98, NULL, NOW()),
    ('PAYDUNYA', false, 'PayDunya', 99, NULL, NOW())
ON CONFLICT (provider) DO UPDATE
    SET enabled             = EXCLUDED.enabled,
        display_name        = EXCLUDED.display_name,
        display_order       = EXCLUDED.display_order,
        paytech_method_code = EXCLUDED.paytech_method_code;

-- ───── Drop indexes Stripe avant drop des colonnes ─────
DROP INDEX IF EXISTS joseph_subscriptions.idx_subscriptions_stripe_customer_id;
DROP INDEX IF EXISTS joseph_subscriptions.idx_subscriptions_stripe_subscription_id;
DROP INDEX IF EXISTS joseph_subscriptions.idx_transactions_stripe_invoice_id;

-- ───── Drop colonnes Stripe sur subscriptions ─────
ALTER TABLE joseph_subscriptions.subscriptions
    DROP COLUMN IF EXISTS stripe_customer_id,
    DROP COLUMN IF EXISTS stripe_subscription_id,
    DROP COLUMN IF EXISTS stripe_price_id,
    DROP COLUMN IF EXISTS stripe_coupon_id,
    DROP COLUMN IF EXISTS coupon_duration;

-- ───── Drop colonnes Stripe sur transactions ─────
ALTER TABLE joseph_subscriptions.transactions
    DROP COLUMN IF EXISTS stripe_invoice_id,
    DROP COLUMN IF EXISTS stripe_payment_intent_id;
