import {Component, OnInit} from '@angular/core';
import {Breadcrumb} from "primeng/breadcrumb";
import {LoadingComponent} from "../../shared/components/loading/loading.component";
import {NgIf} from "@angular/common";
import {MenuItem, PrimeTemplate} from "primeng/api";
import {Skeleton} from "primeng/skeleton";
import {TableModule} from "primeng/table";
import {Toast} from "primeng/toast";
import {executionWithoutPreMeasurement, MaterialInStockDTO} from '../executions.model';
import {ContractItemsResponse} from '../../contract/contract-models';
import {ActivatedRoute, Router} from '@angular/router';
import {UtilsService} from '../../core/service/utils.service';
import {ContractService} from '../../contract/services/contract.service';
import {Tag} from 'primeng/tag';
import {Button} from 'primeng/button';
import {Tooltip} from 'primeng/tooltip';

@Component({
  selector: 'app-execution-no-pre-measurement',
  standalone: true,
  imports: [
    Breadcrumb,
    LoadingComponent,
    NgIf,
    PrimeTemplate,
    Skeleton,
    TableModule,
    Toast,
    Tag,
    Button,
    Tooltip
  ],
  templateUrl: './execution-no-pre-measurement.component.html',
  styleUrl: './execution-no-pre-measurement.component.scss'
})
export class ExecutionNoPreMeasurementComponent implements OnInit {

  home: MenuItem = {icon: 'pi pi-home', routerLink: '/'};
  items: MenuItem[] = [
    {label: 'Execuções'},
    {label: 'Iniciar sem pré-medição'},
  ];

  showMessage: string = '';
  loading: boolean = false;
  private contractId: number = 0;

  execution: executionWithoutPreMeasurement = {
    contractId: 0,
    teamId: 1,
    items: [],
  }

  referenceItems: ContractItemsResponse[] = [];


  constructor(private route: ActivatedRoute,
              private utils: UtilsService,
              protected router: Router,
              private contractService: ContractService) {
  }

  ngOnInit() {
    this.loading = true;

    const contractId = this.route.snapshot.paramMap.get('id');
    if (contractId == null) {
      void this.router.navigate(['/']);
      return;
    }
    this.contractId = Number(contractId);

    this.contractService.getContractItems(this.contractId).subscribe({
      next: item => {
        this.referenceItems = item;
      },
      error: err => {
        this.loading = false;
        this.utils.showMessage(err.error.message, 'error');
      },
      complete: () => {
        this.loading = false;
      }
    });
  }

  tableSk: any[] = Array.from({length: 5}).map((_, i) => `Item #${i}`);

  sleep(ms: number) {
    return new Promise(resolve => setTimeout(resolve, ms));
  }


  verifyTeamData() {

  }

  selectedItem!: ContractItemsResponse | null;
  currentItemId = 0;
  onRowClick(event: MouseEvent, currentItemId: number) {
    // Ignora o clique se foi em um botão (ou dentro de um botão)
    const target = event.target as HTMLElement;
    if (target.closest('button')) return;

    this.currentItemId = currentItemId;
  }
}
