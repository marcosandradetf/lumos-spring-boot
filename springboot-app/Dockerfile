# Etapa 1: Build com Maven e JDK completo
FROM maven:3.9.8-eclipse-temurin-25 AS builder
WORKDIR /app

# Copia só o pom para aproveitar cache de dependências
COPY pom.xml ./
RUN mvn dependency:go-offline -B

# Copia o restante e empacota
COPY ./src ./src
RUN mvn clean package -DskipTests --no-transfer-progress

# Etapa 2: Runtime leve com JRE Alpine
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app

# Segurança: cria usuário sem privilégios
RUN adduser -D -u 1000 appuser
USER appuser

# Define fuso horário (opcional, mas comum)
ENV TZ=UTC

# Copia JAR gerado
COPY --from=builder /app/target/*.jar /app/lumos-spring.jar

# Expõe a porta do app
EXPOSE 8080

# JVM otimizada para containers leves
CMD ["java", "-XX:+UseG1GC", "-XX:InitialRAMPercentage=30.0", "-XX:MaxRAMPercentage=60.0", "-XX:+ExitOnOutOfMemoryError", "-XX:+AlwaysPreTouch", "--enable-preview", "-jar", "lumos-spring.jar"]
