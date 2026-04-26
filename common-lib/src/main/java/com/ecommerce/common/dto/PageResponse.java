package com.ecommerce.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Sayfalanmış (paginated) API yanıtları için sarmalayıcı sınıf.
 *
 * Spring Data'nın Page<T> nesnesi frontend'e doğrudan gönderilemez çünkü
 * Spring'e özgü serileştirme sorunları yaratır. Bu DTO, Page'i temiz bir
 * yapıya dönüştürür.
 *
 * @param <T> Sayfa içindeki elemanların tipi
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageResponse<T> {

    /** Mevcut sayfadaki elemanlar */
    private List<T> content;

    /** Mevcut sayfa numarası (0'dan başlar) */
    private int pageNumber;

    /** Sayfa başına eleman sayısı */
    private int pageSize;

    /** Toplam eleman sayısı (tüm sayfalardaki) */
    private long totalElements;

    /** Toplam sayfa sayısı */
    private int totalPages;

    /** İlk sayfa mı? */
    private boolean first;

    /** Son sayfa mı? */
    private boolean last;

    /**
     * Spring Data Page<T> nesnesinden PageResponse<T> üretir.
     * Static factory method — kullanım kolaylığı sağlar.
     *
     * Kullanım: PageResponse.from(productPage)
     */
    public static <T> PageResponse<T> from(Page<T> page) {
        return PageResponse.<T>builder()
                .content(page.getContent())           // Sayfadaki elemanlar
                .pageNumber(page.getNumber())          // 0 tabanlı sayfa numarası
                .pageSize(page.getSize())              // İstenen sayfa boyutu
                .totalElements(page.getTotalElements()) // DB'deki toplam kayıt sayısı
                .totalPages(page.getTotalPages())       // Math.ceil(total/size)
                .first(page.isFirst())                 // pageNumber == 0
                .last(page.isLast())                   // pageNumber == totalPages - 1
                .build();
    }
}
