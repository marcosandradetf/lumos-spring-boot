import {Component, OnInit} from '@angular/core';
import {CurrencyPipe, DatePipe, NgForOf, NgIf} from "@angular/common";
import {UtilsService} from '../../../../core/service/utils.service';
import {ActivatedRoute, Router} from '@angular/router';
import {ContractService} from '../../../services/contract.service';
import {
    ContractItemsResponseWithExecutionsSteps, ContractReferenceItemsDTO,
    ContractResponse
} from '../../../contract-models';
import {LoadingComponent} from '../../../../shared/components/loading/loading.component';
import {Table, TableModule} from 'primeng/table';
import {ButtonDirective} from 'primeng/button';
import {FormsModule} from '@angular/forms';
import {InputText} from 'primeng/inputtext';
import {Toast} from 'primeng/toast';
import {MenuItem} from 'primeng/api';
import {Title} from '@angular/platform-browser';
import {FileService} from '../../../../core/service/file-service.service';
import {ContextMenu} from 'primeng/contextmenu';
import {
    PrimeConfirmDialogComponent
} from '../../../../shared/components/prime-confirm-dialog/prime-confirm-dialog.component';
import {ViewChild} from '@angular/core';
import {IconField} from 'primeng/iconfield';
import {InputIcon} from 'primeng/inputicon';
import {SharedState} from '../../../../core/service/shared-state';
import {Calendar} from 'primeng/calendar';
import {DropdownModule} from 'primeng/dropdown';
import {Message} from 'primeng/message';
import {OverlayPanelModule} from 'primeng/overlaypanel';
import {Utils} from '../../../../core/service/utils';
import {isEqual, cloneDeep} from 'lodash';
import {InputNumber} from 'primeng/inputnumber';
import {Popover} from 'primeng/popover';
import {LoadingOverlayComponent} from '../../../../shared/components/loading-overlay/loading-overlay.component';

@Component({
    selector: 'app-contract-list',
    standalone: true,
    imports: [
        NgForOf,
        NgIf,
        LoadingComponent,
        CurrencyPipe,
        TableModule,
        ButtonDirective,
        FormsModule,
        InputText,
        Toast,
        ContextMenu,
        PrimeConfirmDialogComponent,
        IconField,
        InputIcon,
        Calendar,
        DropdownModule,
        Message,
        DatePipe,
        OverlayPanelModule,
        InputNumber,
        Popover,
        LoadingOverlayComponent
    ],
    templateUrl: './contract-list.component.html',
    styleUrl: './contract-list.component.scss'
})
export class ContractListComponent implements OnInit {
    contracts: ContractResponse[] = [];
    contractsBackup: ContractResponse[] = [];

    contractItems: ContractItemsResponseWithExecutionsSteps[] = [];
    contractItemsBackup: ContractItemsResponseWithExecutionsSteps[] = [];

    loading: boolean = false;
    protected status: string = "";
    openModal: boolean = false;
    preMeasurementId: number = 0;
    city: string = '';
    reason: string = '';
    items: MenuItem[] | undefined;
    selectedContract: any = null;

    message = "";
    contextItems: MenuItem[] = [
        {
            label: 'Exibir/Editar Itens', icon:
                'pi pi-list',
            command: async () => await this.getItems(this.selectedContract.contractId),
        },
        {
            label: 'Editar Contrato', icon:
                'pi pi-pencil',
            command: async () => {
                await this.getItems(this.selectedContract.contractId, false);

                const items: ContractReferenceItemsDTO[] = [];
                this.contractItems.forEach((item) => {
                    items.push({
                        contractReferenceItemId: item.contractReferenceItemId,
                        description: item.description,
                        nameForImport: item.nameForImport ?? '',
                        type: item.type,
                        linking: item.linking ?? '',
                        itemDependency: '',
                        quantity: item.contractedQuantity,
                        price: item.unitPrice,
                        executedQuantity: item.totalExecuted,
                        contractItemId: item.contractItemId
                    });
                });

                void this.router.navigate(['/contratos/editar'], {
                    state: {
                        contract: this.selectedContract,
                        items: items
                    }
                })
            },
        },
        {
            label: 'Excluir Contrato',
            icon: 'pi pi-trash',
            command: () => {
                this.message = "Confirma a exclusão do contrato " + this.selectedContract.contractor + "?"
                this.openModal = true;
            }
        },
        {
            label: 'Arquivar',
            icon: 'pi pi-folder-open',
            command: () => {
                this.archive = true
                this.message = "Confirma o arquivamento do contrato " + this.selectedContract.contractor + "?"
                this.openModal = true;
            }
        },
    ];

    home: MenuItem | undefined;
    private archive: boolean = false;

    constructor(
        private contractService: ContractService,
        protected utils: UtilsService,
        protected router: Router,
        private route: ActivatedRoute,
        private titleService: Title,
        private minioService: FileService
    ) {
    }

    ngOnInit() {
        this.loading = true;
        this.route.queryParams.subscribe(params => {
            this.reason = params['for'];
        });

        if (this.reason.toLowerCase() !== 'premeasurement') {
            this.titleService.setTitle("Visualizar Contratos");
        } else {
            this.titleService.setTitle("Importar Pré-Medição");
        }

        this.getContracts();

        SharedState.setCurrentPath(['Contratos', 'Exibir Todos']);

    }

    getContracts() {
        this.contractService.getAllContracts(this.filters).subscribe({
            next: c => {
                this.contracts = c;
                this.contractsBackup = c;
            },
            error: err => {
                this.utils.showMessage(err.error.message ?? err.error.error ?? err.error, 'error');
                this.loading = false;
            },
            complete: () => {
                this.loading = false;

                if (this.contracts.length > 0) this.showMenu = false;
                else this.utils.showMessage("Não existe nenhum contrato para os filtros selecionados", "info", "");
            }
        });
    }


    // private loadPreMeasurements() {
    //   switch (this.status) {
    //     case 'pendente':
    //       this.preMeasurementService.getPreMeasurements('pending').subscribe(preMeasurements => {
    //         this.preMeasurements = preMeasurements;
    //         this.city = this.preMeasurements[0].streets[0].city;
    //       });
    //       break;
    //     case 'aguardando-retorno':
    //       this.preMeasurementService.getPreMeasurements('waiting').subscribe(preMeasurements => {
    //         this.preMeasurements = preMeasurements;
    //         this.city = this.preMeasurements[0].streets[0].city;
    //       });
    //       break;
    //     case 'validando':
    //       this.preMeasurementService.getPreMeasurements('validating').subscribe(preMeasurements => {
    //         this.preMeasurements = preMeasurements;
    //         this.city = this.preMeasurements[0].streets[0].city;
    //       });
    //       break;
    //     case 'disponivel':
    //       this.preMeasurementService.getPreMeasurements('available').subscribe(preMeasurements => {
    //         this.preMeasurements = preMeasurements;
    //         this.city = this.preMeasurements[0].streets[0].city;
    //       });
    //       break;
    //   }
    // }


    async getItems(contractId: number, showItems: boolean = true): Promise<void> {
        if (contractId === 0 || this.contractId === contractId) return;

        this.contractId = contractId;
        this.loading = true;

        return new Promise<void>((resolve, reject) => {
            this.contractService.getContractItemsWithExecutionsSteps(contractId).subscribe({
                next: items => {
                    this.contractItems = items || [];
                    this.contractItemsBackup = cloneDeep(items || []);
                    this.normalizeExecutedQuantities();
                    this.showItems = showItems;
                },
                error: err => {
                    this.loading = false;
                    reject(err);
                },
                complete: () => {
                    this.loading = !showItems;
                    resolve();
                }
            });
        });
    }


    contractId: number = 0;
    showItems: boolean = false;

    getTotalPrice() {
        return this.contracts.find(c => c.contractId == this.contractId)?.contractValue || "0.00";
    }


    clonedItems: { [key: string]: any } = {};

    @ViewChild('dt') table!: Table;

    onRowEditInit(item: any) {
        this.clonedItems[item.id] = {...item}; // backup da linha
        this.table.initRowEdit(item); // ativa a edição só desta linha
    }

    onRowEditSave(item: any, rowElement: HTMLTableRowElement) {
        // aqui você valida ou recalcula valores
        delete this.clonedItems[item.id];
        this.table.saveRowEdit(item, rowElement); // fecha a edição só desta linha
    }

    onRowEditCancel(item: any) {
        const index = this.contractItems.findIndex(i => i.contractItemId === item.contractItemId);
        this.contractItems[index] = this.clonedItems[item.id]; // restaura backup
        delete this.clonedItems[item.id];
        this.table.cancelRowEdit(item); // fecha a edição só desta linha
    }


    downloadContractFiles() {
        const contract = this.contracts.find(c => c.contractId == this.contractId);
        const contractFile = contract?.contractFile;
        if (contractFile) {
            this.minioService.downloadFile(contractFile).subscribe(response => {
                const contentDisposition = response.headers.get('Content-Disposition');
                const filenameMatch = contentDisposition?.match(/filename="(.+)"/);
                const filename = filenameMatch ? filenameMatch[1] : contractFile;

                const blob = new Blob([response.body!], {type: response.headers.get('Content-Type') || 'application/octet-stream'});
                const url = window.URL.createObjectURL(blob);

                const link = document.createElement('a');
                link.href = url;
                link.download = filename;
                link.click();

                window.URL.revokeObjectURL(url);
            });
        } else
            this.utils.showMessage('Nenhum arquivo foi encontrado', "warn", 'Arquivo não existente')
    }

    getExecutions() {
        return this.contractItems.find(i => i.contractItemId)
    }

    get maxExecutedSteps(): number {
        if (!this.contractItems || this.contractItems.length === 0) return 0;

        return this.contractItems.reduce((max, item) => {
            const steps = item.executedQuantity?.map(q => q.step) || [];
            const maxStep = Math.max(0, ...steps);
            return maxStep > max ? maxStep : max;
        }, 0);
    }

    normalizeExecutedQuantities(): void {
        const maxSteps = this.maxExecutedSteps;

        this.contractItems.forEach(item => {
            const quantities = item.executedQuantity || [];

            // Garante que todos os steps estejam presentes com zero se ausentes
            item.executedQuantity = Array.from({length: maxSteps}, (_, i) => {
                const step = i + 1;
                const found = quantities.find(q => q.step === step);
                return found ?? {
                    directExecutionId: 0, // ou null, se preferir
                    step,
                    quantity: 0
                };
            });
        });
    }


    handleAction(action: "accept" | "reject") {
        switch (action) {
            case 'accept':
                this.loading = true;
                this.openModal = false;

                if (this.archive) {
                    this.contractService.archiveById(this.selectedContract.contractId)
                        .subscribe({
                            next: () => {
                                this.contracts = this.contracts.filter(c => c.contractId !== this.selectedContract.contractId);
                                this.utils.showMessage("Contrato " + this.selectedContract.contractor + " arquivado com sucesso!", "success", "Operação realizada")
                            },
                            error: (err) => {
                                this.utils.showMessage(err.error.message, "error", "Não foi possível arquivar o contrato " + this.selectedContract.contractor);
                                this.loading = false;
                            },
                            complete: () => {
                                this.loading = false;
                            }
                        });
                } else {
                    this.contractService.deleteById(this.selectedContract.contractId)
                        .subscribe({
                            next: () => {
                                this.contracts = this.contracts.filter(c => c.contractId !== this.selectedContract.contractId);
                                this.utils.showMessage("Contrato " + this.selectedContract.contractor + " excluido com sucesso!", "success", "Operação realizada")
                            },
                            error: (err) => {
                                this.utils.showMessage(err.error.message, "error", "Não foi possível excluir o contrato " + this.selectedContract.contractor);
                                this.loading = false;
                            },
                            complete: () => {
                                this.loading = false;
                            }
                        });
                }
                break;
            case 'reject':
                this.openModal = false;
                this.message = ''
                break;
        }
    }

    onRightClick(event: MouseEvent, contract: any): void {
        event.preventDefault(); // impede menu padrão do navegador
        this.selectedContract = contract;
    }

    @ViewChild('menu') contextMenu: ContextMenu | undefined = undefined;

    openContextMenu(event: MouseEvent, contract: any): void {
        event.preventDefault(); // Evita scroll inesperado ou comportamento nativo
        this.selectedContract = contract; // Salva se precisar no menu

        if (this.contextMenu) this.contextMenu.show(event); // Abre o menu na posição do clique
    }

    filterData(event: Event) {
        let value = (event.target as HTMLInputElement).value;

        if (value === null || value === undefined || value === '') {
            this.contracts = this.contractsBackup;
        }

        this.contracts = this.contractsBackup.filter(c => c.contractor.toLowerCase().includes(value.toLowerCase()));
    }

    onCardClick(c: ContractResponse): void {
        if (this.reason === 'view') {
            return; // não faz nada
        }

        if (this.reason === 'preMeasurement') {
            void this.router.navigate([
                `/pre-medicao/importar/contrato/${c.contractId}`
            ]);
            return;
        }

        this.router.navigate(
            ['/execucoes/iniciar-sem-pre-medicao/'],
            {
                queryParams: {
                    codigo: c.contractId,
                    nome: c.contractor
                }
            }
        );
    }


    // new filters
    submitted = false;
    showMenu = false;
    statuses = [
        {label: 'Ativo', value: 'ACTIVE'},
        {label: 'Arquivado', value: 'ARCHIVED'}
    ];
    filters: {
        contractor: string | null;
        startDate: Date | null;
        endDate: Date | null;
        status: 'ACTIVE' | 'ARCHIVED';
    } = {
        contractor: null,
        startDate: new Date(new Date().setMonth(new Date().getMonth() - 2)),
        endDate: new Date(),
        status: 'ACTIVE',
    };

    protected applyFilters() {
        this.submitted = true;

        if ((!this.filters.startDate || !this.filters.endDate || !this.filters.status) && !this.filters.contractor) {
            return;
        }

        if (!this.filters.contractor) {
            const days = Utils.diffInDays(this.filters.startDate!, this.filters.endDate!);

            if (days > 62) {
                this.utils.showMessage("O Período máximo é de 62 dias.", 'warn', 'Período inválido');
                return;
            }
        }

        this.loading = true;
        this.getContracts();
    }


    protected resetFilters() {
        this.filters = {
            contractor: null,
            startDate: null,
            endDate: null,
            status: 'ACTIVE',
        };
    }

    protected readonly Number = Number;

    protected deleteItem(item: ContractItemsResponseWithExecutionsSteps) {
        if (item.totalExecuted > 0) {
            this.utils.showMessage('Por motivo de segurança de dados, não é permitido excluir um item com registro de execução.', 'warn', 'Atenção');
            return;
        }

        this.contractItems = this.contractItems.filter(i => i.contractItemId !== item.contractItemId);
    }

    protected hasDiff() {
        return !isEqual(this.contractItems, this.contractItemsBackup);
    }

    loadingOverlay = false;
    protected updateItems() {
        this.loadingOverlay = true;
        const items: ContractReferenceItemsDTO[]
            = this.contractItems.map(item =>
            ({
                contractReferenceItemId: item.contractReferenceItemId,
                description: item.description,
                nameForImport: item.nameForImport ?? '',
                type: item.type,
                linking: "",
                itemDependency: "",
                quantity: item.contractedQuantity,
                price: item.unitPrice.toString(),
                executedQuantity: item.totalExecuted,
                contractItemId: item.contractItemId
            })
        );

        this.contractService.updateItems(
            items,
            this.contractId
        ).subscribe({
            error: err => {
                this.loadingOverlay = false;
                this.utils.showMessage(err.error.message ?? err.error.error ?? err.error, 'error');
            },
            complete: () => {
                this.contractItemsBackup = cloneDeep(this.contractItems);
                this.loadingOverlay = false;
                this.utils.showMessage('Itens atualizados com sucesso', 'success');
            }
        });
    }

    protected cancelChanges() {
        this.contractItems = cloneDeep(this.contractItemsBackup);
    }
}
