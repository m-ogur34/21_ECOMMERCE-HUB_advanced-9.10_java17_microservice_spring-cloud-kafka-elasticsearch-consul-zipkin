package com.ecommerce.order;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Order Service ana uygulama sınıfı.
 * @EnableFeignClients: OpenFeign HTTP istemci arayüzlerini aktifleştirir.
 */
@SpringBootApplication(scanBasePackages = {
    "com.ecommerce.order",
    "com.ecommerce.common"
})
@EnableJpaAuditing
@EnableFeignClients
public class OrderServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }
}
