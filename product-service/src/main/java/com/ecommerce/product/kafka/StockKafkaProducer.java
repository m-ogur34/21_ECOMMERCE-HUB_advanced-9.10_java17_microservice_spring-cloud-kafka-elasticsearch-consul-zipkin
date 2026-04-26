package com.ecommerce.product.kafka;

import com.ecommerce.common.constants.KafkaTopics;
import com.ecommerce.common.event.StockReservedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Stok rezervasyon sonucunu Kafka'ya yayınlar.
 *
 * Saga Pattern - product-service'in Kafka cevabı:
 * order-service ORDER_CREATED yayınlar → biz stok rezervasyonu yaparız
 * → sonucu STOCK_RESERVED topic'ine yayınlarız → order-service dinler
 *
 * KafkaTemplate<Key, Value>:
 * Key: String (orderId) → aynı sipariş mesajları aynı partition'a gider (sıralı)
 * Value: Object → JSON serializasyon
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StockKafkaProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Stok rezervasyon sonucunu yayınlar.
     *
     * @param orderId    Siparişin ID'si
     * @param success    Rezervasyon başarılı mı?
     * @param failReason Başarısız olursa sebebi
     * @param items      Rezerve edilen kalemler (başarılıysa rollback için)
     */
    public void publishStockReserved(Long orderId, boolean success,
                                     String failReason,
                                     List<StockReservedEvent.StockItem> items) {

        StockReservedEvent event = StockReservedEvent.builder()
                .orderId(orderId)
                .success(success)
                .failureReason(failReason)
                .reservedItems(items)
                .build();

        // send() async'tir — CompletableFuture ile sonuç takibi
        CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(KafkaTopics.STOCK_RESERVED, String.valueOf(orderId), event);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                // Kafka send başarısız — order-service PENDING'de kalır, timeout olur
                log.error("STOCK_RESERVED yayınlanamadı: sipariş={}, hata={}",
                        orderId, ex.getMessage());
            } else {
                log.info("STOCK_RESERVED yayınlandı: sipariş={}, başarılı={}, partition={}",
                        orderId, success,
                        result.getRecordMetadata().partition());
            }
        });
    }
}
