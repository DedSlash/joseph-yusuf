-- months_count : nombre de mois d'abonnement payés sur cette transaction.
-- Permet aux paiements mobile money (PayTech) de souscrire plusieurs mois
-- d'avance (1-12). À l'activation post-webhook, expiresAt = now + 30 × N
-- jours. Les paiements carte (Paddle) restent à monthsCount=1 puisque
-- Paddle gère le renouvellement natif mensuel.

ALTER TABLE transactions
    ADD COLUMN months_count INT NOT NULL DEFAULT 1;
