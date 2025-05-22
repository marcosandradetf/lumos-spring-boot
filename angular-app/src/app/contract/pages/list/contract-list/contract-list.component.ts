import {Component, OnInit} from '@angular/core';
import {CurrencyPipe, NgForOf, NgIf} from "@angular/common";
import {ScreenMessageComponent} from "../../../../shared/components/screen-message/screen-message.component";
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
import {Toast} from 'primeng/toast';
import {Breadcrumb} from 'primeng/breadcrumb';
import {MenuItem} from 'primeng/api';
import {Title} from '@angular/platform-browser';

@Component({
  selector: 'app-contract-list',
  standalone: true,
  imports: [
    NgForOf,
    NgIf,
    LoadingComponent,
    CurrencyPipe,
    Dialog,
    TableModule,
    Button,
    ButtonDirective,
    FormsModule,
    InputText,
    Toast,
    Breadcrumb
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
  items: MenuItem[] | undefined;

  home: MenuItem | undefined;

  constructor(
    private contractService: ContractService,
    protected utils: UtilsService,
    protected router: Router,
    private route: ActivatedRoute,
    private titleService: Title
  ) {
  }

  ngOnInit() {
    this.loading = true;
    this.route.queryParams.subscribe(params => {
      this.reason = params['for'];
    });

    if(this.reason.toLowerCase() !== 'premeasurement') {
      this.titleService.setTitle("Visualizar Contratos");
    } else {
      this.titleService.setTitle("Importar Pré-Medição");
    }

    this.contractService.getAllContracts().subscribe(c => {
      this.contracts = c;
      this.loading = false;
    });

    this.items = [
      {label: 'Contratos'},
      {label: 'Exibir Todos'},
    ];

    this.home = {icon: 'pi pi-home', routerLink: '/'};
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
