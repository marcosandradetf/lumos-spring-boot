import {Component, OnInit} from '@angular/core';
import {Breadcrumb} from 'primeng/breadcrumb';
import {LoadingComponent} from '../../shared/components/loading/loading.component';
import {PrimeBreadcrumbComponent} from '../../shared/components/prime-breadcrumb/prime-breadcrumb.component';
import {ActivatedRoute, Router} from '@angular/router';
import {UtilsService} from '../../core/service/utils.service';
import {TeamService} from '../../manage/team/team-service.service';
import {StockService} from '../../stock/services/stock.service';
import {AuthService} from '../../core/auth/auth.service';
import {ExecutionService} from '../../executions/execution.service';
import {ContractService} from '../../contract/services/contract.service';
import {DirectExecutionDTO, StockistModel} from '../../executions/executions.model';
import {Select} from 'primeng/select';
import {FormsModule} from '@angular/forms';
import {TeamsModel} from '../../models/teams.model';
import {ContractItemsResponse} from '../../contract/contract-models';
import {Toast} from 'primeng/toast';
import {NgIf} from '@angular/common';
import {PrimeTemplate} from 'primeng/api';
import {Skeleton} from 'primeng/skeleton';
import {TableModule} from 'primeng/table';
import {FloatLabel} from 'primeng/floatlabel';
import {InputText} from 'primeng/inputtext';

@Component({
  selector: 'app-maintenance.create',
  standalone: true,
  imports: [
    Breadcrumb,
    LoadingComponent,
    PrimeBreadcrumbComponent,
    Select,
    FormsModule,
    Toast,
    NgIf,
    PrimeTemplate,
    Skeleton,
    TableModule,
    FloatLabel,
    InputText
  ],
  templateUrl: './maintenance.create.component.html',
  styleUrl: './maintenance.create.component.scss'
})
export class MaintenanceCreateComponent implements OnInit {
  private contractId: number = 0;
  loading = false;
  contractor: string | null = null;
  teamName: string | null = null;
  tableSk: any[] = Array.from({length: 5}).map((_, i) => `Item #${i}`);
  address: string | null = null;

  execution: DirectExecutionDTO = {
    contractId: 0,
    teamId: 1,
    stockistId: '',
    currentUserUUID: '',
    instructions: null,
    items: [],
  }
  teams: TeamsModel[] = [];
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
    this.getEssentialsData();
  }

  getEssentialsData() {
    this.contractService.getContractItems(this.contractId).subscribe({
      next: items => {
        this.referenceItems = items;
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

  getTeamById(id: string) {
    return this.teams.find(i => i.idTeam === id)?.teamName ?? null;
  }

}
