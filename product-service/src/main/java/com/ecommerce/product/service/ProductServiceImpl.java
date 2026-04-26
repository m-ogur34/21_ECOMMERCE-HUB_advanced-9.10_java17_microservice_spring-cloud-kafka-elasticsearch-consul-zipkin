package com.ecommerce.product.service;

import com.ecommerce.common.constants.CacheConstants;
import com.ecommerce.common.dto.PageResponse;
import com.ecommerce.common.dto.product.ProductRequest;
import com.ecommerce.common.dto.product.ProductResponse;
import com.ecommerce.common.exception.BusinessException;
import com.ecommerce.common.exception.InsufficientStockException;
import com.ecommerce.common.exception.ResourceNotFoundException;
import com.ecommerce.product.factory.ProductFactory;
import com.ecommerce.product.mapper.ProductMapper;
import com.ecommerce.product.model.Category;
import com.ecommerce.product.model.Product;
import com.ecommerce.product.observer.ProductEventPublisher;
import com.ecommerce.product.repository.CategoryRepository;
import com.ecommerce.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Ürün servisi implementasyonu.
 *
 * Spring Cache Annotasyonları:
 * @Cacheable: metod ilk çalışınca sonuç cache'e yazar, sonrakinde cache'den okur.
 * @CachePut: her çağrıda hem çalışır hem cache'i günceller (update için).
 * @CacheEvict: cache'den siler (delete için).
 *
 * Cache key stratejisi:
 * value = cache adı (Redis'te key prefix)
 * key = #id → gerçek key: "product::123"
 * SpEL (Spring Expression Language) ile key hesaplanır.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductFactory productFactory;
    private final ProductMapper productMapper;
    private final ProductEventPublisher eventPublisher;
    private final SearchService searchService;

    /**
     * Yeni ürün oluştur.
     * Cache'e yazılmaz — @Cacheable getProductById'de olduğu için
     * ilk GET isteğinde otomatik cache'e girer.
     */
    @Override
    public ProductResponse createProduct(ProductRequest request) {
        log.info("Yeni ürün oluşturuluyor: {}", request.getName());

        // SKU benzersizlik kontrolü
        if (productRepository.existsBySku(request.getSku())) {
            throw new BusinessException(
                "Bu SKU zaten kullanılıyor: " + request.getSku(),
                "SKU_ALREADY_EXISTS"
            );
        }

        // Kategori varlık kontrolü
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", request.getCategoryId()));

        // Factory pattern ile nesne oluştur
        Product product = productFactory.createProduct(request, category);
        Product savedProduct = productRepository.save(product);

        // Elasticsearch'e indexle — Observer ile async olarak
        searchService.indexProduct(savedProduct);

        // Observer olayı yayınla — stok gözlemcileri bilgilendirilir
        eventPublisher.publishProductCreatedEvent(savedProduct);

        log.info("Ürün başarıyla oluşturuldu: ID={}", savedProduct.getId());
        return productMapper.toResponse(savedProduct);
    }

    /**
     * Ürünü ID ile getir — Redis cache ile.
     *
     * @Cacheable:
     * - İlk çağrıda: DB sorgusu yapılır, sonuç Redis'e yazılır, döndürülür.
     * - Sonraki çağrılarda: Redis'ten okunur, metod ÇALIŞMAZ (proxy mekanizması).
     *
     * value = "product": Redis key prefix
     * key = "#id": Spring EL — "product::123" gibi bir key üretir
     * unless = "#result == null": null sonuç cache'lenmez
     */
    @Override
    @Cacheable(value = CacheConstants.PRODUCT, key = "#id", unless = "#result == null")
    @Transactional(readOnly = true)
    public ProductResponse getProductById(Long id) {
        log.debug("Ürün DB'den getiriliyor: ID={}", id); // Cache hit'te bu log görünmez!

        return productRepository.findById(id)
                .filter(Product::isActive) // Silinmiş ürünü gösterme
                .map(productMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));
    }

    @Override
    @Cacheable(value = CacheConstants.PRODUCT, key = "#sku")
    @Transactional(readOnly = true)
    public ProductResponse getProductBySku(String sku) {
        return productRepository.findBySku(sku)
                .map(productMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Product", sku));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> getAllProducts(Pageable pageable) {
        Page<ProductResponse> page = productRepository
                .findAll(pageable)
                .map(productMapper::toResponse);
        return PageResponse.from(page);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> getProductsByCategory(Long categoryId, Pageable pageable) {
        // Kategori var mı?
        if (!categoryRepository.existsById(categoryId)) {
            throw new ResourceNotFoundException("Category", categoryId);
        }

        Page<ProductResponse> page = productRepository
                .findByCategoryIdAndActiveTrue(categoryId, pageable)
                .map(productMapper::toResponse);

        return PageResponse.from(page);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> getProductsByPriceRange(
            BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable) {

        Page<ProductResponse> page = productRepository
                .findByPriceBetweenAndActiveTrue(minPrice, maxPrice, pageable)
                .map(productMapper::toResponse);

        return PageResponse.from(page);
    }

    /**
     * Ürün güncelle.
     *
     * @CachePut: Her zaman çalışır ve sonucu cache'e yazar.
     * @Cacheable'dan farkı: cache hit olsa bile metod çalışır.
     * Bu sayede güncelleme yapılır ve cache de güncel veriyle güncellenir.
     */
    @Override
    @CachePut(value = CacheConstants.PRODUCT, key = "#id")
    public ProductResponse updateProduct(Long id, ProductRequest request) {
        log.info("Ürün güncelleniyor: ID={}", id);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));

        // SKU değiştiyse yeni SKU zaten kullanımda mı?
        if (!product.getSku().equals(request.getSku()) &&
            productRepository.existsBySku(request.getSku())) {
            throw new BusinessException("Bu SKU zaten kullanılıyor: " + request.getSku());
        }

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", request.getCategoryId()));

        // Alanları güncelle
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStockQuantity(request.getStockQuantity());
        product.setSku(request.getSku());
        product.setBrand(request.getBrand());
        product.setImageUrl(request.getImageUrl());
        product.setActive(request.isActive());
        product.setCategory(category);

        Product updatedProduct = productRepository.save(product);

        // Elasticsearch'i de güncelle
        searchService.indexProduct(updatedProduct);

        log.info("Ürün güncellendi: ID={}", id);
        return productMapper.toResponse(updatedProduct);
    }

    /**
     * Ürün sil — soft delete (ürünü aktif=false yaparak).
     *
     * @CacheEvict: Cache'deki kaydı siler.
     * allEntries = false: sadece bu ID'nin kaydını sil, tüm cache'i temizleme.
     * beforeInvocation = false: metod başarılıysa sil (default).
     */
    @Override
    @CacheEvict(value = CacheConstants.PRODUCT, key = "#id")
    public void deleteProduct(Long id) {
        log.info("Ürün siliniyor (soft delete): ID={}", id);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));

        product.setActive(false); // Soft delete
        productRepository.save(product);

        // ES'ten de kaldır
        searchService.removeFromIndex(String.valueOf(id));

        log.info("Ürün silindi: ID={}", id);
    }

    /**
     * Stok düşürme.
     * @CacheEvict: Stok değişince cache'deki eski veriyi temizle.
     * Bir sonraki GET isteğinde DB'den taze veri gelir.
     */
    @Override
    @CacheEvict(value = CacheConstants.PRODUCT, key = "#productId")
    public void decreaseStock(Long productId, int quantity) {
        log.info("Stok düşürülüyor: ürün={}, miktar={}", productId, quantity);

        // Önce stok kontrolü yap
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", productId));

        if (product.getStockQuantity() < quantity) {
            throw new InsufficientStockException(productId, quantity, product.getStockQuantity());
        }

        // Optimistik locking yerine DB seviyesinde atomik güncelleme
        int updated = productRepository.decreaseStock(productId, quantity);
        if (updated == 0) {
            // Bu noktaya gelindiyse race condition var — iki istek aynı anda stok düşürmeye çalıştı
            throw new InsufficientStockException("Stok güncellemesi başarısız — eş zamanlılık sorunu");
        }

        // Stok sıfırlandı mı?
        product.setStockQuantity(product.getStockQuantity() - quantity);
        if (!product.isInStock()) {
            eventPublisher.publishStockDepletedEvent(product);
        }

        // ES'teki stok bilgisini güncelle
        searchService.updateStockInIndex(String.valueOf(productId), product.getStockQuantity() - quantity);
    }

    @Override
    @CacheEvict(value = CacheConstants.PRODUCT, key = "#productId")
    public void increaseStock(Long productId, int quantity) {
        log.info("Stok artırılıyor: ürün={}, miktar={}", productId, quantity);
        productRepository.increaseStock(productId, quantity);
    }
}
