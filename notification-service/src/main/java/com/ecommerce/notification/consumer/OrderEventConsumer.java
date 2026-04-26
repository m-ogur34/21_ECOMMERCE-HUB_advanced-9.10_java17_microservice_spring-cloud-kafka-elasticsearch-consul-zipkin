package com.ecommerce.notification.consumer;

import com.ecommerce.common.constants.KafkaTopics;
import com.ecommerce.common.event.OrderConfirmedEvent;
import com.ecommerce.common.event.OrderCreatedEvent;
import com.ecommerce.notification.service.EmailNotificationService;
import com.ecommerce.notification.service.SmsNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Sipariş olay Kafka tüketicisi.
 *
 * Observer Pattern — bu sınıf, order-service'in yayınladığı olayların gözlemcisi.
 * Kafka consumer group sayesinde:
 * - Aynı mesajı birden fazla notification-service instance'ı işlemez (yük dağılımı)
 * - Farklı consumer group'lar aynı mesajı tekrar alabilir
 *
 * Bildirim içerikleri gerçek e-posta şablonlarından türetilmeli.
 * Thymeleaf gibi şablon motoru ile HTML e-posta üretilebilir.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventConsumer {

    private final EmailNotificationService emailService;
    private final SmsNotificationService smsService;

    /**
     * Sipariş oluşturuldu olayı — kullanıcıya "sipariş alındı" bildirimi.
     */
    @KafkaListener(
        topics = KafkaTopics.ORDER_CREATED,
        groupId = KafkaTopics.NOTIFICATION_SERVICE_GROUP
    )
    public void handleOrderCreated(
            @Payload OrderCreatedEvent event,
            Acknowledgment acknowledgment) {

        log.info("Sipariş oluşturuldu olayı alındı: {} ({})", event.getOrderNumber(), event.getUserEmail());

        try {
            String subject = "Siparişiniz Alındı - " + event.getOrderNumber();
            String content = buildOrderCreatedContent(event);

            // E-posta bildir
            emailService.send(event.getUserEmail(), subject, content, event.getOrderId());

            acknowledgment.acknowledge();
            log.info("Sipariş alındı bildirimi gönderildi: {}", event.getUserEmail());

        } catch (Exception e) {
            log.error("Bildirim gönderilemedi: {}", e.getMessage());
        }
    }

    /**
     * Sipariş onaylandı olayı — stok rezerve edildi, sipariş işleme alındı.
     */
    @KafkaListener(
        topics = KafkaTopics.ORDER_CONFIRMED,
        groupId = KafkaTopics.NOTIFICATION_SERVICE_GROUP
    )
    public void handleOrderConfirmed(
            @Payload OrderConfirmedEvent event,
            Acknowledgment acknowledgment) {

        log.info("Sipariş onaylandı olayı alındı: {}", event.getOrderNumber());

        try {
            String subject = "Siparişiniz Onaylandı - " + event.getOrderNumber();
            String content = "Sevgili müşterimiz,\n\n"
                    + event.getOrderNumber() + " numaralı siparişiniz onaylandı.\n"
                    + "Teslimat adresi: " + event.getShippingAddress() + "\n\n"
                    + "Siparişinizi takip etmek için hesabınıza giriş yapabilirsiniz.\n\n"
                    + "İyi alışverişler!";

            emailService.send(event.getUserEmail(), subject, content, event.getOrderId());

            // SMS ile de bildir
            smsService.send(event.getUserEmail(),
                    "Sipariş Onaylandı",
                    "Siparişiniz onaylandı: " + event.getOrderNumber(),
                    event.getOrderId());

            acknowledgment.acknowledge();

        } catch (Exception e) {
            log.error("Onay bildirimi gönderilemedi: {}", e.getMessage());
        }
    }

    /** Sipariş oluşturuldu e-posta içeriği */
    private String buildOrderCreatedContent(OrderCreatedEvent event) {
        StringBuilder sb = new StringBuilder();
        sb.append("Sevgili müşterimiz,\n\n");
        sb.append("Siparişiniz başarıyla alındı.\n");
        sb.append("Sipariş Numarası: ").append(event.getOrderNumber()).append("\n");
        sb.append("Toplam Tutar: ").append(event.getTotalAmount()).append(" TL\n");
        sb.append("Teslimat Adresi: ").append(event.getShippingAddress()).append("\n\n");
        sb.append("Siparişiniz işleme alınıyor. Onaylandığında bildirim alacaksınız.\n\n");
        sb.append("İyi alışverişler!\nECommerceHub Ekibi");
        return sb.toString();
    }
}
