package com.ecommerce.product.document;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Elasticsearch document sınıfı — ürün arama için.
 *
 * @Document: Bu sınıfın bir ES document (kayıt) olduğunu belirtir.
 * indexName: Elasticsearch'teki index (tablo karşılığı) adı.
 *
 * JPA Entity vs ES Document farkı:
 * - JPA Entity: ilişkisel DB'de tam veri, JOIN destekler
 * - ES Document: arama için optimize edilmiş, denormalize (JOIN yok)
 *
 * Denormalize örneği: category nesnesini gömlemek yerine categoryName string olarak saklıyoruz.
 * Bu ES'te JOIN yapmadan arama yapabilmemizi sağlar.
 *
 * Senkronizasyon: Ürün güncellendiğinde hem PostgreSQL (JPA) hem ES (bu document)
 * güncellenmelidir. Bu işlem servis katmanında yapılır.
 */
@Document(indexName = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductDocument {

    @Id // ES document ID — PostgreSQL'deki ürün ID'si ile aynı tutulur
    private String id;

    /**
     * @Field(type = FieldType.Text): Tam metin araması için analiz edilir.
     * Text: tokenize edilir (küçük harfe çevrilir, kök alınır vb.)
     * Keyword: tam eşleşme araması için (filtreleme, sıralama)
     * "analyzer = turkish": Türkçe dil analizi (eklerin kaldırılması)
     */
    @Field(type = FieldType.Text, analyzer = "turkish")
    private String name;

    @Field(type = FieldType.Text, analyzer = "turkish")
    private String description;

    /** Keyword: tam eşleşme ile filtreleme için (marka filtreleme) */
    @Field(type = FieldType.Keyword)
    private String brand;

    @Field(type = FieldType.Keyword)
    private String categoryName;

    @Field(type = FieldType.Long)
    private Long categoryId;

    /** Double: sayısal arama ve range sorguları için */
    @Field(type = FieldType.Double)
    private BigDecimal price;

    @Field(type = FieldType.Integer)
    private Integer stockQuantity;

    @Field(type = FieldType.Boolean)
    private boolean active;

    @Field(type = FieldType.Boolean)
    private boolean inStock;

    @Field(type = FieldType.Keyword)
    private String sku;

    @Field(type = FieldType.Text)
    private String imageUrl;

    @Field(type = FieldType.Date)
    private LocalDateTime createdAt;
}
