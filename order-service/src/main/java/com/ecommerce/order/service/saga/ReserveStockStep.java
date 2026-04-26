package com.ecommerce.order.service.saga;

import com.ecommerce.common.constants.KafkaTopics;
import com.ecommerce.common.event.OrderCreatedEvent;
import com.ecommerce.common.event.StockReleasedEvent;
import com.ecommerce.order.model.Order;
import com.ecommerce.order.model.OrderItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Saga 2. Adım: Kafka ile stok rezervasyonu talep eder.
 *
 * execute(): Kafka'ya OrderCreatedEvent yayınlar.
 *            product-service bu olayı dinleyerek stok rezerve eder.
 *
 * compensate(): Eğer sonraki adım başarısız olursa stok serbest bırakılır.
 *               Kafka'ya StockReleasedEvent yayınlanır.
 *
 * NOT: Bu adım asenkron — stok rezervasyonunun onayı ayrı bir Kafka topic'ten gelir.
 * Basitlik için bu örnekte senkron akış simüle edilmiştir.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReserveStockStep implements SagaStep<SagaContext> {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public void execute(SagaContext context) throws Exception {
        Order order = context.getCreatedOrder();
        log.info("Saga Adım 2: Stok rezervasyonu talep ediliyor - sipariş={}", order.getOrderNumber());

        // Sipariş kalemlerini Kafka event formatına dönüştür
        List<OrderCreatedEvent.OrderItemEvent> itemEvents = order.getItems().stream()
                .map(item -> OrderCreatedEvent.OrderItemEvent.builder()
                        .productId(item.getProductId())
                        .variantId(item.getVariantId())
                        .quantity(item.getQuantity())
                        .unitPrice(item.getUnitPrice())
                        .build())
                .collect(Collectors.toList());

        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .orderId(order.getId())
                .userId(context.getUserId())
                .userEmail(context.getUserEmail())
                .orderNumber(order.getOrderNumber())
                .totalAmount(order.getTotalAmount())
                .shippingAddress(order.getShippingAddress())
                .items(itemEvents)
                .build();

        // Kafka'ya gönder — product-service dinler
        // key: orderId ile aynı partition'a gider — sıralı işlem garantisi
        kafkaTemplate.send(KafkaTopics.ORDER_CREATED, String.valueOf(order.getId()), event);

        log.info("Saga Adım 2 tamamlandı: OrderCreatedEvent yayınlandı - sipariş={}",
                order.getOrderNumber());
    }

    @Override
    public void compensate(SagaContext context) {
        Order order = context.getCreatedOrder();
        if (order == null) return;

        log.warn("Saga Adım 2 telafi: Stok serbest bırakılıyor - sipariş={}", order.getOrderNumber());

        // Rezerve edilen stoğu geri ver
        List<StockReleasedEvent.StockReleaseItem> releaseItems = order.getItems().stream()
                .map(item -> StockReleasedEvent.StockReleaseItem.builder()
                        .productId(item.getProductId())
                        .variantId(item.getVariantId())
                        .quantity(item.getQuantity())
                        .build())
                .collect(Collectors.toList());

        StockReleasedEvent releaseEvent = StockReleasedEvent.builder()
                .orderId(order.getId())
                .reason("Saga rollback — sipariş iptal edildi")
                .items(releaseItems)
                .build();

        kafkaTemplate.send(KafkaTopics.STOCK_RELEASED, String.valueOf(order.getId()), releaseEvent);
    }
}
