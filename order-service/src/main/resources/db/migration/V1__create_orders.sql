-- Sipariş tablosu
CREATE TABLE IF NOT EXISTS orders (
    id               BIGSERIAL    PRIMARY KEY,
    user_id          BIGINT       NOT NULL,
    user_email       VARCHAR(100) NOT NULL,
    order_number     VARCHAR(30)  NOT NULL UNIQUE,
    status           VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    total_amount     DECIMAL(14,2) NOT NULL,
    shipping_address VARCHAR(500) NOT NULL,
    notes            VARCHAR(1000),
    created_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_orders_user_id ON orders(user_id);
CREATE INDEX IF NOT EXISTS idx_orders_order_number ON orders(order_number);
CREATE INDEX IF NOT EXISTS idx_orders_status ON orders(status);
CREATE INDEX IF NOT EXISTS idx_orders_created_at ON orders(created_at DESC);
