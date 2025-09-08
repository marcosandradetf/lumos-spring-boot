import {Component, OnInit, ViewChild} from '@angular/core';
import {SafeUrlPipe} from '../../safe-url.pipe';
import {ReportService} from '../report.service';
import {UtilsService} from '../../core/service/utils.service';
import {Title} from '@angular/platform-browser';
import {PrimeBreadcrumbComponent} from '../../shared/components/prime-breadcrumb/prime-breadcrumb.component';
import {LoadingComponent} from '../../shared/components/loading/loading.component';
import {DatePipe, NgForOf, NgIf} from '@angular/common';
import {Toast} from 'primeng/toast';
import {ContextMenu} from 'primeng/contextmenu';
import {MenuItem} from 'primeng/api';
import {Button} from 'primeng/button';
import {Menu} from 'primeng/menu';
import {IconField} from 'primeng/iconfield';
import {InputIcon} from 'primeng/inputicon';
import {InputText} from 'primeng/inputtext';

@Component({
  selector: 'app-installation',
  standalone: true,
  imports: [
    PrimeBreadcrumbComponent,
    LoadingComponent,
    NgForOf,
    SafeUrlPipe,
    Toast,
    Menu,
    IconField,
    InputIcon,
    InputText
  ],
  templateUrl: './installation.component.html',
  styleUrl: './installation.component.scss'
})
export class InstallationComponent implements OnInit {
  pdfUrl: string | null = null;
  loading = false;

  data: {
    contract: {
      contract_id: number;
      contractor: string;
    },
    steps: {
      direct_execution_id: number;
      step: string;
      description: string;
      type: string;
      team: {
        name: string;
        last_name: string;
        role: string;
      }[];
    }[];
  }[] = [];

  dataBackup: {
    contract: {
      contract_id: number;
      contractor: string;
    },
    steps: {
      direct_execution_id: number;
      step: string;
      description: string;
      type: string;
      team: {
        name: string;
        last_name: string;
        role: string;
      }[];
    }[];
  }[] = [];

  @ViewChild('menu') menu: Menu | undefined;

  contextItems: MenuItem[] = [
    {
      label: 'Gerar Relatório Comum',
      icon: 'pi pi-file',
      command: () => this.actionDataReport(),
    },
    {
      label: 'Gerar Relatório Fotográfico',
      icon: 'pi pi-camera',
      command: () => this.actionPhotoReport(),
    },
    {
      separator: true
    },
    {
      label: 'Arquivar',
      icon: 'pi pi-folder-open',
      command: () => this.actionArchive(),
    },
  ];

  selectedStep: any = null;

  openContextMenu(event: MouseEvent, step: any) {
    event.preventDefault();
    this.selectedStep = step;

    // Abre o menu popup alinhado ao botão clicado
    this.menu?.show(event);
  }

  // Métodos para as ações do menu
  actionDataReport() {
    this.loadPdf(this.selectedStep.direct_execution_id, 'data');
  }

  actionPhotoReport() {
    this.loadPdf(this.selectedStep.direct_execution_id, 'photos');
  }

  actionArchive() {
    this.utilService.showMessage("Recurso não implementado", "contrast", "Lumos - Relatórios")
  }


  constructor(private reportService: ReportService, protected utilService: UtilsService, private title: Title) {
  }

  ngOnInit() {
    this.title.setTitle('Relatórios de manutenção');
    this.loading = true;
    this.loadInstallations();
  }

  public loadInstallations() {
    this.reportService.getFinishedInstallations().subscribe({
      next: (data) => {
        this.data = data;
        this.dataBackup = data;
      },
      error: (err) => {
        this.utilService.showMessage(err.error.message || err.error, 'error', 'Erro ao Manutenções finalizadas');
      },
      complete: () => {
        this.loading = false
      }
    });
  }

  public loadPdf(executionId: number, type: string) {
    this.loading = true;
    this.reportService.getInstallationPdf(executionId, type).subscribe({
      next: (res) => {
        if (this.pdfUrl) {
          URL.revokeObjectURL(this.pdfUrl);  // limpa URL anterior
        }
        this.pdfUrl = URL.createObjectURL(res);
      },
      error: (err) => {
        this.utilService.showMessage(err.error.message || err.error, 'error', 'Erro ao gerar gerar PDF');
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


  filterData(event: Event) {
    let value = (event.target as HTMLInputElement).value;

    if (value === null || value === undefined || value === '') {
      this.data = this.dataBackup;
    }

    this.data = this.dataBackup.filter(d => d.contract.contractor.toLowerCase().includes(value.toLowerCase()));
  }

}
