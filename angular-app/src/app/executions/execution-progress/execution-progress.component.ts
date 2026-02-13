import {Component, OnInit} from '@angular/core';
import {Title} from '@angular/platform-browser';
import {SharedState} from '../../core/service/shared-state';
import {ActivatedRoute, Router} from '@angular/router';
import {ExecutionService} from '../execution.service';
import {UtilsService} from '../../core/service/utils.service';
import {DatePipe, NgIf} from '@angular/common';
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

@Component({
    selector: 'app-execution-progress',
    standalone: true,
    imports: [
        NgIf,
        LoadingOverlayComponent,
        Toast,
        TableModule,
        DatePipe,
        DropdownModule,
        FormsModule,
        EditableOutputComponent,
        ButtonDirective
    ],
    templateUrl: './execution-progress.component.html',
    styleUrl: './execution-progress.component.scss'
})
export class ExecutionProgressComponent implements OnInit {
    statuses: Record<string, any> = {
        'aguardando-estoque': {
            title: 'Aguardando estoque',
            value: 'WAITING_STOCKIST'
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
        'WAITING_STOCKIST': {
            title: 'Aguardando estoquista'
        },
        'AVAILABLE_EXECUTION': {
            title: 'Disponível para execução'
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

                    this.title.setTitle(statusConfig.title);
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
                    this.executionsBackup = cloneDeep(executions);
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

    protected hasDiff() {
        return !isEqual(this.executions, this.executionsBackup);
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

    protected updateExecutions() {
        this.loading = true;
        const changed = this.buildDiff();
        this.executionService.updateManagements(
            {
                deleted: changed.deleted,
                updates: changed.updated.map(exec => ({
                    reservationManagementId: exec.reservationManagementId,
                    userId: exec.userId,
                    teamId: exec.teamId
                }))
            }
        ).subscribe({
            next: () => {
                this.executionsBackup = cloneDeep(this.executions);
                this.loading = false;
            },
            error: (err) => {
                this.utils.showMessage(err?.error?.message ?? 'Erro inesperado', 'error');
                this.loading = false;
            }
        });
    }

    private buildDiff() {

        const backupMap = keyBy(this.executionsBackup, 'reservationManagementId');

        const updated = this.executions.filter(exec => {
            const original = backupMap[exec.reservationManagementId] ?? [];
            return original && (exec.userId !== original.userId || exec.teamId !== original.teamId);
        });

        const deleted = this.executionsBackup
            .filter(b => !this.executions.some(e => e.executionId === b.executionId))
            .map(d => d.executionId);

        return {updated, deleted};
    }

    protected cancelChanges() {
        this.executions = cloneDeep(this.executionsBackup);
    }
}
