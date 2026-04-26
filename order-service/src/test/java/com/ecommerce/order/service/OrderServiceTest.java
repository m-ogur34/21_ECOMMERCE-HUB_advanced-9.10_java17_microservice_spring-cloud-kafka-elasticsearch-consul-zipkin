package com.ecommerce.order.service;

import com.ecommerce.common.dto.order.OrderItemRequest;
import com.ecommerce.common.dto.order.OrderRequest;
import com.ecommerce.common.dto.order.OrderResponse;
import com.ecommerce.common.exception.BusinessException;
import com.ecommerce.order.mapper.OrderMapper;
import com.ecommerce.order.model.Order;
import com.ecommerce.order.model.OrderStatus;
import com.ecommerce.order.repository.OrderRepository;
import com.ecommerce.order.service.saga.OrderSaga;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderService Unit Testleri")
class OrderServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private OrderSaga orderSaga;
    @Mock private OrderMapper orderMapper;

    @InjectMocks
    private OrderServiceImpl orderService;

    private OrderRequest orderRequest;
    private Order testOrder;
    private OrderResponse orderResponse;

    @BeforeEach
    void setUp() {
        orderRequest = OrderRequest.builder()
                .shippingAddress("Test Sokak No:1")
                .city("İstanbul")
                .postalCode("34000")
                .items(List.of(
                    OrderItemRequest.builder()
                        .productId(1L)
                        .quantity(2)
                        .build()
                ))
                .build();

        testOrder = new Order();
        testOrder.setId(1L);
        testOrder.setOrderNumber("ORD-20240115-ABC123");
        testOrder.setStatus(OrderStatus.PENDING);
        testOrder.setUserId(100L);
        testOrder.setUserEmail("user@test.com");
        testOrder.setTotalAmount(BigDecimal.valueOf(200));

        orderResponse = OrderResponse.builder()
                .id(1L)
                .orderNumber("ORD-20240115-ABC123")
                .status("PENDING")
                .totalAmount(BigDecimal.valueOf(200))
                .build();
    }

    @Test
    @DisplayName("Geçerli istek ile sipariş başarıyla oluşturulur")
    void createOrder_WhenValidRequest_ShouldReturnOrderResponse() {
        // GIVEN
        when(orderSaga.execute(any(), anyLong(), anyString())).thenReturn(testOrder);
        when(orderMapper.toResponse(testOrder)).thenReturn(orderResponse);

        // WHEN
        OrderResponse result = orderService.createOrder(orderRequest, 100L, "user@test.com");

        // THEN
        assertThat(result).isNotNull();
        assertThat(result.getOrderNumber()).isEqualTo("ORD-20240115-ABC123");
        verify(orderSaga).execute(orderRequest, 100L, "user@test.com");
    }

    @Test
    @DisplayName("Teslim edilmiş sipariş iptal edilemez")
    void cancelOrder_WhenDelivered_ShouldThrowBusinessException() {
        // GIVEN
        testOrder.setStatus(OrderStatus.DELIVERED); // Terminal durum
        when(orderRepository.findByIdWithItems(1L)).thenReturn(Optional.of(testOrder));

        // WHEN & THEN
        assertThatThrownBy(() -> orderService.cancelOrder(1L, 100L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("iptal edilemez");
    }

    @Test
    @DisplayName("Geçersiz durum geçişi BusinessException fırlatır")
    void updateOrderStatus_WhenInvalidTransition_ShouldThrowException() {
        // GIVEN: DELIVERED durumundan PENDING'e geçilemez
        testOrder.setStatus(OrderStatus.DELIVERED);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

        // WHEN & THEN
        assertThatThrownBy(() -> orderService.updateOrderStatus(1L, OrderStatus.PENDING))
                .isInstanceOf(BusinessException.class);
    }
}
