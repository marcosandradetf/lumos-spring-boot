import { Component } from '@angular/core';
import {SidebarComponent} from '../../../../shared/components/sidebar/sidebar.component';
import {ContractService} from '../../services/contract.service';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [
    SidebarComponent
  ],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss'
})
export class DashboardComponent {

  constructor(protected contractService: ContractService) {
  }


}
