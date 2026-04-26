package com.ecommerce.order.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Sipariş kalemi entity sınıfı.
 *
 * Ürün fiyatı sipariş anında kaydedilir (unitPrice).
 * Neden? Ürün fiyatı sonradan değişebilir. Sipariş geçmişi için
 * sipariş anındaki fiyat önemlidir.
 */
@Entity
@Table(name = "order_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem extends BaseEntity {

    /** Ait olduğu sipariş */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    /** Ürün ID — product-service'deki ürüne referans (farklı DB, JPA ilişkisi yok) */
    @Column(name = "product_id", nullable = false)
    private Long productId;

    /** Sipariş anındaki ürün adı — ürün silinse bile sipariş geçmişi korunur */
    @Column(name = "product_name", nullable = false, length = 200)
    private String productName;

    @Column(name = "product_sku", nullable = false, length = 100)
    private String productSku;

    /** Varyant ID — null ise varyantsız ürün */
    @Column(name = "variant_id")
    private Long variantId;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    /** Sipariş anındaki birim fiyat */
    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    /** Toplam fiyat = unitPrice × quantity */
    @Column(name = "total_price", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalPrice;

    /** Toplam fiyatı hesapla ve set et */
    public void calculateTotalPrice() {
        this.totalPrice = unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
