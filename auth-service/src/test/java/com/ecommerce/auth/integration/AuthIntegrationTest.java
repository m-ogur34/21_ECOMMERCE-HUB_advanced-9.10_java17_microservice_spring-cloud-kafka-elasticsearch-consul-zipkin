package com.ecommerce.auth.integration;

import com.ecommerce.common.dto.auth.AuthRequest;
import com.ecommerce.common.dto.auth.RegisterRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Auth Service entegrasyon testi.
 *
 * @SpringBootTest: Tam uygulama context'ini yükler — gerçek servisler çalışır.
 * @AutoConfigureMockMvc: HTTP isteklerini gerçek sunucu başlatmadan simüle eder.
 * @ActiveProfiles("test"): test profile'ı aktifleştirir — test DB kullanılır.
 *
 * Unit test vs Integration test farkı:
 * Unit test: tek sınıfı, mock bağımlılıklarla test eder — hızlı, izole
 * Integration test: tüm katmanları (controller→service→DB) birlikte test eder — yavaş, gerçekçi
 *
 * NOT: Bu test gerçek bir PostgreSQL DB gerektirir.
 * Testcontainers ile Docker'da otomatik PostgreSQL başlatılabilir.
 * Şimdilik @SpringBootTest(webEnvironment) ile embedded server kullanılıyor.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Auth Service Entegrasyon Testleri")
class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper; // JSON dönüşümü için

    @Test
    @DisplayName("Kayıt ve giriş tam akışı çalışmalı")
    void fullAuthFlow_ShouldWorkCorrectly() throws Exception {
        // ---- 1. Kayıt ----
        RegisterRequest registerRequest = RegisterRequest.builder()
                .firstName("Integration")
                .lastName("Test")
                .email("integration@test.com")
                .password("Test123!")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())                     // HTTP 201
                .andExpect(jsonPath("$.success").value(true))        // Başarı flag'i
                .andExpect(jsonPath("$.data.accessToken").exists())  // Token mevcut
                .andExpect(jsonPath("$.data.refreshToken").exists());

        // ---- 2. Giriş ----
        AuthRequest authRequest = AuthRequest.builder()
                .email("integration@test.com")
                .password("Test123!")
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("integration@test.com"))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty());
    }

    @Test
    @DisplayName("Geçersiz e-posta formatı ile kayıt 400 döndürür")
    void register_WithInvalidEmail_ShouldReturn400() throws Exception {
        RegisterRequest invalidRequest = RegisterRequest.builder()
                .firstName("Test")
                .lastName("User")
                .email("gecersiz-email")  // Geçersiz format
                .password("Test123!")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }
}
