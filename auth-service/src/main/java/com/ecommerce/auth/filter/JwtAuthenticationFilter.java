package com.ecommerce.auth.filter;

import com.ecommerce.auth.service.JwtService;
import com.ecommerce.auth.service.UserDetailsServiceImpl;
import com.ecommerce.common.constants.SecurityConstants;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT kimlik doğrulama filtresi.
 *
 * Spring Security Filter Chain'e eklenir. Her HTTP isteğinde SADECE BİR KEZ çalışır
 * (OncePerRequestFilter garantisi — yönlendirme durumlarında tekrar çalışmaz).
 *
 * Filter akışı:
 * 1. İstekten Authorization header'ını al
 * 2. "Bearer " ile başlıyor mu? Başlamıyorsa filteri atla (sonraki filter'a geç)
 * 3. Token'dan kullanıcı adını (e-postayı) çıkar
 * 4. SecurityContext boşsa (başka filter login yapmamışsa) kullanıcıyı yükle
 * 5. Token geçerliyse SecurityContext'i güncelle
 * 6. SecurityContext'te authentication varsa istek yetkili sayılır
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsServiceImpl userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        // ---- 1. Authorization header'ı al ----
        final String authHeader = request.getHeader(SecurityConstants.AUTHORIZATION_HEADER);

        // Bearer token yoksa bu filter'ı atla — public endpoint olabilir
        if (authHeader == null || !authHeader.startsWith(SecurityConstants.BEARER_PREFIX)) {
            filterChain.doFilter(request, response); // Sonraki filter'a geç
            return;
        }

        // ---- 2. Token'ı header'dan çıkar ----
        // "Bearer eyJhbGci..." → "eyJhbGci..."
        final String jwt = authHeader.substring(SecurityConstants.BEARER_PREFIX_LENGTH);

        try {
            // ---- 3. Token'dan kullanıcı adını çıkar ----
            final String userEmail = jwtService.extractUsername(jwt);

            // ---- 4. SecurityContext boşsa authentication yap ----
            // getAuthentication() != null ise kullanıcı zaten authenticate edilmiş
            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                // Kullanıcıyı veritabanından yükle
                UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);

                // Token geçerli mi? (kullanıcı eşleşiyor + süresi dolmamış)
                if (jwtService.isTokenValid(jwt, userDetails)) {

                    // ---- 5. SecurityContext'i güncelle ----
                    // UsernamePasswordAuthenticationToken: Spring Security'nin kimlik nesnesi
                    // 3. parametre (authorities): null değil, yetkilendirme için dolu
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,                          // credentials: JWT doğrulandı, şifreye gerek yok
                                userDetails.getAuthorities()   // Roller
                            );

                    // İstek detaylarını (IP, session) authentication'a ekle
                    authToken.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request)
                    );

                    // SecurityContext'e kaydet — artık bu istek yetkili
                    SecurityContextHolder.getContext().setAuthentication(authToken);

                    log.debug("Kullanıcı doğrulandı: {} | URL: {}",
                            userEmail, request.getRequestURI());
                }
            }

        } catch (ExpiredJwtException e) {
            // Token süresi dolmuş — 401 döndürmek yerine filter'ı geç
            // SecurityContext boş kalır → Spring Security 401 döndürür
            log.warn("Süresi dolmuş JWT token: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.warn("Hatalı JWT token formatı: {}", e.getMessage());
        } catch (Exception e) {
            log.error("JWT işleme hatası: {}", e.getMessage());
        }

        // Bir sonraki filter'a veya controller'a geç
        filterChain.doFilter(request, response);
    }
}
