package com.ecommerce.auth.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Refresh token entity — veritabanında saklanır.
 *
 * Neden refresh token'ı veritabanına kaydediyoruz?
 * - Access token stateless'tır (JWT içinde her şey var), veritabanı gerekmez.
 * - Refresh token stateful olmalıdır: kullanıcı çıkış yaptığında token geçersiz
 *   kılınabilmeli, bu da DB kaydını silmek anlamına gelir.
 * - Token rotation: her kullanımda yeni refresh token üretilir, eskisi silinir.
 *
 * Tek kullanımlık (one-time use) tasarım: refresh token bir kez kullanıldıktan
 * sonra silinir, yerine yeni biri oluşturulur. Bu token theft (çalınma) riskini azaltır.
 */
@Entity
@Table(
    name = "refresh_tokens",
    indexes = {
        @Index(name = "idx_refresh_token_value", columnList = "token"),
        @Index(name = "idx_refresh_token_user", columnList = "user_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Token değeri — UUID veya random string.
     * unique = true: aynı token değeri iki kez DB'de olamaz.
     * length = 500: JWT refresh token uzunluğuna yetecek kadar alan.
     */
    @Column(name = "token", nullable = false, unique = true, length = 500)
    private String token;

    /**
     * Bu token'a sahip kullanıcı.
     * @ManyToOne: bir kullanıcının birden fazla refresh token'ı olabilir
     * (farklı cihazlardan giriş senaryosu için — isteğe bağlı).
     * Sistemimizde bir kullanıcının tek aktif refresh token'ı var.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Token son kullanım tarihi — bu tarihi geçmiş token reddedilir */
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    /** Token'ın hâlâ geçerli olup olmadığını kontrol eder */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
}
