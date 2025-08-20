import {Component, OnInit, ViewChild} from '@angular/core';
import {SafeUrlPipe} from '../../safe-url.pipe';
import {ReportService} from '../report.service';
import {UtilsService} from '../../core/service/utils.service';
import {Title} from '@angular/platform-browser';
import {PrimeBreadcrumbComponent} from '../../shared/components/prime-breadcrumb/prime-breadcrumb.component';
import {LoadingComponent} from '../../shared/components/loading/loading.component';
import {DatePipe, NgForOf, NgIf} from '@angular/common';
import {Toast} from 'primeng/toast';
import {Menu} from 'primeng/menu';
import {MenuItem} from 'primeng/api';
import {PrimeConfirmDialogComponent} from '../../shared/components/prime-confirm-dialog/prime-confirm-dialog.component';

@Component({
  selector: 'app-maintenance',
  standalone: true,
  imports: [
    PrimeBreadcrumbComponent,
    LoadingComponent,
    NgForOf,
    SafeUrlPipe,
    DatePipe,
    Toast,
    Menu,
    PrimeConfirmDialogComponent
  ],
  providers: [SafeUrlPipe],
  templateUrl: './maintenance.component.html',
  styleUrl: './maintenance.component.scss'
})
export class MaintenanceComponent implements OnInit {
  pdfUrl: string | null = null;
  loading = false;

  data: {
    contract: {
      contract_id: number;
      contractor: string;
    },
    maintenances: {
      maintenance_id: string;
      streets: [];
      date_of_visit: string;
      team: {
        electrician: {
          name: string,
          last_name: string
        },
        driver: {
          name: string,
          last_name: string
        }
      }
    }[];
  }[] = [];

  @ViewChild('menu') menu: Menu | undefined;
  contextItems: MenuItem[] = [
    {
      label: 'Gerar Relatório Convencional',
      icon: 'pi pi-replay',
      command: () => this.conventionalDataReport(),
    },
    {
      label: 'Gerar Relatório Leds',
      icon: 'pi pi-lightbulb',
      command: () => this.ledDataReport(),
    },
    {
      separator: true
    },
    {
      label: 'Arquivar',
      icon: 'pi pi-folder-open',
      command: () => {
        this.action = "ARCHIVE";
      },
    },
    {
      label: 'Excluir',
      icon: 'pi pi-trash',
      command: () => {
        this.action = "DELETE";
      },
    },
  ];

  maintenanceId: string | null = null;
  currentContractId: number | null = null;
  action: string | null = null;

  openContextMenu(event: MouseEvent, maintenanceId: string, contractId: number) {
    event.preventDefault();
    this.maintenanceId = maintenanceId;
    this.currentContractId = contractId;

    // Abre o menu popup alinhado ao botão clicado
    this.menu?.show(event);
  }

  // Métodos para as ações do menu
  conventionalDataReport() {
    this.loadPdf(this.maintenanceId!!, 'conventional');
  }

  ledDataReport() {
    this.loadPdf(this.maintenanceId!!, 'led');
  }

  actionArchiveOrDelete() {
    this.loading = true;
    const message = this.action === "ARCHIVE" ? "Relatório arquivado com sucesso"
      : "Relatório excluido com sucesso";

    this.reportService.archiveOrDelete(this.maintenanceId!!, this.action!!).subscribe({
      next: () => {
        const index = this.data.findIndex(c => c.contract.contract_id === this.currentContractId);
        if (index !== -1) {
          this.data[index].maintenances = this.data[index].maintenances
            .filter(m => m.maintenance_id !== this.maintenanceId!!);

          // remove o contrato se não restar nenhuma manutenção
          if (this.data[index].maintenances.length === 0) {
            this.data.splice(index, 1);
          }
        }

        this.utilService.showMessage(message, "success", "Lumos - Relatórios")
      },
      error: err => {
        this.utilService.showMessage(err.error.error ?? err.error.message, "info", "Lumos - Relatórios")
        this.loading = false;
        this.action = null;
      },
      complete: () => {
        this.loading = false;
        this.action = null;
      }
    });
  }


  constructor(private reportService: ReportService, private utilService: UtilsService, private title: Title) {
  }

  ngOnInit() {
    this.title.setTitle('Relatórios de manutenção');
    this.loading = true;
    this.loadMaintenances();
  }

  public loadMaintenances() {
    this.reportService.getFinishedMaintenances().subscribe({
      next: (data) => {
        this.data = data
      },
      error: (err) => {
        this.utilService.showMessage(err.error.message || err.error, 'error', 'Erro ao Manutenções finalizadas');
      },
      complete: () => {
        this.loading = false
      }
    });
  }

  public loadPdf(maintenanceId: string, type: string) {
    let desc = '';
    if (type === 'led') desc = 'Led';
    else desc = 'Convencional';

    this.loading = true;
    this.reportService.getMaintenancePdf(maintenanceId, type).subscribe({
      next: (res) => {
        if (this.pdfUrl) {
          URL.revokeObjectURL(this.pdfUrl);  // limpa URL anterior
        }
        this.pdfUrl = URL.createObjectURL(res);
      },
      error: (err) => {
        this.utilService.showMessage(
          `O tipo ${desc} não possui registros. Os dados estão no outro tipo de relatório.`,
          'info',
          'Lumos - Relatórios'
        );
        console.log(err);
        this.loading = false
      },
      complete: () => {
        this.loading = false
      }
    });
  }

  public downloadPdf() {
    if (!this.pdfUrl) return;
    const a = document.createElement('a');
    a.href = this.pdfUrl;
    a.download = `maintenance.pdf`;
    a.click();
  }


}
