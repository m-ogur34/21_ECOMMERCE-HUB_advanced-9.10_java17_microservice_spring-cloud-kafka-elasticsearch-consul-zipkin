package com.ecommerce.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Saga rollback sırasında rezerve edilen stok serbest bırakıldığında yayınlanan olay.
 * Saga pattern'in compensating transaction (telafi işlemi) adımıdır.
 *
 * Senaryo: sipariş oluşturuldu → stok rezerve edildi → ödeme başarısız →
 * ROLLBACK: bu olay ile stok geri bırakılır.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockReleasedEvent {

    private Long orderId;
    private String reason; // Neden serbest bırakıldığı — loglama için

    /** Hangi ürünlerin stoğu geri verilecek */
    private List<StockReleaseItem> items;

    @Builder.Default
    private LocalDateTime occurredAt = LocalDateTime.now();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StockReleaseItem {
        private Long productId;
        private Long variantId;
        private Integer quantity;
    }
}
