import {Component, OnInit} from '@angular/core';
import {Calendar} from 'primeng/calendar';
import {NgIf} from '@angular/common';
import {DropdownModule} from 'primeng/dropdown';
import {LoadingOverlayComponent} from '../../../shared/components/loading-overlay/loading-overlay.component';
import {Message} from 'primeng/message';
import {PrimeTemplate} from 'primeng/api';
import {SafeUrlPipe} from '../../../safe-url.pipe';
import {Toast} from 'primeng/toast';
import {FormsModule} from '@angular/forms';
import {Utils} from '../../../core/service/utils';
import {MultiSelect} from 'primeng/multiselect';
import {Title} from '@angular/platform-browser';
import {SharedState} from '../../../core/service/shared-state';
import {StockService} from '../../../stock/services/stock.service';
import {forkJoin} from 'rxjs';
import {UtilsService} from '../../../core/service/utils.service';
import {ReportService} from '../../report.service';

@Component({
    selector: 'app-operation',
    standalone: true,
    imports: [
        Calendar,
        DropdownModule,
        LoadingOverlayComponent,
        Message,
        NgIf,
        PrimeTemplate,
        SafeUrlPipe,
        Toast,
        FormsModule,
        MultiSelect
    ],
    templateUrl: './operation.component.html',
    styleUrl: './operation.component.scss'
})
export class OperationComponent implements OnInit {
    protected loading: boolean = false;
    protected showMenu: boolean = true;
    protected pdfUrl: string | null = null;
    protected submitted: boolean = false;
    protected isApple: boolean = false;
    protected isAndroid: boolean = false;
    protected pdfBlob: Blob | null = null;
    protected fileName: string | null = null;
    protected shareText: string | null = null;

    contracts: any[] = [];
    types: any[] = [];
    brands: any[] = [];
    orientations: {
        value: 'portrait' | 'landscape';
        label: 'Retrato' | 'Paisagem';
    }[] = [
        {
            value: 'portrait',
            label: 'Retrato'
        },
        {
            value: 'landscape',
            label: 'Paisagem'
        }
    ];

    filters: {
        contractIds: number[];
        materialTypesIds: number[];
        materialBrands: number[];
        startDate: Date;
        endDate: Date;
        orientation: 'portrait' | 'landscape';
    } = {
        contractIds: [],
        materialTypesIds: [],
        materialBrands: [],
        startDate: new Date(new Date().getFullYear(), new Date().getMonth(), 1),
        endDate: new Date(),
        orientation: 'portrait'
    };

    constructor(
        private title: Title,
        private stockService: StockService,
        private utils: UtilsService,
        private reportService: ReportService
    ) {
    }

    ngOnInit() {
        this.title.setTitle("Relatório Analítico de Operações");
        SharedState.setCurrentPath(['Relatórios', 'Execuções', 'Analítico de Operações']);

        forkJoin({
            types: this.stockService.findAllTypeSubtype(),
            contracts: this.reportService.getContracts(),
            brands: this.stockService.getBrands(),
        }).subscribe({
            next: ({types, contracts, brands}) => {
                this.types = types;
                this.contracts = Array.from(
                    new Map(contracts.map(c => [c.contractId, c])).values()
                );
                this.brands = brands;
                this.loading = false;
            },
            error: err => {
                this.loading = false;
                this.utils.showMessage(err.error.message ?? err.error.error, 'error');
            }
        });
    }

    protected revokeUrl() {
        this.showMenu = true;
        this.pdfUrl = null;
        this.pdfBlob = null;
    }

    protected resetFilters() {
        this.filters = {
            contractIds: [],
            materialTypesIds: [],
            materialBrands: [],
            startDate: new Date(new Date().getFullYear(), new Date().getMonth(), 1),
            endDate: new Date(),
            orientation: "portrait"
        };
    }

    protected applyFilters() {
        this.submitted = true;

        if (!this.filters.startDate || !this.filters.endDate || this.filters.contractIds.length === 0) {
            return;
        }

        const days = Utils.diffInDays(this.filters.startDate!, this.filters.endDate!);

        if (days > 93) {
            this.utils.showMessage("O Período máximo é de 3 meses.", 'warn', 'Período inválido');
            return;
        }

        this.loading = true;
        this.reportService.generateOperationalReport(this.filters).subscribe({
            next: (resp) => {
                // limpa URL antiga
                if (this.pdfUrl) {
                    URL.revokeObjectURL(this.pdfUrl);
                }

                // blob
                this.pdfBlob = resp.body!;
                this.pdfUrl = URL.createObjectURL(this.pdfBlob);

                this.fileName = `relatorio_analitico_operacional_${Utils.formatNowToDDMMYYHHmm()}.pdf`;

                this.shareText =
                    `Relatório Analítico Operacional\n` +
                    `Gerado pelo sistema Lumos às ${Utils.formatNowToDDMMYYHHmm(true)}`;

                this.showMenu = false;
                this.loading = false;

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
            // fallback
            this.downloadPdf();
        }
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

    protected readonly Utils = Utils;
}
