package com.ecommerce.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * CORS yapılandırması — WebFlux uyumlu (reaktif).
 *
 * Gateway seviyesinde CORS: her servis kendi CORS'unu yönetmek zorunda kalmaz.
 * Tüm servisler gateway üzerinden erişildiği için gateway'de merkezi CORS yeterlidir.
 *
 * NOT: CorsWebFilter, Servlet CorsConfigurationSource'dan farklı.
 * WebFlux: reactive → UrlBasedCorsConfigurationSource (reactive versiyonu)
 * Servlet: blocking → org.springframework.web.cors.UrlBasedCorsConfigurationSource
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration config = new CorsConfiguration();

        // İzin verilen origin'ler — production'da spesifik domain girilmeli
        config.setAllowedOriginPatterns(List.of(
                "http://localhost:4200",     // Angular geliştirme sunucusu
                "http://localhost:3000",     // React geliştirme sunucusu (alternatif)
                "https://*.ecommercehub.com" // Production domain
        ));

        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Authorization", "X-Total-Count", "X-Gateway-Service"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return new CorsWebFilter(source);
    }
}
