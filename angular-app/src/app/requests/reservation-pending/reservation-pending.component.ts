import {Component, OnInit} from '@angular/core';
import {OrderDto, OrdersByCaseResponse} from '../reservation.models';
import {RequestService} from '../request.service';
import {ActivatedRoute, Router} from '@angular/router';
import {AuthService} from '../../core/auth/auth.service';
import {UtilsService} from '../../core/service/utils.service';
import {Breadcrumb} from 'primeng/breadcrumb';
import {Button} from 'primeng/button';
import {InputText} from 'primeng/inputtext';
import {LoadingComponent} from '../../shared/components/loading/loading.component';
import {NgForOf, NgIf} from '@angular/common';
import {MenuItem, PrimeTemplate} from 'primeng/api';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {Select} from 'primeng/select';
import {Skeleton} from 'primeng/skeleton';
import {TableModule} from 'primeng/table';
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

@Component({
  selector: 'app-reservation-pending',
  standalone: true,
  imports: [
    Breadcrumb,
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
    LoadingComponent,
    FormsModule,
    PrimeConfirmDialogComponent,
    IconField,
    InputIcon,
    InputText
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

  home: MenuItem = {icon: 'pi pi-home', routerLink: '/'};
  items: MenuItem[] | undefined = undefined;


  constructor(
    private requestService: RequestService,
    private router: Router,
    private route: ActivatedRoute,
    private authService: AuthService,
    private utils: UtilsService,
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
        this.items = [
          {label: 'Requisições'},
          {label: 'Materiais Pendentes de Aprovação'},
        ];
      } else {
        this.titleService.setTitle("Materiais Disponíveis para Coleta");
        this.items = [
          {label: 'Requisições'},
          {label: 'Materiais Disponíveis para Coleta'},
        ];
      }

      this.stockService.getDepositsByStockist(this.authService.getUser().uuid).subscribe({
        next: (response) => {
          this.deposits = response;
          if (response.length === 1) {
            this.depositName = response[0].depositName
            this.requestService.getReservation(response[0].depositId, this.status).subscribe({
              next: (response) => {
                this.orders = response.map(group => ({
                  ...group,
                  reservations: group.orders.map(item => ({
                    ...item,
                    uniqueId: item.reserveId ?? item.materialId
                  }))
                }));

                this.ordersBackup = this.orders;

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

  getReservations(depositId: number) {
    this.loading = true;
    this.depositName = this.deposits.find(d => d.depositId = depositId)?.depositName || null;
    this.requestService.getReservation(depositId, "PENDING").subscribe({
      next: (response) => {
        this.orders = response.map(group => ({
          ...group,
          reservations: group.orders.map(item => ({
            ...item,
            uniqueId: item.reserveId ?? item.materialId
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


  replies: {
    approved: { reserveId: number | null, order: { orderId: string | null, materialId: number } }[],
    rejected: { reserveId: number | null, order: { orderId: string | null, materialId: number } }[],
  } = {
    approved: [],
    rejected: [],
  };

  collected: { reserveId: number | null, order: { orderId: string | null, materialId: number } }[] = [];

  reply(order: OrderDto, action: 'APPROVE' | 'REJECT' | 'COLLECT') {
    const target = {
      reserveId: order.reserveId,
      order: {
        orderId: order.orderId,
        materialId: order.materialId
      }
    };

    switch (action) {
      case 'APPROVE':
        this.replies.rejected = this.replies.rejected.filter(obj => !isEqual(obj, target));
        const approvedIndex = this.replies.approved.findIndex(obj => isEqual(obj, target));
        if (approvedIndex === -1) {
          this.replies.approved.push(
            {reserveId: order.reserveId, order: {orderId: order.orderId, materialId: order.materialId}}
          );
        }

        break;
      case 'REJECT':
        this.replies.approved = this.replies.approved.filter(obj => !isEqual(obj, target));
        const rejectedIndex = this.replies.rejected.findIndex(obj => isEqual(obj, target));
        if (rejectedIndex === -1) {
          this.replies.rejected.push(
            {reserveId: order.reserveId, order: {orderId: order.orderId, materialId: order.materialId}}
          );
        }
        break;
      case 'COLLECT':
        this.collected = this.collected.filter(obj => !isEqual(obj, target));
        this.collected.push(
          {reserveId: order.reserveId, order: {orderId: order.orderId, materialId: order.materialId}}
        );
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

      const allWithout = group.orders.every(r => r.requestQuantity !== null && r.internStatus !== 'APROVADO');
      const noneWithout = group.orders.every(r => r.requestQuantity === null && r.internStatus !== 'APROVADO');

      // Se tiver apenas alguns preenchidos (nem todos, nem nenhum), erro.
      if (!allFilled && !noneFilled) {
        this.utils.showMessage(`Responda todas as reservas pendentes da ${group.description}.`, 'warn', 'Atenção');
        return;
      } else if (!allWithout && !noneWithout) {
        this.utils.showMessage(`Informe a quantidade a ser aprovada dos materiais para ${group.description}.`, 'warn', 'Atenção');
        return;
      }
    }

    this.modalSendData = true;
  }


  handleAction($event: "accept" | "reject") {
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
                        order: {orderId: orderObj.orderId, materialId: orderObj.materialId}
                      },
                      reply
                    )
                  )
                )
              })).filter(group => group.reservations.length > 0);

              this.utils.showMessage(
                "Acesse a tela (Materiais disponíveis para Coleta) para notificar a equipe responsável e marcar como coletado.",
                "success", "Dados Salvos - Os materiais estão pendentes de coleta", true
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
                        order: {orderId: orderObj.orderId, materialId: orderObj.materialId}
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

  filterOrders(event: Event) {
    let value = (event.target as HTMLInputElement).value;

    if (value === null || value === undefined || value === '') {
      this.orders = this.ordersBackup;
    }

    this.orders = this.ordersBackup.filter(r => r.description.toLowerCase().includes(value.toLowerCase()));
  }


}
