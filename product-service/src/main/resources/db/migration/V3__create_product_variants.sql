-- Ürün varyant tablosu — ProductVariant entity'si için
-- BaseProduct alanları + varyanta özgü alanlar
CREATE TABLE IF NOT EXISTS product_variants (
    id               BIGSERIAL      PRIMARY KEY,
    name             VARCHAR(200)   NOT NULL,
    description      TEXT,
    price            DECIMAL(12, 2) NOT NULL,
    stock_quantity   INTEGER        NOT NULL DEFAULT 0,
    sku              VARCHAR(100)   NOT NULL UNIQUE,
    active           BOOLEAN        NOT NULL DEFAULT TRUE,
    product_id       BIGINT         NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    color            VARCHAR(50),
    size             VARCHAR(20),
    additional_price DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    created_at       TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_variants_product_id ON product_variants(product_id);
CREATE INDEX IF NOT EXISTS idx_variants_sku ON product_variants(sku);
