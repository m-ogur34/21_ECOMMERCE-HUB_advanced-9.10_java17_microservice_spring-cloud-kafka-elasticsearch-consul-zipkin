package com.ecommerce.product.config;

import com.ecommerce.common.constants.SecurityConstants;
import com.ecommerce.product.filter.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Product Service güvenlik yapılandırması.
 *
 * Auth-service'den farkı: Bu servis token ÜRETMEZ, sadece DOĞRULAR.
 * JWT token'ı auth-service üretir; product-service token'ı parse ederek
 * kullanıcının kim olduğunu ve rollerini anlar.
 *
 * Bu yaklaşım Resource Server modelidir: her servis kendi JWT doğrulamasını yapar.
 * Alternatif: API Gateway tüm doğrulamayı yapar, servisler güvenir — daha merkezi.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Ürün listeleme ve arama herkese açık
                .requestMatchers(HttpMethod.GET, "/api/v1/products/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/categories/**").permitAll()
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                // Diğerleri kimlik doğrulama gerektirir (rol kontrolü @PreAuthorize ile)
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
