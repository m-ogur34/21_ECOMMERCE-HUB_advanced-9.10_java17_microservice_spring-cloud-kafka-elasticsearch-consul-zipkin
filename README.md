# ECommerceHub — Mikroservis E-Ticaret Platformu

Java 17, Spring Boot 3, Angular 17 ve Kubernetes ile tam yığın mikroservis mimarisi.

---

## Mimari Genel Bakış

```
Angular 17 Frontend
        │
        ▼
  API Gateway :8080   ← JWT doğrulama, rate limiting (Redis), routing
   /          |    \
  /           |     \
Auth        Product  Order   Notification
:8081       :8082   :8083    :8084
  │           │       │         │
PostgreSQL  PostgreSQL PostgreSQL PostgreSQL
  │           │       │
            Redis  Kafka ────────────────┐
          (cache) (events)               │
            Elasticsearch        Notification tüketir
           (full-text)
              │
           Consul (service discovery)
           Zipkin (tracing)
           Grafana (metrics)
```

### Tasarım Desenleri

| Desen | Nerede |
|-------|--------|
| Repository | Tüm JPA repository'leri |
| Factory | `ProductFactory` — ürün/varyant oluşturma |
| Strategy | `PriceStrategy` — fiyat hesaplama |
| Observer | `ProductEventPublisher` + `StockObserver` |
| Saga (Orchestration) | `OrderSaga` — sipariş + stok rezervasyonu |
| Circuit Breaker | `OrderServiceImpl` — Resilience4J |
| State Machine | `OrderStatus.canTransitionTo()` |

---

## Ön Koşullar

| Araç | Versiyon |
|------|----------|
| Java | 17+ |
| Maven | 3.9+ |
| Docker | 24+ |
| Docker Compose | 2.x |
| Node.js | 20+ |
| npm | 10+ |

---

## Hızlı Başlangıç

### 1. Depoyu klonla

```bash
git clone https://github.com/youruser/ECommerceHub.git
cd ECommerceHub
```

### 2. Docker ile tüm sistemi başlat

```bash
cd docker
docker-compose up -d
```

İlk çalıştırmada Docker image'ları indirilir (~5-10 dakika). Servislerin sağlıklı olmasını bekle:

```bash
docker-compose ps          # tüm servislerin STATUS = healthy olmasını bekle
docker-compose logs -f api-gateway   # gateway loglarını izle
```

### 3. Servis URL'leri

| Servis | URL |
|--------|-----|
| API Gateway | http://localhost:8080 |
| Auth Service | http://localhost:8081 |
| Product Service | http://localhost:8082 |
| Order Service | http://localhost:8083 |
| Notification Service | http://localhost:8084 |
| Consul UI | http://localhost:8500 |
| Zipkin UI | http://localhost:9411 |
| Grafana | http://localhost:3000 (admin/admin123) |
| Elasticsearch | http://localhost:9200 |
| Kafka | localhost:29092 |

### 4. Angular frontend

```bash
cd frontend
npm install
npm start           # http://localhost:4200
```

---

## Geliştirme Ortamı (Docker olmadan)

Her servisi ayrı ayrı çalıştırmak için önce infrastructure'ı docker ile başlat:

```bash
cd docker
docker-compose up -d postgres-auth postgres-product postgres-order postgres-notification redis kafka zookeeper elasticsearch consul zipkin
```

Sonra her servisi IDE'den veya terminalden başlat:

```bash
# Auth Service
cd auth-service
mvn spring-boot:run

# Product Service (yeni terminal)
cd product-service
mvn spring-boot:run

# Order Service (yeni terminal)
cd order-service
mvn spring-boot:run

# Notification Service (yeni terminal)
cd notification-service
mvn spring-boot:run

# API Gateway (yeni terminal)
cd api-gateway
mvn spring-boot:run
```

---

## API Kullanımı

### Kayıt & Giriş

```bash
# Kayıt
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "Ahmet",
    "lastName": "Yılmaz",
    "email": "ahmet@example.com",
    "password": "Password123!"
  }'

# Giriş (varsayılan admin kullanıcı)
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "admin@ecommerce.com", "password": "Admin123!"}'
```

Yanıt `data.accessToken` alanını kopyala ve sonraki isteklerde `Authorization: Bearer <token>` header'ı olarak gönder.

### Ürün İşlemleri

```bash
# Ürün listesi
curl http://localhost:8080/api/products?page=0&size=10

# Elasticsearch ile arama
curl "http://localhost:8080/api/products/search?query=laptop"

# Ürün oluştur (admin token gerekli)
curl -X POST http://localhost:8080/api/products \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Gaming Laptop",
    "description": "Yüksek performanslı oyun laptopu",
    "price": 25999.99,
    "stockQuantity": 10,
    "sku": "LAP-GAME-001",
    "categoryId": 1
  }'
```

### Sipariş Oluşturma (Saga)

```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "shippingAddress": "Atatürk Cad. No:1 Kadıköy İstanbul 34710",
    "items": [
      {"productId": 1, "quantity": 2},
      {"productId": 2, "quantity": 1}
    ]
  }'
```

Saga akışı:
1. `CreateOrderStep` → Order PENDING durumunda oluşturulur
2. `ReserveStockStep` → Kafka'ya `ORDER_CREATED` eventi gönderilir
3. Product Service stok rezervasyonu yapar → `STOCK_RESERVED` eventi
4. Order Service `STOCK_RESERVED`'i tüketir → Order CONFIRMED
5. Hata durumunda LIFO sırasıyla compensate çalışır

---

## Testler

```bash
# Tüm modülleri test et
mvn test

# Belirli modül
cd auth-service && mvn test

# Integration testleri (Testcontainers gerektirir — Docker çalışıyor olmalı)
mvn test -Dgroups=integration

# Test coverage raporu
mvn jacoco:report
# Rapor: target/site/jacoco/index.html
```

---

## Kubernetes Deployment

### Ön koşul: kubectl ve cluster

```bash
# minikube ile local cluster (opsiyonel)
minikube start --memory=8192 --cpus=4

# Docker image'ları build et
docker build -t ecommerce/auth-service:latest -f auth-service/Dockerfile .
docker build -t ecommerce/product-service:latest -f product-service/Dockerfile .
docker build -t ecommerce/order-service:latest -f order-service/Dockerfile .
docker build -t ecommerce/notification-service:latest -f notification-service/Dockerfile .
docker build -t ecommerce/api-gateway:latest -f api-gateway/Dockerfile .
```

### Deploy

```bash
# Namespace oluştur
kubectl apply -f k8s/namespace.yaml

# ConfigMap ve Secret
kubectl apply -f k8s/configmap/
kubectl apply -f k8s/secrets/

# Infrastructure (sıralı — bağımlılıklara dikkat)
kubectl apply -f k8s/infrastructure/postgres-auth.yaml
kubectl apply -f k8s/infrastructure/postgres-others.yaml
kubectl apply -f k8s/infrastructure/redis.yaml
kubectl apply -f k8s/infrastructure/kafka.yaml
kubectl apply -f k8s/infrastructure/elasticsearch.yaml
kubectl apply -f k8s/infrastructure/consul.yaml
kubectl apply -f k8s/infrastructure/zipkin-grafana.yaml

# Infrastructure hazır olana kadar bekle
kubectl wait --for=condition=ready pod -l app=postgres-auth -n ecommerce --timeout=120s
kubectl wait --for=condition=ready pod -l app=kafka -n ecommerce --timeout=180s

# Mikroservisler
kubectl apply -f k8s/services/auth-service.yaml
kubectl apply -f k8s/services/product-service.yaml
kubectl apply -f k8s/services/order-service.yaml
kubectl apply -f k8s/services/notification-service.yaml
kubectl apply -f k8s/services/api-gateway.yaml

# Durumu kontrol et
kubectl get pods -n ecommerce
kubectl get svc -n ecommerce
```

### Erişim

```bash
# API Gateway (NodePort)
kubectl port-forward svc/api-gateway-svc 8080:8080 -n ecommerce

# minikube ile
minikube service api-gateway-svc -n ecommerce
```

### Hata Ayıklama

```bash
# Pod logları
kubectl logs -f deployment/auth-service -n ecommerce

# Pod içine gir
kubectl exec -it deployment/auth-service -n ecommerce -- /bin/sh

# Pod açıklaması (event'ler dahil)
kubectl describe pod -l app=auth-service -n ecommerce

# HPA durumu
kubectl get hpa -n ecommerce
```

---

## Postman Koleksiyonu

`postman/ECommerceHub.postman_collection.json` dosyasını Postman'e import et:

1. Postman → Import → dosyayı seç
2. Collection Variables'da `baseUrl` kontrol et: `http://localhost:8080`
3. **Auth/Login** isteğini çalıştır → `accessToken` otomatik kaydedilir
4. Diğer istekleri çalıştır

---

## Mimari Kararlar

### Neden Database per Service?

Her servisin kendi PostgreSQL instance'ı var. Bu:
- Servislerin bağımsız deploy edilmesini sağlar
- Şema değişiklikleri diğer servisleri etkilemez
- Her servis kendi migration'larını (Flyway) yönetir

### Neden Saga Pattern?

Sipariş oluşturma birden fazla servisi etkiler (Order + Product stok). `ACID` transaction mümkün değil (farklı DB'ler). Saga ile:
- Her adım local transaction
- Hata durumunda compensate (ters işlem) çalışır
- LIFO sırasıyla rollback: son adım ilk önce geri alınır

### Neden JWT Stateless?

Her servis (product, order) kendi `JwtAuthFilter`'ına sahip. Token doğrulaması için auth-service'e istek atmaz — sadece JWT secret ile imzayı kontrol eder. Bu sayede auth-service tek hata noktası olmaz.

### Neden Redis Rate Limiting (Gateway)?

API Gateway, her IP için saniye başına istek sayısını Redis'te takip eder. Bu sayede:
- DDoS koruması
- API abuse önleme
- Servis başına farklı rate limit (ürün arama vs. sipariş)

### Neden Circuit Breaker (Order Service)?

Stok rezervasyonu Kafka üzerinden async. Senkron fallback için Resilience4J circuit breaker:
- 3 saniye timeout
- %50 hata eşiğinde OPEN
- 30 saniye sonra HALF-OPEN → test isteği

---

## Proje Yapısı

```
ECommerceHub/
├── pom.xml                     # Parent POM — tüm modüller burada
├── common-lib/                 # Paylaşılan DTO, event, exception
├── auth-service/               # JWT, kullanıcı yönetimi
├── product-service/            # Ürün CRUD, Elasticsearch, Redis cache
├── order-service/              # Sipariş Saga, Circuit Breaker
├── notification-service/       # Kafka consumer, email/SMS, ActiveMQ
├── api-gateway/                # Spring Cloud Gateway, rate limiting
├── frontend/                   # Angular 17 standalone
├── docker/
│   └── docker-compose.yml      # Tüm servisler tek komutla
├── k8s/
│   ├── namespace.yaml
│   ├── configmap/
│   ├── secrets/
│   ├── infrastructure/         # PostgreSQL, Redis, Kafka, ES, Consul
│   └── services/               # Mikroservis Deployment + HPA + Ingress
└── postman/
    └── ECommerceHub.postman_collection.json
```

---

## Teknoloji Yığını

**Backend:** Java 17, Spring Boot 3.2, Spring Security, Spring Data JPA, Spring Cloud Gateway, Resilience4J, OpenFeign

**Mesajlaşma:** Apache Kafka, ActiveMQ Artemis

**Veri:** PostgreSQL 16, Redis 7, Elasticsearch 8

**Gözlemlenebilirlik:** Micrometer, Zipkin, Grafana

**Servis Keşfi:** Consul

**Frontend:** Angular 17, Angular Material, Signals, Reactive Forms

**DevOps:** Docker, Docker Compose, Kubernetes, Flyway, Testcontainers
