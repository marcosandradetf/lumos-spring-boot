import {Component, inject, OnInit} from '@angular/core';
import {MaterialResponse} from '../../material-response.dto';
import {MaterialService} from '../../services/material.service';
import { CommonModule } from '@angular/common'; // Importando o CommonModule
import { RouterModule } from '@angular/router';
import {DeleteMaterialModalComponent} from '../../../../shared/components/modal-delete/delete.component'; // Importando o RouterModule

import {FormBuilder, FormControl, ReactiveFormsModule} from '@angular/forms';
import {toSignal} from '@angular/core/rxjs-interop';
import {map} from 'rxjs';
import {MaterialFormComponent} from './material-form/material-form.component';
import {SidebarComponent} from "../../../../shared/components/sidebar/sidebar.component";
import {TabelaComponent} from './tabela/tabela.component';
import {HeaderComponent} from '../../../../shared/components/header/header.component';
import {Router} from 'express';
import {EstoqueService} from '../../services/estoque.service'; // Importar MatSnackBar



@Component({
  selector: 'app-material-page',
  standalone: true,
  templateUrl: './material-page.component.html',
  styleUrls: ['./material-page.component.scss'],
  imports: [CommonModule, RouterModule, DeleteMaterialModalComponent, ReactiveFormsModule, MaterialFormComponent, SidebarComponent, TabelaComponent] // Adicionando os módulos aqui
})
export class MaterialPageComponent {
  private searchFilter: string = '';
  formOpen: boolean = false;
  materiais: MaterialResponse[] = []; // Inicializando a lista de materiais
  sidebarLinks = [
    { title: 'Gerenciar', path: '/estoque/materiais', id: 'opt1' },
    { title: 'Movimentar Estoque', path: '/estoque/movimento', id: 'opt2' },
    { title: 'Entrada de Nota Fiscal', path: '/estoque/entrada', id: 'opt3' },
    { title: 'Importar Material (.xlsx)', path: '/estoque/importar', id: 'opt4' },
    { title: 'Sugestão de Compra', path: '/estoque/sugestao', id: 'opt5' }
  ];

  constructor(private materialService: MaterialService, private estoque: EstoqueService,) {}


  // Atualiza o filtro de pesquisa e aplica os filtros combinados
  filterSearch(value: string): void {
    this.searchFilter = value.toLowerCase(); // Armazena o filtro de pesquisa
  }


  protected readonly alert = alert;

  setOpen() {
    this.formOpen = !this.formOpen;
  }

}
