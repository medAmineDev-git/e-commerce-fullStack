-- Frais de livraison regles par chaque boutique.
--
-- Ils etaient fixes dans le code, identiques pour toutes : 6,900 DT, offerts
-- a partir de 100 DT. Chaque boutique reprend ces valeurs, pour que rien ne
-- change tant que le vendeur ne les modifie pas dans ses parametres.
--
-- free_delivery_from est facultatif : null, la livraison n est jamais offerte.

ALTER TABLE stores ADD COLUMN delivery_fee NUMERIC(12,3) NOT NULL DEFAULT 6.900;
ALTER TABLE stores ADD COLUMN free_delivery_from NUMERIC(12,3) DEFAULT 100.000;

ALTER TABLE stores ADD CONSTRAINT chk_stores_delivery_fee CHECK (delivery_fee >= 0);
ALTER TABLE stores ADD CONSTRAINT chk_stores_free_delivery_from
    CHECK (free_delivery_from IS NULL OR free_delivery_from > 0);
