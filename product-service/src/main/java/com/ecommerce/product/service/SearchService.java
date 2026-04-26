package com.ecommerce.product.service;

import com.ecommerce.common.dto.product.ProductResponse;
import com.ecommerce.product.document.ProductDocument;
import com.ecommerce.product.mapper.ProductMapper;
import com.ecommerce.product.model.Product;
import com.ecommerce.product.repository.ProductSearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Elasticsearch arama servisi.
 *
 * İki yaklaşım kullanılır:
 * 1. ProductSearchRepository: basit sorgular için — Spring Data metod isimleri
 * 2. ElasticsearchOperations: karmaşık sorgular için — programatik Criteria API
 *
 * ElasticsearchOperations: JPA'daki JpaRepository yerine Criteria sorgusu yazmak gibi.
 * Çok kriterli filtreleme (fiyat + kategori + marka + kelime) için kullanılır.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SearchService {

    private final ProductSearchRepository searchRepository;
    private final ElasticsearchOperations elasticsearchOperations;
    private final ProductMapper productMapper;

    /**
     * Full-text ürün arama — birden fazla alanda arar.
     * keyword hem isimde hem açıklamada hem markada aranır.
     */
    public List<ProductResponse> search(String keyword) {
        log.debug("Elasticsearch araması: keyword={}", keyword);

        // Criteria: çok alanlı arama sorgusu
        Criteria criteria = new Criteria("name").contains(keyword)
                .or(new Criteria("description").contains(keyword))
                .or(new Criteria("brand").contains(keyword));

        // Sadece aktif ve stokta olan ürünleri getir
        criteria = criteria.and(new Criteria("active").is(true));

        CriteriaQuery query = new CriteriaQuery(criteria);

        // ES'ten çekilen sonuçları işle
        SearchHits<ProductDocument> hits = elasticsearchOperations.search(query, ProductDocument.class);

        return hits.getSearchHits().stream()
                .map(SearchHit::getContent)           // SearchHit -> ProductDocument
                .map(this::documentToResponse)         // ProductDocument -> ProductResponse
                .collect(Collectors.toList());
    }

    /**
     * Çok kriterli filtreleme araması.
     * Parametreler null ise o kriter uygulanmaz (esnek filtreleme).
     */
    public List<ProductResponse> searchWithFilters(
            String keyword,
            Long categoryId,
            String brand,
            BigDecimal minPrice,
            BigDecimal maxPrice) {

        Criteria criteria = new Criteria("active").is(true);

        if (keyword != null && !keyword.isBlank()) {
            criteria = criteria.and(
                new Criteria("name").contains(keyword)
                    .or(new Criteria("description").contains(keyword))
            );
        }

        if (categoryId != null) {
            criteria = criteria.and(new Criteria("categoryId").is(categoryId));
        }

        if (brand != null && !brand.isBlank()) {
            criteria = criteria.and(new Criteria("brand").is(brand));
        }

        if (minPrice != null) {
            criteria = criteria.and(new Criteria("price").greaterThanEqual(minPrice));
        }

        if (maxPrice != null) {
            criteria = criteria.and(new Criteria("price").lessThanEqual(maxPrice));
        }

        CriteriaQuery query = new CriteriaQuery(criteria);
        SearchHits<ProductDocument> hits = elasticsearchOperations.search(query, ProductDocument.class);

        return hits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .map(this::documentToResponse)
                .collect(Collectors.toList());
    }

    /** Ürünü Elasticsearch'e indexle — yeni veya güncellenmiş ürün için */
    public void indexProduct(Product product) {
        try {
            ProductDocument document = productMapper.toDocument(product);
            searchRepository.save(document);
            log.debug("Ürün ES'e indexlendi: ID={}", product.getId());
        } catch (Exception e) {
            // ES hatası ana akışı engellememeli — loglama yeterli
            log.error("Ürün ES indexleme hatası: ID={}, Hata={}", product.getId(), e.getMessage());
        }
    }

    /** ES'ten ürünü kaldır */
    public void removeFromIndex(String productId) {
        try {
            searchRepository.deleteById(productId);
            log.debug("Ürün ES'ten kaldırıldı: ID={}", productId);
        } catch (Exception e) {
            log.error("Ürün ES silme hatası: ID={}", productId);
        }
    }

    /** ES'teki stok bilgisini güncelle */
    public void updateStockInIndex(String productId, int newStock) {
        searchRepository.findById(productId).ifPresent(doc -> {
            doc.setStockQuantity(newStock);
            doc.setInStock(newStock > 0);
            searchRepository.save(doc);
        });
    }

    /** ProductDocument → ProductResponse (mapper kullanamayız — entity yok) */
    private ProductResponse documentToResponse(ProductDocument doc) {
        return ProductResponse.builder()
                .id(Long.parseLong(doc.getId()))
                .name(doc.getName())
                .description(doc.getDescription())
                .price(doc.getPrice())
                .stockQuantity(doc.getStockQuantity())
                .inStock(doc.isInStock())
                .categoryId(doc.getCategoryId())
                .categoryName(doc.getCategoryName())
                .brand(doc.getBrand())
                .sku(doc.getSku())
                .imageUrl(doc.getImageUrl())
                .active(doc.isActive())
                .build();
    }
}
