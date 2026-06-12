-- Champ paddle_discount_id : id du discount Paddle correspondant à ce code
-- promo Joseph. Permet à PaddleService de pré-appliquer la réduction dans
-- l'overlay Paddle Checkout (sinon l'utilisateur doit ressaisir le code et
-- celui-ci n'est pas reconnu si pas créé côté Paddle Dashboard).
--
-- Avant cette migration, seul EARLY50 était mappé, en dur dans
-- PaddleService + via PADDLE_EARLY50_DISCOUNT_ID. Ici on déplace la mapping
-- en DB pour que chaque admin puisse créer son propre code Paddle-aware.

ALTER TABLE promo_codes ADD COLUMN paddle_discount_id VARCHAR(64);

-- Backfill EARLY50 (si la ligne existe — créée manuellement en prod via
-- admin UI ou bootstrap fondateurs). Sur dev clean, UPDATE 0 rows, no-op.
UPDATE promo_codes
SET paddle_discount_id = 'dsc_01ksq16caf60a7664a1vah71h2'
WHERE code = 'EARLY50';
