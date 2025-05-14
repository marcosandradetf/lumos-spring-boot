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
import {PreMeasurementResponseDTO} from '../../models/pre-measurement-response-d-t.o';
import {response} from 'express';

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
  preMeasurement: PreMeasurementResponseDTO = {
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

  preMeasurementCopy: PreMeasurementResponseDTO = {
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
        this.preMeasurementCopy = JSON.parse(JSON.stringify(preMeasurement));
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

  cancelStreet(id: number, cancel: boolean) {
    let streetIndex = this.preMeasurement.streets.findIndex(s => s.preMeasurementStreetId == id);
    if (streetIndex === -1) {
      return
    }
    let street = this.preMeasurement.streets[streetIndex];
    this.cancelledStreets = this.cancelledStreets.filter(cs => cs.streetId !== id);
    this.cancelledItems = this.cancelledItems.filter(ci => ci.streetId !== id);
    this.changedItems = this.changedItems.filter(ci => ci.streetId !== id);

    switch (cancel) {
      case true:
        street.status = "CANCELLED";
        street.items.forEach((item) => {
          if (item) item.status = "CANCELLED";
        });
        this.preMeasurement.streets[streetIndex] = street;
        this.cancelledStreets.push({streetId: id});
        this.openModal = false;
        this.utils.showMessage("Todos os itens da rua " + street.street + " foram cancelados", false);
        break;
      case false:
        street.items.forEach((item) => {
          if (item) {
            if (item.materialQuantity === this.getOriginalItem(item.preMeasurementStreetItemId)) {
              item.status = 'PENDING';
            } else {
              item.status = 'EDITED';
              this.changedItems.push({
                itemId: item.preMeasurementStreetItemId,
                streetId: this.streetId,
                quantity: item.materialQuantity
              });
            }
          }
        });
        street.status = this.streetItems.some(i => i.status === 'EDITED') ? 'EDITED' : 'PENDING';
        this.preMeasurement.streets[streetIndex] = street;
        this.openModal = false;
        this.utils.showMessage("Todos os itens da rua " + street.street + " foram reativados", false);
        break;
    }

  }

  cancelItem(id: number) {
    let itemIndex = this.streetItems.findIndex(i => i.preMeasurementStreetItemId == id);
    if (itemIndex === -1) return

    let relayIndex = 0;
    if (this.streetItems[itemIndex].materialType.toUpperCase() === 'LED') relayIndex = this.streetItems.findIndex(i => i.materialType.toUpperCase() === 'RELÉ');
    let cableIndex = 0;
    if (this.streetItems[itemIndex].materialType.toUpperCase() === 'BRAÇO') cableIndex = this.streetItems.findIndex(i => i.materialType.toUpperCase() === 'CABO');
    let cableQuantity = 0.0;
    let ledQuantity = 0.0;
    let armQuantity = 0.0;

    let item = this.streetItems[itemIndex];
    let message = '';
    switch (this.streetItems[itemIndex].status) {
      case 'PENDING':
      case 'EDITED':
        this.streetItems[itemIndex].status = "CANCELLED";
        if (relayIndex > 0) {
          ledQuantity = this.streetItems.filter(si => si.materialType?.toUpperCase() === "LED" && si.status !== "CANCELLED").length;
          if (ledQuantity > 0) {
            this.streetItems[relayIndex].materialQuantity -= this.streetItems[itemIndex].materialQuantity;
            this.streetItems[relayIndex].status = "EDITED";
            message = `ITEM ${item.materialName} CANCELADO E ITEM ${this.streetItems[relayIndex].materialName} ALTERADO`;
          } else {
            this.streetItems[relayIndex].status = "CANCELLED";
            message = `ITENS ${item.materialName} E ${this.streetItems[relayIndex].materialName} CANCELADOS`;
          }
        } else if (cableIndex > 0) {
          armQuantity = this.streetItems.filter(si => si.materialType?.toUpperCase() === "BRAÇO" && si.status !== "CANCELLED").length;
          if (armQuantity > 0) {
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
      case 'CANCELLED':
        if (this.streetItems[itemIndex].materialQuantity === this.getOriginalItem(this.streetItems[itemIndex].preMeasurementStreetItemId)) {
          this.streetItems[itemIndex].status = 'PENDING';
        } else {
          this.streetItems[itemIndex].status = 'EDITED';
        }
        if (relayIndex > 0) {
          ledQuantity = this.streetItems.filter(si => si.materialType?.toUpperCase() === "LED" && si.status === "CANCELLED").length;
          if (ledQuantity === 0) {
            this.streetItems[relayIndex].materialQuantity += this.streetItems[itemIndex].materialQuantity;
            message = `ITEM ${item.materialName} ATIVADO E ITEM ${this.streetItems[relayIndex].materialName} ALTERADO`;
          } else {
            message = `ITENS ${item.materialName} E ${this.streetItems[relayIndex].materialName} ATIVADOS`;
          }
          if (this.streetItems[relayIndex].materialQuantity === this.getOriginalItem(this.streetItems[relayIndex].preMeasurementStreetItemId)) {
            this.streetItems[relayIndex].status = 'PENDING';
          } else {
            this.streetItems[relayIndex].status = 'EDITED';
          }

        } else if (cableIndex > 0) {
          armQuantity = this.streetItems.filter(si => si.materialType?.toUpperCase() === "BRAÇO" && si.status === "CANCELLED").length;
          if (armQuantity === 0) {
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
            message = `ITENS ${item.materialName} E ${this.streetItems[cableIndex].materialName} ATIVADOS`;
          }
          if (this.streetItems[cableIndex].materialQuantity === this.getOriginalItem(this.streetItems[cableIndex].preMeasurementStreetItemId)) {
            this.streetItems[cableIndex].status = 'PENDING';
          } else {
            this.streetItems[cableIndex].status = 'EDITED';
          }
        } else {
          message = "ITEM " + item.materialName + " " + (item.materialLength ? item.materialLength : '') + (item.materialPower ? item.materialPower : '') + " ATIVADO";
        }
        break;
    }

    // adicao items
    this.cancelledItems = this.cancelledItems.filter(ci => ci.itemId !== id);
    this.changedItems = this.changedItems.filter(ci => ci.itemId !== id);
    if (item.status === "CANCELLED") {
      this.cancelledItems.push({itemId: id, streetId: this.streetId});
    } else if (item.status === "EDITED") {
      this.changedItems.push({itemId: id, streetId: this.streetId, quantity: item.materialQuantity});
    }

    // adicao itens dependentes
    if (relayIndex > 0) {
      this.cancelledItems = this.cancelledItems.filter(ci => ci.itemId !== this.streetItems[relayIndex].preMeasurementStreetItemId);
      this.changedItems = this.changedItems.filter(ci => ci.itemId !== this.streetItems[relayIndex].preMeasurementStreetItemId)
      if (this.streetItems[relayIndex].status === "EDITED") {
        this.changedItems.push({
          itemId: this.streetItems[relayIndex].preMeasurementStreetItemId,
          streetId: this.streetId,
          quantity: this.streetItems[relayIndex].materialQuantity
        });
      } else if (this.streetItems[relayIndex].status === "CANCELLED") {
        this.cancelledItems.push({
          itemId: this.streetItems[relayIndex].preMeasurementStreetItemId,
          streetId: this.streetId
        });
      }
    } else if (cableIndex > 0) {
      this.cancelledItems = this.cancelledItems.filter(ci => ci.itemId !== this.streetItems[cableIndex].preMeasurementStreetItemId);
      this.changedItems = this.changedItems.filter(ci => ci.itemId !== this.streetItems[cableIndex].preMeasurementStreetItemId)
      if (this.streetItems[cableIndex].status === "EDITED") {
        this.changedItems.push({
          itemId: this.streetItems[cableIndex].preMeasurementStreetItemId,
          streetId: this.streetId,
          quantity: this.streetItems[cableIndex].materialQuantity
        });

      } else if (this.streetItems[cableIndex].status === "CANCELLED") {
        this.cancelledItems.push({
          itemId: this.streetItems[cableIndex].preMeasurementStreetItemId,
          streetId: this.streetId
        });
      }
    }
    const street = this.preMeasurement.streets?.find(s => s.preMeasurementStreetId == this.streetId);
    if (street) {
      street.status = this.streetItems.some(i => i.status === 'CANCELLED') ? 'EDITED' : 'PENDING';
    }
    this.utils.showMessage(message, false);

  }

  changeItem(preMeasurementStreetItemId: number, action: 'increment' | 'decrement') {
    let index = this.streetItems.findIndex(i => i.preMeasurementStreetItemId === preMeasurementStreetItemId);
    if (index === -1) return
    let relayIndex = 0;
    if (this.streetItems[index].materialType.toUpperCase() === 'LED') relayIndex = this.streetItems.findIndex(i => i.materialType.toUpperCase() === 'RELÉ');
    let cableIndex = 0;
    if (this.streetItems[index].materialType.toUpperCase() === 'BRAÇO') cableIndex = this.streetItems.findIndex(i => i.materialType.toUpperCase() === 'CABO');
    let cableQuantity = 0.0;

    if (action === 'increment') {
      this.streetItems[index].materialQuantity += 1;
      if (this.streetItems[index].materialQuantity === this.getOriginalItem(this.streetItems[index].preMeasurementStreetItemId)) {
        this.streetItems[index].status = 'PENDING';
      } else {
        this.streetItems[index].status = 'EDITED';
      }

      if (relayIndex > 0) {
        this.streetItems[relayIndex].materialQuantity += 1;
        if (this.streetItems[relayIndex].materialQuantity === this.getOriginalItem(this.streetItems[relayIndex].preMeasurementStreetItemId)) {
          this.streetItems[relayIndex].status = 'PENDING';
        } else {
          this.streetItems[relayIndex].status = 'EDITED';
        }
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
        if (this.streetItems[cableIndex].materialQuantity === this.getOriginalItem(this.streetItems[cableIndex].preMeasurementStreetItemId)) {
          this.streetItems[cableIndex].status = 'PENDING';
        } else {
          this.streetItems[cableIndex].status = 'EDITED';
        }
      }

    } else if (action === 'decrement') {
      if (this.streetItems[index].materialQuantity == 0) return
      this.streetItems[index].materialQuantity -= 1;
      if (this.streetItems[index].materialQuantity === this.getOriginalItem(this.streetItems[index].preMeasurementStreetItemId)) {
        this.streetItems[index].status = 'PENDING';
      } else {
        this.streetItems[index].status = 'EDITED';
      }

      if (relayIndex > 0) {
        // adicionar validacao de rua
        this.streetItems[relayIndex].materialQuantity -= 1;
        if (this.streetItems[relayIndex].materialQuantity === this.getOriginalItem(this.streetItems[relayIndex].preMeasurementStreetItemId)) {
          this.streetItems[relayIndex].status = 'PENDING';
        } else {
          this.streetItems[relayIndex].status = 'EDITED';
        }
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
        if (this.streetItems[cableIndex].materialQuantity === this.getOriginalItem(this.streetItems[cableIndex].preMeasurementStreetItemId)) {
          this.streetItems[cableIndex].status = 'PENDING';
        } else {
          this.streetItems[cableIndex].status = 'EDITED';
        }
      }
    }

    // adicao itens primarios
    this.changedItems = this.changedItems.filter(ci => ci.itemId !== preMeasurementStreetItemId);
    if (this.streetItems[index].status === 'EDITED') {
      this.changedItems.push({
        itemId: preMeasurementStreetItemId,
        streetId: this.streetId,
        quantity: this.streetItems[index].materialQuantity
      });
    }

    // adicao itens dependentes
    if (cableIndex > 0) {
      this.changedItems = this.changedItems.filter(ci => ci.itemId !== this.streetItems[cableIndex].preMeasurementStreetItemId)
      if (this.streetItems[cableIndex].status === 'EDITED')
        this.changedItems.push({
          itemId: this.streetItems[cableIndex].preMeasurementStreetItemId,
          streetId: this.streetId,
          quantity: this.streetItems[cableIndex].materialQuantity
        });
    } else if (relayIndex) {
      this.changedItems = this.changedItems.filter(ci => ci.itemId !== this.streetItems[relayIndex].preMeasurementStreetItemId)
      if (this.streetItems[relayIndex].status === 'EDITED')
        this.changedItems.push({
          itemId: this.streetItems[relayIndex].preMeasurementStreetItemId,
          streetId: this.streetId,
          quantity: this.streetItems[relayIndex].materialQuantity
        });
    }

    const street = this.preMeasurement.streets?.find(s => s.preMeasurementStreetId == this.streetId);
    if (street) {
      street.status = this.streetItems.some(i => i.status === 'EDITED') ? 'EDITED' : 'PENDING';
    }

  }

  getStreetName(streetId: number) {
    return this.preMeasurement.streets.find(s => s.preMeasurementStreetId == streetId)?.street;
  }

  finish: boolean = false;

  conclude() {
    if (this.cancelledItems.length === 0 && this.cancelledStreets.length === 0 && this.changedItems.length === 0) {
      this.utils.showMessage("Não é permitido salvar uma edição com nenhuma rua/item modificado", true);
      return
    }
    this.finish = true;
    this.openModal = true;
  }

  hideContent: boolean = false;
  sendModifications() {
    this.openModal = false;
    const modifications = {
      cancelledStreets: this.cancelledStreets,
      cancelledItems: this.cancelledItems,
      changedItems: this.changedItems
    }

    this.preMeasurementService.sendModifications(modifications).subscribe({
      next: (response: {message: string}) => {
        this.utils.showMessage(response.message, false);
        this.hideContent = true;
        console.log(response);
      }, error: (error) => {
        console.log(error);
      }
    });
  }

  getOriginalItem(preMeasurementStreetItemId: number) {
    return this.preMeasurementCopy
      .streets.find(s => s.preMeasurementStreetId == this.streetId)
      ?.items.find(i => i.preMeasurementStreetItemId == preMeasurementStreetItemId)?.materialQuantity || -1;
  }
}

