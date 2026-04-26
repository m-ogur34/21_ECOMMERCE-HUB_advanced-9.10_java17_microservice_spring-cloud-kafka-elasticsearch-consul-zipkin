-- Sipariş kalemleri tablosu
CREATE TABLE IF NOT EXISTS order_items (
    id           BIGSERIAL      PRIMARY KEY,
    order_id     BIGINT         NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_id   BIGINT         NOT NULL,
    product_name VARCHAR(200)   NOT NULL,
    product_sku  VARCHAR(100)   NOT NULL,
    variant_id   BIGINT,
    quantity     INTEGER        NOT NULL,
    unit_price   DECIMAL(12,2)  NOT NULL,
    total_price  DECIMAL(14,2)  NOT NULL,
    created_at   TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_order_items_order_id ON order_items(order_id);
CREATE INDEX IF NOT EXISTS idx_order_items_product_id ON order_items(product_id);
