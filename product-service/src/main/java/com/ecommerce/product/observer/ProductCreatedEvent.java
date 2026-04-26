package com.ecommerce.product.observer;

import com.ecommerce.product.model.Product;
import org.springframework.context.ApplicationEvent;

/** Yeni ürün oluşturulduğunda yayınlanan Spring Application Event */
public class ProductCreatedEvent extends ApplicationEvent {

    private final Product product;

    public ProductCreatedEvent(Object source, Product product) {
        super(source);
        this.product = product;
    }

    public Product getProduct() {
        return product;
    }
}
