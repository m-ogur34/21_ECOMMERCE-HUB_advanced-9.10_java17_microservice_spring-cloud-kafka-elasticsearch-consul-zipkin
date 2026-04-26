package com.ecommerce.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Veritabanında veya cache'te aranan kaynak bulunamadığında fırlatılır.
 * HTTP 404 Not Found döner.
 *
 * Kullanım örnekleri:
 *   - productRepository.findById(id) → boş → throw new ResourceNotFoundException("Product", id)
 *   - userRepository.findByEmail(email) → boş → throw new ResourceNotFoundException("User", email)
 */
public class ResourceNotFoundException extends BaseException {

    private static final String ERROR_CODE = "RESOURCE_NOT_FOUND";

    /**
     * @param resourceName Bulunamayan kaynağın adı (örn: "Product", "User")
     * @param identifier   Aranılan değer (ID, e-posta vb.)
     */
    public ResourceNotFoundException(String resourceName, Object identifier) {
        super(
            String.format("%s bulunamadı: %s", resourceName, identifier),
            HttpStatus.NOT_FOUND,
            ERROR_CODE
        );
    }

    public ResourceNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND, ERROR_CODE);
    }
}
