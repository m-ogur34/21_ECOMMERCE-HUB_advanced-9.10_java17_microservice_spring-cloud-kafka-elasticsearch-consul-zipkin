package com.ecommerce.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Auth Service ana uygulama sınıfı.
 *
 * @SpringBootApplication üç annotasyonu birden içerir:
 * - @SpringBootConfiguration: Spring Boot yapılandırması
 * - @EnableAutoConfiguration: classpath'e göre bean'leri otomatik yapılandırır
 *   (PostgreSQL driver + JPA → otomatik DataSource oluşturur)
 * - @ComponentScan: bu paket ve alt paketlerdeki @Component, @Service, @Repository'leri tarar
 *
 * @EnableJpaAuditing: BaseEntity'deki @CreatedDate ve @LastModifiedDate'i aktifleştirir.
 *
 * @EnableScheduling: @Scheduled annotasyonlu metodları (örn: token temizleme) etkinleştirir.
 */
@SpringBootApplication(scanBasePackages = {
    "com.ecommerce.auth",    // Auth servisi sınıfları
    "com.ecommerce.common"   // common-lib (GlobalExceptionHandler vb.)
})
@EnableJpaAuditing
@EnableScheduling
public class AuthServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }
}
