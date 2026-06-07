export type NavItem = {
  id: string;
  label: string;
  customLabel?: string;
  searchTerms?: string[];
  icon: string;
  iconColor?: string;
  path?: string;
  queryParams?: Record<string, string>;
  disabled?: boolean;
  badge?: string;
  children?: NavItem[];
};

export type NavSection = {
  id: string;
  label: string;
  icon: string;
  storageKey?: string;
  defaultExpanded?: boolean;
  requiresSupport?: boolean;
  requiresOnboarding?: boolean;
  children: NavItem[];
};

export const NAV_SECTIONS: NavSection[] = [
  {
    id: 'onboarding',
    label: 'Primeiros passos',
    icon: 'pi-play',
    requiresOnboarding: true,
    defaultExpanded: true,
    children: [
      { id: 'onboarding-link', label: 'Primeiros passos', icon: 'pi-play', iconColor: 'text-blue-500', path: '/configuracoes/onboarding' },
    ],
  },
  {
    id: 'saas-admin',
    label: 'Administração SaaS',
    icon: 'pi-sliders-v',
    storageKey: 'toggleSaasAdmin',
    requiresSupport: true,
    defaultExpanded: false,
    children: [
      { id: 'saas-add-client', label: 'Adicionar novo cliente', icon: 'pi-plus', iconColor: 'text-blue-500', disabled: true },
      { id: 'saas-pending-contracts', label: 'Contratos pendentes', icon: 'pi-book', iconColor: 'text-blue-500', path: '/configuracoes/conta' },
      { id: 'saas-billing', label: 'Faturamento de Clientes', icon: 'pi-wallet', iconColor: 'text-blue-500', path: '/cobranca' },
    ],
  },
  {
    id: 'dashboards',
    label: 'Dashboards',
    icon: 'pi-chart-bar',
    defaultExpanded: true,
    children: [
      { id: 'dashboard-map', label: 'Mapa de Execuções', icon: 'pi-map-marker', iconColor: 'text-blue-500', path: '/dashboard/mapa-execucoes' },
      { id: 'dashboard-exec', label: 'Visão Executiva', icon: 'pi-compass', iconColor: 'text-purple-700', path: '/dashboard/visao-executiva' },
      { id: 'dashboard-team', label: 'Produtividade da Equipe', icon: 'pi-users', iconColor: 'text-green-500', path: '/dashboard/produtividade-equipe', queryParams: { status: 'APPROVED' } },
    ],
  },
  {
    id: 'executions',
    label: 'Ordens de Serviço',
    icon: 'pi-briefcase',
    storageKey: 'toggleExecution',
    defaultExpanded: false,
    children: [
      {
        id: 'new-os', label: 'Nova Ordem de Serviço', icon: 'pi-plus-circle', iconColor: 'text-green-500',
        children: [
          {
            id: 'new-os-direct',
            customLabel: 'Nova OS Sem Pré-medição',
            searchTerms: ['criar os', 'nova os', 'sem pre-medicao', 'os', 'ordem de serviço', 'ordens de serviço', 'o.s'],
            label: 'Criar sem pré-medição', icon: 'pi-plus', iconColor: 'text-green-500', path: '/contratos/listar', queryParams: { for: 'execution' }
          },
          {
            id: 'new-os-premedition',
            customLabel: 'Nova OS Com Pré-medição',
            searchTerms: ['criar os', 'nova os', 'com pre-medicao', 'os', 'ordem de serviço', 'ordens de serviço', 'o.s'],
            label: 'Usar pré-medição analisada', icon: 'pi-clipboard', iconColor: 'text-blue-500', path: '/pre-medicao/disponivel'
          },
        ],
      },
      { id: 'premed-analysis', label: 'Pré-medições para Análise', icon: 'pi-exclamation-circle', iconColor: 'text-yellow-500', path: '/pre-medicao/pendente' },
      {
        id: 'stock-analysis', label: 'Em Análise de Estoque',
        searchTerms: ['o.s', 'ordem de serviço', 'ordens de serviço'],
        icon: 'pi-search', iconColor: 'text-yellow-500', path: '/execucoes/analise-estoque'
      },
      {
        id: 'awaiting-collect',
        searchTerms: ['o.s', 'ordem de serviço', 'ordens de serviço'],
        label: 'Aguardando Coleta', icon: 'pi-box', iconColor: 'text-orange-500', path: '/execucoes/aguardando-coleta'
      },
      { id: 'ready-exec', 
        searchTerms: ['o.s', 'ordem de serviço', 'ordens de serviço'],
        label: 'Prontas para Execução', icon: 'pi-play', iconColor: 'text-blue-500', path: '/execucoes/prontas-para-execucao' },
      { id: 'in-exec', 
        searchTerms: ['o.s', 'ordem de serviço', 'ordens de serviço'],
        label: 'Em Execução', icon: 'pi-cog', iconColor: 'text-blue-600', path: '/execucoes/em-execucao' },
      { id: 'done-exec', 
        searchTerms: ['o.s', 'ordem de serviço', 'ordens de serviço'],
        label: 'Concluídas', icon: 'pi-check-circle', iconColor: 'text-green-600', path: '/execucoes/concluidas' },
    ],
  },
  {
    id: 'requests',
    label: 'Solicitações ao Estoquista',
    icon: 'pi-box',
    storageKey: 'toggleRequest',
    defaultExpanded: false,
    children: [
      { id: 'req-pending', label: 'Pendentes de Aprovação', icon: 'pi-clock', iconColor: 'text-yellow-500', path: '/requisicoes', queryParams: { status: 'PENDING' } },
      { id: 'req-available', label: 'Disponíveis para Coleta', icon: 'pi-check-circle', iconColor: 'text-green-500', path: '/requisicoes', queryParams: { status: 'APPROVED' } },
      {
        id: 'req-manage',
        label: 'Gerenciar Ordens de Serviço',
        searchTerms: ['gerenciar os', 'ordem de serviço', 'ordens de serviço', 'gerenciar ordens de serviço', 'requisicoes os', 'o.s'],
        icon: 'pi-briefcase', iconColor: 'text-blue-500', path: '/requisicoes/instalacoes/gerenciamento-estoque'
      },
    ],
  },
  {
    id: 'reports',
    label: 'Relatórios',
    icon: 'pi-chart-line',
    storageKey: 'toggleport',
    defaultExpanded: false,
    children: [
      {
        id: 'reports-exec', label: 'Execuções', icon: 'pi-wrench', iconColor: 'text-orange-400',
        children: [
          { id: 'report-maintenance', label: 'Manutenções', icon: 'pi-wrench', iconColor: 'text-blue-500', path: '/relatorios/manutencoes' },
          { id: 'report-installations', label: 'Instalações', icon: 'pi-lightbulb', iconColor: 'text-blue-500', path: '/relatorios/instalacoes' },
          { id: 'report-analytic', label: 'Analítico de Operações', icon: 'pi-sliders-h', iconColor: 'text-blue-500', path: '/relatorios/execucoes/analitico-de-operacoes' },
          { id: 'report-grouped', label: 'Agrupados', icon: 'pi-box', iconColor: 'text-blue-500', path: '/relatorios/gerenciamento' },
        ],
      },
      {
        id: 'reports-stock', label: 'Estoque', icon: 'pi-box', iconColor: 'text-indigo-500',
        children: [
          { id: 'report-stock-output', label: 'Saída/Saldo por Instalação', icon: 'pi-hammer', iconColor: 'text-orange-400', path: '/relatorios/estoque/saida-saldo-instalacao' },
          { id: 'report-stock-req', label: 'Saída por Requisições', icon: 'pi-briefcase', iconColor: 'text-orange-300', disabled: true },
        ],
      },
    ],
  },
  {
    id: 'contracts',
    label: 'Contratos',
    icon: 'pi-file',
    storageKey: 'toggleContracts',
    defaultExpanded: false,
    children: [
      { id: 'contract-new', label: 'Novo Contrato', icon: 'pi-plus-circle', iconColor: 'text-green-500', path: '/contratos/criar' },
      { id: 'contract-list', label: 'Listar Contratos', icon: 'pi-folder-open', iconColor: 'text-blue-500', path: '/contratos/listar', queryParams: { for: 'view' } },
      { id: 'contract-link-inst', label: 'Vincular Instalações', icon: 'pi-link', iconColor: 'text-emerald-500', path: '/contratos/instalacoes-pendentes' },
      { id: 'contract-catalogue', label: 'Catálogo de Itens Contratuais', icon: 'pi-database', iconColor: 'text-neutral-500', path: '/contratos/itens-contratuais/cadastro' },
      { id: 'contract-link-items', label: 'Vincular Itens Contratuais', icon: 'pi-link', iconColor: 'text-cyan-500', path: '/contratos/itens-contratuais/vinculos', queryParams: { operation: 'item' } },
    ],
  },
  {
    id: 'stock',
    label: 'Estoque',
    icon: 'pi-warehouse',
    storageKey: 'toggleStock',
    defaultExpanded: true,
    children: [
      { id: 'stock-movement', label: 'Movimentar Estoque', icon: 'pi-arrow-right-arrow-left', iconColor: 'text-blue-500', path: '/estoque/movimentar-estoque' },
      { id: 'stock-movement-pending', label: 'Movimentações Pendentes', icon: 'pi-clock', iconColor: 'text-yellow-500', path: '/estoque/movimentar-estoque-pendente' },
      { id: 'stock-material-form', label: 'Cadastro de Materiais', icon: 'pi-plus-circle', iconColor: 'text-green-500', path: '/estoque/cadastrar-material' },
      { id: 'stock-deposits', label: 'Almoxarifados', icon: 'pi-home', iconColor: 'text-blue-500', path: '/estoque/almoxarifados' },
      { id: 'stock-trucks', label: 'Caminhões', icon: 'pi-truck', iconColor: 'text-blue-500', path: '/estoque/caminhoes' },
      { id: 'stock-catalogue', label: 'Catálogo de Materiais', icon: 'pi-table', iconColor: 'text-neutral-500', path: '/estoque/catalogo-materiais' },
    ],
  },
  {
    id: 'settings',
    label: 'Configurações',
    icon: 'pi-cog',
    storageKey: 'toggleSettings',
    defaultExpanded: false,
    children: [
      { id: 'settings-users', label: 'Usuários', icon: 'pi-users', iconColor: 'text-blue-500', path: '/configuracoes/usuarios' },
      { id: 'settings-account', label: 'Minha Conta', icon: 'pi-user', iconColor: 'text-blue-500', path: '/configuracoes/conta' },
      { id: 'settings-teams', label: 'Equipes Operacionais', icon: 'pi-sitemap', iconColor: 'text-blue-500', path: '/configuracoes/equipes' },
      { id: 'settings-stockists', label: 'Estoquistas', icon: 'pi-id-card', iconColor: 'text-blue-500', path: '/configuracoes/estoquistas' },
    ],
  },
];
