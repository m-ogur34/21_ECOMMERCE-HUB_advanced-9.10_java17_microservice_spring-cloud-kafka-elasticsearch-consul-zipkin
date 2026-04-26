package com.ecommerce.auth.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * CORS (Cross-Origin Resource Sharing) yapılandırması.
 *
 * CORS nedir?
 * Tarayıcı güvenlik politikası: farklı origin'den (domain/port) gelen istekleri varsayılan
 * olarak engeller. Örneğin Angular uygulaması localhost:4200'de çalışırken
 * Spring Boot API'si localhost:8081'de çalışır — farklı port = farklı origin.
 * CORS bu kısıtlamayı açıkça izin vererek aşar.
 *
 * NOT: API Gateway seviyesinde de CORS yapılandırılmıştır.
 * Bu yapılandırma, servise doğrudan erişim (Gateway olmadan) durumlar için.
 */
@Configuration
public class CorsConfig {

    @Value("${cors.allowed-origins:http://localhost:4200}")
    private String allowedOrigins;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // Hangi origin'lerden istek kabul edilir
        config.setAllowedOriginPatterns(List.of(allowedOrigins.split(",")));

        // İzin verilen HTTP metodları
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));

        // İzin verilen header'lar
        config.setAllowedHeaders(List.of("*")); // Tüm header'lara izin ver

        // Tarayıcının görebileceği header'lar (örn: Authorization yanıt header'ı)
        config.setExposedHeaders(List.of("Authorization", "Content-Type"));

        // Cookie/Credentials izni — Authorization header için gerekli
        config.setAllowCredentials(true);

        // Preflight (OPTIONS) isteği sonucunun ne kadar cache'leneceği (saniye)
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config); // Tüm path'lere uygula
        return source;
    }
}
