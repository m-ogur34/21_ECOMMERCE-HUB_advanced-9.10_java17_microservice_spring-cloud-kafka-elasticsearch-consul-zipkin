package com.ecommerce.product;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Product Service ana uygulama sınıfı.
 *
 * @EnableAsync: @Async ile işaretli metodlar ayrı thread'de çalışır.
 * StockObserver'daki async olay işleyicileri için gerekli.
 */
@SpringBootApplication(scanBasePackages = {
    "com.ecommerce.product",
    "com.ecommerce.common"
})
@EnableJpaAuditing
@EnableAsync
public class ProductServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProductServiceApplication.class, args);
    }
}
