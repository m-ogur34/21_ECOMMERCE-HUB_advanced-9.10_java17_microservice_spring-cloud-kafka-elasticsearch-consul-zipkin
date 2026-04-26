package com.ecommerce.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Sipariş sırasında yeterli stok olmadığında fırlatılır.
 * HTTP 409 Conflict döner — kaynak var ama istekle çelişiyor.
 */
public class InsufficientStockException extends BaseException {

    public InsufficientStockException(Long productId, int requested, int available) {
        super(
            String.format("Ürün ID=%d için yetersiz stok. İstenen: %d, Mevcut: %d",
                productId, requested, available),
            HttpStatus.CONFLICT,
            "INSUFFICIENT_STOCK"
        );
    }

    public InsufficientStockException(String message) {
        super(message, HttpStatus.CONFLICT, "INSUFFICIENT_STOCK");
    }
}
