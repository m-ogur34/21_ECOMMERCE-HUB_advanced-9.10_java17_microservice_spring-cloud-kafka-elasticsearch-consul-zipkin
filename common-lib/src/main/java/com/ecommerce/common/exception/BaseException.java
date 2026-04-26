package com.ecommerce.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Tüm özel exception'ların kalıtım aldığı temel soyut sınıf.
 *
 * OOP - Inheritance (Kalıtım) örneği:
 * BaseException (abstract)
 *   ├── BusinessException       → 400 Bad Request
 *   ├── ResourceNotFoundException → 404 Not Found
 *   ├── InsufficientStockException → 409 Conflict
 *   ├── AuthenticationException → 401 Unauthorized
 *   └── TokenExpiredException  → 401 Unauthorized
 *
 * OOP - Encapsulation (Kapsülleme) örneği:
 * httpStatus ve errorCode alanları private, getter ile erişilir.
 *
 * Neden RuntimeException extend edilir?
 * - Checked exception (Exception) olsaydı her metotta throws bildirimi gerekirdi.
 * - Spring @ExceptionHandler zaten RuntimeException'ları yakalar.
 * - Modern Java pratiğinde unchecked exception tercih edilir.
 */
@Getter
public abstract class BaseException extends RuntimeException {

    /** HTTP durum kodu — GlobalExceptionHandler bu kodu response'a yazar */
    private final HttpStatus httpStatus;

    /**
     * İş mantığı hata kodu — frontend bunu göre farklı işlem yapabilir.
     * Örnek: "PRODUCT_NOT_FOUND", "INSUFFICIENT_STOCK", "TOKEN_EXPIRED"
     */
    private final String errorCode;

    /**
     * Temel constructor — tüm alt sınıflar super() ile çağırır.
     *
     * @param message    İnsan okunabilir hata mesajı
     * @param httpStatus HTTP yanıt kodu
     * @param errorCode  İş mantığı hata kodu
     */
    protected BaseException(String message, HttpStatus httpStatus, String errorCode) {
        super(message); // RuntimeException'a mesajı ilet — getMessage() için
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
    }

    /**
     * Zincir exception için ek constructor.
     * Orijinal hatayı kaybetmeden sarmalamak için kullanılır (cause zinciri).
     */
    protected BaseException(String message, HttpStatus httpStatus, String errorCode, Throwable cause) {
        super(message, cause);
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
    }
}
