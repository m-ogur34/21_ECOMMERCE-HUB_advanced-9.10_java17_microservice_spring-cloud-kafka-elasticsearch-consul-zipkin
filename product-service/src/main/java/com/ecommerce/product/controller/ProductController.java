package com.ecommerce.product.controller;

import com.ecommerce.common.dto.ApiResponse;
import com.ecommerce.common.dto.PageResponse;
import com.ecommerce.common.dto.product.ProductRequest;
import com.ecommerce.common.dto.product.ProductResponse;
import com.ecommerce.product.service.ProductService;
import com.ecommerce.product.service.SearchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * Ürün REST API controller'ı.
 *
 * @PreAuthorize: Metod seviyesinde güvenlik — SecurityConfig'den daha granüler kontrol.
 * SpEL ifadeleri: hasRole('ADMIN'), hasAnyRole('ADMIN','MODERATOR'), isAuthenticated()
 *
 * GET metodları herkese açık (SecurityConfig'de ayarlandı).
 * POST/PUT/DELETE sadece ADMIN'e açık.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final SearchService searchService;

    /**
     * GET /api/v1/products
     * Tüm ürünleri sayfalı getir.
     *
     * @RequestParam default değerleri: sayfa 0'dan başlar, 10 ürün, fiyata göre artan sırala.
     * Spring, bu parametrelerden Pageable nesnesini otomatik oluşturur.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ProductResponse>>> getAllProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("ASC")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);
        PageResponse<ProductResponse> products = productService.getAllProducts(pageable);

        return ResponseEntity.ok(ApiResponse.success(products, "Ürünler getirildi"));
    }

    /**
     * GET /api/v1/products/{id}
     * Tek ürün getir — Redis cache'den veya DB'den.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> getProductById(@PathVariable Long id) {
        ProductResponse product = productService.getProductById(id);
        return ResponseEntity.ok(ApiResponse.success(product, "Ürün getirildi"));
    }

    /**
     * GET /api/v1/products/sku/{sku}
     * SKU ile ürün getir — sipariş servisinden kullanılır.
     */
    @GetMapping("/sku/{sku}")
    public ResponseEntity<ApiResponse<ProductResponse>> getProductBySku(@PathVariable String sku) {
        ProductResponse product = productService.getProductBySku(sku);
        return ResponseEntity.ok(ApiResponse.success(product, "Ürün getirildi"));
    }

    /**
     * GET /api/v1/products/search?keyword=telefon
     * Elasticsearch ile full-text arama.
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> searchProducts(
            @RequestParam String keyword) {

        log.info("Ürün araması: keyword={}", keyword);
        List<ProductResponse> results = searchService.search(keyword);
        return ResponseEntity.ok(ApiResponse.success(results, results.size() + " sonuç bulundu"));
    }

    /**
     * GET /api/v1/products/search/advanced?keyword=&categoryId=&brand=&minPrice=&maxPrice=
     * Çok kriterli gelişmiş arama.
     */
    @GetMapping("/search/advanced")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> advancedSearch(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice) {

        List<ProductResponse> results = searchService.searchWithFilters(
                keyword, categoryId, brand, minPrice, maxPrice);

        return ResponseEntity.ok(ApiResponse.success(results, "Arama tamamlandı"));
    }

    /**
     * GET /api/v1/products/category/{categoryId}
     * Kategoriye göre ürünleri sayfalı getir.
     */
    @GetMapping("/category/{categoryId}")
    public ResponseEntity<ApiResponse<PageResponse<ProductResponse>>> getByCategory(
            @PathVariable Long categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        PageResponse<ProductResponse> products = productService.getProductsByCategory(categoryId, pageable);
        return ResponseEntity.ok(ApiResponse.success(products, "Kategori ürünleri getirildi"));
    }

    /**
     * POST /api/v1/products
     * Yeni ürün oluştur — sadece ADMIN.
     *
     * @PreAuthorize: Spring Security AOP ile metod çağrılmadan önce kontrol yapılır.
     * hasRole('ADMIN'): kullanıcının ROLE_ADMIN rolü var mı?
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(
            @Valid @RequestBody ProductRequest request) {

        log.info("Ürün oluşturma isteği: {}", request.getName());
        ProductResponse product = productService.createProduct(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(product, "Ürün başarıyla oluşturuldu"));
    }

    /**
     * PUT /api/v1/products/{id}
     * Ürün güncelle — sadece ADMIN.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequest request) {

        ProductResponse product = productService.updateProduct(id, request);
        return ResponseEntity.ok(ApiResponse.success(product, "Ürün güncellendi"));
    }

    /**
     * DELETE /api/v1/products/{id}
     * Ürün sil (soft delete) — sadece ADMIN.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.ok(ApiResponse.success("Ürün silindi"));
    }

    /**
     * PATCH /api/v1/products/{id}/stock/decrease?quantity=5
     * Stok düşür — order-service bu endpoint'i çağırır.
     * ROLE_USER de çağırabilir (sipariş verme sürecinde).
     */
    @PatchMapping("/{id}/stock/decrease")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> decreaseStock(
            @PathVariable Long id,
            @RequestParam int quantity) {

        productService.decreaseStock(id, quantity);
        return ResponseEntity.ok(ApiResponse.success("Stok güncellendi"));
    }

    /**
     * PATCH /api/v1/products/{id}/stock/increase?quantity=5
     * Stok artır — iade/iptal durumunda.
     */
    @PatchMapping("/{id}/stock/increase")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> increaseStock(
            @PathVariable Long id,
            @RequestParam int quantity) {

        productService.increaseStock(id, quantity);
        return ResponseEntity.ok(ApiResponse.success("Stok güncellendi"));
    }
}
