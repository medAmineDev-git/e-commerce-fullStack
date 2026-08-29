CREATE TABLE IF NOT EXISTS product_seasons (
    product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    position INTEGER NOT NULL,
    season_value VARCHAR(20) NOT NULL,
    PRIMARY KEY (product_id, position)
);

CREATE INDEX IF NOT EXISTS idx_product_seasons_value ON product_seasons(season_value);
