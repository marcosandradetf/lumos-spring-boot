# Etapa 1: Usando uma imagem do Maven para construir o JAR
FROM maven:3.9.8-eclipse-temurin-21 AS builder

# Defina o diretório de trabalho no container
WORKDIR /app

# Copie o pom.xml e o diretório de código fonte para o container
COPY pom.xml /app/
COPY ./src /app/src/

# Execute o Maven para construir o JAR
RUN mvn clean package

# Verifique se o arquivo JAR foi gerado corretamente
RUN ls -l /app/target

# Etapa 2: Usando uma imagem do OpenJDK para rodar o Spring Boot
FROM openjdk:21

# Defina o diretório de trabalho no container
WORKDIR /app

# Copie o JAR gerado pela etapa de build para o container
COPY --from=builder /app/target/lumos-spring-0.0.1-SNAPSHOT.jar /app/lumos-spring.jar

# Exponha a porta 8080 para o Spring Boot
EXPOSE 8080

# Comando para rodar a aplicação Spring Boot
CMD ["java", "--enable-preview", "-jar", "lumos-spring.jar"]
