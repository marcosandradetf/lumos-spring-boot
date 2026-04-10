import {Component, OnInit, ViewChild} from '@angular/core';
import {SafeUrlPipe} from '../../../safe-url.pipe';
import {ReportService} from '../../report.service';
import {UtilsService} from '../../../core/service/utils.service';
import {Title} from '@angular/platform-browser';
import {LoadingComponent} from '../../../shared/components/loading/loading.component';
import {NgForOf} from '@angular/common';
import {Toast} from 'primeng/toast';
import {MenuItem} from 'primeng/api';
import {Menu} from 'primeng/menu';
import {IconField} from 'primeng/iconfield';
import {InputIcon} from 'primeng/inputicon';
import {InputText} from 'primeng/inputtext';
import {SharedState} from '../../../core/service/shared-state';
import {Utils} from '../../../core/service/utils';
import {Router} from '@angular/router';
import {Calendar} from 'primeng/calendar';
import {FormsModule} from '@angular/forms';

type InstallationStep = {
    installation_id: number;
    installation_type: string;
    step: string;
    description: string;
    finished_at?: string;
    date_of_visit?: string;
    created_at?: string;
    updated_at?: string;
    team: {
        name: string;
        last_name: string;
        role: string;
    }[];
};

type InstallationContract = {
    contract: {
        contract_id: number;
        contractor: string;
    };
    steps: InstallationStep[];
};

@Component({
    selector: 'app-installation',
    standalone: true,
    imports: [
        LoadingComponent,
        NgForOf,
        SafeUrlPipe,
        Toast,
        Menu,
        IconField,
        InputIcon,
        InputText,
        Calendar,
        FormsModule
    ],
    templateUrl: './installation.component.html',
    styleUrl: './installation.component.scss'
})
export class InstallationComponent implements OnInit {
    pdfUrl: string | null = null;
    loading = false;
    searchTerm = '';
    startDate: Date | null = null;
    endDate: Date | null = null;
    minStartDateLimit: Date | null = null;
    maxEndDateLimit: Date = this.todayInput();
    readonly maxRangeDays = 90;

    data: InstallationContract[] = [];
    dataBackup: InstallationContract[] = [];

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

    selectedStep: InstallationStep | null = null;
    isApple = false;

    constructor(
        private router: Router,
        private reportService: ReportService,
        protected utilService: UtilsService,
        private title: Title
    ) {
    }

    ngOnInit() {
        SharedState.setCurrentPath(['Execuções Realizadas', 'Relatórios de Instalações']);
        this.title.setTitle('Relatórios de instalações');
        const ua = navigator.userAgent;
        this.isApple = /iPad|iPhone|iPod|Mac/.test(ua);
        this.setDefaultDateRange();

        this.loadInstallations();
    }

    private loadInstallations() {
        if (!this.startDate || !this.endDate) return;

        this.loading = true;
        this.reportService.getFinishedInstallations(this.startDate, this.endDate).subscribe({
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

    openContextMenu(event: MouseEvent, step: InstallationStep) {
        event.preventDefault();
        this.selectedStep = step;
        this.menu?.show(event);
    }

    actionDataReport() {
        if (!this.selectedStep) return;
        this.loadPdf(this.selectedStep, 'data');
    }

    actionPhotoReport() {
        if (!this.selectedStep) return;
        this.loadPdf(this.selectedStep, 'photos');
    }

    actionArchive() {
        this.utilService.showMessage('Recurso não implementado', 'contrast', 'Lumos - Relatórios');
    }

    pdfBlob: Blob | null = null;
    fileName: string | null = null;
    descTitle: string | null = null;

    public loadPdf(execution: InstallationStep, type: string) {
        const desc = type === 'data' ? 'led' : 'fotografico';
        this.descTitle = type === 'data' ? 'Relatório de Instalação de LEDs' : 'Relatório Fotográfico';

        this.loading = true;
        this.reportService.getInstallationPdf(execution, type).subscribe({
            next: (resp) => {
                if (this.pdfUrl) {
                    URL.revokeObjectURL(this.pdfUrl);
                }

                this.pdfBlob = resp.body!;
                this.pdfUrl = URL.createObjectURL(this.pdfBlob);

                const cd = resp.headers.get('content-disposition');
                this.fileName = this.extractFilename(cd) ??
                    `relatorio_${desc}_${Utils.normalizeString(this.selectedStep?.description ?? '')}_etapa_${this.selectedStep?.step}.pdf`;
            },
            error: (err) => {
                this.utilService.showMessage(err.error.message || err.error, 'error', 'Erro ao gerar gerar PDF');
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

        if (navigator.share && navigator.canShare?.({files: [file]})) {
            const shareText =
                `${this.descTitle}\n` +
                `${this.selectedStep?.description ?? ''}` +
                `Gerado pelo sistema Lumos às ${Utils.formatNowToDDMMYYHHmm(true)}`;

            await navigator.share({
                title: 'Relatório Lumos',
                text: shareText,
                files: [file]
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

    downloadPdf() {
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
        this.loadInstallations();
    }

    get totalContracts(): number {
        return this.data.length;
    }

    get totalReports(): number {
        return this.data.reduce((sum, contract) => sum + contract.steps.length, 0);
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

        this.loadInstallations();
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
                steps: contract.steps.filter(step => {
                    const matchesSearch = !normalizedSearch ||
                        contract.contract.contractor.toLowerCase().includes(normalizedSearch);

                    return matchesSearch;
                })
            }))
            .filter(contract => contract.steps.length > 0);
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
