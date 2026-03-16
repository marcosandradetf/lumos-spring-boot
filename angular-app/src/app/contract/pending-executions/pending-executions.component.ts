import {Component, OnInit} from '@angular/core';
import {Router} from '@angular/router';
import {Title} from '@angular/platform-browser';
import {ExecutionService} from '../../executions/execution.service';
import {SharedState} from '../../core/service/shared-state';
import {UtilsService} from '../../core/service/utils.service';
import {TableModule} from 'primeng/table';
import {DatePipe} from '@angular/common';
import {LoadingOverlayComponent} from '../../shared/components/loading-overlay/loading-overlay.component';

@Component({
  selector: 'app-pending-executions',
  standalone: true,
    imports: [
        TableModule,
        DatePipe,
        LoadingOverlayComponent
    ],
  templateUrl: './pending-executions.component.html',
  styleUrl: './pending-executions.component.scss'
})
export class PendingExecutionsComponent implements OnInit {
    executions: any[] = [];
    loading = false;

    constructor(
        private router: Router,
        private title: Title,
        private utils: UtilsService,
        private api: ExecutionService
    ) {
    }

    ngOnInit(): void {
        this.loading = true;
        SharedState.setCurrentPath(["Contratos", "Instalações Pendentes de Vínculo Contratual"]);
        this.title.setTitle("Listando Instalações Pendentes de Vínculo contratual");

        this.api.getInstallationsWaitingValidation().subscribe({
            next: data => {
                this.executions = data;
                this.loading = false;
            },
            error: err => {
                this.utils.showMessage(err.error.message ?? err.error.error, "error");
                this.loading = false;
            }
        });
    }

    protected navigateToLink(directExecutionId: number) {
        void this.router.navigate([`/contratos/validar-execucao/${directExecutionId}`]);
    }
}
