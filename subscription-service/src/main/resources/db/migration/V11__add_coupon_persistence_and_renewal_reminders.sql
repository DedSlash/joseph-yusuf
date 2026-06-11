-- PR-3 — Persistance du coupon appliqué + table des rappels de renouvellement.
--
-- Pourquoi :
-- 1) PayTech n'offre pas de débit silencieux. Chaque renouvellement passe
--    par une nouvelle redirection checkout, donc on doit pouvoir réappliquer
--    automatiquement un coupon "lifetime" (ex: EARLY50) au lieu de demander
--    à l'utilisateur de le retaper.
-- 2) On envoie des rappels J-3, J-1 et un email d'expiration. Sans dédup
--    persistante, un cron qui tourne plusieurs fois par jour spammerait.

-- 1) Coupon persistant sur la subscription
ALTER TABLE subscriptions ADD COLUMN coupon_applied VARCHAR(50);
ALTER TABLE subscriptions ADD COLUMN coupon_lifetime BOOLEAN NOT NULL DEFAULT FALSE;

-- 2) Coupon lifetime aussi tracé sur la transaction (source pour activateAfterPayment)
ALTER TABLE transactions ADD COLUMN coupon_lifetime BOOLEAN NOT NULL DEFAULT FALSE;

-- 3) Dédup des rappels de renouvellement
CREATE TABLE renewal_reminders (
    id              UUID PRIMARY KEY,
    subscription_id UUID NOT NULL,
    user_id         UUID NOT NULL,
    reminder_type   VARCHAR(20) NOT NULL,
    period_end_at   TIMESTAMP NOT NULL,
    sent_at         TIMESTAMP NOT NULL,
    CONSTRAINT renewal_reminders_unique UNIQUE (subscription_id, reminder_type, period_end_at)
);

CREATE INDEX idx_renewal_reminders_user ON renewal_reminders (user_id);
