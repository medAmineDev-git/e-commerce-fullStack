-- Frais de livraison enregistres avec la commande.
--
-- La vitrine les ajoutait au total affiche au client, mais le serveur ne les
-- comptait pas : le client validait 66,800 DT et le vendeur voyait 59,900 DT.
-- Desormais le total enregistre les inclut, et ce champ en garde le detail.
--
-- Les commandes existantes n ont jamais porte de frais : zero est leur valeur
-- exacte, et leur total reste inchange.

ALTER TABLE orders ADD COLUMN delivery_fee NUMERIC(12,3) NOT NULL DEFAULT 0;
