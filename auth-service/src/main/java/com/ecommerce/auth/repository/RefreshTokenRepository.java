package com.ecommerce.auth.repository;

import com.ecommerce.auth.model.RefreshToken;
import com.ecommerce.auth.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

/** Refresh token veritabanı erişim katmanı */
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    /** Token değeriyle arama — doğrulama işleminde */
    Optional<RefreshToken> findByToken(String token);

    /** Kullanıcının mevcut token'ını bul */
    Optional<RefreshToken> findByUser(User user);

    /** Kullanıcının tüm token'larını sil — logout veya şifre değiştirme */
    void deleteByUser(User user);

    /**
     * Süresi dolmuş tüm token'ları temizle — cron job tarafından çalıştırılır.
     * @Modifying: bu sorgunun DB'yi değiştirdiğini belirtir (SELECT değil DELETE).
     * Büyük tablolarda performans için toplu (bulk) silme tercih edilir.
     */
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiresAt < :now")
    int deleteExpiredTokens(@Param("now") LocalDateTime now);
}
