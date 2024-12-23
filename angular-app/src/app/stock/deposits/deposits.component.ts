import { Component } from '@angular/core';
import {SidebarComponent} from '../../shared/components/sidebar/sidebar.component';
import {EstoqueService} from '../../features/stock/services/estoque.service';
import {Title} from '@angular/platform-browser';
import {Router} from '@angular/router';
import {MaterialFormComponent} from '../../features/stock/pages/material/material-form/material-form.component';
import {FormsModule, NgForm} from '@angular/forms';
import {State} from '../../features/stock/services/material.service';
import {NgIf} from '@angular/common';
import {Company} from '../../core/models/empresa.model';
import {TableComponent} from '../../shared/components/table/table.component';
import {Deposit} from '../../core/models/almoxarifado.model';

@Component({
  selector: 'app-deposits',
  standalone: true,
  imports: [
    SidebarComponent,
    MaterialFormComponent,
    FormsModule,
    NgIf,
    TableComponent
  ],
  templateUrl: './deposits.component.html',
  styleUrl: './deposits.component.scss'
})
export class DepositsComponent {
  sidebarLinks = [
    { title: 'Gerenciar', path: '/estoque/materiais', id: 'opt1' },
    { title: 'Movimentar Estoque', path: '/estoque/movimento', id: 'opt2' },
    { title: 'Entrada de Nota Fiscal', path: '/estoque/entrada', id: 'opt3' },
    { title: 'Importar Material (.xlsx)', path: '/estoque/importar', id: 'opt4' },
    { title: 'Sugestão de Compra', path: '/estoque/sugestao', id: 'opt5' }
  ];
  formOpen: boolean = false;
  deposit: any = {
    depositName: "",
    companyId: ""
  }
  formSubmitted: null | boolean = false;
  companies: Company[] = []
  deposits: any[] = [];

  constructor(private stockService: EstoqueService,
              private title: Title, protected router: Router) {
    this.stockService.getCompanies().subscribe(
      c => this.companies = c
    );
  }

  setOpen() {
    this.formOpen = !this.formOpen;
  }

  onSubmit(myForm: NgForm) {

  }
}
