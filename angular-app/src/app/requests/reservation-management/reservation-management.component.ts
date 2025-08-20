import {Component, ViewChild} from '@angular/core';
import {Title} from '@angular/platform-browser';
import {ExecutionService} from '../../executions/execution.service';
import {AuthService} from '../../core/auth/auth.service';
import {GetObjectRequest, SetObjectRequest, UtilsService} from '../../core/service/utils.service';
import {ReserveDTOResponse} from '../../executions/executions.model';
import {NgForOf, NgIf} from '@angular/common';
import {LoadingComponent} from '../../shared/components/loading/loading.component';
import {Router} from '@angular/router';
import {Menu} from 'primeng/menu';
import {MenuItem} from 'primeng/api';

@Component({
  selector: 'app-reservation-management',
  standalone: true,
  imports: [
    NgIf,
    LoadingComponent,
    NgForOf,
    Menu
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
      label: 'Cancelar e excluir solicitação',
      icon: 'pi pi-close',
      command: () => this.cancelDirectManagement(),
    },
  ];

  currentId: number | null = null;
  type: string | null = null;

  openContextMenu(event: MouseEvent, management: any) {
    event.preventDefault();
    if (management.directExecutionId !== null) {
      this.type = 'directExecution';
      this.currentId = management.directExecutionId;
      console.log(management);
    } else {
      this.type = 'indirectExecution';
      this.currentId = management.preMeasurementStreetId;
    }

    // Abre o menu popup alinhado ao botão clicado
    this.menu?.show(event);
  }

  cancelDirectManagement() {
    const command: SetObjectRequest = {
      command: "delete",
      tables: ['direct_execution_item', 'direct_execution', 'reservation_management'],
      where: 'direct_execution_id',
      equal: 118
    }

    this.utils.setObject(command).subscribe({
      next: () => {
        this.reservations = this.reservations.filter(r =>
          r.streets.some(s => s.directExecutionId === this.currentId)
        );
      }, error: (error) => {
        this.utils.showMessage(error.error.message, 'error');
        this.loading = false
      },
      complete: () => {
        this.loading = false;
      }
    });

  }


}
