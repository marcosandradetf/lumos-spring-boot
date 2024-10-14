import { Routes } from '@angular/router';
import {MaterialListComponent} from './estoque/material/material-list/material-list.component';
import {MaterialCreateComponent} from './estoque/material/material-create/material-create.component';
import {MaterialEditComponent} from './estoque/material/material-edit/material-edit.component';


export const routes: Routes = [
  {path: 'estoque/materiais', component: MaterialListComponent },
  {path: 'estoque/materiais/cadastro', component: MaterialCreateComponent},
  {path: 'estoque/materiais/editar/:id', component: MaterialEditComponent},
  { path: '', redirectTo: '/estoque/materiais', pathMatch: 'full' },
  { path: '**', redirectTo: '/estoque/materiais' } // Para rotas não encontradas
];
