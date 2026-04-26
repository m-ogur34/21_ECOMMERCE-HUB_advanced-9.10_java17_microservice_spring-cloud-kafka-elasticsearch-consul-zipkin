package com.ecommerce.order.service.saga;

import com.ecommerce.common.dto.order.OrderItemRequest;
import com.ecommerce.common.util.ValidationUtil;
import com.ecommerce.order.model.Order;
import com.ecommerce.order.model.OrderItem;
import com.ecommerce.order.model.OrderStatus;
import com.ecommerce.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Saga 1. Adım: Siparişi veritabanına yazar.
 *
 * execute(): Order entity'sini oluşturur ve PENDING durumuyla kaydeder.
 * compensate(): Sipariş oluşturulmuşsa siler (hard delete — CANCELLED da yapılabilir).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CreateOrderStep implements SagaStep<SagaContext> {

    private final OrderRepository orderRepository;

    @Override
    @Transactional
    public void execute(SagaContext context) {
        log.info("Saga Adım 1: Sipariş oluşturuluyor - kullanıcı={}", context.getUserId());

        Order order = Order.builder()
                .userId(context.getUserId())
                .userEmail(context.getUserEmail())
                .orderNumber(generateOrderNumber()) // Benzersiz sipariş numarası
                .status(OrderStatus.PENDING)
                .shippingAddress(context.getOrderRequest().getShippingAddress())
                .notes(context.getOrderRequest().getNotes())
                .build();

        // Kalemleri oluştur — fiyatlar product-service'den alınmalı (gerçek uygulamada)
        for (OrderItemRequest itemRequest : context.getOrderRequest().getItems()) {
            OrderItem item = OrderItem.builder()
                    .productId(itemRequest.getProductId())
                    .variantId(itemRequest.getVariantId())
                    .quantity(itemRequest.getQuantity())
                    .productName("Ürün #" + itemRequest.getProductId()) // Gerçekte product-service'den alınır
                    .productSku("SKU-" + itemRequest.getProductId())
                    .unitPrice(BigDecimal.valueOf(99.99)) // Gerçekte product-service'den alınır
                    .build();

            item.calculateTotalPrice(); // unit × quantity
            order.addItem(item);
        }

        order.calculateTotal(); // Tüm kalemlerin toplamı

        Order savedOrder = orderRepository.save(order);

        // Context'i güncelle — bir sonraki adım bu siparişi kullanır
        context.setCreatedOrder(savedOrder);
        log.info("Saga Adım 1 tamamlandı: sipariş ID={}, numara={}",
                savedOrder.getId(), savedOrder.getOrderNumber());
    }

    @Override
    @Transactional
    public void compensate(SagaContext context) {
        if (context.getCreatedOrder() != null) {
            log.warn("Saga Adım 1 telafi: Sipariş iptal ediliyor - ID={}",
                    context.getCreatedOrder().getId());

            // Siparişi CANCELLED durumuna al — hard delete yerine soft cancel
            Order order = context.getCreatedOrder();
            order.transitionTo(OrderStatus.CANCELLED);
            orderRepository.save(order);
        }
    }

    /** Benzersiz sipariş numarası üretir: ORD-20240115-ABC12345 */
    private String generateOrderNumber() {
        String uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        return "ORD-" + java.time.LocalDate.now().toString().replace("-", "") + "-" + uuid;
    }
}
