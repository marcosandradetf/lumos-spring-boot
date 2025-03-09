import {Component} from '@angular/core';
import {ExecutionService} from '../execution.service';
import {KeyValuePipe, NgForOf} from '@angular/common';
import {Router} from '@angular/router';
import {PreMeasurementService} from '../pre-measurement-pending/premeasurement-service.service';
import {UtilsService} from '../../core/service/utils.service';

@Component({
  selector: 'app-measurement-list',
  standalone: true,
  imports: [
    NgForOf,
  ],
  templateUrl: './measurement-list.component.html',
  styleUrl: './measurement-list.component.scss'
})
export class MeasurementListComponent {
  preMeasurements: {
    preMeasurementId: number;
    city: string;
    createdBy: string;
    createdAt: string;
    preMeasurementType: string;
    preMeasurementStyle: string;
    teamName: string;

    streets: {
      preMeasurementStreetId: number;
      lastPower: string;
      latitude: number;
      longitude: number;
      address: string;

      items: {
        preMeasurementStreetItemId: number;
        materialId: number;
        materialName: string;
        materialType: string;
        materialPower: string;
        materialLength: string;
        materialQuantity: number;
      }[]

    }[];

  }[] = [];

  constructor(private preMeasurementService: PreMeasurementService, public utils: UtilsService, protected router: Router) {
    preMeasurementService.getPreMeasurements('validating').subscribe(preMeasurements => {
      this.preMeasurements = preMeasurements;
    });
  }

  getItemsQuantity(preMeasurementId: number) {
    let quantity: number = 0;
    this.preMeasurements.find(p => p.preMeasurementId === preMeasurementId)
      ?.streets.forEach((street) => {
      quantity += street.items.length;
    });

    return quantity;
  }
}
