package com.ecommerce.notification.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Bildirim log kaydı — gönderilen tüm bildirimler burada tutulur.
 * Hata ayıklama ve denetim (audit) için önemlidir.
 */
@Entity
@Table(name = "notification_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "recipient_email", nullable = false)
    private String recipientEmail;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30)
    private NotificationType type; // EMAIL, SMS

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private NotificationStatus status; // SENT, FAILED, PENDING

    @Column(name = "subject", length = 200)
    private String subject;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    /** Bildirim tipi */
    public enum NotificationType { EMAIL, SMS, PUSH }

    /** Bildirim durumu */
    public enum NotificationStatus { PENDING, SENT, FAILED }
}
