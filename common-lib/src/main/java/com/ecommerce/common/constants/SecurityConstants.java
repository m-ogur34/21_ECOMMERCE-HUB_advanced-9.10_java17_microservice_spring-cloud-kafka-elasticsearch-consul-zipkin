package com.ecommerce.common.constants;

/**
 * Güvenlik ile ilgili sabit değerleri tutan sınıf.
 *
 * JWT token yapısı:
 * - Header: algoritma bilgisi (HS256)
 * - Payload: kullanıcı bilgileri (claims)
 * - Signature: gizli anahtar ile imzalanmış hash
 *
 * Token akışı:
 * 1. Kullanıcı giriş yapar → access token (15dk) + refresh token (7gün) alır
 * 2. Her istekte Authorization: Bearer <access_token> header'ı gönderir
 * 3. Access token süresi dolunca refresh token ile yeni access token alır
 * 4. Refresh token da süresi dolarsa kullanıcı yeniden giriş yapar
 */
public final class SecurityConstants {

    private SecurityConstants() {
        throw new UnsupportedOperationException("Bu sınıf örneklenemez");
    }

    // ===== TOKEN SÜRELERİ =====

    /** Access token geçerlilik süresi: 15 dakika (milisaniye) */
    public static final long ACCESS_TOKEN_EXPIRATION = 15 * 60 * 1000L; // 900_000 ms

    /** Refresh token geçerlilik süresi: 7 gün (milisaniye) */
    public static final long REFRESH_TOKEN_EXPIRATION = 7 * 24 * 60 * 60 * 1000L; // 604_800_000 ms

    // ===== HTTP HEADER =====

    /** JWT token'ı taşıyan HTTP başlığı adı */
    public static final String AUTHORIZATION_HEADER = "Authorization";

    /** Bearer token prefix — "Bearer " + token şeklinde kullanılır */
    public static final String BEARER_PREFIX = "Bearer ";

    /** Bearer prefix uzunluğu — substring için kullanılır */
    public static final int BEARER_PREFIX_LENGTH = 7;

    // ===== JWT CLAIM ANAHTARLARI =====

    /** Token payload'ında rol listesinin tutulduğu claim adı */
    public static final String ROLES_CLAIM = "roles";

    /** Token payload'ında kullanıcı ID'sinin tutulduğu claim adı */
    public static final String USER_ID_CLAIM = "userId";

    // ===== ROL İSİMLERİ =====

    /** Standart kullanıcı rolü — alışveriş yapabilir */
    public static final String ROLE_USER = "ROLE_USER";

    /** Yönetici rolü — ürün ekleyebilir, silebilir, siparişleri yönetebilir */
    public static final String ROLE_ADMIN = "ROLE_ADMIN";

    /** Moderatör rolü — içerik denetimi yapabilir */
    public static final String ROLE_MODERATOR = "ROLE_MODERATOR";

    // ===== GÜVENLİK DIŞI TUTULACAK URL'LER =====

    /** Bu endpoint'ler JWT doğrulaması gerektirmez (public erişim) */
    public static final String[] PUBLIC_URLS = {
        "/api/v1/auth/**",        // Giriş ve kayıt endpoint'leri
        "/api/v1/products/search/**", // Ürün arama (herkes görebilir)
        "/api/v1/products",       // Ürün listesi (herkes görebilir)
        "/api/v1/products/{id}",  // Ürün detayı (herkes görebilir)
        "/actuator/health",       // Kubernetes health probe
        "/actuator/info",         // Uygulama bilgisi
        "/v3/api-docs/**",        // Swagger UI
        "/swagger-ui/**"          // Swagger UI
    };
}
