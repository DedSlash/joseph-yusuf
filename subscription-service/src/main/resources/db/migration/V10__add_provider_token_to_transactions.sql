-- Capture du token PayTech (champ `token` retourné par request-payment) afin
-- de pouvoir appeler GET /payment/get-status?token_payment=... lors de la
-- réconciliation des transactions PENDING dont l'IPN a été perdue.
--
-- NULL accepté : les anciennes transactions n'ont pas de token capturé.
-- Pour celles-là, l'admin doit utiliser force-activate manuellement.

ALTER TABLE transactions ADD COLUMN provider_token VARCHAR(255);
