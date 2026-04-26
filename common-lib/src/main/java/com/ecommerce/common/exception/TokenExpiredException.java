package com.ecommerce.common.exception;

import org.springframework.http.HttpStatus;

/**
 * JWT token süresi dolduğunda fırlatılır.
 * HTTP 401 döner, frontend refresh token akışını başlatmalıdır.
 */
public class TokenExpiredException extends BaseException {

    public TokenExpiredException() {
        super("Token süresi doldu. Lütfen yeniden giriş yapın.", HttpStatus.UNAUTHORIZED, "TOKEN_EXPIRED");
    }

    public TokenExpiredException(String message) {
        super(message, HttpStatus.UNAUTHORIZED, "TOKEN_EXPIRED");
    }
}
