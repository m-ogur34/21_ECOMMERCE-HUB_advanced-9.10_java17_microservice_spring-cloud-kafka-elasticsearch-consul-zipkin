FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /build
COPY pom.xml .
COPY common-lib/pom.xml common-lib/
COPY product-service/pom.xml product-service/
RUN mvn dependency:go-offline -pl common-lib,product-service -am -q
COPY common-lib/src common-lib/src
COPY product-service/src product-service/src
RUN mvn package -pl common-lib,product-service -am -DskipTests -q

FROM eclipse-temurin:17-jre-alpine AS runtime
RUN addgroup -S spring && adduser -S spring -G spring
WORKDIR /app
COPY --from=builder /build/product-service/target/*.jar app.jar
RUN chown spring:spring app.jar
USER spring
EXPOSE 8082
ENTRYPOINT ["java", "-Xmx512m", "-Xms256m", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
