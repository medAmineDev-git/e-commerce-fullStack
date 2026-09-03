CREATE TABLE IF NOT EXISTS stores (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    slug VARCHAR(80) NOT NULL UNIQUE,
    description VARCHAR(1500),
    logo_url VARCHAR(2000),
    banner_url VARCHAR(2000),
    phone VARCHAR(40),
    email VARCHAR(160),
    address VARCHAR(500),
    domain VARCHAR(255) UNIQUE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    owner_id BIGINT REFERENCES admin_users(id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_stores_slug ON stores(slug);
CREATE INDEX IF NOT EXISTS idx_stores_domain ON stores(domain);
CREATE INDEX IF NOT EXISTS idx_stores_owner_id ON stores(owner_id);

-- Inserer la boutique par defaut pour migrer l'existant sans rupture
INSERT INTO stores (id, name, slug, description, phone, email, address, is_active, created_at, updated_at)
VALUES (1, 'NOVA Boutique Urbaine', 'nova', 'Boutique urbaine streetwear premium', '+33 1 23 45 67 89', 'contact@nova.local', '10 Rue de la Mode, Paris', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

SELECT setval(pg_get_serial_sequence('stores', 'id'), COALESCE((SELECT MAX(id) FROM stores), 1));

UPDATE stores
SET owner_id = (SELECT id FROM admin_users WHERE username = 'admin' LIMIT 1)
WHERE id = 1 AND EXISTS (SELECT 1 FROM admin_users WHERE username = 'admin');

-- Associer les produits existants
ALTER TABLE products
    ADD COLUMN IF NOT EXISTS store_id BIGINT;

UPDATE products SET store_id = 1 WHERE store_id IS NULL;

ALTER TABLE products
    ALTER COLUMN store_id SET NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_products_store'
    ) THEN
        ALTER TABLE products
            ADD CONSTRAINT fk_products_store FOREIGN KEY (store_id) REFERENCES stores(id) ON DELETE CASCADE;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_products_store_id ON products(store_id);

DROP INDEX IF EXISTS idx_products_sku_unique;
CREATE UNIQUE INDEX IF NOT EXISTS idx_products_store_sku_unique ON products(store_id, sku) WHERE sku IS NOT NULL;

-- Associer les categories existantes
ALTER TABLE categories
    ADD COLUMN IF NOT EXISTS store_id BIGINT;

UPDATE categories SET store_id = 1 WHERE store_id IS NULL;

ALTER TABLE categories
    ALTER COLUMN store_id SET NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_categories_store'
    ) THEN
        ALTER TABLE categories
            ADD CONSTRAINT fk_categories_store FOREIGN KEY (store_id) REFERENCES stores(id) ON DELETE CASCADE;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_categories_store_id ON categories(store_id);

ALTER TABLE categories DROP CONSTRAINT IF EXISTS categories_name_key;
CREATE UNIQUE INDEX IF NOT EXISTS idx_categories_store_name_unique ON categories(store_id, name);

-- Associer les commandes existantes
ALTER TABLE orders
    ADD COLUMN IF NOT EXISTS store_id BIGINT;

UPDATE orders SET store_id = 1 WHERE store_id IS NULL;

ALTER TABLE orders
    ALTER COLUMN store_id SET NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_orders_store'
    ) THEN
        ALTER TABLE orders
            ADD CONSTRAINT fk_orders_store FOREIGN KEY (store_id) REFERENCES stores(id) ON DELETE RESTRICT;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_orders_store_id ON orders(store_id);

-- Associer la configuration de home existante
ALTER TABLE home_configurations
    ADD COLUMN IF NOT EXISTS store_id BIGINT;

UPDATE home_configurations SET store_id = 1 WHERE store_id IS NULL;

ALTER TABLE home_configurations
    ALTER COLUMN store_id SET NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_home_config_store'
    ) THEN
        ALTER TABLE home_configurations
            ADD CONSTRAINT fk_home_config_store FOREIGN KEY (store_id) REFERENCES stores(id) ON DELETE CASCADE;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_home_config_store_id ON home_configurations(store_id);

ALTER TABLE home_configurations DROP CONSTRAINT IF EXISTS home_configurations_config_key_key;
CREATE UNIQUE INDEX IF NOT EXISTS idx_home_config_store_key_unique ON home_configurations(store_id, config_key);
