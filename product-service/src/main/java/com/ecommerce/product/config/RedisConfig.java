package com.ecommerce.product.config;

import com.ecommerce.common.constants.CacheConstants;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Redis önbellek yapılandırması.
 *
 * Neden RedisCacheManager özelleştiriyoruz?
 * Default konfigürasyonda Redis, Java nesnelerini binary (JDK serialization) olarak saklar.
 * Bu yaklaşımın sorunları:
 * 1. Okunaksız — Redis CLI'da değerleri göremezsin
 * 2. Sınıf değişirse eski cache deserialize edilemez
 * 3. Farklı JVM versiyonları arasında uyumsuzluk
 *
 * JSON serialization: insanlar Redis'te anlayabilir, sınıf değişimine daha dayanıklı.
 *
 * @EnableCaching: @Cacheable, @CacheEvict gibi annotasyonları Spring AOP ile aktifleştirir.
 */
@Configuration
@EnableCaching
public class RedisConfig {

    /**
     * Ana CacheManager — @Cacheable, @CacheEvict için kullanılır.
     *
     * Cache başına farklı TTL tanımlanabilir:
     * - product cache: 10 dakika
     * - products (liste) cache: 5 dakika
     * - category cache: 1 saat
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // JSON serializer — type info ile birlikte (deserialize için class bilgisi)
        GenericJackson2JsonRedisSerializer jsonSerializer = buildJsonSerializer();

        // Varsayılan cache yapılandırması
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofSeconds(CacheConstants.PRODUCT_TTL_SECONDS))
                .serializeKeysWith(
                    RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer())
                )
                .serializeValuesWith(
                    RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer)
                )
                .disableCachingNullValues(); // null değerleri cache'leme

        // Cache başına özel TTL konfigürasyonları
        Map<String, RedisCacheConfiguration> cacheConfigs = new HashMap<>();

        // "product" cache: tek ürün — 10 dakika
        cacheConfigs.put(CacheConstants.PRODUCT,
            defaultConfig.entryTtl(Duration.ofSeconds(CacheConstants.PRODUCT_TTL_SECONDS)));

        // "products" cache: ürün listesi — 5 dakika (daha sık değişir)
        cacheConfigs.put(CacheConstants.PRODUCTS,
            defaultConfig.entryTtl(Duration.ofSeconds(CacheConstants.PRODUCTS_LIST_TTL_SECONDS)));

        // "category" cache: tek kategori — 1 saat
        cacheConfigs.put(CacheConstants.CATEGORY,
            defaultConfig.entryTtl(Duration.ofSeconds(CacheConstants.CATEGORY_TTL_SECONDS)));

        // "categories" cache: tüm kategoriler — 1 saat
        cacheConfigs.put(CacheConstants.CATEGORIES,
            defaultConfig.entryTtl(Duration.ofSeconds(CacheConstants.CATEGORY_TTL_SECONDS)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigs) // Cache bazlı TTL
                .transactionAware() // @Transactional ile cache işlemleri koordineli çalışır
                .build();
    }

    /**
     * Düşük seviyeli Redis erişimi için RedisTemplate.
     * @Cacheable dışında doğrudan Redis'e yazmak/okumak için kullanılır.
     * Örneğin: rate limiting, session, pub/sub.
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Key: String serialize
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());

        // Value: JSON serialize
        GenericJackson2JsonRedisSerializer jsonSerializer = buildJsonSerializer();
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);

        template.afterPropertiesSet();
        return template;
    }

    /**
     * Jackson JSON serializer — LocalDateTime desteği ile.
     * activateDefaultTyping: JSON'a class ismi ekler, deserialize sırasında doğru tipe dönüşür.
     */
    private GenericJackson2JsonRedisSerializer buildJsonSerializer() {
        ObjectMapper mapper = new ObjectMapper();

        // Java 8 tarih/saat nesneleri (LocalDateTime) JSON'a düzgün yazılsın
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS); // ISO string formatı

        // Type bilgisi ekle: deserialize sırasında hangi class kullanılacağını bilmek için
        mapper.activateDefaultTyping(
            mapper.getPolymorphicTypeValidator(),
            ObjectMapper.DefaultTyping.NON_FINAL,
            JsonTypeInfo.As.PROPERTY
        );

        return new GenericJackson2JsonRedisSerializer(mapper);
    }
}
