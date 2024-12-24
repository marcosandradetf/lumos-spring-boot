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

  // start paths stock
  {
    path: 'estoque/materiais',
    loadComponent: () => import('./stock/material/material-page.component').then(m => m.MaterialPageComponent),
    canActivate: [AuthGuard],
  },
  {
    path: 'estoque/grupos',
    loadComponent: () => import('./stock/groups/groups.component').then(m => m.GroupsComponent),
    canActivate: [AuthGuard],
  },
  {
    path: 'estoque/tipos',
    loadComponent: () => import('./stock/types/types.component').then(m => m.TypesComponent),
    canActivate: [AuthGuard],
  },
  {
    path: 'estoque/movimento',
    loadComponent: () => import('./stock/stock-movement/stock-movement.component').then(m => m.StockMovementComponent),
    canActivate: [AuthGuard],
  },
  {
    path: 'estoque/movimento/pendente',
    loadComponent: () => import('./stock/stock-movement-pending/stock-movement-pending.component').then(m => m.StockMovementPendingComponent),
    canActivate: [AuthGuard],
  },
  {
    path: 'estoque/movimento/aprovado',
    loadComponent: () => import('./stock/stock-movement-approvated/stock-movement-approvated.component').then(m => m.StockMovementApprovatedComponent),
    canActivate: [AuthGuard],
  },
  {
    path: 'estoque/almoxarifados',
    loadComponent: () => import('./stock/deposits/deposits.component').then(m => m.DepositsComponent),
    canActivate: [AuthGuard],
  },
  { path: 'estoque', redirectTo: 'estoque/materiais' },
  // end

  // start contract paths
  {
    path: 'contratos/dashboard',
    loadComponent: () => import('./contract/pages/dashboard/dashboard.component').then(m => m.DashboardComponent),
    canActivate: [AuthGuard],
  },
  {
    path: 'contratos/criar',
    loadComponent: () => import('./contract/pages/create/create.component').then(m => m.CreateComponent),
    canActivate: [AuthGuard],
  },
  // end

  { path: '', redirectTo: 'estoque/materiais', pathMatch: 'full' },
  { path: 'chamados', redirectTo: 'estoque/materiais' },
  { path: 'equipes', redirectTo: 'estoque/materiais' },
  { path: 'contratos', redirectTo: 'contratos/criar' },
  { path: 'requisicoes', redirectTo: '**' },
  { path: '**', redirectTo: 'estoque/materiais' }

];
