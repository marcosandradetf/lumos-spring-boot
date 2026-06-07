import { useEffect, useMemo, useState } from 'react';
import { Outlet, useNavigate, useOutlet } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';

import { useAppStore } from '@/store/use-app-store';
import { useNotify } from '@/shared/hooks/use-notify';
import { useNotificationStore } from '@/core/notifications/useNotificationStore';
import { useAuthStore } from '@/core/auth/useAuthStore';
import { useUsers } from '@/features/manage/hooks/useUsers';
import { useTeams } from '@/features/manage/hooks/useTeams';
import type { ManagedUser } from '@/features/manage/types/manageTypes';
import { contractsApi } from '@/features/contract/api/contractsApi';
import { contractKeys } from '@/features/contract/api/contractQueryKeys';
import { useCatalogue } from '@/features/stock/hooks/use-catalogue';
import { useDeposits } from '@/features/stock/hooks/use-deposits';
import { useStockists } from '@/features/stock/hooks/use-stockists';

interface OnboardingStep {
  id: string;
  title: string;
  description: string;
  helper: string;
  icon: string;
  route: string;
  ctaLabel: string;
  done: boolean;
  metric: string;
  blockers: string[];
  externalUrl?: boolean;
  disabled: boolean;
}

const EMBEDDED_ROUTE_MAP: Record<string, string> = {
  '/configuracoes/usuarios': 'usuarios',
  '/configuracoes/equipes': 'equipes',
  '/configuracoes/estoquistas': 'estoquistas',
  '/estoque/almoxarifados': 'almoxarifados',
  '/estoque/caminhoes': 'caminhoes',
  '/estoque/cadastrar-material': 'cadastrar-material',
  '/contratos/itens-contratuais/cadastro': 'itens-contratuais',
  '/contratos/criar': 'criar-contrato',
};

function normalizeRoleTokens(user: ManagedUser): string[] {
  return (user.role ?? []).flatMap((roleItem) => {
    const values = [
      roleItem?.roleId,
      roleItem?.label,
      (roleItem as unknown as { roleName?: string })?.roleName,
    ].filter(Boolean) as string[];
    return values.map((value) => String(value).toUpperCase());
  });
}

export default function Settings() {
  const navigate = useNavigate();
  const outlet = useOutlet();
  const { setPageContext, setOnboarding } = useAppStore();
  const { notify } = useNotify();
  const { user } = useAuthStore();
  const { status: notificationStatus, initialize: initializeNotifications, requestPermission } = useNotificationStore();

  const [activeStepIndex, setActiveStepIndex] = useState(0);
  const [fieldActivationDone, setFieldActivationDone] = useState(localStorage.getItem('isFinishedTeamInstruction') === 'true');
  const [embeddedExternalUrl, setEmbeddedExternalUrl] = useState<string | null>(null);
  useEffect(() => {
    setPageContext(['Primeiros passos'], 'Primeiros passos');
    initializeNotifications();
  }, [initializeNotifications, setPageContext]);

  const contractFilters = useMemo(() => ({
    contractor: null,
    startDate: new Date(new Date().setMonth(new Date().getMonth() - 6)),
    endDate: new Date(),
    status: null,
  }), []);

  const { data: referenceContractItems = [], isLoading: loadingReferenceItems } = useQuery({
    queryKey: contractKeys.referenceItems(),
    queryFn: contractsApi.getContractReferenceItems,
  });
  const { data: contracts = [], isLoading: loadingContracts } = useQuery({
    queryKey: contractKeys.list(contractFilters),
    queryFn: () => contractsApi.getAllContracts(contractFilters),
  });
  const { data: users = [], isLoading: loadingUsers } = useUsers();
  const { data: teams = [], isLoading: loadingTeams } = useTeams();

  const { data: stockists = [], isLoading: loadingStockists } = useStockists();
  const { data: deposits = [], isLoading: loadingDeposits } = useDeposits();
  const { data: materials = [], isLoading: loadingMaterials } = useCatalogue();

  const totals = useMemo(() => {
    const usersList = users as ManagedUser[];
    const adminUsers = usersList.filter((user) => normalizeRoleTokens(user).some((token) => token.includes('ADMIN'))).length;
    const operationalUsers = usersList.filter((user) => {
      const tokens = normalizeRoleTokens(user);
      return tokens.includes('ELETRICISTA') || tokens.includes('MOTORISTA');
    }).length;
    const stockistUsers = usersList.filter((user) => {
      const tokens = normalizeRoleTokens(user);
      return tokens.includes('ESTOQUISTA') || tokens.includes('ESTOQUISTA_CHEFE');
    }).length;

    return {
      referenceContractItems: referenceContractItems.length,
      totalContracts: contracts.length,
      totalUsers: usersList.length,
      adminUsers,
      operationalUsers,
      stockistUsers,
      teamsCount: teams.length,
      stockistsCount: stockists.length,
      depositsCount: (deposits as Array<{ isTruck: boolean }>).filter((deposit) => !deposit.isTruck).length,
      trucksCount: (deposits as Array<{ isTruck: boolean }>).filter((deposit) => deposit.isTruck).length,
      materialsCount: materials.length,
    };
  }, [contracts.length, deposits, materials.length, referenceContractItems.length, stockists.length, teams.length, users]);

  const onboardingSteps = useMemo<OnboardingStep[]>(() => {
    return [
      {
        id: 'users',
        title: 'Estruture os acessos',
        description: 'Cadastre perfis administrativos e operacionais para destravar os vínculos das próximas etapas.',
        helper: 'Sua operação precisa de estoquistas, eletricistas e motoristas previamente cadastrados.',
        icon: 'pi pi-users',
        route: '/configuracoes/usuarios',
        ctaLabel: totals.totalUsers === 0 ? 'Cadastrar usuários...' : 'Revisar usuários...',
        done: totals.totalUsers > 0 && totals.operationalUsers > 0 && totals.stockistUsers > 0,
        metric: `Usuários: ${totals.totalUsers}`,
        blockers: [
          totals.totalUsers === 0 ? 'Nenhum usuário cadastrado.' : '',
          totals.operationalUsers === 0 ? 'Cadastrar eletricistas e motoristas.' : '',
          totals.stockistUsers === 0 ? 'Cadastrar estoquistas.' : '',
        ].filter(Boolean),
        disabled: false,
      },
      {
        id: 'teams',
        title: 'Monte a equipe operacional',
        description: 'Crie equipes com colaboradores, região de atuação e vínculo de caminhão para liberar a operação em campo.',
        helper: 'Essa etapa destrava ordens de serviço e também reflete nos caminhões do estoque.',
        icon: 'pi pi-user-edit',
        route: '/configuracoes/equipes',
        ctaLabel: totals.teamsCount === 0 ? 'Cadastrar equipes...' : 'Gerenciar equipes...',
        done: totals.teamsCount > 0,
        metric: `Equipes: ${totals.teamsCount}`,
        blockers: [
          totals.operationalUsers === 0 ? 'Voltar ao passo anterior e cadastrar usuários operacionais.' : '',
          totals.teamsCount === 0 ? 'Cadastrar as equipes operacionais.' : '',
        ].filter(Boolean),
        disabled: totals.operationalUsers === 0,
      },
      {
        id: 'field-activation',
        title: 'Coloque a equipe em campo',
        description: 'Garanta que a equipe operacional tenha acesso ao aplicativo e esteja pronta para iniciar as atividades em campo.',
        helper: totals.teamsCount === 0
          ? 'Crie uma equipe operacional antes de iniciar a operação.'
          : 'Peça para a equipe baixar o app, fazer login e seguir as instruções iniciais.',
        icon: 'pi pi-mobile',
        route: 'https://lumosip.com.br/como-usar/07-operation/01-android-app-admin/',
        externalUrl: true,
        ctaLabel: 'Ver instruções...',
        done: fieldActivationDone,
        metric: fieldActivationDone ? 'Equipe pronta para operação' : 'Ativação pendente',
        blockers: [
          totals.operationalUsers === 0 ? 'Voltar ao passo 1 e cadastrar usuários operacionais.' : '',
          totals.teamsCount === 0 ? 'Voltar ao passo anterior e cadastrar as equipes operacionais.' : '',
          !fieldActivationDone ? 'Orientar a equipe a baixar o aplicativo e acessar o sistema.' : '',
        ].filter(Boolean),
        disabled: totals.operationalUsers === 0 || totals.teamsCount === 0,
      },
      {
        id: 'deposits',
        title: 'Prepare a base logística',
        description: 'Configure almoxarifados e caminhões para garantir origem, destino e rastreabilidade do material.',
        helper: 'Movimentação de estoque depende de pelo menos um almoxarifado e um caminhão.',
        icon: 'pi pi-truck',
        route: totals.depositsCount === 0 ? '/estoque/almoxarifados' : '/estoque/caminhoes',
        ctaLabel: totals.depositsCount === 0 ? 'Cadastrar almoxarifados...' : totals.trucksCount === 0 ? 'Cadastrar caminhões...' : 'Revisar logística...',
        done: totals.depositsCount > 0 && totals.trucksCount > 0,
        metric: `Almoxarifados: ${totals.depositsCount} • Caminhões: ${totals.trucksCount}`,
        blockers: [
          totals.depositsCount === 0 ? 'Cadastrar almoxarifados fixos.' : '',
          totals.trucksCount === 0 ? 'Cadastrar almoxarifados móveis/caminhões.' : '',
        ].filter(Boolean),
        disabled: false,
      },
      {
        id: 'stockists',
        title: 'Defina os estoquistas',
        description: 'Associe responsáveis pelo estoque para liberar o gerenciamento de reservas e expedições.',
        helper: 'Essa etapa é obrigatória para criação e acompanhamento de ordens com movimentação de material.',
        icon: 'pi pi-briefcase',
        route: '/configuracoes/estoquistas',
        ctaLabel: totals.stockistsCount === 0 ? 'Cadastrar estoquistas...' : 'Gerenciar estoquistas...',
        done: totals.stockistsCount > 0,
        metric: `Estoquistas: ${totals.stockistsCount}`,
        blockers: [
          totals.depositsCount === 0 ? 'Voltar ao passo anterior e cadastrar os almoxarifados.' : '',
          totals.stockistsCount === 0 ? 'Cadastrar os estoquistas.' : '',
        ].filter(Boolean),
        disabled: totals.depositsCount === 0,
      },
      {
        id: 'materials',
        title: 'Abasteça o catálogo de materiais',
        description: 'Cadastre materiais para que estoque, contratos e execuções consigam operar sem bloqueios.',
        helper: 'Sem catálogo não há movimentação, separação e apontamento de itens.',
        icon: 'pi pi-box',
        route: '/estoque/cadastrar-material',
        ctaLabel: totals.materialsCount === 0 ? 'Cadastrar materiais...' : 'Ver catálogo...',
        done: totals.materialsCount > 0,
        metric: `Materiais: ${totals.materialsCount}`,
        blockers: [totals.materialsCount === 0 ? 'Cadastrar o catálogo de materiais.' : ''].filter(Boolean),
        disabled: false,
      },
      {
        id: 'reference-items',
        title: 'Cadastre os itens contratuais',
        description: 'Crie os itens contratuais que serão usados como base para vincular contratos nas próximas etapas.',
        helper: 'Sem itens contratuais cadastrados, não é possível avançar na configuração de contratos.',
        icon: 'pi pi-file-edit',
        route: '/contratos/itens-contratuais/cadastro',
        ctaLabel: totals.referenceContractItems === 0 ? 'Cadastrar itens contratuais...' : 'Gerenciar itens...',
        done: totals.referenceContractItems > 0,
        metric: `Itens contratuais: ${totals.referenceContractItems}`,
        blockers: [totals.referenceContractItems === 0 ? 'Cadastrar o catálogo de itens contratuais.' : ''].filter(Boolean),
        disabled: false,
      },
      {
        id: 'contracts',
        title: 'Cadastre os contratos',
        description: 'Crie contratos para organizar e vincular seus itens contratuais e iniciar a operação.',
        helper: totals.referenceContractItems === 0
          ? 'Antes de cadastrar contratos, você precisa criar pelo menos um item contratual.'
          : 'Com os itens contratuais cadastrados, você já pode criar seus contratos.',
        icon: 'pi pi-briefcase',
        route: '/contratos/criar',
        ctaLabel: totals.totalContracts === 0 ? 'Cadastrar contratos...' : 'Gerenciar contratos...',
        done: totals.totalContracts > 0,
        metric: `Contratos: ${totals.totalContracts}`,
        blockers: [
          totals.referenceContractItems === 0 ? 'Voltar ao passo anterior e cadastrar o catálogo de itens contratuais.' : '',
          totals.totalContracts === 0 ? 'Cadastrar os contratos.' : '',
        ].filter(Boolean),
        disabled: totals.referenceContractItems === 0,
      },
      {
        id: 'notifications',
        title: 'Ative as notificações',
        description: 'Para não perder nenhum alerta importante na sua operação.',
        helper: ['default', 'denied'].includes(notificationStatus) ? 'Notificações desativadas.' : 'Notificações ativadas.',
        icon: 'pi pi-bell',
        route: '/contratos/criar',
        ctaLabel: ['default', 'denied'].includes(notificationStatus) ? 'Ativar notificações...' : 'Gerar primeira ordem de serviço...',
        done: notificationStatus === 'granted',
        metric: '',
        blockers: [['default', 'denied'].includes(notificationStatus) ? 'Ativar as notificações do sistema.' : ''].filter(Boolean),
        disabled: false,
      },
    ];
  }, [fieldActivationDone, notificationStatus, totals]);

  useEffect(() => {
    const firstPending = onboardingSteps.findIndex((step) => !step.done);
    setActiveStepIndex(firstPending >= 0 ? firstPending : 0);
  }, [onboardingSteps]);

  const totalSteps = onboardingSteps.length;
  const completedSteps = onboardingSteps.filter((step) => step.done).length;
  const pendingSteps = onboardingSteps.filter((step) => !step.done);
  const activeStep = onboardingSteps[activeStepIndex];
  const isComplete = pendingSteps.length === 0;
  const hasPreviousStep = activeStepIndex > 0;
  const hasNextStep = activeStepIndex < totalSteps - 1;
  const completionPercent = totalSteps === 0 ? 0 : Math.round((completedSteps / totalSteps) * 100);
  const loading = loadingReferenceItems || loadingContracts || loadingUsers || loadingTeams || loadingStockists || loadingDeposits || loadingMaterials;

  const heroTitle = isComplete ? 'Tudo pronto para começar' : 'Configure o sistema e comece a usar';
  const heroSubtitle = isComplete
    ? 'Tudo pronto! Agora é só colocar sua equipe em campo e começar a usar o sistema.'
    : 'Vamos finalizar sua configuração inicial. Comece pelo próximo passo.';

  const embeddedViewActive = Boolean(outlet || embeddedExternalUrl);

  useEffect(() => {
    if (isComplete) {
      localStorage.setItem('onboarding', 'finished');
      setOnboarding(false);
      return;
    }

    localStorage.removeItem('onboarding');
    setOnboarding(true);
  }, [isComplete, setOnboarding]);

  const handleNavigateStep = async () => {
    if (!activeStep || (activeStep.disabled && !isComplete)) {
      return;
    }

    if (isComplete) {
      navigate('/');
      return;
    }

    if (activeStep.id === 'notifications' && ['default', 'denied'].includes(notificationStatus)) {
      if (typeof Notification === 'undefined') {
        notify('Este navegador não suporta notificações.', 'warn');
        return;
      }
      try {
        const permission = await requestPermission(user?.roles ?? []);
        if (permission === 'granted') {
          notify('Notificações ativadas com sucesso.', 'success');
        } else {
          notify('Permissão de notificações não concedida.', 'warn');
        }
      } catch {
        notify('Não foi possível solicitar permissão de notificações.', 'error');
      }
      return;
    }

    if (activeStep.externalUrl) {
      setEmbeddedExternalUrl(activeStep.route);
      localStorage.setItem('isFinishedTeamInstruction', 'true');
      setFieldActivationDone(true);
      navigate('/configuracoes/onboarding');
      return;
    }

    const embeddedChildPath = EMBEDDED_ROUTE_MAP[activeStep.route];
    if (embeddedChildPath) {
      navigate(`/configuracoes/onboarding/${embeddedChildPath}`);
      return;
    }

    navigate(activeStep.route);
  };

  const closeEmbeddedView = () => {
    setEmbeddedExternalUrl(null);
    navigate('/configuracoes/onboarding');
  };

  return (
    <>
      <div className="min-h-full px-3 py-3 text-slate-900 dark:text-slate-100 sm:px-4 sm:py-4 lg:px-8">
        <div className="mx-auto flex w-full max-w-7xl flex-col gap-4 sm:gap-6">
          <section className="rounded-3xl border border-slate-200 bg-white p-4 shadow-sm dark:border-slate-800 dark:bg-slate-900 sm:p-5">
            <div className="flex flex-col gap-3 md:flex-row md:items-end md:justify-between">
              <div className="min-w-0 max-w-2xl">
                <p className="text-[11px] font-semibold uppercase tracking-[0.22em] text-cyan-700 dark:text-cyan-300 sm:text-xs">
                  Configuração inicial
                </p>
                <h1 className="mt-2 text-xl font-semibold tracking-tight sm:text-2xl lg:text-3xl">
                  {heroTitle}
                </h1>
                <p className="mt-2 text-sm leading-5 text-slate-600 dark:text-slate-300 sm:leading-6">
                  {heroSubtitle}
                </p>
              </div>
              <div className="w-full rounded-2xl bg-slate-50 px-4 py-3 text-sm font-medium text-slate-600 dark:bg-slate-800 dark:text-slate-200 md:w-auto">
                {completedSteps} de {totalSteps} etapas concluídas
              </div>
            </div>
            <div className="mt-4 h-2.5 overflow-hidden rounded-full bg-slate-200 dark:bg-slate-800 sm:mt-5 sm:h-3">
              <div className="h-full rounded-full bg-gradient-to-r from-blue-600 to-cyan-500 transition-all" style={{ width: `${completionPercent}%` }} />
            </div>
          </section>

          {loading ? (
            <div className="grid gap-4 lg:grid-cols-[300px_minmax(0,1fr)] xl:grid-cols-[440px_minmax(0,1fr)] lg:gap-5">
              <div className="order-2 rounded-3xl border border-slate-200 bg-white p-4 shadow-sm dark:border-slate-800 dark:bg-slate-900 lg:order-1">
                <div className="space-y-3">
                  {Array.from({ length: 6 }).map((_, index) => (
                    <div key={index} className="h-20 animate-pulse rounded-2xl bg-slate-100 dark:bg-slate-800" />
                  ))}
                </div>
              </div>
              <div className="order-1 rounded-3xl border border-slate-200 bg-white p-5 shadow-sm dark:border-slate-800 dark:bg-slate-900 lg:order-2">
                <div className="space-y-4">
                  <div className="h-5 w-1/3 animate-pulse rounded bg-slate-100 dark:bg-slate-800" />
                  <div className="h-8 w-2/3 animate-pulse rounded bg-slate-100 dark:bg-slate-800" />
                  <div className="h-24 animate-pulse rounded-2xl bg-slate-100 dark:bg-slate-800" />
                  <div className="h-24 animate-pulse rounded-2xl bg-slate-100 dark:bg-slate-800" />
                </div>
              </div>
            </div>
          ) : (
            <div className="grid gap-4 lg:grid-cols-[300px_minmax(0,1fr)] xl:grid-cols-[440px_minmax(0,1fr)] lg:gap-5">
              <aside className="order-2 rounded-3xl border border-slate-200 bg-white p-3 shadow-sm dark:border-slate-800 dark:bg-slate-900 sm:p-4 lg:order-1">
                <div className="mb-3 sm:mb-4">
                  <h2 className="text-sm font-semibold sm:text-base">Etapas</h2>
                  <p className="mt-1 text-xs text-slate-500 dark:text-slate-400 sm:text-sm">Acompanhe o onboarding em ordem.</p>
                </div>
                <div className="flex flex-col gap-2">
                  {onboardingSteps.map((step, index) => {
                    const isActive = index === activeStepIndex;
                    const isDone = step.done;
                    return (
                      <button
                        key={step.id}
                        type="button"
                        onClick={() => setActiveStepIndex(index)}
                        className={[
                          'w-full rounded-2xl border p-3 text-left transition',
                          isActive
                            ? 'border-cyan-300 bg-cyan-50/95 dark:border-cyan-600/60 dark:bg-cyan-900/30'
                            : isDone
                              ? 'border-transparent bg-emerald-50/70 dark:bg-emerald-900/20'
                              : 'border-transparent hover:bg-slate-50 dark:hover:bg-slate-800',
                        ].join(' ')}
                      >
                        <div className="flex items-start gap-3">
                          <div className={[
                            'mt-0.5 inline-flex h-8 min-w-8 items-center justify-center rounded-full text-xs font-bold',
                            isActive
                              ? 'bg-cyan-600 text-white'
                              : isDone
                                ? 'bg-emerald-600 text-white'
                                : 'bg-slate-100 text-slate-600 dark:bg-slate-800 dark:text-slate-200',
                          ].join(' ')}>
                            {isDone ? <i className="pi pi-check text-[10px]" /> : index + 1}
                          </div>
                          <div className="min-w-0 flex-1">
                            <p className="line-clamp-2 text-sm font-medium leading-5">{step.title}</p>
                            <p className="mt-1 text-xs leading-5 text-slate-500 dark:text-slate-400">{step.metric}</p>
                          </div>
                        </div>
                      </button>
                    );
                  })}
                </div>
              </aside>

              <section className="order-1 rounded-3xl border border-slate-200 bg-white p-4 shadow-sm dark:border-slate-800 dark:bg-slate-900 sm:p-5 lg:order-2">
                {activeStep && (
                  <div className="flex flex-col gap-5 sm:gap-6">
                    <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
                      <div className="min-w-0">
                        <p className="text-[11px] font-semibold uppercase tracking-[0.18em] text-slate-500 dark:text-slate-400 sm:text-xs">
                          Etapa {activeStepIndex + 1} de {totalSteps}
                        </p>
                        <h2 className="mt-2 text-xl font-semibold tracking-tight sm:text-2xl">{activeStep.title}</h2>
                        <p className="mt-3 text-sm leading-6 text-slate-600 dark:text-slate-300">{activeStep.description}</p>
                      </div>
                      <div className="flex h-11 w-11 shrink-0 items-center justify-center rounded-2xl bg-slate-900 text-white dark:bg-slate-100 dark:text-slate-900 sm:h-12 sm:w-12">
                        <i className={`${activeStep.icon} text-base sm:text-lg`} />
                      </div>
                    </div>

                    <div className="grid gap-3 sm:gap-4 md:grid-cols-2">
                      <div className="rounded-2xl border border-slate-200 bg-slate-50 p-4 dark:border-slate-800 dark:bg-slate-950/60">
                        <p className="text-[11px] font-semibold uppercase tracking-[0.16em] text-slate-500 dark:text-slate-400 sm:text-xs">Status atual</p>
                        <p className="mt-2 text-base font-semibold sm:text-lg">{activeStep.metric || 'Em andamento'}</p>
                        <p className="mt-2 text-sm leading-6 text-slate-600 dark:text-slate-300">{activeStep.helper}</p>
                      </div>
                      <div className="rounded-2xl border border-slate-200 bg-slate-50 p-4 dark:border-slate-800 dark:bg-slate-950/60">
                        <p className="text-[11px] font-semibold uppercase tracking-[0.16em] text-slate-500 dark:text-slate-400 sm:text-xs">Para avançar, você precisa:</p>
                        {activeStep.blockers.length > 0 ? (
                          <div className="mt-3 flex flex-col gap-2">
                            {activeStep.blockers.map((blocker) => (
                              <div key={blocker} className="flex items-start gap-2 rounded-xl border border-amber-200 bg-amber-50 px-3 py-2 text-sm leading-5 text-amber-800 dark:border-amber-900/60 dark:bg-amber-950/30 dark:text-amber-200">
                                <i className="pi pi-exclamation-circle mt-0.5 text-xs" />
                                <span>{blocker}</span>
                              </div>
                            ))}
                          </div>
                        ) : (
                          <div className="mt-3 flex items-center gap-2 rounded-xl border border-emerald-200 bg-emerald-50 px-3 py-2 text-sm leading-5 text-emerald-800 dark:border-emerald-900/60 dark:bg-emerald-950/30 dark:text-emerald-200">
                            <i className="pi pi-check-circle text-sm" />
                            <span className="font-medium">Etapa concluída</span>
                          </div>
                        )}
                      </div>
                    </div>

                    <div className="rounded-2xl border border-cyan-100 bg-cyan-50 p-4 dark:border-cyan-900/60 dark:bg-cyan-950/20">
                      <p className="text-sm font-medium text-cyan-900 dark:text-cyan-200">Vamos continuar?</p>
                      <p className="mt-1 text-sm leading-6 text-cyan-800 dark:text-cyan-300">
                        {activeStep.done
                          ? 'Se quiser, você pode revisar esta configuração ou seguir para a próxima etapa.'
                          : 'Clique abaixo, complete o cadastro e siga para a próxima etapa.'}
                      </p>
                    </div>

                    <div className="flex flex-col gap-3 border-t border-slate-200 pt-4 dark:border-slate-800 sm:pt-5 xl:flex-row xl:items-center xl:justify-between">
                      <button
                        type="button"
                        disabled={!isComplete && activeStep.disabled}
                        onClick={() => void handleNavigateStep()}
                        title={!isComplete && activeStep.disabled ? activeStep.blockers[0] : ''}
                        className={[
                          'rounded-lg px-4 py-2 text-xs text-white transition',
                          !isComplete && activeStep.disabled ? 'cursor-not-allowed bg-blue-300' : 'bg-blue-600 hover:bg-blue-700',
                        ].join(' ')}
                      >
                        {isComplete ? 'Ir para o dashboard' : activeStep.ctaLabel}
                      </button>

                      <div className="grid grid-cols-2 gap-2 sm:flex sm:justify-between">
                        <button
                          type="button"
                          onClick={() => hasPreviousStep && setActiveStepIndex((previous) => previous - 1)}
                          disabled={!hasPreviousStep}
                          className="rounded-lg px-3 py-2 text-sm text-slate-600 transition hover:bg-slate-100 disabled:opacity-40 dark:text-slate-300 dark:hover:bg-slate-800"
                        >
                          <i className="pi pi-arrow-left mr-1 text-xs" />
                          Anterior
                        </button>
                        <button
                          type="button"
                          onClick={() => hasNextStep && setActiveStepIndex((previous) => previous + 1)}
                          disabled={!hasNextStep}
                          className="rounded-lg px-3 py-2 text-sm text-slate-600 transition hover:bg-slate-100 disabled:opacity-40 dark:text-slate-300 dark:hover:bg-slate-800"
                        >
                          Próxima
                          <i className="pi pi-arrow-right ml-1 text-xs" />
                        </button>
                      </div>
                    </div>
                  </div>
                )}
              </section>
            </div>
          )}
        </div>
      </div>

      {embeddedViewActive && (
        <>
          <div className="fixed inset-0 z-40 bg-slate-950/35 backdrop-blur-[2px]" onClick={closeEmbeddedView} />
          <section className="fixed inset-y-0 right-0 z-50 flex h-full w-full max-w-[96vw] flex-col bg-white shadow-2xl dark:bg-slate-900 lg:w-[min(1200px,92vw)]">
            <div className="flex flex-col gap-3 border-b border-slate-200 px-4 py-4 dark:border-slate-800 sm:flex-row sm:items-center sm:justify-between sm:px-5">
              <div>
                <p className="text-xs font-semibold uppercase tracking-[0.18em] text-slate-500 dark:text-slate-400">Tela incorporada</p>
                <h3 className="mt-1 text-lg font-semibold text-slate-900 dark:text-slate-100">{activeStep?.title}</h3>
              </div>
              <button
                type="button"
                onClick={closeEmbeddedView}
                className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm text-slate-700 hover:bg-slate-100 dark:border-slate-700 dark:text-slate-200 dark:hover:bg-slate-800 sm:w-auto"
              >
                <i className="pi pi-times mr-1 text-xs" />
                Fechar
              </button>
            </div>
            <div className="flex-1 overflow-y-auto p-4 sm:p-5">
              {embeddedExternalUrl ? (
                <iframe
                  src={embeddedExternalUrl}
                  className="min-h-[70vh] w-full rounded-2xl border border-slate-200 bg-white dark:border-slate-800 dark:bg-slate-950"
                  referrerPolicy="strict-origin-when-cross-origin"
                  title="Conteúdo externo incorporado"
                />
              ) : (
                <Outlet />
              )}
            </div>
          </section>
        </>
      )}
    </>
  );
}
