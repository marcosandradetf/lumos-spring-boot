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
import {NgIf} from '@angular/common';
import {PrimeTemplate} from 'primeng/api';
import {ReactiveFormsModule} from '@angular/forms';
import {Select} from 'primeng/select';
import {Skeleton} from 'primeng/skeleton';
import {TableModule} from 'primeng/table';
import {Tag} from 'primeng/tag';
import {Textarea} from 'primeng/textarea';
import {Toast} from 'primeng/toast';
import {Tooltip} from 'primeng/tooltip';

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
    Tooltip
  ],
  templateUrl: './reservation-pending.component.html',
  styleUrl: './reservation-pending.component.scss'
})
export class ReservationPendingComponent {
  loading = false;
  reservations: ReservationsByCaseDtoResponse[] = [];

  constructor(
    private requestService: RequestService,
    private router: Router,
    private authService: AuthService,
    private utils: UtilsService
  ) {
    this.loading = true
    const userId = this.authService.getUser().uuid;
    this.requestService.getReservation(userId, "pending").subscribe({
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
    })
  }

}
