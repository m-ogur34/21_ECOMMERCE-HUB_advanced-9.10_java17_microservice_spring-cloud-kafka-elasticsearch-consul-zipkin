package com.ecommerce.auth.config;

import com.ecommerce.auth.filter.JwtAuthenticationFilter;
import com.ecommerce.auth.service.UserDetailsServiceImpl;
import com.ecommerce.common.constants.SecurityConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security yapılandırma sınıfı.
 *
 * Spring Security 6.x ile gelen lambda DSL yaklaşımı kullanılır.
 * Eski WebSecurityConfigurerAdapter extend etme yöntemi artık deprecated.
 *
 * Temel kavramlar:
 * - SecurityFilterChain: HTTP istekleri için güvenlik kuralları zinciri
 * - AuthenticationProvider: kimlik doğrulama stratejisi
 * - PasswordEncoder: şifre hashleme algoritması
 * - AuthenticationManager: provider'ları koordine eden yönetici
 */
@Configuration
@EnableWebSecurity          // Spring Security'yi aktifleştirir
@EnableMethodSecurity       // @PreAuthorize, @PostAuthorize method level güvenlik
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final UserDetailsServiceImpl userDetailsService;

    /**
     * Ana güvenlik filtre zinciri.
     *
     * Konfigürasyon sırası önemlidir:
     * 1. CSRF → devre dışı (stateless JWT için gerekli değil)
     * 2. CORS → etkin
     * 3. Session → STATELESS (JWT kullandığımız için session tutmuyoruz)
     * 4. Authorization → hangi URL'e kim erişebilir
     * 5. Authentication → JWT filtresini ekle
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // CSRF (Cross-Site Request Forgery) koruması: REST API + JWT için gerekli değil.
            // CSRF session cookie'ye dayalı saldırıyı önler; biz cookie kullanmıyoruz.
            .csrf(AbstractHttpConfigurer::disable)

            // Session yönetimi: STATELESS — Spring Security SecurityContext'i session'da saklamaz.
            // Her istek kendi JWT'siyle kimliğini kanıtlar.
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            // URL bazlı yetkilendirme kuralları
            .authorizeHttpRequests(auth -> auth
                // Public endpoint'ler — token gerektirmez
                .requestMatchers(SecurityConstants.PUBLIC_URLS).permitAll()

                // GET ürün endpoint'leri herkese açık
                .requestMatchers(HttpMethod.GET, "/api/v1/products/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/categories/**").permitAll()

                // Ürün ekleme/silme sadece admin yapabilir
                .requestMatchers(HttpMethod.POST, "/api/v1/products/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/v1/products/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/v1/products/**").hasRole("ADMIN")

                // Geri kalan tüm endpoint'ler giriş gerektirir
                .anyRequest().authenticated()
            )

            // Özel kimlik doğrulama sağlayıcısı
            .authenticationProvider(authenticationProvider())

            // JWT filtresini, Spring'in varsayılan username/password filtresinden ÖNCE ekle.
            // Bu sayede her istekte JWT kontrolü yapılır.
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * DaoAuthenticationProvider: veritabanı tabanlı kimlik doğrulama sağlayıcısı.
     *
     * Akış:
     * 1. UserDetailsService.loadUserByUsername() → DB'den kullanıcı yükler
     * 2. PasswordEncoder.matches() → hash karşılaştırması yapar
     * 3. Kullanıcı aktif/kilitli değilse authentication başarılı
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService); // Kullanıcı yükleme stratejisi
        provider.setPasswordEncoder(passwordEncoder());     // Şifre karşılaştırma algoritması
        return provider;
    }

    /**
     * BCryptPasswordEncoder: şifre hashleme için güvenli algoritma.
     * strength=10: hash hesaplama zorluğu — her artış süreyi 2 katına çıkarır.
     * 10 değeri: ~100ms hesaplama süresi — brute force'u yavaşlatır.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

    /**
     * AuthenticationManager: Spring Security'nin kimlik doğrulama koordinatörü.
     * AuthServiceImpl'de doğrudan inject edilir.
     * AuthenticationConfiguration üzerinden alınır — Spring Boot otomatik yapılandırır.
     */
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }
}
