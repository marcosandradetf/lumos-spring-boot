import {Component, OnInit} from '@angular/core';
import {SidebarComponent} from '../../shared/components/sidebar/sidebar.component';
import {NgForOf} from '@angular/common';
import {ReactiveFormsModule} from '@angular/forms';
import {TableComponent} from '../../shared/components/table/table.component';
import {ButtonComponent} from '../../shared/components/button/button.component';
import {ModalComponent} from '../../shared/components/modal/modal.component';
import {AlertMessageComponent} from '../../shared/components/alert-message/alert-message.component';
import {StockMovementResponse} from '../../models/stock-movement-response.dto';
import {EstoqueService} from '../services/estoque.service';
import {Title} from '@angular/platform-browser';
import {catchError, tap, throwError} from 'rxjs';
import {Steps} from 'primeng/steps';
import {MenuItem} from 'primeng/api';


@Component({
  selector: 'app-stock-movement-pending',
  standalone: true,
  imports: [
    NgForOf,
    ReactiveFormsModule,
    TableComponent,
    ButtonComponent,
    ModalComponent,
    AlertMessageComponent,
    Steps
  ],
  templateUrl: './stock-movement-pending.component.html',
  styleUrl: './stock-movement-pending.component.scss'
})
export class StockMovementPendingComponent implements OnInit {
  sidebarLinks = [
    { title: 'Gerenciar', path: '/estoque/materiais', id: 'opt1' },
    { title: 'Movimentar Estoque', path: '/estoque/movimento', id: 'opt2' },
    { title: 'Entrada de Nota Fiscal', path: '/estoque/entrada', id: 'opt3' },
    { title: 'Importar Material (.xlsx)', path: '/estoque/importar', id: 'opt4' },
    { title: 'Sugestão de Compra', path: '/estoque/sugestao', id: 'opt5' }
  ];

  stockMovement: StockMovementResponse[] = [];
  openModalAprovation: boolean = false;
  movementId: number = 0;
  serverMessage: string | null = null;
  alertType: string | null = null;
  items: MenuItem[] | undefined;

  constructor(private stockService: EstoqueService, private title: Title) {
    this.title.setTitle('Estoque - Pendente');
    this.stockService.getStockMovement().subscribe(
      stockMovement => {
        this.stockMovement = stockMovement;
      }
    );
  }

  ngOnInit() {
    this.items = [
      {
        label: 'Selecionar itens',
        routerLink: '/estoque/movimento'
      },
      {
        label: 'Pendente de Aprovação',
        routerLink: '/estoque/movimento-pendente'
      },
      {
        label: 'Aprovado',
        routerLink: '/estoque/movimento/aprovado'
      },
    ];
  }

  handleOpenModal(id: number): void {
    this.movementId = id;
    this.openModalAprovation = true;
  }

  closeAprovationModal() {
    this.openModalAprovation = false;
    this.movementId = 0;
  }

  submitAprovationMovement() {
    this.stockService.approveStockMovement(this.movementId).pipe(
      tap(response => {
        this.stockMovement = this.stockMovement.filter(m => m.id !== this.movementId);
        this.closeAprovationModal();
        this.serverMessage = response;
        this.alertType = 'alert-success';
      }),
      catchError(err => {
        this.serverMessage = err.message;
        this.alertType = 'alert-error';
        this.closeAprovationModal();
        return throwError(() => err);
      })
    ).subscribe();
  }


  getMovementmovement(Id: number): any {
    return this.stockMovement.find((x) => x.id === Id);
  }


  RejectMovement() {
    this.stockService.rejectStockMovement(this.movementId).pipe(
      tap(response => {
        this.closeAprovationModal();
        this.serverMessage = response;
        this.alertType = 'alert-success';
      }),
      catchError(err => {
        this.serverMessage = err.message;
        this.alertType = 'alert-error';
        this.closeAprovationModal();
        return throwError(() => err);
      })
    )
  }

}
