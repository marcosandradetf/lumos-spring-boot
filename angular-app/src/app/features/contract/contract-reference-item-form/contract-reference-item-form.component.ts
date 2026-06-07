import {CommonModule} from '@angular/common';
import {Component, OnInit} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {Router} from '@angular/router';

import {MessageService} from 'primeng/api';
import {ButtonModule} from 'primeng/button';
import {DropdownModule} from 'primeng/dropdown';
import {InputTextModule} from 'primeng/inputtext';
import {TagModule} from 'primeng/tag';
import {ToastModule} from 'primeng/toast';

import {
    ContractReferenceItemBaseManagementDTO,
    SaveContractReferenceItemBaseDTO,
} from '../contract-models';
import {ContractService} from '../services/contract.service';
import {SharedState} from '../../../core/service/shared-state';
import {isEqual, cloneDeep} from 'lodash';
import {Tooltip} from 'primeng/tooltip';

interface ContractReferenceItemTypeOption {
    label: string;
    value: string;
    link?: string;
}

interface EditableBaseReferenceItem {
    draftId: string;
    contractReferenceItemId: number | null;
    description: string;
    type: string | null;
    status: string;
}

@Component({
    selector: 'app-contract-reference-item-form',
    standalone: true,
    imports: [
        CommonModule,
        FormsModule,
        InputTextModule,
        DropdownModule,
        ButtonModule,
        TagModule,
        ToastModule,
        Tooltip,
    ],
    providers: [MessageService],
    templateUrl: './contract-reference-item-form.component.html',
})
export class ContractReferenceItemFormComponent implements OnInit {
    readonly typeOptions: ContractReferenceItemTypeOption[] = [
        {label: 'BRAÇO', value: 'BRAÇO', link: 'CABO'},
        {label: 'REFLETOR', value: 'REFLETOR', },
        {label: 'LED', value: 'LED'},
        {label: 'PORCA', value: 'PORCA'},
        {label: 'FITA ISOLANTE ADESIVO', value: 'FITA ISOLANTE ADESIVO'},
        {label: 'POSTE', value: 'POSTE'},
        {label: 'EXTENSÃO DE REDE', value: 'EXTENSÃO DE REDE'},
        {label: 'CINTA', value: 'CINTA'},
        {label: 'PARAFUSO', value: 'PARAFUSO'},
        {label: 'FITA ISOLANTE AUTOFUSÃO', value: 'FITA ISOLANTE AUTOFUSÃO'},
        {label: 'CONECTOR', value: 'CONECTOR'},
        {label: 'POSTE GALVANIZADO', value: 'POSTE GALVANIZADO'},
        {label: 'CABO', value: 'CABO'},
        {label: 'RELÉ', value: 'RELÉ'},
        {label: 'SERVIÇO', value: 'SERVIÇO', link: 'ITEM'},
        {label: 'PROJETO', value: 'PROJETO', link: 'ITEM'},
        {label: 'CIMENTO', value: 'CIMENTO'},
        {label: 'POSTE CIMENTO', value: 'POSTE CIMENTO'},
        {label: 'MANUTENÇÃO', value: 'MANUTENÇÃO'},
    ];

    readonly quickAddOptions = [1, 3];

    items: EditableBaseReferenceItem[] = [];
    itemsBackup: EditableBaseReferenceItem[] = [];
    quickAddMenuOpen = false;
    loading = true;
    saving = false;
    formSubmitted = false;

    constructor(
        private readonly messageService: MessageService,
        private readonly router: Router,
        private readonly contractService: ContractService,
    ) {
    }

    ngOnInit(): void {
        SharedState.setCurrentPath(['Contratos', 'Cadastrar Itens']);

        this.contractService.getReferenceItemBaseManagement().subscribe({
            next: (referenceItems) => {
                this.items = referenceItems.length > 0
                    ? referenceItems.map(item => this.mapToEditableItem(item))
                    : [this.createDraftItem()];
                this.itemsBackup = cloneDeep(this.items);
                this.loading = false;
            },
            error: () => {
                this.loading = false;
                this.items = [this.createDraftItem()];
                this.itemsBackup = cloneDeep(this.items);
                this.messageService.add({
                    severity: 'error',
                    summary: 'Falha ao carregar dados',
                    detail: 'Nao foi possivel carregar os itens referenciais.',
                });
            },
        });
    }

    get draftItemsCount(): number {
        return this.items.filter(item => item.contractReferenceItemId === null).length;
    }

    get hasDraftItems(): boolean {
        return this.draftItemsCount > 0;
    }

    toggleQuickAddMenu(): void {
        this.quickAddMenuOpen = !this.quickAddMenuOpen;
    }

    newItem(): void {
        this.addItemsBatch(1);
    }

    addItemsBatch(count: number): void {
        for (let index = 0; index < count; index += 1) {
            this.items.unshift(this.createDraftItem());
        }

        this.quickAddMenuOpen = false;
    }

    removeDraftItems(): void {
        this.items = this.items.filter(item => item.contractReferenceItemId !== null);
        if (this.items.length === 0) {
            this.items = [this.createDraftItem()];
        }
        this.itemsBackup = cloneDeep(this.items);
    }

    removeItem(item: EditableBaseReferenceItem): void {
        this.items = this.items.filter(current => current.draftId !== item.draftId);
        if (this.items.length === 0) {
            this.items = [this.createDraftItem()];
        }
        this.itemsBackup = cloneDeep(this.items);
    }

    goToLinkManagement(): void {
        void this.router.navigate(
            ['/contratos/itens-contratuais/vinculos'],
            {
                queryParams: {
                    operation: 'item'
                }
            }
        );
    }

    saveItem(item: EditableBaseReferenceItem): void {
        this.formSubmitted = true;
        if (!this.isBaseItemValid(item)) {
            this.messageService.add({
                severity: 'warn',
                summary: 'Linha incompleta',
                detail: 'Descricao e tipo sao obrigatorios.',
            });
            return;
        }

        this.saving = true;
        this.contractService.saveReferenceItemsBase([this.toPayload(item)]).subscribe({
            next: (response) => {
                const saved = response[0];
                if (saved) {
                    this.replaceItem(item, this.mapToEditableItem(saved));
                }

                this.saving = false;
                this.messageService.add({
                    severity: 'success',
                    summary: 'Cadastro salvo',
                    detail: 'Item salvo com sucesso.',
                });
            },
            error: (error) => {
                this.saving = false;
                this.messageService.add({
                    severity: 'error',
                    summary: 'Falha ao salvar',
                    detail: error?.error?.error ?? 'Nao foi possivel salvar o item.',
                });
            },
        });
    }

    submit(): void {
        this.formSubmitted = true;
        const changedItems = this.items.filter(item => {
            const original =
                this.itemsBackup.find(x => x.contractReferenceItemId === item.contractReferenceItemId);

            return !isEqual(item, original);
        });

        if (changedItems.length === 0) {
            this.saving = true;
            setTimeout(() => {
                this.saving = false;
            }, 10);
            return;
        }

        const invalidItems = changedItems.filter(item => !this.isBaseItemValid(item));
        if (invalidItems.length > 0) {
            this.messageService.add({
                severity: 'error',
                summary: 'Existem linhas incompletas',
                detail: 'Descricao e tipo precisam ser preenchidos nas linhas selecionadas.',
            });
            return;
        }

        this.saving = true;
        this.contractService.saveReferenceItemsBase(changedItems.map(item => this.toPayload(item))).subscribe({
            next: (response) => {
                const responseMap = new Map(response.map(item => [item.contractReferenceItemId, item]));
                this.items = this.items.map(item => {
                    const saved = item.contractReferenceItemId !== null
                        ? responseMap.get(item.contractReferenceItemId)
                        : response.find(candidate =>
                            candidate.description === item.description &&
                            candidate.type === item.type
                        );

                    return saved ? this.mapToEditableItem(saved) : item;
                });
                this.itemsBackup = cloneDeep(this.items);

                this.saving = false;
                this.messageService.add({
                    severity: 'success',
                    summary: 'Cadastros salvos',
                    detail: 'Os itens foram salvos como pendentes de validacao. Complete os vinculos na tela dedicada.',
                });
            },
            error: (error) => {
                this.saving = false;
                this.messageService.add({
                    severity: 'error',
                    summary: 'Falha ao salvar',
                    detail: error?.error?.message ?? 'Nao foi possivel salvar os itens selecionados.',
                });
            },
        });
    }

    itemHasError(item: EditableBaseReferenceItem, field: 'description' | 'type'): boolean {
        if (!this.formSubmitted) {
            return false;
        }

        if (field === 'description') {
            return !item.description.trim();
        }

        return !item.type;
    }

    getStatusLabel(item: EditableBaseReferenceItem): string {
        return item.status === 'ACTIVE' ? 'Ativo' : item.status;
    }

    getStatusSeverity(item: EditableBaseReferenceItem): 'success' | 'warn' {
        return item.status === 'ACTIVE' ? 'success' : 'warn';
    }

    private mapToEditableItem(item: ContractReferenceItemBaseManagementDTO): EditableBaseReferenceItem {
        return {
            draftId: this.generateDraftId(),
            contractReferenceItemId: item.contractReferenceItemId,
            description: item.description,
            type: item.type,
            status: item.status,
        };
    }

    private isBaseItemValid(item: EditableBaseReferenceItem): boolean {
        return !!item.description.trim() && !!item.type;
    }

    private toPayload(item: EditableBaseReferenceItem): SaveContractReferenceItemBaseDTO {
        const link = this.typeOptions.find(t => t.value === item.type);

        return {
            clientDraftId: item.contractReferenceItemId === null ? item.draftId : null,
            contractReferenceItemId: item.contractReferenceItemId,
            description: item.description.trim(),
            type: item.type,
            link: link?.link
        };
    }

    private createDraftItem(): EditableBaseReferenceItem {
        return {
            draftId: this.generateDraftId(),
            contractReferenceItemId: null,
            description: '',
            type: null,
            status: '',
        };
    }


    private generateDraftId(): string {
        return `${Date.now()}-${Math.random().toString(36).slice(2, 8)}`;
    }

    private replaceItem(target: EditableBaseReferenceItem, replacement: EditableBaseReferenceItem): void {
        this.items = this.items.map(item => item.draftId === target.draftId ? replacement : item);
        this.itemsBackup = cloneDeep(this.items);
    }


}
