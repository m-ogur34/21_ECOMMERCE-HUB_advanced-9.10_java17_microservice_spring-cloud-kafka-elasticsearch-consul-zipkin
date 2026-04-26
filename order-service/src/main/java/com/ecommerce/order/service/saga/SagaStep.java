package com.ecommerce.order.service.saga;

/**
 * Saga adımı arayüzü — Strategy + Command pattern birleşimi.
 *
 * Saga Pattern nedir?
 * Mikroservisler arası distributed transaction yönetimi için kullanılır.
 * Klasik 2PC (Two-Phase Commit) yerine asenkron, telafi tabanlı bir yaklaşım.
 *
 * Choreography vs Orchestration:
 * - Choreography: her servis olayları dinler ve kendi kararını verir
 * - Orchestration: merkezi koordinatör her adımı yönetir (bu proje)
 *
 * OrderSaga sırası:
 * 1. CreateOrderStep  → siparişi DB'ye yaz (PENDING)
 * 2. ReserveStockStep → Kafka ile stok rezerve et
 * 3. ConfirmOrderStep → siparişi CONFIRMED yap
 *
 * Her adım başarısız olursa compensate() çalışır (geri alma işlemi).
 *
 * @param <T> Adımın giriş veri tipi
 */
public interface SagaStep<T> {

    /**
     * Adımı çalıştır.
     * @param context Adımın ihtiyaç duyduğu bağlam verisi
     * @throws Exception Adım başarısız olursa
     */
    void execute(T context) throws Exception;

    /**
     * Telafi işlemi — adım başarısız olunca veya sonraki adım başarısız olunca çalışır.
     * Distributed transaction'ı geri almak için compensating transaction.
     * @param context Aynı bağlam verisi
     */
    void compensate(T context);
}
