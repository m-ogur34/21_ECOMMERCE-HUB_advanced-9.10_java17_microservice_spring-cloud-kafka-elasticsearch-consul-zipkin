package com.ecommerce.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Sipariş oluşturulduğunda Kafka'ya yayınlanan olay (event) sınıfı.
 *
 * Event-Driven Architecture: servisler birbirini doğrudan çağırmak yerine
 * olaylar üzerinden haberleşir. Bu, loose coupling (gevşek bağlılık) sağlar.
 *
 * Bu olay order-service tarafından publish edilir.
 * product-service (stok düş) ve notification-service (bildirim gönder) bu olayı dinler.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreatedEvent {

    /** Sipariş ID — olayı hangi siparişin tetiklediği */
    private Long orderId;

    /** Kullanıcı ID — bildirim için kime gönderileceği */
    private Long userId;

    /** Kullanıcı e-postası — bildirim için */
    private String userEmail;

    /** Sipariş numarası — insan okunabilir: ORD-2024-00001 */
    private String orderNumber;

    /** Toplam tutar */
    private BigDecimal totalAmount;

    /** Teslimat adresi */
    private String shippingAddress;

    /** Sipariş kalemleri — stok rezervasyonu için gerekli */
    private List<OrderItemEvent> items;

    /** Olayın oluşturulma zamanı — sıralama ve idempotency için */
    @Builder.Default
    private LocalDateTime occurredAt = LocalDateTime.now();

    /**
     * Sipariş kalemi olay verisi — inner class kullanımı OOP örneği.
     * Bağımsız bir class yaratmak yerine ilgili veriyi bir arada tutar.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemEvent {
        private Long productId;
        private Long variantId;  // null olabilir
        private Integer quantity;
        private BigDecimal unitPrice;
    }
}
