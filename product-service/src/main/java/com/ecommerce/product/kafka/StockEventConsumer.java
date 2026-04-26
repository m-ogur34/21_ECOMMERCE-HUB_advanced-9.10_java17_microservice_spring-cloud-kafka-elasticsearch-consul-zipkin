package com.ecommerce.product.kafka;

import com.ecommerce.common.constants.KafkaTopics;
import com.ecommerce.common.event.StockReleasedEvent;
import com.ecommerce.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Kafka stok serbest bırakma olayı tüketicisi.
 *
 * Saga pattern rollback adımı: sipariş iptal edildiğinde
 * rezerve edilen stok geri bırakılır.
 *
 * @KafkaListener: Bu metodu Kafka consumer olarak kaydeder.
 * topics: Hangi Kafka topic'ini dinleyeceği
 * groupId: Consumer group adı — aynı group'taki tüm instance'lar load balance eder
 *
 * AckMode.MANUAL: mesaj işlendikten sonra offset manuel olarak commit edilir.
 * Otomatik commit'te mesaj işlenemezse kaybolabilir; manuel commit güvenli.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StockEventConsumer {

    private final ProductService productService;

    @KafkaListener(
        topics = KafkaTopics.STOCK_RELEASED,
        groupId = KafkaTopics.PRODUCT_SERVICE_GROUP,
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleStockReleased(
            @Payload StockReleasedEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            Acknowledgment acknowledgment) {

        log.info("Stok serbest bırakma olayı alındı: sipariş={}, sebep={}",
                event.getOrderId(), event.getReason());

        try {
            // Her kalem için stoku geri ver
            event.getItems().forEach(item -> {
                log.info("Stok geri veriliyor: ürün={}, miktar={}",
                        item.getProductId(), item.getQuantity());
                productService.increaseStock(item.getProductId(), item.getQuantity());
            });

            // Başarıyla işlendi — offset'i commit et
            acknowledgment.acknowledge();
            log.info("Stok serbest bırakma başarılı: sipariş={}", event.getOrderId());

        } catch (Exception e) {
            // Hata durumunda acknowledge etme — Kafka yeniden gönderir (retry)
            log.error("Stok serbest bırakma hatası: sipariş={}, hata={}",
                    event.getOrderId(), e.getMessage());
            // Gerçek uygulamada: Dead Letter Topic'e gönder
        }
    }
}
