package com.ecommerce.order.controller;

import com.ecommerce.common.dto.ApiResponse;
import com.ecommerce.common.dto.PageResponse;
import com.ecommerce.common.dto.order.OrderRequest;
import com.ecommerce.common.dto.order.OrderResponse;
import com.ecommerce.order.model.OrderStatus;
import com.ecommerce.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * Sipariş REST API controller'ı.
 *
 * @AuthenticationPrincipal: SecurityContext'teki kimlik doğrulanmış kullanıcıyı inject eder.
 * JWT filtresinde set edilen kullanıcı bilgisini controller'da kullanmak için.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /**
     * POST /api/v1/orders
     * Yeni sipariş oluştur.
     * Kullanıcı bilgisi JWT'den alınır — istemci userId göndermez (güvenli).
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
            @Valid @RequestBody OrderRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        // JWT'den kullanıcı e-postasını al
        // Gerçek uygulamada userId de token'dan alınır (UserIdClaim)
        String userEmail = userDetails.getUsername();
        Long userId = extractUserIdFromToken(userDetails);

        log.info("Sipariş isteği: kullanıcı={}", userEmail);
        OrderResponse order = orderService.createOrder(request, userId, userEmail);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(order, "Sipariş başarıyla oluşturuldu"));
    }

    /**
     * GET /api/v1/orders/{id}
     * Sipariş detayı — sadece sahibi veya admin görebilir.
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = extractUserIdFromToken(userDetails);
        OrderResponse order = orderService.getOrderById(id, userId);
        return ResponseEntity.ok(ApiResponse.success(order, "Sipariş getirildi"));
    }

    /**
     * GET /api/v1/orders/number/{orderNumber}
     * Sipariş numarasıyla arama — müşteri destek için.
     */
    @GetMapping("/number/{orderNumber}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderByNumber(
            @PathVariable String orderNumber) {

        OrderResponse order = orderService.getOrderByNumber(orderNumber);
        return ResponseEntity.ok(ApiResponse.success(order, "Sipariş getirildi"));
    }

    /**
     * GET /api/v1/orders/my-orders
     * Giriş yapmış kullanıcının siparişleri.
     */
    @GetMapping("/my-orders")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PageResponse<OrderResponse>>> getMyOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = extractUserIdFromToken(userDetails);
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        PageResponse<OrderResponse> orders = orderService.getUserOrders(userId, pageable);
        return ResponseEntity.ok(ApiResponse.success(orders, "Siparişler getirildi"));
    }

    /**
     * GET /api/v1/orders
     * Tüm siparişler — sadece ADMIN.
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<OrderResponse>>> getAllOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        PageResponse<OrderResponse> orders = orderService.getAllOrders(pageable);
        return ResponseEntity.ok(ApiResponse.success(orders, "Tüm siparişler getirildi"));
    }

    /**
     * PATCH /api/v1/orders/{id}/status
     * Sipariş durumu güncelle — sadece ADMIN.
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<OrderResponse>> updateStatus(
            @PathVariable Long id,
            @RequestParam OrderStatus status) {

        OrderResponse order = orderService.updateOrderStatus(id, status);
        return ResponseEntity.ok(ApiResponse.success(order, "Sipariş durumu güncellendi"));
    }

    /**
     * PATCH /api/orders/{id}/cancel
     * Sipariş iptal et — kullanıcı kendi siparişini iptal edebilir.
     * DELETE değil PATCH: kaynağı silmiyoruz, durumunu değiştiriyoruz.
     */
    @PatchMapping("/{id}/cancel")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<OrderResponse>> cancelOrder(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = extractUserIdFromToken(userDetails);
        OrderResponse order = orderService.cancelOrder(id, userId);
        return ResponseEntity.ok(ApiResponse.success(order, "Sipariş iptal edildi"));
    }

    /**
     * JWT token'daki userId claim'ini çıkarır.
     * Gerçek uygulamada: CustomUserDetails.getUserId() ile alınır.
     * Bu örnekte basitlik için e-postadan türetilmiş sahte ID kullanılıyor.
     */
    private Long extractUserIdFromToken(UserDetails userDetails) {
        // NOT: Gerçek uygulamada JWT filter'da set edilen CustomUserDetails'ten alınır
        // Şimdilik email hashCode'unu userId olarak kullanıyoruz
        return (long) Math.abs(userDetails.getUsername().hashCode());
    }
}
