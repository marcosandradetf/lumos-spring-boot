import { useEffect, useMemo, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useMutation, useQuery } from '@tanstack/react-query';

import { useAppStore } from '@/store/use-app-store';
import { useNotify } from '@/shared/hooks/use-notify';
import { billingApi } from '@/features/billing/api/billingApi';
import { GlassListbox } from '@/shared/components/glass-list-box';

const reasonMap: Record<string, string> = {
  teste_finalizado: 'Seu período de teste foi finalizado.',
  expirado: 'Sua assinatura expirou.',
  cancelado: 'Sua assinatura foi cancelada.',
};

const statusTone: Record<string, string> = {
  ACTIVE: 'bg-emerald-100 text-emerald-700 dark:bg-emerald-500/20 dark:text-emerald-200',
  TRIAL: 'bg-sky-100 text-sky-700 dark:bg-sky-500/20 dark:text-sky-200',
  PAST_DUE: 'bg-amber-100 text-amber-700 dark:bg-amber-500/20 dark:text-amber-200',
  CANCELED: 'bg-rose-100 text-rose-700 dark:bg-rose-500/20 dark:text-rose-200',
  EXPIRED: 'bg-rose-100 text-rose-700 dark:bg-rose-500/20 dark:text-rose-200',
};

const BILLING_CYCLE_OPTIONS = [
  { value: 'MONTHLY', label: 'Mensal' },
  { value: 'YEARLY', label: 'Anual' },
];

export default function Billing() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const { setPageContext } = useAppStore();
  const { notify } = useNotify();

  const [renewOpen, setRenewOpen] = useState(false);
  const [upgradeOpen, setUpgradeOpen] = useState(false);
  const [billingCycle, setBillingCycle] = useState<'MONTHLY' | 'YEARLY'>('MONTHLY');
  const [planName, setPlanName] = useState('');
  const [targetPlanName, setTargetPlanName] = useState('');

  const reasonCode = searchParams.get('motivo');
  const reasonMessage = reasonCode ? reasonMap[reasonCode] ?? 'Acesso indisponível.' : null;

  const subscriptionQuery = useQuery({
    queryKey: ['billing', 'subscription-status'],
    queryFn: billingApi.getSubscriptionStatus,
  });
  const paymentMutation = useMutation({
    mutationFn: billingApi.processPayment,
  });

  useEffect(() => {
    setPageContext(['Cobrança'], 'Gerenciar Assinatura');
  }, [setPageContext]);

  const subscription = subscriptionQuery.data;
  const busy = subscriptionQuery.isLoading || paymentMutation.isPending;

  const statusLabel = useMemo(() => subscription?.status ?? 'Desconhecido', [subscription?.status]);
  const statusClass = useMemo(
    () => statusTone[subscription?.status ?? ''] ?? 'bg-slate-200 text-slate-700 dark:bg-slate-700 dark:text-slate-100',
    [subscription?.status],
  );

  const accessClass = subscription?.accessAllowed
    ? 'bg-emerald-100 text-emerald-700 dark:bg-emerald-500/20 dark:text-emerald-200'
    : 'bg-rose-100 text-rose-700 dark:bg-rose-500/20 dark:text-rose-200';

  return (
    <section className="min-h-full bg-gradient-to-br from-blue-50 to-indigo-50 p-4 dark:from-slate-950 dark:to-slate-900 md:p-6">
      <div className="mx-auto max-w-6xl space-y-6">
        <header className="text-center">
          <h1 className="text-3xl font-bold text-slate-900 dark:text-slate-100">Gerenciar Assinatura</h1>
          <p className="mt-2 text-slate-600 dark:text-slate-400">Renove ou atualize seu plano de assinatura.</p>
        </header>

        {reasonMessage && (
          <div className="rounded-xl border-l-4 border-amber-500 bg-amber-50 p-4 text-amber-800 dark:bg-amber-500/10 dark:text-amber-200">
            <i className="pi pi-exclamation-triangle mr-2" />
            {reasonMessage}
          </div>
        )}

        {busy && (
          <div className="flex justify-center py-12">
            <i className="pi pi-spin pi-spinner text-2xl text-blue-500" />
          </div>
        )}

        {!busy && subscription && (
          <>
            <div className="grid grid-cols-1 gap-6 lg:grid-cols-3">
              <article className="rounded-2xl border border-slate-200 bg-white shadow-sm dark:border-slate-800 dark:bg-slate-900 lg:col-span-2">
                <div className="rounded-t-2xl bg-gradient-to-r from-blue-600 to-cyan-500 px-6 py-5 text-white">
                  <h2 className="text-xl font-semibold">Status da Assinatura</h2>
                </div>

                <div className="space-y-4 p-6 text-sm">
                  <div className="flex items-center justify-between border-b border-slate-200 pb-3 dark:border-slate-800">
                    <span className="text-slate-500 dark:text-slate-400">Status</span>
                    <span className={`rounded-full px-2.5 py-1 text-xs font-semibold ${statusClass}`}>{statusLabel}</span>
                  </div>

                  <div className="flex items-center justify-between border-b border-slate-200 pb-3 dark:border-slate-800">
                    <span className="text-slate-500 dark:text-slate-400">Plano</span>
                    <span className="font-semibold text-slate-900 dark:text-slate-100">{subscription.planName ?? 'N/A'}</span>
                  </div>

                  {subscription.trialEndsAt && (
                    <div className="flex items-center justify-between border-b border-slate-200 pb-3 dark:border-slate-800">
                      <span className="text-slate-500 dark:text-slate-400">Fim do período de teste</span>
                      <span className="font-semibold text-slate-900 dark:text-slate-100">{new Date(subscription.trialEndsAt).toLocaleString('pt-BR')}</span>
                    </div>
                  )}

                  <div className="flex items-center justify-between border-b border-slate-200 pb-3 dark:border-slate-800">
                    <span className="text-slate-500 dark:text-slate-400">Acesso permitido</span>
                    <span className={`rounded-full px-2.5 py-1 text-xs font-semibold ${accessClass}`}>
                      {subscription.accessAllowed ? 'Sim' : 'Não'}
                    </span>
                  </div>

                  {subscription.message && (
                    <div className="text-slate-700 dark:text-slate-300">
                      <span className="font-medium">Mensagem:</span> {subscription.message}
                    </div>
                  )}
                </div>

                <footer className="flex flex-col gap-3 border-t border-slate-200 p-6 dark:border-slate-800 sm:flex-row">
                  <button
                    type="button"
                    onClick={() => {
                      setRenewOpen((prev) => !prev);
                      setUpgradeOpen(false);
                    }}
                    className="flex-1 rounded-xl bg-blue-600 px-4 py-2 text-sm font-semibold text-white hover:bg-blue-500"
                  >
                    <i className="pi pi-refresh mr-2" />Renovar Assinatura
                  </button>
                  <button
                    type="button"
                    onClick={() => {
                      setUpgradeOpen((prev) => !prev);
                      setRenewOpen(false);
                    }}
                    className="flex-1 rounded-xl bg-emerald-600 px-4 py-2 text-sm font-semibold text-white hover:bg-emerald-500"
                  >
                    <i className="pi pi-arrow-up mr-2" />Fazer Upgrade
                  </button>
                </footer>
              </article>

              <aside className="space-y-4">
                <article className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm dark:border-slate-800 dark:bg-slate-900">
                  <h3 className="text-sm font-semibold text-slate-900 dark:text-slate-100">Ações rápidas</h3>
                  <div className="mt-4 space-y-2">
                    <button
                      type="button"
                      disabled={paymentMutation.isPending}
                      onClick={() => paymentMutation.mutate({}, {
                        onSuccess: () => notify('Pagamento processado com sucesso.', 'success'),
                        onError: (error: unknown) => {
                          const message = (error as { response?: { data?: { message?: string } } })?.response?.data?.message;
                          notify(message ?? 'Erro ao processar pagamento.', 'error');
                        },
                      })}
                      className="w-full rounded-xl bg-amber-500 px-4 py-2 text-sm font-semibold text-black hover:bg-amber-400 disabled:opacity-50"
                    >
                      <i className="pi pi-credit-card mr-2" />Processar Pagamento
                    </button>
                    <button
                      type="button"
                      onClick={() => navigate('/dashboard')}
                      className="w-full rounded-xl border border-slate-300 px-4 py-2 text-sm font-semibold text-slate-700 hover:bg-slate-50 dark:border-slate-700 dark:text-slate-200 dark:hover:bg-slate-800"
                    >
                      <i className="pi pi-home mr-2" />Voltar ao Dashboard
                    </button>
                  </div>
                </article>

                <article className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm dark:border-slate-800 dark:bg-slate-900">
                  <h3 className="text-sm font-semibold text-slate-900 dark:text-slate-100">Precisa de ajuda?</h3>
                  <p className="mt-2 text-sm text-slate-500 dark:text-slate-400">Entre em contato com o suporte para dúvidas sobre assinatura.</p>
                  <a
                    href="mailto:support@lumos.com?subject=Suporte%20Lumos&body=Ol%C3%A1%2C%20preciso%20de%20suporte%20com%20minha%20assinatura."
                    className="mt-3 inline-flex text-sm font-semibold text-blue-600 hover:text-blue-500"
                  >
                    Contatar suporte
                  </a>
                </article>
              </aside>
            </div>

            {(renewOpen || upgradeOpen) && (
              <div className="grid grid-cols-1 gap-6 md:grid-cols-2">
                {renewOpen && (
                  <article className="rounded-2xl border-2 border-blue-500 bg-white p-6 shadow-sm dark:bg-slate-900">
                    <h3 className="text-lg font-semibold text-slate-900 dark:text-slate-100">Renovar assinatura</h3>
                    <div className="mt-4 space-y-4">
                      <div className="space-y-1">
                        <label className="text-xs font-medium text-slate-500 dark:text-slate-400">Ciclo de cobrança</label>
                        <GlassListbox
                          value={billingCycle}
                          onChange={(value) => setBillingCycle(value as 'MONTHLY' | 'YEARLY')}
                          options={BILLING_CYCLE_OPTIONS}
                        />
                      </div>
                      <div className="space-y-1">
                        <label className="text-xs font-medium text-slate-500 dark:text-slate-400">Plano (opcional)</label>
                        <input
                          value={planName}
                          onChange={(event) => setPlanName(event.target.value)}
                          placeholder="Deixe em branco para manter o plano atual"
                          className="w-full rounded-xl border border-slate-200 bg-white px-3 py-2 text-sm text-slate-800 outline-none focus:border-blue-400 dark:border-slate-700 dark:bg-slate-900 dark:text-slate-100"
                        />
                      </div>
                      <div className="rounded-xl bg-slate-100 p-3 text-xs text-slate-600 dark:bg-slate-800 dark:text-slate-300">
                        Renovação disponível na API. A chamada será habilitada após confirmação de regras comerciais.
                      </div>
                    </div>
                  </article>
                )}

                {upgradeOpen && (
                  <article className="rounded-2xl border-2 border-emerald-500 bg-white p-6 shadow-sm dark:bg-slate-900">
                    <h3 className="text-lg font-semibold text-slate-900 dark:text-slate-100">Fazer upgrade</h3>
                    <div className="mt-4 space-y-4">
                      <div className="space-y-1">
                        <label className="text-xs font-medium text-slate-500 dark:text-slate-400">Novo plano</label>
                        <input
                          value={targetPlanName}
                          onChange={(event) => setTargetPlanName(event.target.value)}
                          placeholder="Digite o nome do novo plano"
                          className="w-full rounded-xl border border-slate-200 bg-white px-3 py-2 text-sm text-slate-800 outline-none focus:border-blue-400 dark:border-slate-700 dark:bg-slate-900 dark:text-slate-100"
                        />
                      </div>
                      <div className="rounded-xl bg-slate-100 p-3 text-xs text-slate-600 dark:bg-slate-800 dark:text-slate-300">
                        Upgrade disponível na API. A chamada será habilitada após confirmação de regras comerciais.
                      </div>
                    </div>
                  </article>
                )}
              </div>
            )}
          </>
        )}
      </div>
    </section>
  );
}
