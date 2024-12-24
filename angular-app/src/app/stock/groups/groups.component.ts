import { Component } from '@angular/core';
import {FormsModule, NgForm} from '@angular/forms';
import {NgIf} from '@angular/common';
import {SidebarComponent} from '../../shared/components/sidebar/sidebar.component';
import {TableComponent} from '../../shared/components/table/table.component';
import {Group} from '../../core/models/grupo.model';
import {Router} from '@angular/router';
import {EstoqueService} from '../services/estoque.service';
import {Title} from '@angular/platform-browser';

@Component({
  selector: 'app-groups',
  standalone: true,
  imports: [
    FormsModule,
    NgIf,
    SidebarComponent,
    TableComponent
  ],
  templateUrl: './groups.component.html',
  styleUrl: './groups.component.scss'
})
export class GroupsComponent {
  sidebarLinks = [
    { title: 'Gerenciar', path: '/estoque/materiais', id: 'opt1' },
    { title: 'Movimentar Estoque', path: '/estoque/movimento', id: 'opt2' },
    { title: 'Entrada de Nota Fiscal', path: '/estoque/entrada', id: 'opt3' },
    { title: 'Importar Material (.xlsx)', path: '/estoque/importar', id: 'opt4' },
    { title: 'Sugestão de Compra', path: '/estoque/sugestao', id: 'opt5' }
  ];
  formSubmitted: null | boolean = false;
  formOpen: boolean = false;
  group: any = {
    groupName: '',
  }
  gps: Group[] = [];

  constructor(protected router: Router, private stockService: EstoqueService,
              private title: Title,) {
    this.title.setTitle('Gerenciar - Grupos');
    this.stockService.getGroups().subscribe(
      g => this.gps = g
    );
  }

  setOpen() {
    this.formOpen = !this.formOpen;
  }

  onSubmit(myForm: NgForm) {

  }

}
