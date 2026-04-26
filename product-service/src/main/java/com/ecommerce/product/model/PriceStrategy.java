package com.ecommerce.product.model;

import java.math.BigDecimal;

/**
 * Fiyat hesaplama stratejisi arayüzü — Strategy Pattern.
 *
 * Strategy Pattern: algoritmaları kapsüller ve birbirinin yerine kullanılabilir yapar.
 * Yeni fiyatlandırma kuralı eklmek için sadece yeni bir implementasyon yeterli.
 *
 * Kullanım senaryoları:
 * - RegularPriceStrategy: standart fiyat
 * - DiscountPriceStrategy: yüzde indirimli fiyat
 * - BulkPriceStrategy: toplu alım indirimi
 * - SeasonalPriceStrategy: mevsimsel fiyatlandırma
 */
@FunctionalInterface // Tek soyut metot içeren interface — lambda ile kullanılabilir
public interface PriceStrategy {

    /**
     * Fiyat hesapla.
     * @param basePrice Temel fiyat
     * @param quantity  Miktar
     * @return Hesaplanmış toplam fiyat
     */
    BigDecimal calculate(BigDecimal basePrice, int quantity);

    // ===== STATIK FACTORY METODLARI (Java 8+ interface default/static metod) =====

    /** Standart fiyat stratejisi: basePrice × quantity */
    static PriceStrategy regular() {
        return (basePrice, quantity) -> basePrice.multiply(BigDecimal.valueOf(quantity));
    }

    /**
     * İndirimli fiyat stratejisi.
     * @param discountPercent İndirim yüzdesi (0-100)
     */
    static PriceStrategy discounted(double discountPercent) {
        return (basePrice, quantity) -> {
            BigDecimal discount = BigDecimal.valueOf(1 - discountPercent / 100);
            return basePrice.multiply(discount).multiply(BigDecimal.valueOf(quantity));
        };
    }

    /**
     * Toplu alım stratejisi.
     * @param threshold    Kaç adetten sonra indirim uygulanır
     * @param bulkDiscount Bu miktarın üzerindeki indirim yüzdesi
     */
    static PriceStrategy bulk(int threshold, double bulkDiscount) {
        return (basePrice, quantity) -> {
            if (quantity >= threshold) {
                BigDecimal discount = BigDecimal.valueOf(1 - bulkDiscount / 100);
                return basePrice.multiply(discount).multiply(BigDecimal.valueOf(quantity));
            }
            return basePrice.multiply(BigDecimal.valueOf(quantity));
        };
    }
}
