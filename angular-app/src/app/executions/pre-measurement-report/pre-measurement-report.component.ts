import {Component} from '@angular/core';

import {NgForOf, NgIf} from '@angular/common';
import {PreMeasurementService} from '../pre-measurement-pending/premeasurement-service.service';
import {ActivatedRoute, Router} from '@angular/router';
import {UtilsService} from '../../core/service/utils.service';
import {ModalComponent} from '../../shared/components/modal/modal.component';
import {FormsModule} from '@angular/forms';
import {UserService} from '../../manage/user/user-service.service';
import {Title} from '@angular/platform-browser';

@Component({
  selector: 'app-pre-measurement-pending-report',
  standalone: true,
  imports: [
    NgForOf,
    NgIf,
    ModalComponent,
    FormsModule
  ],
  templateUrl: './pre-measurement-report.component.html',
  styleUrl: './pre-measurement-report.component.scss'
})
export class PreMeasurementReportComponent {
  preMeasurement: {
    preMeasurementId: number;
    contractId: number;
    city: string;
    createdBy: string;
    createdAt: string;
    preMeasurementType: string;
    preMeasurementStyle: string;
    teamName: string;
    totalPrice: string;

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

  } = {
    preMeasurementId: 0,
    contractId: 0,
    city: '',
    createdBy: '',
    createdAt: '',
    preMeasurementType: '',
    preMeasurementStyle: '',
    teamName: '',
    totalPrice: '',
    streets: []
  };

  contract : any;
  openModal: boolean = false;

  constructor(protected router: Router,  protected utils: UtilsService, private titleService: Title,
              private preMeasurementService: PreMeasurementService, private route: ActivatedRoute,) {

    const measurementId = this.route.snapshot.paramMap.get('id');
    this.titleService.setTitle("Relatório de Pré-medição");

    if (measurementId) {
      this.preMeasurementService.getPreMeasurement(measurementId).subscribe(preMeasurement => {
        this.preMeasurement = preMeasurement;
        this.preMeasurementService.getContract(preMeasurement.contractId).subscribe(contract => {
          this.contract = contract;
        })
      });
    }
  }

  generatePDF(content: HTMLDivElement): void {

  }

  getItem(attributeName: string, street: {
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
  }) {
    return street.items.find(item =>
      (item.materialLength?.toLowerCase().startsWith(attributeName) ||
        item.materialPower?.toLowerCase().startsWith(attributeName) ||
        item.materialType?.toLowerCase().startsWith(attributeName))
    )?.materialQuantity || 0;
  }

  condition(attributeName: string): boolean {
    return this.preMeasurement.streets.some(street =>
      street.items.some(item =>
        item.materialLength?.toLowerCase().startsWith(attributeName) ||
        item.materialPower?.toLowerCase().startsWith(attributeName) ||
        item.materialType?.toLowerCase().startsWith(attributeName)
      )
    );
  }


  getTotalQuantity(attributeName: string) {
    let quantity = 0;
    this.preMeasurement.streets.forEach((street) => {
      street.items.forEach((item) => {
        if (item.materialLength?.toLowerCase().startsWith(attributeName) ||
          item.materialPower?.toLowerCase().startsWith(attributeName) ||
          item.materialType?.toLowerCase().startsWith(attributeName)) {
          quantity += item.materialQuantity;
        }
      })
    })
    return quantity;
  }
}
