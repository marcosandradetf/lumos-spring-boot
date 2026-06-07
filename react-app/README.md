# 🚀 Frontend Architecture Guide

Documentação oficial da arquitetura frontend do projeto.

Objetivo:
Garantir consistência, escalabilidade e previsibilidade no desenvolvimento do sistema.

Stack principal:

* React
* TypeScript
* Vite
* Zustand
* TanStack Query
* Zod
* Capacitor

---

# 📁 Estrutura do Projeto

```txt
src/
├── app/
├── core/
├── features/
├── shared/
```

---

# 📦 O que significa cada pasta

---

## app/

Responsável pela inicialização da aplicação.

Aqui ficam:

* providers globais
* configuração de rotas
* QueryClientProvider
* tema global
* App.tsx
* bootstrap da aplicação

Exemplo:

```txt
app/
├── providers/
├── routes/
├── App.tsx
├── main.tsx
```

---

## core/

Tudo que é infraestrutura da aplicação.

Aqui ficam:

* configuração do axios
* interceptors
* auth base
* config
* constants globais
* integração com Capacitor
* Query Client
* libs compartilhadas da aplicação

Exemplo:

```txt
core/
├── api/
│   ├── httpClient.ts
│   ├── interceptors.ts
│
├── config/
├── constants/
├── query/
│   └── queryClient.ts
```

---

## features/

Coração do sistema.

Cada domínio/regra de negócio deve viver isoladamente em uma feature.

Exemplos:

```txt
features/
├── users/
├── contracts/
├── dashboard/
├── auth/
```

A feature deve conter tudo relacionado ao domínio dela.

---

## shared/

Apenas código reutilizável SEM regra de negócio.

Pode conter:

* componentes UI
* hooks genéricos
* layouts
* helpers
* utilidades reutilizáveis

NÃO colocar:

* lógica de contrato
* lógica de usuário
* regra específica de negócio

---

# 🧩 Estrutura Oficial de uma Feature

Toda feature DEVE seguir exatamente esta estrutura:

```txt
features/<feature-name>/
├── api/
├── hooks/
├── components/
├── pages/
├── stores/
├── types/
├── validations/
├── utils/
├── index.ts
```

Exemplo:

```txt
features/users/
├── api/
├── hooks/
├── components/
├── pages/
├── stores/
├── types/
├── validations/
├── utils/
├── index.ts
```

---

# 📚 O que cada pasta da feature significa

---

## api/

Responsável APENAS pela comunicação com backend.

Deve conter:

* chamadas HTTP
* integração com API
* requests
* mutations
* funções async

NÃO deve conter:

* regra de UI
* estado React
* JSX
* lógica visual

Exemplo:

```ts
export async function getUsers() {
  const { data } = await httpClient.get('/users')
  return data
}
```

---

## hooks/

Hooks customizados da feature.

Responsável por:

* TanStack Query
* cache
* loading
* mutations
* query keys
* composição de lógica

Fluxo correto:

```txt
Page
↓
Hook
↓
API
↓
Backend
```

NUNCA:

```txt
Page -> axios diretamente
```

Exemplo:

```ts
export function useUsers() {
  return useQuery({
    queryKey: ['users'],
    queryFn: getUsers,
  })
}
```

---

## components/

Componentes visuais da feature.

Responsabilidade:

* renderização
* interação visual
* composição de UI

Evitar:

* fetch
* axios
* regras grandes
* lógica pesada

Componentes devem ser pequenos e previsíveis.

---

## pages/

Páginas da feature.

Responsável apenas por:

* composição da tela
* organização da página
* consumo dos hooks

Pages NÃO devem conter:

* lógica complexa
* chamadas HTTP
* regras pesadas

---

## stores/

Estado global da feature usando Zustand.

Usar SOMENTE quando necessário.

Permitido:

* auth
* filtros
* tema
* pequenos estados globais

Evitar:

* mega stores
* store gigante centralizada
* regra de negócio excessiva

---

## types/

Centralização de interfaces e tipos da feature.

Exemplo:

```ts
export interface User {
  id: string
  name: string
}
```

---

## validations/

Schemas Zod.

Responsável por:

* validação de formulários
* validação de payload
* validação de inputs

Exemplo:

```ts
export const userSchema = z.object({
  name: z.string(),
  email: z.string().email(),
})
```

---

## utils/

Funções utilitárias da feature.

Exemplos:

* formatadores
* mapeadores
* helpers específicos da feature

Evitar:

* lógica pesada
* regra de negócio crítica

---

## index.ts

Barrel file da feature.

Responsável por exportar:

* hooks
* components
* pages
* types

Objetivo:

Melhorar imports e organização.

Exemplo:

```ts
export * from './hooks/useUsers'
export * from './pages/UsersPage'
```

---

# 🔥 Regras Oficiais do Projeto

---

## 1. Toda regra de negócio deve viver dentro da feature

Errado:

```txt
shared/
```

Correto:

```txt
features/contracts/
```

---

## 2. Componentes NÃO fazem request

❌ Errado:

```ts
axios.get('/users')
```

✅ Correto:

```txt
Component -> Hook -> API
```

---

## 3. Nunca usar useEffect para fetch manual

Sempre usar:

* useQuery
* useMutation

---

## 4. TanStack Query é responsável pelo server state

Server State:

* dados da API
* cache
* loading
* sync backend

Client State:

* modal aberto
* tema
* filtros locais

---

## 5. Zustand NÃO substitui React Query

Zustand é apenas para estados globais pequenos.

---

## 6. Shared NÃO pode ter regra de negócio

Shared é reutilização genérica.

---

## 7. Evitar God Components

Se o componente cresceu demais:

* quebrar
* compor
* extrair hooks

---

## 8. Evitar Hooks Gigantes

Hooks devem ser pequenos e focados.

---

## 9. Evitar arquivos gigantes

Preferir:

* modularização
* separação por responsabilidade

---

## 10. Preferir composição funcional

Evitar:

* classes
* arquitetura estilo Angular
* abstrações desnecessárias

---

# 🧠 Padrões Oficiais

---

# Nome de APIs

```txt
usersApi.ts
contractsApi.ts
authApi.ts
```

Evitar:

```txt
userService.ts
contractService.ts
```

---

# Nome de Hooks

```txt
useUsers.ts
useCreateContract.ts
useAuth.ts
```

---

# Nome de Components

```txt
UserCard.tsx
ContractTable.tsx
```

---

# Nome de Pages

```txt
UsersPage.tsx
ContractsPage.tsx
```

---

# 🧭 Alias de Imports

Usar aliases:

```ts
@/features
@/shared
@/core
```

Exemplo:

```ts
import { useUsers } from '@/features/users'
```

Evitar:

```ts
../../../hooks/useUsers
```

---

# ⚡ TanStack Query

Toda feature deve organizar query keys.

Exemplo:

```ts
export const usersKeys = {
  all: ['users'],
  list: () => [...usersKeys.all, 'list'],
  detail: (id: string) => [...usersKeys.all, id],
}
```

---

# 🔄 Invalidation

Após mutation:

```ts
queryClient.invalidateQueries({
  queryKey: usersKeys.all,
})
```

---

# 🏗 Estrutura Recomendada de Fluxo

```txt
pages/
↓
hooks/
↓
api/
↓
backend
```

---

# 🎯 Objetivo Final da Arquitetura

O projeto deve permanecer:

* modular
* simples
* previsível
* escalável
* consistente
* fácil onboarding
* fácil manutenção

---

# 🚫 O que evitar

* overengineering
* abstrações excessivas
* mega stores
* mega hooks
* componentes gigantes
* lógica espalhada
* fetch manual
* axios dentro de components/pages

---

# ✅ O que priorizar

Sempre priorizar:

1. Legibilidade
2. Simplicidade
3. Consistência
4. Manutenção
5. Previsibilidade

---

# 🧱 Exemplo de Fluxo Correto

```txt
UsersPage
↓
useUsers
↓
usersApi
↓
Backend
```

---

# 🛠 Script de criação de Feature

```bash
#!/bin/bash

FEATURE=$1

mkdir -p src/features/$FEATURE/{api,components,hooks,pages,stores,types,utils,validations}

touch src/features/$FEATURE/index.ts

echo "Feature $FEATURE criada com estrutura completa!"
```

Uso:

```bash
./create-feature.sh users
```

---

# 📌 Regra Final

Se surgir dúvida:

* manter simples
* manter previsível
* manter consistente

A arquitetura existe para ajudar o time a desenvolver mais rápido e com menos retrabalho.
