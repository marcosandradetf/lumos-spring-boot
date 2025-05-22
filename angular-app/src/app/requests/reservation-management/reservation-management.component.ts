import { Component } from '@angular/core';
import {Title} from '@angular/platform-browser';

@Component({
  selector: 'app-reservation-management',
  standalone: true,
  imports: [],
  templateUrl: './reservation-management.component.html',
  styleUrl: './reservation-management.component.scss'
})
export class ReservationManagementComponent {

  constructor(private titleService: Title) {
    this.titleService.setTitle("Gerenciamento de Reservas");
  }
}
