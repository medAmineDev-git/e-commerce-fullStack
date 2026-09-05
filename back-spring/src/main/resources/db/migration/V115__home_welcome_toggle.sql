-- Le texte de bienvenue devient facultatif a l'affichage, et le produit mis en
-- avant disparait.
--
-- Le produit mis en avant n'etait plus lu par la vitrine depuis que la tete de
-- page ne porte qu'une banniere. La colonne restait exigee a l'enregistrement :
-- une boutique sans aucun produit ne pouvait donc pas sauvegarder son texte
-- d'accueil, sur une erreur qui parlait d'un produit introuvable.
--
-- L'interrupteur permet de garder un texte redige sans l'afficher, plutot que
-- de devoir le vider pour le masquer puis le ressaisir pour le remettre.

ALTER TABLE home_configurations
    ADD COLUMN IF NOT EXISTS welcome_enabled BOOLEAN NOT NULL DEFAULT TRUE;

ALTER TABLE home_configurations
    DROP COLUMN IF EXISTS featured_product_id;
