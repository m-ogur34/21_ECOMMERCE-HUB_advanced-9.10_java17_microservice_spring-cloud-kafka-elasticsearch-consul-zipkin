package com.ecommerce.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Tüm REST API yanıtları için standart sarmalayıcı (wrapper) sınıf.
 *
 * OOP: Generic tip parametresi <T> kullanılarak herhangi bir veri tipi taşınabilir.
 * Bu, polymorphism'in generic programming ile birleşimidir.
 *
 * Kullanım örnekleri:
 *   ApiResponse<ProductResponse> → tek ürün
 *   ApiResponse<List<ProductResponse>> → ürün listesi
 *   ApiResponse<Void> → veri içermeyen başarı yanıtı (örn: silme işlemi)
 *
 * @param <T> Yanıt gövdesindeki verinin tipi
 */
@Data                    // Lombok: getter, setter, equals, hashCode, toString üretir
@Builder                 // Lombok: Builder pattern — ApiResponse.builder().success(true)...build()
@NoArgsConstructor       // Jackson deserialize için parametresiz constructor
@AllArgsConstructor      // Builder için tüm alanlı constructor
@JsonInclude(JsonInclude.Include.NON_NULL) // null alanlar JSON çıktısına eklenmez
public class ApiResponse<T> {

    /** İşlemin başarılı olup olmadığı */
    private boolean success;

    /** İnsan okunabilir mesaj — başarı veya hata açıklaması */
    private String message;

    /** Asıl yanıt verisi — hata durumunda null olabilir */
    private T data;

    /** Hata kodu — sadece hata yanıtlarında dolu olur */
    private String errorCode;

    /** Yanıt oluşturulma zamanı — loglama ve debug için */
    @Builder.Default // Builder kullanıldığında bu alan otomatik set edilir
    private LocalDateTime timestamp = LocalDateTime.now();

    // ===== STATIK FACTORY METODLARI =====
    // Factory Method pattern: nesne oluşturmayı kapsüller, isimli constructor gibi çalışır

    /**
     * Başarı yanıtı oluşturur — veri ve mesaj ile.
     * Kullanım: ApiResponse.success(productDto, "Ürün başarıyla getirildi")
     */
    public static <T> ApiResponse<T> success(T data, String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .build();
    }

    /**
     * Başarı yanıtı oluşturur — sadece mesaj ile (veri yok).
     * Kullanım: silme, güncelleme gibi veri dönmeyen işlemlerde
     */
    public static <T> ApiResponse<T> success(String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .build();
    }

    /**
     * Hata yanıtı oluşturur — mesaj ve hata kodu ile.
     * GlobalExceptionHandler bu metodu kullanır.
     */
    public static <T> ApiResponse<T> error(String message, String errorCode) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .errorCode(errorCode)
                .build();
    }
}
