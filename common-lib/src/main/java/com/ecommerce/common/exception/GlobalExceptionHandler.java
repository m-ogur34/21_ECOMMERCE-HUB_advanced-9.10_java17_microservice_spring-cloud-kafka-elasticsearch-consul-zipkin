package com.ecommerce.common.exception;

import com.ecommerce.common.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.util.HashMap;
import java.util.Map;

/**
 * Merkezi hata yönetim sınıfı — tüm controller'larda fırlatılan exception'ları yakalar.
 *
 * @RestControllerAdvice = @ControllerAdvice + @ResponseBody
 * Spring AOP mekanizması: exception fırlatıldığında Spring bu sınıftaki
 * ilgili @ExceptionHandler metodunu otomatik olarak çağırır.
 *
 * Bu yaklaşımın avantajları:
 * - Her controller'da try-catch yazmak gerekmez
 * - Tutarlı hata yanıt formatı sağlanır
 * - Hata loglama merkezi yapılır
 * - HTTP durum kodları tek yerden yönetilir
 */
@Slf4j               // Lombok: private static final Logger log = LoggerFactory.getLogger(...)
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Projemizin tüm özel exception'larını yakalar.
     * BaseException'dan türeyen: ResourceNotFoundException, BusinessException,
     * InsufficientStockException, AuthenticationException, TokenExpiredException
     *
     * OOP - Polymorphism: BaseException tipiyle tüm alt tipler yakalanır.
     */
    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ApiResponse<Void>> handleBaseException(
            BaseException ex, WebRequest request) {

        // Hata detayını server loguna yaz — stack trace dahil
        log.error("İş mantığı hatası: {} | URL: {} | Kod: {}",
                ex.getMessage(), request.getDescription(false), ex.getErrorCode());

        ApiResponse<Void> response = ApiResponse.error(ex.getMessage(), ex.getErrorCode());
        return ResponseEntity.status(ex.getHttpStatus()).body(response);
    }

    /**
     * @Valid veya @Validated ile tetiklenen validation hatalarını yakalar.
     * Örnek: RegisterRequest'te email formatı yanlışsa buraya düşer.
     * HTTP 400 döner ve hangi alan neden hatalı olduğunu gösterir.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationException(
            MethodArgumentNotValidException ex) {

        // Her hatalı alan için field_name → error_message eşleşmesi oluştur
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField(); // Hatalı alan adı
            String errorMessage = error.getDefaultMessage();     // @NotBlank mesajı vb.
            errors.put(fieldName, errorMessage);
        });

        log.warn("Validation hatası: {}", errors);

        ApiResponse<Map<String, String>> response = ApiResponse.<Map<String, String>>builder()
                .success(false)
                .message("Girdi doğrulama hatası")
                .data(errors)          // Hangi alanların hatalı olduğu
                .errorCode("VALIDATION_ERROR")
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Beklenmeyen tüm exception'ları yakalar — son güvenlik ağı.
     * Detaylı hata bilgisini dışarıya sızdırmaz (güvenlik!), sadece genel mesaj döner.
     * Gerçek hata detayı server logunda bulunur.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(
            Exception ex, WebRequest request) {

        // Stack trace ile tam detayı logla — izleme sistemi (Zipkin) için
        log.error("Beklenmeyen hata: {} | URL: {}",
                ex.getMessage(), request.getDescription(false), ex);

        ApiResponse<Void> response = ApiResponse.error(
                "Sunucu hatası oluştu. Lütfen daha sonra tekrar deneyin.",
                "INTERNAL_SERVER_ERROR"
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
