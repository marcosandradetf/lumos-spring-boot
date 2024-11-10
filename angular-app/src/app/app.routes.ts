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
    path: 'contratos',
    loadComponent: () => import('./features/contract/pages/create/create.component').then(m => m.CreateComponent),
    canActivate: [AuthGuard],
  },

  { path: '**', redirectTo: 'estoque/materiais' }
];
