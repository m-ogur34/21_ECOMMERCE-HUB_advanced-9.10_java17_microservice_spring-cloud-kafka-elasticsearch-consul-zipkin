-- Kategori tablosu — self-referencing (öz referanslı)
CREATE TABLE IF NOT EXISTS categories (
    id          BIGSERIAL    PRIMARY KEY,
    name        VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(500),
    parent_id   BIGINT       REFERENCES categories(id) ON DELETE SET NULL
);

-- Başlangıç kategorileri
INSERT INTO categories (name, description) VALUES
    ('Elektronik', 'Elektronik ürünler'),
    ('Giyim', 'Giyim ve aksesuar'),
    ('Kitap', 'Kitap ve kırtasiye'),
    ('Spor', 'Spor ve outdoor'),
    ('Ev & Yaşam', 'Ev dekorasyon ve yaşam')
ON CONFLICT (name) DO NOTHING;

-- Alt kategoriler
INSERT INTO categories (name, description, parent_id)
SELECT 'Telefonlar', 'Akıllı telefonlar', id FROM categories WHERE name = 'Elektronik'
ON CONFLICT (name) DO NOTHING;

INSERT INTO categories (name, description, parent_id)
SELECT 'Bilgisayarlar', 'Laptop ve masaüstü', id FROM categories WHERE name = 'Elektronik'
ON CONFLICT (name) DO NOTHING;
