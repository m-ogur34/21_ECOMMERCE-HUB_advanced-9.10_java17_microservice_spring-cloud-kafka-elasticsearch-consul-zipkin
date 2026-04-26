package com.ecommerce.auth.mapper;

import com.ecommerce.auth.model.User;
import com.ecommerce.common.dto.auth.AuthResponse;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

/**
 * User entity → AuthResponse DTO dönüşüm sınıfı.
 *
 * Manuel mapper (MapStruct kullanmadan) — öğrenim için tercih edildi.
 * MapStruct versiyonunda @Mapper annotasyonu ve interface yeterli olur.
 *
 * OOP - Encapsulation: dönüşüm mantığı bu sınıfta kapsüllenir.
 * Service sınıfları entity → DTO dönüşümüyle uğraşmaz.
 */
@Component
public class UserMapper {

    /**
     * User entity'sini AuthResponse DTO'ya dönüştürür.
     * Token bilgileri servisten eklenir — mapper sadece kullanıcı bilgilerini dönüştürür.
     */
    public AuthResponse toAuthResponse(User user, String accessToken, String refreshToken, Long expiresIn) {
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(expiresIn)
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .roles(user.getAuthorities().stream()
                        .map(auth -> auth.getAuthority())
                        .collect(Collectors.toList()))
                .build();
    }
}
