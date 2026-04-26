package com.ecommerce.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * API Gateway ana uygulama sınıfı.
 *
 * ÖNEMLİ: Bu sınıfta @EnableJpaAuditing veya @EnableScheduling OLMAMALI.
 * Gateway JPA kullanmaz — veritabanı bağlantısı gerektirmez (rate limiting için Redis hariç).
 * spring-boot-starter-web yerine spring-cloud-starter-gateway var — WebFlux tabanlı.
 */
@SpringBootApplication(scanBasePackages = {
    "com.ecommerce.gateway",
    "com.ecommerce.common"
})
public class ApiGatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
