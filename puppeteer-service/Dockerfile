FROM node:20-slim

# Dependências para o Chromium
RUN apt-get update && apt-get install -y \
  fonts-liberation \
  libasound2 \
  libatk-bridge2.0-0 \
  libatk1.0-0 \
  libcups2 \
  libdbus-1-3 \
  libdrm2 \
  libxcomposite1 \
  libxdamage1 \
  libxrandr2 \
  xdg-utils \
  wget \
  ca-certificates \
  --no-install-recommends && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY package.json tsconfig.json ./
RUN npm install

COPY src ./src
RUN npm run build

CMD ["node", "dist/index.js"]
