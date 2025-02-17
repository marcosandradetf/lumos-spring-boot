import {Component} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';
import {KeyValuePipe, NgForOf} from '@angular/common';

@Component({
  selector: 'app-measurement-details',
  standalone: true,
  imports: [
    KeyValuePipe,
    NgForOf
  ],
  templateUrl: './measurement-details.component.html',
  styleUrl: './measurement-details.component.scss'
})
export class MeasurementDetailsComponent {
  measurement: {
    measurement: {
      measurementId: number,
      latitude: number,
      longitude: number,
      address: string,
      city: string,
      depositId: number,
      deviceId: string,
      depositName: string,
      measurementType: string,
      measurementStyle: string,
      createdBy: string
    },
    items: {
      materialId: string,
      materialQuantity: number,
      lastPower: string,
      measurementId: number,
      material: string
    }[]
  } = {
    measurement: {
      measurementId: 0,
      latitude: 0,
      longitude: 0,
      address: '',
      city: '',
      depositId: 0,
      deviceId: '',
      depositName: '',
      measurementType: '',
      measurementStyle: '',
      createdBy: ''
    },
    items: []
  }

  constructor(private route: ActivatedRoute, private router: Router) {
    const navigation = this.router.getCurrentNavigation();
    this.measurement = navigation?.extras.state?.['measurement'];
    console.log('Measurement recebido:', this.measurement);
  }
}
