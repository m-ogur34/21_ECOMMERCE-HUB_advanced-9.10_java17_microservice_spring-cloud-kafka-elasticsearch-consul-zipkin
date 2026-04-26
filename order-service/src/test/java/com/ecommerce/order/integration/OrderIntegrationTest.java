package com.ecommerce.order.integration;

import com.ecommerce.common.dto.order.OrderItemRequest;
import com.ecommerce.common.dto.order.OrderRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Order Service Entegrasyon Testi.
 *
 * Gerçek PostgreSQL ve Kafka container'ları ile test edilir.
 * JWT filter test profilinde bypass edilir — güvenlik testi ayrı yapılır.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Order Service Entegrasyon Testleri")
class OrderIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("order_db_test")
                    .withUsername("postgres")
                    .withPassword("postgres");

    @Container
    static final KafkaContainer kafka =
            new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.5.0"));

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static Long createdOrderId;
    private static String createdOrderNumber;

    private OrderRequest buildOrderRequest() {
        OrderItemRequest item = new OrderItemRequest();
        item.setProductId(1L);
        item.setQuantity(2);

        OrderRequest req = new OrderRequest();
        req.setItems(List.of(item));
        req.setShippingAddress("Test Cad. No:1 Kadıköy İstanbul 34710");
        return req;
    }

    @Test
    @org.junit.jupiter.api.Order(1)
    @DisplayName("1. Geçerli istekle sipariş oluşturulur — PENDING durumunda")
    void createOrder_validRequest_returnsPendingOrder() throws Exception {
        var result = mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer test-user-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildOrderRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.orderNumber").isNotEmpty())
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.items[0].productId").value(1))
                .andReturn();

        var json = objectMapper.readTree(result.getResponse().getContentAsString());
        createdOrderId = json.path("data").path("id").asLong();
        createdOrderNumber = json.path("data").path("orderNumber").asText();
    }

    @Test
    @org.junit.jupiter.api.Order(2)
    @DisplayName("2. Sipariş ID ile getirilebilir")
    void getOrderById_existingOrder_returnsOrder() throws Exception {
        Assumptions.assumeTrue(createdOrderId != null);

        mockMvc.perform(get("/api/orders/{id}", createdOrderId)
                        .header("Authorization", "Bearer test-user-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(createdOrderId))
                .andExpect(jsonPath("$.data.orderNumber").value(createdOrderNumber));
    }

    @Test
    @org.junit.jupiter.api.Order(3)
    @DisplayName("3. Kullanıcının siparişleri sayfalı listelenir")
    void getMyOrders_authenticatedUser_returnsPagedOrders() throws Exception {
        mockMvc.perform(get("/api/orders/my-orders")
                        .header("Authorization", "Bearer test-user-token")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.totalElements").value(greaterThanOrEqualTo(1)));
    }

    @Test
    @org.junit.jupiter.api.Order(4)
    @DisplayName("4. PENDING sipariş iptal edilebilir")
    void cancelOrder_pendingOrder_returnsCancelledOrder() throws Exception {
        Assumptions.assumeTrue(createdOrderId != null);

        mockMvc.perform(patch("/api/orders/{id}/cancel", createdOrderId)
                        .header("Authorization", "Bearer test-user-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));
    }

    @Test
    @org.junit.jupiter.api.Order(5)
    @DisplayName("5. İptal edilmiş sipariş tekrar iptal edilemez")
    void cancelOrder_alreadyCancelled_returns400() throws Exception {
        Assumptions.assumeTrue(createdOrderId != null);

        mockMvc.perform(patch("/api/orders/{id}/cancel", createdOrderId)
                        .header("Authorization", "Bearer test-user-token"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @org.junit.jupiter.api.Order(6)
    @DisplayName("6. Geçersiz (boş) sipariş isteği validation hatası verir")
    void createOrder_emptyItems_returns400() throws Exception {
        OrderRequest invalidRequest = new OrderRequest();
        invalidRequest.setItems(List.of()); // @NotEmpty ihlali
        invalidRequest.setShippingAddress("Test Adres");

        mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer test-user-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @org.junit.jupiter.api.Order(7)
    @DisplayName("7. Token olmadan istek 401 döner")
    void createOrder_noToken_returns401() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildOrderRequest())))
                .andExpect(status().isUnauthorized());
    }

    // Hamcrest matcher import
    private static org.hamcrest.Matcher<Integer> greaterThanOrEqualTo(int n) {
        return org.hamcrest.Matchers.greaterThanOrEqualTo(n);
    }
}
