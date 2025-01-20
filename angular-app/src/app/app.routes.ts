// app/app.routes.ts
import { Routes } from '@angular/router';
import { AuthGuard } from './core/auth/auth.guard';


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
    data: { role: [''], path: '' },
  },

  // start paths stock
  {
    path: 'estoque/materiais',
    loadComponent: () => import('./stock/material/material-page.component').then(m => m.MaterialPageComponent),
    canActivate: [AuthGuard],
    data: { role: ['ADMIN', 'ESTOQUISTA', 'ESTOQUISTA_CHEFE', 'ANALISTA'], path: 'estoque' },
  },
  {
    path: 'estoque/grupos',
    loadComponent: () => import('./stock/groups/groups.component').then(m => m.GroupsComponent),
    canActivate: [AuthGuard],
    data: { role: ['ADMIN', 'ESTOQUISTA', 'ESTOQUISTA_CHEFE'], path: 'estoque' },
  },
  {
    path: 'estoque/tipos',
    loadComponent: () => import('./stock/types/types.component').then(m => m.TypesComponent),
    canActivate: [AuthGuard],
    data: { role: ['ADMIN', 'ESTOQUISTA', 'ESTOQUISTA_CHEFE'], path: 'estoque' },
  },
  {
    path: 'estoque/movimento',
    loadComponent: () => import('./stock/stock-movement/stock-movement.component').then(m => m.StockMovementComponent),
    canActivate: [AuthGuard],
    data: { role: ['ADMIN', 'ESTOQUISTA', 'ESTOQUISTA_CHEFE'], path: 'estoque' },
  },
  {
    path: 'estoque/movimento/pendente',
    loadComponent: () => import('./stock/stock-movement-pending/stock-movement-pending.component').then(m => m.StockMovementPendingComponent),
    canActivate: [AuthGuard],
    data: { role: ['ADMIN', 'ESTOQUISTA_CHEFE'], path: 'estoque' },
  },
  {
    path: 'estoque/movimento/aprovado',
    loadComponent: () => import('./stock/stock-movement-approvated/stock-movement-approvated.component').then(m => m.StockMovementApprovatedComponent),
    canActivate: [AuthGuard],
    data: { role: ['ADMIN', 'ESTOQUISTA', 'ESTOQUISTA_CHEFE'], path: 'estoque' },
  },
  {
    path: 'estoque/almoxarifados',
    loadComponent: () => import('./stock/deposits/deposits.component').then(m => m.DepositsComponent),
    canActivate: [AuthGuard],
    data: { role: ['ADMIN', 'ANALISTA', 'ESTOQUISTA', 'ESTOQUISTA_CHEFE'], path: 'estoque' },
  },
  {
    path: 'estoque/importar',
    loadComponent: () => import('./stock/import-materials/import-materials.component').then(m => m.ImportMaterialsComponent),
    canActivate: [AuthGuard],
    data: { role: ['ADMIN', 'ESTOQUISTA', 'ESTOQUISTA_CHEFE'], path: 'estoque' },
  },


  { path: 'estoque', redirectTo: 'estoque/materiais' },
  // end

  // start contract paths
  {
    path: 'contratos/dashboard',
    loadComponent: () => import('./contract/pages/dashboard/dashboard.component').then(m => m.DashboardComponent),
    canActivate: [AuthGuard],
    data: { role: ['ADMIN', 'ANALISTA'], path: 'contratos' },
  },
  {
    path: 'contratos/criar',
    loadComponent: () => import('./contract/pages/create/create.component').then(m => m.CreateComponent),
    canActivate: [AuthGuard],
    data: { role: ['ADMIN', 'ANALISTA'], path: 'contratos' },
  },
  // end

  // start settings path
  {
    path: 'configuracoes/dashboard',
    loadComponent: () => import('./manage/settings/settings.component').then(s => s.SettingsComponent),
    canActivate: [AuthGuard],
    data: { role: ['ADMIN', 'ANALISTA'], path: 'configuracoes' },
  },
  {
    path: 'configuracoes/usuarios',
    loadComponent: () => import('./manage/user/user.component').then(m => m.UserComponent),
    canActivate: [AuthGuard],
    data: { role: ['ADMIN', ''], path: 'configuracoes' },
  },
  {
    path: 'configuracoes/equipes',
    loadComponent: () => import('./manage/team/team.component').then(m => m.TeamComponent),
    canActivate: [AuthGuard],
    data: { role: ['ADMIN', 'ANALISTA'], path: 'configuracoes' },
  },
  {
    path: 'configuracoes/empresa',
    loadComponent: () => import('./manage/team/team.component').then(m => m.TeamComponent),
    canActivate: [AuthGuard],
    data: { role: ['ADMIN', 'ANALISTA'], path: 'configuracoes' },
  },
  // end

  { path: '', redirectTo: 'estoque/materiais', pathMatch: 'full' },
  { path: 'chamados', redirectTo: 'estoque/materiais' },
  { path: 'configuracoes', redirectTo: 'configuracoes/equipes' },
  { path: 'contratos', redirectTo: 'contratos/criar' },
  { path: 'requisicoes', redirectTo: '**' },
  { path: '**', redirectTo: 'estoque/materiais' }

];
