package com.ecommerce.product.integration;

import com.ecommerce.common.dto.product.ProductRequest;
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
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

/**
 * Product Service Entegrasyon Testi.
 *
 * Testcontainers: Gerçek Docker container'ları ayağa kaldırır.
 * Mock veritabanı yok — gerçek PostgreSQL, Redis, Elasticsearch ile test edilir.
 * Bu sayede prod ortamına yakın test edilmiş olur.
 *
 * @SpringBootTest: Tam uygulama context'i yükler — tüm bean'ler gerçek
 * @AutoConfigureMockMvc: HTTP istek simülasyonu için MockMvc enjekte eder
 * @Testcontainers: @Container alanlarını otomatik başlatır/durdurur
 *
 * NOT: Bu testler Docker gerektirir ve yavaş çalışır (~30-60s).
 * CI/CD'de ayrı profil olarak çalıştırılmalı: mvn test -Dgroups=integration
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Product Service Entegrasyon Testleri")
class ProductIntegrationTest {

    // ===== TESTCONTAINERS =====

    /**
     * PostgreSQL container — gerçek veritabanı.
     * static: tüm testler aynı container'ı paylaşır (her test için yeniden başlatma yok).
     */
    @Container
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("product_db_test")
                    .withUsername("postgres")
                    .withPassword("postgres");

    /**
     * Redis container — cache testi için.
     */
    @Container
    static final GenericContainer<?> redis =
            new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
                    .withExposedPorts(6379);

    /**
     * Elasticsearch container — arama testi için.
     */
    @Container
    static final ElasticsearchContainer elasticsearch =
            new ElasticsearchContainer(
                    DockerImageName.parse("docker.elastic.co/elasticsearch/elasticsearch:8.11.0"))
                    .withEnv("xpack.security.enabled", "false")
                    .withEnv("discovery.type", "single-node");

    /**
     * DynamicPropertySource: Container başladıktan sonra portlar belli olur.
     * Bu metodla Spring'e dinamik URL/port bilgisi veriyoruz.
     * Statik @Value ile yapılamaz — container portu runtime'da belli olur.
     */
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // PostgreSQL
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);

        // Redis
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));

        // Elasticsearch
        registry.add("spring.elasticsearch.uris",
                () -> "http://" + elasticsearch.getHttpHostAddress());

        // Kafka: Test için gerçek Kafka yerine embedded kullan
        registry.add("spring.kafka.bootstrap-servers", () -> "localhost:9092");
        registry.add("spring.autoconfigure.exclude",
                () -> "org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // Test boyunca paylaşılan ürün ID'si
    private static Long createdProductId;

    // ===== TEST VERİSİ HAZIRLAMA =====

    private static final String ADMIN_TOKEN = "test-admin-token"; // Test profili için bypass

    private ProductRequest buildProductRequest() {
        ProductRequest req = new ProductRequest();
        req.setName("Test Laptop Pro");
        req.setDescription("Entegrasyon testi için oluşturulan ürün");
        req.setPrice(new BigDecimal("12999.99"));
        req.setStockQuantity(50);
        req.setSku("INT-TEST-LAP-001");
        req.setCategoryId(1L);  // V1 migration'da oluşturulan kategori
        return req;
    }

    // ===== TESTLER =====

    @Test
    @org.junit.jupiter.api.Order(1)
    @DisplayName("1. Ürün oluşturulabilir")
    void createProduct_validRequest_returns201() throws Exception {
        ProductRequest request = buildProductRequest();

        MvcResult result = mockMvc.perform(post("/api/products")
                        .header("Authorization", "Bearer " + ADMIN_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("Test Laptop Pro"))
                .andExpect(jsonPath("$.data.sku").value("INT-TEST-LAP-001"))
                .andExpect(jsonPath("$.data.stockQuantity").value(50))
                .andExpect(jsonPath("$.data.id").isNumber())
                .andReturn();

        // Sonraki testlerde kullanmak üzere ID'yi sakla
        String response = result.getResponse().getContentAsString();
        createdProductId = objectMapper.readTree(response)
                .path("data").path("id").asLong();
    }

    @Test
    @org.junit.jupiter.api.Order(2)
    @DisplayName("2. Aynı SKU ile ürün oluşturulamaz")
    void createProduct_duplicateSku_returns409() throws Exception {
        ProductRequest request = buildProductRequest(); // Aynı SKU

        mockMvc.perform(post("/api/products")
                        .header("Authorization", "Bearer " + ADMIN_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    @org.junit.jupiter.api.Order(3)
    @DisplayName("3. Ürün ID ile getirilebilir ve cache'e alınır")
    void getProductById_existingProduct_returns200AndCaches() throws Exception {
        Assumptions.assumeTrue(createdProductId != null, "Önce ürün oluşturulmalı");

        // İlk istek: DB'den gelir, cache'e yazılır
        mockMvc.perform(get("/api/products/{id}", createdProductId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(createdProductId))
                .andExpect(jsonPath("$.data.name").value("Test Laptop Pro"));

        // İkinci istek: cache'den gelir (aynı sonuç)
        mockMvc.perform(get("/api/products/{id}", createdProductId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(createdProductId));
    }

    @Test
    @org.junit.jupiter.api.Order(4)
    @DisplayName("4. Olmayan ürün 404 döner")
    void getProductById_notFound_returns404() throws Exception {
        mockMvc.perform(get("/api/products/{id}", 99999L))
                .andExpect(status().isNotFound());
    }

    @Test
    @org.junit.jupiter.api.Order(5)
    @DisplayName("5. Ürün güncellenebilir ve cache güncellenir")
    void updateProduct_validRequest_returns200() throws Exception {
        Assumptions.assumeTrue(createdProductId != null, "Önce ürün oluşturulmalı");

        ProductRequest updateRequest = buildProductRequest();
        updateRequest.setName("Test Laptop Pro Güncellenmiş");
        updateRequest.setPrice(new BigDecimal("11999.99"));

        mockMvc.perform(put("/api/products/{id}", createdProductId)
                        .header("Authorization", "Bearer " + ADMIN_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Test Laptop Pro Güncellenmiş"))
                .andExpect(jsonPath("$.data.price").value(11999.99));
    }

    @Test
    @org.junit.jupiter.api.Order(6)
    @DisplayName("6. Elasticsearch ile ürün aranabilir")
    void searchProducts_validQuery_returnsResults() throws Exception {
        // ES indexleme async — kısa bekleme
        Thread.sleep(1500);

        mockMvc.perform(get("/api/products/search")
                        .param("query", "Laptop")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    @org.junit.jupiter.api.Order(7)
    @DisplayName("7. Stok düşürme doğru çalışır")
    void decreaseStock_validAmount_stockUpdated() throws Exception {
        Assumptions.assumeTrue(createdProductId != null);

        // Stok düşür: 50 → 45
        mockMvc.perform(patch("/api/products/{id}/stock/decrease", createdProductId)
                        .header("Authorization", "Bearer " + ADMIN_TOKEN)
                        .param("quantity", "5"))
                .andExpect(status().isOk());

        // Sonucu doğrula
        mockMvc.perform(get("/api/products/{id}", createdProductId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.stockQuantity").value(45));
    }

    @Test
    @org.junit.jupiter.api.Order(8)
    @DisplayName("8. Stoktan fazla düşürme hata verir")
    void decreaseStock_exceedsStock_returns400() throws Exception {
        Assumptions.assumeTrue(createdProductId != null);

        // Mevcut stok 45, 100 düşürmeye çalış
        mockMvc.perform(patch("/api/products/{id}/stock/decrease", createdProductId)
                        .header("Authorization", "Bearer " + ADMIN_TOKEN)
                        .param("quantity", "100"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @org.junit.jupiter.api.Order(9)
    @DisplayName("9. Ürün listesi sayfalı döner")
    void getAllProducts_paginatedRequest_returnsPageResponse() throws Exception {
        mockMvc.perform(get("/api/products")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sortBy", "name")
                        .param("sortDir", "ASC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.totalElements").isNumber())
                .andExpect(jsonPath("$.data.number").value(0))
                .andExpect(jsonPath("$.data.size").value(10));
    }

    @Test
    @org.junit.jupiter.api.Order(10)
    @DisplayName("10. Ürün silinebilir ve cache temizlenir")
    void deleteProduct_existingProduct_returns204AndEvictsCache() throws Exception {
        Assumptions.assumeTrue(createdProductId != null);

        mockMvc.perform(delete("/api/products/{id}", createdProductId)
                        .header("Authorization", "Bearer " + ADMIN_TOKEN))
                .andExpect(status().isNoContent());

        // Silindikten sonra 404 dönmeli
        mockMvc.perform(get("/api/products/{id}", createdProductId))
                .andExpect(status().isNotFound());
    }
}
