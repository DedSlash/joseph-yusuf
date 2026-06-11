-- Alignement des paytech_method_code avec la doc officielle PayTech
-- (https://doc.intech.sn/doc_paytech.php — collection Postman §TARGET PAYMENT LIST)
--
-- Les valeurs valides du champ target_payment sont littéralement :
--   "Wave", "Orange Money", "Free Money", "Carte Bancaire"
-- (et non wave/orange_money/free_money/card comme initialement codé en V8).
--
-- Sans cette correction, PayTech ignore silencieusement target_payment et
-- l'utilisateur arrive sur la grille de choix de méthodes complète au lieu
-- d'être pré-orienté vers Wave / Orange Money / Free Money / Carte.

UPDATE payment_method_config SET paytech_method_code = 'Wave'           WHERE provider = 'WAVE';
UPDATE payment_method_config SET paytech_method_code = 'Orange Money'   WHERE provider = 'ORANGE_MONEY';
UPDATE payment_method_config SET paytech_method_code = 'Free Money'     WHERE provider = 'FREE_MONEY';
UPDATE payment_method_config SET paytech_method_code = 'Carte Bancaire' WHERE provider = 'CARTE';
