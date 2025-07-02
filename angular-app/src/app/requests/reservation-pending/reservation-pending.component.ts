import {Component, OnInit} from '@angular/core';
import {ReservationsByCaseDtoResponse} from '../reservation.models';
import {RequestService} from '../request.service';
import {ActivatedRoute, Router} from '@angular/router';
import {AuthService} from '../../core/auth/auth.service';
import {UtilsService} from '../../core/service/utils.service';
import {Breadcrumb} from 'primeng/breadcrumb';
import {Button} from 'primeng/button';
import {FloatLabel} from 'primeng/floatlabel';
import {InputText} from 'primeng/inputtext';
import {LoadingComponent} from '../../shared/components/loading/loading.component';
import {NgForOf, NgIf} from '@angular/common';
import {MenuItem, PrimeTemplate} from 'primeng/api';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {Select} from 'primeng/select';
import {Skeleton} from 'primeng/skeleton';
import {TableModule} from 'primeng/table';
import {Tag} from 'primeng/tag';
import {Textarea} from 'primeng/textarea';
import {Toast} from 'primeng/toast';
import {Tooltip} from 'primeng/tooltip';
import {DepositByStockist, StockistModel} from '../../executions/executions.model';
import {StockService} from '../../stock/services/stock.service';
import {PrimeConfirmDialogComponent} from '../../shared/components/prime-confirm-dialog/prime-confirm-dialog.component';
import {Title} from '@angular/platform-browser';

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
    InputText,
    FormsModule,
    PrimeConfirmDialogComponent
  ],
  templateUrl: './reservation-pending.component.html',
  styleUrl: './reservation-pending.component.scss'
})
export class ReservationPendingComponent implements OnInit {
  loading = false;

  deposits: DepositByStockist[] = [];
  reservations: ReservationsByCaseDtoResponse[] = [];
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
    this.loading = true;

    this.route.queryParams.subscribe(params => {
      this.status = params['status'];
    });

    if (this.status === "PENDING") {
      this.titleService.setTitle("Materiais Pendentes de Aprovação");
      this.items =  [
        {label: 'Requisições'},
        {label: 'Materiais Pendentes de Aprovação'},
      ];
    } else {
      this.titleService.setTitle("Materiais Disponíveis para Coleta");
      this.items =  [
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
              this.reservations = response;
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
  }

  getReservations(depositId: number) {
    this.loading = true;
    this.depositName = this.deposits.find(d => d.depositId = depositId)?.depositName || null;
    this.requestService.getReservation(depositId, "PENDING").subscribe({
      next: (response) => {
        this.reservations = response;
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
    approved: { reserveId: number }[],
    rejected: { reserveId: number }[],
  } = {
    approved: [],
    rejected: [],
  };

  collected: number[] = [];

  reply(reserveId: number, action: 'APPROVE' | 'REJECT' | 'COLLECT') {
    switch (action) {
      case 'APPROVE':
        this.replies.rejected = this.replies.rejected.filter(reply => reply.reserveId !== reserveId);
        const approvedIndex = this.replies.approved.findIndex(approved => approved.reserveId === reserveId);
        if (approvedIndex === -1) {
          this.replies.approved.push({reserveId});
        }
        break;
      case 'REJECT':
        this.replies.approved = this.replies.approved.filter(reply => reply.reserveId !== reserveId);
        const rejectedIndex = this.replies.rejected.findIndex(approved => approved.reserveId === reserveId);
        if (rejectedIndex === -1) {
          this.replies.rejected.push({reserveId});
        }
        break;
      case 'COLLECT':
        this.collected.filter(n => n !== reserveId);
        this.collected.push(reserveId);
        break;
    }

  }

  modalSendData = false;
  sendData() {
    const hasAtLeastOneResponse = this.reservations.some(group =>
      group.reservations.some(r => r.internStatus !== null && r.internStatus !== undefined)
    );

    if (!hasAtLeastOneResponse) {
      this.utils.showMessage("Nenhuma reserva foi respondida.", 'warn', 'Atenção');
      return;
    }

    for (const group of this.reservations) {
      const allFilled = group.reservations.every(r => r.internStatus !== null && r.internStatus !== undefined);
      const noneFilled = group.reservations.every(r => r.internStatus === null || r.internStatus === undefined);

      // Se tiver apenas alguns preenchidos (nem todos, nem nenhum), erro.
      if (!allFilled && !noneFilled) {
        this.utils.showMessage(`Responda todas as reservas pendentes da ${group.description}.`, 'warn', 'Atenção');
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
              const repliedIds = [
                ...this.replies.approved.map(r => r.reserveId),
                ...this.replies.rejected.map(r => r.reserveId),
              ];

              this.reservations = this.reservations.map(group => ({
                ...group,
                reservations: group.reservations.filter(reservation =>
                  !repliedIds.includes(reservation.reserveId)
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
              this.reservations = this.reservations.map(group => ({
                ...group,
                reservations: group.reservations.filter(reservation =>
                  !this.collected.includes(reservation.reserveId)
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

}
