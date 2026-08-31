ALTER TABLE orders
    ADD COLUMN IF NOT EXISTS publisher_ref VARCHAR(20);

CREATE INDEX IF NOT EXISTS idx_orders_publisher_ref ON orders(publisher_ref);
