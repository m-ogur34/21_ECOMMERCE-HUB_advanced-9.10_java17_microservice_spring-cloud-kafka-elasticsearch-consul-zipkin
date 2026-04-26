package com.ecommerce.product.observer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Stok olaylarını dinleyen Observer bileşeni.
 *
 * Observer Pattern - Gözlemci (Observer) bileşeni.
 * @EventListener: Spring bu sınıfı otomatik olarak publisher'ın listener'ı olarak kaydeder.
 *
 * @TransactionalEventListener:
 * - AFTER_COMMIT: transaction başarıyla tamamlandıktan SONRA çalışır.
 * - Bu sayede DB rollback olursa bildirim gönderilmez.
 * - Önemli: bu event listener transaction dışında çalışır, yeni transaction lazımsa
 *   @Transactional(propagation = REQUIRES_NEW) kullanılmalı.
 *
 * @Async: Bu işlemi ayrı bir thread'de çalıştır — ana thread'i bloklama.
 * @EnableAsync ana uygulama sınıfında açılmalıdır.
 */
@Slf4j
@Component
public class StockObserver {

    /**
     * Stok tükendi olayını işler.
     * Gerçek uygulamada: e-posta gönderme, admin paneli bildirimi vb.
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleStockDepletedEvent(StockDepletedEvent event) {
        log.warn("UYARI: Stok tükendi! Ürün: {} | SKU: {} | Stok: {}",
                event.getProduct().getName(),
                event.getProduct().getSku(),
                event.getProduct().getStockQuantity());

        // Gerçek uygulamada: admin'e e-posta gönder, sipariş kuyruğunu durdur vb.
        // notificationService.sendLowStockAlert(event.getProduct());
    }

    /**
     * Yeni ürün oluşturuldu olayını işler.
     * Elasticsearch index'ini günceller.
     */
    @Async
    @EventListener // @TransactionalEventListener değil — transaction gerekmez
    public void handleProductCreatedEvent(ProductCreatedEvent event) {
        log.info("Yeni ürün ES index'ine eklenecek: {}", event.getProduct().getName());
        // searchService.indexProduct(event.getProduct()); — döngüsel bağımlılık önlemek için
        // Bu örnekte loglama yeterli; gerçekte SearchService inject edilebilir.
    }
}
