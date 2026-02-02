import {Component, OnInit, ViewChild} from '@angular/core';
import {SafeUrlPipe} from '../../safe-url.pipe';
import {ReportService} from '../report.service';
import {UtilsService} from '../../core/service/utils.service';
import {Title} from '@angular/platform-browser';
import {LoadingComponent} from '../../shared/components/loading/loading.component';
import {NgForOf} from '@angular/common';
import {Toast} from 'primeng/toast';
import {MenuItem} from 'primeng/api';
import {Menu} from 'primeng/menu';
import {IconField} from 'primeng/iconfield';
import {InputIcon} from 'primeng/inputicon';
import {InputText} from 'primeng/inputtext';
import {SharedState} from '../../core/service/shared-state';
import {Utils} from '../../core/service/utils';

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
        InputText
    ],
    templateUrl: './installation.component.html',
    styleUrl: './installation.component.scss'
})
export class InstallationComponent implements OnInit {
    pdfUrl: string | null = null;
    loading = false;

    data: {
        contract: {
            contract_id: number;
            contractor: string;
        },
        steps: {
            direct_execution_id: number;
            step: string;
            description: string;
            type: string;
            team: {
                name: string;
                last_name: string;
                role: string;
            }[];
        }[];
    }[] = [];

    dataBackup: {
        contract: {
            contract_id: number;
            contractor: string;
        },
        steps: {
            direct_execution_id: number;
            step: string;
            description: string;
            type: string;
            team: {
                name: string;
                last_name: string;
                role: string;
            }[];
        }[];
    }[] = [];

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

    selectedStep: any = null;
    isApple = false;

    openContextMenu(event: MouseEvent, step: any) {
        event.preventDefault();
        this.selectedStep = step;

        // Abre o menu popup alinhado ao botão clicado
        this.menu?.show(event);
    }

    // Métodos para as ações do menu
    actionDataReport() {
        this.loadPdf(this.selectedStep.direct_execution_id, 'data');
    }

    actionPhotoReport() {
        this.loadPdf(this.selectedStep.direct_execution_id, 'photos');
    }

    actionArchive() {
        this.utilService.showMessage("Recurso não implementado", "contrast", "Lumos - Relatórios")
    }


    constructor(private reportService: ReportService, protected utilService: UtilsService, private title: Title) {
    }

    ngOnInit() {
        SharedState.setCurrentPath(["Execuções Realizadas", "Relatórios de Instalações (90 dias)"]);
        this.title.setTitle('Relatórios de instalações');
        const ua = navigator.userAgent;
        this.isApple = /iPad|iPhone|iPod|Mac/.test(ua);

        this.loading = true;
        this.loadInstallations();
    }

    public loadInstallations() {
        this.reportService.getFinishedInstallations().subscribe({
            next: (data) => {
                this.data = data;
                this.dataBackup = data;
            },
            error: (err) => {
                this.utilService.showMessage(err.error.message || err.error, 'error', 'Erro ao Manutenções finalizadas');
            },
            complete: () => {
                this.loading = false
            }
        });
    }

    pdfBlob: Blob | null = null;
    fileName: string | null = null;
    descTitle: string | null = null;
    public loadPdf(executionId: number, type: string) {
        const desc = type === 'data' ? 'led' : 'fotografico';
        this.descTitle = type === 'data' ? 'Relatório de Instalação de LEDs' : 'Relatório Fotográfico';

        this.loading = true;
        this.reportService.getInstallationPdf(executionId, type).subscribe({
            next: (resp) => {
                // limpa URL antiga
                if (this.pdfUrl) {
                    URL.revokeObjectURL(this.pdfUrl);
                }

                // blob
                this.pdfBlob = resp.body!;
                this.pdfUrl = URL.createObjectURL(this.pdfBlob);

                // filename
                const cd = resp.headers.get('content-disposition');
                this.fileName = this.extractFilename(cd) ??
                    `relatorio_${desc}_${Utils.normalizeString(this.selectedStep.description ?? '')}_etapa_${this.selectedStep.step}.pdf`;
            },
            error: (err) => {
                this.utilService.showMessage(err.error.message || err.error, 'error', 'Erro ao gerar gerar PDF');
                this.loading = false
            },
            complete: () => {
                this.loading = false
            }
        });
    }

    async sharePdf() {
        if (!this.pdfBlob) return;

        const file = new File(
            [this.pdfBlob],
            this.fileName ?? 'relatorio.pdf',
            { type: 'application/pdf' }
        );

        if (navigator.share && navigator.canShare?.({ files: [file] })) {
            const shareText =
                `${this.descTitle}\n` +
                `${this.selectedStep.description ?? ''}` +
                `Gerado pelo sistema Lumos às ${Utils.formatNowToDDMMYYHHmm(true)}`;

            await navigator.share({
                title: 'Relatório Lumos',
                text: shareText,
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

    downloadPdf() {
        if (!this.pdfBlob || !this.fileName) return;

        const url = window.URL.createObjectURL(this.pdfBlob);
        const a = document.createElement('a');
        a.href = url;
        a.download = this.fileName;
        a.target = '_blank'; // 👈 ESSENCIAL no iOS
        a.click();
        window.URL.revokeObjectURL(url);
    }


    filterData(event: Event) {
        let value = (event.target as HTMLInputElement).value;

        if (value === null || value === undefined || value === '') {
            this.data = this.dataBackup;
        }

        this.data = this.dataBackup.filter(d => d.contract.contractor.toLowerCase().includes(value.toLowerCase()));
    }

}
