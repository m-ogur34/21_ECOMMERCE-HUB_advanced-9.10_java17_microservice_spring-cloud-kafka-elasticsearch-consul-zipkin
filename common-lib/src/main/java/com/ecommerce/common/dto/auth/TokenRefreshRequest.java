package com.ecommerce.common.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Access token yenileme isteği için DTO.
 * Kullanıcı refresh token'ını gönderir, karşılığında yeni access token alır.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TokenRefreshRequest {

    @NotBlank(message = "Refresh token boş olamaz")
    private String refreshToken;
}
