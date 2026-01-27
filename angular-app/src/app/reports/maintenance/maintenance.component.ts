import {Component, ElementRef, OnInit, ViewChild} from '@angular/core';
import {SafeUrlPipe} from '../../safe-url.pipe';
import {ReportService} from '../report.service';
import {UtilsService} from '../../core/service/utils.service';
import {Title} from '@angular/platform-browser';
import {PrimeBreadcrumbComponent} from '../../shared/components/prime-breadcrumb/prime-breadcrumb.component';
import {LoadingComponent} from '../../shared/components/loading/loading.component';
import {DatePipe, NgForOf, NgIf} from '@angular/common';
import {Toast} from 'primeng/toast';
import {Menu} from 'primeng/menu';
import {MenuItem} from 'primeng/api';
import {PrimeConfirmDialogComponent} from '../../shared/components/prime-confirm-dialog/prime-confirm-dialog.component';
import {AuthService} from '../../core/auth/auth.service';
import {IconField} from 'primeng/iconfield';
import {InputIcon} from 'primeng/inputicon';
import {InputText} from 'primeng/inputtext';
import {SharedState} from '../../core/service/shared-state';
import {Button} from 'primeng/button';
import * as pdfjsLib from 'pdfjs-dist';


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
        Button,
        NgIf
    ],
    providers: [SafeUrlPipe],
    templateUrl: './maintenance.component.html',
    styleUrl: './maintenance.component.scss'
})
export class MaintenanceComponent implements OnInit {
    pdfUrl: string | null = null;
    loading = false;

    data: {
        contract: {
            contract_id: number;
            contractor: string;
        },
        maintenances: {
            maintenance_id: string;
            streets: string[];
            date_of_visit: string;
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
        maintenances: {
            maintenance_id: string;
            streets: string[];
            date_of_visit: string;
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
                this.action = "ARCHIVE";
            },
        },
        {
            label: 'Excluir',
            icon: 'pi pi-trash',
            command: () => {
                this.action = "DELETE";
            },
        },
    ];

    maintenanceId: string | null = null;
    currentContractId: number | null = null;
    action: string | null = null;
    isIOS = /iPad|iPhone|iPod/.test(navigator.userAgent)
        || (navigator.platform === 'MacIntel' && navigator.maxTouchPoints > 1);


    openContextMenu(event: MouseEvent, maintenanceId: string, contractId: number) {
        event.preventDefault();
        this.maintenanceId = maintenanceId;
        this.currentContractId = contractId;

        // Abre o menu popup alinhado ao botão clicado
        this.menu?.show(event);
    }

    // Métodos para as ações do menu
    conventionalDataReport() {
        this.loadPdf(this.maintenanceId!!, 'conventional');
    }

    ledDataReport() {
        this.loadPdf(this.maintenanceId!!, 'led');
    }

    actionArchiveOrDelete() {
        this.loading = true;

        if (!this.authService.user?.getRoles()?.some(
            r => ['ADMIN', 'RESPONSAVEL_TECNICO', 'ANALISTA'].includes(r)
        )) {
            this.action = null;
            this.loading = false;
            this.utilService.showMessage("Sua função atual no sistema não permite executar essa ação.", "info", "Lumos - Relatórios");
            return;
        }

        const message = this.action === "ARCHIVE" ? "Relatório arquivado com sucesso"
            : "Relatório excluido com sucesso";

        this.reportService.archiveOrDelete(this.maintenanceId!!, this.action!!).subscribe({
            next: () => {
                const index = this.data.findIndex(c => c.contract.contract_id === this.currentContractId);
                if (index !== -1) {
                    this.data[index].maintenances = this.data[index].maintenances
                        .filter(m => m.maintenance_id !== this.maintenanceId!!);

                    // remove o contrato se não restar nenhuma manutenção
                    if (this.data[index].maintenances.length === 0) {
                        this.data.splice(index, 1);
                    }
                }

                this.utilService.showMessage(message, "success", "Lumos - Relatórios");
            },
            error: err => {
                this.utilService.showMessage(err.error.error ?? err.error.message, "info", "Lumos - Relatórios");
                this.loading = false;
                this.action = null;
            },
            complete: () => {
                this.loading = false;
                this.action = null;
            }
        });
    }


    constructor(private reportService: ReportService, protected utilService: UtilsService,
                private authService: AuthService,
                private title: Title) {
    }

    ngOnInit() {
        this.title.setTitle('Relatórios de manutenções');
        SharedState.setCurrentPath(["Relatórios", "Manutenções"]);
        this.loading = true;
        this.loadMaintenances();
    }

    public loadMaintenances() {
        this.reportService.getFinishedMaintenances().subscribe({
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

    public loadPdf(maintenanceId: string, type: string) {
        const desc = type === 'led' ? 'Led' : 'Convencional';
        this.loading = true;

        this.reportService.getMaintenancePdf(maintenanceId, type).subscribe({
            next: async (resp) => {
                // limpa URL antiga
                if (this.pdfUrl) {
                    URL.revokeObjectURL(this.pdfUrl);
                }

                // blob
                this.pdfBlob = resp.body!;
                this.pdfUrl = URL.createObjectURL(this.pdfBlob);

                // filename
                const cd = resp.headers.get('content-disposition');
                this.fileName = this.extractFilename(cd) ?? 'relatorio.pdf';

                // 👉 renderiza com PDF.js
                await this.renderPdf(this.pdfBlob);
            },
            error: (err) => {
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

    @ViewChild('pdfCanvas', {static: true})
    canvasRef!: ElementRef<HTMLCanvasElement>;

    async renderPdf(blob: Blob) {
        const url = URL.createObjectURL(blob);

        const pdf = await pdfjsLib.getDocument(url).promise;
        const page = await pdf.getPage(1);

        const viewport = page.getViewport({ scale: 1.2 });
        const canvas = this.canvasRef.nativeElement;

        const context = canvas.getContext('2d')!;
        canvas.height = viewport.height;
        canvas.width = viewport.width;

        await page.render({
            canvasContext: context,
            canvas,
            viewport
        }).promise;

        URL.revokeObjectURL(url);
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
