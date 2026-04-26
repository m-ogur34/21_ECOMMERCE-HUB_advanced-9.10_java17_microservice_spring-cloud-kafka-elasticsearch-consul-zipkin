package com.ecommerce.gateway.filter;

import com.ecommerce.common.constants.SecurityConstants;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.security.Key;
import java.util.Arrays;
import java.util.List;

/**
 * API Gateway JWT doğrulama filtresi.
 *
 * Spring Cloud Gateway WebFlux tabanlıdır — reaktif programlama modeli kullanır.
 * Bu nedenle HttpServletRequest değil ServerHttpRequest kullanılır.
 * Mono<Void>: reaktif publisher — asenkron işlemi temsil eder.
 *
 * GatewayFilter: her route için uygulanabilen filtre.
 * AbstractGatewayFilterFactory: factory deseni — route'a özel konfigürasyon sağlar.
 *
 * Filtre akışı:
 * İstemci → Gateway → [JwtFilter] → Auth/Product/Order Service
 *
 * Public endpoint'ler filtreyi atlar (WHITELISTED_PATHS).
 * Token geçerliyse istek downstream servise iletilir.
 * Geçersiz token: 401 Unauthorized döner, servise ulaşmaz.
 */
@Slf4j
@Component
public class JwtGatewayFilter extends AbstractGatewayFilterFactory<JwtGatewayFilter.Config> {

    @Value("${jwt.secret}")
    private String secretKey;

    /** JWT doğrulama gerektirmeyen public endpoint'ler */
    private static final List<String> WHITELISTED_PATHS = Arrays.asList(
            "/api/v1/auth/login",
            "/api/v1/auth/register",
            "/api/v1/auth/refresh-token",
            "/api/v1/products",         // GET ürün listesi
            "/actuator/health",
            "/actuator/info"
    );

    public JwtGatewayFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String path = request.getPath().value();

            // Public path'ler için filtre atla
            if (isWhitelisted(path)) {
                return chain.filter(exchange);
            }

            // GET /api/v1/products/** herkese açık
            if (request.getMethod().name().equals("GET") &&
                path.startsWith("/api/v1/products")) {
                return chain.filter(exchange);
            }

            // Authorization header var mı?
            if (!request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
                return onError(exchange, "Authorization header eksik", HttpStatus.UNAUTHORIZED);
            }

            String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

            if (authHeader == null || !authHeader.startsWith(SecurityConstants.BEARER_PREFIX)) {
                return onError(exchange, "Geçersiz token formatı", HttpStatus.UNAUTHORIZED);
            }

            String token = authHeader.substring(SecurityConstants.BEARER_PREFIX_LENGTH);

            try {
                // Token doğrula
                Claims claims = validateToken(token);

                // Kullanıcı bilgisini downstream servislerine header olarak ekle
                // Böylece her servis token'ı tekrar parse etmek zorunda kalmaz
                ServerHttpRequest modifiedRequest = request.mutate()
                        .header("X-User-Email", claims.getSubject())
                        .header("X-User-Id", String.valueOf(claims.get("userId", Long.class) != null
                                ? claims.get("userId", Long.class) : ""))
                        .build();

                return chain.filter(exchange.mutate().request(modifiedRequest).build());

            } catch (ExpiredJwtException e) {
                log.warn("Süresi dolmuş token: {}", e.getMessage());
                return onError(exchange, "Token süresi dolmuş", HttpStatus.UNAUTHORIZED);
            } catch (Exception e) {
                log.warn("Geçersiz token: {}", e.getMessage());
                return onError(exchange, "Geçersiz token", HttpStatus.UNAUTHORIZED);
            }
        };
    }

    /** Token'ı doğrula ve claims döner */
    private Claims validateToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /** Path whitelist kontrolü — prefix veya tam eşleşme */
    private boolean isWhitelisted(String path) {
        return WHITELISTED_PATHS.stream().anyMatch(path::startsWith);
    }

    /**
     * Hata yanıtı döner — reaktif Mono ile.
     * ServerHttpResponse: WebFlux yanıt nesnesi.
     * setComplete(): yanıt gövdesi boş, sadece status kodu.
     */
    private Mono<Void> onError(ServerWebExchange exchange, String message, HttpStatus status) {
        log.warn("Gateway auth hatası: {} - {}", status, message);
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        return response.setComplete();
    }

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretKey));
    }

    /** Filter yapılandırma sınıfı — şu an ek config yok */
    public static class Config {
        // Gelecekte: exclude paths, custom error messages vb.
    }
}
