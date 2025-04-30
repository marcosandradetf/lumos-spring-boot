import {Component} from '@angular/core';
import { CommonModule } from '@angular/common'; // Importando o CommonModule
import { Router } from '@angular/router';

import {ReactiveFormsModule} from '@angular/forms';
import {MaterialFormComponent} from './material-form/material-form.component';
import {TabelaComponent} from './tabela/tabela.component';
import {Title} from '@angular/platform-browser';
import {MaterialResponse} from '../../models/material-response.dto';
import {MaterialService} from '../services/material.service';
import {EstoqueService} from '../services/estoque.service';
import {SidebarComponent} from '../../shared/components/sidebar/sidebar.component';


@Component({
  selector: 'app-material-page',
  standalone: true,
  templateUrl: './material-page.component.html',
  styleUrls: ['./material-page.component.scss'],
  imports: [CommonModule, ReactiveFormsModule, MaterialFormComponent, SidebarComponent, TabelaComponent] // Adicionando os módulos aqui
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

  constructor(private materialService: MaterialService, private estoque: EstoqueService,
              private titleService:Title, protected router: Router) {
    this.titleService.setTitle("Gerenciar - Materiais");
  }


  // Atualiza o filtro de pesquisa e aplica os filtros combinados
  filterSearch(value: string): void {
    this.searchFilter = value.toLowerCase(); // Armazena o filtro de pesquisa
  }


  protected readonly alert = alert;

  setOpen() {
    this.formOpen = !this.formOpen;
  }

}
