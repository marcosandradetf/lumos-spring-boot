import {Component} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';
import {UtilsService} from '../../core/service/utils.service';
import {Title} from '@angular/platform-browser';
import {PreMeasurementService} from '../pre-measurement-home/premeasurement-service.service';
import {AuthService} from '../../core/auth/auth.service';
import {UserService} from '../../manage/user/user-service.service';
import {ReportService} from '../../core/service/report-service';
import {ScreenMessageComponent} from '../../shared/components/screen-message/screen-message.component';
import {NgClass, NgForOf, NgIf} from '@angular/common';
import {ModalComponent} from '../../shared/components/modal/modal.component';

@Component({
  selector: 'app-pre-measurement-edit',
  standalone: true,
  imports: [
    ScreenMessageComponent,
    NgForOf,
    NgIf,
    ModalComponent,
    NgClass
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
        status: string;
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

  edit(entity: string, id: number, divElementOrigin: HTMLDivElement, divElementDest: HTMLDivElement) {
    switch (entity) {
      case 'street':
        this.streetId = id;
        this.itemId = 0;
        const items = this.preMeasurement.streets.find(s => s.preMeasurementStreetId == id)?.items;
        if (items) this.streetItems = items
        if (this.isNotActive(entity, id)) {
          this.openModal = true;
        } else {
          this.changeFromTo(divElementOrigin, divElementDest);
        }
        break;
      case 'item':
        this.itemId = id;
        this.cancelItem(id);
        break;
    }
  }

  isNotActive(entity: string, id: number): boolean {
    switch (entity) {
      case "street":
        return this.preMeasurement.streets.find(s => s.preMeasurementStreetId === id)?.status === "CANCELLED";
      case "item":
        return this.streetItems.find(i => i.preMeasurementStreetItemId == id)?.status === "CANCELLED";
      default:
        return true;
    }
  }

  changeFromTo(divElementOrigin: HTMLDivElement, divElementDest: HTMLDivElement) {
    divElementOrigin.classList.add("hidden");
    divElementDest.classList.remove("hidden");
    this.openModal = false;
  }

  cancelledStreets: {
    streetId: number;
  }[] = [];
  cancelledItems: {
    streetId: number;
    itemId: number;
  }[] = [];
  changedItems: {
    streetId: number;
    itemId: number;
    quantity: number;
  }[] = [];

  cancelItem(id: number) {
    let itemIndex = this.streetItems.findIndex(i => i.preMeasurementStreetItemId == id);
    if (itemIndex === -1) {
      return
    }
    let item = this.streetItems[itemIndex];
    let canceled = this.cancelledItems.some(ci => ci.itemId == id);
    switch (canceled) {
      case false:
        item.status = "CANCELLED";
        this.streetItems[itemIndex] = item;
        this.cancelledItems.push({itemId: id, streetId: this.streetId});
        this.utils.showMessage("ITEM " + item.materialName + " " + (item.materialLength ? item.materialLength : '') + (item.materialPower ? item.materialPower : '') + " CANCELADO", false);
        console.log(this.cancelledStreets)
        console.log(this.cancelledItems)
        break;
      case true:
        item.status = "PENDING";
        this.streetItems[itemIndex] = item;
        this.cancelledItems = this.cancelledItems.filter(ci => ci.itemId !== id);
        this.utils.showMessage("ITEM " + item.materialName + " " + (item.materialLength ? item.materialLength : '') + (item.materialPower ? item.materialPower : '') + " ATIVADO", false);
        console.log(this.cancelledStreets)
        console.log(this.cancelledItems)
        break;
    }
  }

  cancelStreet(id: number, cancel: boolean) {
    let streetIndex = this.preMeasurement.streets.findIndex(s => s.preMeasurementStreetId == id);
    if (streetIndex === -1) {
      return
    }
    let street = this.preMeasurement.streets[streetIndex];
    switch (cancel) {
      case true:
        street.status = "CANCELLED";
        street.items.forEach((item) => {
          if (item) item.status = "CANCELLED";
        });
        this.preMeasurement.streets[streetIndex] = street;
        this.cancelledStreets.push({streetId: id});
        this.openModal = false;
        this.utils.showMessage("Todos os itens da rua " + street.address + " foram cancelados", false);
        break;
      case false:
        street.status = "PENDING";
        street.items.forEach((item) => {
          if (item) item.status = "PENDING";
        });
        this.preMeasurement.streets[streetIndex] = street;
        this.cancelledStreets = this.cancelledStreets.filter(cs => cs.streetId !== id);
        this.openModal = false;
        this.utils.showMessage("Todos os itens da rua " + street.address + " foram reativados", false);
        break;
    }
  }

  getStreetName(streetId: number) {
    return this.preMeasurement.streets.find(s => s.preMeasurementStreetId == streetId)?.address;
  }

  finish: boolean = false;

  conclude() {
    this.utils.showMessage("Não é permitido salvar uma edição com nenhuma rua/item modificado", true);
    this.finish = true;
  }

  changeValue(preMeasurementStreetItemId: number, action: 'increment' | 'decrement') {
    if (action === 'increment') {
      let index = this.streetItems.findIndex(i => i.preMeasurementStreetItemId === preMeasurementStreetItemId);
      if (index !== -1) {
        this.streetItems[index].materialQuantity += 1;
      }
    } else if (action === 'decrement') {
      let index = this.streetItems.findIndex(i => i.preMeasurementStreetItemId === preMeasurementStreetItemId);
      if (index !== -1) {
        if (this.streetItems[index].materialQuantity > 0) this.streetItems[index].materialQuantity -= 1;
        this.changedItems.push({
          itemId: this.streetItems[index].preMeasurementStreetItemId,
          streetId: this.streetId,
          quantity: this.streetItems[index].materialQuantity
        });

        if (this.streetItems[index].materialType.toUpperCase() === 'LED') {
          let relayIndex = this.streetItems.findIndex(i => i.materialType.toUpperCase() === 'RELÉ');
          // adicionar validacao de rua
          this.streetItems[relayIndex].materialQuantity -= 1;
          this.changedItems.push({
            itemId: this.streetItems[relayIndex].preMeasurementStreetItemId,
            streetId: this.streetId,
            quantity: this.streetItems[relayIndex].materialQuantity
          });
        }

        if (this.streetItems[index].materialType.toUpperCase() === 'BRAÇO') {
          // implementar para cabos
        }

      }
    }
  }
}
