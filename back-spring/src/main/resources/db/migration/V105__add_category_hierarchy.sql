ALTER TABLE categories
    ADD COLUMN IF NOT EXISTS parent_id BIGINT REFERENCES categories(id) ON DELETE SET NULL;

ALTER TABLE products
    ADD COLUMN IF NOT EXISTS subcategory VARCHAR(80);

CREATE INDEX IF NOT EXISTS idx_categories_parent_id ON categories(parent_id);
CREATE INDEX IF NOT EXISTS idx_products_subcategory ON products(subcategory);
