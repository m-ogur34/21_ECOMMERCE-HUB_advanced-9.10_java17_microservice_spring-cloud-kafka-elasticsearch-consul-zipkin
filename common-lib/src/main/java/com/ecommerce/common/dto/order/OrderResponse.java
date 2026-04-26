package com.ecommerce.common.dto.order;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Sipariş yanıt DTO'su */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrderResponse {

    private Long id;
    private String orderNumber;  // Kullanıcıya gösterilen sipariş numarası (ORD-2024-00001)
    private String status;       // PENDING, CONFIRMED, SHIPPED, DELIVERED, CANCELLED
    private BigDecimal totalAmount;
    private String shippingAddress;
    private String notes;

    private Long userId;
    private String userEmail;

    private List<OrderItemResponse> items;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
