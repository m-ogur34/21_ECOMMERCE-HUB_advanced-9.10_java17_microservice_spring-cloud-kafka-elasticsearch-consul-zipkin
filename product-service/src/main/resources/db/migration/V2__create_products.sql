-- Ürün tablosu (BaseProduct + Product alanları)
CREATE TABLE IF NOT EXISTS products (
    id             BIGSERIAL      PRIMARY KEY,
    name           VARCHAR(200)   NOT NULL,
    description    TEXT,
    price          DECIMAL(12, 2) NOT NULL,
    stock_quantity INTEGER        NOT NULL DEFAULT 0,
    sku            VARCHAR(100)   NOT NULL UNIQUE,
    active         BOOLEAN        NOT NULL DEFAULT TRUE,
    brand          VARCHAR(100),
    image_url      VARCHAR(500),
    category_id    BIGINT         NOT NULL REFERENCES categories(id),
    created_at     TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- İndeksler — sık kullanılan sorgular için
CREATE INDEX IF NOT EXISTS idx_products_sku ON products(sku);
CREATE INDEX IF NOT EXISTS idx_products_category_id ON products(category_id);
CREATE INDEX IF NOT EXISTS idx_products_active ON products(active);
CREATE INDEX IF NOT EXISTS idx_products_price ON products(price);

-- Örnek ürünler
INSERT INTO products (name, description, price, stock_quantity, sku, brand, category_id)
SELECT 'iPhone 15 Pro', 'Apple''ın en yeni amiral gemisi telefonu', 49999.99, 50, 'APPLE-IP15PRO', 'Apple', id
FROM categories WHERE name = 'Telefonlar'
ON CONFLICT (sku) DO NOTHING;

INSERT INTO products (name, description, price, stock_quantity, sku, brand, category_id)
SELECT 'Samsung Galaxy S24', 'Samsung''ın üst segment telefonu', 39999.99, 75, 'SAMSUNG-S24', 'Samsung', id
FROM categories WHERE name = 'Telefonlar'
ON CONFLICT (sku) DO NOTHING;

INSERT INTO products (name, description, price, stock_quantity, sku, brand, category_id)
SELECT 'MacBook Pro M3', 'Apple Silicon çipli profesyonel laptop', 89999.99, 30, 'APPLE-MBP-M3', 'Apple', id
FROM categories WHERE name = 'Bilgisayarlar'
ON CONFLICT (sku) DO NOTHING;
