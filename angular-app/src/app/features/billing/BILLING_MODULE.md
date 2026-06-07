# Módulo de Cobrança - Documentação

## Visão Geral

O módulo de cobrança fornece duas páginas para gerenciar assinaturas e acesso de usuários:

1. **Página de Cobrança** (`/cobranca`) - Para administradores gerenciarem suas assinaturas
2. **Página de Acesso Indisponível** (`/acesso-indisponivel`) - Exibida quando o acesso está bloqueado

## Componentes

### 1. BillingComponent (`/cobranca`)

**Localização:** `src/app/billing/billing.component.ts`

**Descrição:** Página de gerenciamento de assinatura onde admins podem visualizar o status e executar ações.

**Funcionalidades:**
- Visualizar status atual da assinatura
- Renovar assinatura (mensal ou anual)
- Fazer upgrade para novo plano
- Processar pagamento
- Visualizar período de teste (se aplicável)

**Endpoints utilizados:**
- `GET /api/billing/subscription-status` - Obter status
- `POST /api/billing/renew` - Renovar assinatura
- `POST /api/billing/upgrade` - Fazer upgrade
- `POST /api/billing/payment` - Processar pagamento

**Query Parameters:**
- `motivo`: Opcional. Motivo pelo qual a página foi acessada (teste_finalizado, expirado, cancelado)

**Exemplo de acesso com parâmetro:**
```
/cobranca?motivo=teste_finalizado&action=upgrade
```

### 2. UnavailableAccessComponent (`/acesso-indisponivel`)

**Localização:** `src/app/billing/unavailable-access.component.ts`

**Descrição:** Página exibida quando o acesso está bloqueado por problemas de assinatura.

**Funcionalidades:**
- Exibir motivo do bloqueio
- Botão para ir para gerenciamento de assinatura
- Opção de logout

## Modelo de Dados

### SubscriptionStatusResponse
```typescript
{
  planName: string | null;           // Nome do plano (Pro, Enterprise, etc)
  status: string | null;              // TRIAL, ACTIVE, PAST_DUE, CANCELED, EXPIRED
  trialEndsAt: string | null;         // Data fim do período de teste (ISO 8601)
  accessAllowed: boolean;             // Se acesso está permitido
  message: string | null;             // Mensagem adicional
}
```

### RenewRequest
```typescript
{
  planName?: string | null;           // Nome do plano (optional)
  billingCycle: 'MONTHLY' | 'YEARLY'; // Ciclo de cobrança
}
```

### UpgradeRequest
```typescript
{
  targetPlanName: string;  // Nome do novo plano (required)
}
```

### PaymentRequest
```typescript
{
  billingEmail?: string | null;  // Email para cobrança (optional)
  invoiceId?: string | null;     // ID da fatura (optional)
}
```

## Fluxo de Integração com Auth Interceptor

O `AuthInterceptor` captura erros 403 e redireciona para as páginas de cobrança baseado em códigos de erro:

```typescript
// Códigos de erro que disparam redirecionamento
TRIAL_EXPIRED → /cobranca?motivo=teste_finalizado
SUBSCRIPTION_EXPIRED → /cobranca?motivo=expirado
SUBSCRIPTION_CANCELED → /cobranca?motivo=cancelado
```

Se o usuário for **ADMIN**: Redireciona para `/cobranca` com `action=upgrade`
Se o usuário **NÃO for ADMIN**: Redireciona para `/acesso-indisponivel`

## Serviço de Cobrança

### BillingService

**Localização:** `src/app/billing/services/billing.service.ts`

**Métodos disponíveis:**

#### getSubscriptionStatus()
```typescript
getSubscriptionStatus(): Observable<SubscriptionStatusResponse>
```
Obtém o status atual da assinatura do tenant autenticado.

#### renewSubscription(request: RenewRequest)
```typescript
renewSubscription(request: RenewRequest): Observable<SubscriptionMutationResponse>
```
Renova a assinatura com novo ciclo de cobrança.

#### upgradeSubscription(request: UpgradeRequest)
```typescript
upgradeSubscription(request: UpgradeRequest): Observable<SubscriptionMutationResponse>
```
Faz upgrade para novo plano de assinatura.

#### processPayment(request: PaymentRequest)
```typescript
processPayment(request: PaymentRequest): Observable<PaymentProcessResult>
```
Processa um pagamento para assinatura.

## Estilos e Responsividade

Ambos componentes incluem:
- Design responsivo (mobile-first)
- Dark mode compatible (usando CSS variables)
- Loading states e spinners
- Mensagens de erro claras
- Animações suaves

## Roteamento

As rotas estão configuradas em `src/app/app.routes.ts`:

```typescript
{
  path: 'cobranca',
  loadComponent: () => import('./billing/billing.component').then(m => m.BillingComponent),
  canActivate: [AuthGuard],
  data: {role: ['ADMIN'], path: 'cobranca'},
},
{
  path: 'acesso-indisponivel',
  loadComponent: () => import('./billing/unavailable-access.component').then(m => m.UnavailableAccessComponent),
  canActivate: [AuthGuard],
  data: {role: [], path: 'acesso-indisponivel'},
}
```

## Tratamento de Erros

Ambos componentes incluem:
- Tratamento de erros HTTP
- Mensagens de erro user-friendly
- Retry logic (reload da página)
- Fallback para logout em caso de erro crítico

## Testes

Execute os testes unitários:
```bash
npm run test -- billing.service.spec.ts
```

## Segurança

- ✅ Autenticação obrigatória via AuthGuard
- ✅ Tokens JWT adicionados automaticamente via AuthInterceptor
- ✅ CORS habilitado com `withCredentials: true`
- ✅ Endpoints privados que requerem autenticação
- ✅ Validação no backend dos acessos

## Exemplo de Uso Completo

### 1. Usuário tenta acessar recurso mas assinatura expirou
```
↓ Backend retorna 403 com error code: SUBSCRIPTION_EXPIRED
↓ AuthInterceptor captura erro 403
↓ Redireciona para /cobranca?motivo=expirado
↓ BillingComponent carrega e exibe status
↓ Usuário pode renovar ou fazer upgrade
```

### 2. Usuário não admin tenta acessar
```
↓ Backend retorna 403 com error code: SUBSCRIPTION_CANCELED
↓ AuthInterceptor redireciona para /acesso-indisponivel
↓ UnavailableAccessComponent explica motivo
↓ Usuário pode fazer logout
```

## Melhorias Futuras

- [ ] Integração com gateway de pagamento (Stripe, PagSeguro)
- [ ] Histórico de faturas e pagamentos
- [ ] Gerenciamento de assentos/usuários por plano
- [ ] Notificações de expiração iminente
- [ ] Suporte para múltiplos idiomas
- [ ] Analytics e tracking de conversão
