# ================= BUILD =================
FROM maven:3.9.8-eclipse-temurin-21 AS builder
WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src src
RUN mvn clean package -DskipTests --no-transfer-progress

# ================= RUNTIME =================
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN adduser -D -u 1000 appuser
USER appuser

ENV TZ=UTC

COPY --from=builder /app/target/*.jar /app/lumos-spring.jar

EXPOSE 8080

CMD ["java" ,"-jar", "lumos-spring.jar"]
