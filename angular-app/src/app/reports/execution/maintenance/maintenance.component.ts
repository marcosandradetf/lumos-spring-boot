import {Component, OnInit, ViewChild} from '@angular/core';
import {SafeUrlPipe} from '../../../safe-url.pipe';
import {ReportService} from '../../report.service';
import {UtilsService} from '../../../core/service/utils.service';
import {Title} from '@angular/platform-browser';
import {LoadingComponent} from '../../../shared/components/loading/loading.component';
import {DatePipe, NgForOf} from '@angular/common';
import {Toast} from 'primeng/toast';
import {Menu} from 'primeng/menu';
import {MenuItem} from 'primeng/api';
import {PrimeConfirmDialogComponent} from '../../../shared/components/prime-confirm-dialog/prime-confirm-dialog.component';
import {AuthService} from '../../../core/auth/auth.service';
import {IconField} from 'primeng/iconfield';
import {InputIcon} from 'primeng/inputicon';
import {InputText} from 'primeng/inputtext';
import {SharedState} from '../../../core/service/shared-state';
import {Utils} from '../../../core/service/utils';
import {Router} from '@angular/router';
import {Calendar} from 'primeng/calendar';
import {FormsModule} from '@angular/forms';

type MaintenanceExecution = {
    execution_id: string;
    streets: string[];
    date_of_visit: string;
    team: {
        name: string;
        last_name: string;
        role: string;
    }[];
};

type MaintenanceContract = {
    contract: {
        contract_id: number;
        contractor: string;
    };
    executions: MaintenanceExecution[];
};

@Component({
    selector: 'app-maintenance',
    standalone: true,
    imports: [
        LoadingComponent,
        NgForOf,
        SafeUrlPipe,
        DatePipe,
        Toast,
        Menu,
        PrimeConfirmDialogComponent,
        IconField,
        InputIcon,
        InputText,
        Calendar,
        FormsModule
    ],
    providers: [SafeUrlPipe],
    templateUrl: './maintenance.component.html',
    styleUrl: './maintenance.component.scss'
})
export class MaintenanceComponent implements OnInit {
    pdfUrl: string | null = null;
    loading = false;
    searchTerm = '';
    startDate: Date | null = null;
    endDate: Date | null = null;
    minStartDateLimit: Date | null = null;
    maxEndDateLimit: Date = this.todayInput();
    readonly maxRangeDays = 90;

    data: MaintenanceContract[] = [];
    dataBackup: MaintenanceContract[] = [];

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
                this.action = 'ARCHIVE';
            },
        },
        {
            label: 'Excluir',
            icon: 'pi pi-trash',
            command: () => {
                this.action = 'DELETE';
            },
        },
    ];

    maintenanceId: string | null = null;
    currentContractId: number | null = null;
    action: string | null = null;

    canShare = false;
    isApple = false;

    constructor(
        private router: Router,
        private reportService: ReportService,
        protected utilService: UtilsService,
        private authService: AuthService,
        private title: Title
    ) {
    }

    ngOnInit() {
        this.title.setTitle('Relatórios de manutenções');
        SharedState.setCurrentPath(['Execuções Realizadas', 'Relatórios de Manutenções (30 dias)']);

        const ua = navigator.userAgent;
        this.isApple = /iPad|iPhone|iPod|Mac/.test(ua);
        this.setDefaultDateRange();

        this.canShare = Utils.isShareAvailable();

        this.loadMaintenances();
    }

    private loadMaintenances() {
        if (!this.startDate || !this.endDate) return;

        this.loading = true;
        this.reportService.getFinishedMaintenances(this.startDate, this.endDate).subscribe({
            next: (data) => {
                this.dataBackup = data;
                this.applyFilters();
            },
            error: (err) => {
                Utils.handleHttpError(err, this.router);
                this.loading = false;
            },
            complete: () => {
                this.loading = false;
            }
        });
    }

    openContextMenu(event: MouseEvent, maintenanceId: string, contractId: number) {
        event.preventDefault();
        this.maintenanceId = maintenanceId;
        this.currentContractId = contractId;
        this.menu?.show(event);
    }

    conventionalDataReport() {
        if (!this.maintenanceId) return;
        this.loadPdf(this.maintenanceId, 'conventional');
    }

    ledDataReport() {
        if (!this.maintenanceId) return;
        this.loadPdf(this.maintenanceId, 'led');
    }

    actionArchiveOrDelete() {
        this.loading = true;

        if (!this.authService.user?.getRoles()?.some(
            r => ['ADMIN', 'RESPONSAVEL_TECNICO', 'ANALISTA'].includes(r)
        )) {
            this.action = null;
            this.loading = false;
            this.utilService.showMessage('Sua função atual no sistema não permite executar essa ação.', 'info', 'Lumos - Relatórios');
            return;
        }

        const message = this.action === 'ARCHIVE' ? 'Relatório arquivado com sucesso'
            : 'Relatório excluido com sucesso';

        this.reportService.archiveOrDelete(this.maintenanceId!, this.action!).subscribe({
            next: () => {
                const backupIndex = this.dataBackup.findIndex(c => c.contract.contract_id === this.currentContractId);
                if (backupIndex !== -1) {
                    this.dataBackup[backupIndex].executions = this.dataBackup[backupIndex].executions
                        .filter(m => m.execution_id !== this.maintenanceId!);

                    if (this.dataBackup[backupIndex].executions.length === 0) {
                        this.dataBackup.splice(backupIndex, 1);
                    }
                }

                this.applyFilters();
                this.utilService.showMessage(message, 'success', 'Lumos - Relatórios');
            },
            error: err => {
                this.utilService.showMessage(err.error.error ?? err.error.message, 'info', 'Lumos - Relatórios');
                this.loading = false;
                this.action = null;
            },
            complete: () => {
                this.loading = false;
                this.action = null;
            }
        });
    }

    pdfBlob: Blob | null = null;
    fileName: string | null = null;
    shareText: string | null = null;

    public loadPdf(maintenanceId: string, type: string) {
        const desc = type === 'led' ? 'Led' : 'Convencional';
        this.loading = true;

        this.reportService.getMaintenancePdf(maintenanceId, type).subscribe({
            next: async (resp) => {
                if (this.pdfUrl) {
                    URL.revokeObjectURL(this.pdfUrl);
                }

                this.pdfBlob = resp.body!;
                this.pdfUrl = URL.createObjectURL(this.pdfBlob);

                const cd = resp.headers.get('content-disposition');
                const contractName = this.dataBackup
                    .find(c => c.contract.contract_id === this.currentContractId)?.contract
                    .contractor;
                this.fileName = this.extractFilename(cd) ??
                    `relatorio_${desc.toLowerCase()}_${Utils.normalizeString(contractName ?? '')}_${Utils.formatNowToDDMMYYHHmm()}.pdf`;
                this.shareText =
                    `Relatório de Manutenção ${type === 'led' ? 'em LEDs' : 'Convencional'}\n` +
                    `${contractName ? `Contrato: ${contractName}\n` : ''}` +
                    `Gerado pelo sistema Lumos às ${Utils.formatNowToDDMMYYHHmm(true)}`;
            },
            error: () => {
                this.utilService.showMessage(
                    `O tipo ${desc} não possui registros.`,
                    'info',
                    'Lumos - Relatórios'
                );
                this.loading = false;
            },
            complete: () => {
                this.loading = false;
            }
        });
    }

    async sharePdf() {
        if (!this.pdfBlob) return;

        const file = new File(
            [this.pdfBlob],
            this.fileName ?? 'relatorio.pdf',
            {type: 'application/pdf'}
        );

        if (Utils.isMobileDevice() && Utils.isShareAvailable()) {
            await Utils.shareMessage(this.shareText ?? 'Relatório de manutenção', {
                title: 'Relatório Lumos',
                subject: 'Relatório Lumos',
                file
            });
        } else {
            this.downloadPdf();
        }
    }

    private extractFilename(cd: string | null): string | null {
        if (!cd) return null;
        const match = cd.match(/filename="?([^"]+)"?/i);
        return match?.[1] ?? null;
    }

    private downloadPdf() {
        if (!this.pdfBlob || !this.fileName) return;

        const url = window.URL.createObjectURL(this.pdfBlob);
        const a = document.createElement('a');
        a.href = url;
        a.download = this.fileName;
        a.target = '_blank';
        a.click();
        window.URL.revokeObjectURL(url);
    }

    clearFilter() {
        this.searchTerm = '';
        this.applyFilters();
    }

    resetDateRange() {
        this.setDefaultDateRange();
        this.loadMaintenances();
    }

    get totalContracts(): number {
        return this.data.length;
    }

    get totalReports(): number {
        return this.data.reduce((sum, contract) => sum + contract.executions.length, 0);
    }

    get rangeSummary(): string {
        if (!this.startDate || !this.endDate) {
            return 'Selecione um período de até 90 dias.';
        }

        const days = this.getRangeDays(this.startDate, this.endDate);
        return `${days} ${days === 1 ? 'dia selecionado' : 'dias selecionados'} de um máximo de ${this.maxRangeDays}.`;
    }

    filterData(event: Event) {
        this.searchTerm = (event.target as HTMLInputElement).value ?? '';
        this.applyFilters();
    }

    onDateChange(field: 'start' | 'end', value: Date | null) {
        if (field === 'start') {
            this.startDate = value;
        } else {
            this.endDate = value;
        }
        this.updateDateBounds();

        if (!this.startDate || !this.endDate) {
            return;
        }

        const start = this.parseDateInput(this.startDate);
        const end = this.parseDateInput(this.endDate);

        if (!start || !end) {
            return;
        }

        if (start > end) {
            if (field === 'start') {
                this.endDate = this.startDate;
            } else {
                this.startDate = this.endDate;
            }
            this.updateDateBounds();
        }

        if (this.getRangeDays(this.startDate, this.endDate) > this.maxRangeDays) {
            if (field === 'start') {
                this.endDate = this.addDaysToInput(this.startDate, this.maxRangeDays - 1);
            } else {
                this.startDate = this.addDaysToInput(this.endDate, -(this.maxRangeDays - 1));
            }
            this.updateDateBounds();

            this.utilService.showMessage(
                'O período máximo permitido é de 90 dias.',
                'info',
                'Lumos - Relatórios'
            );
        }

        this.loadMaintenances();
    }

    private setDefaultDateRange() {
        this.endDate = this.todayInput();
        this.startDate = this.addDaysToInput(this.endDate, -29);
        this.updateDateBounds();
    }

    private updateDateBounds() {
        const today = this.todayInput();
        this.maxEndDateLimit = this.startDate
            ? (() => {
                const capped = this.addDaysToInput(this.startDate, this.maxRangeDays - 1);
                return capped > today ? today : capped;
            })()
            : today;

        this.minStartDateLimit = this.endDate
            ? this.addDaysToInput(this.endDate, -(this.maxRangeDays - 1))
            : null;
    }

    private applyFilters() {
        const normalizedSearch = this.searchTerm.trim().toLowerCase();

        this.data = this.dataBackup
            .map(contract => ({
                ...contract,
                executions: contract.executions.filter(execution => {
                    const matchesSearch = !normalizedSearch ||
                        contract.contract.contractor.toLowerCase().includes(normalizedSearch);

                    return matchesSearch;
                })
            }))
            .filter(contract => contract.executions.length > 0);
    }

    private normalizeDate(value: string): Date | null {
        const parsed = new Date(value);
        if (Number.isNaN(parsed.getTime())) {
            return null;
        }

        parsed.setHours(0, 0, 0, 0);
        return parsed;
    }

    private parseDateInput(value: Date | null): Date | null {
        if (!value) return null;
        const parsed = new Date(value);
        if (Number.isNaN(parsed.getTime())) return null;
        parsed.setHours(0, 0, 0, 0);
        return parsed;
    }

    private formatDateInput(date: Date): string {
        const year = date.getFullYear();
        const month = `${date.getMonth() + 1}`.padStart(2, '0');
        const day = `${date.getDate()}`.padStart(2, '0');
        return `${year}-${month}-${day}`;
    }

    private todayInput(): Date {
        const today = new Date();
        today.setHours(0, 0, 0, 0);
        return today;
    }

    private addDaysToInput(value: Date | null, days: number): Date {
        const base = this.parseDateInput(value) ?? new Date();
        base.setDate(base.getDate() + days);
        base.setHours(0, 0, 0, 0);
        return base;
    }

    private getRangeDays(startValue: Date | null, endValue: Date | null): number {
        const start = this.parseDateInput(startValue);
        const end = this.parseDateInput(endValue);
        if (!start || !end) return 0;

        const diff = end.getTime() - start.getTime();
        return Math.floor(diff / 86400000) + 1;
    }
}
