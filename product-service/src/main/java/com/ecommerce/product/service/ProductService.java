package com.ecommerce.product.service;

import com.ecommerce.common.dto.PageResponse;
import com.ecommerce.common.dto.product.ProductRequest;
import com.ecommerce.common.dto.product.ProductResponse;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;

/**
 * Ürün servisi arayüzü — kontrat tanımı.
 * OOP - Interface: implementasyon detaylarını gizler, test kolaylığı sağlar.
 */
public interface ProductService {

    /** Yeni ürün oluştur */
    ProductResponse createProduct(ProductRequest request);

    /** Ürünü ID ile getir — Redis cache'den veya DB'den */
    ProductResponse getProductById(Long id);

    /** SKU ile ürün getir */
    ProductResponse getProductBySku(String sku);

    /** Tüm ürünleri sayfalı getir */
    PageResponse<ProductResponse> getAllProducts(Pageable pageable);

    /** Kategori bazlı filtreleme */
    PageResponse<ProductResponse> getProductsByCategory(Long categoryId, Pageable pageable);

    /** Fiyat aralığına göre filtreleme */
    PageResponse<ProductResponse> getProductsByPriceRange(
            BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable);

    /** Ürün güncelle — cache geçersiz kılınır */
    ProductResponse updateProduct(Long id, ProductRequest request);

    /** Ürün sil (soft delete) */
    void deleteProduct(Long id);

    /** Stok düşürme — sipariş servisinden çağrılır */
    void decreaseStock(Long productId, int quantity);

    /** Stok artırma — iade/iptal durumunda */
    void increaseStock(Long productId, int quantity);
}
