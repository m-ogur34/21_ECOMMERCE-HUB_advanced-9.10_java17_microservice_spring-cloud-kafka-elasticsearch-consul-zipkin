package com.ecommerce.auth.model;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Kullanıcı entity sınıfı.
 *
 * OOP - Inheritance: BaseEntity'den id, createdAt, updatedAt, deletedAt kalıtılır.
 *
 * OOP - Interface Implementation: UserDetails arayüzünü implemente eder.
 * Spring Security, kullanıcı bilgilerini bu arayüz üzerinden okur.
 * UserDetails contract'ı: getUsername(), getPassword(), getAuthorities() vb.
 *
 * Bu yaklaşımla entity doğrudan Spring Security ile entegre olur —
 * ayrı bir adapter sınıfı gerekmez (basit tasarım).
 */
@Entity
@Table(
    name = "users",
    indexes = {
        // E-posta ile kullanıcı arama çok sık yapılır — index performansı artırır
        @Index(name = "idx_users_email", columnList = "email")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends BaseEntity implements UserDetails {

    @Column(name = "first_name", nullable = false, length = 50)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 50)
    private String lastName;

    /**
     * E-posta — login için kullanılan benzersiz tanımlayıcı.
     * unique = true: aynı e-posta ile iki kullanıcı kayıt olamaz.
     */
    @Column(name = "email", nullable = false, unique = true, length = 100)
    private String email;

    /**
     * BCrypt ile hashlenmiş şifre.
     * Asla plain text saklanmaz. BCrypt: adaptive hashing + salt üretimi sağlar.
     * Brute force saldırılarına karşı yavaş çalışması kasıtlıdır (work factor).
     */
    @Column(name = "password_hash", nullable = false)
    private String password;

    @Column(name = "phone", length = 20)
    private String phone;

    /** Hesap aktif mi? false = giriş yapamaz */
    @Builder.Default
    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    /** Hesap kilitli mi? (çok fazla başarısız giriş gibi durumlarda) */
    @Builder.Default
    @Column(name = "account_non_locked", nullable = false)
    private boolean accountNonLocked = true;

    /**
     * Kullanıcı-Rol çoka-çok ilişkisi.
     * @ManyToMany: bir kullanıcının birden fazla rolü olabilir.
     * @JoinTable: ara tablo (user_roles) tanımı — JPA bunu otomatik yönetir.
     * FetchType.EAGER: kullanıcı yüklenince roller de hemen yüklenir.
     * Güvenlik kararları için roller hemen lazım olduğundan EAGER tercih edilir.
     */
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "user_roles",
        joinColumns = @JoinColumn(name = "user_id"),       // Bu tablodaki FK
        inverseJoinColumns = @JoinColumn(name = "role_id") // roles tablosundaki FK
    )
    @Builder.Default
    private Set<Role> roles = new HashSet<>();

    // ===== UserDetails Arayüzü Implementasyonu =====

    /**
     * Spring Security'nin kullanıcı adı olarak kabul ettiği alan.
     * Sistemimizde kullanıcı adı olarak e-posta kullanılıyor.
     */
    @Override
    public String getUsername() {
        return email;
    }

    /**
     * Kullanıcının yetkileri (roller) — Spring Security yetkilendirme için kullanır.
     * SimpleGrantedAuthority: "ROLE_ADMIN" gibi string'i GrantedAuthority'ye çevirir.
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority(role.getName().name()))
                .collect(Collectors.toSet());
    }

    /** BCrypt hashlenmiş şifreyi döner — Spring Security bu hash'i karşılaştırır */
    @Override
    public String getPassword() {
        return password;
    }

    /** Hesap süresi doldu mu? Sistemimizde süre kısıtlaması yok */
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return accountNonLocked;
    }

    /** Şifre süresi doldu mu? Sistemimizde şifre süresi yok */
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    /** Kullanıcının tam adını döner — UI'da göstermek için */
    public String getFullName() {
        return firstName + " " + lastName;
    }

    /** Rol ekleme yardımcı metodu */
    public void addRole(Role role) {
        this.roles.add(role);
    }
}
