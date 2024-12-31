# Use a imagem oficial do Node.js
FROM node:22-alpine

# Cria e define o diretório de trabalho dentro do container
WORKDIR /app

# Copia o package.json e o package-lock.json para o container
COPY package*.json ./

# Instala as dependências do Angular
RUN npm install

# Copia todo o código da aplicação para o container
COPY . .

# Exponha a porta do Angular
EXPOSE 4200

# Executa o servidor de desenvolvimento Angular com hot reload
CMD ["npm", "start"]
