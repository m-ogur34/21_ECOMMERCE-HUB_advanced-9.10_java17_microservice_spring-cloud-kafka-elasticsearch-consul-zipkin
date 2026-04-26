package com.ecommerce.product.mapper;

import com.ecommerce.common.dto.product.ProductResponse;
import com.ecommerce.common.dto.product.VariantResponse;
import com.ecommerce.product.document.ProductDocument;
import com.ecommerce.product.model.Product;
import com.ecommerce.product.model.ProductVariant;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Product entity ↔ DTO dönüşüm sınıfı.
 *
 * Manuel mapper — MapStruct kullanılsaydı şöyle olurdu:
 * @Mapper(componentModel = "spring")
 * public interface ProductMapper {
 *     @Mapping(source = "category.name", target = "categoryName")
 *     ProductResponse toResponse(Product product);
 * }
 *
 * Manuel yazmanın avantajı: hesaplanmış alan (inStock) gibi özel mantık eklenebilir.
 */
@Component
public class ProductMapper {

    /** Product entity → ProductResponse DTO */
    public ProductResponse toResponse(Product product) {
        if (product == null) return null;

        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .stockQuantity(product.getStockQuantity())
                .inStock(product.isInStock()) // Hesaplanmış alan — entity metodundan
                .categoryId(product.getCategory() != null ? product.getCategory().getId() : null)
                .categoryName(product.getCategory() != null ? product.getCategory().getName() : null)
                .imageUrl(product.getImageUrl())
                .sku(product.getSku())
                .brand(product.getBrand())
                .active(product.isActive())
                .variants(mapVariants(product.getVariants()))
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }

    /** Product list → ProductResponse list — stream kullanımı */
    public List<ProductResponse> toResponseList(List<Product> products) {
        if (products == null) return Collections.emptyList();
        return products.stream()
                .map(this::toResponse)  // Method reference — p -> toResponse(p) ile eşdeğer
                .collect(Collectors.toList());
    }

    /** ProductVariant list → VariantResponse list */
    private List<VariantResponse> mapVariants(List<ProductVariant> variants) {
        if (variants == null || variants.isEmpty()) return Collections.emptyList();
        return variants.stream()
                .map(this::toVariantResponse)
                .collect(Collectors.toList());
    }

    private VariantResponse toVariantResponse(ProductVariant variant) {
        return VariantResponse.builder()
                .id(variant.getId())
                .color(variant.getColor())
                .size(variant.getSize())
                .additionalPrice(variant.getAdditionalPrice())
                .stockQuantity(variant.getStockQuantity())
                .sku(variant.getSku())
                .build();
    }

    /** Product entity → Elasticsearch ProductDocument dönüşümü */
    public ProductDocument toDocument(Product product) {
        return ProductDocument.builder()
                .id(String.valueOf(product.getId()))
                .name(product.getName())
                .description(product.getDescription())
                .brand(product.getBrand())
                .categoryId(product.getCategory() != null ? product.getCategory().getId() : null)
                .categoryName(product.getCategory() != null ? product.getCategory().getName() : null)
                .price(product.getPrice())
                .stockQuantity(product.getStockQuantity())
                .active(product.isActive())
                .inStock(product.isInStock())
                .sku(product.getSku())
                .imageUrl(product.getImageUrl())
                .createdAt(product.getCreatedAt())
                .build();
    }
}
