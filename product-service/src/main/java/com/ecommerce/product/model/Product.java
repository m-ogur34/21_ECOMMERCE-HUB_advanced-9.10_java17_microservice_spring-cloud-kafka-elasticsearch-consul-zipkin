package com.ecommerce.product.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Ürün entity sınıfı — BaseProduct'tan kalıtım alır.
 *
 * OOP - Inheritance zinciri:
 * BaseProduct (MappedSuperclass)
 *   └── Product (Entity, JPA tablosu: products)
 *         └── ProductVariant (Entity, JPA tablosu: product_variants)
 *
 * @Entity: JPA bu sınıfı veritabanı tablosuna map eder.
 */
@Entity
@Table(
    name = "products",
    indexes = {
        @Index(name = "idx_products_sku", columnList = "sku"),
        @Index(name = "idx_products_category", columnList = "category_id"),
        @Index(name = "idx_products_active", columnList = "active")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product extends BaseProduct {

    @Column(name = "brand", length = 100)
    private String brand;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    /**
     * Kategori ilişkisi — bir ürün bir kategoriye aittir.
     * @ManyToOne: birçok ürün aynı kategoride olabilir.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    /**
     * Ürün varyantları — renk, beden gibi seçenekler.
     * @OneToMany: bir ürünün birçok varyantı olabilir.
     * orphanRemoval: varyant listeden çıkarılınca DB'den de silinir.
     */
    @OneToMany(mappedBy = "product",
               cascade = CascadeType.ALL,
               orphanRemoval = true)
    @Builder.Default
    private List<ProductVariant> variants = new ArrayList<>();

    /**
     * BaseProduct'ta tanımlanan soyut metodun implementasyonu.
     * Basit ürün: price × quantity
     *
     * OOP - Polymorphism: ProductVariant bu metodu farklı implementasyonla override eder.
     */
    @Override
    public BigDecimal calculateTotal(int quantity) {
        return getPrice().multiply(BigDecimal.valueOf(quantity));
    }

    /** Varyant ekleme yardımcı metodu */
    public void addVariant(ProductVariant variant) {
        variants.add(variant);
        variant.setProduct(this); // İlişkinin iki tarafını da set et
    }

    /** Varyant kaldırma */
    public void removeVariant(ProductVariant variant) {
        variants.remove(variant);
        variant.setProduct(null);
    }
}
