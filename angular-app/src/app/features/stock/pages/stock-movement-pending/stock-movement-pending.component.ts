import { Component } from '@angular/core';
import {SidebarComponent} from '../../../../shared/components/sidebar/sidebar.component';
import {StockMovementDTO} from '../../stock-movement.dto';
import {StockMovementResponse} from '../../stock-movement-response.dto';
import {HttpClient} from '@angular/common/http';
import {EstoqueService} from '../../services/estoque.service';
import {NgForOf} from '@angular/common';
import {ReactiveFormsModule} from '@angular/forms';
import {TableComponent} from '../../../../shared/components/table/table.component';

@Component({
  selector: 'app-stock-movement-pending',
  standalone: true,
  imports: [
    SidebarComponent,
    NgForOf,
    ReactiveFormsModule,
    TableComponent
  ],
  templateUrl: './stock-movement-pending.component.html',
  styleUrl: './stock-movement-pending.component.scss'
})
export class StockMovementPendingComponent {
  sidebarLinks = [
    { title: 'Gerenciar', path: '/estoque/materiais', id: 'opt1' },
    { title: 'Movimentar Estoque', path: '/estoque/movimento', id: 'opt2' },
    { title: 'Entrada de Nota Fiscal', path: '/estoque/entrada', id: 'opt3' },
    { title: 'Importar Material (.xlsx)', path: '/estoque/importar', id: 'opt4' },
    { title: 'Sugestão de Compra', path: '/estoque/sugestao', id: 'opt5' }
  ];

  stockMovement: StockMovementResponse[] = [];

  constructor(private stockService: EstoqueService) {
    this.stockService.getStockMovement().subscribe(
      stockMovement => {
        this.stockMovement = stockMovement;
      }
    );
  }


}
