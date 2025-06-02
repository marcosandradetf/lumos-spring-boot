import {Component, OnInit} from '@angular/core';
import {AlertMessageComponent} from '../../shared/components/alert-message/alert-message.component';
import {ButtonComponent} from '../../shared/components/button/button.component';
import {ModalComponent} from '../../shared/components/modal/modal.component';
import {CurrencyPipe, NgForOf} from '@angular/common';
import {SidebarComponent} from '../../shared/components/sidebar/sidebar.component';
import {TableComponent} from '../../shared/components/table/table.component';
import {StockMovementResponse} from '../../models/stock-movement-response.dto';
import {EstoqueService} from '../services/estoque.service';
import {Title} from '@angular/platform-browser';
import {Steps} from 'primeng/steps';
import {MenuItem} from 'primeng/api';


@Component({
  selector: 'app-stock-movement-approvated',
  standalone: true,
  imports: [
    NgForOf,
    TableComponent,
    Steps,
    CurrencyPipe
  ],
  templateUrl: './stock-movement-approvated.component.html',
  styleUrl: './stock-movement-approvated.component.scss'
})
export class StockMovementApprovatedComponent implements OnInit {
  sidebarLinks = [
    { title: 'Gerenciar', path: '/estoque/materiais', id: 'opt1' },
    { title: 'Movimentar Estoque', path: '/estoque/movimento', id: 'opt2' },
    { title: 'Entrada de Nota Fiscal', path: '/estoque/entrada', id: 'opt3' },
    { title: 'Importar Material (.xlsx)', path: '/estoque/importar', id: 'opt4' },
    { title: 'Sugestão de Compra', path: '/estoque/sugestao', id: 'opt5' }
  ];

  stockMovement: StockMovementResponse[] = [];
  items: MenuItem[] | undefined;

  constructor(private stockService: EstoqueService, private title: Title) {
    this.title.setTitle('Estoque - Aprovado');
    this.stockService.getStockMovementApproved().subscribe(
      stockMovement => {
        this.stockMovement = stockMovement;
      }
    );
  }

  ngOnInit() {
    this.items = [
      {
        label: 'Selecionar itens',
        routerLink: '/estoque/movimento',
        routerLinkActiveOptions: {exact: true}
      },
      {
        label: 'Pendente de Aprovação',
        routerLink: '/estoque/movimento-pendente'
      },
      {
        label: 'Aprovado',
        routerLink: '/estoque/movimento-aprovado',
      },
    ];
  }


  protected readonly Number = Number;
}
