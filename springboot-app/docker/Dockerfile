# Usando a imagem base do OpenJDK para compilar e rodar o Spring Boot
FROM openjdk:21-jdk-slim

# Defina o diretório de trabalho no container
WORKDIR /app

# Copie o arquivo JAR da aplicação Spring Boot para o container
COPY ./springboot-app/target/springboot-app.jar /app/springboot-app.jar

# Exponha a porta 8080 para o Spring Boot
EXPOSE 8080

# Comando para rodar a aplicação Spring Boot
CMD ["java", "-jar", "springboot-app.jar"]
