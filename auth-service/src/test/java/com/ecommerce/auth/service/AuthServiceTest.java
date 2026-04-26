package com.ecommerce.auth.service;

import com.ecommerce.auth.model.Role;
import com.ecommerce.auth.model.User;
import com.ecommerce.auth.repository.RefreshTokenRepository;
import com.ecommerce.auth.repository.RoleRepository;
import com.ecommerce.auth.repository.UserRepository;
import com.ecommerce.common.dto.auth.AuthRequest;
import com.ecommerce.common.dto.auth.AuthResponse;
import com.ecommerce.common.dto.auth.RegisterRequest;
import com.ecommerce.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * AuthService unit testleri.
 *
 * @ExtendWith(MockitoExtension.class): JUnit 5 ile Mockito entegrasyonu.
 * @SpringBootTest kullanmıyoruz — uygulama context'i yüklenmez, sadece test sınıfı çalışır.
 * Bu yaklaşım çok daha hızlıdır (milisaniyeler vs saniyeler).
 *
 * Mockito temel kavramları:
 * @Mock: Sahte (mock) nesne oluşturur — gerçek implementasyon çalışmaz
 * @InjectMocks: Mock'ları inject ederek test edilen sınıfı oluşturur
 * when().thenReturn(): mock'un belirli bir çağrıya nasıl yanıt vereceğini ayarlar
 * verify(): mock'un belirli bir metodunun çağrılıp çağrılmadığını doğrular
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Unit Testleri")
class AuthServiceTest {

    // ---- Mock Bağımlılıklar ----
    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private AuthenticationManager authenticationManager;

    // ---- Test Edilen Sınıf ----
    @InjectMocks
    private AuthServiceImpl authService;

    // ---- Test Veri Değişkenleri ----
    private RegisterRequest registerRequest;
    private AuthRequest authRequest;
    private User testUser;
    private Role userRole;

    /** Her testten önce çalışır — test verisini hazırlar */
    @BeforeEach
    void setUp() {
        // Kayıt isteği hazırla
        registerRequest = RegisterRequest.builder()
                .firstName("Test")
                .lastName("Kullanıcı")
                .email("test@ecommerce.com")
                .password("Test123!")
                .build();

        // Giriş isteği hazırla
        authRequest = AuthRequest.builder()
                .email("test@ecommerce.com")
                .password("Test123!")
                .build();

        // Test kullanıcısı hazırla
        userRole = Role.builder()
                .name(Role.RoleName.ROLE_USER)
                .description("Kullanıcı rolü")
                .build();

        testUser = User.builder()
                .firstName("Test")
                .lastName("Kullanıcı")
                .email("test@ecommerce.com")
                .password("$2a$10$hashedPassword") // BCrypt hash simülasyonu
                .enabled(true)
                .accountNonLocked(true)
                .build();
        testUser.addRole(userRole);
    }

    // ===== REGISTER TESTLERİ =====

    @Test
    @DisplayName("Başarılı kayıt: yeni kullanıcı oluşturulur ve token döner")
    void register_WhenValidRequest_ShouldReturnAuthResponse() {
        // GIVEN — mock davranışları ayarla
        when(userRepository.existsByEmail(registerRequest.getEmail()))
                .thenReturn(false); // E-posta kullanılmıyor

        when(roleRepository.findByName(Role.RoleName.ROLE_USER))
                .thenReturn(Optional.of(userRole));

        when(passwordEncoder.encode(registerRequest.getPassword()))
                .thenReturn("$2a$10$hashedPassword");

        when(userRepository.save(any(User.class)))
                .thenReturn(testUser);

        when(jwtService.generateAccessToken(any()))
                .thenReturn("mock.access.token");

        when(jwtService.generateRefreshToken(any()))
                .thenReturn("mock.refresh.token");

        when(refreshTokenRepository.save(any()))
                .thenReturn(null); // Dönen değer test için önemli değil

        // WHEN — test edilen metodu çalıştır
        AuthResponse response = authService.register(registerRequest);

        // THEN — sonuçları doğrula
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("mock.access.token");
        assertThat(response.getRefreshToken()).isEqualTo("mock.refresh.token");
        assertThat(response.getEmail()).isEqualTo("test@ecommerce.com");
        assertThat(response.getTokenType()).isEqualTo("Bearer");

        // Şifrenin hashlendiğini doğrula
        verify(passwordEncoder).encode("Test123!");
        // Kullanıcının kaydedildiğini doğrula
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Kayıt başarısız: aynı e-posta zaten kayıtlı")
    void register_WhenEmailAlreadyExists_ShouldThrowBusinessException() {
        // GIVEN
        when(userRepository.existsByEmail(registerRequest.getEmail()))
                .thenReturn(true); // E-posta zaten kullanılıyor!

        // WHEN & THEN — exception fırlatılıyor mu?
        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("zaten kullanılıyor");

        // Kayıt yapılmamış olmalı
        verify(userRepository, never()).save(any(User.class));
    }

    // ===== LOGIN TESTLERİ =====

    @Test
    @DisplayName("Başarılı giriş: geçerli kimlik bilgileri ile token döner")
    void login_WhenValidCredentials_ShouldReturnAuthResponse() {
        // GIVEN
        // authenticationManager.authenticate() başarılı olur (exception fırlatmaz)
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(null); // Başarılı authentication — null dönebilir

        when(userRepository.findByEmailWithRoles(authRequest.getEmail()))
                .thenReturn(Optional.of(testUser));

        when(refreshTokenRepository.findByUser(testUser))
                .thenReturn(Optional.empty()); // Mevcut token yok

        when(jwtService.generateAccessToken(testUser))
                .thenReturn("mock.access.token");

        when(jwtService.generateRefreshToken(testUser))
                .thenReturn("mock.refresh.token");

        when(refreshTokenRepository.save(any()))
                .thenReturn(null);

        // WHEN
        AuthResponse response = authService.login(authRequest);

        // THEN
        assertThat(response).isNotNull();
        assertThat(response.getEmail()).isEqualTo("test@ecommerce.com");
        assertThat(response.getAccessToken()).isNotBlank();

        verify(authenticationManager).authenticate(any());
    }

    @Test
    @DisplayName("Giriş başarısız: yanlış şifre ile exception fırlatılır")
    void login_WhenInvalidCredentials_ShouldThrowAuthenticationException() {
        // GIVEN — authenticate() BadCredentialsException fırlatır
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Hatalı kimlik bilgileri"));

        // WHEN & THEN
        assertThatThrownBy(() -> authService.login(authRequest))
                .isInstanceOf(com.ecommerce.common.exception.AuthenticationException.class)
                .hasMessageContaining("hatalı");

        // Token üretilmemiş olmalı
        verify(jwtService, never()).generateAccessToken(any());
    }
}
