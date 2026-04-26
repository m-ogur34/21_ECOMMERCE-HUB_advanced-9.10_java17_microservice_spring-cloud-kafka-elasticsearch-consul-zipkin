package com.ecommerce.common.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Kullanıcı kaydı (register) isteği için DTO.
 *
 * Neden DTO kullanırız?
 * - Entity sınıfı (User.java) veritabanı şemasını yansıtır; API kontratını değil.
 * - DTO, API'ye gelen/giden veriyi kontrol eder — gereksiz alanları gizler.
 * - Validation annotationları DTO'ya uygulanır, entity'e değil.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    /** Kullanıcının adı — boş olamaz, 2-50 karakter arası */
    @NotBlank(message = "Ad alanı boş olamaz")
    @Size(min = 2, max = 50, message = "Ad 2-50 karakter arasında olmalıdır")
    private String firstName;

    /** Kullanıcının soyadı */
    @NotBlank(message = "Soyad alanı boş olamaz")
    @Size(min = 2, max = 50, message = "Soyad 2-50 karakter arasında olmalıdır")
    private String lastName;

    /** E-posta adresi — login için kullanılır, eşsiz olmalı */
    @NotBlank(message = "E-posta alanı boş olamaz")
    @Email(message = "Geçerli bir e-posta adresi girin")
    private String email;

    /**
     * Şifre — plain text gelir, serviste BCrypt ile hashlenir.
     * Frontend'den hash gönderilmez: hash üretimi sunucu tarafında yapılmalıdır.
     */
    @NotBlank(message = "Şifre alanı boş olamaz")
    @Size(min = 8, max = 100, message = "Şifre en az 8 karakter olmalıdır")
    private String password;

    /** Telefon numarası — opsiyonel */
    private String phone;
}
