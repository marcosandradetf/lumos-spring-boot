import {Component} from '@angular/core';
import {Router} from '@angular/router';
import {ReserveDTOResponse} from '../../executions/executions.model';
import {BreadcrumbComponent} from '../../shared/components/breadcrumb/breadcrumb.component';
import {NgForOf, NgIf} from '@angular/common';

@Component({
  selector: 'app-reservation-management-select',
  standalone: true,
  imports: [
    BreadcrumbComponent,
    NgIf,
    NgForOf
  ],
  templateUrl: './reservation-management-select.component.html',
  styleUrl: './reservation-management-select.component.scss'
})
export class ReservationManagementSelectComponent {
  reserve: ReserveDTOResponse = {
    description: '',
    streets: []
  };
  streetId: number = 0;

  constructor(private router: Router) {
    const navigation = this.router.getCurrentNavigation();
    const state = navigation?.extras.state as { reserve: ReserveDTOResponse };

    if (state?.reserve) {
      this.reserve = state.reserve;
    } else {
      void this.router.navigate(['/requisicoes/execucoes/reservas/gerenciamento']);
    }
  }

  scrollLeft(slider: HTMLElement) {
    slider.scrollBy({left: -300, behavior: 'smooth'});
  }

  scrollRight(slider: HTMLElement) {
    slider.scrollBy({left: 300, behavior: 'smooth'});
  }

}
