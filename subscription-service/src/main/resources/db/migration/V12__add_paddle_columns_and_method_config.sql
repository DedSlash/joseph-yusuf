-- ════════════════════════════════════════════════════════════════════
-- V12 : Intégration Paddle Billing (carte bancaire internationale)
-- Paddle remplace PayTech pour le moyen "Carte Bancaire".
-- PayTech ne garde que Wave / Orange Money / Free Money.
-- Cf. https://developer.paddle.com/api-reference/transactions/create-transaction
-- ════════════════════════════════════════════════════════════════════

-- ───── Colonnes Paddle sur subscriptions ─────
ALTER TABLE joseph_subscriptions.subscriptions
    ADD COLUMN IF NOT EXISTS paddle_subscription_id VARCHAR(255),
    ADD COLUMN IF NOT EXISTS paddle_customer_id     VARCHAR(255);

CREATE INDEX IF NOT EXISTS idx_subscriptions_paddle_subscription_id
    ON joseph_subscriptions.subscriptions (paddle_subscription_id);

-- ───── Colonne routing : quel backend gère ce moyen ─────
-- Valeurs : 'PAYTECH' (default, existants) | 'PADDLE'
ALTER TABLE payment_method_config
    ADD COLUMN IF NOT EXISTS routing VARCHAR(20) NOT NULL DEFAULT 'PAYTECH';

-- ───── Bascule Carte Bancaire : PayTech → Paddle ─────
-- paytech_method_code mis à NULL : la grille frontend ne route plus
-- "Carte Bancaire" via PayTech mais via Paddle (POST /paddle/create).
UPDATE payment_method_config
   SET routing             = 'PADDLE',
       paytech_method_code = NULL,
       updated_at          = NOW()
 WHERE provider = 'CARTE';

-- ───── Agrégateur PADDLE (caché, display_order > 97) ─────
-- Symétrique aux lignes PAYTECH/PAYDUNYA de V8 — jamais affiché en grille
-- (PaymentMethodConfigService.AGGREGATOR_ORDER_THRESHOLD = 97 ; > 97 = caché).
-- Sert uniquement à tracer provider=PADDLE dans les transactions.
INSERT INTO payment_method_config
    (provider, enabled, display_name, display_order, paytech_method_code, routing, updated_at)
VALUES
    ('PADDLE', false, 'Paddle', 100, NULL, 'PADDLE', NOW())
ON CONFLICT (provider) DO UPDATE
    SET enabled             = EXCLUDED.enabled,
        display_name        = EXCLUDED.display_name,
        display_order       = EXCLUDED.display_order,
        paytech_method_code = EXCLUDED.paytech_method_code,
        routing             = EXCLUDED.routing,
        updated_at          = NOW();
