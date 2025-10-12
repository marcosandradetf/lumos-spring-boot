#!/bin/bash
set -e

echo "Atualizando Android SDK..."
sdkmanager --update

echo "Instalando plataformas e build-tools..."
sdkmanager "platforms;android-34" "build-tools;34.0.0"

echo "Instalando ferramentas extras..."
sdkmanager "platform-tools" "cmdline-tools;latest"

echo "Instalando Kotlin via SDKMAN..."
curl -s https://get.sdkman.io | bash
# Inicializa o SDKMAN no script para instalar Kotlin
bash -c "source $HOME/.sdkman/bin/sdkman-init.sh && sdk install kotlin"

echo "Configuração Android e Kotlin concluída!"