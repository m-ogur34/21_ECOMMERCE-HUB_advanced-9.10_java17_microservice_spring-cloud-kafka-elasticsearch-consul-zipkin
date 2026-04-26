package com.ecommerce.order.service;

import com.ecommerce.common.dto.PageResponse;
import com.ecommerce.common.dto.order.OrderRequest;
import com.ecommerce.common.dto.order.OrderResponse;
import com.ecommerce.order.model.OrderStatus;
import org.springframework.data.domain.Pageable;

/** Sipariş servisi arayüzü */
public interface OrderService {

    OrderResponse createOrder(OrderRequest request, Long userId, String userEmail);

    OrderResponse getOrderById(Long id, Long userId);

    OrderResponse getOrderByNumber(String orderNumber);

    PageResponse<OrderResponse> getUserOrders(Long userId, Pageable pageable);

    PageResponse<OrderResponse> getAllOrders(Pageable pageable);

    OrderResponse updateOrderStatus(Long id, OrderStatus newStatus);

    OrderResponse cancelOrder(Long id, Long userId);
}
