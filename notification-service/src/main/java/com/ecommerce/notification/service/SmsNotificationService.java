package com.ecommerce.notification.service;

import com.ecommerce.notification.model.NotificationLog;
import com.ecommerce.notification.repository.NotificationLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * SMS bildirim servisi implementasyonu.
 * OOP - Polymorphism: NotificationService arayüzünün SMS implementasyonu.
 * Gerçek uygulamada: Twilio, Netgsm gibi SMS gateway API'si kullanılır.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SmsNotificationService implements NotificationService {

    private final NotificationLogRepository logRepository;

    @Override
    public void send(String recipientEmail, String subject, String content, Long orderId) {
        // SMS'te e-posta yerine telefon numarası kullanılır
        // Bu örnekte e-posta adresine yazıyoruz (simülasyon)
        log.info("=== SMS SİMÜLASYONU ===");
        log.info("Alıcı: {}", recipientEmail);
        log.info("Mesaj: {}", content.substring(0, Math.min(160, content.length()))); // SMS 160 karakter sınırı
        log.info("=======================");

        NotificationLog logEntry = NotificationLog.builder()
                .recipientEmail(recipientEmail)
                .type(NotificationLog.NotificationType.SMS)
                .status(NotificationLog.NotificationStatus.SENT)
                .subject(subject)
                .content(content)
                .orderId(orderId)
                .sentAt(LocalDateTime.now())
                .build();

        logRepository.save(logEntry);
    }

    @Override
    public NotificationLog.NotificationType getType() {
        return NotificationLog.NotificationType.SMS;
    }
}
