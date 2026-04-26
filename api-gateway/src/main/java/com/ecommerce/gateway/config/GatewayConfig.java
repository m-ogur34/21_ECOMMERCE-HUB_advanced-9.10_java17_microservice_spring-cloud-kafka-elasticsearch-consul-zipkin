package com.ecommerce.gateway.config;

import com.ecommerce.gateway.filter.JwtGatewayFilter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * API Gateway route (yönlendirme) yapılandırması.
 *
 * Route tanımı iki yöntemle yapılabilir:
 * 1. Programatik (bu sınıf): Java kodu — daha esnek, IDE desteği tam
 * 2. Deklaratif (application.yml): YAML — daha kısa, ortam değişkeniyle override edilir
 *
 * lb://auth-service: load balanced URL
 * lb:// prefix'i → Spring Cloud LoadBalancer devreye girer
 * auth-service → Consul'dan bu isimle kayıtlı instance'ları bulur → round-robin dağıtır
 *
 * Filtreler:
 * - JwtGatewayFilter: token doğrulama
 * - RewritePath: URL yeniden yazma
 * - RequestRateLimiter: rate limiting
 * - CircuitBreaker: hata toleransı
 */
@Configuration
public class GatewayConfig {

    private final JwtGatewayFilter jwtFilter;

    public GatewayConfig(JwtGatewayFilter jwtFilter) {
        this.jwtFilter = jwtFilter;
    }

    @Bean
    public RouteLocator routeLocator(RouteLocatorBuilder builder) {
        return builder.routes()

                // ===== AUTH SERVICE =====
                // /api/v1/auth/** → auth-service (JWT filtre uygulanmaz — login/register public)
                .route("auth-service", route -> route
                        .path("/api/v1/auth/**")  // Bu path ile eşleşen istekler
                        .filters(f -> f
                                .filter(jwtFilter.apply(new JwtGatewayFilter.Config()))
                                // Yanıt header'ına servis adı ekle (debug için)
                                .addResponseHeader("X-Gateway-Service", "auth-service")
                        )
                        .uri("lb://auth-service")  // Consul'dan auth-service bul
                )

                // ===== PRODUCT SERVICE =====
                .route("product-service", route -> route
                        .path("/api/v1/products/**", "/api/v1/categories/**")
                        .filters(f -> f
                                .filter(jwtFilter.apply(new JwtGatewayFilter.Config()))
                                .addResponseHeader("X-Gateway-Service", "product-service")
                                // Rate limiting: Redis tabanlı token bucket
                                .requestRateLimiter(config -> config
                                        .setRateLimiter(redisRateLimiter())
                                        // Key: IP adresi veya kullanıcı e-postası
                                        .setKeyResolver(exchange -> {
                                            String userEmail = exchange.getRequest()
                                                    .getHeaders().getFirst("X-User-Email");
                                            return reactor.core.publisher.Mono.just(
                                                userEmail != null ? userEmail : "anonymous"
                                            );
                                        })
                                )
                        )
                        .uri("lb://product-service")
                )

                // ===== ORDER SERVICE =====
                .route("order-service", route -> route
                        .path("/api/v1/orders/**")
                        .filters(f -> f
                                .filter(jwtFilter.apply(new JwtGatewayFilter.Config()))
                                .addResponseHeader("X-Gateway-Service", "order-service")
                        )
                        .uri("lb://order-service")
                )

                // ===== ACTUATOR (SAĞLIK KONTROL) =====
                // Tüm servislerin health endpoint'leri gateway üzerinden erişilebilir
                .route("actuator", route -> route
                        .path("/actuator/**")
                        .uri("lb://auth-service") // Varsayılan auth-service
                )

                .build();
    }

    /**
     * Redis tabanlı rate limiter.
     * Token bucket algoritması:
     * - replenishRate: saniyede eklenen token sayısı (ortalama istek hızı)
     * - burstCapacity: anlık maksimum token (spike toleransı)
     * - requestedTokens: her istek için tüketilen token sayısı
     */
    @Bean
    public org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter redisRateLimiter() {
        return new org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter(
                10,  // replenishRate: saniyede 10 token
                20,  // burstCapacity: anlık max 20 token
                1    // requestedTokens: her istek 1 token tüketir
        );
    }
}
