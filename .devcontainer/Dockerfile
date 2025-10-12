# Base mínima Ubuntu 22.04
FROM mcr.microsoft.com/devcontainers/base:ubuntu-22.04

# Evita prompts interativos
ARG DEBIAN_FRONTEND=noninteractive

# Instala utilitários essenciais + JDK + Maven + Node (opcional)
RUN apt-get update && apt-get install -y \
    curl \
    wget \
    git \
    unzip \
    zip \
    procps \
    sudo \
    bash \
    coreutils \
    openjdk-21-jdk \
    maven \
    && rm -rf /var/lib/apt/lists/*

# Define JAVA_HOME
ENV JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
ENV PATH=$JAVA_HOME/bin:$PATH

# Android SDK
ENV ANDROID_HOME=/opt/android-sdk
ENV PATH=$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH

# Node.js e Angular CLI instaláveis via feature devcontainer
