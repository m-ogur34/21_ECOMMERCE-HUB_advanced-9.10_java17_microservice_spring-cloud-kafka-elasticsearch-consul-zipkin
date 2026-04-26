package com.ecommerce.product.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Tüm ürün entity'lerinin kalıtım aldığı soyut temel sınıf.
 *
 * OOP - Inheritance + Abstract Class:
 * Bu sınıf doğrudan örneklenemez (abstract) — sadece alt sınıflar kullanılır.
 * Ortak alanları (id, name, price, stock) ve ortak davranışları (calculateTotal)
 * tek yerden tanımlar.
 *
 * JPA Kalıtım Stratejisi:
 * @Inheritance(strategy = InheritanceType.JOINED):
 * - Her sınıf için ayrı tablo oluşturulur
 * - Alt sınıf tablosu, üst sınıf tablosuna foreign key ile bağlanır
 * - base_products tablosu ortak alanları tutar
 * - product_variants tablosu sadece varyanta özgü alanları tutar
 * - JOIN sorgusu ile birleştirilir — normalize edilmiş tasarım
 *
 * Alternatif stratejiler:
 * - SINGLE_TABLE: tüm sınıflar tek tabloda, null sütunlar var — basit ama normalize değil
 * - TABLE_PER_CLASS: her sınıf tam bağımsız tablo — tekrar var, join yok
 */
@Getter
@Setter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "description", length = 2000)
    private String description;

    /**
     * Fiyat — BigDecimal ile kesin ondalık aritmetik.
     * precision=12: toplam 12 basamak
     * scale=2: ondalık kısımda 2 basamak (kuruş hassasiyeti)
     */
    @Column(name = "price", nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(name = "stock_quantity", nullable = false)
    private Integer stockQuantity;

    @Column(name = "sku", nullable = false, unique = true, length = 100)
    private String sku;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Soyut metod — alt sınıflar kendi fiyat hesaplama mantığını uygular.
     * OOP - Polymorphism: Product ve ProductVariant farklı hesaplama yapar.
     *
     * @param quantity Miktar
     * @return Toplam fiyat
     */
    public abstract BigDecimal calculateTotal(int quantity);

    /** Stokta var mı? */
    public boolean isInStock() {
        return stockQuantity != null && stockQuantity > 0;
    }

    /** Stok düş — negatife düşmesini önle */
    public void decreaseStock(int quantity) {
        if (this.stockQuantity < quantity) {
            throw new IllegalStateException("Yetersiz stok");
        }
        this.stockQuantity -= quantity;
    }

    /** Stok artır — iade/iptal durumunda */
    public void increaseStock(int quantity) {
        this.stockQuantity += quantity;
    }
}
