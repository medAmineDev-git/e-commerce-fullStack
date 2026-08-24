CREATE TABLE IF NOT EXISTS categories (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(120) NOT NULL UNIQUE,
    description VARCHAR(1200) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_categories_name ON categories(name);
