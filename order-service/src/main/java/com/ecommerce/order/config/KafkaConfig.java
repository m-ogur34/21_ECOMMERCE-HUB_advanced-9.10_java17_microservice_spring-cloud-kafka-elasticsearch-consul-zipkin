package com.ecommerce.order.config;

import com.ecommerce.common.event.StockReservedEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka yapılandırması — Producer ve Consumer factory'leri.
 *
 * Producer: Kafka'ya mesaj gönderir (OrderCreatedEvent)
 * Consumer: Kafka'dan mesaj dinler (StockReservedEvent)
 *
 * KafkaTemplate: Spring'in Kafka producer soyutlaması.
 * KafkaListenerContainerFactory: @KafkaListener metodlarını yönetir.
 *
 * Serileştirme stratejisi: JSON — insanlar tarafından okunabilir, schema-flexible.
 * Alternatif: Avro (şema zorunluluğu, daha verimli) — üretim ortamı için tercih edilir.
 */
@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    // ===== PRODUCER =====

    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        // Güvenilir producer ayarları
        config.put(ProducerConfig.ACKS_CONFIG, "all");      // Tüm replica'lar onaylasın
        config.put(ProducerConfig.RETRIES_CONFIG, 3);         // Hata durumunda 3 kez tekrar
        config.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true); // Duplicate mesaj engelle

        return new DefaultKafkaProducerFactory<>(config);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    // ===== CONSUMER =====

    @Bean
    public ConsumerFactory<String, StockReservedEvent> consumerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, "order-service-group");
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false); // Manuel acknowledge

        JsonDeserializer<StockReservedEvent> deserializer =
                new JsonDeserializer<>(StockReservedEvent.class);
        deserializer.addTrustedPackages("com.ecommerce.common.event");

        return new DefaultKafkaConsumerFactory<>(
                config,
                new StringDeserializer(),
                deserializer
        );
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, StockReservedEvent>
            kafkaListenerContainerFactory() {

        ConcurrentKafkaListenerContainerFactory<String, StockReservedEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());

        // Manuel acknowledge modu
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);

        // Eş zamanlı dinleyici sayısı — Kafka partition sayısına eşit olmalı
        factory.setConcurrency(3);

        return factory;
    }
}
