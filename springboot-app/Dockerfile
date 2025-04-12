# Etapa 1: Build
FROM maven:3.9.8-eclipse-temurin-21 AS builder
WORKDIR /app

COPY pom.xml /app/
COPY ./src /app/src/

RUN mvn clean package -DskipTests --no-transfer-progress -Pprod

# Etapa 2: Runtime
FROM eclipse-temurin:21-jdk-alpine
WORKDIR /app

COPY --from=builder /app/target/*.jar /app/lumos-spring.jar

EXPOSE 8080
CMD ["java", "-jar", "lumos-spring.jar"]

