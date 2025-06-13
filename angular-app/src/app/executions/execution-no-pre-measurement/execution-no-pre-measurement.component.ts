import {Component, OnInit, ViewChild} from '@angular/core';
import {Breadcrumb} from "primeng/breadcrumb";
import {LoadingComponent} from "../../shared/components/loading/loading.component";
import {NgIf} from "@angular/common";
import {MenuItem, PrimeTemplate} from "primeng/api";
import {Skeleton} from "primeng/skeleton";
import {Table, TableModule} from "primeng/table";
import {Toast} from "primeng/toast";
import {executionWithoutPreMeasurement} from '../executions.model';
import {ContractItemsResponse} from '../../contract/contract-models';
import {ActivatedRoute, Router} from '@angular/router';
import {UtilsService} from '../../core/service/utils.service';
import {ContractService} from '../../contract/services/contract.service';
import {Tag} from 'primeng/tag';
import {Button} from 'primeng/button';
import {Tooltip} from 'primeng/tooltip';
import {InputText} from 'primeng/inputtext';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';

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
    Tooltip,
    InputText,
    ReactiveFormsModule,
    FormsModule
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

  referenceItems: ContractItemsResponse[] = [
    {
      number: 1,
      contractItemId: 1,
      description: 'LED',
      unitPrice: '',
      contractedQuantity: 0,
      linking: '',
      nameForImport: 'LED'
    },
    {
      number: 2,
      contractItemId: 2,
      description: 'RELE',
      unitPrice: '',
      contractedQuantity: 0,
      linking: '',
      nameForImport: 'RELE'
    }
  ];


  constructor(private route: ActivatedRoute,
              protected utils: UtilsService,
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

  @ViewChild('table_parent') table: Table | undefined; // importa de primeng/table
  onRowClick(event: MouseEvent, item: any) {
    const target = event.target as HTMLElement;
    if (target.closest('button')) return;

    // Se já tem uma linha em edição e não é essa, bloqueia abrir outra
    if (this.currentItemId !== 0 && this.currentItemId !== item.contractItemId) {
      // Aqui pode até mostrar mensagem alertando o usuário
      this.utils.showMessage('Por favor, conclua ou cancele a edição atual antes de editar outra linha.', 'warn', 'Atenção');
      return; // bloqueia abrir outra linha
    }

    if (this.table) {
      this.currentItemId = item.contractItemId;
      this.quantity = this.getQuantity(item.contractItemId);
      this.table.initRowEdit(item);
    }
  }


  getQuantity(contractItemId: number) {
    let quantity = 0;
    const item = this.execution.items.find(i => i.contractItemId === contractItemId);
    if (item) quantity = item?.quantity | 0;
    return quantity;
  }

  Cancel(item: any, rowElement: HTMLTableRowElement) {
    const index = this.execution.items.findIndex(i => i.contractItemId === this.currentItemId);
    if (index !== -1) {
      this.execution.items = this.execution.items.filter(i => i.contractItemId != this.currentItemId);
      this.utils.showMessage("Item removido com sucesso.", "success", "Exclusão");
    }

    if(this.table)
      this.table.cancelRowEdit(item);
    this.selectedItem = null;
    this.quantity = 0;
    this.currentItemId = 0;
  }

  cancelEdit() {
    const item = this.referenceItems.find(i => i.contractItemId === this.currentItemId);
    if (this.table && item) {
      this.table.editingCell = null;
      this.table.cancelRowEdit(item); // objeto, não ID
    }
    this.selectedItem = null;
    this.quantity = 0;
    this.currentItemId = 0;
  }


  existsMaterial(contractItemId: number): boolean {
    return this.execution.items.some(i => i.contractItemId === contractItemId);
  }

  quantity: number = 0;

  Confirm(item: any, rowElement: HTMLTableRowElement) {
    const index = this.execution.items.findIndex(i => i.contractItemId === this.currentItemId);
    if (this.quantity < 1) {
      this.utils.showMessage("A quantidade não pode ser igual a 0.", "warn", "Atenção");
      return;
    }

    if (index === -1) {
      this.execution.items.push(
        {
          contractItemId: this.currentItemId,
          quantity: this.quantity
        }
      );
      this.utils.showMessage("Item adiconado com sucesso.", "success", "Adição");
    } else {
      this.execution.items[index].quantity = this.quantity;
      this.utils.showMessage("Item alterado com sucesso.", "success", "Alteração");
    }

    if(this.table) this.table.saveRowEdit(item, rowElement);
    this.selectedItem = null;
    this.quantity = 0;
    this.currentItemId = 0;
  }
}
