import {Component, OnInit} from '@angular/core';
import {SafeUrlPipe} from '../../safe-url.pipe';
import {ReportService} from '../report.service';
import {UtilsService} from '../../core/service/utils.service';
import {Title} from '@angular/platform-browser';
import {PrimeBreadcrumbComponent} from '../../shared/components/prime-breadcrumb/prime-breadcrumb.component';
import {LoadingComponent} from '../../shared/components/loading/loading.component';
import {DatePipe, NgForOf, NgIf} from '@angular/common';
import {Toast} from 'primeng/toast';

@Component({
  selector: 'app-maintenance',
  standalone: true,
  imports: [
    PrimeBreadcrumbComponent,
    LoadingComponent,
    NgForOf,
    SafeUrlPipe,
    NgIf,
    DatePipe,
    Toast
  ],
  providers: [SafeUrlPipe],
  templateUrl: './maintenance.component.html',
  styleUrl: './maintenance.component.scss'
})
export class MaintenanceComponent implements OnInit {
  pdfUrl: string | null = null;
  loading = false;

  data: {
    maintenance: {
      maintenance_id: string,
      type: string,
      streets: [],
      contractor: string,
      date_of_visit: string,
    },
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
  }[] = [];

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

  public loadPdf(maintenanceId: string, streetIds: number[], type: string) {
    let report = ""
    if (type.includes('Convencional')) {
      report = 'conventional'
    } else {
      report = 'led'
    }

    this.loading = true;
    this.reportService.getPDF(maintenanceId, streetIds, report).subscribe({
      next: (res) => {
        if (this.pdfUrl) {
          URL.revokeObjectURL(this.pdfUrl);  // limpa URL anterior
        }
        this.pdfUrl = URL.createObjectURL(res);
      },
      error: (err) => {
        this.utilService.showMessage(err.error.message || err.error, 'error', 'Erro ao gerar gerar PDF');
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
