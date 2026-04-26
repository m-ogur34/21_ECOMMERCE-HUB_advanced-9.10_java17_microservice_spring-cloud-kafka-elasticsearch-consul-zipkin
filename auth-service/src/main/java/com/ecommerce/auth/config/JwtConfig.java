package com.ecommerce.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * JWT yapılandırma değerlerini application.yml'den bağlayan sınıf.
 *
 * @ConfigurationProperties ile @Value karşılaştırması:
 * - @Value: tek alan için uygundur
 * - @ConfigurationProperties: ilgili alanları bir sınıfta gruplayarak tip-güvenli bağlama sağlar
 *
 * application.yml'deki jwt.* prefiksiyle başlayan değerler bu sınıfa bind olur.
 * IDE'de otomatik tamamlama da çalışır (spring-configuration-processor jar ile).
 */
@Configuration
@ConfigurationProperties(prefix = "jwt")
public class JwtConfig {

    /** JWT imzalama anahtarı */
    private String secret;

    /** Access token süresi (ms) */
    private long accessTokenExpiration;

    /** Refresh token süresi (ms) */
    private long refreshTokenExpiration;

    // Lombok @Data kullanmak yerine manuel getter/setter — ConfigurationProperties için önerilen
    public String getSecret() { return secret; }
    public void setSecret(String secret) { this.secret = secret; }
    public long getAccessTokenExpiration() { return accessTokenExpiration; }
    public void setAccessTokenExpiration(long accessTokenExpiration) { this.accessTokenExpiration = accessTokenExpiration; }
    public long getRefreshTokenExpiration() { return refreshTokenExpiration; }
    public void setRefreshTokenExpiration(long refreshTokenExpiration) { this.refreshTokenExpiration = refreshTokenExpiration; }
}
