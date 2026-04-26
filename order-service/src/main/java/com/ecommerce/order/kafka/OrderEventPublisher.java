package com.ecommerce.order.kafka;

import com.ecommerce.common.constants.KafkaTopics;
import com.ecommerce.common.event.OrderConfirmedEvent;
import com.ecommerce.common.event.StockReleasedEvent;
import com.ecommerce.order.model.Order;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Order service'in Kafka event yayıncısı.
 *
 * İki tip event yayınlar:
 * 1. ORDER_CONFIRMED  → notification-service email/sms gönderir
 * 2. STOCK_RELEASED   → product-service stok geri verir (sipariş iptalinde)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Sipariş onaylandığında bildirim servisini tetikler.
     * Consumer: notification-service → kullanıcıya e-posta/SMS gönderir.
     */
    public void publishOrderConfirmed(Order order) {
        OrderConfirmedEvent event = OrderConfirmedEvent.builder()
                .orderId(order.getId())
                .orderNumber(order.getOrderNumber())
                .userId(order.getUserId())
                .userEmail(order.getUserEmail())
                .totalAmount(order.getTotalAmount())
                .shippingAddress(order.getShippingAddress())
                .build();

        kafkaTemplate.send(
                KafkaTopics.ORDER_CONFIRMED,
                String.valueOf(order.getId()),
                event
        ).whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("ORDER_CONFIRMED yayınlanamadı: sipariş={}", order.getOrderNumber());
            } else {
                log.info("ORDER_CONFIRMED yayınlandı: sipariş={}", order.getOrderNumber());
            }
        });
    }

    /**
     * Sipariş iptal edildiğinde stok serbest bırakılır.
     * Consumer: product-service → stok miktarını artırır.
     *
     * @param order İptal edilen sipariş — kalem bilgileri stok geri alma için kullanılır
     */
    public void publishStockReleased(Order order, String reason) {
        List<StockReleasedEvent.StockReleaseItem> items = order.getItems().stream()
                .map(item -> StockReleasedEvent.StockReleaseItem.builder()
                        .productId(item.getProductId())
                        .variantId(item.getVariantId())
                        .quantity(item.getQuantity())
                        .build())
                .collect(Collectors.toList());

        StockReleasedEvent event = StockReleasedEvent.builder()
                .orderId(order.getId())
                .reason(reason)
                .items(items)
                .build();

        kafkaTemplate.send(
                KafkaTopics.STOCK_RELEASED,
                String.valueOf(order.getId()),
                event
        ).whenComplete((result, ex) -> {
            if (ex != null) {
                // Kritik: stok geri verilemedi — manuel müdahale gerekebilir
                log.error("CRITICAL: STOCK_RELEASED yayınlanamadı: sipariş={}", order.getOrderNumber());
            } else {
                log.info("STOCK_RELEASED yayınlandı: sipariş={}, sebep={}",
                        order.getOrderNumber(), reason);
            }
        });
    }
}
