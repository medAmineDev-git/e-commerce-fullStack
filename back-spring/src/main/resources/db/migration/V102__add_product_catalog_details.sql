ALTER TABLE products
    ADD COLUMN IF NOT EXISTS sku VARCHAR(80),
    ADD COLUMN IF NOT EXISTS compare_at_price NUMERIC(12,2),
    ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    ADD COLUMN IF NOT EXISTS seo_title VARCHAR(160),
    ADD COLUMN IF NOT EXISTS seo_description VARCHAR(320);

CREATE UNIQUE INDEX IF NOT EXISTS idx_products_sku_unique
    ON products (sku)
    WHERE sku IS NOT NULL;

CREATE TABLE IF NOT EXISTS product_images (
    product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    position INTEGER NOT NULL,
    image_url VARCHAR(2000) NOT NULL,
    PRIMARY KEY (product_id, position)
);

CREATE TABLE IF NOT EXISTS product_sizes (
    product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    position INTEGER NOT NULL,
    size_value VARCHAR(20) NOT NULL,
    PRIMARY KEY (product_id, position)
);

CREATE TABLE IF NOT EXISTS product_colors (
    product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    color_name VARCHAR(80) NOT NULL,
    color_hex VARCHAR(7) NOT NULL,
    PRIMARY KEY (product_id, color_name)
);
