package com.ecommerce.order.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Resilience4J yapılandırması — hata toleransı pattern'ları.
 *
 * NOT: Bu bean yapılandırması application.yml'deki resilience4j.* ayarlarının
 * programatik alternatifidir. İkisi birlikte çalışabilir; yml önceliklidir.
 *
 * CircuitBreaker States (Devre Kesici Durumları):
 * ┌──────────┐  hata eşiği aşıldı  ┌──────────┐
 * │  CLOSED  │────────────────────►│   OPEN   │
 * │ (normal) │                     │(reddeder)│
 * └──────────┘                     └──────────┘
 *      ▲                                │
 *      │ test başarılı                  │ waitDuration sonra
 *      │                                ▼
 *      │                         ┌────────────┐
 *      └─────────────────────────│ HALF-OPEN  │
 *                                │  (test)    │
 *                                └────────────┘
 */
@Configuration
public class Resilience4jConfig {

    @Bean
    public CircuitBreakerConfig orderServiceCircuitBreakerConfig() {
        return CircuitBreakerConfig.custom()
                // Hata oranı bu eşiği geçerse devre açılır (OPEN)
                .failureRateThreshold(50)              // %50 hata → devre açık
                // Yavaş istek oranı eşiği
                .slowCallRateThreshold(80)             // %80 yavaş → devre açık
                // Yavaş istek tanımı
                .slowCallDurationThreshold(Duration.ofSeconds(3))
                // Eşiği hesaplamak için gereken minimum istek sayısı
                .minimumNumberOfCalls(10)
                // Sliding window: son N istekte hesapla
                .slidingWindowSize(20)
                // OPEN durumdan HALF-OPEN'a geçiş için bekleme
                .waitDurationInOpenState(Duration.ofSeconds(30))
                // HALF-OPEN'da izin verilen test isteği sayısı
                .permittedNumberOfCallsInHalfOpenState(3)
                .build();
    }

    @Bean
    public RetryConfig orderServiceRetryConfig() {
        return RetryConfig.custom()
                .maxAttempts(3)                              // Toplam 3 deneme (ilk + 2 retry)
                .waitDuration(Duration.ofMillis(500))        // Denemeler arası 500ms bekle
                // Hangi exception'larda retry yapılır?
                .retryExceptions(
                    java.io.IOException.class,               // Ağ hatası — retry mantıklı
                    org.apache.kafka.common.KafkaException.class // Kafka hatası
                )
                // Hangi exception'larda retry yapılmaz?
                .ignoreExceptions(
                    com.ecommerce.common.exception.BusinessException.class // İş kuralı hatası — retry anlamsız
                )
                .build();
    }

    @Bean
    public TimeLimiterConfig orderServiceTimeLimiterConfig() {
        return TimeLimiterConfig.custom()
                // Bu süre içinde yanıt gelmezse TimeoutException fırlatır
                .timeoutDuration(Duration.ofSeconds(5))
                .cancelRunningFuture(true) // Timeout olunca devam eden işlemi iptal et
                .build();
    }
}
