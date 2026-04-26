package com.ecommerce.common.dto.product;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Ürün yanıt DTO'su — API'den istemciye döner.
 *
 * Entity'de olmayan hesaplanmış alanlar (örn: discountedPrice) buraya eklenebilir.
 * Bu sayede entity değişmeden API kontratı zenginleştirilebilir.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductResponse {

    private Long id;
    private String name;
    private String description;
    private BigDecimal price;

    /** Stok durumu — hassas stok sayısı yerine boolean daha güvenli olabilir */
    private Integer stockQuantity;
    private boolean inStock;

    private Long categoryId;
    private String categoryName;
    private String imageUrl;
    private String sku;
    private String brand;
    private boolean active;

    /** Varyantlar — opsiyonel, sadece varyantlı ürünlerde dolu olur */
    private List<VariantResponse> variants;

    /** Oluşturulma tarihi — @JsonInclude sayesinde null ise JSON'a eklenmez */
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
