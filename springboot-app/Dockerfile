# Etapa 1: Build com Maven e JDK (necessário para compilar)
FROM maven:3.9.8-eclipse-temurin-21 AS builder
WORKDIR /app

COPY pom.xml .
COPY ./src ./src

RUN mvn clean package -DskipTests --no-transfer-progress

# Etapa 2: Runtime com JRE (mais leve)
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=builder /app/target/*.jar /app/lumos-spring.jar

EXPOSE 8080

CMD ["java", "-Xmx512m", "-Xms256m", "-jar", "lumos-spring.jar"]

