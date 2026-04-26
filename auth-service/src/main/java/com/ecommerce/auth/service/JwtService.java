package com.ecommerce.auth.service;

import com.ecommerce.common.constants.SecurityConstants;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * JWT token oluşturma, doğrulama ve parse etme servisi.
 *
 * JWT Yapısı (3 bölüm, nokta ile ayrılmış):
 * Header.Payload.Signature
 * eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ1c2VyQGVtYWlsLmNvbSIsInJvbGVzIjpbIlJPTEVfVVNFUiJdfQ.xxx
 *
 * HMAC-SHA256 (HS256) algoritması: simetrik şifreleme — aynı anahtar ile imzalama ve doğrulama.
 * Asimetrik (RS256) alternatifte private key ile imzalanır, public key ile doğrulanır.
 * Mikroservis ortamında RS256 daha güvenlidir (her servis public key ile doğrulayabilir).
 * Kolaylık için HS256 kullanıyoruz — gerçek üretimde RS256 tercih edilir.
 */
@Slf4j
@Service
public class JwtService {

    /**
     * JWT imzalama gizli anahtarı.
     * application.yml'den inject edilir: jwt.secret
     * Base64 encode edilmiş, en az 256 bit uzunluğunda olmalı (HS256 için).
     */
    @Value("${jwt.secret}")
    private String secretKey;

    /** Access token geçerlilik süresi — application.yml'den alınır */
    @Value("${jwt.access-token-expiration}")
    private long accessTokenExpiration;

    /** Refresh token geçerlilik süresi */
    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    // ===== TOKEN OLUŞTURMA =====

    /**
     * Kullanıcı için access token üretir.
     * Claims (payload): kullanıcı e-postası, rolleri, kullanıcı ID'si.
     *
     * @param userDetails Spring Security kullanıcı detayları
     * @return İmzalanmış JWT token string'i
     */
    public String generateAccessToken(UserDetails userDetails) {
        Map<String, Object> extraClaims = new HashMap<>();

        // Rolleri string listesi olarak token'a ekle
        extraClaims.put(SecurityConstants.ROLES_CLAIM,
                userDetails.getAuthorities().stream()
                        .map(auth -> auth.getAuthority()) // "ROLE_ADMIN" gibi
                        .collect(Collectors.toList()));

        return buildToken(extraClaims, userDetails.getUsername(), accessTokenExpiration);
    }

    /**
     * Refresh token üretir — access token'dan farklı olarak sadece subject (email) içerir.
     * Daha uzun ömürlü, ancak daha az bilgi içerir.
     */
    public String generateRefreshToken(UserDetails userDetails) {
        return buildToken(new HashMap<>(), userDetails.getUsername(), refreshTokenExpiration);
    }

    /**
     * JWT token'ı inşa eder (builder pattern kullanımı).
     *
     * @param extraClaims Ek payload bilgileri
     * @param subject     Token sahibi (e-posta)
     * @param expiration  Milisaniye cinsinden geçerlilik süresi
     */
    private String buildToken(Map<String, Object> extraClaims, String subject, long expiration) {
        Date now = new Date();
        Date expirationDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .setClaims(extraClaims)          // Ek claim'ler (roller vb.)
                .setSubject(subject)             // Token sahibi: e-posta
                .setIssuedAt(now)               // Token üretim zamanı
                .setExpiration(expirationDate)  // Token son kullanım tarihi
                .signWith(getSigningKey(), SignatureAlgorithm.HS256) // İmzala
                .compact();                     // String'e dönüştür
    }

    // ===== TOKEN DOĞRULAMA =====

    /**
     * Token'ın geçerli olup olmadığını kontrol eder.
     * İki koşul: token içindeki kullanıcı, istekteki kullanıcıyla aynı VE süresi dolmamış.
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            final String username = extractUsername(token);
            return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
        } catch (Exception e) {
            log.warn("Token doğrulama başarısız: {}", e.getMessage());
            return false;
        }
    }

    /** Token'ın süresinin dolup dolmadığını kontrol eder */
    public boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    // ===== TOKEN PARSE ETME =====

    /** Token'dan kullanıcı adını (e-postayı) çıkarır */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /** Token'dan son kullanım tarihini çıkarır */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /** Token'dan rolleri çıkarır */
    @SuppressWarnings("unchecked")
    public List<String> extractRoles(String token) {
        return extractClaim(token, claims ->
                claims.get(SecurityConstants.ROLES_CLAIM, List.class));
    }

    /**
     * Generic claim çıkarma metodu — Function<Claims, T> ile herhangi bir claim alınır.
     * Generic tip + functional interface kullanımı — OOP ve functional programming birlikte.
     *
     * @param token          JWT token
     * @param claimsResolver Hangi claim'in alınacağını belirleyen fonksiyon
     * @param <T>            Döndürülecek claim tipi
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims); // Lambda ile claim seçimi
    }

    /**
     * Token'ı doğrular ve tüm claim'leri çıkarır.
     * parseClaimsJws: token imzasını da doğrular — değiştirilmiş token reddedilir.
     * ExpiredJwtException: süresi dolmuş token fırlatır.
     */
    private Claims extractAllClaims(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getSigningKey()) // Doğrulama için anahtar
                    .build()
                    .parseClaimsJws(token)          // İmzayı doğrula + parse et
                    .getBody();                     // Payload (claims)
        } catch (ExpiredJwtException e) {
            log.debug("JWT token süresi dolmuş: {}", e.getMessage());
            throw e; // Filter'da yakalanacak
        }
    }

    /**
     * Base64 kodlu gizli anahtarı HMAC-SHA Key nesnesine dönüştürür.
     * Keys.hmacShaKeyFor: minimum key uzunluğunu da doğrular (HS256 için 256 bit).
     */
    private Key getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
