import {Component} from '@angular/core';

import {CurrencyPipe, NgForOf, NgIf} from '@angular/common';
import {PreMeasurementService} from '../pre-measurement-pending/premeasurement-service.service';
import {ActivatedRoute, Router} from '@angular/router';
import {UtilsService} from '../../core/service/utils.service';
import {ModalComponent} from '../../shared/components/modal/modal.component';
import {FormsModule} from '@angular/forms';
import {Title} from '@angular/platform-browser';
import {UserService} from '../../manage/user/user-service.service';
import {AuthService} from '../../core/auth/auth.service';

@Component({
  selector: 'app-pre-measurement-pending-report',
  standalone: true,
  imports: [
    NgForOf,
    NgIf,
    ModalComponent,
    FormsModule,
    CurrencyPipe
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
      number: number;
      preMeasurementStreetId: number;
      lastPower: string;
      latitude: number;
      longitude: number;
      address: string;

      items: {
        preMeasurementStreetItemId: number;
        materialId: number;
        contractItemId: number;
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

  contract: {
    contractId: number,
    contractNumber: string,
    contractor: string,
    cnpj: string,
    phone: string,
    address: string,
    contractFile: string,
    createdBy: string,
    createdAt: string,
    items: {
      number: number;
      contractItemId: number;
      description: string;
      unitPrice: string;
      contractedQuantity: number;
      linking: string;
    }[],
  } = {
    contractId: 0,
    contractNumber: "",
    contractor: "",
    cnpj: "",
    phone: "",
    address: "",
    contractFile: "",
    createdBy: '',
    createdAt: '',
    items: []
  };
  openModal: boolean = false;
  loading: boolean = true;

  user: {
    username: string,
    name: string,
    lastname: string,
    email: string,
    role: string[],
    status: boolean
  } = {
    username: '',
    name: '',
    lastname: '',
    email: '',
    role: [],
    status: false,
  };

  constructor(protected router: Router, protected utils: UtilsService, private titleService: Title,
              private preMeasurementService: PreMeasurementService, private route: ActivatedRoute, private authService: AuthService, private userService: UserService) {

    const measurementId = this.route.snapshot.paramMap.get('id');
    this.titleService.setTitle("Relatório de Pré-medição");

    const uuid = authService.getUser().uuid;

    if (measurementId) {
      this.preMeasurementService.getPreMeasurement(measurementId).subscribe(preMeasurement => {
        this.preMeasurement = preMeasurement;
        this.preMeasurementService.getContract(preMeasurement.contractId).subscribe(contract => {
          this.contract = contract;
          this.userService.getUser(uuid).subscribe(
            user => {
              this.user = user;
              this.loading = false
            });
        });
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
      (item.materialLength?.toLowerCase() === attributeName?.toLowerCase() ||
        item.materialPower?.toLowerCase() === attributeName?.toLowerCase() ||
        item.materialType?.toLowerCase() === attributeName?.toLowerCase())
    )?.materialQuantity || 0;
  }

  condition(attributeName: string): boolean {
    return this.preMeasurement.streets.some(street =>
      street.items.some(item =>
        item.materialLength?.toLowerCase() === attributeName?.toLowerCase() ||
        item.materialPower?.toLowerCase() === attributeName?.toLowerCase() ||
        item.materialType?.toLowerCase() === attributeName?.toLowerCase()
      )
    );
  }


  getTotalQuantity(attributeName: string) {
    let quantity = 0;
    this.preMeasurement.streets.forEach((street) => {
      street.items.forEach((item) => {
        if (item.materialLength?.toLowerCase() === attributeName?.toLowerCase() ||
          item.materialPower?.toLowerCase() === attributeName?.toLowerCase() ||
          item.materialType?.toLowerCase() === attributeName?.toLowerCase()) {
          quantity += item.materialQuantity;
        }
      })
    })
    return quantity;
  }

  getTotalPreMeasured(contractItemId: number, linking: string, description: string) {
    let quantity = 0;

    if (description.startsWith("SERVIÇO")) {
      switch (description) {
        case "SERVIÇO DE INSTALAÇÃO DE LUMINÁRIA EM LED":
        case "SERVIÇO DE EXECUÇÃO DE PROJETO POR IP":
          this.preMeasurement.streets.forEach((street) => {
            street.items.forEach((item) => {
              if (item.materialType?.toUpperCase() === "LED") {
                quantity += item.materialQuantity;
              }
            });
          });
          break;
        case "SERVIÇO DE RECOLOCAÇÃO DE BRAÇOS":
          this.preMeasurement.streets.forEach((street) => {
            street.items.forEach((item) => {
              if (item.materialType?.toUpperCase() === "BRAÇO") {
                quantity += item.materialQuantity;
              }
            });
          });
          break;

        case "SERVIÇO DE MANUTENÇÃO MENSAL POR PONTO DE ILUMINAÇÃO PÚBLICA - LED":
          break;

        case "SERVIÇO DE MANUTENÇÃO MENSAL POR PONTO DE ILUMINAÇÃO PÚBLICA - LUMINÁRIA CONVENCIONAL":
          break;
      }
    } else if (linking === null) {
      this.preMeasurement.streets.forEach((street) => {
        street.items.forEach((item) => {
          if (item.contractItemId === contractItemId) {
            quantity += item.materialQuantity;
          }
        });
      });
    } else {
      this.preMeasurement.streets.forEach((street) => {
        street.items.forEach((item) => {
          if (item.materialLength?.toUpperCase() === linking?.toUpperCase() ||
            item.materialPower?.toUpperCase() === linking?.toUpperCase()
          ) {
            quantity += item.materialQuantity;
          }
        });
      });
    }
    return quantity;
  }

  getTotalPrice(contractItemId: number, linking: string, description: string, unitPrice: string) {
    let price = 0.00;

    if (description.startsWith("SERVIÇO")) {
      switch (description) {
        case "SERVIÇO DE INSTALAÇÃO DE LUMINÁRIA EM LED":
        case "SERVIÇO DE EXECUÇÃO DE PROJETO POR IP":
          this.preMeasurement.streets.forEach((street) => {
            street.items.forEach((item) => {
              if (item.materialType?.toUpperCase() === "LED") {
                price += item.materialQuantity * parseFloat(unitPrice);
              }
            });
          });
          break;
        case "SERVIÇO DE RECOLOCAÇÃO DE BRAÇOS":
          this.preMeasurement.streets.forEach((street) => {
            street.items.forEach((item) => {
              if (item.materialType?.toUpperCase() === "BRAÇO") {
                price += item.materialQuantity * parseFloat(unitPrice);
              }
            });
          });
          break;

        case "SERVIÇO DE MANUTENÇÃO MENSAL POR PONTO DE ILUMINAÇÃO PÚBLICA - LED":
          break;

        case "SERVIÇO DE MANUTENÇÃO MENSAL POR PONTO DE ILUMINAÇÃO PÚBLICA - LUMINÁRIA CONVENCIONAL":
          break;
      }
    } else if (linking === null) {
      this.preMeasurement.streets.forEach((street) => {
        street.items.forEach((item) => {
          if (item.contractItemId === contractItemId) {
            price += item.materialQuantity * parseFloat(unitPrice);
          }
        });
      });
    } else {
      this.preMeasurement.streets.forEach((street) => {
        street.items.forEach((item) => {
          if (item.materialLength?.toUpperCase() === linking?.toUpperCase() ||
            item.materialPower?.toUpperCase() === linking?.toUpperCase()
          ) {
            price += item.materialQuantity * parseFloat(unitPrice);
          }
        });
      });
    }
    return price;
  }

  protected readonly parseFloat = parseFloat;
}
