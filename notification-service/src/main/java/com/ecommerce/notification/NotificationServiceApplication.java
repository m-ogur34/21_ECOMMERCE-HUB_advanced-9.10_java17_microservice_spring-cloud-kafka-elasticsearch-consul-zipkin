package com.ecommerce.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/** Notification Service ana uygulama sınıfı */
@SpringBootApplication(scanBasePackages = {
    "com.ecommerce.notification",
    "com.ecommerce.common"
})
@EnableJpaAuditing
public class NotificationServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
    }
}
