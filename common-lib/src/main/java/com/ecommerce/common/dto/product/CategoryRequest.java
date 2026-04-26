package com.ecommerce.common.dto.product;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Kategori oluşturma/güncelleme isteği DTO'su */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryRequest {

    @NotBlank(message = "Kategori adı boş olamaz")
    @Size(min = 2, max = 100)
    private String name;

    private String description;

    /** Üst kategori ID — null ise kök (root) kategori */
    private Long parentId;
}
