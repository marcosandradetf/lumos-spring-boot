#FROM ghcr.io/graalvm/native-image-community:21 AS build
#
## Instala Maven e ferramentas básicas
#RUN microdnf install -y maven git unzip && microdnf clean all
#
#WORKDIR /app
#
## Copia o projeto
#COPY maintenance-service/pom.xml .
#COPY maintenance-service/src ./src
#
## Compila em modo nativo
#RUN mvn package -Dnative -DskipTests

# Imagem final leve para produção
#FROM quay.io/quarkus/ubi9-quarkus-micro-image:2.0
#WORKDIR /work/
#COPY --from=build /app/target/*-runner /work/application
#
#EXPOSE 8081
#USER 1001
#ENTRYPOINT ["./application", "-Dquarkus.http.host=0.0.0.0"]

FROM quay.io/quarkus/ubi9-quarkus-micro-image:2.0

WORKDIR /work/
COPY application /work/application

EXPOSE 8081

USER 1001
ENTRYPOINT ["./application", "-Dquarkus.http.host=0.0.0.0"]