package com.ecommerce.order.service;

import com.ecommerce.common.dto.PageResponse;
import com.ecommerce.common.dto.order.OrderRequest;
import com.ecommerce.common.dto.order.OrderResponse;
import com.ecommerce.common.exception.BusinessException;
import com.ecommerce.common.exception.ResourceNotFoundException;
import com.ecommerce.order.mapper.OrderMapper;
import com.ecommerce.order.model.Order;
import com.ecommerce.order.model.OrderStatus;
import com.ecommerce.order.repository.OrderRepository;
import com.ecommerce.order.kafka.OrderEventPublisher;
import com.ecommerce.order.service.saga.OrderSaga;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.CompletableFuture;

/**
 * Sipariş servisi implementasyonu.
 *
 * Resilience4J Pattern'ları:
 *
 * @CircuitBreaker (Devre Kesici):
 * - CLOSED: normal çalışma, istekler geçer
 * - OPEN: hata eşiği aşıldı, istekler reddedilir (fallback çalışır)
 * - HALF-OPEN: deneme istekleri gönderilir
 * Faydası: downstream servis düştüğünde ana servisi de düşürmez.
 *
 * @Retry (Yeniden Deneme):
 * - Geçici hatalarda (network glitch) otomatik tekrar
 * - maxAttempts: kaç kez dene
 * - waitDuration: denemeler arası bekleme
 *
 * @TimeLimiter (Zaman Sınırlayıcı):
 * - Belirlenen süre içinde yanıt gelmezse timeout
 * - Sarkan (hanging) istekleri önler
 * - CompletableFuture gerektirir
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderSaga orderSaga;
    private final OrderMapper orderMapper;
    private final OrderEventPublisher eventPublisher;

    /**
     * Yeni sipariş oluşturur — Saga pattern ile.
     *
     * @CircuitBreaker: Eğer stok servisi veya Kafka defalarca hata verirse
     * devre açılır ve createOrderFallback() çalışır.
     *
     * @Retry: Geçici Kafka bağlantı hatalarında 3 kez tekrar dener.
     */
    @Override
    @CircuitBreaker(name = "orderService", fallbackMethod = "createOrderFallback")
    @Retry(name = "orderService")
    public OrderResponse createOrder(OrderRequest request, Long userId, String userEmail) {
        log.info("Sipariş oluşturma başlatılıyor: kullanıcı={}", userId);

        // Saga orkestratörü — tüm adımları koordine eder
        Order order = orderSaga.execute(request, userId, userEmail);

        log.info("Sipariş başarıyla oluşturuldu: {}", order.getOrderNumber());
        return orderMapper.toResponse(order);
    }

    /**
     * CircuitBreaker fallback metodu.
     * İmzası: aynı parametreler + Throwable
     * CircuitBreaker açıkken veya hata eşiği aşılınca bu metod çalışır.
     */
    public OrderResponse createOrderFallback(OrderRequest request, Long userId,
                                              String userEmail, Throwable throwable) {
        log.error("Sipariş oluşturma devre dışı - fallback çalışıyor: {}", throwable.getMessage());
        throw new BusinessException(
            "Sipariş servisi şu anda kullanılamıyor. Lütfen birkaç dakika sonra tekrar deneyin.",
            "SERVICE_UNAVAILABLE"
        );
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long id, Long userId) {
        Order order = orderRepository.findByIdWithItems(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order", id));

        // Kullanıcı sadece kendi siparişine erişebilir (ADMIN hariç)
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException("Bu siparişe erişim yetkiniz yok", "FORBIDDEN");
        }

        return orderMapper.toResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderByNumber(String orderNumber) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderNumber));
        return orderMapper.toResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> getUserOrders(Long userId, Pageable pageable) {
        Page<OrderResponse> page = orderRepository
                .findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(orderMapper::toResponse);
        return PageResponse.from(page);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> getAllOrders(Pageable pageable) {
        Page<OrderResponse> page = orderRepository.findAll(pageable)
                .map(orderMapper::toResponse);
        return PageResponse.from(page);
    }

    /**
     * Sipariş durumu güncelle — admin işlemi.
     * OrderStatus.canTransitionTo() ile geçersiz geçişler engellenir.
     */
    @Override
    public OrderResponse updateOrderStatus(Long id, OrderStatus newStatus) {
        log.info("Sipariş durumu güncelleniyor: ID={}, yeni durum={}", id, newStatus);

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order", id));

        // State machine kontrolü — Order.transitionTo() içinde
        order.transitionTo(newStatus);
        Order updatedOrder = orderRepository.save(order);

        log.info("Sipariş durumu güncellendi: {} → {}", id, newStatus);
        return orderMapper.toResponse(updatedOrder);
    }

    /** Sipariş iptal et */
    @Override
    public OrderResponse cancelOrder(Long id, Long userId) {
        Order order = orderRepository.findByIdWithItems(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order", id));

        if (!order.getUserId().equals(userId)) {
            throw new BusinessException("Bu siparişi iptal etme yetkiniz yok", "FORBIDDEN");
        }

        if (!order.isCancellable()) {
            throw new BusinessException(
                "Bu sipariş iptal edilemez: " + order.getStatus(),
                "ORDER_NOT_CANCELLABLE"
            );
        }

        order.transitionTo(OrderStatus.CANCELLED);
        orderRepository.save(order);

        // Kafka ile product-service'e stok geri verme sinyali gönder
        eventPublisher.publishStockReleased(order, "Kullanıcı tarafından iptal edildi");

        return orderMapper.toResponse(order);
    }
}
