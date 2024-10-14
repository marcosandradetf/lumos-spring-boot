import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import {MaterialListComponent} from './material/material-list/material-list.component';
import {MaterialCreateComponent} from './material/material-create/material-create.component';
import {MaterialEditComponent} from './material/material-edit/material-edit.component';


@NgModule({
  declarations: [
    // Adicione outros componentes de estoque aqui
  ],
  imports: [
    CommonModule,
    FormsModule,
    MaterialListComponent,
    MaterialCreateComponent,
    MaterialEditComponent
  ],
  exports: [
    MaterialListComponent,
    MaterialCreateComponent,
    MaterialEditComponent,
    // Exporte outros componentes de estoque, se necessário
  ]
})
export class EstoqueModule { }
