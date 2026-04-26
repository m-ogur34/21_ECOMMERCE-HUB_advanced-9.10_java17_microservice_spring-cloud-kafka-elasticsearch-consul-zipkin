package com.ecommerce.common.dto.product;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Ürün oluşturma ve güncelleme isteği için DTO.
 *
 * BigDecimal kullanım sebebi: float/double para değerleri için tehlikelidir.
 * Örneğin 0.1 + 0.2 = 0.30000000000000004 olabilir — finansal hesaplamalarda
 * bu hata kabul edilemez. BigDecimal kesin ondalık aritmetik sağlar.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductRequest {

    @NotBlank(message = "Ürün adı boş olamaz")
    @Size(min = 2, max = 200, message = "Ürün adı 2-200 karakter arasında olmalıdır")
    private String name;

    @Size(max = 2000, message = "Açıklama en fazla 2000 karakter olabilir")
    private String description;

    /** Para birimi için BigDecimal — ondalık hassasiyeti korur */
    @NotNull(message = "Fiyat boş olamaz")
    @DecimalMin(value = "0.01", message = "Fiyat 0'dan büyük olmalıdır")
    @Digits(integer = 10, fraction = 2, message = "Fiyat en fazla 10 tam 2 ondalık basamak olabilir")
    private BigDecimal price;

    @NotNull(message = "Stok miktarı boş olamaz")
    @Min(value = 0, message = "Stok miktarı negatif olamaz")
    private Integer stockQuantity;

    @NotNull(message = "Kategori ID boş olamaz")
    private Long categoryId;

    /** Ürün görseli URL — opsiyonel */
    private String imageUrl;

    /** SKU (Stock Keeping Unit): ürünün benzersiz stok kodu */
    @NotBlank(message = "SKU boş olamaz")
    private String sku;

    /** Marka adı */
    private String brand;

    /** Aktif mi? false ise ürün listede görünmez */
    @Builder.Default
    private boolean active = true;
}
