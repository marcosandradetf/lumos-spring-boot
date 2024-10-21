import { Routes } from '@angular/router';
import {MaterialCreateComponent} from './estoque/material/material-create/material-create.component';
import {MaterialEditComponent} from './estoque/material/material-edit/material-edit.component';


export const routes: Routes = [
  // {path: 'estoque/materiais/lista', component: MaterialListComponent },
  {path: 'estoque/materiais', component: MaterialCreateComponent},
  {path: 'estoque/materiais/editar/:id', component: MaterialEditComponent},
  { path: '', redirectTo: '/estoque/materiais', pathMatch: 'full' },
  { path: '**', redirectTo: '/estoque/materiais' } // Para rotas não encontradas
];
