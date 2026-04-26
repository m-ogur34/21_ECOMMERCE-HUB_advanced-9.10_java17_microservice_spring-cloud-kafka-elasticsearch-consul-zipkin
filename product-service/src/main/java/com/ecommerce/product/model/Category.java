package com.ecommerce.product.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Ürün kategorisi entity sınıfı.
 * Self-referencing (öz referanslı) ilişki: bir kategori, alt kategorilere sahip olabilir.
 * Örnek: Elektronik → Telefonlar → Akıllı Telefonlar
 */
@Entity
@Table(name = "categories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, unique = true, length = 100)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    /**
     * Üst kategori — null ise kök kategori.
     * @ManyToOne: birçok kategori aynı üst kategoriye sahip olabilir.
     * FetchType.LAZY: üst kategori sorgulama sırasında yüklenmez, ilk erişimde yüklenir.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Category parent;

    /**
     * Alt kategoriler listesi.
     * mappedBy: ilişkiyi "parent" alanı yönetir (sahip taraf).
     * cascade: kategori silindiğinde alt kategoriler de silinir.
     * CascadeType.ALL yerine PERSIST ve MERGE kullanmak daha güvenli.
     */
    @OneToMany(mappedBy = "parent", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @Builder.Default
    private List<Category> children = new ArrayList<>();

    /** Bu kategorideki ürünler */
    @OneToMany(mappedBy = "category")
    @Builder.Default
    private List<Product> products = new ArrayList<>();

    /** Kök kategori mi? (üst kategorisi yok) */
    public boolean isRoot() {
        return parent == null;
    }
}
