import {Component, OnInit} from '@angular/core';
import {CommonModule, DatePipe, NgClass} from '@angular/common';
import {Calendar} from 'primeng/calendar';
import {FormsModule} from '@angular/forms';
import {DropdownModule} from 'primeng/dropdown';
import {SelectButton} from 'primeng/selectbutton';
import {SharedState} from '../../core/service/shared-state';
import {Title} from '@angular/platform-browser';
import {Utils} from '../../core/service/utils';
import {Message} from 'primeng/message';
import {UtilsService} from '../../core/service/utils.service';
import {LoadingOverlayComponent} from '../../shared/components/loading-overlay/loading-overlay.component';
import {ReportService} from '../report.service';
import {HttpResponse} from '@angular/common/http';
import {Toast} from 'primeng/toast';
import {SafeUrlPipe} from '../../safe-url.pipe';

@Component({
    selector: 'app-report-manage',
    standalone: true,
    imports: [
        CommonModule,
        Calendar,
        FormsModule,
        DropdownModule,
        SelectButton,
        DatePipe,
        Message,
        LoadingOverlayComponent,
        Toast,
        SafeUrlPipe,
    ],
    templateUrl: './report-manage.component.html',
    styleUrl: './report-manage.component.scss'
})
export class ReportManageComponent implements OnInit {
    serviceScopes = [
        {label: 'Manutenção', value: 'MAINTENANCE'},
        {label: 'Instalação', value: 'INSTALLATION'}
    ];

    selectedScope = 'MAINTENANCE';

    viewModes = [
        {label: 'Lista', value: 'LIST'},
        {label: 'Agrupado', value: 'GROUP'}
    ];


    filters: {
        contractId: number | null;
        type: string | null;
        startDate: Date | null;
        endDate: Date | null;
        viewMode: 'LIST' | 'GROUP';
        scope: 'MAINTENANCE' | 'INSTALLATION';
        executionId: string | null;
    } = {
        contractId: null,
        type: null,
        startDate: null,
        endDate: null,
        viewMode: 'LIST',
        scope: 'MAINTENANCE',
        executionId: null
    };

    contracts: any[] = [];
    filteredContracts: any[] = [];

    serviceTypes: Record<string, any[]> = {
        "MAINTENANCE": [
            {label: 'Manutenção Convencional', value: 'lampada'},
            {label: 'Manutenção em Leds', value: 'led'},
        ],
        "INSTALLATION": [
            {label: 'Instalação de LEDs', value: 'data'},
            {label: 'Relatório fotográfico', value: 'photo'},
        ]
    }

    classificationClasses: Record<string, string> = {
        'Ação imediata':
            'border-red-500/40 bg-red-50 text-red-700 dark:border-red-400/40 dark:bg-red-500/10 dark:text-red-300',
        'Crítico':
            'border-orange-500/40 bg-orange-50 text-orange-700 dark:border-orange-400/40 dark:bg-orange-500/10 dark:text-orange-300',
        'Atenção':
            'border-yellow-500/40 bg-yellow-50 text-yellow-800 dark:border-yellow-400/40 dark:bg-yellow-500/10 dark:text-yellow-300',
        'Monitorar':
            'border-sky-500/30 bg-sky-50 text-sky-700 dark:border-sky-400/30 dark:bg-sky-500/10 dark:text-sky-300'
    };

    results: {
        contract: {
            contract_id: number,
            contractor: string,
        },
        maintenances: {
            maintenance_id: string,
            streets: string[],
            date_of_visit: Date,
            sign_date: Date,
            team: {
                name: string,
                last_name: string,
                role: string,
            }[]
        }[]
    }[] = [{
        contract: {
            contract_id: 0,
            contractor: ''
        },
        maintenances: []
    }];

    constructor(
        private title: Title,
        private utils: UtilsService,
        private reportService: ReportService
    ) {
    }

    ngOnInit(): void {
        SharedState.setCurrentPath(["Execuções Realizadas", "Relatórios Personalizados"]);
        this.title.setTitle("Gerenciamento de Execuções");
        this.loading = true

        const ua = navigator.userAgent;
        this.isAndroid = /Android/i.test(ua);
        this.isApple = /iPhone|iPad|iPod|Macintosh/i.test(ua);

        this.reportService.getContracts().subscribe({
            next: (data) => {
                data.forEach(c => {
                    this.contracts.push({
                        contractId: c.contractId,
                        contractor: Utils.abbreviate(c.contractor),
                        type: c.type,
                        contractNumber: c.contractNumber
                    })
                });
                this.filteredContracts = this.contracts
                    .filter(c => c.type === this.filters.scope);
            },
            error: (err) => {
                this.loading = false;
                this.utils.showMessage(err.error.message ?? err.error.error, "error", "Erro ao buscar contratos");
            },
            complete: () => {
                this.loading = false;
            }
        })
    }

    submitted = false;
    loading = false;
    showMenu = true;
    isApple = false;
    isAndroid = false;

    applyFilters() {
        this.submitted = true;

        if (!this.filters.startDate || !this.filters.endDate || !this.filters.contractId || !this.filters.type) {
            return;
        }

        const days = Utils.diffInDays(this.filters.startDate!, this.filters.endDate!);

        if (days > 31) {
            this.utils.showMessage("O Período máximo é de 31 dias.", 'warn', 'Período inválido');
            return;
        }

        if (this.filters.scope == 'INSTALLATION') {
            this.utils.showMessage("Relatórios personalizados de instalações em desenvolvimento.", 'warn', 'Lumos Relatórios');
            return;
        }

        this.loading = true;
        let type = this.filters.type ?? '';
        type = type === 'led' ? 'led' : 'convencional';
        this.reportService.getReport(this.filters).subscribe({
            next: (resp) => {
                if (resp instanceof HttpResponse) {
                    // limpa URL antiga
                    if (this.pdfUrl) {
                        URL.revokeObjectURL(this.pdfUrl);
                    }

                    // blob
                    this.pdfBlob = resp.body!;
                    this.pdfUrl = URL.createObjectURL(this.pdfBlob);

                    // filename
                    const cd = resp.headers.get('content-disposition');
                    const contractName = this.results[0].contract.contractor;
                    const startsName = this.filters.executionId === null ? "relatorio_" : "relatorio_mensal_";
                    this.fileName = `${startsName}${type.toLowerCase()}_${Utils.normalizeString(contractName ?? '')}_${Utils.formatNowToDDMMYYHHmm()}.pdf`;

                    this.shareText =
                        `Relatório de Manutenção ${type === 'led' ? 'em LEDs' : 'Convencional'}\n` +
                        `${contractName ? `Contrato: ${contractName}\n` : ''}` +
                        `Gerado pelo sistema Lumos às ${Utils.formatNowToDDMMYYHHmm(true)}`;

                    this.showMenu = false;
                    this.loading = false;

                } else {
                    // JSON
                    if (resp.length === 0) {
                        this.utils.showMessage("Não existe nenhum relatório para os filtros selecionados", "warn", "Atenção");
                        return;
                    }

                    this.showMenu = false;
                    this.results = resp;
                    this.loading = false;
                }

            },
            error: (error) => {
                this.utils.showMessage(
                    error.error.message ?? error.error.error,
                    'error',
                    'Erro'
                );
                this.loading = false;
            },
            complete: () => {
                this.loading = false;
            }
        });


    }

    resetFilters(scope: "MAINTENANCE" | "INSTALLATION") {
        this.filters = {
            contractId: null,
            type: null,
            startDate: null,
            endDate: null,
            viewMode: 'LIST',
            scope: scope,
            executionId: null
        };
        this.filteredContracts = this.contracts
            .filter(c => c.type === this.filters.scope);
        this.submitted = false;
    }

    protected readonly Utils = Utils;

    protected getType() {
        return this.serviceTypes[this.filters.scope].find(t => t.value === this.filters.type)?.label ?? '';
    }

    protected getContractNumber() {
        return this.filteredContracts.find(c => c.contractId === this.filters.contractId)?.contractNumber ?? '';
    }

    pdfBlob: Blob | null = null;
    fileName: string | null = null;
    shareText: string | null = null;
    pdfUrl: string | null = null;

    async sharePdf() {
        if (!this.pdfBlob) return;

        const file = new File(
            [this.pdfBlob],
            this.fileName ?? 'relatorio.pdf',
            { type: 'application/pdf' }
        );

        if (this.isApple || this.isAndroid) {
            await navigator.share({
                title: 'Relatório Lumos',
                text: this.shareText ?? 'Relatório de manutenção',
                files: [file]
            });
        } else {
            // fallback
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
        a.target = '_blank'; // 👈 ESSENCIAL no iOS
        a.click();
        window.URL.revokeObjectURL(url);
    }

    protected generateIndividualPdf(maintenanceId: string) {
        this.filters.executionId = maintenanceId;
        this.filters.viewMode = 'GROUP';
        this.applyFilters();
    }

    protected revokeUrl() {
        if (this.pdfUrl) {
            URL.revokeObjectURL(this.pdfUrl);
            this.pdfUrl = null;
        }
    }
}
