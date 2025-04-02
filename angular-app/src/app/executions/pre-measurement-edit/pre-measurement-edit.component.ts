import {Component} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';
import {UtilsService} from '../../core/service/utils.service';
import {Title} from '@angular/platform-browser';
import {PreMeasurementService} from '../pre-measurement-home/premeasurement-service.service';
import {AuthService} from '../../core/auth/auth.service';
import {UserService} from '../../manage/user/user-service.service';
import {ReportService} from '../../core/service/report-service';
import {ScreenMessageComponent} from '../../shared/components/screen-message/screen-message.component';
import {NgForOf, NgIf} from '@angular/common';
import {ModalComponent} from '../../shared/components/modal/modal.component';

@Component({
  selector: 'app-pre-measurement-edit',
  standalone: true,
  imports: [
    ScreenMessageComponent,
    NgForOf,
    NgIf,
    ModalComponent
  ],
  templateUrl: './pre-measurement-edit.component.html',
  styleUrl: './pre-measurement-edit.component.scss'
})
export class PreMeasurementEditComponent {
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
    status: string;

    streets: {
      number: number;
      preMeasurementStreetId: number;
      lastPower: string;
      latitude: number;
      longitude: number;
      address: string;
      status: string;

      items: {
        preMeasurementStreetItemId: number;
        materialId: number;
        contractItemId: number;
        materialName: string;
        materialType: string;
        materialPower: string;
        materialLength: string;
        materialQuantity: number;
        status: string
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
    status: '',
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

  streetItems: {
    preMeasurementStreetItemId: number;
    materialId: number;
    contractItemId: number;
    materialName: string;
    materialType: string;
    materialPower: string;
    materialLength: string;
    materialQuantity: number;
    status: string
  }[] = [];

  constructor(protected router: Router, protected utils: UtilsService, private titleService: Title,
              private preMeasurementService: PreMeasurementService, private route: ActivatedRoute, authService: AuthService,
              private userService: UserService, private reportService: ReportService) {

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

  streetId: number = 0;
  itemId: number = 0;

  edit(entity: string, id: number) {
    switch (entity) {
      case 'street':
        this.streetId = id;
        this.itemId = 0;
        const items = this.preMeasurement.streets.find(s => s.preMeasurementStreetId == id)?.items;
        if (items) this.streetItems = items
        this.openModal = true;
        break;
      case 'item':
        this.streetId = 0;
        this.itemId = id;
        this.openModal = true;
        break;
    }
  }

  isNotActive(entity: string, id: number): boolean {
    switch (entity) {
      case "street":
        return this.preMeasurement.streets.find(s => s.preMeasurementStreetId === id)?.status !== "CANCELLED";
      case "item":
        return this.streetItems.find(i => i.preMeasurementStreetItemId == id)?.status !== "CANCELLED";
      default:
        return true;
    }
  }

  changeFromTo(divElementOrigin: HTMLDivElement, divElementDest: HTMLDivElement) {
    divElementOrigin.classList.add("hidden");
    divElementDest.classList.remove("hidden");
  }

  cancelledStreets: {
    streetId: number;
  }[] = [];
  cancelledItems: {
    streetId: number;
    itemId: number;
  }[] = [];

  cancelStreet(id: number, cancel: boolean) {
    let index = this.preMeasurement.streets.findIndex(s => s.preMeasurementStreetId == id);
    if (index === -1) {
      return
    }
    let street = this.preMeasurement.streets[index];
    switch (cancel) {
      case true:
        street.status = "CANCELLED";
        street.items.forEach((item) => {
          if (item) item.status = "CANCELLED";
        });
        this.preMeasurement.streets[index] = street;
        this.cancelledStreets.push({streetId: id});
        this.utils.showMessage("Todos os itens da rua foram cancelados", false);
        break;
      case false:
        street.status = "PENDING";
        street.items.forEach((item) => {
          if (item) item.status = "PENDING";
        });
        this.preMeasurement.streets[index] = street;
        index = this.cancelledStreets.findIndex(s => s.streetId == id);
        // remover continua...
        this.utils.showMessage("Todos os itens da rua foram reativados", false);
        break;
    }
  }
}
