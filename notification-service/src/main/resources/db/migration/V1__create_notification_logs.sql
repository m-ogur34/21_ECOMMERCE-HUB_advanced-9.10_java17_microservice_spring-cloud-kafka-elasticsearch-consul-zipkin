-- =====================================================================
-- Bildirim Log Tablosu
-- Gönderilen tüm e-posta ve SMS kayıtları burada saklanır.
-- Denetim (audit) ve hata ayıklama için kritiktir.
-- =====================================================================

CREATE TABLE notification_logs (
    id               BIGSERIAL PRIMARY KEY,

    -- Kime gönderildi
    recipient_email  VARCHAR(255) NOT NULL,

    -- Bildirim tipi: EMAIL, SMS, PUSH
    type             VARCHAR(30)  NOT NULL,

    -- Gönderim durumu: PENDING → SENT | FAILED
    status           VARCHAR(20)  NOT NULL DEFAULT 'PENDING',

    -- E-posta konusu (SMS için boş olabilir)
    subject          VARCHAR(200),

    -- Bildirim içeriği (HTML veya düz metin)
    content          TEXT,

    -- İlgili sipariş ID — hangi sipariş için gönderildi
    order_id         BIGINT,

    -- Hata durumunda hata mesajı
    error_message    VARCHAR(500),

    -- Gönderim zamanı (başarılıysa dolar)
    sent_at          TIMESTAMP,

    -- Kayıt oluşturulma zamanı
    created_at       TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- Sipariş bazlı sorgular için index — "Bu siparişin bildirimleri neler?"
CREATE INDEX idx_notification_logs_order_id ON notification_logs (order_id);

-- E-posta bazlı sorgular — "Bu kullanıcıya ne zaman bildirim gönderildi?"
CREATE INDEX idx_notification_logs_recipient ON notification_logs (recipient_email);

-- Durum bazlı sorgular — "Başarısız bildirimler hangileri?"
CREATE INDEX idx_notification_logs_status ON notification_logs (status);

-- Zaman bazlı sorgular — son 24 saat gibi dönemsel raporlar için
CREATE INDEX idx_notification_logs_created_at ON notification_logs (created_at DESC);
