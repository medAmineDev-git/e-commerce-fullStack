-- Prix de gros d'un produit, facultatif.
--
-- Donnee interne au vendeur : elle n est renvoyee que par les routes du
-- back-office, jamais par celles de la vitrine.

ALTER TABLE products ADD COLUMN wholesale_price NUMERIC(12,3);

ALTER TABLE products ADD CONSTRAINT chk_products_wholesale_price
    CHECK (wholesale_price IS NULL OR wholesale_price > 0);
