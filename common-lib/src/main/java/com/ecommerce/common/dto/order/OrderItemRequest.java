package com.ecommerce.common.dto.order;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Sipariş kalemi isteği DTO'su — bir siparişte birden fazla kalem olabilir */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemRequest {

    @NotNull(message = "Ürün ID boş olamaz")
    private Long productId;

    /** Varyant ID — opsiyonel, varyantsız ürünler için null */
    private Long variantId;

    @NotNull(message = "Miktar boş olamaz")
    @Min(value = 1, message = "Miktar en az 1 olmalıdır")
    private Integer quantity;
}
