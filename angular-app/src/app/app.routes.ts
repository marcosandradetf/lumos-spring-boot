// app/app.routes.ts
import { Routes } from '@angular/router';
import { AuthGuard } from './core/guards/auth.guard';


export const routes: Routes = [
  { path: '', redirectTo: 'estoque/materiais', pathMatch: 'full' },
  {
    path: 'auth/login',
    loadComponent: () => import('./auth/login/login.component').then(m => m.LoginComponent)
  },
  {
    path: 'estoque/materiais',
    loadComponent: () => import('./estoque/material/material-page/material-page.component').then(m => m.MaterialPageComponent),
    canActivate: [AuthGuard],
  },
  { path: '**', redirectTo: 'auth/login' }
];
