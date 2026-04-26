package com.ecommerce.notification.service;

import com.ecommerce.notification.model.NotificationLog;
import com.ecommerce.notification.repository.NotificationLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * E-posta bildirim servisi implementasyonu.
 *
 * OOP - Polymorphism: NotificationService arayüzünün e-posta implementasyonu.
 * Gerçek uygulamada: JavaMailSender veya SendGrid API kullanılır.
 * Burada simülasyon: log yaz + DB'ye kaydet + ActiveMQ kuyruğuna at.
 *
 * ActiveMQ kullanım senaryosu:
 * - Kafka'dan gelen olay → doğrudan e-posta göndermek yerine
 * - ActiveMQ e-posta kuyruğuna yaz → e-posta worker okur → gönderir
 * - Bu yaklaşım: gönderme başarısız olursa kuyrukta kalır, tekrar denenir
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailNotificationService implements NotificationService {

    private final NotificationLogRepository logRepository;
    private final JmsTemplate jmsTemplate; // ActiveMQ JMS template

    @Override
    public void send(String recipientEmail, String subject, String content, Long orderId) {
        log.info("E-posta bildirimi gönderiliyor: alıcı={}, konu={}", recipientEmail, subject);

        // ActiveMQ kuyruğuna yaz — e-posta worker bu kuyruğu okur
        jmsTemplate.convertAndSend("email.queue",
                "To: " + recipientEmail + "\nSubject: " + subject + "\n\n" + content);

        // Gönderimi logla
        NotificationLog logEntry = NotificationLog.builder()
                .recipientEmail(recipientEmail)
                .type(NotificationLog.NotificationType.EMAIL)
                .status(NotificationLog.NotificationStatus.SENT)
                .subject(subject)
                .content(content)
                .orderId(orderId)
                .sentAt(LocalDateTime.now())
                .build();

        logRepository.save(logEntry);

        // Simülasyon log'u — gerçekte JavaMailSender ile gönderilir
        log.info("=== E-POSTA SİMÜLASYONU ===");
        log.info("Alıcı: {}", recipientEmail);
        log.info("Konu: {}", subject);
        log.info("İçerik: {}", content);
        log.info("===========================");
    }

    @Override
    public NotificationLog.NotificationType getType() {
        return NotificationLog.NotificationType.EMAIL;
    }
}
