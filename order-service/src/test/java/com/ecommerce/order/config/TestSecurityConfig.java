package com.ecommerce.order.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Test profili için güvenlik yapılandırması.
 *
 * @TestConfiguration: Sadece test context'ine eklenir, production'ı etkilemez.
 * @Profile("test"): application-test.yml aktifken bu bean yüklenir.
 *
 * JWT filtresi devre dışı bırakılır — entegrasyon testlerinde auth bypass.
 * Güvenlik testi ayrı bir test sınıfında yapılmalı.
 */
@TestConfiguration
@Profile("test")
public class TestSecurityConfig {

    @Bean
    public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth
                .anyRequest().permitAll()  // Tüm isteklere izin ver
            );
        return http.build();
    }
}
