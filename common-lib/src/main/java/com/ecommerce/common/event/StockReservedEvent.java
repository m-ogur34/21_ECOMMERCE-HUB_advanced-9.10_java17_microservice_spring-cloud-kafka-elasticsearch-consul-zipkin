package com.ecommerce.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * product-service stok rezervasyonunu onayladığında yayınlanan olay.
 * Saga pattern'de bu olay order-service'e "devam et" sinyali verir.
 *
 * reservedItems: Başarılı rezervasyonda hangi ürünler rezerve edildi.
 * Rollback gerekirse bu liste kullanılır.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockReservedEvent {

    private Long orderId;
    private String orderNumber;
    private boolean success;      // true: stok var ve rezerve edildi
    private String failureReason; // success=false ise sebebi (stok yetersiz vb.)

    /** Başarıyla rezerve edilen kalemler — rollback için saklanır */
    private List<StockItem> reservedItems;

    @Builder.Default
    private LocalDateTime occurredAt = LocalDateTime.now();

    /** Rezerve edilen tek bir stok kalemi */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StockItem {
        private Long productId;
        private Long variantId;
        private Integer quantity;
    }
}
