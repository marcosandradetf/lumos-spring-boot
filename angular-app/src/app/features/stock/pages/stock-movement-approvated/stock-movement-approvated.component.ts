import { Component } from '@angular/core';
import {AlertMessageComponent} from '../../../../shared/components/alert-message/alert-message.component';
import {ButtonComponent} from '../../../../shared/components/button/button.component';
import {ModalComponent} from '../../../../shared/components/modal/modal.component';
import {NgForOf} from '@angular/common';
import {SidebarComponent} from '../../../../shared/components/sidebar/sidebar.component';
import {TableComponent} from '../../../../shared/components/table/table.component';
import {StockMovementResponse} from '../../stock-movement-response.dto';
import {EstoqueService} from '../../services/estoque.service';
import {Title} from '@angular/platform-browser';

@Component({
  selector: 'app-stock-movement-approvated',
  standalone: true,
  imports: [
    AlertMessageComponent,
    ButtonComponent,
    ModalComponent,
    NgForOf,
    SidebarComponent,
    TableComponent
  ],
  templateUrl: './stock-movement-approvated.component.html',
  styleUrl: './stock-movement-approvated.component.scss'
})
export class StockMovementApprovatedComponent {
  sidebarLinks = [
    { title: 'Gerenciar', path: '/estoque/materiais', id: 'opt1' },
    { title: 'Movimentar Estoque', path: '/estoque/movimento', id: 'opt2' },
    { title: 'Entrada de Nota Fiscal', path: '/estoque/entrada', id: 'opt3' },
    { title: 'Importar Material (.xlsx)', path: '/estoque/importar', id: 'opt4' },
    { title: 'Sugestão de Compra', path: '/estoque/sugestao', id: 'opt5' }
  ];

  stockMovement: StockMovementResponse[] = [];

  constructor(private stockService: EstoqueService, private title: Title) {
    this.title.setTitle('Estoque - Aprovado');
    this.stockService.getStockMovementApproved().subscribe(
      stockMovement => {
        this.stockMovement = stockMovement;
      }
    );
  }


}
