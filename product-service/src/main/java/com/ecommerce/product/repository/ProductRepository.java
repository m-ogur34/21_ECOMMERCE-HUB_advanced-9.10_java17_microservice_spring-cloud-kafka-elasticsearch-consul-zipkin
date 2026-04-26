package com.ecommerce.product.repository;

import com.ecommerce.product.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Ürün veritabanı erişim katmanı.
 *
 * Page<T>: Spring Data'nın sayfalama yapısı.
 * Pageable: sayfa numarası, boyutu ve sıralama bilgisini taşır.
 * Controller'dan gelen ?page=0&size=10&sort=price,asc parametreleri otomatik dönüştürülür.
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    /**
     * SKU ile ürün arama — SKU benzersiz olduğu için Optional döner.
     * Siparişte SKU ile ürün doğrulaması için kullanılır.
     */
    Optional<Product> findBySku(String sku);

    /** SKU daha önce kullanıldı mı? Yeni ürün eklerken kontrol. */
    boolean existsBySku(String sku);

    /**
     * Kategori ID'ye göre aktif ürünleri sayfalı getir.
     * Spring Data metodundan SQL türetir:
     * SELECT * FROM products WHERE category_id = ? AND active = true
     */
    Page<Product> findByCategoryIdAndActiveTrue(Long categoryId, Pageable pageable);

    /**
     * Belirli fiyat aralığındaki aktif ürünleri getir.
     * BETWEEN: minPrice ≤ price ≤ maxPrice
     */
    Page<Product> findByPriceBetweenAndActiveTrue(
            BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable);

    /**
     * Ürün adında arama — basit LIKE sorgusu.
     * Gelişmiş arama için Elasticsearch kullanılır (ProductSearchRepository).
     * %keyword% : herhangi bir yerde geçen kelime
     */
    @Query("SELECT p FROM Product p WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) AND p.active = true")
    Page<Product> searchByName(@Param("keyword") String keyword, Pageable pageable);

    /**
     * Stok düşürme — toplu güncelleme (bulk update).
     * @Modifying: bu sorgunun veri değiştirdiğini belirtir.
     * clearAutomatically: L1 cache'i temizler — sonraki sorgu DB'den taze veri okur.
     *
     * @return Güncellenen kayıt sayısı (1 ise başarılı, 0 ise ürün yok/stok yetersiz)
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Product p SET p.stockQuantity = p.stockQuantity - :quantity " +
           "WHERE p.id = :productId AND p.stockQuantity >= :quantity")
    int decreaseStock(@Param("productId") Long productId, @Param("quantity") int quantity);

    /**
     * Stok artırma — iade/iptal durumunda.
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Product p SET p.stockQuantity = p.stockQuantity + :quantity " +
           "WHERE p.id = :productId")
    int increaseStock(@Param("productId") Long productId, @Param("quantity") int quantity);
}
