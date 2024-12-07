import { Component } from '@angular/core';
import {SidebarComponent} from '../../../../shared/components/sidebar/sidebar.component';
import {StockMovementDTO} from '../../stock-movement.dto';
import {StockMovementResponse} from '../../stock-movement-response.dto';
import {HttpClient} from '@angular/common/http';
import {EstoqueService} from '../../services/estoque.service';
import {NgForOf} from '@angular/common';
import {ReactiveFormsModule} from '@angular/forms';
import {TableComponent} from '../../../../shared/components/table/table.component';
import {Title} from '@angular/platform-browser';
import {ButtonComponent} from '../../../../shared/components/button/button.component';
import {ModalComponent} from '../../../../shared/components/modal/modal.component';
import {AlertMessageComponent} from '../../../../shared/components/alert-message/alert-message.component';
import {catchError, tap, throwError} from 'rxjs';

@Component({
  selector: 'app-stock-movement-pending',
  standalone: true,
  imports: [
    SidebarComponent,
    NgForOf,
    ReactiveFormsModule,
    TableComponent,
    ButtonComponent,
    ModalComponent,
    AlertMessageComponent
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
  openModalAprovation: boolean = false;
  movementId: number = 0;
  serverMessage: string | null = null;
  alertType: string | null = null;

  constructor(private stockService: EstoqueService, private title: Title) {
    this.title.setTitle('Estoque - Pendente');
    this.stockService.getStockMovement().subscribe(
      stockMovement => {
        this.stockMovement = stockMovement;
      }
    );
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
