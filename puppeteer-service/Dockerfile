# Stage 1: Build
FROM node:20-slim AS build

WORKDIR /app
COPY package*.json ./
RUN npm ci

COPY . .
RUN npx tsc

# Stage 2: Production image
FROM node:20-slim

ENV PUPPETEER_SKIP_DOWNLOAD=true \
    NODE_ENV=production \
    TZ=America/Sao_Paulo

RUN apt-get update && apt-get install -y \
    chromium \
    ca-certificates \
    fonts-liberation \
    libasound2 \
    libatk-bridge2.0-0 \
    libatk1.0-0 \
    libc6 \
    libcairo2 \
    libcups2 \
    libdbus-1-3 \
    libexpat1 \
    libfontconfig1 \
    libgbm1 \
    libglib2.0-0 \
    libgtk-3-0 \
    libnspr4 \
    libnss3 \
    libpango-1.0-0 \
    libx11-6 \
    libxcomposite1 \
    libxdamage1 \
    libxext6 \
    libxfixes3 \
    libxrandr2 \
    xdg-utils \
    wget \
    --no-install-recommends && apt-get clean && rm -rf /var/lib/apt/lists/*

RUN ln -s /usr/bin/chromium /usr/bin/google-chrome

WORKDIR /app

# Copy only production dependencies and built files from build stage
COPY --from=build /app/package*.json ./
RUN npm ci --omit=dev
COPY --from=build /app/dist ./dist

EXPOSE 3000

CMD ["node", "dist/index.js"]
