package com.ecommerce.auth.service;

import com.ecommerce.auth.model.RefreshToken;
import com.ecommerce.auth.model.Role;
import com.ecommerce.auth.model.User;
import com.ecommerce.auth.repository.RefreshTokenRepository;
import com.ecommerce.auth.repository.RoleRepository;
import com.ecommerce.auth.repository.UserRepository;
import com.ecommerce.common.constants.SecurityConstants;
import com.ecommerce.common.dto.auth.AuthRequest;
import com.ecommerce.common.dto.auth.AuthResponse;
import com.ecommerce.common.dto.auth.RegisterRequest;
import com.ecommerce.common.dto.auth.TokenRefreshRequest;
import com.ecommerce.common.exception.AuthenticationException;
import com.ecommerce.common.exception.BusinessException;
import com.ecommerce.common.exception.ResourceNotFoundException;
import com.ecommerce.common.exception.TokenExpiredException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * AuthService arayüzünün somut implementasyonu.
 *
 * OOP - Polymorphism: Controller'da AuthService tipi kullanılır.
 * Spring IoC container, bu sınıfı inject eder.
 * Gelecekte farklı bir implementasyon yazılırsa controller değişmez.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional // Sınıf seviyesinde: tüm public metodlar transaction içinde çalışır
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;    // BCryptPasswordEncoder inject edilecek
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager; // Spring Security

    /**
     * Yeni kullanıcı kaydı.
     *
     * Akış:
     * 1. E-posta benzersizliğini kontrol et
     * 2. Şifreyi BCrypt ile hashle
     * 3. Varsayılan rolü (ROLE_USER) ata
     * 4. Kullanıcıyı kaydet
     * 5. Access + refresh token üret
     */
    @Override
    public AuthResponse register(RegisterRequest request) {
        log.info("Yeni kullanıcı kaydı başlatılıyor: {}", request.getEmail());

        // E-posta zaten kayıtlı mı?
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException(
                "Bu e-posta adresi zaten kullanılıyor: " + request.getEmail(),
                "EMAIL_ALREADY_EXISTS"
            );
        }

        // Varsayılan rolü bul — yoksa sistem hatası (migration'da eklenmeli)
        Role userRole = roleRepository.findByName(Role.RoleName.ROLE_USER)
                .orElseThrow(() -> new ResourceNotFoundException("ROLE_USER", "rol"));

        // Kullanıcı entity'sini oluştur (Builder pattern)
        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword())) // BCrypt hash
                .phone(request.getPhone())
                .enabled(true)
                .accountNonLocked(true)
                .build();

        user.addRole(userRole); // Rolü ata

        User savedUser = userRepository.save(user);
        log.info("Kullanıcı başarıyla kaydedildi: ID={}", savedUser.getId());

        // Token üret ve döndür
        return generateTokenResponse(savedUser);
    }

    /**
     * Kullanıcı girişi.
     *
     * Spring Security'nin AuthenticationManager kullanılır:
     * - UsernamePasswordAuthenticationToken: giriş bilgilerini taşır
     * - AuthenticationManager.authenticate(): şifreyi kontrol eder
     * - BadCredentialsException: yanlış şifrede fırlatılır
     */
    @Override
    public AuthResponse login(AuthRequest request) {
        log.info("Giriş denemesi: {}", request.getEmail());

        try {
            // Spring Security authentication — şifre karşılaştırması burada yapılır
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    request.getEmail(),  // username
                    request.getPassword() // plain text şifre — Spring Security BCrypt ile karşılaştırır
                )
            );
        } catch (BadCredentialsException e) {
            log.warn("Başarısız giriş denemesi: {}", request.getEmail());
            throw new AuthenticationException("E-posta veya şifre hatalı");
        }

        // Kimlik doğrulandı — kullanıcıyı yükle
        User user = userRepository.findByEmailWithRoles(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User", request.getEmail()));

        // Mevcut refresh token'ı sil (tek aktif token politikası)
        refreshTokenRepository.findByUser(user)
                .ifPresent(refreshTokenRepository::delete);

        log.info("Başarılı giriş: {}", user.getEmail());
        return generateTokenResponse(user);
    }

    /**
     * Refresh token ile yeni access token üretir.
     * Token rotation: eski refresh token silinir, yeni biri verilir.
     */
    @Override
    public AuthResponse refreshToken(TokenRefreshRequest request) {
        // Refresh token'ı DB'den bul
        RefreshToken refreshToken = refreshTokenRepository
                .findByToken(request.getRefreshToken())
                .orElseThrow(() -> new AuthenticationException("Geçersiz refresh token"));

        // Süresi dolmuş mu?
        if (refreshToken.isExpired()) {
            refreshTokenRepository.delete(refreshToken); // Geçersiz token'ı temizle
            throw new TokenExpiredException("Refresh token süresi dolmuş. Yeniden giriş yapın.");
        }

        User user = refreshToken.getUser();

        // Token rotation: eski sil, yeni üret
        refreshTokenRepository.delete(refreshToken);

        log.info("Token yenilendi: {}", user.getEmail());
        return generateTokenResponse(user);
    }

    /**
     * Kullanıcıyı çıkış yaptırır — refresh token'ı geçersiz kılar.
     * Access token stateless olduğu için geçersiz kılınamaz (süresi dolana kadar geçerlidir).
     * Bu sınırlama kabul edilebilir çünkü access token ömrü kısa (15 dakika).
     */
    @Override
    public void logout(String refreshTokenValue) {
        refreshTokenRepository.findByToken(refreshTokenValue)
                .ifPresent(token -> {
                    log.info("Çıkış yapılıyor: {}", token.getUser().getEmail());
                    refreshTokenRepository.delete(token);
                });
    }

    // ===== ÖZEL YARDIMCI METODLAR =====

    /**
     * Kullanıcı için access + refresh token üretir ve AuthResponse döner.
     * Hem register hem login metodunda kullanılır — DRY prensibi.
     */
    private AuthResponse generateTokenResponse(User user) {
        // Access token üret (JWT)
        String accessToken = jwtService.generateAccessToken(user);

        // Refresh token üret ve DB'ye kaydet
        String refreshTokenValue = jwtService.generateRefreshToken(user);
        saveRefreshToken(user, refreshTokenValue);

        // Kullanıcının rollerini string listesine çevir
        List<String> roles = user.getAuthorities().stream()
                .map(auth -> auth.getAuthority())
                .collect(Collectors.toList());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshTokenValue)
                .tokenType("Bearer")
                .expiresIn(SecurityConstants.ACCESS_TOKEN_EXPIRATION / 1000) // Saniyeye çevir
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .roles(roles)
                .build();
    }

    /** Refresh token'ı veritabanına kaydeder */
    private void saveRefreshToken(User user, String tokenValue) {
        RefreshToken refreshToken = RefreshToken.builder()
                .token(tokenValue)
                .user(user)
                // 7 gün sonrası — SecurityConstants'tan alınabilir ama burada hesapla
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();

        refreshTokenRepository.save(refreshToken);
    }
}
