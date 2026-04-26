package com.ecommerce.product.observer;

import com.ecommerce.product.model.Product;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Observer Pattern - Yayıncı (Publisher) bileşeni.
 *
 * Observer Pattern:
 * - Publisher (Subject): olay yayınlar
 * - Observer (Listener): olayı dinler ve tepki verir
 *
 * Spring Application Event sistemi Observer Pattern'in hazır implementasyonudur.
 * Kafka'ya göre avantajı: uygulama içi, senkron, transaction'a katılabilir.
 * Dezavantajı: başka uygulamalar dinleyemez (Kafka farklı uygulamalar arası).
 *
 * Kullanım: Stok sıfırlandığında admin'e uyarı vermek gibi dahili olaylar için.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductEventPublisher {

    private final ApplicationEventPublisher eventPublisher;

    /**
     * Stok tükendi olayını yayınlar.
     * @Transactional metot içinden çağrıldığında, event transaction commit'inden SONRA
     * işlenir (@TransactionalEventListener ile).
     */
    public void publishStockDepletedEvent(Product product) {
        log.info("Stok tükendi olayı yayınlanıyor: ürün={}", product.getName());
        StockDepletedEvent event = new StockDepletedEvent(this, product);
        eventPublisher.publishEvent(event);
    }

    public void publishProductCreatedEvent(Product product) {
        log.info("Ürün oluşturuldu olayı yayınlanıyor: ürün={}", product.getName());
        ProductCreatedEvent event = new ProductCreatedEvent(this, product);
        eventPublisher.publishEvent(event);
    }
}
