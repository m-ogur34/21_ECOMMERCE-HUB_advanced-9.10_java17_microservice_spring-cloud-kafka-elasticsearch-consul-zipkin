package com.ecommerce.notification.service;

import com.ecommerce.notification.model.NotificationLog;

/**
 * Bildirim servisi arayüzü — Strategy Pattern.
 * E-posta ve SMS implementasyonları bu arayüzü uygular.
 * OOP - Interface + Polymorphism: notify(event) çağrısı ile doğru implementasyon çalışır.
 */
public interface NotificationService {

    /**
     * Bildirim gönder.
     * @param recipientEmail Alıcı e-posta
     * @param subject        Konu/başlık
     * @param content        İçerik
     * @param orderId        İlgili sipariş ID (opsiyonel)
     */
    void send(String recipientEmail, String subject, String content, Long orderId);

    /** Bu servisin bildirim tipini döner */
    NotificationLog.NotificationType getType();
}
