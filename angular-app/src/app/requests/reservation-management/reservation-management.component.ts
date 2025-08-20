import {Component, ViewChild} from '@angular/core';
import {Title} from '@angular/platform-browser';
import {ExecutionService} from '../../executions/execution.service';
import {AuthService} from '../../core/auth/auth.service';
import {GetObjectRequest, SetObjectRequest, UtilsService} from '../../core/service/utils.service';
import {ReserveDTOResponse, ReserveStreetDTOResponse} from '../../executions/executions.model';
import {NgForOf, NgIf} from '@angular/common';
import {LoadingComponent} from '../../shared/components/loading/loading.component';
import {Router} from '@angular/router';
import {Menu} from 'primeng/menu';
import {MenuItem} from 'primeng/api';
import {Toast} from 'primeng/toast';

@Component({
  selector: 'app-reservation-management',
  standalone: true,
  imports: [
    NgIf,
    LoadingComponent,
    NgForOf,
    Menu,
    Toast
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

  @ViewChild('menu') menu: Menu | undefined;
  contextItems: MenuItem[] = [
    {
      label: 'Cancelar e excluir etapa',
      icon: 'pi pi-trash',
      command: () => this.cancelDirectManagement(),
    },
  ];

  description: string | null = null;
  currentIds: number[] = [];
  type: string | null = null;

  openContextMenu(event: MouseEvent, management: ReserveDTOResponse) {
    event.preventDefault();
    this.description = management ? management.description : null;
    this.type = management.streets[0].comment;
    this.currentIds = management.streets
      .map(u => u.directExecutionId ?? u.preMeasurementStreetId)
      .filter((id): id is number => id !== null && id !== undefined);

    // Abre o menu popup alinhado ao botão clicado
    this.menu?.show(event);
  }

  cancelDirectManagement() {
    this.loading = true;
    this.executionService.cancelStep(this.currentIds, this.type).subscribe({
      next: () => {
        this.reservations = this.reservations.filter(reservation =>
          reservation.description !== this.description
        );
        this.utils.showMessage(this.description + " foi excluida.", "success", "Operação realizda com sucesso");
      },
      error: (error) => {
        this.utils.showMessage(error.error.error ?? error.error.message , 'info', "Não foi possível excluir a etapa");
        this.loading = false;
      },
      complete: () => {
        this.loading = false;
      }
    });

  }


}
