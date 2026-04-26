package com.ecommerce.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Kimlik doğrulama başarısız olduğunda fırlatılır.
 * HTTP 401 Unauthorized döner.
 * Örnekler: yanlış şifre, geçersiz token, hesap pasif.
 */
public class AuthenticationException extends BaseException {

    public AuthenticationException(String message) {
        super(message, HttpStatus.UNAUTHORIZED, "AUTHENTICATION_FAILED");
    }
}
