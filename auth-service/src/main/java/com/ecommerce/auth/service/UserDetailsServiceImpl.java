package com.ecommerce.auth.service;

import com.ecommerce.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Spring Security'nin UserDetailsService arayüzünü implemente eden sınıf.
 *
 * OOP - Interface Implementation: Spring Security, kullanıcı yüklemesi için
 * bu arayüzü kullanır. Implementasyonu değiştirerek farklı veri kaynakları
 * (LDAP, OAuth, DB) kullanılabilir — Dependency Inversion prensibi.
 *
 * Spring Security flow:
 * 1. Kullanıcı giriş yapar → AuthenticationManager çalışır
 * 2. AuthenticationManager → UserDetailsService.loadUserByUsername() çağırır
 * 3. UserDetails döner → şifre karşılaştırılır
 * 4. Başarılıysa SecurityContext güncellenir
 */
@Slf4j
@Service
@RequiredArgsConstructor // Lombok: final alanlar için constructor inject
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * Kullanıcı adı (e-posta) ile kullanıcıyı veritabanından yükler.
     *
     * @Transactional: Bu metot bir transaction içinde çalışır.
     * EAGER yüklenen rollerin transaction dışında erişilmesi LazyInitializationException
     * fırlatabilir — @Transactional bu problemi önler.
     *
     * @param username Sistemimizde e-posta adresi olarak kullanılır
     * @throws UsernameNotFoundException Kullanıcı bulunamazsa — Spring Security bunu yakalar
     */
    @Override
    @Transactional(readOnly = true) // readOnly=true: salt okunur transaction, performans optimizasyonu
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("Kullanıcı yükleniyor: {}", username);

        // findByEmailWithRoles: roller JOIN FETCH ile tek sorguda yüklenir
        return userRepository.findByEmailWithRoles(username)
                .orElseThrow(() -> {
                    log.warn("Kullanıcı bulunamadı: {}", username);
                    // Spring Security'nin beklediği exception tipi
                    return new UsernameNotFoundException(
                            "Kullanıcı bulunamadı: " + username
                    );
                });
    }
}
