#!/bin/bash

FEATURES_DIR="src/features"

REQUIRED_FOLDERS=(
  "api"
  "hooks"
  "components"
  "pages"
  "stores"
  "types"
  "utils"
  "validations"
)

for feature in "$FEATURES_DIR"/*; do
  if [ -d "$feature" ]; then
    echo "Verificando feature: $(basename "$feature")"

    for folder in "${REQUIRED_FOLDERS[@]}"; do
      TARGET="$feature/$folder"

      if [ ! -d "$TARGET" ]; then
        mkdir -p "$TARGET"
        touch "$TARGET/.gitkeep"

        echo "  Criada pasta: $folder"
      fi
    done
  fi
done

echo ""
echo "Estrutura das features validada com sucesso!"