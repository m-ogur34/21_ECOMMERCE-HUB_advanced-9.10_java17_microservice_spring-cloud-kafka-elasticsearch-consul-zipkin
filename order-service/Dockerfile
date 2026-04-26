FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /build
COPY pom.xml .
COPY common-lib/pom.xml common-lib/
COPY order-service/pom.xml order-service/
RUN mvn dependency:go-offline -pl common-lib,order-service -am -q
COPY common-lib/src common-lib/src
COPY order-service/src order-service/src
RUN mvn package -pl common-lib,order-service -am -DskipTests -q

FROM eclipse-temurin:17-jre-alpine AS runtime
RUN addgroup -S spring && adduser -S spring -G spring
WORKDIR /app
COPY --from=builder /build/order-service/target/*.jar app.jar
RUN chown spring:spring app.jar
USER spring
EXPOSE 8083
ENTRYPOINT ["java", "-Xmx256m", "-Xms128m", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
