package com.ecommerce.auth.model;

import jakarta.persistence.*;
import lombok.*;

/**
 * Kullanıcı rolü entity sınıfı.
 * Çok-a-çok ilişki: bir kullanıcının birden fazla rolü, bir rolün birden fazla kullanıcısı olabilir.
 *
 * Roller: ROLE_USER, ROLE_ADMIN, ROLE_MODERATOR
 * Spring Security, yetkilendirme kararlarında bu isimleri kullanır.
 * "ROLE_" prefix Spring Security convention'ıdır — hasRole("ADMIN") çalışması için gerekir.
 */
@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Role extends BaseEntity {

    /**
     * Rol adı — eşsiz (unique) olmalı.
     * EnumType.STRING: veritabanında "ROLE_ADMIN" gibi yazıyla saklanır.
     * EnumType.ORDINAL olsaydı sıra numarasıyla saklanırdı — enum sırası değişince data bozulur!
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "name", nullable = false, unique = true, length = 50)
    private RoleName name;

    /** İnsan okunabilir açıklama */
    @Column(name = "description", length = 200)
    private String description;

    /** Rol adı enum — tip güvenliği için String yerine enum kullanıyoruz */
    public enum RoleName {
        ROLE_USER,       // Standart kullanıcı
        ROLE_ADMIN,      // Tam yetkili yönetici
        ROLE_MODERATOR   // İçerik moderatörü
    }
}
