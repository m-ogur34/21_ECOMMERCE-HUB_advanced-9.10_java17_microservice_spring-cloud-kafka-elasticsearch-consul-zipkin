package com.ecommerce.common.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Kullanıcı girişi (login) isteği için DTO.
 * Sadece e-posta ve şifre gerekir — minimal tasarım prensibi.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthRequest {

    @NotBlank(message = "E-posta alanı boş olamaz")
    @Email(message = "Geçerli bir e-posta adresi girin")
    private String email;

    @NotBlank(message = "Şifre alanı boş olamaz")
    private String password;
}
