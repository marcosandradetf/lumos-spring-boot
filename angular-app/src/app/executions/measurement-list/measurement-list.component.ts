import {Component} from '@angular/core';
import {ExecutionService} from '../execution.service';
import {KeyValuePipe, NgForOf} from '@angular/common';

@Component({
  selector: 'app-measurement-list',
  standalone: true,
  imports: [
    NgForOf,
    KeyValuePipe
  ],
  templateUrl: './measurement-list.component.html',
  styleUrl: './measurement-list.component.scss'
})
export class MeasurementListComponent {


  groupedMeasurements: { [p: string]: any[] } = {};

  measurementDTO: {
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
  }[] = []

  constructor(private executionService: ExecutionService,) {
    this.executionService.getMeasurements().subscribe(
      d => {
        this.measurementDTO = d;
        this.groupedMeasurements = this.groupMeasurementsByCity(this.measurementDTO);
      }
    );
  }

  groupMeasurementsByCity(measurements: any[]): { [city: string]: any[] } {
    return measurements.reduce((acc, measurement) => {
      const city = measurement.measurement.city;
      if (!acc[city]) {
        acc[city] = [];
      }
      acc[city].push(measurement);
      return acc;
    }, {} as { [city: string]: any[] });
  }

  getType(measurementType: string) {
    switch (measurementType) {
      case "INSTALLATION":
        return "Instalação";
      case "MAINTENANCE":
        return "Manutenção";
      default:
        return "Tipo não encontrado"
    }
  }

  getStreet(address: string) {
    let street = address.split(',');

    return street[0];
  }
}
