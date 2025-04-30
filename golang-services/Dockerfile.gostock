# Etapa de construção
FROM golang:1.23-alpine AS build

# Criar o diretório ~/.ssh dentro do diretório home do usuário root
RUN mkdir -p /root/.ssh && chmod 700 /root/.ssh

# Copiar a chave pública para o diretório ~/.ssh do usuário root
COPY ./keys/app.pub /root/.ssh/app.pub

# Ajustar permissões da chave pública
RUN chmod 644 /root/.ssh/app.pub

# Copiar os arquivos do módulo e baixar as dependências
WORKDIR /app
COPY go.mod .
COPY go.sum .
RUN go mod download

# Copiar o código-fonte
COPY . .

# Compilar o binário
ENV CGO_ENABLED=0 GOOS=linux GOARCH=amd64
RUN go build -o /myapp ./cmd/import-data

# Etapa final
FROM alpine:latest AS run

# Criar o diretório ~/.ssh dentro do diretório home do usuário root
RUN mkdir -p /root/.ssh && chmod 700 /root/.ssh

# Copiar a chave pública para o diretório ~/.ssh do usuário root
COPY --from=build /root/.ssh/app.pub /root/.ssh/app.pub

# Ajustar permissões da chave pública
RUN chmod 644 /root/.ssh/app.pub

# Copiar o binário do estágio de construção
COPY --from=build /myapp /myapp

# Rodar o aplicativo
WORKDIR /app
EXPOSE 8082
CMD ["/myapp"]