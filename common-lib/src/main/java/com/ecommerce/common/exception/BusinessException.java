package com.ecommerce.common.exception;

import org.springframework.http.HttpStatus;

/**
 * İş kuralı ihlallerinde fırlatılır — genel amaçlı iş mantığı hatası.
 * HTTP 400 Bad Request döner.
 *
 * Kullanım: daha spesifik exception sınıfı yoksa bu kullanılır.
 * Örnek: "Sipariş zaten teslim edildi, iptal edilemez"
 */
public class BusinessException extends BaseException {

    public BusinessException(String message) {
        super(message, HttpStatus.BAD_REQUEST, "BUSINESS_ERROR");
    }

    public BusinessException(String message, String errorCode) {
        super(message, HttpStatus.BAD_REQUEST, errorCode);
    }
}
