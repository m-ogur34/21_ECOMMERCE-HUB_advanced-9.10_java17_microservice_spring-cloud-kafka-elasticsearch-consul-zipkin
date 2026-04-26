package com.ecommerce.product.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Ürün varyantı entity sınıfı — Product'tan kalıtım alır.
 *
 * OOP - Inheritance (3. Seviye):
 * BaseProduct → Product → ProductVariant
 *
 * Varyant: aynı ürünün farklı seçeneği.
 * Örnek: Nike Air Max 90 ürünü için:
 * - Kırmızı / 42 Beden → ProductVariant (additionalPrice: +0 TL)
 * - Mavi / 43 Beden → ProductVariant (additionalPrice: +50 TL)
 *
 * OOP - Polymorphism: calculateTotal() metodu varyant fiyatını da hesaba katar.
 */
@Entity
@Table(name = "product_variants")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductVariant extends BaseProduct {

    /** Ana ürüne referans */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    /** Renk seçeneği */
    @Column(name = "color", length = 50)
    private String color;

    /** Beden seçeneği */
    @Column(name = "size", length = 20)
    private String size;

    /**
     * Ana fiyata eklenen ek fiyat.
     * Toplam fiyat: product.price + additionalPrice
     * Negatif olabilir (indirimli varyant).
     */
    @Column(name = "additional_price", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal additionalPrice = BigDecimal.ZERO;

    /**
     * Varyant toplam fiyat hesaplama.
     * OOP - Polymorphism: Product.calculateTotal() override edildi.
     * Ana ürün fiyatı + ek fiyat + miktar çarpımı.
     */
    @Override
    public BigDecimal calculateTotal(int quantity) {
        BigDecimal basePrice = product != null ? product.getPrice() : getPrice();
        BigDecimal variantPrice = basePrice.add(additionalPrice);
        return variantPrice.multiply(BigDecimal.valueOf(quantity));
    }

    /** Varyant için görüntüleme etiketi: "Kırmızı / 42" */
    public String getLabel() {
        StringBuilder sb = new StringBuilder();
        if (color != null) sb.append(color);
        if (size != null) {
            if (sb.length() > 0) sb.append(" / ");
            sb.append(size);
        }
        return sb.toString();
    }
}
