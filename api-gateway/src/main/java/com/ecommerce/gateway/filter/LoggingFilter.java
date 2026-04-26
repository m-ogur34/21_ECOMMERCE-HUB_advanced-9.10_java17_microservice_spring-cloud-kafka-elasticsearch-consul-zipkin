package com.ecommerce.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Tüm istekleri loglayan global filtre.
 *
 * GlobalFilter: tüm route'lara uygulanır (JwtGatewayFilter gibi seçici değil).
 * Ordered: filtre öncelik sırası — düşük değer = önce çalışır.
 *
 * then() operatörü: reaktif zincir — istek işlendikten SONRA yanıtı logla.
 * Mono.fromRunnable(): non-blocking kod bloğunu Mono'ya sarar.
 */
@Slf4j
@Component
public class LoggingFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        long startTime = System.currentTimeMillis();

        // İstek log'u
        log.info("→ {} {} | IP: {} | TraceId: {}",
                request.getMethod(),
                request.getPath().value(),
                getClientIp(request),
                request.getHeaders().getFirst("X-B3-TraceId"));

        return chain.filter(exchange)
                .then(Mono.fromRunnable(() -> {
                    // Yanıt log'u — istek tamamlandıktan sonra
                    ServerHttpResponse response = exchange.getResponse();
                    long duration = System.currentTimeMillis() - startTime;

                    log.info("← {} {} | Durum: {} | Süre: {}ms",
                            request.getMethod(),
                            request.getPath().value(),
                            response.getStatusCode(),
                            duration);
                }));
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE - 10; // En son çalışır (yanıt sonrası loglama için)
    }

    /** İstemci IP adresini alır — proxy arkasında X-Forwarded-For header'ını kontrol eder */
    private String getClientIp(ServerHttpRequest request) {
        String forwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isEmpty()) {
            return forwardedFor.split(",")[0].trim(); // İlk IP gerçek istemci
        }
        if (request.getRemoteAddress() != null) {
            return request.getRemoteAddress().getAddress().getHostAddress();
        }
        return "unknown";
    }
}
