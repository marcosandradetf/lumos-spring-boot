import { Component } from '@angular/core';
import {SidebarComponent} from '../../shared/components/sidebar/sidebar.component';
import {Router, RouterLinkActive} from '@angular/router';
import {PreMeasurementService} from './premeasurement-service.service';

@Component({
  selector: 'app-pre-measurement',
  standalone: true,
  imports: [
    SidebarComponent,
    RouterLinkActive
  ],
  templateUrl: './pre-measurement.component.html',
  styleUrl: './pre-measurement.component.scss'
})
export class PreMeasurementComponent {
  sidebarLinks: { title: string; path: string; id: string }[] = [

  ];

  constructor(private preMeasurementService: PreMeasurementService,
              public router: Router,) {}

}
