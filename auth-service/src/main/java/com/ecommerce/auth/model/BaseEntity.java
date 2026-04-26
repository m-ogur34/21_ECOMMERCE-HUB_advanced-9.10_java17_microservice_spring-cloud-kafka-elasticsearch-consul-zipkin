package com.ecommerce.auth.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Tüm JPA entity sınıflarının kalıtım aldığı soyut temel sınıf.
 *
 * OOP - Inheritance (Kalıtım):
 * Bu abstract sınıf, tüm entity'lerde tekrar eden alanları (id, createdAt, updatedAt)
 * tek bir yerden yönetir. DRY (Don't Repeat Yourself) prensibini uygular.
 *
 * @MappedSuperclass: Bu sınıf kendi veritabanı tablosunu OLUŞTURMAZ.
 * Alt sınıfların tablolarına bu alanlar eklenir (tablo başına sınıf stratejisi).
 *
 * @EntityListeners(AuditingEntityListener.class): Spring Data JPA'nın
 * otomatik zaman damgası özelliğini aktifleştirir.
 * @EnableJpaAuditing ana uygulama sınıfında açılmalıdır.
 */
@Getter
@Setter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

    /**
     * Her entity için otomatik üretilen birincil anahtar.
     * IDENTITY stratejisi: veritabanının auto-increment özelliğini kullanır.
     * PostgreSQL'de SERIAL veya GENERATED ALWAYS AS IDENTITY karşılığıdır.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Kaydın ilk oluşturulma tarihi.
     * @CreatedDate: entity ilk persist edildiğinde Spring otomatik set eder.
     * updatable = false: bu alan sonradan güncellenmez.
     */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Kaydın son güncellenme tarihi.
     * @LastModifiedDate: her merge/update işleminde Spring otomatik günceller.
     */
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Soft delete (yumuşak silme) için silinme tarihi.
     * null = aktif kayıt, dolu = silinmiş kayıt.
     * Fiziksel silme yerine bu alan set edilir — veri kaybı yaşanmaz.
     */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    /** Kaydın fiziksel olarak silinip silinmediğini kontrol eder */
    public boolean isDeleted() {
        return deletedAt != null;
    }

    /** Soft delete işlemi — kaydı silmek yerine tarih damgası basar */
    public void markAsDeleted() {
        this.deletedAt = LocalDateTime.now();
    }
}
