#!/bin/bash

FEATURE=$1

mkdir -p src/features/$FEATURE/{api,components,hooks,pages,stores,types,utils,validations}

touch src/features/$FEATURE/index.ts

echo "Feature $FEATURE criada com estrutura completa!"