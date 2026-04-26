package com.ecommerce.product.kafka;

import com.ecommerce.common.constants.KafkaTopics;
import com.ecommerce.common.event.OrderCreatedEvent;
import com.ecommerce.common.event.StockReservedEvent;
import com.ecommerce.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * ORDER_CREATED topic'ini dinler ve stok rezervasyonu yapar.
 *
 * Bu sınıf Saga Pattern'ının product-service tarafıdır:
 *
 * order-service → Kafka[ORDER_CREATED] → (bu sınıf) → stok düşür → Kafka[STOCK_RESERVED] → order-service
 *
 * Başarı akışı:
 *   1. Her sipariş kalemi için decreaseStock() çağır
 *   2. Tümü başarılıysa StockReservedEvent(success=true) yayınla
 *
 * Hata akışı (stok yetersiz):
 *   1. Başarısız olan kalemde dur
 *   2. Önceki azaltılan stokları geri ver (partial rollback)
 *   3. StockReservedEvent(success=false) yayınla → order-service siparişi iptal eder
 *
 * AckMode.MANUAL: mesaj başarıyla işlenirse acknowledge et.
 * Hata durumunda acknowledge etmeden mesajı Kafka'da bırak → yeniden gönderim.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCreatedEventConsumer {

    private final ProductService productService;
    private final StockKafkaProducer stockKafkaProducer;

    @KafkaListener(
        topics = KafkaTopics.ORDER_CREATED,
        groupId = KafkaTopics.PRODUCT_SERVICE_GROUP,
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleOrderCreated(
            @Payload OrderCreatedEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            Acknowledgment acknowledgment) {

        log.info("ORDER_CREATED alındı: sipariş={}, kalem sayısı={}",
                event.getOrderId(), event.getItems().size());

        // Başarıyla işlenen kalemleri takip et — hata durumunda geri alınacak
        List<StockReservedEvent.StockItem> processedItems = new ArrayList<>();
        String failureReason = null;
        boolean success = true;

        for (OrderCreatedEvent.OrderItemEvent item : event.getItems()) {
            try {
                // Stoku düşür — race condition için DB-level atomic UPDATE kullanılır
                productService.decreaseStock(item.getProductId(), item.getQuantity());

                processedItems.add(StockReservedEvent.StockItem.builder()
                        .productId(item.getProductId())
                        .variantId(item.getVariantId())
                        .quantity(item.getQuantity())
                        .build());

                log.debug("Stok düşürüldü: ürün={}, miktar={}",
                        item.getProductId(), item.getQuantity());

            } catch (Exception e) {
                // Bu kalem için stok yetersiz veya ürün bulunamadı
                log.warn("Stok rezervasyon hatası: ürün={}, miktar={}, sebep={}",
                        item.getProductId(), item.getQuantity(), e.getMessage());

                success = false;
                failureReason = String.format("Ürün ID=%d için stok yetersiz (talep: %d)",
                        item.getProductId(), item.getQuantity());

                // Partial rollback: daha önce düşürülen stokları geri ver
                for (StockReservedEvent.StockItem processed : processedItems) {
                    try {
                        productService.increaseStock(processed.getProductId(), processed.getQuantity());
                        log.info("Partial rollback: ürün={} stok geri verildi", processed.getProductId());
                    } catch (Exception rollbackEx) {
                        // Kritik: rollback başarısız — alarm/alert gerektirir
                        log.error("CRITICAL: Partial rollback başarısız! ürün={}, hata={}",
                                processed.getProductId(), rollbackEx.getMessage());
                    }
                }
                processedItems.clear();
                break;  // Diğer kalemleri işleme — zaten başarısız
            }
        }

        // Sonucu Kafka'ya yayınla — order-service bu sonucu bekliyor
        stockKafkaProducer.publishStockReserved(
                event.getOrderId(), success, failureReason, processedItems);

        // Her durumda acknowledge et — mesajı tekrar işlemek istemiyoruz
        // (Başarısız durumda da acknowledge: idempotent değiliz, tekrar işleme çift stok düşürür)
        acknowledgment.acknowledge();

        log.info("ORDER_CREATED işlendi: sipariş={}, sonuç={}",
                event.getOrderId(), success ? "BAŞARILI" : "BAŞARISIZ");
    }
}
