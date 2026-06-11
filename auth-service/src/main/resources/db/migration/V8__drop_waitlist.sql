-- Waitlist jamais alimentée (page d'inscription cachée jusqu'à l'ouverture des paiements)
-- Retrait complet de la table et de ses indexes.

DROP INDEX IF EXISTS joseph_auth.idx_waitlist_email_plan;
DROP INDEX IF EXISTS joseph_auth.idx_waitlist_notified;
DROP INDEX IF EXISTS joseph_auth.idx_waitlist_plan_tier;

DROP TABLE IF EXISTS joseph_auth.waitlist;
