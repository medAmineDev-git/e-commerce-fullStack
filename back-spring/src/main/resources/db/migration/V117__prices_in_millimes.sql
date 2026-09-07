-- Passage au dinar tunisien : trois decimales au lieu de deux.
--
-- Le dinar se subdivise en 1000 millimes, et les prix s ecrivent couramment
-- avec trois decimales : 12,750 DT vaut 12 dinars et 750 millimes. Avec deux
-- decimales, un vendeur ne pouvait exprimer que les multiples de 10 millimes.
--
-- L elargissement est sans perte : 12.75 devient 12.750, exactement la meme
-- valeur. Aucune donnee existante n est modifiee.

ALTER TABLE products ALTER COLUMN price TYPE NUMERIC(12,3);
ALTER TABLE products ALTER COLUMN compare_at_price TYPE NUMERIC(12,3);

ALTER TABLE orders ALTER COLUMN total TYPE NUMERIC(12,3);
ALTER TABLE order_items ALTER COLUMN unit_price TYPE NUMERIC(12,3);
