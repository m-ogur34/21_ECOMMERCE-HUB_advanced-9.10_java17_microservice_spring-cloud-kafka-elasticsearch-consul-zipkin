package com.ecommerce.common.constants;

/**
 * Redis cache key isimleri ve TTL (Time To Live) sürelerini tutan sabit sınıf.
 *
 * Cache key tasarımı için en iyi pratik:
 * - Namespace kullan: "product::" + id → farklı servis anahtarları çakışmaz
 * - TTL'yi iş gereksinimine göre belirle: sık değişen veri = kısa TTL
 * - @Cacheable(value = CacheConstants.PRODUCTS, key = "#id") şeklinde kullanılır
 */
public final class CacheConstants {

    private CacheConstants() {
        throw new UnsupportedOperationException("Bu sınıf örneklenemez");
    }

    // ===== CACHE ADLARI (@Cacheable value parametresi) =====

    /** Tek ürün önbelleği — key: productId */
    public static final String PRODUCT = "product";

    /** Ürün listesi önbelleği — key: sayfa parametrelerine göre */
    public static final String PRODUCTS = "products";

    /** Kategori önbelleği */
    public static final String CATEGORY = "category";

    /** Tüm kategoriler listesi */
    public static final String CATEGORIES = "categories";

    // ===== TTL DEĞERLERİ (saniye cinsinden) =====

    /** Ürün detayı 10 dakika önbellekte tutulur */
    public static final long PRODUCT_TTL_SECONDS = 600L; // 10 dakika

    /** Ürün listesi 5 dakika önbellekte tutulur (daha sık değişir) */
    public static final long PRODUCTS_LIST_TTL_SECONDS = 300L; // 5 dakika

    /** Kategoriler nadiren değişir, 1 saat önbellekte tutulabilir */
    public static final long CATEGORY_TTL_SECONDS = 3600L; // 1 saat

    // ===== RATE LIMITING ANAHTARLARI =====

    /** API Gateway rate limit için Redis key prefix */
    public static final String RATE_LIMIT_PREFIX = "rate_limit::";

    /** Token bucket kapasitesi: 1 dakikada izin verilen maksimum istek sayısı */
    public static final int RATE_LIMIT_REPLENISH_RATE = 10;  // Saniyede eklenen token
    public static final int RATE_LIMIT_BURST_CAPACITY = 20;  // Anlık maksimum istek
}
