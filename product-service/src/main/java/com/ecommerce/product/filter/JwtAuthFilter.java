package com.ecommerce.product.filter;

import com.ecommerce.common.constants.SecurityConstants;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.security.Key;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Product-service JWT doğrulama filtresi.
 *
 * Auth-service'deki JwtAuthenticationFilter ile aynı mantık.
 * Fark: UserDetailsService yok — token'dan gelen rol bilgisiyle authority oluşturulur.
 * Bu şekilde her JWT doğrulama için DB'ye gitmek gerekmez (stateless).
 *
 * Token payload'ında zaten roller var:
 * {"sub": "user@email.com", "roles": ["ROLE_ADMIN"], "exp": ...}
 * Bu roller SimpleGrantedAuthority'ye dönüştürülür.
 */
@Slf4j
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    @Value("${jwt.secret}")
    private String secretKey;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String authHeader = request.getHeader(SecurityConstants.AUTHORIZATION_HEADER);

        if (authHeader == null || !authHeader.startsWith(SecurityConstants.BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String token = authHeader.substring(SecurityConstants.BEARER_PREFIX_LENGTH);

            // Token parse et — imzayı doğrula
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            String username = claims.getSubject();

            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                // Token'dan rolleri çıkar ve SimpleGrantedAuthority'ye dönüştür
                @SuppressWarnings("unchecked")
                List<String> roles = claims.get(SecurityConstants.ROLES_CLAIM, List.class);

                List<SimpleGrantedAuthority> authorities = roles != null
                        ? roles.stream()
                              .map(SimpleGrantedAuthority::new)
                              .collect(Collectors.toList())
                        : List.of();

                // DB sorgusu YOK — token'daki bilgilerle authentication oluştur
                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(username, null, authorities);

                SecurityContextHolder.getContext().setAuthentication(auth);
            }

        } catch (Exception e) {
            log.warn("JWT doğrulama hatası: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    private Key getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
