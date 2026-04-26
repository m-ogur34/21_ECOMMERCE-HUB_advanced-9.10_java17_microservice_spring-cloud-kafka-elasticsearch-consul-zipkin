-- =====================================================================
-- V1: Kullanıcılar, Roller ve Bağlantı Tabloları
-- =====================================================================
-- Flyway migration dosyası. V1__ prefix'i Flyway'e bu script'in
-- diğerlerinden önce çalıştırılması gerektiğini söyler.
-- Versiyonlar sırayla ve TEK SEFERLIK çalışır — production'da bu
-- dosyayı asla DEĞİŞTİRME, yeni migration dosyası oluştur!
-- =====================================================================

-- Rol tablosu — kullanıcıdan önce oluşturulmalı (foreign key bağımlılığı)
CREATE TABLE IF NOT EXISTS roles (
    id          BIGSERIAL PRIMARY KEY,           -- Otomatik artan birincil anahtar
    name        VARCHAR(50)  NOT NULL UNIQUE,    -- ROLE_USER, ROLE_ADMIN, ROLE_MODERATOR
    description VARCHAR(200),
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at  TIMESTAMP                        -- Soft delete: null = aktif
);

-- Kullanıcı tablosu
CREATE TABLE IF NOT EXISTS users (
    id                  BIGSERIAL    PRIMARY KEY,
    first_name          VARCHAR(50)  NOT NULL,
    last_name           VARCHAR(50)  NOT NULL,
    email               VARCHAR(100) NOT NULL UNIQUE,  -- Giriş için benzersiz
    password_hash       VARCHAR(255) NOT NULL,          -- BCrypt hash (~60 karakter)
    phone               VARCHAR(20),
    enabled             BOOLEAN      NOT NULL DEFAULT TRUE,   -- Hesap aktif mi?
    account_non_locked  BOOLEAN      NOT NULL DEFAULT TRUE,   -- Hesap kilitli mi?
    created_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at          TIMESTAMP                             -- Soft delete
);

-- Kullanıcı-Rol ara tablosu (çoka-çok ilişki)
CREATE TABLE IF NOT EXISTS user_roles (
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id BIGINT NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)  -- Bileşik birincil anahtar — duplicate engeller
);

-- Refresh token tablosu
CREATE TABLE IF NOT EXISTS refresh_tokens (
    id         BIGSERIAL    PRIMARY KEY,
    token      VARCHAR(500) NOT NULL UNIQUE,  -- JWT token değeri
    user_id    BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    expires_at TIMESTAMP    NOT NULL,
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ---- İNDEKSLER ----
-- Sık kullanılan sorgular için indeks — WHERE email = ? sorgusu hızlanır
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_deleted_at ON users(deleted_at);
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_token ON refresh_tokens(token);
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user_id ON refresh_tokens(user_id);
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_expires_at ON refresh_tokens(expires_at);

-- ---- BAŞLANGIÇ VERİSİ (SEED DATA) ----

-- Sistem rollerini ekle — bu satırlar idempotent (INSERT OR IGNORE)
INSERT INTO roles (name, description) VALUES
    ('ROLE_USER',      'Standart kullanıcı — alışveriş yapabilir')
    ON CONFLICT (name) DO NOTHING;

INSERT INTO roles (name, description) VALUES
    ('ROLE_ADMIN',     'Yönetici — tam yetki')
    ON CONFLICT (name) DO NOTHING;

INSERT INTO roles (name, description) VALUES
    ('ROLE_MODERATOR', 'Moderatör — içerik denetimi')
    ON CONFLICT (name) DO NOTHING;

-- Geliştirme ortamı için admin kullanıcı
-- Şifre: Admin123! (BCrypt hash — https://bcrypt-generator.com ile üretildi)
-- PRODUCTION'DA MUTLAKA DEĞİŞTİR!
INSERT INTO users (first_name, last_name, email, password_hash, enabled, account_non_locked)
VALUES ('Admin', 'User', 'admin@ecommerce.com',
        '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', -- Admin123!
        TRUE, TRUE)
ON CONFLICT (email) DO NOTHING;

-- Admin kullanıcısına ROLE_ADMIN rolü ver
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u, roles r
WHERE u.email = 'admin@ecommerce.com' AND r.name = 'ROLE_ADMIN'
ON CONFLICT DO NOTHING;

-- Yorum: ON CONFLICT DO NOTHING — migration tekrar çalışırsa duplicate hata vermez
