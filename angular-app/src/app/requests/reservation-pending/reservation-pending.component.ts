import {Component, OnInit} from '@angular/core';
import {OrderDto, OrdersByCaseResponse} from '../reservation.models';
import {RequestService} from '../request.service';
import {ActivatedRoute, Router} from '@angular/router';
import {AuthService} from '../../core/auth/auth.service';
import {UtilsService} from '../../core/service/utils.service';
import {Button} from 'primeng/button';
import {InputText} from 'primeng/inputtext';
import {NgForOf, NgIf} from '@angular/common';
import {PrimeTemplate} from 'primeng/api';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {Select} from 'primeng/select';
import {Skeleton} from 'primeng/skeleton';
import {Table, TableModule} from 'primeng/table';
import {Tag} from 'primeng/tag';
import {Toast} from 'primeng/toast';
import {Tooltip} from 'primeng/tooltip';
import {DepositByStockist} from '../../executions/executions.model';
import {StockService} from '../../stock/services/stock.service';
import {PrimeConfirmDialogComponent} from '../../shared/components/prime-confirm-dialog/prime-confirm-dialog.component';
import {Title} from '@angular/platform-browser';
import {IconField} from 'primeng/iconfield';
import {InputIcon} from 'primeng/inputicon';
import {isEqual} from 'lodash';
import {LoadingOverlayComponent} from '../../shared/components/loading-overlay/loading-overlay.component';
import {SharedState} from '../../core/service/shared-state';

@Component({
  selector: 'app-reservation-pending',
  standalone: true,
  imports: [
    Button,
    NgIf,
    PrimeTemplate,
    ReactiveFormsModule,
    Select,
    Skeleton,
    TableModule,
    Tag,
    Toast,
    Tooltip,
    NgForOf,
    FormsModule,
    PrimeConfirmDialogComponent,
    IconField,
    InputIcon,
    InputText,
    LoadingOverlayComponent
  ],
  templateUrl: './reservation-pending.component.html',
  styleUrl: './reservation-pending.component.scss'
})
export class ReservationPendingComponent implements OnInit {
  loading = false;
  deposits: DepositByStockist[] = [];
  orders: OrdersByCaseResponse[] = [];
  ordersBackup: OrdersByCaseResponse[] = [];
  tableSk: any[] = Array.from({length: 5}).map((_, i) => `Item #${i}`);
  showTeamModal: boolean = false;
  depositName: string | null = null;
  status = "";
  currentStock: {
    materialId: number,
    stockQuantity: number
  }[] = [];

  replies: {
    approved: {
      reserveId: number | null,
      order: { orderId: string | null, materialId: number, quantity: string | null }
    }[],
    rejected: {
      reserveId: number | null,
      order: { orderId: string | null, materialId: number, quantity: string | null }
    }[],
  } = {
    approved: [],
    rejected: [],
  };

  collected: {
    reserveId: number | null,
    order: { orderId: string | null, materialId: number, quantity: string | null }
  }[] = [];

  selectedOrder: any | null = null;

  lastStock: {
    reserveId: number | null,
    orderId: string | null,
    materialId: number,
    quantity: number
  }[] = [];

  constructor(
    private requestService: RequestService,
    private route: ActivatedRoute,
    private authService: AuthService,
    protected utils: UtilsService,
    private stockService: StockService,
    private titleService: Title
  ) {
  }

  ngOnInit() {
    this.route.queryParams.subscribe(params => {
      this.loading = true;
      this.status = params['status'];

      if (this.status === "PENDING") {
        this.titleService.setTitle("Materiais Pendentes de Aprovação");
        SharedState.setCurrentPath(["Requisições","Materiais Pendentes de Aprovação"]);
      } else {
        this.titleService.setTitle("Materiais Disponíveis para Coleta");
          SharedState.setCurrentPath(["Requisições","Materiais Disponíveis para Coleta"]);
      }

      this.stockService.getDepositsByStockist(this.authService.getUser().uuid).subscribe({
        next: (response) => {
          this.deposits = response;
          if (response.length === 1) {
            this.depositName = response[0].depositName;
            this.requestService.getReservation(response[0].depositId, this.status).subscribe({
              next: (response) => {
                this.orders = response.map(group => ({
                  ...group,
                  reservations: group.orders.map(item => ({
                    ...item,
                    uniqueId: item.reserveId ?? item.orderId ?? item.materialId
                  }))
                }));

                this.ordersBackup = this.orders;

                this.currentStock = Array.from(
                  new Map(
                    this.orders
                      .flatMap(g => g.orders.map(o => ({materialId: o.materialId, stockQuantity: o.stockQuantity})))
                      .map(o => [o.materialId, o])
                  ).values()
                );

              },
              error: (error) => {
                this.utils.showMessage(error.error.message, "error", "Erro ao buscar Reservas");
                this.loading = false;
              }
            });
          }
        },
        error: (error: { error: { message: string } }) => {
          this.utils.showMessage("Erro ao carregar Estoquistas", 'error');
          this.utils.showMessage(error.error.message, 'error');
          this.loading = false;
        },
        complete: () => {
          this.loading = false;
        }
      });
    });
  }

  selectAndEdit(reserve: any, table: Table) {
    if (this.selectedOrder !== null) table.cancelRowEdit(this.selectedOrder);
    this.selectedOrder = reserve;
    table.initRowEdit(reserve);
  }

  getStockQuantity(materialId: number) {
    return this.currentStock
      .find(s => s.materialId === materialId)
      ?.stockQuantity || 0;
  }

  resetQuantity(orderId: string, materialId: number) {
    this.orders = this.orders.map(group => ({
      ...group,
      orders: group.orders.map(orderObj =>
        orderObj.orderId === orderId && orderObj.materialId === materialId
          ? {...orderObj, requestQuantity: null}
          : orderObj
      )
    }));
  }

  setStatus(reserveId: number | null, orderId: string | null, materialId: number, status: string) {
    this.orders = this.orders.map(group => ({
      ...group,
      orders: group.orders.map(orderObj => {
        const matchesOrder =
          orderId !== null && orderObj.orderId === orderId && orderObj.materialId === materialId;

        const matchesReserve =
          reserveId !== null && orderObj.reserveId === reserveId;

        return (matchesOrder || matchesReserve)
          ? { ...orderObj, internStatus: status }
          : orderObj;
      })
    }));
  }

  debitStockQuantity(
    order: {
      reserveId: number | null,
      order: {
        orderId: string | null,
        materialId: number,
        quantity: string | null
      }
    }
  ) {
    const materialId = order.order.materialId;
    const orderId = order.order.orderId;
    const reserveId = order.reserveId;
    const quantity = Number(order.order.quantity || 0);

    const stockIndex = this.currentStock.findIndex(s => s.materialId === materialId);
    let lastStockIndex = this.lastStock
      .findIndex(s => s.materialId === materialId && (s.orderId === orderId || s.reserveId === reserveId));

    if (stockIndex === -1) {
      this.utils.showMessage("Stock Index doesn't exist", 'error');
      if (order.order.orderId !== null)
        this.resetQuantity(order.order.orderId, materialId);
      return;
    }

    if (lastStockIndex === -1) {
      this.lastStock.push({
        reserveId: order.reserveId,
        orderId: order.order.orderId,
        materialId: order.order.materialId,
        quantity: 0,
      });
      lastStockIndex = this.lastStock
        .findIndex(s => s.materialId === materialId && (s.orderId === orderId || s.reserveId === reserveId));
    }

    const currentStockQuantity = this.currentStock[stockIndex].stockQuantity || 0;
    const previousQuantity = this.lastStock[lastStockIndex].quantity || 0;

    // Primeiro, devolve o que já estava reservado (se houver)
    const availableStock = currentStockQuantity + previousQuantity;

    if (availableStock < quantity) {
      this.utils.showMessage(
        `Não há estoque disponível para esse material, a quantidade atual é ${currentStockQuantity}`,
        'warn',
        'Operação não concluída'
      );
      if (order.order.orderId !== null)
        this.resetQuantity(order.order.orderId, materialId);
      return;
    }

    // Atualiza o estoque e a quantidade usada
    this.currentStock[stockIndex].stockQuantity = availableStock - quantity;
    this.lastStock[lastStockIndex].quantity = quantity;

    this.utils.showMessage(
      'Quantidade atribuída com sucesso',
      'info',
      'Operação realizada'
    );
  }


  returnStockQuantity(
    order: {
      reserveId: number | null,
      order: {
        orderId: string | null,
        materialId: number,
        quantity: string | null
      }
    }
  ) {
    const materialId = order.order.materialId;
    const orderId = order.order.orderId;
    const reserveId = order.reserveId;

    const stockIndex = this.currentStock
      .findIndex(s => s.materialId === order.order.materialId);
    const lastStockIndex = this.lastStock
      .findIndex(s => s.materialId === materialId && (s.orderId === orderId || s.reserveId === reserveId));

    if (stockIndex === -1) {
      this.utils.showMessage("Stock Index doesn't exist", 'error');
      if (order.order.orderId !== null) this.resetQuantity(order.order.orderId, order.order.materialId);
      return;
    }

    const currentStockQuantity = this.currentStock[stockIndex].stockQuantity || 0;
    if (lastStockIndex !== -1) {
      this.currentStock[stockIndex].stockQuantity = currentStockQuantity + this.lastStock[lastStockIndex].quantity;
      this.lastStock[lastStockIndex].quantity = 0;
    }
  }

  getReservations(depositId
                  :
                  number
  ) {
    this.loading = true;
    this.depositName = this.deposits.find(d => d.depositId = depositId)?.depositName || null;
    this.requestService.getReservation(depositId, "PENDING").subscribe({
      next: (response) => {
        this.orders = response.map(group => ({
          ...group,
          reservations: group.orders.map(item => ({
            ...item,
            uniqueId: item.reserveId ?? item.orderId ?? item.materialId
          }))
        }));

        this.ordersBackup = this.orders;

      },
      error: (error) => {
        this.utils.showMessage(error.error.message, "error", "Erro ao buscar Reservas");
        this.loading = false;
      },
      complete: () => {
        this.loading = false;
      }
    });
  }

  loadingButton = false;

  reply(order: OrderDto, action: 'APPROVE' | 'REJECT' | 'COLLECT') {
    const target = {
      reserveId: order.reserveId,
      order: {
        orderId: order.orderId,
        materialId: order.materialId,
        quantity: order.requestQuantity
      }
    };

    this.loadingButton = true;
    switch (action) {
      case 'APPROVE':
        this.replies.rejected = this.replies.rejected.filter(obj => !isEqual(obj, target));
        const approvedIndex = this.replies.approved.findIndex(obj => isEqual(obj, target));
        if (approvedIndex === -1) {
          if (target.order.quantity === null || target.order.quantity === '0' || target.order.quantity === '') {
            this.utils.showMessage('Informe a quantidade a ser liberada', 'warn', 'Ação não realizada');
            this.loadingButton = false;
            return;
          } else if (target.order.orderId || target.reserveId) {
            this.debitStockQuantity(target);
          }

          this.setStatus(target.reserveId, target.order.orderId, target.order.materialId, "APROVADO");

          this.replies.approved.push(target);
          this.loadingButton = false;
        }

        break;
      case 'REJECT':
        this.replies.approved = this.replies.approved.filter(obj => !isEqual(obj, target));
        const rejectedIndex = this.replies.rejected.findIndex(obj => isEqual(obj, target));
        if (rejectedIndex === -1) {
          this.returnStockQuantity(target);
          this.replies.rejected.push(target);
          this.setStatus(target.reserveId, target.order.orderId, target.order.materialId, "REJEITADO");
        }
        break;
      case 'COLLECT':
        this.collected = this.collected.filter(obj => !isEqual(obj, target));
        this.debitStockQuantity(target);
        this.collected.push(target);
        this.setStatus(target.reserveId, target.order.orderId, target.order.materialId, "COLETADO");
        break;
    }

  }

  modalSendData = false;

  sendData() {
    const hasAtLeastOneResponse = this.orders.some(group =>
      group.orders.some(r => r.internStatus !== null && r.internStatus !== undefined)
    );

    if (!hasAtLeastOneResponse) {
      this.utils.showMessage("Nenhuma reserva foi respondida.", 'warn', 'Atenção');
      return;
    }

    for (const group of this.orders) {
      const allFilled = group.orders.every(r => r.internStatus !== null && r.internStatus !== undefined);
      const noneFilled = group.orders.every(r => r.internStatus === null || r.internStatus === undefined);

      const anyWithout = group.orders.some(r => r.requestQuantity === null && r.internStatus === 'APROVADO');

      // Se tiver apenas alguns preenchidos (nem todos, nem nenhum), erro.
      if (!allFilled && !noneFilled) {
        this.utils.showMessage(`Responda todas as reservas pendentes da ${group.description}.`, 'warn', 'Atenção');
        return;
      } else if (anyWithout) {
        this.utils.showMessage(`Informe a quantidade a ser aprovada dos materiais para ${group.description}.`, 'warn', 'Atenção');
        return;
      }
    }

    this.modalSendData = true;
  }


  handleAction($event
               :
                 "accept" | "reject"
  ) {
    switch ($event) {
      case 'reject':
        this.modalSendData = false;
        this.utils.showMessage('Operação cancelada com sucesso', 'info', 'Feito');
        break;
      case 'accept':
        this.modalSendData = false;
        this.loading = true;
        if (this.status === "PENDING") {
          this.requestService.reply(this.replies).subscribe({
            next: () => {
              const replies = [
                ...this.replies.approved,
                ...this.replies.rejected,
              ];

              this.orders = this.orders.map(group => ({
                ...group,
                reservations: group.orders.filter(orderObj =>
                  !replies.some(reply =>
                    isEqual(
                      {
                        reserveId: orderObj.reserveId,
                        order: {orderId: orderObj.orderId, materialId: orderObj.materialId, quantity: orderObj.requestQuantity}
                      },
                      reply
                    )
                  )
                )
              })).filter(group => group.reservations.length > 0);


              this.utils.showMessage(
                "Para materiais aprovados: Acesse o menu (Materiais disponíveis para Coleta) pois é necessário marcar como coletado e atualizar o estoque.",
                "success", "Dados Salvos", true
              );
            },
            error: (err) => {
              this.loading = false;
              this.utils.showMessage(err.error.message, "error", "Erro ao responder reserva de material");
            },
            complete: () => {
              this.loading = false;
            }
          });
        } else {
          this.requestService.markAsCollected(this.collected).subscribe({
            next: () => {
              this.orders = this.orders.map(group => ({
                ...group,
                reservations: group.orders.filter(orderObj =>
                  !this.collected.some(reply =>
                    isEqual(
                      {
                        reserveId: orderObj.reserveId,
                        order: {orderId: orderObj.orderId, materialId: orderObj.materialId, quantity: orderObj.requestQuantity}
                      },
                      reply
                    )
                  )
                )
              })).filter(group => group.reservations.length > 0);

              this.utils.showMessage("A Equipe já pode iniciar a execução com esses materiais", "success", "Dados enviados com sucesso");
            },
            error: (err) => {
              this.loading = false;
              this.utils.showMessage(err.error.message, "error", "Erro ao marcar materiais como coletados");
            },
            complete: () => {
              this.loading = false;
            }
          });
        }
        break;
    }

  }

  filterOrders(event
               :
               Event
  ) {
    let value = (event.target as HTMLInputElement).value;

    if (value === null || value === undefined || value === '') {
      this.orders = this.ordersBackup;
    }

    this.orders = this.ordersBackup.filter(r => r.description.toLowerCase().includes(value.toLowerCase()));
  }


  protected readonly Number = Number;
}
