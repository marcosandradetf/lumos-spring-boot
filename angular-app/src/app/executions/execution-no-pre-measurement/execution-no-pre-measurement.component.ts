import {Component, ElementRef, OnInit, ViewChild} from '@angular/core';
import {Breadcrumb} from "primeng/breadcrumb";
import {LoadingComponent} from "../../shared/components/loading/loading.component";
import {NgIf} from "@angular/common";
import {MenuItem, PrimeTemplate} from "primeng/api";
import {Skeleton} from "primeng/skeleton";
import {Table, TableModule} from "primeng/table";
import {Toast} from "primeng/toast";
import {DirectExecutionDTO, StockistModel} from '../executions.model';
import {ContractItemsResponse} from '../../contract/contract-models';
import {ActivatedRoute, Router} from '@angular/router';
import {UtilsService} from '../../core/service/utils.service';
import {ContractService} from '../../contract/services/contract.service';
import {Tag} from 'primeng/tag';
import {Button} from 'primeng/button';
import {Tooltip} from 'primeng/tooltip';
import {InputText} from 'primeng/inputtext';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {Select} from 'primeng/select';
import {TeamsModel} from '../../models/teams.model';
import {TeamService} from '../../manage/team/team-service.service';
import {FloatLabel} from 'primeng/floatlabel';
import {Textarea} from 'primeng/textarea';
import {StockService} from '../../stock/services/stock.service';
import {AuthService} from '../../core/auth/auth.service';
import {ExecutionService} from '../execution.service';

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
    InputText,
    ReactiveFormsModule,
    FormsModule,
    Select,
    Tooltip,
    FloatLabel,
    Textarea
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

  nextStep: boolean = false;
  loading: boolean = false;
  private contractId: number = 0;
  contractor: string | null = null;

  execution: DirectExecutionDTO = {
    contractId: 0,
    teamId: 0,
    stockistId: '',
    currentUserUUID: '',
    instructions: null,
    items: [],
  }
  stockists: StockistModel[] = [];

  referenceItems: ContractItemsResponse[] = [];


  constructor(private route: ActivatedRoute,
              protected utils: UtilsService,
              protected router: Router,
              private teamService: TeamService,
              private stockService: StockService,
              private authService: AuthService,
              private executionService: ExecutionService,
              private contractService: ContractService) {
  }

  ngOnInit() {
    this.loading = true;

    let contractId: string | null = null;
    this.route.queryParams.subscribe(params => {
      contractId = params['codigo']
      this.contractor = params['nome']
    });

    if (contractId == null) {
      void this.router.navigate(['/']);
      return;
    }
    this.contractId = Number(contractId);
    this.execution.contractId = this.contractId;
    this.execution.currentUserUUID = this.authService.getUser().uuid;

    this.contractService.getContractItems(this.contractId).subscribe({
      next: item => {
        this.referenceItems = item;
      },
      error: err => {
        this.loading = false;
        this.utils.showMessage(err.error.message, 'error');
      },
      complete: () => {
        this.teamService.getTeams().subscribe({
          next: (response) => {
            this.teams = response;
          },
          error: (error: { error: { message: string } }) => {
            this.utils.showMessage("Erro ao carregar Equipe", 'error');
            this.utils.showMessage(error.error.message, 'error');
            this.loading = false;
          },
          complete: () => {
            this.loading = false;
          }
        });
      }
    });

    this.stockService.getStockists().subscribe({
      next: (response) => {
        this.stockists = response;
      },
      error: (error: { error: { message: string } }) => {
        this.utils.showMessage("Erro ao carregar Estoquistas", 'error');
        this.utils.showMessage(error.error.message, 'error');
      }
    });


  }

  tableSk: any[] = Array.from({length: 5}).map((_, i) => `Item #${i}`);

  showTeamModal = false;
  selectedItem!: ContractItemsResponse | null;
  currentItemId = 0;

  @ViewChild('table_parent') table: Table | undefined; // importa de primeng/table
  @ViewChild('qtyInput') qtyInput!: ElementRef;

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
      setTimeout(() => {
        this.qtyInput?.nativeElement?.focus();
      }, 0);
    }
  }


  getQuantity(contractItemId: number) {
    let quantity: number | null = null;
    const item = this.execution.items.find(i => i.contractItemId === contractItemId);
    if (item) quantity = item?.quantity ?? null;
    return quantity;
  }

  Cancel(item: any, rowElement: HTMLTableRowElement) {
    const index = this.execution.items.findIndex(i => i.contractItemId === this.currentItemId);
    if (index !== -1) {
      this.execution.items = this.execution.items.filter(i => i.contractItemId != this.currentItemId);
      this.utils.showMessage("Item removido com sucesso.", "success", "Exclusão");
    }

    if (this.table) this.table.cancelRowEdit(item);
    this.selectedItem = null;
    this.quantity = null;
    this.currentItemId = 0;
  }

  existsMaterial(contractItemId: number): boolean {
    return this.execution.items.some(i => i.contractItemId === contractItemId);
  }

  quantity: number | null = null;
  teams: TeamsModel[] = [];
  teamName: string | null = null;

  Confirm(item: ContractItemsResponse, rowElement: HTMLTableRowElement) {
    const index = this.execution.items.findIndex(i => i.contractItemId === this.currentItemId);
    if ((this.quantity ?? 0) < 1) {
      this.utils.showMessage("A quantidade não pode ser igual a 0.", "warn", "Atenção");
      return;
    }

    if ((this.quantity ?? 0) > (item.contractedQuantity - item.executedQuantity)) {
      this.utils.showMessage(`O Saldo atual desse item é igual a ${item.contractedQuantity - item.executedQuantity}`, "error", "Saldo contratual não disponível");
      return;
    }

    if (index === -1) {
      this.execution.items.push(
        {
          contractItemId: this.currentItemId,
          quantity: this.quantity ?? 0
        }
      );
      this.utils.showMessage("Item adiconado com sucesso.", "success", "Adição");
    } else {
      this.execution.items[index].quantity = this.quantity ?? 0;
      this.utils.showMessage("Item alterado com sucesso.", "success", "Alteração");
    }

    this.table?.saveRowEdit(item, rowElement);
    this.selectedItem = null;
    this.quantity = 0;
    this.currentItemId = 0;

  }

  getTeamById(id: string) {
    return this.teams.find(i => i.idTeam === id)?.teamName ?? null;
  }

  goToNextStep() {
    if(this.execution.items.length === 0) {
      this.utils.showMessage("Para continuar, é necessário selecionar os itens", "warn", "Atenção");
      return;
    }

    this.nextStep = true;
  }

  finish = false;
  sendData() {
    if(this.execution.stockistId.length === 0) {
      this.utils.showMessage("Para conlcuir, é necessário selecionar o estoquista", "warn", "Atenção");
      return;
    }
    this.loading = true;

    this.executionService.delegateDirectExecution(this.execution).subscribe({
      next: () => {
        this.finish = true;
      },
      error: err => {
        this.loading = false;
        this.utils.showMessage(err.error.message, "error", "Problema ao delegar os itens");
      },
      complete: () => {
        this.loading = false;
      }
    })
  }

}
