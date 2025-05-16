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
import {ContractAndItemsResponse} from '../../contract/contract-models';
import {PreMeasurementResponseDTO} from '../pre-measurement-models';
import {ButtonIcon} from 'primeng/button';

@Component({
  selector: 'app-pre-measurement-edit',
  standalone: true,
  imports: [
    ScreenMessageComponent,
    NgForOf,
    NgIf,
    ModalComponent,
    NgClass,
    ButtonIcon
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
    depositName: '',
    streets: [],
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
    depositName: '',
    streets: [],
  };

  contract: ContractAndItemsResponse = {
    contractId: 0,
    number: "",
    contractor: "",
    cnpj: "",
    phone: "",
    address: "",
    contractFile: "",
    createdBy: '',
    createdAt: '',
    items: [],
    noticeFile: '',
    itemQuantity: 0,
    contractStatus: '',
    contractValue: '',
    additiveFile: ''
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

  streetItems: PreMeasurementResponseDTO["streets"][0]["items"] = [];

  constructor(protected router: Router, protected utils: UtilsService, private titleService: Title,
              private preMeasurementService: PreMeasurementService, private route: ActivatedRoute, authService: AuthService,
              private userService: UserService, private reportService: ReportService) {

    const measurementId = this.route.snapshot.paramMap.get('id');
    this.titleService.setTitle("Editar Pré-medição");

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
        return this.streetItems.find(i => i.preMeasurementStreetItemId == id)?.itemStatus === "CANCELLED";
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
          if (item) item.itemStatus = "CANCELLED";
        });
        this.preMeasurement.streets[streetIndex] = street;
        this.cancelledStreets.push({streetId: id});
        this.openModal = false;
        this.utils.showMessage("Todos os itens da rua " + street.street + " foram cancelados", false);
        break;
      case false:
        street.items.forEach((item) => {
          if (item) {
            if (item.measuredQuantity === this.getOriginalItem(item.preMeasurementStreetItemId)) {
              item.itemStatus = 'PENDING';
            } else {
              item.itemStatus = 'EDITED';
              this.changedItems.push({
                itemId: item.preMeasurementStreetItemId,
                streetId: this.streetId,
                quantity: item.measuredQuantity
              });
            }
          }
        });
        street.status = this.streetItems.some(i => i.itemStatus === 'EDITED') ? 'EDITED' : 'PENDING';
        this.preMeasurement.streets[streetIndex] = street;
        this.openModal = false;
        this.utils.showMessage("Todos os itens da rua " + street.street + " foram reativados", false);
        break;
    }

  }

  cancelItem(id: number) {
    let itemIndex = this.streetItems.findIndex(i => i.preMeasurementStreetItemId == id);
    if (itemIndex === -1) return

    let relayIndex = -1;
    let projectIndex = -1;
    let ledServiceIndex = -1;
    let cableIndex = -1;
    let armServiceIndex = -1;
    let item = this.streetItems[itemIndex];
    let message: string = '';

    if (this.streetItems[itemIndex].contractReferenceItemType.toUpperCase() === 'LED') {
      relayIndex = this.streetItems.findIndex(i => i.contractReferenceItemType.toUpperCase() === 'RELÉ');
      projectIndex = this.streetItems.findIndex(i => i.contractReferenceItemType.toUpperCase() === 'PROJETO');
      ledServiceIndex = this.streetItems.findIndex(i => i.contractReferenceItemType.toUpperCase() === 'SERVIÇO' && i.contractReferenceItemDependency === 'LED');
    }
    if (this.streetItems[itemIndex].contractReferenceItemType.toUpperCase() === 'BRAÇO') {
      cableIndex = this.streetItems.findIndex(i => i.contractReferenceItemType.toUpperCase() === 'CABO');
      armServiceIndex = this.streetItems.findIndex(i => i.contractReferenceItemType.toUpperCase() === 'SERVIÇO' && i.contractReferenceItemDependency === 'BRAÇO');
    }

    this.cancelledItems = this.cancelledItems.filter(ci => ci.itemId !== id);
    this.changedItems = this.changedItems.filter(ci => ci.itemId !== id);
    switch (this.streetItems[itemIndex].itemStatus) {
      case 'PENDING':
      case 'EDITED':
        this.streetItems[itemIndex].itemStatus = "CANCELLED";
        this.cancelledItems.push({itemId: id, streetId: this.streetId});
        message = "ITEM " + item.contractReferenceNameForImport + " CANCELADO"
        break;
      case 'CANCELLED':
        if (this.streetItems[itemIndex].measuredQuantity === this.getOriginalItem(this.streetItems[itemIndex].preMeasurementStreetItemId)) {
          this.streetItems[itemIndex].itemStatus = 'PENDING';
          message = "ITEM " + item.contractReferenceNameForImport + " REATIVADO"
        } else {
          this.streetItems[itemIndex].itemStatus = 'EDITED';
          this.changedItems.push({itemId: id, streetId: this.streetId, quantity: item.measuredQuantity});
          message = "ITEM " + item.contractReferenceNameForImport + " ALTERADO"
        }
        break;
    }

    // adicao itens dependentes
    message = message + this.changeDependencyItems(id, relayIndex, projectIndex, ledServiceIndex, cableIndex, armServiceIndex);

    const street = this.preMeasurement.streets?.find(s => s.preMeasurementStreetId == this.streetId);
    if (street) {
      street.status = this.streetItems.some(i => i.itemStatus === 'CANCELLED') ? 'EDITED' : 'PENDING';
    }
    this.utils.showMessage(message, false);
  }

  changeItem(id: number, action: 'increment' | 'decrement') {
    let currentItemIndex = this.streetItems.findIndex(i => i.preMeasurementStreetItemId == id);
    if (currentItemIndex === -1) return

    let relayIndex = -1;
    let projectIndex = -1;
    let ledServiceIndex = -1;
    let cableIndex = -1;
    let armServiceIndex = -1;
    let message: string = '';

    if (this.streetItems[currentItemIndex].contractReferenceItemType.toUpperCase() === 'LED') {
      relayIndex = this.streetItems.findIndex(i => i.contractReferenceItemType.toUpperCase() === 'RELÉ');
      projectIndex = this.streetItems.findIndex(i => i.contractReferenceItemType.toUpperCase() === 'PROJETO');
      ledServiceIndex = this.streetItems.findIndex(i => i.contractReferenceItemType.toUpperCase() === 'SERVIÇO' && i.contractReferenceItemDependency === 'LED');
    }
    if (this.streetItems[currentItemIndex].contractReferenceItemType.toUpperCase() === 'BRAÇO') {
      cableIndex = this.streetItems.findIndex(i => i.contractReferenceItemType.toUpperCase() === 'CABO');
      armServiceIndex = this.streetItems.findIndex(i => i.contractReferenceItemType.toUpperCase() === 'SERVIÇO' && i.contractReferenceItemDependency === 'BRAÇO');
    }

    this.cancelledItems = this.cancelledItems.filter(ci => ci.itemId !== id);
    this.changedItems = this.changedItems.filter(ci => ci.itemId !== id);

    if (action === 'increment') {
      this.streetItems[currentItemIndex].measuredQuantity += 1;
      if (this.streetItems[currentItemIndex].measuredQuantity === this.getOriginalItem(id)) {
        this.streetItems[currentItemIndex].itemStatus = 'PENDING';
      } else {
        this.streetItems[currentItemIndex].itemStatus = 'EDITED';
      }
    } else if (action === 'decrement') {
      if (this.streetItems[currentItemIndex].measuredQuantity == 0) return
      this.streetItems[currentItemIndex].measuredQuantity -= 1;
      if (this.streetItems[currentItemIndex].measuredQuantity === this.getOriginalItem(id)) {
        this.streetItems[currentItemIndex].itemStatus = 'PENDING';
      } else {
        this.streetItems[currentItemIndex].itemStatus = 'EDITED';
      }
    }

    message = message + this.changeDependencyItems(this.streetItems[currentItemIndex].preMeasurementStreetItemId, relayIndex, projectIndex, ledServiceIndex, cableIndex, armServiceIndex);

    const street = this.preMeasurement.streets?.find(s => s.preMeasurementStreetId == this.streetId);
    if (street) {
      street.status = this.streetItems.some(i => i.itemStatus === 'EDITED') ? 'EDITED' : 'PENDING';
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
      next: (response: { message: string }) => {
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
      ?.items.find(i => i.preMeasurementStreetItemId == preMeasurementStreetItemId)?.measuredQuantity || -1;
  }

  cancelProject() {
    const itemId = this.streetItems.find(i => i.contractReferenceItemType?.toUpperCase() == "PROJETO")?.preMeasurementStreetItemId;
    if (!itemId) return;
    let itemIndex = this.streetItems.findIndex(i => i.preMeasurementStreetItemId == itemId);
    if (itemIndex === -1) return

    let item = this.streetItems[itemIndex];
    let message = '';
    const quantity = this.getProjectQuantity();
    switch (this.streetItems[itemIndex].itemStatus) {
      case 'PENDING':
      case 'EDITED':
        this.streetItems[itemIndex].itemStatus = "CANCELLED";
        this.streetItems[itemIndex].measuredQuantity = quantity;
        message = "PROJETO CANCELADO NA RUA ATUAL";
        break;
      case 'CANCELLED':
        if (quantity === this.getOriginalItem(this.streetItems[itemIndex].preMeasurementStreetItemId)) {
          this.streetItems[itemIndex].itemStatus = 'PENDING';
        } else {
          this.streetItems[itemIndex].itemStatus = 'EDITED';
        }
        message = "PROJETO RESTAURADO NA RUA ATUAL";
        break;
    }

    this.cancelledItems = this.cancelledItems.filter(ci => ci.itemId !== itemId);
    this.changedItems = this.changedItems.filter(ci => ci.itemId !== itemId);
    if (item.itemStatus === "CANCELLED") {
      this.cancelledItems.push({itemId: itemId, streetId: this.streetId});
    } else if (item.itemStatus === "EDITED") {
      this.changedItems.push({itemId: itemId, streetId: this.streetId, quantity: item.measuredQuantity});
    }
    this.utils.showMessage(message, false);
  }

  getProjectQuantity() {
    const project = this.streetItems.find(i => i.contractReferenceItemType?.toUpperCase() == "PROJETO")
    if (!project) return 0;
    let quantity = 0;
    this.streetItems.forEach(item => {
      if (item.contractReferenceItemType?.toUpperCase() == project.contractReferenceItemDependency) {
        quantity += item.measuredQuantity;
      }
    });
    return quantity;
  }

  isProjectCancelled() {
    const itemId = this.streetItems.find(i => i.contractReferenceItemType?.toUpperCase() == "PROJETO")?.preMeasurementStreetItemId;
    if (!itemId) return false;
    let itemIndex = this.streetItems.findIndex(i => i.preMeasurementStreetItemId == itemId);
    if (itemIndex === -1) return false;

    return this.streetItems[itemIndex].itemStatus === 'CANCELLED';
  }

  clearItems(itemIndex: number, where: "all" | 'cancelled' | 'changed') {
    switch (where) {
      case 'all':
        this.cancelledItems = this.cancelledItems.filter(ci => ci.itemId !== this.streetItems[itemIndex].preMeasurementStreetItemId);
        this.changedItems = this.changedItems.filter(ci => ci.itemId !== this.streetItems[itemIndex].preMeasurementStreetItemId);
        break;
      case 'cancelled':
        this.cancelledItems = this.cancelledItems.filter(ci => ci.itemId !== this.streetItems[itemIndex].preMeasurementStreetItemId);
        break;
      case 'changed':
        this.changedItems = this.changedItems.filter(ci => ci.itemId !== this.streetItems[itemIndex].preMeasurementStreetItemId);
        break;
    }
  }

  insertItems(itemIndex: number, where: 'cancelled' | 'changed') {
    switch (where) {
      case 'cancelled':
        this.cancelledItems.push({
          itemId: this.streetItems[itemIndex].preMeasurementStreetItemId,
          streetId: this.streetId
        });
        break;
      case 'changed':
        this.changedItems.push({
          itemId: this.streetItems[itemIndex].preMeasurementStreetItemId,
          streetId: this.streetId,
          quantity: this.streetItems[itemIndex].measuredQuantity
        });
        break;
    }
  }

  private getCableQuantity() {
    let cableQuantity = 0.0;
    this.streetItems.forEach(item => {
      if (item.contractReferenceItemType?.toUpperCase() == 'BRAÇO' && item.itemStatus !== 'CANCELLED') {
        if (item?.contractReferenceLinking.startsWith('1')) {
          cableQuantity += item.measuredQuantity * 2.5;
        } else if (item?.contractReferenceLinking.startsWith('2')) {
          cableQuantity += item.measuredQuantity * 8.5;
        } else if (item?.contractReferenceLinking.startsWith('3')) {
          cableQuantity += item.measuredQuantity * 12.5;
        }
      }
    });
    return cableQuantity;
  }

  private getLedQuantity() {
    let ledQuantity = 0;
    this.streetItems.forEach(item => {
      if (item.contractReferenceItemType?.toUpperCase() == 'LED' && item.itemStatus !== 'CANCELLED') {
        ledQuantity += item.measuredQuantity;
      }
    });
    return ledQuantity;
  }

  private changeDependencyItems(currentItemId: number, relayIndex: number, projectIndex: number, ledServiceIndex: number, cableIndex: number, armServiceIndex: number): string {
    let message = '';
    let allLed: any[] = []
    let ledQuantity = 0;
    let allArms: any[] = []
    let armQuantity = 0;
    let status = '';

    if (relayIndex !== -1) {
      allLed = this.streetItems.filter(si => si.contractReferenceItemType?.toUpperCase() === "LED" && si.itemStatus !== "CANCELLED");
      ledQuantity = allLed.reduce((sum, si) => sum + (si.measuredQuantity || 0), 0);

      this.clearItems(relayIndex, 'all');
      this.clearItems(projectIndex, 'all');
      this.clearItems(ledServiceIndex, 'all');

      this.streetItems[relayIndex].measuredQuantity = ledQuantity;
      this.streetItems[projectIndex].measuredQuantity = ledQuantity;
      this.streetItems[ledServiceIndex].measuredQuantity = ledQuantity;

      if (ledQuantity > 0) {
        if (ledQuantity !== this.getOriginalItem(currentItemId)) {
          status = "EDITED";
          message = message + ` / ITENS ${this.streetItems[relayIndex].contractReferenceNameForImport}, ${this.streetItems[projectIndex].contractReferenceNameForImport} E ${this.streetItems[ledServiceIndex].contractReferenceNameForImport} ALTERADOS`
        } else {
          status = "PENDING";
          message = message + ` / ITENS ${this.streetItems[relayIndex].contractReferenceNameForImport}, ${this.streetItems[projectIndex].contractReferenceNameForImport} E ${this.streetItems[ledServiceIndex].contractReferenceNameForImport} REATIVADOS`
        }
      } else {
        status = "CANCELLED";
        message = message + ` / ITENS ${this.streetItems[relayIndex].contractReferenceNameForImport}, ${this.streetItems[projectIndex].contractReferenceNameForImport} E ${this.streetItems[ledServiceIndex].contractReferenceNameForImport} CANCELADOS`
      }

      this.streetItems[relayIndex].itemStatus = status;
      this.streetItems[projectIndex].itemStatus = status;
      this.streetItems[ledServiceIndex].itemStatus = status;

      if (status === "EDITED") {
        this.insertItems(relayIndex, 'changed');
        this.insertItems(projectIndex, 'changed');
        this.insertItems(ledServiceIndex, 'changed');
      } else if (status === "CANCELLED") {
        this.insertItems(relayIndex, 'cancelled');
        this.insertItems(projectIndex, 'cancelled');
        this.insertItems(ledServiceIndex, 'cancelled');
      }

    } else if (cableIndex !== -1) {
      allArms = this.streetItems.filter(si => si.contractReferenceItemType?.toUpperCase() === "BRAÇO" && si.itemStatus !== "CANCELLED");
      armQuantity = allArms.reduce((sum, si) => sum + (si.measuredQuantity || 0), 0);

      this.clearItems(cableIndex, 'all');
      this.clearItems(armServiceIndex, 'all');

      this.streetItems[armServiceIndex].measuredQuantity = armQuantity;
      this.streetItems[cableIndex].measuredQuantity = this.getCableQuantity();

      if (allArms.length > 0) {
        if (armQuantity !== this.getOriginalItem(currentItemId)) {
          status = "EDITED";
          message = message + ` / ITENS ${this.streetItems[cableIndex].contractReferenceNameForImport} E ${this.streetItems[armServiceIndex].contractReferenceNameForImport} ALTERADOS`
        } else {
          status = "PENDING";
          message = message + ` / ITENS ${this.streetItems[cableIndex].contractReferenceNameForImport} E ${this.streetItems[armServiceIndex].contractReferenceNameForImport} REATIVADOS`
        }
      } else {
        status = "CANCELLED";
        this.streetItems[armServiceIndex].itemStatus = "CANCELLED";
        message = message + ` / ITENS ${this.streetItems[cableIndex].contractReferenceNameForImport} E ${this.streetItems[armServiceIndex].contractReferenceNameForImport} CANCELADOS`
      }

      this.streetItems[cableIndex].itemStatus = status;
      this.streetItems[armServiceIndex].itemStatus = status;

      if (status === "EDITED") {
        this.insertItems(cableIndex, 'changed');
        this.insertItems(armServiceIndex, 'changed');
      } else if (status === "CANCELLED") {
        this.insertItems(cableIndex, 'cancelled');
        this.insertItems(armServiceIndex, 'cancelled');
      }
    }

    return message;
  }

}

