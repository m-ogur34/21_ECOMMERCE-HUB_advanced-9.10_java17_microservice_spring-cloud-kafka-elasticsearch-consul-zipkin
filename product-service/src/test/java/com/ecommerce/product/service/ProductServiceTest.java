package com.ecommerce.product.service;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductService Unit Testleri")
class ProductServiceTest {

    @Mock private ProductRepository productRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private ProductFactory productFactory;
    @Mock private ProductMapper productMapper;
    @Mock private ProductEventPublisher eventPublisher;
    @Mock private SearchService searchService;

    @InjectMocks
    private ProductServiceImpl productService;

    private Product testProduct;
    private Category testCategory;
    private ProductRequest productRequest;
    private ProductResponse productResponse;

    @BeforeEach
    void setUp() {
        testCategory = new Category();
        testCategory.setId(1L);
        testCategory.setName("Elektronik");

        testProduct = new Product();
        testProduct.setId(1L);
        testProduct.setName("Test Ürün");
        testProduct.setPrice(BigDecimal.valueOf(100));
        testProduct.setStockQuantity(10);
        testProduct.setSku("TEST-001");
        testProduct.setActive(true);
        testProduct.setCategory(testCategory);

        productRequest = ProductRequest.builder()
                .name("Test Ürün")
                .price(BigDecimal.valueOf(100))
                .stockQuantity(10)
                .sku("TEST-001")
                .categoryId(1L)
                .active(true)
                .build();

        productResponse = ProductResponse.builder()
                .id(1L)
                .name("Test Ürün")
                .price(BigDecimal.valueOf(100))
                .stockQuantity(10)
                .sku("TEST-001")
                .build();
    }

    // @Nested: ilgili testleri mantıksal gruplar altında toplar — okunabilirlik artar
    @Nested
    @DisplayName("createProduct testleri")
    class CreateProductTests {

        @Test
        @DisplayName("Geçerli istek ile ürün başarıyla oluşturulur")
        void createProduct_WhenValidRequest_ShouldReturnProductResponse() {
            // GIVEN
            when(productRepository.existsBySku("TEST-001")).thenReturn(false);
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
            when(productFactory.createProduct(productRequest, testCategory)).thenReturn(testProduct);
            when(productRepository.save(testProduct)).thenReturn(testProduct);
            when(productMapper.toResponse(testProduct)).thenReturn(productResponse);

            // WHEN
            ProductResponse result = productService.createProduct(productRequest);

            // THEN
            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo("Test Ürün");
            verify(productRepository).save(testProduct);
            verify(searchService).indexProduct(testProduct);
        }

        @Test
        @DisplayName("Aynı SKU varsa BusinessException fırlatılır")
        void createProduct_WhenSkuExists_ShouldThrowBusinessException() {
            // GIVEN
            when(productRepository.existsBySku("TEST-001")).thenReturn(true);

            // WHEN & THEN
            assertThatThrownBy(() -> productService.createProduct(productRequest))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("SKU");

            verify(productRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("decreaseStock testleri")
    class DecreaseStockTests {

        @Test
        @DisplayName("Yeterli stok varsa stok başarıyla düşürülür")
        void decreaseStock_WhenSufficientStock_ShouldDecreaseSuccessfully() {
            // GIVEN
            when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
            when(productRepository.decreaseStock(1L, 3)).thenReturn(1); // 1 kayıt güncellendi

            // WHEN
            productService.decreaseStock(1L, 3);

            // THEN
            verify(productRepository).decreaseStock(1L, 3);
        }

        @Test
        @DisplayName("Yetersiz stok varsa InsufficientStockException fırlatılır")
        void decreaseStock_WhenInsufficientStock_ShouldThrowException() {
            // GIVEN: stok 10, istenen 20
            when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
            // testProduct.stockQuantity = 10, quantity = 20 → exception

            // WHEN & THEN
            assertThatThrownBy(() -> productService.decreaseStock(1L, 20))
                    .isInstanceOf(InsufficientStockException.class);
        }

        @Test
        @DisplayName("Ürün bulunamazsa ResourceNotFoundException fırlatılır")
        void decreaseStock_WhenProductNotFound_ShouldThrowException() {
            // GIVEN
            when(productRepository.findById(99L)).thenReturn(Optional.empty());

            // WHEN & THEN
            assertThatThrownBy(() -> productService.decreaseStock(99L, 1))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
