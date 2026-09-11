-- Taille et couleur choisies par le client, sur chaque ligne de commande.
--
-- Le panier ne transmettait que le produit et la quantite : le vendeur
-- recevait « Body x 1 » sans savoir s il s agissait du 3 mois ou du 12 mois.
--
-- Colonnes facultatives : un article sans declinaison n en porte pas, et les
-- commandes existantes n ont jamais enregistre ce choix. Les longueurs sont
-- celles de product_sizes.size_value et product_colors.color_name.

ALTER TABLE order_items ADD COLUMN size_value VARCHAR(20);
ALTER TABLE order_items ADD COLUMN color_name VARCHAR(80);
