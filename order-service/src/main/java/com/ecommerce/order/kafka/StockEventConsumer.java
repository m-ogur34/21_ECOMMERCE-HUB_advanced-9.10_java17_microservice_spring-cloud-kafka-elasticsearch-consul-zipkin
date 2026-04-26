package com.ecommerce.order.kafka;

import com.ecommerce.common.constants.KafkaTopics;
import com.ecommerce.common.event.StockReservedEvent;
import com.ecommerce.order.model.Order;
import com.ecommerce.order.model.OrderStatus;
import com.ecommerce.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Stok rezervasyon yanıt tüketicisi.
 *
 * Saga async akışı:
 * order-service → Kafka[ORDER_CREATED] → product-service → Kafka[STOCK_RESERVED] → order-service
 *
 * product-service stok rezervasyonunu tamamladığında bu consumer tetiklenir.
 * Başarılıysa: sipariş CONFIRMED durumuna geçer
 * Başarısızsa: sipariş CANCELLED durumuna geçer (stok yetersiz)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StockEventConsumer {

    private final OrderRepository orderRepository;
    private final OrderEventPublisher eventPublisher;

    @KafkaListener(
        topics = KafkaTopics.STOCK_RESERVED,
        groupId = KafkaTopics.ORDER_SERVICE_GROUP,
        containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void handleStockReserved(
            @Payload StockReservedEvent event,
            Acknowledgment acknowledgment) {

        log.info("Stok rezervasyon yanıtı alındı: sipariş={}, başarılı={}",
                event.getOrderId(), event.isSuccess());

        try {
            Order order = orderRepository.findById(event.getOrderId())
                    .orElse(null);

            if (order == null) {
                log.warn("Sipariş bulunamadı: {}", event.getOrderId());
                acknowledgment.acknowledge();
                return;
            }

            if (event.isSuccess()) {
                // Stok rezerve edildi — siparişi onayla
                order.transitionTo(OrderStatus.CONFIRMED);
                orderRepository.save(order);
                // Bildirim servisi tetikle — kullanıcıya e-posta/SMS gönderilecek
                eventPublisher.publishOrderConfirmed(order);
                log.info("Sipariş onaylandı ve bildirim gönderildi: {}", order.getOrderNumber());
            } else {
                // Stok yetersiz — siparişi iptal et
                order.transitionTo(OrderStatus.CANCELLED);
                log.warn("Sipariş iptal edildi (stok yetersiz): {} - {}",
                        order.getOrderNumber(), event.getFailureReason());
            }

            if (!event.isSuccess()) {
                orderRepository.save(order);
            }
            acknowledgment.acknowledge();

        } catch (Exception e) {
            log.error("Stok yanıt işleme hatası: {}", e.getMessage());
            // Acknowledge etme — Kafka yeniden gönderir
        }
    }
}
