package com.ecommerce.order.service.saga;

import com.ecommerce.common.dto.order.OrderRequest;
import com.ecommerce.order.model.Order;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Sipariş Saga Orkestratörü.
 *
 * Saga Pattern - Orchestration yaklaşımı:
 * Bu sınıf tüm saga adımlarını koordine eder.
 * Her adım başarılıysa bir sonrakine geçer.
 * Herhangi bir adım başarısız olursa önceki tüm adımların compensate() metodu çalışır.
 *
 * Adımlar sırayla çalışır: CreateOrder → ReserveStock
 *
 * Compensating Transaction örneği:
 * Step 1 ✓ (sipariş oluşturuldu)
 * Step 2 ✗ (stok yetersiz)
 * → Step 1 compensate() çalışır (sipariş iptal edilir)
 *
 * Bu sayede veri tutarlılığı sağlanır — tüm adımlar başarılı ya da hiçbiri.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderSaga {

    private final CreateOrderStep createOrderStep;
    private final ReserveStockStep reserveStockStep;

    /**
     * Saga'yı çalıştırır.
     * Başarılı olursa oluşturulan siparişi döner.
     * Başarısız olursa tamamlanan adımlar telafi edilir ve exception fırlatılır.
     *
     * @param request   Sipariş isteği
     * @param userId    Siparişi veren kullanıcı
     * @param userEmail Kullanıcı e-postası
     * @return Oluşturulan sipariş
     */
    public Order execute(OrderRequest request, Long userId, String userEmail) {
        log.info("Sipariş Saga başlatılıyor - kullanıcı={}", userId);

        // Saga bağlamını oluştur — adımlar arası veri taşınır
        SagaContext context = SagaContext.builder()
                .orderRequest(request)
                .userId(userId)
                .userEmail(userEmail)
                .build();

        // Tamamlanan adımları takip et — rollback sırasında ters sırada compensate edilecek
        List<SagaStep<SagaContext>> completedSteps = new ArrayList<>();

        // ===== ADIM 1: Sipariş oluştur =====
        try {
            createOrderStep.execute(context);
            completedSteps.add(createOrderStep); // Başarılıysa listeye ekle
        } catch (Exception e) {
            log.error("Saga Adım 1 başarısız: {}", e.getMessage());
            rollback(completedSteps, context, "Sipariş oluşturulamadı: " + e.getMessage());
            throw new RuntimeException("Sipariş oluşturulamadı", e);
        }

        // ===== ADIM 2: Stok rezerve et =====
        try {
            reserveStockStep.execute(context);
            completedSteps.add(reserveStockStep);
        } catch (Exception e) {
            log.error("Saga Adım 2 başarısız: {}", e.getMessage());
            rollback(completedSteps, context, "Stok rezervasyonu başarısız: " + e.getMessage());
            throw new RuntimeException("Stok rezervasyonu başarısız", e);
        }

        context.setCompleted(true);
        log.info("Sipariş Saga tamamlandı: sipariş={}",
                context.getCreatedOrder().getOrderNumber());

        return context.getCreatedOrder();
    }

    /**
     * Tamamlanan adımları ters sırada compensate et.
     * LIFO (Last In First Out) sırası: son tamamlanan adım ilk geri alınır.
     *
     * Örnek: Step1 ✓, Step2 ✓, Step3 ✗
     * Rollback: Step2.compensate() → Step1.compensate()
     */
    private void rollback(List<SagaStep<SagaContext>> completedSteps,
                          SagaContext context, String reason) {
        log.warn("Saga rollback başlatılıyor: {}", reason);
        context.setFailureReason(reason);

        // Ters sırada compensate et
        for (int i = completedSteps.size() - 1; i >= 0; i--) {
            try {
                completedSteps.get(i).compensate(context);
            } catch (Exception e) {
                // Compensate hatası loglanır ama saga'yı durdurmaz
                // Gerçek uygulamada: DLQ (Dead Letter Queue) veya manuel müdahale
                log.error("Compensate hatası - adım {}: {}", i, e.getMessage());
            }
        }

        log.warn("Saga rollback tamamlandı");
    }
}
