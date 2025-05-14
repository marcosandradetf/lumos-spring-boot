import {Component, ElementRef, OnInit, ViewChild} from '@angular/core';
import {ModalComponent} from "../../../../shared/components/modal/modal.component";
import {CurrencyPipe, NgForOf, NgIf} from "@angular/common";
import {ScreenMessageComponent} from "../../../../shared/components/screen-message/screen-message.component";
import {PreMeasurementResponseDTO} from '../../../../models/pre-measurement-response-d-t.o';
import {PreMeasurementService} from '../../../../executions/pre-measurement-home/premeasurement-service.service';
import {UtilsService} from '../../../../core/service/utils.service';
import {ActivatedRoute, Router} from '@angular/router';
import {ContractService} from '../../../services/contract.service';
import {ContractItemsResponse, ContractResponse} from '../../../contract-models';
import {LoadingComponent} from '../../../../shared/components/loading/loading.component';
import {Dialog} from 'primeng/dialog';
import {TableModule} from 'primeng/table';
import {Button, ButtonDirective, ButtonIcon, ButtonLabel} from 'primeng/button';
import {FormsModule} from '@angular/forms';
import {InputText} from 'primeng/inputtext';
import {Ripple} from 'primeng/ripple';

@Component({
  selector: 'app-contract-list',
  standalone: true,
  imports: [
    NgForOf,
    NgIf,
    ScreenMessageComponent,
    LoadingComponent,
    CurrencyPipe,
    Dialog,
    TableModule,
    Button,
    ButtonDirective,
    FormsModule,
    InputText,
    Ripple,
    ButtonIcon,
    ButtonLabel
  ],
  templateUrl: './contract-list.component.html',
  styleUrl: './contract-list.component.scss'
})
export class ContractListComponent implements OnInit {
  contracts: ContractResponse[] = [];
  contractItems: ContractItemsResponse[] = []

  loading: boolean = false;
  protected status: string = "";
  openModal: boolean = false;
  preMeasurementId: number = 0;
  city: string = '';
  reason: string = '';

  constructor(
    private contractService: ContractService,
    protected utils: UtilsService,
    protected router: Router,
    private route: ActivatedRoute
  ) {
  }

  ngOnInit() {
    this.loading = true;
    this.route.queryParams.subscribe(params => {
      this.reason = params['for'];
    });

    this.contractService.getAllContracts().subscribe(c => {
      this.contracts = c;
      this.loading = false;
    });
  }


  // private loadPreMeasurements() {
  //   switch (this.status) {
  //     case 'pendente':
  //       this.preMeasurementService.getPreMeasurements('pending').subscribe(preMeasurements => {
  //         this.preMeasurements = preMeasurements;
  //         this.city = this.preMeasurements[0].streets[0].city;
  //       });
  //       break;
  //     case 'aguardando-retorno':
  //       this.preMeasurementService.getPreMeasurements('waiting').subscribe(preMeasurements => {
  //         this.preMeasurements = preMeasurements;
  //         this.city = this.preMeasurements[0].streets[0].city;
  //       });
  //       break;
  //     case 'validando':
  //       this.preMeasurementService.getPreMeasurements('validating').subscribe(preMeasurements => {
  //         this.preMeasurements = preMeasurements;
  //         this.city = this.preMeasurements[0].streets[0].city;
  //       });
  //       break;
  //     case 'disponivel':
  //       this.preMeasurementService.getPreMeasurements('available').subscribe(preMeasurements => {
  //         this.preMeasurements = preMeasurements;
  //         this.city = this.preMeasurements[0].streets[0].city;
  //       });
  //       break;
  //   }
  // }


  showItems(contractId: number) {
    if (contractId !== 0) {
      this.contractId = contractId;
      this.loading = true;
      this.contractService.getContractItems(contractId).subscribe({
        next: items => {
          this.contractItems = items;
          this.showDialog();
        },
        error: err => {
          console.error(err);
          // talvez mostrar uma notificação
        },
        complete: () => {
          this.loading = false;
        }
      });
    }
  }

  protected readonly parseFloat = parseFloat;

  contractId: number = 0;
  dialogVisible: boolean = false;

  getTotalPrice() {
    return this.contracts.find(c => c.contractId == this.contractId)?.contractValue || "0.00";
  }

  showDialog() {
    this.dialogVisible = true;
  }

  onRowEditInit(item: any) {
    console.log('Edit init', item);
  }

  onRowEditSave(item: any) {
    // Aqui você pode validar ou salvar
    console.log('Item salvo:', item);
  }

  onRowEditCancel(item: any, index: number) {
    console.log('Edição cancelada', item);
  }



}
