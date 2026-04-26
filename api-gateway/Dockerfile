FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /build
COPY pom.xml .
COPY common-lib/pom.xml common-lib/
COPY api-gateway/pom.xml api-gateway/
RUN mvn dependency:go-offline -pl common-lib,api-gateway -am -q
COPY common-lib/src common-lib/src
COPY api-gateway/src api-gateway/src
RUN mvn package -pl common-lib,api-gateway -am -DskipTests -q

FROM eclipse-temurin:17-jre-alpine AS runtime
RUN addgroup -S spring && adduser -S spring -G spring
WORKDIR /app
COPY --from=builder /build/api-gateway/target/*.jar app.jar
RUN chown spring:spring app.jar
USER spring
EXPOSE 8080
ENTRYPOINT ["java", "-Xmx256m", "-Xms128m", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
