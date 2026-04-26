package com.ecommerce.order.service.saga;

import com.ecommerce.common.dto.order.OrderItemRequest;
import com.ecommerce.common.dto.order.OrderRequest;
import com.ecommerce.order.model.Order;
import com.ecommerce.order.model.OrderStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * OrderSaga birim testi.
 *
 * Test Stratejisi:
 * - Her adımın bağımsız olarak doğrulanması
 * - Başarılı Saga akışı
 * - Adım 1 başarısız → compensate çalışmamalı (tamamlanmış adım yok)
 * - Adım 2 başarısız → Adım 1 compensate edilmeli (LIFO)
 *
 * @ExtendWith(MockitoExtension): Mockito anotasyonlarını (@Mock, @InjectMocks) aktifleştirir.
 * Spring context yüklenmez → hızlı birim testi.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OrderSaga Testleri")
class OrderSagaTest {

    @Mock
    private CreateOrderStep createOrderStep;

    @Mock
    private ReserveStockStep reserveStockStep;

    @InjectMocks
    private OrderSaga orderSaga;

    private OrderRequest orderRequest;
    private final Long userId = 1L;
    private final String userEmail = "test@example.com";

    @BeforeEach
    void setUp() {
        // Her test için temiz bir OrderRequest oluştur
        OrderItemRequest item = new OrderItemRequest();
        item.setProductId(1L);
        item.setQuantity(2);

        orderRequest = new OrderRequest();
        orderRequest.setItems(List.of(item));
        orderRequest.setShippingAddress("Test Adres, İstanbul");
    }

    @Nested
    @DisplayName("Başarılı Saga Akışı")
    class SuccessScenarios {

        @Test
        @DisplayName("Tüm adımlar başarılıysa sipariş döner")
        void execute_allStepsSucceed_returnsOrder() throws Exception {
            // GIVEN: Her iki adım da başarıyla tamamlanır
            // CreateOrderStep, context'e Order nesnesini set eder
            Order mockOrder = Order.builder()
                    .id(1L)
                    .orderNumber("ORD-20240101-001")
                    .status(OrderStatus.PENDING)
                    .build();

            doAnswer(invocation -> {
                SagaContext ctx = invocation.getArgument(0);
                ctx.setCreatedOrder(mockOrder);
                return null;
            }).when(createOrderStep).execute(any(SagaContext.class));

            doNothing().when(reserveStockStep).execute(any(SagaContext.class));

            // WHEN
            Order result = orderSaga.execute(orderRequest, userId, userEmail);

            // THEN
            assertThat(result).isNotNull();
            assertThat(result.getOrderNumber()).isEqualTo("ORD-20240101-001");

            // Her adım tam olarak bir kez çağrılmalı
            verify(createOrderStep, times(1)).execute(any(SagaContext.class));
            verify(reserveStockStep, times(1)).execute(any(SagaContext.class));

            // Başarılıysa compensate çağrılmamalı
            verify(createOrderStep, never()).compensate(any(SagaContext.class));
            verify(reserveStockStep, never()).compensate(any(SagaContext.class));
        }
    }

    @Nested
    @DisplayName("Hata Senaryoları ve Rollback")
    class FailureAndRollbackScenarios {

        @Test
        @DisplayName("Adım 1 başarısız olunca exception fırlatılır, compensate çağrılmaz")
        void execute_step1Fails_throwsExceptionWithoutCompensate() throws Exception {
            // GIVEN: CreateOrderStep exception fırlatır
            doThrow(new RuntimeException("DB bağlantı hatası"))
                    .when(createOrderStep).execute(any(SagaContext.class));

            // WHEN + THEN
            assertThatThrownBy(() -> orderSaga.execute(orderRequest, userId, userEmail))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Sipariş oluşturulamadı");

            // Adım 1 başarısız → henüz tamamlanan adım yok → compensate çağrılmamalı
            verify(createOrderStep, never()).compensate(any(SagaContext.class));
            // Adım 2 hiç çalışmamalı
            verify(reserveStockStep, never()).execute(any(SagaContext.class));
            verify(reserveStockStep, never()).compensate(any(SagaContext.class));
        }

        @Test
        @DisplayName("Adım 2 başarısız olunca Adım 1 LIFO ile compensate edilir")
        void execute_step2Fails_step1IsCompensated() throws Exception {
            // GIVEN: Adım 1 başarılı, Adım 2 exception fırlatır
            Order mockOrder = Order.builder()
                    .id(1L)
                    .orderNumber("ORD-20240101-002")
                    .status(OrderStatus.PENDING)
                    .build();

            doAnswer(invocation -> {
                SagaContext ctx = invocation.getArgument(0);
                ctx.setCreatedOrder(mockOrder);
                return null;
            }).when(createOrderStep).execute(any(SagaContext.class));

            doThrow(new RuntimeException("Kafka bağlantı hatası"))
                    .when(reserveStockStep).execute(any(SagaContext.class));

            // WHEN + THEN
            assertThatThrownBy(() -> orderSaga.execute(orderRequest, userId, userEmail))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Stok rezervasyonu başarısız");

            // Adım 2 başarısız → Adım 1 compensate edilmeli (LIFO)
            verify(createOrderStep, times(1)).compensate(any(SagaContext.class));

            // Adım 2 çalışmadı → Adım 2 compensate edilmemeli
            verify(reserveStockStep, never()).compensate(any(SagaContext.class));
        }

        @Test
        @DisplayName("Compensate kendisi de hata fırlatırsa Saga yine de tamamlanır")
        void execute_compensateFails_sagaStillCompletes() throws Exception {
            // GIVEN: Adım 1 başarılı ama compensate'i de exception fırlatır
            //        Adım 2 başarısız
            Order mockOrder = Order.builder().id(1L).orderNumber("ORD-TEST").build();

            doAnswer(invocation -> {
                ((SagaContext) invocation.getArgument(0)).setCreatedOrder(mockOrder);
                return null;
            }).when(createOrderStep).execute(any(SagaContext.class));

            // Adım 2 başarısız
            doThrow(new RuntimeException("Stok yetersiz"))
                    .when(reserveStockStep).execute(any(SagaContext.class));

            // Adım 1'in compensate'i de hata verirse → Saga durmamalı, log yazılmalı
            doThrow(new RuntimeException("DB erişilemiyor"))
                    .when(createOrderStep).compensate(any(SagaContext.class));

            // WHEN + THEN: Exception fırlatılmalı ama compensate hatası bunu etkilemez
            assertThatThrownBy(() -> orderSaga.execute(orderRequest, userId, userEmail))
                    .isInstanceOf(RuntimeException.class);

            // Compensate çağrıldı ama exception yutuldu (log yazıldı)
            verify(createOrderStep).compensate(any(SagaContext.class));
        }
    }

    @Nested
    @DisplayName("SagaContext Doğrulama")
    class SagaContextTests {

        @Test
        @DisplayName("Saga context, userId ve userEmail'i adımlara doğru iletir")
        void execute_contextPassedCorrectly() throws Exception {
            // GIVEN
            Order mockOrder = Order.builder().id(1L).orderNumber("ORD-CTX").build();

            doAnswer(invocation -> {
                SagaContext ctx = invocation.getArgument(0);
                // Context'te doğru veriler olduğunu doğrula
                assertThat(ctx.getUserId()).isEqualTo(userId);
                assertThat(ctx.getUserEmail()).isEqualTo(userEmail);
                assertThat(ctx.getOrderRequest()).isEqualTo(orderRequest);
                ctx.setCreatedOrder(mockOrder);
                return null;
            }).when(createOrderStep).execute(any(SagaContext.class));

            doNothing().when(reserveStockStep).execute(any(SagaContext.class));

            // WHEN
            orderSaga.execute(orderRequest, userId, userEmail);

            // THEN: doAnswer içindeki assertThat'ler zaten doğrulama yapar
            verify(createOrderStep).execute(any(SagaContext.class));
        }
    }
}
