package com.ecommerce.auth.service;

import com.ecommerce.common.dto.auth.AuthRequest;
import com.ecommerce.common.dto.auth.AuthResponse;
import com.ecommerce.common.dto.auth.RegisterRequest;
import com.ecommerce.common.dto.auth.TokenRefreshRequest;

/**
 * Kimlik doğrulama servisi arayüzü.
 *
 * OOP - Interface (Soyutlama): Bu arayüz, kimlik doğrulama işlemlerinin
 * kontratını tanımlar. İmplementasyon detaylarını gizler.
 *
 * Neden arayüz kullanıyoruz?
 * 1. Dependency Inversion: Controller, somut sınıfa değil arayüze bağlı.
 * 2. Test kolaylığı: Mock implementasyon oluşturmak kolay.
 * 3. Değiştirilebilirlik: OAuth2 implementasyonu eklemek için yeni bir
 *    impl sınıfı yazıp inject etmek yeterli.
 * 4. Okunabilirlik: Arayüz, servisi "ne yapıyor" açısından belgeliyor.
 */
public interface AuthService {

    /**
     * Yeni kullanıcı kaydeder ve token döner.
     * @param request Kayıt bilgileri (ad, soyad, e-posta, şifre)
     * @return Access ve refresh token içeren yanıt
     */
    AuthResponse register(RegisterRequest request);

    /**
     * Kullanıcı girişi yapar ve token döner.
     * @param request Giriş bilgileri (e-posta, şifre)
     * @return Access ve refresh token içeren yanıt
     */
    AuthResponse login(AuthRequest request);

    /**
     * Refresh token ile yeni access token üretir.
     * @param request Mevcut refresh token
     * @return Yeni access token (refresh token değişmeyebilir veya rotasyona girebilir)
     */
    AuthResponse refreshToken(TokenRefreshRequest request);

    /**
     * Kullanıcıyı çıkış yaptırır — refresh token'ı geçersiz kılar.
     * @param refreshToken Geçersiz kılınacak token
     */
    void logout(String refreshToken);
}
