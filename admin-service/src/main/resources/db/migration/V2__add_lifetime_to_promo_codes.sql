-- Flag "lifetime" sur les codes promo : permet la réapplication automatique
-- du code à chaque renouvellement manuel sans bloquer pour cause de
-- "déjà utilisé par cet utilisateur". EARLY50 est l'unique code lifetime
-- prévu pour l'offre Fondateurs (-50% à vie, 100 premiers inscrits).

ALTER TABLE promo_codes ADD COLUMN lifetime BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE promo_codes SET lifetime = TRUE WHERE code = 'EARLY50';
