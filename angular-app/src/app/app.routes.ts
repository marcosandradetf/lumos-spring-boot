import { Routes } from '@angular/router';
import {MaterialCreateComponent} from './estoque/material/material-create/material-create.component';


export const routes: Routes = [
  // chamados
  {path: 'chamados', component: MaterialCreateComponent},

  // equipes
  {path: 'equipes', component: MaterialCreateComponent},

  // contratos
  {path: 'contratos', component: MaterialCreateComponent},

  // requisicoes
  {path: 'requisicoes', component: MaterialCreateComponent},

  // estoque
  {path: 'estoque', component: MaterialCreateComponent},

  { path: '', redirectTo: '/home', pathMatch: 'full' },
  { path: '**', redirectTo: '/estoque/materiais' } // Para rotas não encontradas
];
