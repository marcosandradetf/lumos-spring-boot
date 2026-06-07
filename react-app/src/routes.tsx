import { lazy, Suspense } from 'react';
import { createBrowserRouter, Navigate } from 'react-router-dom';
import { ProtectedRoute } from './core/auth/ProtectedRoute';
import { AppLayout } from './shared/layout/app-layout';

const Login = lazy(() => import('./features/auth/pages/Login'));
const AutoLogin = lazy(() => import('./features/auth/pages/AutoLogin'));
const FirstAccess = lazy(() => import('./features/auth/pages/FirstAccess'));
const AccessDenied = lazy(() => import('./features/auth/pages/AccessDenied'));
const StatusPage = lazy(() => import('./features/status/pages/status-page'));
const Dashboard = lazy(() => import('./features/dashboard/home/Dashboard'));
const ExecutionsMapDashboard = lazy(() => import('./features/dashboard/pages/ExecutionsMapDashboard'));
const ExecutiveDashboard = lazy(() => import('./features/dashboard/pages/ExecutiveDashboard'));
const TeamProductivityDashboard = lazy(() => import('./features/dashboard/pages/TeamProductivityDashboard'));
const MaterialForm = lazy(() => import('./features/stock/pages/material-form'));
const MaterialCatalog = lazy(() => import('./features/stock/pages/material-catalogue'));
const Deposits = lazy(() => import('./features/stock/pages/deposits'));
const ImportMaterials = lazy(() => import('./features/stock/pages/import-materials'));
const Groups = lazy(() => import('./features/stock/pages/groups'));
const Types = lazy(() => import('./features/stock/pages/types'));
const Stockists = lazy(() => import('./features/stock/pages/stockists'));
const StockMovement = lazy(() => import('./features/stock/pages/stock-movement'));
const StockMovementPending = lazy(() => import('./features/stock/pages/stock-movement-pending'));
const StockMovementApproved = lazy(() => import('./features/stock/pages/stock-movement-approved'));
const ServiceRequestMap = lazy(() => import('./features/maintenance/request/ServiceRequestMap'));
const ReservationPending = lazy(() => import('./features/requests/pages/reservation-pending'));
const ReservationManagement = lazy(() => import('./features/requests/pages/os-management'));
const ReservationManagementSelect = lazy(() => import('./features/requests/pages/os-select'));
// Contratos
const ContractList = lazy(() => import('./features/contract/pages/ContractList'));
const ContractCreate = lazy(() => import('./features/contract/pages/ContractCreate'));
const ContractReferenceItems = lazy(() => import('./features/contract/pages/ContractReferenceItems'));
const ContractReferenceLinks = lazy(() => import('./features/contract/pages/ContractReferenceLinks'));
const PendingExecutions = lazy(() => import('./features/contract/pages/PendingExecutions'));
const ValidateExecution = lazy(() => import('./features/contract/pages/ValidateExecution'));
// Execuções
const ExecutionProgress = lazy(() => import('./features/executions/pages/ExecutionProgress'));
const ExecutionNoPreMeasurement = lazy(() => import('./features/executions/pages/ExecutionNoPreMeasurement'));
const MeasurementDetails = lazy(() => import('./features/executions/pages/MeasurementDetails'));
// Pre-measurement
const PreMeasurementList = lazy(() => import('./features/pre-measurement/pages/PreMeasurementList'));
const PreMeasurementView = lazy(() => import('./features/pre-measurement/pages/PreMeasurementView'));
const ImportPreMeasurements = lazy(() => import('./features/pre-measurement/pages/ImportPreMeasurements'));
// Manage
const Users = lazy(() => import('./features/manage/pages/Users'));
const Teams = lazy(() => import('./features/manage/pages/Teams'));
const Account = lazy(() => import('./features/manage/pages/Account'));
const Settings = lazy(() => import('./features/manage/pages/Settings'));
// Relatórios
const MaintenanceReport = lazy(() => import('./features/reports/pages/MaintenanceReport'));
const InstallationReport = lazy(() => import('./features/reports/pages/InstallationReport'));
const OperationReport = lazy(() => import('./features/reports/pages/OperationReport'));
const ReportManagement = lazy(() => import('./features/reports/pages/ReportManagement'));
const MaterialReservationReport = lazy(() => import('./features/reports/pages/MaterialReservationReport'));
// Billing / download
const Billing = lazy(() => import('./features/billing/pages/Billing'));
const UnavailableAccess = lazy(() => import('./features/billing/pages/UnavailableAccess'));
const AppDownload = lazy(() => import('./features/app-download/pages/AppDownload'));

const Loading = () => (
  <div className="flex h-full items-center justify-center">
    <i className="pi pi-spin pi-spinner text-xl text-indigo-500" />
  </div>
);

export const router = createBrowserRouter([
  // --- Auth routes (no layout) ---
  {
    path: '/auth/login',
    element: <Suspense fallback={null}><Login /></Suspense>,
  },
  {
    path: '/auth/auto-login',
    element: <Suspense fallback={null}><AutoLogin /></Suspense>,
  },
  {
    path: '/primeiro-acesso',
    element: <Suspense fallback={null}><FirstAccess /></Suspense>,
  },
  {
    path: '/app/download',
    element: <Suspense fallback={null}><AppDownload /></Suspense>,
  },

  // --- App routes (AppLayout handles auth redirect) ---
  {
    element: <AppLayout />,
    children: [
      {
        path: '/',
        element: <Suspense fallback={<Loading />}><Dashboard /></Suspense>,
      },
      {
        path: '/dashboard',
        element: <Suspense fallback={<Loading />}><Dashboard /></Suspense>,
      },
      {
        path: '/contratos',
        element: <Navigate to="/contratos/dashboard" replace />,
      },
      {
        path: '/estoque',
        element: <Navigate to="/estoque/catalogo-materiais" replace />,
      },
      {
        path: '/acesso-negado/:section',
        element: <Suspense fallback={<Loading />}><AccessDenied /></Suspense>,
      },
      {
        path: '/cobranca',
        element: (
          <ProtectedRoute roles={['ADMIN']}>
            <Suspense fallback={<Loading />}><Billing /></Suspense>
          </ProtectedRoute>
        ),
      },
      {
        path: '/acesso-indisponivel',
        element: <Suspense fallback={<Loading />}><UnavailableAccess /></Suspense>,
      },

      // Estoque
      {
        path: '/estoque/cadastrar-material',
        element: (
          <ProtectedRoute roles={['ADMIN', 'ESTOQUISTA', 'ESTOQUISTA_CHEFE']}>
            <Suspense fallback={<Loading />}><MaterialForm /></Suspense>
          </ProtectedRoute>
        ),
      },
      {
        path: '/estoque/editar-material',
        element: (
          <ProtectedRoute roles={['ADMIN', 'ESTOQUISTA', 'ESTOQUISTA_CHEFE']}>
            <Suspense fallback={<Loading />}><MaterialForm /></Suspense>
          </ProtectedRoute>
        ),
      },
      {
        path: '/estoque/catalogo-materiais',
        element: (
          <ProtectedRoute roles={['ADMIN', 'ESTOQUISTA']}>
            <Suspense fallback={<Loading />}><MaterialCatalog /></Suspense>
          </ProtectedRoute>
        ),
      },
      {
        path: '/estoque/almoxarifados',
        element: (
          <ProtectedRoute roles={['ADMIN']}>
            <Suspense fallback={<Loading />}><Deposits mode="fixed" /></Suspense>
          </ProtectedRoute>
        ),
      },
      {
        path: '/estoque/caminhoes',
        element: (
          <ProtectedRoute roles={['ADMIN']}>
            <Suspense fallback={<Loading />}><Deposits mode="truck" /></Suspense>
          </ProtectedRoute>
        ),
      },
      {
        path: '/estoque/grupos',
        element: (
          <ProtectedRoute roles={['ADMIN']}>
            <Suspense fallback={<Loading />}><Groups /></Suspense>
          </ProtectedRoute>
        ),
      },
      {
        path: '/estoque/tipos',
        element: (
          <ProtectedRoute roles={['ADMIN']}>
            <Suspense fallback={<Loading />}><Types /></Suspense>
          </ProtectedRoute>
        ),
      },
      {
        path: '/estoque/movimentar-estoque',
        element: (
          <ProtectedRoute roles={['ADMIN', 'ESTOQUISTA', 'ESTOQUISTA_CHEFE']}>
            <Suspense fallback={<Loading />}><StockMovement /></Suspense>
          </ProtectedRoute>
        ),
      },
      {
        path: '/estoque/movimentar-estoque-pendente',
        element: (
          <ProtectedRoute roles={['ADMIN', 'ESTOQUISTA', 'ESTOQUISTA_CHEFE']}>
            <Suspense fallback={<Loading />}><StockMovementPending /></Suspense>
          </ProtectedRoute>
        ),
      },
      {
        path: '/estoque/movimentar-estoque-aprovado',
        element: (
          <ProtectedRoute roles={['ADMIN', 'ESTOQUISTA', 'ESTOQUISTA_CHEFE']}>
            <Suspense fallback={<Loading />}><StockMovementApproved /></Suspense>
          </ProtectedRoute>
        ),
      },
      {
        path: '/configuracoes/estoquistas',
        element: (
          <ProtectedRoute roles={['ADMIN']}>
            <Suspense fallback={<Loading />}><Stockists /></Suspense>
          </ProtectedRoute>
        ),
      },
      {
        path: '/configuracoes/importar-planilha',
        element: (
          <ProtectedRoute roles={['ADMIN', 'ESTOQUISTA', 'ESTOQUISTA_CHEFE']}>
            <Suspense fallback={<Loading />}><ImportMaterials /></Suspense>
          </ProtectedRoute>
        ),
      },

      // Contratos
      {
        path: '/contratos/listar',
        element: <Suspense fallback={<Loading />}><ContractList /></Suspense>,
      },
      {
        path: '/contratos/criar',
        element: (
          <ProtectedRoute roles={['ADMIN', 'ANALISTA']}>
            <Suspense fallback={<Loading />}><ContractCreate /></Suspense>
          </ProtectedRoute>
        ),
      },
      {
        path: '/contratos/editar',
        element: (
          <ProtectedRoute roles={['ADMIN', 'ANALISTA']}>
            <Suspense fallback={<Loading />}><ContractCreate /></Suspense>
          </ProtectedRoute>
        ),
      },
      {
        path: '/contratos/instalacoes-pendentes',
        element: (
          <ProtectedRoute roles={['ADMIN', 'ANALISTA']}>
            <Suspense fallback={<Loading />}><PendingExecutions /></Suspense>
          </ProtectedRoute>
        ),
      },
      {
        path: '/contratos/validar-execucao/:id',
        element: (
          <ProtectedRoute roles={['ADMIN', 'ANALISTA', 'RESPONSAVEL_TECNICO']}>
            <Suspense fallback={<Loading />}><ValidateExecution /></Suspense>
          </ProtectedRoute>
        ),
      },
      {
        path: '/contratos/itens-contratuais/cadastro',
        element: (
          <ProtectedRoute roles={['ADMIN', 'ANALISTA']}>
            <Suspense fallback={<Loading />}><ContractReferenceItems /></Suspense>
          </ProtectedRoute>
        ),
      },
      {
        path: '/contratos/itens-contratuais/vinculos',
        element: (
          <ProtectedRoute roles={['ADMIN', 'ANALISTA']}>
            <Suspense fallback={<Loading />}><ContractReferenceLinks /></Suspense>
          </ProtectedRoute>
        ),
      },
      {
        path: '/contratos/dashboard',
        element: (
          <ProtectedRoute roles={['ADMIN', 'ANALISTA']}>
            <Suspense fallback={<Loading />}><Dashboard contractMode /></Suspense>
          </ProtectedRoute>
        ),
      },

      // Pré-medição
      {
        path: '/pre-medicao/:status',
        element: <Suspense fallback={<Loading />}><PreMeasurementList /></Suspense>,
      },
      {
        path: '/pre-medicao/:status/:id',
        element: <Suspense fallback={<Loading />}><PreMeasurementView /></Suspense>,
      },
      {
        path: '/pre-medicao/importar/contrato/:id',
        element: <Suspense fallback={<Loading />}><ImportPreMeasurements /></Suspense>,
      },
      {
        path: '/pre_medicao/visualizar',
        element: <Navigate to="/pre-medicao/pendente" replace />,
      },
      {
        path: '/pre_medicao/editar',
        element: <Navigate to="/pre-medicao/pendente" replace />,
      },
      {
        path: '/pre-medicao/relatorio/:id/:step',
        element: <Navigate to="/pre-medicao/pendente" replace />,
      },

      // Configurações
      {
        path: '/configuracoes/usuarios',
        element: (
          <ProtectedRoute roles={['ADMIN']}>
            <Suspense fallback={<Loading />}><Users /></Suspense>
          </ProtectedRoute>
        ),
      },
      {
        path: '/configuracoes/equipes',
        element: (
          <ProtectedRoute roles={['ADMIN']}>
            <Suspense fallback={<Loading />}><Teams /></Suspense>
          </ProtectedRoute>
        ),
      },
      {
        path: '/configuracoes/conta',
        element: <Suspense fallback={<Loading />}><Account /></Suspense>,
      },
      {
        path: '/configuracoes/empresa',
        element: (
          <ProtectedRoute roles={['ADMIN']}>
            <Suspense fallback={<Loading />}><Teams /></Suspense>
          </ProtectedRoute>
        ),
      },
      {
        path: '/configuracoes/onboarding',
        element: (
          <ProtectedRoute roles={['ADMIN']}>
            <Suspense fallback={<Loading />}><Settings /></Suspense>
          </ProtectedRoute>
        ),
        children: [
          { path: 'usuarios', element: <Suspense fallback={<Loading />}><Users /></Suspense> },
          { path: 'equipes', element: <Suspense fallback={<Loading />}><Teams /></Suspense> },
          { path: 'estoquistas', element: <Suspense fallback={<Loading />}><Stockists /></Suspense> },
          { path: 'almoxarifados', element: <Suspense fallback={<Loading />}><Deposits mode="fixed" /></Suspense> },
          { path: 'caminhoes', element: <Suspense fallback={<Loading />}><Deposits mode="truck" /></Suspense> },
          { path: 'cadastrar-material', element: <Suspense fallback={<Loading />}><MaterialForm /></Suspense> },
          { path: 'itens-contratuais', element: <Suspense fallback={<Loading />}><ContractReferenceItems /></Suspense> },
          { path: 'criar-contrato', element: <Suspense fallback={<Loading />}><ContractCreate /></Suspense> },
        ],
      },

      // Execuções (status dinâmico via :status param)
      {
        path: '/ordens-de-servico/nova',
        element: (
          <ProtectedRoute roles={['ADMIN', 'ANALISTA', 'RESPONSAVEL_TECNICO']}>
            <Suspense fallback={<Loading />}><ExecutionNoPreMeasurement /></Suspense>
          </ProtectedRoute>
        ),
      },
      {
        path: '/execucao/pre-medicao/:id',
        element: (
          <ProtectedRoute roles={['ADMIN', 'ANALISTA', 'RESPONSAVEL_TECNICO']}>
            <Suspense fallback={<Loading />}><MeasurementDetails /></Suspense>
          </ProtectedRoute>
        ),
      },
      {
        path: '/execucoes/:status',
        element: <Suspense fallback={<Loading />}><ExecutionProgress /></Suspense>,
      },

      // Relatórios
      {
        path: '/relatorios/manutencoes',
        element: <Suspense fallback={<Loading />}><MaintenanceReport /></Suspense>,
      },
      {
        path: '/relatorios/instalacoes',
        element: <Suspense fallback={<Loading />}><InstallationReport /></Suspense>,
      },
      {
        path: '/relatorios/execucoes/analitico-de-operacoes',
        element: <Suspense fallback={<Loading />}><OperationReport /></Suspense>,
      },
      {
        path: '/relatorios/gerenciamento',
        element: <Suspense fallback={<Loading />}><ReportManagement /></Suspense>,
      },
      {
        path: '/relatorios/estoque/saida-saldo-instalacao',
        element: <Suspense fallback={<Loading />}><MaterialReservationReport /></Suspense>,
      },

      // Dashboards avançados
      {
        path: '/dashboard/mapa-execucoes',
        element: <Suspense fallback={<Loading />}><ExecutionsMapDashboard /></Suspense>,
      },
      {
        path: '/dashboard/visao-executiva',
        element: <Suspense fallback={<Loading />}><ExecutiveDashboard /></Suspense>,
      },
      {
        path: '/dashboard/produtividade-equipe',
        element: <Suspense fallback={<Loading />}><TeamProductivityDashboard /></Suspense>,
      },

      // Modo Ronda & Chamado
      {
        path: '/abrir-chamado',
        element: <Suspense fallback={<Loading />}><ServiceRequestMap mode="manual" /></Suspense>,
      },
      {
        path: '/modo-ronda',
        element: <Suspense fallback={<Loading />}><ServiceRequestMap mode="round" /></Suspense>,
      },

      // Requisições ao estoquista
      {
        path: '/requisicoes',
        element: (
          <ProtectedRoute roles={['ADMIN', 'ESTOQUISTA', 'ESTOQUISTA_CHEFE']}>
            <Suspense fallback={<Loading />}><ReservationPending /></Suspense>
          </ProtectedRoute>
        ),
      },
      {
        path: '/requisicoes/instalacoes/gerenciamento-estoque',
        element: (
          <ProtectedRoute roles={['ADMIN', 'ESTOQUISTA', 'ESTOQUISTA_CHEFE']}>
            <Suspense fallback={<Loading />}><ReservationManagement /></Suspense>
          </ProtectedRoute>
        ),
      },
      {
        path: '/requisicoes/gerenciamento/execucao',
        element: (
          <ProtectedRoute roles={['ADMIN', 'ESTOQUISTA', 'ESTOQUISTA_CHEFE']}>
            <Suspense fallback={<Loading />}><ReservationManagementSelect /></Suspense>
          </ProtectedRoute>
        ),
      },
    ],
  },

  // --- Status & fallback ---
  { path: '/status/:type', element: <Suspense fallback={<Loading />}><StatusPage /></Suspense> },
  { path: '*', element: <Navigate to="/status/404" replace /> },
]);
