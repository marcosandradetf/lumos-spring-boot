import {
    Component,
    OnInit,
    signal,
    computed, ViewChild, ElementRef
} from '@angular/core';
import {CommonModule} from '@angular/common';
import {ActivatedRoute, Router} from '@angular/router';
import {forkJoin} from 'rxjs';

import {
    DirectExecution,
    DirectExecutionStreetItem,
    Contract,
    ContractItem,
    ValidationStep
} from './execution-no-work-service.models';

import {ExecutionService} from '../../executions/execution.service';
import {ContractService} from '../services/contract.service';
import {ContractReferenceItemsDTO, ContractResponse} from '../contract-models';
import {DropdownModule} from 'primeng/dropdown';
import {FormsModule} from '@angular/forms';
import {Calendar} from 'primeng/calendar';
import {InputText} from 'primeng/inputtext';
import {Message} from 'primeng/message';
import {Utils} from '../../core/service/utils';
import {UtilsService} from '../../core/service/utils.service';
import {Toast} from 'primeng/toast';
import {Title} from '@angular/platform-browser';
import {SharedState} from '../../core/service/shared-state';
import {Button} from 'primeng/button';
import {LoadingOverlayComponent} from '../../shared/components/loading-overlay/loading-overlay.component';
import {Tag} from 'primeng/tag';
import {ProgressBar} from 'primeng/progressbar';
import {exec} from 'node:child_process';

@Component({
    selector: 'app-execution-no-work-service',
    standalone: true,
    imports: [CommonModule, DropdownModule, FormsModule, Calendar, InputText, Message, Toast, Button, LoadingOverlayComponent, Tag, ProgressBar],
    templateUrl: './execution-no-work-service.component.html',
    styleUrl: './execution-no-work-service.component.scss'
})
export class ExecutionNoWorkServiceComponent implements OnInit {

    // ========================
    // STATE
    // ========================

    execution = signal<DirectExecution | null>(null);
    linkedItemsResponse = signal<DirectExecutionStreetItem[]>([]);
    contracts = signal<ContractResponse[]>([]);
    selectedContract = signal<Contract | null>(null);
    selectedContractItem = signal<ContractItem | null>(null);

    currentStep = signal<ValidationStep>('CONTRACT');
    loading = signal<boolean>(true);
    showOnlyPending = signal<boolean>(false);
    contractSearch = signal<string>('');

    @ViewChild('rightPanel') rightPanelRef?: ElementRef<HTMLElement>;
    @ViewChild('leftPanel') leftPanel?: ElementRef<HTMLElement>;
    previewTop = signal(0);
    minH = signal(2000);

    private readonly previewHeight = 260;
    private readonly previewPadding = 12;
    remainingSeconds = 600; // 10 minutos
    intervalId: any;


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
        startDate: new Date(new Date().setMonth(new Date().getMonth() - 3)),
        endDate: new Date(),
        status: 'ACTIVE',
    };

    constructor(
        private route: ActivatedRoute,
        private api: ExecutionService,
        private contractApi: ContractService,
        protected router: Router,
        private utils: UtilsService,
        private title: Title
    ) {
    }

    // ========================
    // INIT
    // ========================

    ngOnInit(): void {
        this.title.setTitle("Validação de execução operacional");
        SharedState.setCurrentPath(["Execuções", "Validar Execução"]);

        this.loading.set(true);

        const executionId = Number(this.route.snapshot.paramMap.get('id'));

        if (!executionId) {
            console.error('Execution ID inválido');
            return;
        }

        forkJoin({
            contracts: this.contractApi.getAllContracts(this.filters),
            execution: this.api.getExecutionWaitingValidation(executionId)
        }).subscribe({
            next: ({contracts, execution}) => {

                this.execution.set(execution);

                // Se API retornar paginação use contracts.content
                this.contracts.set(contracts);

                if (execution.contractId) {
                    this.loadContract(execution.contractId);
                    this.currentStep.set('MAPPING');
                } else {
                    this.currentStep.set('CONTRACT');
                }

                this.loading.set(false);
            },
            error: err => {
                console.error(err);
                this.loading.set(false);
            }
        });
    }

    // ========================
    // CONTRACT SEARCH
    // ========================

    protected resetFilters() {
        this.filters = {
            contractor: null,
            startDate: new Date(new Date().setMonth(new Date().getMonth() - 3)),
            endDate: new Date(),
            status: 'ACTIVE',
        };
    }

    protected applyFilters() {
        this.submitted = true;

        if ((!this.filters.startDate || !this.filters.endDate || !this.filters.status) && !this.filters.contractor) {
            return;
        }

        if (!this.filters.contractor) {
            const days = Utils.diffInDays(this.filters.startDate!, this.filters.endDate!);

            if (days > 93) {
                this.utils.showMessage("O Período máximo é de 3 meses.", 'warn', 'Período inválido');
                return;
            }
        }

        this.loading.set(true);
        this.contractApi.getAllContracts(this.filters).subscribe({
            next: (data) => {
                this.contracts.set(data);
                this.loading.set(false);
                if (data.length > 0) {
                    this.showMenu = false;
                } else {
                    this.utils.showMessage(
                        "Nenhum contrato encontrado com os filtros atuais",
                        "info"
                    )
                }
            },
            error: err => {
                this.utils.showMessage(err.error.message ?? err.error.error, 'error');
                this.loading.set(false);
            }
        });
    }

    filteredContracts = computed(() => {
        const term = this.contractSearch().trim().toLowerCase();
        const list = this.contracts();

        if (!term) return list;

        return list.filter(contract =>
            contract.number?.toLowerCase().includes(term) ||
            contract.contractor?.toLowerCase().includes(term) ||
            contract.cnpj?.toLowerCase().includes(term)
        );
    });

    // ========================
    // CONTRACT LOAD
    // ========================

    loadContract(contractId: number) {
        this.loading.set(true);

        this.api.getContractItemsForLink(contractId)
            .subscribe({
                next: contract => {
                    this.selectedContract.set(contract);
                    this.preselectContractItems();
                    this.loading.set(false);

                    setTimeout(() => {
                        const height = this.leftPanel?.nativeElement.clientHeight;
                        if (height) {
                            this.minH.set(height);
                        }
                    });
                },
                error: err => {
                    this.loading.set(false);
                    this.utils.showMessage(err.error.message ?? err.error.error, 'error');
                },
            });
    }

    selectContract(contract: ContractResponse) {
        this.loadContract(contract.contractId);
        this.currentStep.set('MAPPING');
    }


    selectContractItem(ci: any | null, event?: any) {
        this.selectedContractItem.set(ci);

        if (!ci || !event || !this.rightPanelRef) {
            return;
        }


        const height = this.leftPanel?.nativeElement.clientHeight;
        if (height && height !== this.minH()) {
            this.minH.set(height);
        }
        const panel = this.rightPanelRef.nativeElement;
        const rect = panel.getBoundingClientRect();

        // posição do mouse relativa ao topo da coluna da direita
        const mouseYInsidePanel = event.clientY - rect.top;

        // centraliza o card no mouse
        let top = mouseYInsidePanel - this.previewHeight / 2;

        // limita dentro da coluna
        const minTop = this.previewPadding;
        const maxTop = rect.height - this.previewHeight - this.previewPadding;

        top = Math.max(minTop, Math.min(top, maxTop));

        this.previewTop.set(top);
    }

    backToContractSelection() {
        this.selectedContract.set(null);
        this.currentStep.set('CONTRACT');
    }

    goToCreateContract() {
        void this.router.navigate(['/contratos/criar']);
    }

    // ========================
    // PRESELECT AUTO
    // ========================

    preselectContractItems() {
        const execution = this.execution();
        const contract = this.selectedContract();
        // 1. Pegamos o saldo que já está comprometido no Banco de Dados
        const dbCommitted = this.committedInDatabaseMap();

        if (!execution || !contract) return;

        // 2. Criamos um mapa de saldo "em tempo real" apenas para esta operação de preselect
        const localBalanceMap = new Map<number, number>();
        contract.items.forEach(ci => {
            const alreadyInDb = dbCommitted.get(ci.contractItemId) ?? 0;
            localBalanceMap.set(ci.contractItemId, ci.contractedQuantity - alreadyInDb);
        });

        const updatedStreets = execution.streets.map(street => ({
            ...street,
            items: street.items.map(item => {
                // Busca o item de contrato que dá match com o material
                const matched = contract.items.find(ci =>
                    item.material?.referenceItemsIds?.includes(ci.referenceItemId)
                );

                let finalSelectedId = null;
                let finalSelectedRefId = null;

                if (matched) {
                    const available = localBalanceMap.get(matched.contractItemId) ?? 0;
                    const needed = item.executedQuantity ?? 0;

                    // 3. Só pré-seleciona se houver saldo disponível no balanço local
                    if (available >= needed && needed > 0) {
                        finalSelectedId = matched.contractItemId;
                        finalSelectedRefId = matched.referenceItemId;

                        // 4. Abate do saldo local para que a próxima rua/item veja o saldo atualizado
                        localBalanceMap.set(matched.contractItemId, available - needed);
                    }
                }

                return {
                    ...item,
                    // Sugestão a gente sempre deixa (pro UI mostrar o "estourinho" se quiser)
                    suggestedContractItemId: matched?.contractItemId ?? null,
                    suggestedContractReferenceItemId: matched?.referenceItemId ?? null,
                    // Seleção real depende do saldo verificado acima
                    selectedContractItemId: finalSelectedId,
                    selectedContractReferenceItemId: finalSelectedRefId
                };
            })
        }));

        this.execution.set({
            ...execution,
            streets: updatedStreets
        });
    }

    clearAllBindings() {
        const execution = this.execution();
        if (!execution) return;

        const updatedStreets = execution.streets.map(street => ({
            ...street,
            items: street.items.map(item => ({
                ...item,
                selectedContractItemId: null,
                selectedContractReferenceItemId: null
            }))
        }));

        this.execution.set({...execution, streets: updatedStreets});
    }

    applySuggestionsForStreet(streetId: number) {
        const execution = this.execution();
        const contract = this.selectedContract();
        const dbCommitted = this.committedInDatabaseMap();

        if (!execution || !contract) return;
        const localBalanceMap = new Map<number, number>();
        contract.items.forEach(ci => {
            const alreadyInDb = dbCommitted.get(ci.contractItemId) ?? 0;
            localBalanceMap.set(ci.contractItemId, ci.contractedQuantity - alreadyInDb);
        });

        const updatedStreets = execution.streets.map(street => {

            if (street.directExecutionStreetId !== streetId) return street;

            return {
                ...street,
                items: street.items.map(item => {

                    const matched = contract.items.find(ci =>
                        item.material?.referenceItemsIds?.includes(
                            ci.referenceItemId
                        )
                    );

                    let finalSelectedId = null;
                    let finalSelectedRefId = null;

                    if (matched) {
                        const available = localBalanceMap.get(matched.contractItemId) ?? 0;
                        const needed = item.executedQuantity ?? 0;

                        if (available >= needed && needed > 0) {
                            finalSelectedId = matched.contractItemId;
                            finalSelectedRefId = matched.referenceItemId;

                            // 4. Abate do saldo local para que a próxima rua/item veja o saldo atualizado
                            localBalanceMap.set(matched.contractItemId, available - needed);
                        }
                    }

                    return {
                        ...item,
                        // Sugestão a gente sempre deixa (pro UI mostrar o "estourinho" se quiser)
                        suggestedContractItemId: matched?.contractItemId ?? null,
                        suggestedContractReferenceItemId: matched?.referenceItemId ?? null,
                        // Seleção real depende do saldo verificado acima
                        selectedContractItemId: finalSelectedId,
                        selectedContractReferenceItemId: finalSelectedRefId
                    };
                })
            };
        });

        this.execution.set({...execution, streets: updatedStreets});
    }

    // ========================
    // UPDATE BINDING
    // ========================

    updateItemBinding(
        directExecutionStreetItemId: number,
        contractItemId: number | null
    ) {
        const execution = this.execution();
        const contract = this.selectedContract();

        if (!execution || !contract) return;

        const selectedContractItem =
            contract.items.find(ci => ci.contractItemId === contractItemId) ?? null;

        const updatedStreets = execution.streets.map(street => ({
            ...street,
            items: street.items.map(item => {

                if (item.directExecutionStreetItemId !== directExecutionStreetItemId) {
                    return item;
                }

                return {
                    ...item,
                    selectedContractItemId: contractItemId,
                    selectedContractReferenceItemId:
                        selectedContractItem?.referenceItemId ?? null
                };
            })
        }));

        this.execution.set({...execution, streets: updatedStreets});
    }

    // ========================
    // COMPUTEDS
    // ========================

    allExecutionItems = computed(() => {
        const execution = this.execution();
        if (!execution) return [];

        return execution.streets.flatMap(street =>
            street.items.map(item => ({
                ...item,
                streetAddress: street.address
            }))
        );
    });

    mappedCount = computed(() =>
        this.allExecutionItems().filter(i => !!i.selectedContractItemId).length
    );

    pendingCount = computed(() =>
        this.allExecutionItems().filter(i => !i.selectedContractItemId).length
    );

    progressPercentage = computed(() => {
        const total = this.allExecutionItems().length;
        if (!total) return 0;
        return Math.round((this.mappedCount() / total) * 100);
    });

    visibleStreets = computed(() => {
        const execution = this.execution();
        if (!execution) return [];

        return execution.streets.map(street => ({
            ...street,
            visibleItems: this.showOnlyPending()
                ? street.items.filter(i => !i.selectedContractItemId)
                : street.items
        }));
    });

    // ========================
    // VALIDATION RULES
    // ========================

    isInsufficient(contractItem: ContractItem, executedQuantity: number): boolean {
        const available =
            Number(contractItem.contractedQuantity) -
            Number(contractItem.quantityExecuted);

        return Number(executedQuantity) > available;
    }

    hasDivergence(): boolean {
        const contract = this.selectedContract();
        if (!contract) return false;

        return this.allExecutionItems().some(item => {
            if (!item.selectedContractItemId) return false;

            const contractItem = contract.items.find(
                ci => ci.contractItemId === item.selectedContractItemId
            );

            if (!contractItem) return false;

            return this.isInsufficient(
                contractItem,
                Number(item.executedQuantity)
            );
        });
    }

    // ========================
    // NAVIGATION
    // ========================

    backToMapping() {
        if(this.execution()) {
            this.loading.set(true);

            const streetItemIds = this.execution()!.streets
                .flatMap(s => s.items)
                .map(i => i.directExecutionStreetItemId)

            this.stopTimer();
            this.api.cancelValidation(
                this.execution()!.directExecutionId,
                streetItemIds
            ).subscribe({
                next: () => {
                    const itemIdsToRemove = new Set(this.linkedItemsResponse().map(item => item.directExecutionStreetItemId));
                    this.removeItems(itemIdsToRemove);
                    this.currentStep.set('MAPPING');
                    this.loading.set(false);
                },
                error: err => {
                    this.utils.showMessage(err.error.message ?? err.error.error, "error");
                    this.loading.set(false);
                }
            });
        }
    }

    toggleOnlyPending() {
        this.showOnlyPending.update(v => !v);
    }

    // ========================
    // REVIEW / FINAL
    // ========================

    goToReview() {
        if (this.pendingCount() > 0) return;
        if (this.hasDivergence()) return;

        const execution = this.execution();
        const contract = this.selectedContract();

        if (!execution || !contract) return;

        if (this.hasBalanceErrors()) {
            this.utils.showMessage(
                'Existem itens que excedem o saldo disponível no contrato. Verifique as quantidades.',
                'error',
                "Erro de Saldo"
            );
            return;
        }

        const payload = {
            directExecutionId: execution.directExecutionId,
            contractId: contract.contractId,
            items: execution.streets.flatMap(street =>
                street.items.map(item => ({
                    directExecutionStreetItemId: item.directExecutionStreetItemId,
                    contractItemId: item.selectedContractItemId
                }))
            )
        };

        this.loading.set(true);
        this.api.preValidateExecution(payload).subscribe({
            next: data => {
                this.linkedItemsResponse.set(data);
                this.addLinkedItems();

                this.currentStep.set('REVIEW');
                this.startTimer();
                this.loading.set(false);
            },
            error: err => {
                this.utils.showMessage(err.error.message ?? err.error.error, "error");
                this.loading.set(false);
            }
        });
    }

    deleteItem(streetItemId: number) {
        this.loading.set(true);
        this.api.deleteItem(streetItemId).subscribe({
            next: () => {
                this.removeItems(new Set([streetItemId]));
                this.utils.showMessage("Item excluído com sucesso.", "success");
                this.loading.set(false);
            },
            error: err => {
                this.utils.showMessage(err.error.message ?? err.error.error, "error");
                this.loading.set(false);
            }
        });
    }

    validateExecution() {
        this.stopTimer();

        if (this.execution()) {
            this.loading.set(true);
            this.api.validateExecution(this.execution()!.directExecutionId).subscribe({
                next: () => {
                    this.currentStep.set("FINISHED");
                    this.loading.set(false);
                },
                error: err => {
                    this.utils.showMessage(err.error.message ?? err.error.error, "error");
                    this.loading.set(false);
                }
            });
        }
    }

    // ========================
    // UTILS
    // ========================

    readonly hasBalanceErrors = computed(() => {
        const balanceMap = this.liveAvailableBalanceMap();
        const execution = this.execution();

        if (!execution) return false;

        // Verifica se algum item selecionado deixou o saldo real negativo
        return Array.from(balanceMap.values()).some(balance => balance < 0);
    });

    getAvailableContractItemsForExecutionItem(
        executionItem: DirectExecutionStreetItem
    ): any[] {
        const contract = this.selectedContract();
        if (!contract) return [];

        // Função única de mapeamento para manter a consistência
        const mapItem = (ci: ContractItem, isPriority: boolean) => {
            return {
                ...ci,
                label: ci.referenceItem.description, // Útil para o filter do PrimeNG
                isPriority // Flag para a gente usar no HTML se quiser dar um destaque
            };
        };

        const preferredReference =
            executionItem.selectedContractReferenceItemId ??
            executionItem.suggestedContractReferenceItemId;

        // 1. Itens que batem com a referência (Sugestões)
        const priorityItems = contract.items
            .filter(item => item.referenceItemId === preferredReference)
            .map(ci => mapItem(ci, true));

        // 2. Todos os outros itens (O resto do contrato)
        const otherItems = contract.items
            .filter(item => item.referenceItemId !== preferredReference)
            .map(ci => mapItem(ci, false));

        // Retorna a união: Sugestões primeiro, depois o resto.
        return [...priorityItems, ...otherItems];
    }

    protected newItem() {
        const items: ContractReferenceItemsDTO[] = [];
        const contract = this.contracts().find(c =>
            c.contractId === this.selectedContract()?.contractId
        );

        if (!contract) return;

        this.selectedContract()?.items.forEach((item) => {
            items.push({
                contractReferenceItemId: item.referenceItemId,
                description: item.referenceItem.description,
                nameForImport: item.referenceItem.description ?? '',
                type: item.referenceItem.type ?? '',
                linking: item.referenceItem.linking ?? '',
                itemDependency: item.referenceItem.itemDependency ?? '',
                quantity: item.contractedQuantity,
                price: Number(item.unitPrice) ?? '',
                totalExecuted: item.quantityExecuted,
                contractItemId: item.contractItemId,
                executedQuantity: item.executedQuantity,
                reservedQuantity: item.reservedQuantity
            });
        });

        void this.router.navigate(['/contratos/editar'], {
            state: {
                contract: contract,
                items: items,
                step: 2
            }
        })
    }


    readonly totalExecutedMap = computed(() => {
        const contract = this.selectedContract();
        const map = new Map<number, number>();

        contract?.items.forEach(item => {
            const total = (item.reservedQuantity as any[] ?? [])
                .reduce((sum, r) => sum + (r.quantity ?? 0), 0);
            map.set(item.contractItemId, total + item.quantityExecuted);
        });

        return map;
    });

    // 1. O que já está comprometido no banco/outras execuções (Você já tem esse)
    readonly committedInDatabaseMap = computed(() => {
        const contract = this.selectedContract();
        const map = new Map<number, number>();
        if (!contract) return map;

        contract.items.forEach(item => {
            const totalReserved = (item.reservedQuantity ?? [])
                .reduce((sum, r) => sum + (r.quantity ?? 0), 0);
            map.set(item.contractItemId, totalReserved + (item.quantityExecuted ?? 0));
        });
        return map;
    });

    // 2. O que o usuário selecionou AGORA na tela
    readonly currentSelectionMap = computed(() => {
        const execution = this.execution();
        const map = new Map<number, number>();
        if (!execution) return map;

        execution.streets.forEach(street => {
            street.items.forEach(item => {
                if (item.selectedContractItemId) {
                    const current = map.get(item.selectedContractItemId) ?? 0;
                    map.set(item.selectedContractItemId, current + (item.executedQuantity ?? 0));
                }
            });
        });
        return map;
    });

    // 3. O SALDO REAL (Banco - Seleção da Tela)
    readonly liveAvailableBalanceMap = computed(() => {
        const contract = this.selectedContract();
        const dbCommitted = this.committedInDatabaseMap();
        const currentScreen = this.currentSelectionMap();
        const map = new Map<number, number>();

        if (!contract) return map;

        contract.items.forEach(item => {
            const totalContracted = item.contractedQuantity ?? 0;
            const alreadyInDb = dbCommitted.get(item.contractItemId) ?? 0;
            const onScreen = currentScreen.get(item.contractItemId) ?? 0;

            map.set(item.contractItemId, totalContracted - alreadyInDb - onScreen);
        });
        return map;
    });

    onContractItemClick(event: MouseEvent, isInsufficient: boolean) {
        if (isInsufficient) {
            event.preventDefault();
            event.stopPropagation();
        }
    }


    startTimer() {
        this.intervalId = setInterval(() => {

            if (this.remainingSeconds > 0) {
                this.remainingSeconds--;
            } else {
                clearInterval(this.intervalId);
                this.validateExecution();
            }

        }, 1000);
    }


    get formattedTime(): string {

        const minutes = Math.floor(this.remainingSeconds / 60);
        const seconds = this.remainingSeconds % 60;

        return `${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}`;
    }

    private stopTimer() {
        if (this.intervalId) {
            clearInterval(this.intervalId);
            this.intervalId = null;
        }
    }


    private addLinkedItems() {
        const execution = this.execution();
        const itemsToAdd = this.linkedItemsResponse();

        if (!execution || !itemsToAdd.length) return;

        const updatedStreets = execution.streets.map(street => {
            // CORREÇÃO: Aqui você deve filtrar os itens que pertencem a ESTA rua.
            // Se o seu itemToAdd traz o ID da rua, use essa propriedade (ex: streetId).
            // Vou assumir que o itemToAdd tenha o 'directExecutionStreetId' para bater com a rua.
            const itemsForThisStreet = itemsToAdd.filter(
                item => item.directExecutionStreetId === street.directExecutionStreetId
            );

            if (itemsForThisStreet.length > 0) {
                return {
                    ...street,
                    items: [...street.items, ...itemsForThisStreet]
                };
            }
            return street;
        });

        this.execution.set({ ...execution, streets: updatedStreets });
    }

    private removeItems(itemIdsToRemove: Set<number>) {
        const execution = this.execution();

        if (!execution) return;

        // Remove pelo ID ÚNICO do item (directExecutionStreetItemId)


        const updatedStreets = execution.streets.map(street => {
            const hasItems = street.items.some(item => itemIdsToRemove.has(item.directExecutionStreetItemId));

            if (hasItems) {
                return {
                    ...street,
                    items: street.items.filter(item => !itemIdsToRemove.has(item.directExecutionStreetItemId))
                };
            }
            return street;
        });

        this.execution.set({ ...execution, streets: updatedStreets });
    }


}


