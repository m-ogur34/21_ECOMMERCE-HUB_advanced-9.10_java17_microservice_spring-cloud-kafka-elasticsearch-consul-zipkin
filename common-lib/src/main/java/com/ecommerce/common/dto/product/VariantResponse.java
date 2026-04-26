package com.ecommerce.common.dto.product;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Ürün varyantı yanıt DTO'su.
 * Örnek: Kırmızı / XL beden — aynı ürünün farklı seçenekleri.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VariantResponse {
    private Long id;
    private String color;
    private String size;
    private BigDecimal additionalPrice; // Ana fiyata eklenen ek fiyat
    private Integer stockQuantity;
    private String sku;
}
