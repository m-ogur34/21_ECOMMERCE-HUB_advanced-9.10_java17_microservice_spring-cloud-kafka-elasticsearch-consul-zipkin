package com.ecommerce.common.dto.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Sipariş oluşturma isteği DTO'su.
 *
 * @Valid annotasyonu: iç içe validation — items listesindeki her OrderItemRequest
 * da validate edilir. Nested validation için zorunludur.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderRequest {

    /** Teslimat adresi — tam adres tek alanda (şehir, posta kodu dahil) */
    @NotBlank(message = "Teslimat adresi boş olamaz")
    private String shippingAddress;

    /** Notlar — opsiyonel (kapı kodu, özel talimat vb.) */
    private String notes;

    /**
     * Sipariş kalemleri — en az 1 ürün olmalı.
     * @Valid ile her OrderItemRequest ayrıca doğrulanır.
     */
    @NotEmpty(message = "Sipariş en az bir ürün içermelidir")
    @Valid
    private List<OrderItemRequest> items;
}
