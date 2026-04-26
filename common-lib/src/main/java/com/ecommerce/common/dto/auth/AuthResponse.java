package com.ecommerce.common.dto.auth;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Başarılı giriş/kayıt işlemi sonrası dönen yanıt DTO'su.
 *
 * Güvenlik notu: şifre hash'i ASLA bu yanıtta yer almaz.
 * Entity'den DTO'ya dönüşüm sayesinde hassas alanlar gizlenir.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthResponse {

    /** Kısa ömürlü JWT access token — API çağrılarında kullanılır (15 dakika) */
    private String accessToken;

    /** Uzun ömürlü refresh token — access token yenilemek için (7 gün) */
    private String refreshToken;

    /** Token tipi — HTTP Authorization header'ında kullanılır: "Bearer" */
    @Builder.Default
    private String tokenType = "Bearer";

    /** Access token'ın kaç saniye sonra sona ereceği — frontend timer için */
    private Long expiresIn;

    /** Kullanıcının ID'si */
    private Long userId;

    /** Kullanıcının e-posta adresi */
    private String email;

    /** Kullanıcının tam adı */
    private String fullName;

    /** Kullanıcının rolleri — frontend menü/yetki kontrolü için */
    private List<String> roles;
}
