// app/app.routes.ts
import {Routes} from '@angular/router';
import {AuthGuard} from './core/auth/auth.guard';


export const routes: Routes = [
  // start path login
  {
    path: 'auth/login',
    loadComponent: () => import('./core/auth/pages/login/login.component').then(m => m.LoginComponent)
  },
  // end

  {
    path: 'acesso-negado/:section',
    loadComponent: () => import('./shared/components/no-access/no-access.component').then(n => n.NoAccessComponent),
    canActivate: [AuthGuard],
    data: {role: [''], path: ''},
  },

  // start paths stock
  {
    path: 'estoque/materiais',
    loadComponent: () => import('./stock/material/material-page.component').then(m => m.MaterialPageComponent),
    canActivate: [AuthGuard],
    data: {role: ['ADMIN', 'ESTOQUISTA', 'ESTOQUISTA_CHEFE', 'ANALISTA', 'RESPOSAVEL_TECNICO'], path: 'estoque'},
  },
  {
    path: 'estoque/grupos',
    loadComponent: () => import('./stock/groups/groups.component').then(m => m.GroupsComponent),
    canActivate: [AuthGuard],
    data: {role: ['ADMIN', 'ESTOQUISTA', 'ESTOQUISTA_CHEFE'], path: 'estoque'},
  },
  {
    path: 'estoque/tipos',
    loadComponent: () => import('./stock/types/types.component').then(m => m.TypesComponent),
    canActivate: [AuthGuard],
    data: {role: ['ADMIN'], path: 'estoque'},
  },
  {
    path: 'estoque/movimento',
    loadComponent: () => import('./stock/stock-movement/stock-movement.component').then(m => m.StockMovementComponent),
    canActivate: [AuthGuard],
    data: {role: ['ADMIN', 'ESTOQUISTA', 'ESTOQUISTA_CHEFE'], path: 'estoque'},
  },
  {
    path: 'estoque/movimento-pendente',
    loadComponent: () => import('./stock/stock-movement-pending/stock-movement-pending.component').then(m => m.StockMovementPendingComponent),
    canActivate: [AuthGuard],
    data: {role: ['ADMIN', 'ESTOQUISTA_CHEFE'], path: 'estoque'},
  },
  {
    path: 'estoque/movimento/aprovado',
    loadComponent: () => import('./stock/stock-movement-approvated/stock-movement-approvated.component').then(m => m.StockMovementApprovatedComponent),
    canActivate: [AuthGuard],
    data: {role: ['ADMIN', 'ESTOQUISTA', 'ESTOQUISTA_CHEFE'], path: 'estoque'},
  },
  {
    path: 'estoque/almoxarifados',
    loadComponent: () => import('./stock/deposits/deposits.component').then(m => m.DepositsComponent),
    canActivate: [AuthGuard],
    data: {role: ['ADMIN', 'ANALISTA', 'ESTOQUISTA', 'ESTOQUISTA_CHEFE'], path: 'estoque'},
  },
  {
    path: 'configuracoes/importar-planilha',
    loadComponent: () => import('./stock/import-materials/import-materials.component').then(m => m.ImportMaterialsComponent),
    canActivate: [AuthGuard],
    data: {role: ['ADMIN', 'ESTOQUISTA', 'ESTOQUISTA_CHEFE'], path: 'estoque'},
  },


  {path: 'estoque', redirectTo: 'estoque/materiais'},
  // end

  // start contract paths
  {
    path: 'contratos/dashboard',
    loadComponent: () => import('./contract/pages/dashboard/dashboard.component').then(m => m.DashboardComponent),
    canActivate: [AuthGuard],
    data: {role: ['ADMIN', 'ANALISTA'], path: 'contratos'},
  },
  {
    path: 'contratos/criar',
    loadComponent: () => import('./contract/pages/create/create.component').then(m => m.CreateComponent),
    canActivate: [AuthGuard],
    data: {role: ['ADMIN', 'ANALISTA'], path: 'contratos'},
  },
  {
    path: 'contratos/listar',
    loadComponent: () => import('./contract/pages/list/contract-list/contract-list.component').then(c => c.ContractListComponent),
    canActivate: [AuthGuard],
    data: {role: ['ADMIN', 'ANALISTA'], path: 'contratos'},
  },
  // end

  // start settings path
  {
    path: 'configuracoes/dashboard',
    loadComponent: () => import('./manage/settings/settings.component').then(s => s.SettingsComponent),
    canActivate: [AuthGuard],
    data: {role: ['ADMIN', 'ANALISTA'], path: 'configuracoes'},
  },
  {
    path: 'configuracoes/usuarios',
    loadComponent: () => import('./manage/user/user.component').then(m => m.UserComponent),
    canActivate: [AuthGuard],
    data: {role: ['ADMIN', 'ANALISTA'], path: 'configuracoes'},
  },
  {
    path: 'configuracoes/equipes',
    loadComponent: () => import('./manage/team/team.component').then(m => m.TeamComponent),
    canActivate: [AuthGuard],
    data: {role: ['ADMIN', 'ANALISTA'], path: 'configuracoes'},
  },
  {
    path: 'configuracoes/empresa',
    loadComponent: () => import('./manage/team/team.component').then(m => m.TeamComponent),
    canActivate: [AuthGuard],
    data: {role: ['ADMIN', 'ANALISTA'], path: 'configuracoes'},
  },
  {
    path: 'configuracoes/conta',
    loadComponent: () => import('./manage/account/account.component').then(a => a.AccountComponent),
    canActivate: [AuthGuard],
    data: {role: [], path: 'conta'},
  },
  // end

  // start executions/pre-measurements path
  {
    path: 'pre-medicao/:status',
    loadComponent: () => import('./executions/pre-measurement-home/pre-measurement.component').then(p => p.PreMeasurementComponent),
    canActivate: [AuthGuard],
    data: {role: ['ADMIN', 'ANALISTA', 'RESPOSAVEL_TECNICO'], path: 'execucoes'},
  },
  {
    path: 'pre-medicao/relatorio/:id',
    loadComponent: () => import('./executions/pre-measurement-report/pre-measurement-report.component').then(p => p.PreMeasurementReportComponent),
    canActivate: [AuthGuard],
    data: {role: ['ADMIN', 'ANALISTA', 'RESPOSAVEL_TECNICO'], path: 'execucoes'},
  },
  {
    path: 'pre-medicao/editar/:id',
    loadComponent: () => import('./executions/pre-measurement-edit/pre-measurement-edit.component').then(p => p.PreMeasurementEditComponent),
    canActivate: [AuthGuard],
    data: {role: ['ADMIN', 'ANALISTA', 'RESPOSAVEL_TECNICO'], path: 'execucoes'},
  },
  //

  // start executions/executions path
  {
    path: 'execucao/pre-medicao/:id',
    loadComponent: () => import('./executions/pre-measurement-available/measurement-details.component').then(m => m.MeasurementDetailsComponent),
    canActivate: [AuthGuard],
    data: {role: ['ADMIN', 'ANALISTA', 'RESPOSAVEL_TECNICO'], path: 'execucoes'},
  },
  {
    path: 'execucoes/em-progresso',
    loadComponent: () => import('./executions/execution-progress/execution-progress.component').then(e => e.ExecutionProgressComponent),
    canActivate: [AuthGuard],
    data: {role: ['ADMIN', 'ANALISTA', 'RESPOSAVEL_TECNICO'], path: 'execucoes'},
  },

  {
    path: 'execucoes/finalizadas',
    loadComponent: () => import('./executions/execution-finished/execution-finished.component').then(e => e.ExecutionFinishedComponent),
    canActivate: [AuthGuard],
    data: {role: ['ADMIN', 'ANALISTA', 'RESPOSAVEL_TECNICO'], path: 'execucoes'},
  },
  //end

  // start request path
  {
    path: 'requisicoes/itens',
    loadComponent: () => import('./requests/request-items/request-items.component').then(r => r.RequestItemsComponent),
    canActivate: [AuthGuard],
    data: {role: ['ADMIN', 'MOTORISTA', 'ELETRICISTA', 'ESTOQUISTA', 'ESTOQUISTA_CHEFE'], path: 'requisicoes'},
  },
  //end

  {
    path: 'dashboard',
    loadComponent: () => import('./dashboard/dashboard.component').then(d => d.DashboardComponent),
    canActivate: [AuthGuard],
    data: {role: [], path: 'dashboard'},
  },
  {path: '**', redirectTo: 'dashboard'}

];
