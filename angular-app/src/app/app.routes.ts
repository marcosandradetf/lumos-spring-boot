// app/app.routes.ts
import { Routes } from '@angular/router';
import { AuthGuard } from './core/auth/auth.guard';


export const routes: Routes = [
  { path: '', redirectTo: 'estoque/materiais', pathMatch: 'full' },
  {
    path: 'auth/login',
    loadComponent: () => import('./core/auth/pages/login/login.component').then(m => m.LoginComponent)
  },
  {
    path: 'estoque/materiais',
    loadComponent: () => import('./features/estoque/pages/material/material-page.component').then(m => m.MaterialPageComponent),
    canActivate: [AuthGuard],
  },
  {
    path: 'contratos/dashboard',
    loadComponent: () => import('./features/contract/pages/dashboard/dashboard.component').then(m => m.DashboardComponent),
    canActivate: [AuthGuard],
  },

  {
    path: 'contratos/criar',
    loadComponent: () => import('./features/contract/pages/create/create.component').then(m => m.CreateComponent),
    canActivate: [AuthGuard],
  },

  {
    path: 'estoque/movimento',
    loadComponent: () => import('./features/estoque/pages/stock-movement/stock-movement.component').then(m => m.StockMovementComponent),
    canActivate: [AuthGuard],
  },

  { path: '**', redirectTo: 'estoque/materiais' }
];
