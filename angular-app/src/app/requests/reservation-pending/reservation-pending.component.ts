import {Component} from '@angular/core';
import {ReservationsByCaseDtoResponse} from '../reservation.models';
import {RequestService} from '../request.service';
import {Router} from '@angular/router';
import {AuthService} from '../../core/auth/auth.service';
import {UtilsService} from '../../core/service/utils.service';
import {Breadcrumb} from 'primeng/breadcrumb';
import {Button} from 'primeng/button';
import {FloatLabel} from 'primeng/floatlabel';
import {InputText} from 'primeng/inputtext';
import {LoadingComponent} from '../../shared/components/loading/loading.component';
import {NgForOf, NgIf} from '@angular/common';
import {MenuItem, PrimeTemplate} from 'primeng/api';
import {ReactiveFormsModule} from '@angular/forms';
import {Select} from 'primeng/select';
import {Skeleton} from 'primeng/skeleton';
import {TableModule} from 'primeng/table';
import {Tag} from 'primeng/tag';
import {Textarea} from 'primeng/textarea';
import {Toast} from 'primeng/toast';
import {Tooltip} from 'primeng/tooltip';
import {DepositByStockist, StockistModel} from '../../executions/executions.model';
import {StockService} from '../../stock/services/stock.service';

@Component({
  selector: 'app-reservation-pending',
  standalone: true,
  imports: [
    Breadcrumb,
    Button,
    FloatLabel,
    InputText,
    LoadingComponent,
    NgIf,
    PrimeTemplate,
    ReactiveFormsModule,
    Select,
    Skeleton,
    TableModule,
    Tag,
    Textarea,
    Toast,
    Tooltip,
    NgForOf
  ],
  templateUrl: './reservation-pending.component.html',
  styleUrl: './reservation-pending.component.scss'
})
export class ReservationPendingComponent {
  home: MenuItem = {icon: 'pi pi-home', routerLink: '/'};
  items: MenuItem[] = [
    {label: 'Requisições'},
    {label: 'Iniciar sem pré-medição'},
  ];

  loading = false;

  deposits: DepositByStockist[] = [];
  reservations: ReservationsByCaseDtoResponse[] = [];
  tableSk: any[] = Array.from({length: 5}).map((_, i) => `Item #${i}`);
  showTeamModal: boolean = false;
  depositName: string | null = null;


  constructor(
    private requestService: RequestService,
    private router: Router,
    private authService: AuthService,
    private utils: UtilsService,
    private stockService: StockService
  ) {
    this.loading = true
    this.stockService.getDepositsByStockist(this.authService.getUser().uuid).subscribe({
      next: (response) => {
        this.deposits = response;
        if(response.length === 1) {
          this.depositName = response[0].depositName
          this.requestService.getReservation(response[0].depositId, "PENDING").subscribe({
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
    this.depositName = this.deposits.find(d => d.depositId = depositId)?.depositName || null;
    this.requestService.getReservation(depositId, "PENDING").subscribe({
      next: (response) => {
        this.reservations = response;
      },
      error: (error) => {
        this.utils.showMessage(error.error.message, "error", "Erro ao buscar Reservas");
        this.loading = false;
      }
    });
  }

}
