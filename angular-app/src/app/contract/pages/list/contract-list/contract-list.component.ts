import {Component, OnInit} from '@angular/core';
import {CurrencyPipe, NgForOf, NgIf} from "@angular/common";
import {UtilsService} from '../../../../core/service/utils.service';
import {ActivatedRoute, Router} from '@angular/router';
import {ContractService} from '../../../services/contract.service';
import {
  ContractItemsResponseWithExecutionsSteps,
  ContractResponse
} from '../../../contract-models';
import {LoadingComponent} from '../../../../shared/components/loading/loading.component';
import {Dialog} from 'primeng/dialog';
import {TableModule} from 'primeng/table';
import {Button, ButtonDirective, ButtonIcon, ButtonLabel} from 'primeng/button';
import {FormsModule} from '@angular/forms';
import {InputText} from 'primeng/inputtext';
import {Toast} from 'primeng/toast';
import {Breadcrumb} from 'primeng/breadcrumb';
import {MenuItem} from 'primeng/api';
import {Title} from '@angular/platform-browser';
import {FileService} from '../../../../core/service/file-service.service';
import {ContextMenu} from 'primeng/contextmenu';
import {
  PrimeConfirmDialogComponent
} from '../../../../shared/components/prime-confirm-dialog/prime-confirm-dialog.component';
import {ViewChild} from '@angular/core';
import {IconField} from 'primeng/iconfield';
import {InputIcon} from 'primeng/inputicon';


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
    Breadcrumb,
    ContextMenu,
    PrimeConfirmDialogComponent,
    IconField,
    InputIcon
  ],
  templateUrl: './contract-list.component.html',
  styleUrl: './contract-list.component.scss'
})
export class ContractListComponent implements OnInit {
  contracts: ContractResponse[] = [];
  contractsBackup: ContractResponse[] = [];
  contractItems: ContractItemsResponseWithExecutionsSteps[] = []

  loading: boolean = false;
  protected status: string = "";
  openModal: boolean = false;
  preMeasurementId: number = 0;
  city: string = '';
  reason: string = '';
  items: MenuItem[] | undefined;
  selectedContract: any = null;

  message = "";
  contextItems: MenuItem[] = [
    {label: 'Novo Contrato', icon: 'pi pi-plus', command: () => this.router.navigate(['/contratos/criar']),},
    {
      label: 'Excluir Contrato', icon: 'pi pi-trash', command: () => {
        this.message = "Confirma a exclusão do contrato " + this.selectedContract.contractor + "?"
        this.openModal = true;
      }
    },
    {
      label: 'Arquivar', icon: 'pi pi-folder-open', command: () => {
        this.archive = true
        this.message = "Confirma o arquivamento do contrato " + this.selectedContract.contractor + "?"
        this.openModal = true;
      }
    },
  ];

  home: MenuItem | undefined;
  private archive: boolean = false;

  constructor(
    private contractService: ContractService,
    protected utils: UtilsService,
    protected router: Router,
    private route: ActivatedRoute,
    private titleService: Title,
    private minioService: FileService
  ) {
  }

  ngOnInit() {
    this.loading = true;
    this.route.queryParams.subscribe(params => {
      this.reason = params['for'];
    });

    if (this.reason.toLowerCase() !== 'premeasurement') {
      this.titleService.setTitle("Visualizar Contratos");
    } else {
      this.titleService.setTitle("Importar Pré-Medição");
    }

    this.contractService.getAllContracts().subscribe(c => {
      this.contracts = c;
      this.contractsBackup = c;
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


  showItems(contractId: number): void {
    if (contractId === 0) return;

    this.contractId = contractId;
    this.loading = true;

    this.contractService.getContractItemsWithExecutionsSteps(contractId).subscribe({
      next: items => {
        this.contractItems = items || [];
        this.normalizeExecutedQuantities(); // 🔧 Preenche etapas faltantes com 0
        this.showDialog();                  // 💬 Abre o modal
      },
      error: err => {
        console.error('Erro ao carregar itens do contrato:', err);
      },
      complete: () => {
        this.loading = false;
      }
    });
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


  download(file: 'contract' | 'notice' | 'additive') {
    const contract = this.contracts.find(c => c.contractId == this.contractId);
    switch (file) {
      case 'contract':
        const contractFile = contract?.contractFile;
        if (contractFile) {
          this.minioService.downloadFile(contractFile).subscribe(response => {
            const contentDisposition = response.headers.get('Content-Disposition');
            const filenameMatch = contentDisposition?.match(/filename="(.+)"/);
            const filename = filenameMatch ? filenameMatch[1] : contractFile;

            const blob = new Blob([response.body!], {type: response.headers.get('Content-Type') || 'application/octet-stream'});
            const url = window.URL.createObjectURL(blob);

            const link = document.createElement('a');
            link.href = url;
            link.download = filename;
            link.click();

            window.URL.revokeObjectURL(url);
          });
        } else this.utils.showMessage('Nenhum contrato foi encontrado', "warn", 'Arquivo não existente')
        break;
      case 'notice':
        const noticeFile = contract?.noticeFile;
        if (noticeFile) {
          this.minioService.downloadFile(noticeFile).subscribe(response => {
            const contentDisposition = response.headers.get('Content-Disposition');
            const filenameMatch = contentDisposition?.match(/filename="(.+)"/);
            const filename = filenameMatch ? filenameMatch[1] : noticeFile;

            const blob = new Blob([response.body!], {type: response.headers.get('Content-Type') || 'application/octet-stream'});
            const url = window.URL.createObjectURL(blob);

            const link = document.createElement('a');
            link.href = url;
            link.download = filename;
            link.click();

            window.URL.revokeObjectURL(url);
          });
        } else this.utils.showMessage('Nenhum edital foi encontrado', "warn", 'Arquivo não existente')
        break
      case 'additive':
        const additiveFile = contract?.additiveFile;
        if (additiveFile) {
          this.minioService.downloadFile(additiveFile).subscribe(response => {
            const contentDisposition = response.headers.get('Content-Disposition');
            const filenameMatch = contentDisposition?.match(/filename="(.+)"/);
            const filename = filenameMatch ? filenameMatch[1] : additiveFile;

            const blob = new Blob([response.body!], {type: response.headers.get('Content-Type') || 'application/octet-stream'});
            const url = window.URL.createObjectURL(blob);

            const link = document.createElement('a');
            link.href = url;
            link.download = filename;
            link.click();

            window.URL.revokeObjectURL(url);
          });
        } else this.utils.showMessage('Nenhum aditivo foi encontrado', "warn", 'Arquivo não existente')
        break;
    }
  }

  getExecutions() {
    return this.contractItems.find(i => i.contractItemId)
  }

  get maxExecutedSteps(): number {
    if (!this.contractItems || this.contractItems.length === 0) return 0;

    return this.contractItems.reduce((max, item) => {
      const steps = item.executedQuantity?.map(q => q.step) || [];
      const maxStep = Math.max(0, ...steps);
      return maxStep > max ? maxStep : max;
    }, 0);
  }

  normalizeExecutedQuantities(): void {
    const maxSteps = this.maxExecutedSteps;

    this.contractItems.forEach(item => {
      const quantities = item.executedQuantity || [];

      // Garante que todos os steps estejam presentes com zero se ausentes
      item.executedQuantity = Array.from({length: maxSteps}, (_, i) => {
        const step = i + 1;
        const found = quantities.find(q => q.step === step);
        return found ?? {
          directExecutionId: 0, // ou null, se preferir
          step,
          quantity: 0
        };
      });
    });
  }


  handleAction(action: "accept" | "reject") {
    switch (action) {
      case 'accept':
        this.loading = true;
        this.openModal = false;

        if (this.archive) {
          this.contractService.archiveById(this.selectedContract.contractId)
            .subscribe({
              next: () => {
                this.contracts = this.contracts.filter(c => c.contractId !== this.selectedContract.contractId);
                this.utils.showMessage("Contrato " + this.selectedContract.contractor + " arquivado com sucesso!", "success", "Operação realizada")
              },
              error: (err) => {
                this.utils.showMessage(err.error.message, "error", "Não foi possível arquivar o contrato " + this.selectedContract.contractor);
                this.loading = false;
              },
              complete: () => {
                this.loading = false;
              }
            });
        } else {
          this.contractService.deleteById(this.selectedContract.contractId)
            .subscribe({
              next: () => {
                this.contracts = this.contracts.filter(c => c.contractId !== this.selectedContract.contractId);
                this.utils.showMessage("Contrato " + this.selectedContract.contractor + " excluido com sucesso!", "success", "Operação realizada")
              },
              error: (err) => {
                this.utils.showMessage(err.error.message, "error", "Não foi possível excluir o contrato " + this.selectedContract.contractor);
                this.loading = false;
              },
              complete: () => {
                this.loading = false;
              }
            });
        }
        break;
      case 'reject':
        this.openModal = false;
        this.message = ''
        break;
    }
  }

  onRightClick(event: MouseEvent, contract: any): void {
    event.preventDefault(); // impede menu padrão do navegador
    this.selectedContract = contract;
  }

  @ViewChild('menu') contextMenu: ContextMenu | undefined = undefined;

  openContextMenu(event: MouseEvent, contract: any): void {
    event.preventDefault(); // Evita scroll inesperado ou comportamento nativo
    this.selectedContract = contract; // Salva se precisar no menu

    if (this.contextMenu) this.contextMenu.show(event); // Abre o menu na posição do clique
  }

  filterData(event: Event) {
    let value = (event.target as HTMLInputElement).value;

    if (value === null || value === undefined || value === '') {
      this.contracts = this.contractsBackup;
    }

    this.contracts = this.contractsBackup.filter(c => c.contractor.toLowerCase().includes(value.toLowerCase()));
  }
}
