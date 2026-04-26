FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /build
COPY pom.xml .
COPY common-lib/pom.xml common-lib/
COPY notification-service/pom.xml notification-service/
RUN mvn dependency:go-offline -pl common-lib,notification-service -am -q
COPY common-lib/src common-lib/src
COPY notification-service/src notification-service/src
RUN mvn package -pl common-lib,notification-service -am -DskipTests -q

FROM eclipse-temurin:17-jre-alpine AS runtime
RUN addgroup -S spring && adduser -S spring -G spring
WORKDIR /app
COPY --from=builder /build/notification-service/target/*.jar app.jar
RUN chown spring:spring app.jar
USER spring
EXPOSE 8084
ENTRYPOINT ["java", "-Xmx256m", "-Xms128m", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
