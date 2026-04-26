package com.ecommerce.common.dto.product;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/** Ürün varyantı oluşturma isteği DTO'su */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VariantRequest {

    private String color;
    private String size;

    @DecimalMin(value = "0.0")
    private BigDecimal additionalPrice;

    @Min(0)
    private Integer stockQuantity;

    @NotBlank
    private String sku;
}
