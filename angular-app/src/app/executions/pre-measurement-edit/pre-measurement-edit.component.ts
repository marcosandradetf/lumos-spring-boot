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
  alert: boolean = false;
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
    if (itemIndex === -1) return

    let relayIndex = 0;
    if (this.streetItems[itemIndex].materialType.toUpperCase() === 'LED') relayIndex = this.streetItems.findIndex(i => i.materialType.toUpperCase() === 'RELÉ');
    let cableIndex = 0;
    if (this.streetItems[itemIndex].materialType.toUpperCase() === 'BRAÇO') cableIndex = this.streetItems.findIndex(i => i.materialType.toUpperCase() === 'CABO');
    let relayQuantity = 0.0;
    let cableQuantity = 0.0;
    let ledQuantity = 0.0;
    let armQuantity = 0.0;


    let item = this.streetItems[itemIndex];
    let canceled = this.cancelledItems.some(ci => ci.itemId == id);
    let message = '';
    switch (canceled) {
      case false:
        this.streetItems[itemIndex].status = "CANCELLED";
        if (relayIndex > 0) {
          ledQuantity = this.streetItems.filter(si => si.materialType?.toUpperCase() === "LED" && si.status !== "CANCELLED").length;
          if (ledQuantity > 1) {
            this.streetItems[relayIndex].materialQuantity -= this.streetItems[itemIndex].materialQuantity;
            this.streetItems[relayIndex].status = "EDITED";
            message = `ITEM ${item.materialName} CANCELADO E ITEM ${this.streetItems[relayIndex].materialName} ALTERADO`;
          } else {
            this.streetItems[relayIndex].status = "CANCELLED";
            message = `ITENS ${item.materialName} E ${this.streetItems[relayIndex].materialName} CANCELADOS`;
          }
        } else if (cableIndex > 0) {
          armQuantity = this.streetItems.filter(si => si.materialType?.toUpperCase() === "BRAÇO" && si.status !== "CANCELLED").length;
          if (armQuantity > 1) {
            if (this.streetItems[itemIndex].materialLength.startsWith('1')) {
              cableQuantity = 2.5;
            } else if (this.streetItems[itemIndex].materialLength.startsWith('2')) {
              cableQuantity = 8.5;
            } else if (this.streetItems[itemIndex].materialLength.startsWith('3')) {
              cableQuantity = 12.5;
            }
            this.streetItems[cableIndex].materialQuantity -= cableQuantity;
            this.streetItems[cableIndex].status = "EDITED";
            message = `ITEM ${item.materialName} CANCELADO E ITEM ${this.streetItems[cableIndex].materialName} ALTERADO`;
          } else {
            this.streetItems[cableIndex].status = "CANCELLED";
            message = `ITENS ${item.materialName} E ${this.streetItems[cableIndex].materialName} CANCELADOS`;
          }
        } else {
          message = "ITEM " + item.materialName + " " + (item.materialLength ? item.materialLength : '') + (item.materialPower ? item.materialPower : '') + " CANCELADO";
        }

        break;
      case true:
        this.streetItems[itemIndex].status = "PENDING";
        if (relayIndex > 0) {
          ledQuantity = this.streetItems.filter(si => si.materialType?.toUpperCase() === "LED" && si.status === "CANCELLED").length;
          if (ledQuantity > 1) {
            this.streetItems[relayIndex].materialQuantity += this.streetItems[itemIndex].materialQuantity;
            this.streetItems[relayIndex].status = "EDITED";
            message = `ITEM ${item.materialName} ATIVADO E ITEM ${this.streetItems[relayIndex].materialName} ALTERADO`;
          } else {
            this.streetItems[relayIndex].status = "PENDING";
            message = `ITENS ${item.materialName} E ${this.streetItems[relayIndex].materialName} ATIVADOS`;
          }
        } else if (cableIndex > 0) {
          armQuantity = this.streetItems.filter(si => si.materialType?.toUpperCase() === "BRAÇO" && si.status === "CANCELLED").length;
          if (armQuantity > 1) {
            if (this.streetItems[itemIndex].materialLength.startsWith('1')) {
              cableQuantity = 2.5;
            } else if (this.streetItems[itemIndex].materialLength.startsWith('2')) {
              cableQuantity = 8.5;
            } else if (this.streetItems[itemIndex].materialLength.startsWith('3')) {
              cableQuantity = 12.5;
            }
            this.streetItems[cableIndex].materialQuantity += cableQuantity;
            this.streetItems[cableIndex].status = "EDITED";
            message = `ITEM ${item.materialName} ATIVADO E ITEM ${this.streetItems[cableIndex].materialName} ALTERADO`;
          } else {
            this.streetItems[cableIndex].status = "PENDING";
            message = `ITENS ${item.materialName} E ${this.streetItems[cableIndex].materialName} ATIVADOS`;
          }
        } else {
          message = "ITEM " + item.materialName + " " + (item.materialLength ? item.materialLength : '') + (item.materialPower ? item.materialPower : '') + " ATIVADO";
        }
        break;
    }

    // adicao items
    if(item.status === "CANCELLED") {
      this.cancelledItems.push({itemId: id, streetId: this.streetId});
    } else {
      this.cancelledItems = this.cancelledItems.filter(ci => ci.itemId !== id);
    }

    // adicao itens dependentes
    if (relayIndex > 0) {
      if (this.streetItems[relayIndex].status === "EDITED") {
        this.cancelledItems = this.cancelledItems.filter(ci => ci.itemId !== this.streetItems[relayIndex].preMeasurementStreetItemId);

        this.changedItems = this.changedItems.filter(ci => ci.itemId !== this.streetItems[relayIndex].preMeasurementStreetItemId)
        this.changedItems.push({
          itemId: this.streetItems[relayIndex].preMeasurementStreetItemId,
          streetId: this.streetId,
          quantity: this.streetItems[relayIndex].materialQuantity
        });
      } else {
        this.cancelledItems = this.cancelledItems.filter(ci => ci.itemId !== this.streetItems[relayIndex].preMeasurementStreetItemId);
        this.changedItems = this.changedItems.filter(ci => ci.itemId !== this.streetItems[relayIndex].preMeasurementStreetItemId);
        this.cancelledItems.push({itemId: this.streetItems[relayIndex].preMeasurementStreetItemId, streetId: this.streetId});
      }
    } else if (cableIndex > 0) {
      if (this.streetItems[cableIndex].status === "EDITED") {
        this.cancelledItems = this.cancelledItems.filter(ci => ci.itemId !== this.streetItems[cableIndex].preMeasurementStreetItemId);

        this.changedItems = this.changedItems.filter(ci => ci.itemId !== this.streetItems[cableIndex].preMeasurementStreetItemId)
        this.changedItems.push({
          itemId: this.streetItems[cableIndex].preMeasurementStreetItemId,
          streetId: this.streetId,
          quantity: this.streetItems[cableIndex].materialQuantity
        });

      } else {
        this.cancelledItems = this.cancelledItems.filter(ci => ci.itemId !== this.streetItems[cableIndex].preMeasurementStreetItemId);
        this.changedItems = this.changedItems.filter(ci => ci.itemId !== this.streetItems[cableIndex].preMeasurementStreetItemId);
        this.cancelledItems.push({itemId: this.streetItems[cableIndex].preMeasurementStreetItemId, streetId: this.streetId});
      }
    }
    console.log(this.cancelledStreets);
    console.log(this.cancelledItems);
    console.log(this.changedItems);
    this.utils.showMessage(message, false);
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

        // remove dos cancelados e modificados
        this.cancelledItems = this.cancelledItems.filter(ci => ci.streetId !== id);
        this.changedItems = this.changedItems.filter(ci => ci.streetId !== id);
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
    if (this.cancelledItems.length === 0 && this.cancelledStreets.length === 0 && this.changedItems.length === 0) {
      this.utils.showMessage("Não é permitido salvar uma edição com nenhuma rua/item modificado", true);
    }
    this.finish = true;
  }

  changeValue(preMeasurementStreetItemId: number, action: 'increment' | 'decrement') {
    let index = this.streetItems.findIndex(i => i.preMeasurementStreetItemId === preMeasurementStreetItemId);
    if (index === -1) return
    this.streetItems[index].status = 'EDITED';
    let relayIndex = 0;
    if (this.streetItems[index].materialType.toUpperCase() === 'LED') relayIndex = this.streetItems.findIndex(i => i.materialType.toUpperCase() === 'RELÉ');
    let cableIndex = 0;
    if (this.streetItems[index].materialType.toUpperCase() === 'BRAÇO') cableIndex = this.streetItems.findIndex(i => i.materialType.toUpperCase() === 'CABO');
    let cableQuantity = 0.0;

    if (action === 'increment') {
      this.streetItems[index].materialQuantity += 1;

      if (relayIndex > 0) {
        this.streetItems[relayIndex].materialQuantity += 1;
        this.streetItems[relayIndex].status = 'EDITED';
        this.changedItems.push({
          itemId: this.streetItems[relayIndex].preMeasurementStreetItemId,
          streetId: this.streetId,
          quantity: this.streetItems[relayIndex].materialQuantity
        });
      }

      if (cableIndex > 0) {
        if (this.streetItems[index].materialLength.startsWith('1')) {
          cableQuantity = 2.5;
        } else if (this.streetItems[index].materialLength.startsWith('2')) {
          cableQuantity = 8.5;
        } else if (this.streetItems[index].materialLength.startsWith('3')) {
          cableQuantity = 12.5;
        }
        this.streetItems[cableIndex].materialQuantity += cableQuantity;
        this.streetItems[cableIndex].status = 'EDITED';
      }

    } else if (action === 'decrement') {
      if (this.streetItems[index].materialQuantity == 0) return
      this.streetItems[index].materialQuantity -= 1;
      this.streetItems[index].status = 'EDITED';

      if (relayIndex > 0) {
        // adicionar validacao de rua
        this.streetItems[relayIndex].materialQuantity -= 1;
        this.streetItems[relayIndex].status = 'EDITED';
      }

      if (cableIndex > 0) {
        if (this.streetItems[index].materialLength.startsWith('1')) {
          cableQuantity = 2.5;
        } else if (this.streetItems[index].materialLength.startsWith('2')) {
          cableQuantity = 8.5;
        } else if (this.streetItems[index].materialLength.startsWith('3')) {
          cableQuantity = 12.5;
        }
        this.streetItems[cableIndex].materialQuantity -= cableQuantity;
        this.streetItems[cableIndex].status = 'EDITED';
      }

    }

    // adicao itens primarios
    this.changedItems = this.changedItems.filter(ci => ci.itemId !== preMeasurementStreetItemId)
    this.changedItems.push({
      itemId: preMeasurementStreetItemId,
      streetId: this.streetId,
      quantity: this.streetItems[index].materialQuantity
    });

    // adicao itens dependentes
    if (cableIndex > 0) {
      this.changedItems = this.changedItems.filter(ci => ci.itemId !== this.streetItems[cableIndex].preMeasurementStreetItemId)
      this.changedItems.push({
        itemId: this.streetItems[cableIndex].preMeasurementStreetItemId,
        streetId: this.streetId,
        quantity: this.streetItems[cableIndex].materialQuantity
      });
    } else if (relayIndex) {
      this.changedItems = this.changedItems.filter(ci => ci.itemId !== this.streetItems[relayIndex].preMeasurementStreetItemId)
      this.changedItems.push({
        itemId: this.streetItems[relayIndex].preMeasurementStreetItemId,
        streetId: this.streetId,
        quantity: this.streetItems[relayIndex].materialQuantity
      });
    }

    console.log(this.changedItems);
  }
}
