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

    let relayIndex = 0;
    let projectIndex = 0;
    let ledServiceIndex = 0;
    if (this.streetItems[itemIndex].contractReferenceItemType.toUpperCase() === 'LED') {
      relayIndex = this.streetItems.findIndex(i => i.contractReferenceItemType.toUpperCase() === 'RELÉ');
      projectIndex = this.streetItems.findIndex(i => i.contractReferenceItemType.toUpperCase() === 'PROJETO');
      ledServiceIndex = this.streetItems.findIndex(i => i.contractReferenceItemType.toUpperCase() === 'SERVIÇO' && i.contractReferenceItemDependency === 'LED');
    }
    let cableIndex = 0;
    let armServiceIndex = 0;
    if (this.streetItems[itemIndex].contractReferenceItemType.toUpperCase() === 'BRAÇO') {
      cableIndex = this.streetItems.findIndex(i => i.contractReferenceItemType.toUpperCase() === 'CABO');
      armServiceIndex = this.streetItems.findIndex(i => i.contractReferenceItemType.toUpperCase() === 'SERVIÇO' && i.contractReferenceItemDependency === 'BRAÇO');
    }
    let cableQuantity = 0.0;
    let ledQuantity = 0.0;
    let armQuantity = 0.0;


    let item = this.streetItems[itemIndex];
    let message = '';
    switch (this.streetItems[itemIndex].itemStatus) {
      case 'PENDING':
      case 'EDITED':
        this.streetItems[itemIndex].itemStatus = "CANCELLED";
        if (relayIndex > 0) {
          ledQuantity = this.streetItems.filter(si => si.contractReferenceItemType?.toUpperCase() === "LED"
            && si.itemStatus !== "CANCELLED").length;
          if (ledQuantity > 0) {
            this.streetItems[relayIndex].measuredQuantity -= this.streetItems[itemIndex].measuredQuantity;
            this.streetItems[relayIndex].itemStatus = "EDITED";

            this.streetItems[projectIndex].measuredQuantity -= this.streetItems[itemIndex].measuredQuantity;
            this.streetItems[projectIndex].itemStatus = "EDITED";

            this.streetItems[ledServiceIndex].measuredQuantity -= this.streetItems[itemIndex].measuredQuantity;
            this.streetItems[ledServiceIndex].itemStatus = "EDITED";

            message = `ITEM ${item.contractReferenceNameForImport} CANCELADO E ITENS ${this.streetItems[relayIndex].contractReferenceNameForImport}, ${this.streetItems[projectIndex].contractReferenceNameForImport} E ${this.streetItems[ledServiceIndex].contractReferenceNameForImport} ALTERADOS`;
          } else {
            this.streetItems[relayIndex].itemStatus = "CANCELLED";
            this.streetItems[projectIndex].itemStatus = "CANCELLED";
            this.streetItems[ledServiceIndex].itemStatus = "CANCELLED";
            message = `ITENS ${item.contractReferenceNameForImport}, ${this.streetItems[relayIndex].contractReferenceNameForImport}, ${this.streetItems[projectIndex].contractReferenceNameForImport} E ${this.streetItems[ledServiceIndex].contractReferenceNameForImport} CANCELADOS`;
          }
        } else if (cableIndex > 0) {
          armQuantity = this.streetItems.filter(si => si.contractReferenceItemType?.toUpperCase() === "BRAÇO" && si.itemStatus !== "CANCELLED").length;
          if (armQuantity > 0) {
            if (this.streetItems[itemIndex].contractReferenceLinking.startsWith('1')) {
              cableQuantity = 2.5;
            } else if (this.streetItems[itemIndex].contractReferenceLinking.startsWith('2')) {
              cableQuantity = 8.5;
            } else if (this.streetItems[itemIndex].contractReferenceLinking.startsWith('3')) {
              cableQuantity = 12.5;
            }
            this.streetItems[cableIndex].measuredQuantity -= cableQuantity;
            this.streetItems[cableIndex].itemStatus = "EDITED";

            this.streetItems[armServiceIndex].measuredQuantity -= this.streetItems[itemIndex].measuredQuantity;
            this.streetItems[armServiceIndex].itemStatus = "EDITED";

            message = `ITEM ${item.contractReferenceNameForImport} CANCELADO E ITEM ${this.streetItems[cableIndex].contractReferenceNameForImport} ALTERADO`;
          } else {
            this.streetItems[cableIndex].itemStatus = "CANCELLED";
            message = `ITENS ${item.contractReferenceNameForImport} E ${this.streetItems[cableIndex].contractReferenceNameForImport} CANCELADOS`;
          }
        } else {
          message = "ITEM " + item.contractReferenceNameForImport + " CANCELADO";
        }

        break;
      case 'CANCELLED':
        if (this.streetItems[itemIndex].measuredQuantity === this.getOriginalItem(this.streetItems[itemIndex].preMeasurementStreetItemId)) {
          this.streetItems[itemIndex].itemStatus = 'PENDING';
        } else {
          this.streetItems[itemIndex].itemStatus = 'EDITED';
        }
        if (relayIndex > 0) {
          ledQuantity = this.streetItems.filter(si => si.contractReferenceItemType?.toUpperCase() === "LED" && si.itemStatus === "CANCELLED").length;
          if (ledQuantity === 0) {
            this.streetItems[relayIndex].measuredQuantity += this.streetItems[itemIndex].measuredQuantity;
            message = `ITEM ${item.contractReferenceNameForImport} ATIVADO E ITEM ${this.streetItems[relayIndex].contractReferenceNameForImport} ALTERADO`;
          } else {
            message = `ITENS ${item.contractReferenceNameForImport} E ${this.streetItems[relayIndex].contractReferenceNameForImport} ATIVADOS`;
          }
          if (this.streetItems[relayIndex].measuredQuantity === this.getOriginalItem(this.streetItems[relayIndex].preMeasurementStreetItemId)) {
            this.streetItems[relayIndex].itemStatus = 'PENDING';
          } else {
            this.streetItems[relayIndex].itemStatus = 'EDITED';
          }

        } else if (cableIndex > 0) {
          armQuantity = this.streetItems.filter(si => si.contractReferenceItemType?.toUpperCase() === "BRAÇO" && si.itemStatus === "CANCELLED").length;
          if (armQuantity === 0) {
            if (this.streetItems[itemIndex].contractReferenceLinking.startsWith('1')) {
              cableQuantity = 2.5;
            } else if (this.streetItems[itemIndex].contractReferenceLinking.startsWith('2')) {
              cableQuantity = 8.5;
            } else if (this.streetItems[itemIndex].contractReferenceLinking.startsWith('3')) {
              cableQuantity = 12.5;
            }
            this.streetItems[cableIndex].measuredQuantity += cableQuantity;
            this.streetItems[cableIndex].itemStatus = "EDITED";
            message = `ITEM ${item.contractReferenceNameForImport} ATIVADO E ITEM ${this.streetItems[cableIndex].contractReferenceNameForImport} ALTERADO`;
          } else {
            message = `ITENS ${item.contractReferenceNameForImport} E ${this.streetItems[cableIndex].contractReferenceNameForImport} ATIVADOS`;
          }
          if (this.streetItems[cableIndex].measuredQuantity === this.getOriginalItem(this.streetItems[cableIndex].preMeasurementStreetItemId)) {
            this.streetItems[cableIndex].itemStatus = 'PENDING';
          } else {
            this.streetItems[cableIndex].itemStatus = 'EDITED';
          }
        } else {
          message = "ITEM " + item.contractReferenceNameForImport + " ATIVADO";
        }
        break;
    }

    // adicao items
    this.cancelledItems = this.cancelledItems.filter(ci => ci.itemId !== id);
    this.changedItems = this.changedItems.filter(ci => ci.itemId !== id);
    if (item.itemStatus === "CANCELLED") {
      this.cancelledItems.push({itemId: id, streetId: this.streetId});
    } else if (item.itemStatus === "EDITED") {
      this.changedItems.push({itemId: id, streetId: this.streetId, quantity: item.measuredQuantity});
    }

    // adicao itens dependentes
    if (relayIndex > 0) {
      this.cancelledItems = this.cancelledItems.filter(ci => ci.itemId !== this.streetItems[relayIndex].preMeasurementStreetItemId);
      this.changedItems = this.changedItems.filter(ci => ci.itemId !== this.streetItems[relayIndex].preMeasurementStreetItemId)
      if (this.streetItems[relayIndex].itemStatus === "EDITED") {
        this.changedItems.push({
          itemId: this.streetItems[relayIndex].preMeasurementStreetItemId,
          streetId: this.streetId,
          quantity: this.streetItems[relayIndex].measuredQuantity
        });
      } else if (this.streetItems[relayIndex].itemStatus === "CANCELLED") {
        this.cancelledItems.push({
          itemId: this.streetItems[relayIndex].preMeasurementStreetItemId,
          streetId: this.streetId
        });
      }
    } else if (cableIndex > 0) {
      this.cancelledItems = this.cancelledItems.filter(ci => ci.itemId !== this.streetItems[cableIndex].preMeasurementStreetItemId);
      this.changedItems = this.changedItems.filter(ci => ci.itemId !== this.streetItems[cableIndex].preMeasurementStreetItemId)
      if (this.streetItems[cableIndex].itemStatus === "EDITED") {
        this.changedItems.push({
          itemId: this.streetItems[cableIndex].preMeasurementStreetItemId,
          streetId: this.streetId,
          quantity: this.streetItems[cableIndex].measuredQuantity
        });

      } else if (this.streetItems[cableIndex].itemStatus === "CANCELLED") {
        this.cancelledItems.push({
          itemId: this.streetItems[cableIndex].preMeasurementStreetItemId,
          streetId: this.streetId
        });
      }
    }
    const street = this.preMeasurement.streets?.find(s => s.preMeasurementStreetId == this.streetId);
    if (street) {
      street.status = this.streetItems.some(i => item.itemStatus === 'CANCELLED') ? 'EDITED' : 'PENDING';
    }
    this.utils.showMessage(message, false);

  }

  changeItem(preMeasurementStreetItemId: number, action: 'increment' | 'decrement') {
    let index = this.streetItems.findIndex(i => i.preMeasurementStreetItemId === preMeasurementStreetItemId);
    if (index === -1) return
    let relayIndex = 0;
    if (this.streetItems[index].contractReferenceItemType.toUpperCase() === 'LED') relayIndex = this.streetItems.findIndex(i => i.contractReferenceItemType.toUpperCase() === 'RELÉ');
    let cableIndex = 0;
    if (this.streetItems[index].contractReferenceItemType.toUpperCase() === 'BRAÇO') cableIndex = this.streetItems.findIndex(i => i.contractReferenceItemType.toUpperCase() === 'CABO');
    let cableQuantity = 0.0;

    if (action === 'increment') {
      this.streetItems[index].measuredQuantity += 1;
      if (this.streetItems[index].measuredQuantity === this.getOriginalItem(this.streetItems[index].preMeasurementStreetItemId)) {
        this.streetItems[index].itemStatus = 'PENDING';
      } else {
        this.streetItems[index].itemStatus = 'EDITED';
      }

      if (relayIndex > 0) {
        this.streetItems[relayIndex].measuredQuantity += 1;
        if (this.streetItems[relayIndex].measuredQuantity === this.getOriginalItem(this.streetItems[relayIndex].preMeasurementStreetItemId)) {
          this.streetItems[relayIndex].itemStatus = 'PENDING';
        } else {
          this.streetItems[relayIndex].itemStatus = 'EDITED';
        }
      }

      if (cableIndex > 0) {
        if (this.streetItems[index].contractReferenceLinking.startsWith('1')) {
          cableQuantity = 2.5;
        } else if (this.streetItems[index].contractReferenceLinking.startsWith('2')) {
          cableQuantity = 8.5;
        } else if (this.streetItems[index].contractReferenceLinking.startsWith('3')) {
          cableQuantity = 12.5;
        }
        this.streetItems[cableIndex].measuredQuantity += cableQuantity;
        if (this.streetItems[cableIndex].measuredQuantity === this.getOriginalItem(this.streetItems[cableIndex].preMeasurementStreetItemId)) {
          this.streetItems[cableIndex].itemStatus = 'PENDING';
        } else {
          this.streetItems[cableIndex].itemStatus = 'EDITED';
        }
      }

    } else if (action === 'decrement') {
      if (this.streetItems[index].measuredQuantity == 0) return
      this.streetItems[index].measuredQuantity -= 1;
      if (this.streetItems[index].measuredQuantity === this.getOriginalItem(this.streetItems[index].preMeasurementStreetItemId)) {
        this.streetItems[index].itemStatus = 'PENDING';
      } else {
        this.streetItems[index].itemStatus = 'EDITED';
      }

      if (relayIndex > 0) {
        // adicionar validacao de rua
        this.streetItems[relayIndex].measuredQuantity -= 1;
        if (this.streetItems[relayIndex].measuredQuantity === this.getOriginalItem(this.streetItems[relayIndex].preMeasurementStreetItemId)) {
          this.streetItems[relayIndex].itemStatus = 'PENDING';
        } else {
          this.streetItems[relayIndex].itemStatus = 'EDITED';
        }
      }

      if (cableIndex > 0) {
        if (this.streetItems[index].contractReferenceLinking.startsWith('1')) {
          cableQuantity = 2.5;
        } else if (this.streetItems[index].contractReferenceLinking.startsWith('2')) {
          cableQuantity = 8.5;
        } else if (this.streetItems[index].contractReferenceLinking.startsWith('3')) {
          cableQuantity = 12.5;
        }
        this.streetItems[cableIndex].measuredQuantity -= cableQuantity;
        if (this.streetItems[cableIndex].measuredQuantity === this.getOriginalItem(this.streetItems[cableIndex].preMeasurementStreetItemId)) {
          this.streetItems[cableIndex].itemStatus = 'PENDING';
        } else {
          this.streetItems[cableIndex].itemStatus = 'EDITED';
        }
      }
    }

    // adicao itens primarios
    this.changedItems = this.changedItems.filter(ci => ci.itemId !== preMeasurementStreetItemId);
    if (this.streetItems[index].itemStatus === 'EDITED') {
      this.changedItems.push({
        itemId: preMeasurementStreetItemId,
        streetId: this.streetId,
        quantity: this.streetItems[index].measuredQuantity
      });
    }

    // adicao itens dependentes
    if (cableIndex > 0) {
      this.changedItems = this.changedItems.filter(ci => ci.itemId !== this.streetItems[cableIndex].preMeasurementStreetItemId)
      if (this.streetItems[cableIndex].itemStatus === 'EDITED')
        this.changedItems.push({
          itemId: this.streetItems[cableIndex].preMeasurementStreetItemId,
          streetId: this.streetId,
          quantity: this.streetItems[cableIndex].measuredQuantity
        });
    } else if (relayIndex) {
      this.changedItems = this.changedItems.filter(ci => ci.itemId !== this.streetItems[relayIndex].preMeasurementStreetItemId)
      if (this.streetItems[relayIndex].itemStatus === 'EDITED')
        this.changedItems.push({
          itemId: this.streetItems[relayIndex].preMeasurementStreetItemId,
          streetId: this.streetId,
          quantity: this.streetItems[relayIndex].measuredQuantity
        });
    }

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
    if(!itemId) return;
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
    if(!project) return 0;
    let quantity = 0;
    this.streetItems.forEach(item => {
      if(item.contractReferenceItemType?.toUpperCase() == project.contractReferenceItemDependency) {
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

}

