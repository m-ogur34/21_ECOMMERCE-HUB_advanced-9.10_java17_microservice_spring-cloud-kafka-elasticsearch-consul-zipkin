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
 * Sipariş Servisi Implementasyonu
 * =================================
 *
 * OOP - Polimorfizm: Controller OrderService arayüzünü kullanır.
 *   Spring IoC bu implementasyonu inject eder.
 *   Test ortamında mock implementasyon kullanılabilir — controller değişmez.
 *
 * Resilience4J — Hata Dayanıklılık Katmanı:
 *
 *   @CircuitBreaker (Devre Kesici):
 *     Elektrik sigortasına benzer — devre yanmasın diye önce sigorta atar.
 *     CLOSED → normal çalışma (istekler geçer)
 *     %50+ hata sonra OPEN → fallback çalışır, downstream'e istek gitmez
 *     Belirli süre sonra HALF-OPEN → deneme isteği gönderir
 *     HALF-OPEN başarılı → CLOSED'a döner
 *     Neden önemli? Stok servisi düştüğünde sipariş servisi de çökmeden fallback döner.
 *
 *   @Retry (Yeniden Deneme):
 *     Ağ geçici kesintisinde (500ms timeout) aynı isteği 3 kez tekrar eder.
 *     Her deneme arası bekleme süresi artabilir (exponential backoff).
 *     Sadece belirli exception'larda retry yapılır (konfigürasyonda tanımlı).
 *
 *   @TimeLimiter (Zaman Sınırlayıcı):
 *     Stok servisi 5sn içinde yanıt vermezse timeout.
 *     CompletableFuture gerektirir — asenkron timeout mekanizması.
 *     Sarkan (hanging) HTTP bağlantıları thread pool'u tüketmez.
 *
 * Saga Pattern:
 *   Sipariş oluşturma çok adımlıdır: DB kaydı + stok rezervasyonu.
 *   Stok rezervasyonu başarısız → DB kaydı da geri alınmalı.
 *   Saga: Her adımın "compensating transaction" (telafi) metodu vardır.
 *   Hata durumunda tamamlanan adımlar ters sırada geri alınır.
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
     * Yeni sipariş oluşturur.
     *
     * @CircuitBreaker(name="orderService", fallbackMethod="createOrderFallback"):
     *   Hata eşiği aşılınca createOrderFallback() çağrılır.
     *   Stok servisi veya Kafka defalarca hata verirse devre açılır.
     *   Fallback: Kullanıcıya "şu an kullanılamıyor" mesajı — boş yanıt değil.
     *
     * @Retry(name="orderService"):
     *   Geçici ağ hatalarında otomatik tekrar.
     *   3 deneme, her deneme arası 500ms bekleme (application.yml'de konfigüre).
     *   Idempotency önemli: Aynı sipariş iki kez DB'ye yazılmamalı.
     *   OrderSaga: Önce idempotency key kontrolü yaparak bunu önler.
     *
     * Saga Akışı:
     *   Adım 1: OrderService DB'ye PENDING sipariş yazar.
     *   Adım 2: product-service'e stok rezervasyon isteği atar (Feign/RestTemplate).
     *   Stok yetersiz → Adım 1 compensate: sipariş CANCELLED olarak güncellenir.
     *   Her iki adım başarılı → Kafka'ya ORDER_CREATED event atılır.
     */
    @Override
    @CircuitBreaker(name = "orderService", fallbackMethod = "createOrderFallback")
    @Retry(name = "orderService")
    public OrderResponse createOrder(OrderRequest request, Long userId, String userEmail) {
        log.info("Sipariş oluşturma başlatılıyor: kullanıcı={}", userId);

        // Saga: Başarısız adımlarda compensating transaction çalışır
        Order order = orderSaga.execute(request, userId, userEmail);

        log.info("Sipariş başarıyla oluşturuldu: {}", order.getOrderNumber());
        return orderMapper.toResponse(order);
    }

    /**
     * CircuitBreaker fallback metodu.
     *
     * Kural: Fallback metod imzası = orijinal metod imzası + son parametre Throwable.
     * CircuitBreaker açıkken (OPEN state) bu metod doğrudan çağrılır.
     * Hata yutulmamalı — kullanıcıya anlamlı hata dönülmeli.
     */
    public OrderResponse createOrderFallback(OrderRequest request, Long userId,
                                              String userEmail, Throwable throwable) {
        log.error("CircuitBreaker AÇIK - Sipariş servisi fallback çalışıyor: {}", throwable.getMessage());
        throw new BusinessException(
            "Sipariş servisi şu anda kullanılamıyor. Lütfen birkaç dakika sonra tekrar deneyin.",
            "SERVICE_UNAVAILABLE"
        );
    }

    /**
     * Sipariş detayını getirir.
     *
     * Neden sahiplik kontrolü yapılır?
     *   URL'de /orders/123 — manipülasyonla başkasının siparişi görülebilir.
     *   JWT'den gelen userId ile siparişin userId'si karşılaştırılır.
     *   ADMIN rolü kontrolü controller'daki @PreAuthorize ile yapılabilir.
     *
     * findByIdWithItems: @EntityGraph veya JOIN FETCH
     *   items lazy loading yerine tek sorguda gelir — N+1 problemi olmaz.
     */
    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long id, Long userId) {
        Order order = orderRepository.findByIdWithItems(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order", id));

        // Güvenlik: Kullanıcı yalnızca kendi siparişine erişebilir
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
     * Sipariş durumu günceller — State Machine pattern.
     *
     * State Machine (Durum Makinesi):
     *   Her sipariş belirli durumlar arasında geçiş yapabilir.
     *   Geçersiz geçişler (DELIVERED → PENDING) engellenmelidir.
     *
     *   Geçerli geçişler:
     *   PENDING → CONFIRMED → PROCESSING → SHIPPED → DELIVERED
     *   PENDING/CONFIRMED/PROCESSING → CANCELLED
     *   DELIVERED → REFUNDED
     *
     *   order.transitionTo(newStatus): Geçiş kurallarını kontrol eder.
     *   Geçersiz geçiş → IllegalStateException fırlatır.
     *   Bu sayede sipariş durumu dışarıdan rastgele değiştirilemez.
     */
    @Override
    public OrderResponse updateOrderStatus(Long id, OrderStatus newStatus) {
        log.info("Sipariş durumu güncelleniyor: ID={}, yeni durum={}", id, newStatus);

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order", id));

        // State machine: Geçersiz geçişte exception fırlatır (örn: DELIVERED → PENDING)
        order.transitionTo(newStatus);
        Order updatedOrder = orderRepository.save(order);

        log.info("Sipariş durumu güncellendi: {} → {}", id, newStatus);
        return orderMapper.toResponse(updatedOrder);
    }

    /**
     * Siparişi iptal eder.
     *
     * Neden iptal edilebilirlik kontrolü gerekli?
     *   Kargo verilmiş (SHIPPED) veya teslim edilmiş (DELIVERED) siparişler iptal edilemez.
     *   order.isCancellable(): PENDING/CONFIRMED/PROCESSING durumları için true.
     *
     * Stok geri verme:
     *   Sipariş oluşturulurken stok rezerve edilmişti (product-service).
     *   İptal edilince stok serbest bırakılmalı.
     *   Kafka event: product-service bu eventi dinler, stoku artırır.
     *   Neden senkron değil Kafka?
     *     product-service geçici olarak düşebilir.
     *     Kafka: Event kalıcı, product-service ayağa kalkınca işler.
     *     Senkron: product-service düştüyse iptal de başarısız olur — kötü UX.
     */
    @Override
    public OrderResponse cancelOrder(Long id, Long userId) {
        Order order = orderRepository.findByIdWithItems(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order", id));

        // Güvenlik: Kullanıcı yalnızca kendi siparişini iptal edebilir
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException("Bu siparişi iptal etme yetkiniz yok", "FORBIDDEN");
        }

        // Durum kontrolü: SHIPPED/DELIVERED → iptal edilemez
        if (!order.isCancellable()) {
            throw new BusinessException(
                "Bu sipariş iptal edilemez. Mevcut durum: " + order.getStatus(),
                "ORDER_NOT_CANCELLABLE"
            );
        }

        order.transitionTo(OrderStatus.CANCELLED);
        orderRepository.save(order);

        // Kafka: product-service stoku geri verir (eventual consistency)
        eventPublisher.publishStockReleased(order, "Kullanıcı tarafından iptal edildi");

        return orderMapper.toResponse(order);
    }
}
