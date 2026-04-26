# =====================================================================
# Auth Service - Multi-Stage Docker Build
# =====================================================================
# Multi-stage build faydaları:
# 1. Final image çok küçük — build araçları (Maven, JDK) dahil değil
# 2. Sadece çalışma zamanı için gerekli bileşenler (JRE) dahil
# 3. Güvenlik: gereksiz araçlar image'da olmaz
# =====================================================================

# ===== AŞAMA 1: BUILD (Derleme) =====
# maven:3.9-eclipse-temurin-17: Maven + JDK 17 içeren resmi image
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /build

# Önce bağımlılıkları kopyala ve indir — Docker layer caching optimizasyonu
# pom.xml değişmediği sürece bu layer cache'den gelir (bağımlılıklar tekrar indirilmez)
COPY pom.xml .
COPY common-lib/pom.xml common-lib/
COPY auth-service/pom.xml auth-service/

# Bağımlılıkları indir (kaynak kod olmadan)
RUN mvn dependency:go-offline -pl common-lib,auth-service -am -q

# Kaynak kodları kopyala ve derle
COPY common-lib/src common-lib/src
COPY auth-service/src auth-service/src

# -DskipTests: test olmadan derle (CI/CD'de test ayrıca çalışır)
RUN mvn package -pl common-lib,auth-service -am -DskipTests -q

# ===== AŞAMA 2: RUNTIME (Çalışma) =====
# eclipse-temurin:17-jre-alpine: Sadece JRE, Alpine Linux — çok küçük image
FROM eclipse-temurin:17-jre-alpine AS runtime

# Güvenlik: root olmayan kullanıcı oluştur
RUN addgroup -S spring && adduser -S spring -G spring

# Uygulama çalışma dizini
WORKDIR /app

# Builder aşamasından sadece JAR dosyasını kopyala
COPY --from=builder /build/auth-service/target/*.jar app.jar

# Dosya sahibini spring kullanıcısına değiştir
RUN chown spring:spring app.jar

# spring kullanıcısına geç (root olarak çalıştırma!)
USER spring

# Port: application.yml'deki server.port ile eşleşmeli
EXPOSE 8081

# JVM ayarları:
# -Xmx256m: maksimum heap belleği 256MB
# -Xms128m: başlangıç heap belleği 128MB
# -Djava.security.egd: Tomcat başlangıç hızlandırma (entropi kaynağı)
# -Dspring.profiles.active: aktif Spring profili
ENTRYPOINT ["java", \
            "-Xmx256m", \
            "-Xms128m", \
            "-Djava.security.egd=file:/dev/./urandom", \
            "-Dspring.profiles.active=${SPRING_PROFILES_ACTIVE:default}", \
            "-jar", \
            "app.jar"]
