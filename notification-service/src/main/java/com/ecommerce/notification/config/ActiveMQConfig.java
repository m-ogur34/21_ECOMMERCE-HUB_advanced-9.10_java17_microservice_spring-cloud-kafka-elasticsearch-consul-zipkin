package com.ecommerce.notification.config;

import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.core.JmsTemplate;

/**
 * ActiveMQ Artemis yapılandırması.
 *
 * ActiveMQ: geleneksel mesaj kuyruğu — JMS (Java Message Service) standardı.
 * Kafka farkı:
 * - ActiveMQ: P2P (Point-to-Point) veya Pub/Sub, mesaj silinir okunca
 * - Kafka: dağıtık log, mesaj saklanır, tekrar okunabilir
 *
 * Bu projede ActiveMQ dahili e-posta kuyruğu için kullanılır:
 * - Kafka → OrderEventConsumer → ActiveMQ [email.queue] → EmailWorker
 * - Bu şekilde e-posta gönderimi retry edilebilir ve izlenebilir
 */
@Configuration
public class ActiveMQConfig {

    @Value("${spring.artemis.broker-url:vm://localhost?broker.persistent=false}")
    private String brokerUrl;

    @Bean
    public ActiveMQConnectionFactory connectionFactory() {
        return new ActiveMQConnectionFactory(brokerUrl);
    }

    @Bean
    public JmsTemplate jmsTemplate() {
        JmsTemplate template = new JmsTemplate(connectionFactory());
        template.setDeliveryPersistent(true); // Mesajlar kalıcı — broker yeniden başlatılınca kaybolmaz
        return template;
    }

    @Bean
    public DefaultJmsListenerContainerFactory jmsListenerContainerFactory() {
        DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory());
        factory.setConcurrency("1-3"); // 1 ile 3 arası concurrent consumer
        return factory;
    }
}
