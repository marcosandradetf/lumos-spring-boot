import {Component} from '@angular/core';
import {Title} from '@angular/platform-browser';
import {ExecutionService} from '../../executions/execution.service';
import {AuthService} from '../../core/auth/auth.service';
import {UtilsService} from '../../core/service/utils.service';
import {ReserveDTOResponse} from '../../executions/executions.model';
import {NgForOf, NgIf} from '@angular/common';
import {LoadingComponent} from '../../shared/components/loading/loading.component';
import {Router} from '@angular/router';

@Component({
  selector: 'app-reservation-management',
  standalone: true,
  imports: [
    NgIf,
    LoadingComponent,
    NgForOf
  ],
  templateUrl: './reservation-management.component.html',
  styleUrl: './reservation-management.component.scss'
})
export class ReservationManagementComponent {
  loading = true;
  reservations: ReserveDTOResponse[] = [];

  constructor(private titleService: Title,
              private authService: AuthService,
              private utils: UtilsService,
              protected router: Router,
              private executionService: ExecutionService) {
    this.titleService.setTitle("Gerenciamento de Estoque – Pré-instalação");

    this.executionService.getPendingReservesForStockist(this.authService.getUser().uuid).subscribe({
      next: (response) => {
        this.reservations = response;
      },
      error: (error) => {
        this.loading = false;
        this.utils.showMessage(error.error.message, 'error');
      },
      complete: () => {
        this.loading = false;
      }
    });

  }

  getItemsQuantity(reserve: ReserveDTOResponse) {
    let quantity = 0;
    reserve.streets.forEach(s => quantity += s.items.length);

    return quantity;
  }

}
