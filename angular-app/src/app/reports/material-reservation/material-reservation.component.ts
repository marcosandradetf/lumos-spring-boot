import {Component, OnInit} from '@angular/core';
import {CommonModule, DatePipe} from '@angular/common';
import {Calendar} from 'primeng/calendar';
import {FormsModule} from '@angular/forms';
import {DropdownModule} from 'primeng/dropdown';
import {SharedState} from '../../core/service/shared-state';
import {Title} from '@angular/platform-browser';
import {Utils} from '../../core/service/utils';
import {Message} from 'primeng/message';
import {UtilsService} from '../../core/service/utils.service';
import {LoadingOverlayComponent} from '../../shared/components/loading-overlay/loading-overlay.component';
import {ReportService} from '../report.service';
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
        Message,
        LoadingOverlayComponent,
        Toast,
        SafeUrlPipe,
    ],
    templateUrl: './material-reservation.component.html',
    styleUrl: './material-reservation.component.scss'
})
export class MaterialReservationComponent implements OnInit {
    filters: {
        contractId: number | null;
        startDate: Date;
        endDate: Date;
    } = {
        contractId: null,
        startDate: new Date(new Date().setMonth(new Date().getMonth() - 3)),
        endDate: new Date(),
    };

    contracts: any[] = [];
    filteredContracts: any[] = [];

    constructor(
        private title: Title,
        private utils: UtilsService,
        private reportService: ReportService
    ) {
    }

    ngOnInit(): void {
        SharedState.setCurrentPath(["Relatórios", "Estoque", "Saída/Saldo por instalação"]);
        this.title.setTitle("Relatório de Saída/Saldo por instalação");
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
                    .filter(c => c.type === 'INSTALLATION');
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

        if (!this.filters.startDate || !this.filters.endDate) {
            return;
        }

        const days = Utils.diffInDays(this.filters.startDate!, this.filters.endDate!);

        if (days > 93) {
            this.utils.showMessage("O Período máximo é de 93 dias.", 'warn', 'Período inválido');
            return;
        }

        this.loading = true;
        this.reportService.getMaterialReservationReport(this.filters).subscribe({
            next: (resp) => {
                // limpa URL antiga
                if (this.pdfUrl) {
                    URL.revokeObjectURL(this.pdfUrl);
                }

                // blob
                this.pdfBlob = resp.body!;
                this.pdfUrl = URL.createObjectURL(this.pdfBlob);

                this.shareText =
                    `Relatório de Saída de Estoque por Instalação\n` +
                    `Gerado pelo sistema Lumos às ${Utils.formatNowToDDMMYYHHmm(true)}`;

                this.showMenu = false;
                this.loading = false;

            },
            error: (error) => {
                this.utils.showMessage(
                    'Nenhum dado encontrado no período informado',
                    'info',
                );
                this.loading = false;
            },
            complete: () => {
                this.loading = false;
            }
        });

    }

    resetFilters() {
        this.filters = {
            contractId: null,
            startDate: new Date(new Date().setMonth(new Date().getMonth() - 3)),
            endDate: new Date(),
        };
        this.filteredContracts = this.contracts
            .filter(c => c.type === "INSTALLATION");
        this.submitted = false;
    }

    protected readonly Utils = Utils;

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

    protected revokeUrl() {
        if (this.pdfUrl) {
            URL.revokeObjectURL(this.pdfUrl);
            this.pdfUrl = null;
        }

        this.showMenu = true
    }
}
