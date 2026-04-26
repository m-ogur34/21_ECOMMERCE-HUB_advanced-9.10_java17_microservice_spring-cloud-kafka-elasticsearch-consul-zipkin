package com.ecommerce.product.repository;

import com.ecommerce.product.document.ProductDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Elasticsearch repository — ürün arama için.
 *
 * ElasticsearchRepository<ProductDocument, String>:
 * - ProductDocument: ES document tipi
 * - String: document ID tipi (ES'te ID string)
 *
 * JpaRepository gibi çalışır ama ES'te sorgu gönderir.
 * Temel metodlar: save(), findById(), deleteById() otomatik gelir.
 *
 * Karmaşık sorgular için SearchService'te ElasticsearchOperations kullanılır.
 */
@Repository
public interface ProductSearchRepository extends ElasticsearchRepository<ProductDocument, String> {

    /**
     * İsimde kelime geçen ürünleri getir.
     * ES'te full-text search: "spring" araması "Spring Boot" ve "Spring Security"yi bulur.
     */
    List<ProductDocument> findByNameContaining(String keyword);

    /** Aktif ve stokta olan ürünler */
    List<ProductDocument> findByActiveTrueAndInStockTrue();

    /** Kategoriye göre arama */
    List<ProductDocument> findByCategoryId(Long categoryId);

    /** Markaya göre arama */
    List<ProductDocument> findByBrand(String brand);
}
