import {Component, OnInit} from '@angular/core';
import {Title} from '@angular/platform-browser';
import {SharedState} from '../../core/service/shared-state';
import {ActivatedRoute, Router} from '@angular/router';
import {ExecutionService} from '../execution.service';
import {UtilsService} from '../../core/service/utils.service';
import {ButtonDirective} from 'primeng/button';
import {NgIf} from '@angular/common';
import {LoadingOverlayComponent} from '../../shared/components/loading-overlay/loading-overlay.component';
import {Toast} from 'primeng/toast';
import {TableModule} from 'primeng/table';

@Component({
    selector: 'app-execution-progress',
    standalone: true,
    imports: [
        ButtonDirective,
        NgIf,
        LoadingOverlayComponent,
        Toast,
        TableModule
    ],
    templateUrl: './execution-progress.component.html',
    styleUrl: './execution-progress.component.scss'
})
export class ExecutionProgressComponent implements OnInit {
    statuses: Record<string, any> = {
        'aguardando-estoque': {
            title: 'Aguardando estoque',
            value: 'PENDING'
        },
        'prontas-para-execucao': {
            title: 'Pronta para execuçao',
            value: 'AVAILABLE_EXECUTION'
        },
        'em-execucao': {
            title: 'Em execução',
            value: 'IN_PROGRESS'
        },
        'concluidas': {
            title: 'Concluída',
            value: 'FINISHED'
        },
    };
    status = 'aguardando-estoque';
    executions: any[] = [];
    loading = false;

    constructor(
        private title: Title,
        protected router: Router,
        private route: ActivatedRoute,
        private executionService: ExecutionService,
        private utils: UtilsService,
    ) {
    }

    ngOnInit(): void {
        this.status = this.route.snapshot.paramMap.get('status') ?? 'aguardando-estoque';

        this.title.setTitle(this.statuses[this.status].title);
        SharedState.setCurrentPath(["Ordens de Serviço", this.statuses[this.status].title]);
        this.getExecutions();
    }

    getStatus(status: string) {
        return this.statuses[status];
    }

    getExecutions() {
        this.loading = true;
        this.executionService.getExecutions(this.getStatus(this.status).value).subscribe({
            next: (data) => {
                this.executions = data;
            },
            error: (error) => {
                this.loading = false;
                this.utils.showMessage(error.error.message, 'error');
            },
            complete: () => {
                this.loading = false;
            }
        })
    }


}
