package com.ecommerce.auth.controller;

import com.ecommerce.auth.service.AuthService;
import com.ecommerce.common.dto.ApiResponse;
import com.ecommerce.common.dto.auth.AuthRequest;
import com.ecommerce.common.dto.auth.AuthResponse;
import com.ecommerce.common.dto.auth.RegisterRequest;
import com.ecommerce.common.dto.auth.TokenRefreshRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Kimlik doğrulama REST API controller'ı.
 *
 * @RestController = @Controller + @ResponseBody
 * Metodların dönüş değerleri otomatik olarak JSON'a dönüştürülür.
 *
 * API versiyonlama: /api/v1/ prefix'i kullanıyoruz.
 * Gelecekte breaking change'ler için /api/v2/ eklenebilir — mevcut istemciler etkilenmez.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    // AuthService arayüzüne bağlı — somut implementasyona değil (DIP)
    private final AuthService authService;

    /**
     * POST /api/v1/auth/register
     * Yeni kullanıcı kaydı.
     *
     * @Valid: RegisterRequest'teki validation annotasyonlarını tetikler.
     * Validation başarısız olursa GlobalExceptionHandler 400 döner.
     * HTTP 201 Created: yeni kaynak oluşturulduğunda standart kod.
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request) {

        log.info("Kayıt isteği alındı: {}", request.getEmail());
        AuthResponse authResponse = authService.register(request);

        return ResponseEntity
                .status(HttpStatus.CREATED) // 201
                .body(ApiResponse.success(authResponse, "Kayıt başarılı"));
    }

    /**
     * POST /api/v1/auth/login
     * Kullanıcı girişi.
     * HTTP 200 OK döner — kaynak oluşturulmadı, sadece token üretildi.
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody AuthRequest request) {

        log.info("Giriş isteği alındı: {}", request.getEmail());
        AuthResponse authResponse = authService.login(request);

        return ResponseEntity.ok(ApiResponse.success(authResponse, "Giriş başarılı"));
    }

    /**
     * POST /api/v1/auth/refresh-token
     * Access token yenileme.
     */
    @PostMapping("/refresh-token")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(
            @Valid @RequestBody TokenRefreshRequest request) {

        AuthResponse authResponse = authService.refreshToken(request);
        return ResponseEntity.ok(ApiResponse.success(authResponse, "Token yenilendi"));
    }

    /**
     * POST /api/v1/auth/logout
     * Kullanıcı çıkışı — refresh token'ı geçersiz kılar.
     * HTTP 200 döner — body'de veri yok (ApiResponse<Void>).
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestBody TokenRefreshRequest request) {

        authService.logout(request.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.success("Çıkış başarılı"));
    }

    /**
     * GET /api/v1/auth/health
     * Servis sağlık kontrolü — Kubernetes liveness probe'u için.
     */
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<String>> health() {
        return ResponseEntity.ok(ApiResponse.success("OK", "Auth servisi çalışıyor"));
    }
}
