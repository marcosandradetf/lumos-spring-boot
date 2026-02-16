import {Component, OnInit} from '@angular/core';
import {Title} from '@angular/platform-browser';
import {SharedState} from '../../core/service/shared-state';
import {ActivatedRoute, Router} from '@angular/router';
import {ExecutionService} from '../execution.service';
import {UtilsService} from '../../core/service/utils.service';
import {DatePipe, NgClass, NgIf} from '@angular/common';
import {LoadingOverlayComponent} from '../../shared/components/loading-overlay/loading-overlay.component';
import {Toast} from 'primeng/toast';
import {TableModule} from 'primeng/table';
import {DropdownModule} from 'primeng/dropdown';
import {FormsModule} from '@angular/forms';
import {EditableOutputComponent} from '../../shared/components/editable-cell/editable-output.component';
import {UserService} from '../../manage/user/user-service.service';
import {forkJoin, switchMap} from 'rxjs';
import {TeamService} from '../../manage/team/team-service.service';
import {ButtonDirective} from 'primeng/button';
import {cloneDeep, isEqual, keyBy} from 'lodash';
import {SkeletonTableComponent} from '../../shared/components/skeleton-table/skeleton-table.component';
import {LoadingComponent} from '../../shared/components/loading/loading.component';
import {Tooltip} from 'primeng/tooltip';
import {Badge} from 'primeng/badge';
import {Tag} from 'primeng/tag';

@Component({
    selector: 'app-execution-progress',
    standalone: true,
    imports: [
        NgIf,
        Toast,
        TableModule,
        DatePipe,
        DropdownModule,
        FormsModule,
        EditableOutputComponent,
        ButtonDirective,
        NgClass,
        SkeletonTableComponent,
        Tooltip,
        Badge,
        Tag
    ],
    templateUrl: './execution-progress.component.html',
    styleUrl: './execution-progress.component.scss'
})
export class ExecutionProgressComponent implements OnInit {
    statuses: Record<string, any> = {
        'analise-estoque': {
            title: 'Em Análise de Estoque',
            value: 'WAITING_STOCKIST'
        },
        'aguardando-coleta': {
            title: 'Aguardando Coleta',
            value: 'WAITING_COLLECT'
        },
        'prontas-para-execucao': {
            title: 'Prontas para Execuçao',
            value: 'AVAILABLE_EXECUTION'
        },
        'em-execucao': {
            title: 'Em execução',
            value: 'IN_PROGRESS'
        },
        'concluidas': {
            title: 'Concluídas',
            value: 'FINISHED'
        },
        'WAITING_STOCKIST': {
            title: 'Aguardando Estoquista'
        },
        'AVAILABLE_EXECUTION': {
            title: 'Disponível para Execução'
        }
    };
    status = 'aguardando-estoque';
    executions: any[] = [];
    executionsBackup: any[] = [];
    loading = false;
    users: any[] = [];
    teams: any[] = [];

    constructor(
        private title: Title,
        protected router: Router,
        private route: ActivatedRoute,
        private executionService: ExecutionService,
        private utils: UtilsService,
        private userService: UserService,
        private teamService: TeamService,
    ) {
    }

    ngOnInit(): void {
        this.route.paramMap
            .pipe(
                switchMap(params => {
                    this.status = params.get('status') ?? 'aguardando-estoque';
                    const statusConfig = this.statuses[this.status];

                    this.title.setTitle('O.S - ' + statusConfig.title);
                    SharedState.setCurrentPath(["Ordens de Serviço", statusConfig.title]);

                    this.loading = true;

                    return forkJoin({
                        executions: this.executionService.getExecutions(statusConfig.value),
                        users: this.userService.getUsers(),
                        teams: this.teamService.getTeams()
                    });
                })
            )
            .subscribe({
                next: ({executions, users, teams}) => {
                    this.executions = executions;
                    this.executionsBackup = cloneDeep(this.executions);
                    this.users.push(...users.map(u => ({userId: u.userId, name: `${u.name} ${u.lastname}`})));
                    this.teams = teams;
                    this.loading = false;
                },
                error: (err) => {
                    this.loading = false;
                    this.utils.showMessage(err?.error?.message ?? 'Erro inesperado', 'error');
                }
            });
    }

    protected getUser(userId: string) {
        const user = this.users.find(u => u.userId === userId);
        if (user) {
            return user.name;
        }
        return 'Usuário Inativo';
    }

    protected getTeam(teamId: string) {
        const team = this.teams.find(t => t.idTeam === teamId);
        if (team) {
            return team.teamName;
        }
        return 'Equipe Inativa';
    }

    protected cancelChange(execution: any) {
        const original = this.executionsBackup
            .find(b => b.reservationManagementId === execution.reservationManagementId);
        if (original) {
            const index = this.executions
                .findIndex(e => e.reservationManagementId === execution.reservationManagementId);
            if (index !== -1) {
                this.executions[index] = original;
            }
        }
    }

    protected saveChanges(execution: any) {
        execution._saving = true;
        if (execution._updated) {
            this.executionService.updateManagement(
                execution.reservationManagementId,
                execution.userId,
                execution.teamId
            ).subscribe({
                next: () => {
                    this.executionsBackup = cloneDeep(this.executions);
                    this.utils.showMessage('O.S Atualizada com sucesso', "success");
                    execution._saving = false;
                },
                error: (err) => {
                    this.utils.showMessage(err?.error?.message ?? 'Erro inesperado', 'error');
                    execution._saving = false;
                }
            });
        } else {
            this.executionService.deleteManagement(
                this.getStatus(),
                execution.reservationManagementId
            ).subscribe({
                next: () => {
                    this.executions = this.executions
                        .filter(e => e.reservationManagementId !== execution.reservationManagementId);
                    this.executionsBackup = cloneDeep(this.executions);
                    this.utils.showMessage('O.S Excluída com sucesso', "success");
                    execution._saving = false;
                },
                error: (err) => {
                    this.utils.showMessage(err?.error?.message ?? 'Erro inesperado', 'error');
                    execution._saving = false;
                }
            });
        }
    }

    protected getStatus() {
        return this.statuses[this.status].value ?? '';
    }
}
