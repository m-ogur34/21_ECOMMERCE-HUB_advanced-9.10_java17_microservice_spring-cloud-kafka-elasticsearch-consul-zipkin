package com.ecommerce.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Sipariş onaylandığında (stok rezerve edildi + ödeme alındı) yayınlanan olay.
 * notification-service bu olayı dinleyerek kullanıcıya onay e-postası gönderir.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderConfirmedEvent {

    private Long orderId;
    private String orderNumber;
    private Long userId;
    private String userEmail;
    private BigDecimal totalAmount;
    private String shippingAddress;

    @Builder.Default
    private LocalDateTime occurredAt = LocalDateTime.now();
}
