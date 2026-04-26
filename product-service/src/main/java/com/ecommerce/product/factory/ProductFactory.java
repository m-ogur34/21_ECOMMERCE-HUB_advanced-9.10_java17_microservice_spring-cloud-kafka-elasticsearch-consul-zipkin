package com.ecommerce.product.factory;

import com.ecommerce.product.model.Category;
import com.ecommerce.product.model.Product;
import com.ecommerce.product.model.ProductVariant;
import com.ecommerce.common.dto.product.ProductRequest;
import com.ecommerce.common.dto.product.VariantRequest;
import org.springframework.stereotype.Component;

/**
 * Ürün nesnesi oluşturma fabrikası — Factory Pattern.
 *
 * Factory Pattern: Nesne oluşturma mantığını merkezi bir yere toplar.
 * Servis sınıfları "nasıl oluşturulacağını" bilmek zorunda kalmaz.
 *
 * Faydaları:
 * - Nesne oluşturma mantığı değişirse sadece factory güncellenir
 * - Servis sınıfları sadece "Product oluştur" der, detaylarla uğraşmaz
 * - Test yazarken mock factory kullanılabilir
 *
 * Creational Patterns kategorisindedir (GoF Design Patterns).
 */
@Component
public class ProductFactory {

    /**
     * ProductRequest DTO'sundan Product entity'si oluşturur.
     * Kategori ayrıca inject edilir çünkü ID'den nesneye dönüşüm servis katmanında yapılır.
     *
     * @param request  Frontend'den gelen ürün bilgileri
     * @param category Kategori entity (DB'den yüklenmiş)
     * @return Henüz kaydedilmemiş Product nesnesi
     */
    public Product createProduct(ProductRequest request, Category category) {
        Product product = new Product();
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStockQuantity(request.getStockQuantity());
        product.setSku(request.getSku());
        product.setBrand(request.getBrand());
        product.setImageUrl(request.getImageUrl());
        product.setActive(request.isActive());
        product.setCategory(category);
        return product;
    }

    /**
     * VariantRequest'ten ProductVariant entity'si oluşturur.
     *
     * @param request Varyant bilgileri
     * @param product Ana ürün
     * @return Henüz kaydedilmemiş ProductVariant nesnesi
     */
    public ProductVariant createVariant(VariantRequest request, Product product) {
        ProductVariant variant = new ProductVariant();
        variant.setName(product.getName() + " - " + request.getColor() + "/" + request.getSize());
        variant.setPrice(product.getPrice().add(request.getAdditionalPrice())); // Toplam fiyat
        variant.setStockQuantity(request.getStockQuantity());
        variant.setSku(request.getSku());
        variant.setColor(request.getColor());
        variant.setSize(request.getSize());
        variant.setAdditionalPrice(request.getAdditionalPrice());
        variant.setProduct(product);
        return variant;
    }
}
