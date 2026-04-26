package com.ecommerce.order.mapper;

import com.ecommerce.common.dto.order.OrderItemResponse;
import com.ecommerce.common.dto.order.OrderResponse;
import com.ecommerce.order.model.Order;
import com.ecommerce.order.model.OrderItem;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/** Sipariş entity ↔ DTO dönüşüm sınıfı */
@Component
public class OrderMapper {

    public OrderResponse toResponse(Order order) {
        if (order == null) return null;

        return OrderResponse.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .status(order.getStatus().name())
                .totalAmount(order.getTotalAmount())
                .shippingAddress(order.getShippingAddress())
                .notes(order.getNotes())
                .userId(order.getUserId())
                .userEmail(order.getUserEmail())
                .items(mapItems(order.getItems()))
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }

    private List<OrderItemResponse> mapItems(List<OrderItem> items) {
        if (items == null) return List.of();
        return items.stream().map(this::toItemResponse).collect(Collectors.toList());
    }

    private OrderItemResponse toItemResponse(OrderItem item) {
        return OrderItemResponse.builder()
                .id(item.getId())
                .productId(item.getProductId())
                .productName(item.getProductName())
                .productSku(item.getProductSku())
                .variantId(item.getVariantId())
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .totalPrice(item.getTotalPrice())
                .build();
    }
}
