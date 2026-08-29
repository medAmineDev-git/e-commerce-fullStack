CREATE TABLE IF NOT EXISTS home_configurations (
    id BIGSERIAL PRIMARY KEY,
    config_key VARCHAR(40) NOT NULL UNIQUE,
    title VARCHAR(160) NOT NULL,
    text VARCHAR(1200) NOT NULL,
    featured_product_id BIGINT NOT NULL REFERENCES products(id)
);

INSERT INTO home_configurations (config_key, title, text, featured_product_id)
SELECT 'home',
       'Style urbain, livraison rapide, paiement a la livraison.',
       'Decouvre une selection orientee streetwear premium avec une experience mobile ultra simple.',
       MIN(id)
FROM products
WHERE NOT EXISTS (SELECT 1 FROM home_configurations)
HAVING COUNT(id) > 0;
