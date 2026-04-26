package com.ecommerce.product.observer;

import com.ecommerce.product.model.Product;
import org.springframework.context.ApplicationEvent;

/**
 * Stok tükendiğinde yayınlanan Spring Application Event.
 * ApplicationEvent: Spring'in olay sistemi için temel sınıf.
 */
public class StockDepletedEvent extends ApplicationEvent {

    private final Product product;

    public StockDepletedEvent(Object source, Product product) {
        super(source); // source: olayı yayınlayan nesne
        this.product = product;
    }

    public Product getProduct() {
        return product;
    }
}
