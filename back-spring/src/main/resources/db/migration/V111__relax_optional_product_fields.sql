-- Categorie et description deviennent facultatives.
--
-- Un vendeur qui publie son premier article n'a pas toujours de taxonomie en
-- tete, ni le temps d'ecrire une description. Les imposer bloquait la creation
-- pour une raison qui ne regarde que lui.
--
-- Cote categorie, la description etait exigee par le serveur alors que le
-- formulaire ne la validait pas : toute creation sans description echouait
-- avec une erreur incomprehensible.

ALTER TABLE products ALTER COLUMN category DROP NOT NULL;
ALTER TABLE products ALTER COLUMN description DROP NOT NULL;

ALTER TABLE categories ALTER COLUMN description DROP NOT NULL;

-- Les chaines vides deja enregistrees deviennent NULL : une valeur absente doit
-- l'etre reellement, sinon les filtres et les affichages doivent gerer deux
-- representations du meme vide.
UPDATE products SET category = NULL WHERE category IS NOT NULL AND trim(category) = '';
UPDATE products SET description = NULL WHERE description IS NOT NULL AND trim(description) = '';
UPDATE categories SET description = NULL WHERE description IS NOT NULL AND trim(description) = '';
