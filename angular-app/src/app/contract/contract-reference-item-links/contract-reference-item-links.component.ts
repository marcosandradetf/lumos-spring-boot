import {CommonModule} from '@angular/common';
import {Component, Input, OnInit} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {ActivatedRoute, Router, RouterOutlet} from '@angular/router';
import {forkJoin} from 'rxjs';

import {MessageService} from 'primeng/api';
import {ButtonModule} from 'primeng/button';
import {MultiSelectModule} from 'primeng/multiselect';
import {TagModule} from 'primeng/tag';
import {ToastModule} from 'primeng/toast';

import {ContractReferenceItemManagementDTO, SaveContractReferenceItemLinksDTO} from '../contract-models';
import {ContractService} from '../services/contract.service';
import {MaterialService} from '../../stock/services/material.service';
import {Tooltip} from 'primeng/tooltip';
import {GuideStateComponent, GuideStateOptions} from '../../guide-state/guide-state.component';
import {Utils} from '../../core/service/utils';
import {SharedState} from '../../core/service/shared-state';

interface MaterialOption {
    materialId: number;
    materialName: string;
    nameForImport?: string;
    unitBase?: string;
}

interface LinkedReferenceItemOption {
    linkKey: string;
    contractReferenceItemId: number;
    description: string;
    type: string | null;
}

interface EditableLinkedReferenceItem {
    contractReferenceItemId: number;
    description: string;
    type: string | null;
    materialLinks: MaterialOption[];
    dependencyLinks: LinkedReferenceItemOption[];
    status: 'ACTIVE' | 'PENDING_VALIDATION';
}

type LinkOperationMode = 'item' | 'material';

@Component({
    selector: 'app-contract-reference-item-links',
    standalone: true,
    imports: [
        CommonModule,
        FormsModule,
        MultiSelectModule,
        ButtonModule,
        TagModule,
        ToastModule,
        RouterOutlet,
        Tooltip,
        GuideStateComponent,
    ],
    providers: [MessageService],
    templateUrl: './contract-reference-item-links.component.html',
})
export class ContractReferenceItemLinksComponent implements OnInit {
    readonly dependencyDrivenTypes = new Set(['SERVIÇO', 'PROJETO']);
    readonly materialOptionalTypes = new Set(['EXTENSÃO DE REDE', 'MANUTENÇÃO']);

    operationMode: LinkOperationMode = 'item';
    items: EditableLinkedReferenceItem[] = [];
    allMaterials: MaterialOption[] = [];
    persistedReferenceItems: LinkedReferenceItemOption[] = [];
    materialFrameVisible = false;
    loading = true;
    saving = false;

    constructor(
        private readonly messageService: MessageService,
        private readonly router: Router,
        private readonly route: ActivatedRoute,
        private readonly contractService: ContractService,
        private readonly materialService: MaterialService,
    ) {
    }

    ngOnInit(): void {
        SharedState.setCurrentPath(['Contratos','Vincular Itens']);

        this.route.queryParamMap.subscribe(params => {
            const requestedMode = params.get('operation');
            this.operationMode = requestedMode === 'item' ? 'item' : 'material';
        });

        forkJoin({
            referenceItems: this.contractService.getReferenceItemLinkManagement(),
            materials: this.materialService.getCatalogue(),
        }).subscribe({
            next: ({referenceItems, materials}) => {
                this.allMaterials = (materials ?? []).map(material => ({
                    materialId: Number(material.materialId),
                    materialName: material.materialName,
                    nameForImport: material.nameForImport ?? material.materialName,
                    unitBase: material.requestUnit ?? material.unitBase ?? material.buyUnit,
                })).filter(material => !!material.materialId && !!material.materialName);

                this.persistedReferenceItems = (referenceItems ?? []).map(item => ({
                    linkKey: this.buildLinkKey(item.contractReferenceItemId!),
                    contractReferenceItemId: item.contractReferenceItemId!,
                    description: item.description,
                    type: item.type,
                }));

                this.items = referenceItems.map(item => this.mapToEditableItem(item));
                this.loading = false;
            },
            error: (error) => {
                Utils.handleHttpError(error, this.router);
                this.loading = false;
            },
        });
    }

    requiresMaterial(item: EditableLinkedReferenceItem): boolean {
        if (!item.type) {
            return false;
        }

        if (this.requiresDependency(item)) {
            return false;
        }

        return !this.materialOptionalTypes.has(item.type);
    }

    requiresDependency(item: EditableLinkedReferenceItem): boolean {
        return !!item.type && this.dependencyDrivenTypes.has(item.type);
    }

    get pageTitle(): string {
        return this.isItemMode ? 'Vínculo de Itens Referenciais' : 'Vínculo de Materiais Referenciais';
    }

    get pageDescription(): string {
        return this.isItemMode
            ? 'Nesta operação você vincula apenas itens de referência. Os materiais permanecem preservados e fora de edição.'
            : 'Nesta operação você vincula apenas materiais. Os itens de referência permanecem preservados e fora de edição.';
    }

    get sectionTitle(): string {
        return this.isItemMode ? 'Vínculos de itens' : 'Vínculos de materiais';
    }

    get sectionDescription(): string {
        return this.isItemMode
            ? 'A descrição e o tipo já vieram do cadastro base. Aqui você gerencia somente os itens vinculados.'
            : 'A descrição e o tipo já vieram do cadastro base. Aqui você gerencia somente os materiais vinculados.';
    }

    get saveButtonLabel(): string {
        return this.isItemMode ? 'Salvar todos os itens vinculados' : 'Salvar todos os materiais vinculados';
    }

    get singleSaveButtonLabel(): string {
        return this.isItemMode ? 'Salvar este item' : 'Salvar este material';
    }

    get isItemMode(): boolean {
        return this.operationMode === 'item';
    }

    get isMaterialMode(): boolean {
        return this.operationMode === 'material';
    }

    switchOperation(mode: LinkOperationMode): void {
        void this.router.navigate([], {
            relativeTo: this.route,
            queryParams: {operation: mode},
            queryParamsHandling: 'merge',
        });
    }

    getLinkedItemOptions(currentItem: EditableLinkedReferenceItem): LinkedReferenceItemOption[] {
        return this.persistedReferenceItems.filter(option =>
            option.contractReferenceItemId !== currentItem.contractReferenceItemId
        );
    }

    onMaterialSelect(item: EditableLinkedReferenceItem, selected: MaterialOption[] | null): void {
        item.materialLinks = selected ?? [];
        item.status = this.resolveStatus(item);
    }

    onDependencySelect(item: EditableLinkedReferenceItem, selected: LinkedReferenceItemOption[] | null): void {
        item.dependencyLinks = selected ?? [];
        item.status = this.resolveStatus(item);
    }

    openMaterialScreen(): void {
        this.materialFrameVisible = true;
        void this.router.navigate(['material-create'], {relativeTo: this.route});
    }

    goToBaseRegistration(): void {
        void this.router.navigate(['/contratos/itens-contratuais/cadastro']);
    }

    saveItem(item: EditableLinkedReferenceItem): void {
        this.saving = true;
        this.contractService.saveReferenceItemLinks([this.toPayload(item)]).subscribe({
            next: (response) => {
                const saved = response[0];
                if (saved) {
                    this.replaceItem(item, this.mapToEditableItem(saved));
                    this.syncPersistedReferenceOptions(response);
                }

                this.saving = false;
                this.messageService.add({
                    severity: saved?.status === 'ACTIVE' ? 'success' : 'warn',
                    summary: saved?.status === 'ACTIVE' ? 'Item ativo' : 'Item pendente de validacao',
                    detail: saved ? this.getStatusMessage(this.mapToEditableItem(saved)) : this.getStatusMessage(item),
                });
            },
            error: (error) => {
                this.saving = false;
                this.messageService.add({
                    severity: 'error',
                    summary: 'Falha ao salvar',
                    detail: error?.error?.message ?? 'Nao foi possivel salvar os vinculos do item.',
                });
            },
        });
    }

    submit(): void {
        if (this.items.length === 0) {
            this.messageService.add({
                severity: 'warn',
                summary: 'Nenhum registro disponivel',
                detail: 'Nao ha registros para salvar nesta tela.',
            });
            return;
        }

        this.saving = true;
        this.contractService.saveReferenceItemLinks(this.items.map(item => this.toPayload(item))).subscribe({
            next: (response) => {
                const responseMap = new Map(response.map(item => [item.contractReferenceItemId, item]));
                this.items = this.items.map(item => {
                    const saved = responseMap.get(item.contractReferenceItemId);
                    return saved ? this.mapToEditableItem(saved) : item;
                });

                this.syncPersistedReferenceOptions(response);
                this.saving = false;
                this.messageService.add({
                    severity: 'success',
                    summary: 'Vinculos salvos',
                    detail: `${response.length} ${response.length === 1 ? 'registro atualizado' : 'registros atualizados'}.`,
                });
            },
            error: (error) => {
                this.saving = false;
                this.messageService.add({
                    severity: 'error',
                    summary: 'Falha ao salvar',
                    detail: error?.error?.message ?? 'Nao foi possivel salvar os registros desta tela.',
                });
            },
        });
    }

    getStatusLabel(item: EditableLinkedReferenceItem): string {
        return item.status === 'ACTIVE' ? 'Ativo' : 'Pendente de validacao';
    }

    getStatusSeverity(item: EditableLinkedReferenceItem): 'success' | 'warn' {
        return item.status === 'ACTIVE' ? 'success' : 'warn';
    }

    getStatusMessage(item: EditableLinkedReferenceItem): string {
        if (this.isItemMode) {
            return this.requiresDependency(item)
                ? 'Servico e projeto exigem item de referencia vinculado.'
                : 'Este tipo nao exige item vinculado, mas a relacao pode ser mantida aqui.';
        }

        if (this.isMaterialMode) {
            return this.requiresMaterial(item)
                ? 'Este tipo exige material vinculado para ativacao.'
                : 'Este tipo nao exige material, mas o vinculo pode ser mantido aqui.';
        }

        if (item.status === 'ACTIVE') {
            return 'Todos os vinculos obrigatorios foram informados.';
        }

        if (this.requiresDependency(item)) {
            return 'Servico e projeto exigem item de referencia vinculado.';
        }

        if (this.requiresMaterial(item)) {
            return 'Este tipo exige material vinculado para ativacao.';
        }

        return 'Revise os vinculos e salve novamente.';
    }

    getLinkCountLabel(item: EditableLinkedReferenceItem): string {
        const count = this.isItemMode ? item.dependencyLinks.length : item.materialLinks.length;

        if (this.isItemMode) {
            if (count === 0) {
                return 'Nenhum item vinculado';
            }

            return `${count} ${count === 1 ? 'item vinculado' : 'itens vinculados'}`;
        }

        if (count === 0) {
            return 'Nenhum material vinculado';
        }

        return `${count} ${count === 1 ? 'material vinculado' : 'materiais vinculados'}`;
    }

    private mapToEditableItem(item: ContractReferenceItemManagementDTO): EditableLinkedReferenceItem {
        return {
            contractReferenceItemId: item.contractReferenceItemId!,
            description: item.description,
            type: item.type,
            materialLinks: item.materialLinks.map(material => ({
                materialId: material.materialId,
                materialName: material.materialName,
            })),
            dependencyLinks: item.dependencyLinks.map(link => ({
                linkKey: this.buildLinkKey(link.contractReferenceItemId),
                contractReferenceItemId: link.contractReferenceItemId,
                description: link.description,
                type: link.type,
            })),
            status: item.status,
        };
    }

    private resolveStatus(item: EditableLinkedReferenceItem): 'ACTIVE' | 'PENDING_VALIDATION' {
        if (this.requiresDependency(item) && item.dependencyLinks.length === 0) {
            return 'PENDING_VALIDATION';
        }

        if (this.requiresMaterial(item) && item.materialLinks.length === 0) {
            return 'PENDING_VALIDATION';
        }

        return 'ACTIVE';
    }

    private toPayload(item: EditableLinkedReferenceItem): SaveContractReferenceItemLinksDTO {
        return {
            contractReferenceItemId: item.contractReferenceItemId,
            materialIds: item.materialLinks.map(material => material.materialId),
            dependencyReferenceItemIds: item.dependencyLinks.map(link => link.contractReferenceItemId),
        };
    }

    private syncPersistedReferenceOptions(items: ContractReferenceItemManagementDTO[]): void {
        const merged = new Map<number, LinkedReferenceItemOption>();

        this.persistedReferenceItems.forEach(item => {
            merged.set(item.contractReferenceItemId, item);
        });

        items.forEach(item => {
            if (item.contractReferenceItemId !== null) {
                merged.set(item.contractReferenceItemId, {
                    linkKey: this.buildLinkKey(item.contractReferenceItemId),
                    contractReferenceItemId: item.contractReferenceItemId,
                    description: item.description,
                    type: item.type,
                });
            }
        });

        this.persistedReferenceItems = Array.from(merged.values()).sort((a, b) =>
            a.description.localeCompare(b.description)
        );
    }

    private replaceItem(target: EditableLinkedReferenceItem, replacement: EditableLinkedReferenceItem): void {
        this.items = this.items.map(item =>
            item.contractReferenceItemId === target.contractReferenceItemId ? replacement : item
        );
    }

    private buildLinkKey(contractReferenceItemId: number): string {
        return `id-${contractReferenceItemId}`;
    }


    get guideOptions(): GuideStateOptions {
        const linkingItems = this.operationMode === 'item';
        const emptyItems = this.items.length === 0;
        const emptyMaterials = this.allMaterials.length === 0;

        return {
            enabled: (linkingItems && emptyItems) || (!linkingItems && (emptyItems || emptyMaterials)),
            title: 'Antes de editar os vínculos dos itens contratuais...',
            subtitle: 'Para continuar, primeiro siga as etapas abaixo que são necessárias para vincular os itens corretamente.',
            icon: 'pi pi-link',
            steps: [
                [emptyItems, "Cadastrar catálogo de Base de Item Referencial"],
                [!linkingItems && emptyMaterials, "Cadastrar catálogo de materiais no sistema"],
            ],
            hasButtonLabel: emptyItems,
            buttonLabel: 'Ir para cadastro de itens',
            buttonRoute: '/contratos/itens-contratuais/cadastro',
            buttonIcon: 'pi pi-list',
            hasSecondaryLabel: !linkingItems && emptyMaterials,
            secondaryLabel: 'Ir para cadastro de materiais',
            secondaryRoute: '/estoque/cadastrar-material',
            secondaryIcon: 'pi pi-box',
            hasTertiaryLabel: false,
            tertiaryLabel: '',
            tertiaryRoute: '',
            tertiaryIcon: ''
        };
    }


}
