package com.ecommerce.product.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchConfiguration;

/**
 * Elasticsearch istemci yapılandırması.
 *
 * Spring Data Elasticsearch 5.x ile yeni Elasticsearch Java Client kullanılır.
 * ElasticsearchConfiguration'ı extend ederek clientConfiguration() override edilir.
 *
 * Bağlantı havuzu: Elasticsearch istemcisi kendi bağlantı havuzunu yönetir.
 * socketTimeout: büyük arama sonuçları için yeterli süre verilmeli.
 */
@Configuration
public class ElasticsearchConfig extends ElasticsearchConfiguration {

    @Value("${elasticsearch.host:localhost}")
    private String esHost;

    @Value("${elasticsearch.port:9200}")
    private int esPort;

    @Value("${elasticsearch.username:}")
    private String username;

    @Value("${elasticsearch.password:}")
    private String password;

    /**
     * Elasticsearch bağlantı yapılandırması.
     * withBasicAuth: Elasticsearch güvenli modda çalışıyorsa kullanıcı adı/şifre gerekir.
     */
    @Override
    public ClientConfiguration clientConfiguration() {
        ClientConfiguration.MaybeSecureClientConfigurationBuilder builder =
                ClientConfiguration.builder()
                        .connectedTo(esHost + ":" + esPort);

        // Güvenlik yapılandırması — username boş değilse ekle
        if (!username.isBlank()) {
            builder.withBasicAuth(username, password);
        }

        return builder
                .withConnectTimeout(5000)  // Bağlantı kurma zaman aşımı (ms)
                .withSocketTimeout(30000)  // Veri okuma zaman aşımı (ms)
                .build();
    }
}
