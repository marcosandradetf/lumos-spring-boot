import {CommonModule, Location} from '@angular/common';
import {Component, OnInit} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {ActivatedRoute, Router, RouterLink, RouterOutlet} from '@angular/router';
import {forkJoin} from 'rxjs';

import {MessageService} from 'primeng/api';
import {ButtonModule} from 'primeng/button';
import {ListboxModule} from 'primeng/listbox';
import {TagModule} from 'primeng/tag';
import {ToastModule} from 'primeng/toast';

import {ContractReferenceItemManagementDTO, SaveContractReferenceItemLinksDTO} from '../contract-models';
import {ContractService} from '../services/contract.service';
import {MaterialService} from '../../stock/services/material.service';
import {Tooltip} from 'primeng/tooltip';
import {GuideStateComponent, GuideStateOptions} from '../../guide-state/guide-state.component';
import {Utils} from '../../core/service/utils';
import {SharedState} from '../../core/service/shared-state';
import {
    ContractReferenceMaterialLinksComponent
} from './contract-reference-material-links/contract-reference-material-links.component';
import {Title} from '@angular/platform-browser';
import {ButtonBackComponent} from '../../shared/components/button-back/button-back.component';

export interface MaterialOption {
    materialId: number;
    materialName: string;
    nameForImport?: string;
    unitBase?: string;
    status?: string;
}

interface LinkedReferenceItemOption {
    linkKey: string;
    contractReferenceItemId: number;
    description: string;
    type: string | null;
}

export interface EditableLinkedReferenceItem {
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
        ListboxModule,
        ButtonModule,
        TagModule,
        ToastModule,
        RouterOutlet,
        Tooltip,
        GuideStateComponent,
        ContractReferenceMaterialLinksComponent,
    ],
    providers: [MessageService],
    templateUrl: './contract-reference-item-links.component.html',
})
export class ContractReferenceItemLinksComponent implements OnInit {
    readonly dependencyDrivenTypes = new Set(['SERVIÇO', 'PROJETO', 'BRAÇO']);
    readonly dependencyDrivenTypesMaterials = new Set(['SERVIÇO', 'PROJETO', 'MANUTENÇÃO', 'EXTENSÃO DE REDE', 'CEMIG']);

    operationMode: LinkOperationMode = 'item';
    items: EditableLinkedReferenceItem[] = [];
    itemModeItems: EditableLinkedReferenceItem[] = [];
    materialModeItems: EditableLinkedReferenceItem[] = [];
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
        protected readonly location: Location,
        protected readonly title: Title
    ) {
    }

    ngOnInit(): void {
        this.route.queryParamMap.subscribe(params => {
            const requestedMode = params.get('operation');
            this.operationMode = requestedMode === 'item' ? 'item' : 'material';
        });

        this.title.setTitle(this.isItemMode ? "Lumos IP - Vínculo de Itens Referenciais" : "Vínculo de Itens Referenciais a Materiais");
        SharedState.setCurrentPath([
            'Contratos',
            this.isItemMode ? "Vincular Serviços" : "Víncular Materiais"
        ]);

        forkJoin({
            referenceItems: this.contractService.getReferenceItemLinkManagement(),
            materials: this.materialService.getCatalogue(true),
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
                this.refreshModeItems();

                this.loading = false;
            },
            error: (error) => {
                Utils.handleHttpError(error, this.router);
                this.loading = false;
            },
        });
    }

    requiresDependency(item: EditableLinkedReferenceItem): boolean {
        return !!item.type && this.dependencyDrivenTypes.has(item.type);
    }

    requiresMaterialDependency(item: EditableLinkedReferenceItem): boolean {
        return !!item.type && !this.dependencyDrivenTypesMaterials.has(item.type);
    }

    get pageTitle(): string {
        return this.isItemMode ? 'Vínculo de Itens Referenciais' : 'Vínculo de Materiais a Itens Referenciais';
    }

    get pageDescription(): string {
        if (this.isItemMode) {
            return 'Vincule serviços e projetos aos itens. Você também pode associar dependências entre itens (ex: Braço 🔗 Cabo).';
        }
        return 'Vincule itens contratuais de referência aos materiais.';
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

    get currentModeItems(): EditableLinkedReferenceItem[] {
        return this.isItemMode ? this.itemModeItems : this.materialModeItems;
    }

    get currentRecordCount(): number {
        return this.isItemMode ? this.itemModeItems.length : this.allMaterials.length;
    }

    get hasCurrentModeRecords(): boolean {
        return this.currentRecordCount > 0;
    }

    switchOperation(mode: LinkOperationMode): void {
        void this.router.navigate([], {
            relativeTo: this.route,
            queryParams: {operation: mode},
            queryParamsHandling: 'merge',
        });
    }

    getLinkedItemOptions(currentItem: EditableLinkedReferenceItem): LinkedReferenceItemOption[] {
        const status = currentItem.status;
        const itemSearch = status.split("com ")[1].trim().toLowerCase();

        return this.persistedReferenceItems.filter(option =>
            option.contractReferenceItemId !== currentItem.contractReferenceItemId && (option.type?.toLowerCase() === itemSearch || itemSearch === "item")
        );
    }

    onDependencySelect(item: EditableLinkedReferenceItem, selected: LinkedReferenceItemOption[] | null): void {
        item.dependencyLinks = selected ?? [];
    }


    onItemMaterialsChange(itemId: number, selectedMaterialIds: number[]): void {
        const item = this.materialModeItems.find(i => i.contractReferenceItemId === itemId);
        if (!item) {
            return;
        }

        item.materialLinks = selectedMaterialIds
            .map(materialId => this.allMaterials.find(m => m.materialId === materialId))
            .filter(Boolean) as MaterialOption[];
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

    saveMaterialLinks(): void {
        if (this.materialModeItems.length === 0) {
            this.messageService.add({
                severity: 'warn',
                summary: 'Nenhum item disponivel',
                detail: 'Nao ha itens referenciais elegiveis para vincular aos materiais.',
            });
            return;
        }

        this.saving = true;
        this.contractService.saveReferenceItemLinks(this.materialModeItems.map(item => this.toPayload(item))).subscribe({
            next: (response) => {
                const responseMap = new Map(response.map(item => [item.contractReferenceItemId, item]));
                this.items = this.items.map(item => {
                    const saved = responseMap.get(item.contractReferenceItemId);
                    return saved ? this.mapToEditableItem(saved) : item;
                });

                this.refreshModeItems();
                this.syncPersistedReferenceOptions(response);
                this.saving = false;
                this.messageService.add({
                    severity: 'success',
                    summary: 'Materiais salvos',
                    detail: `${response.length} ${response.length === 1 ? 'registro atualizado' : 'registros atualizados'}.`,
                });
            },
            error: (error) => {
                this.saving = false;
                this.messageService.add({
                    severity: 'error',
                    summary: 'Falha ao salvar',
                    detail: error?.error?.message ?? 'Nao foi possivel salvar os vinculos de materiais.',
                });
            },
        });
    }

    submit(): void {
        if (this.currentModeItems.length === 0) {
            this.messageService.add({
                severity: 'warn',
                summary: 'Nenhum registro disponivel',
                detail: 'Nao ha registros para salvar nesta tela.',
            });
            return;
        }

        this.saving = true;
        this.contractService.saveReferenceItemLinks(this.currentModeItems.map(item => this.toPayload(item))).subscribe({
            next: (response) => {
                const responseMap = new Map(response.map(item => [item.contractReferenceItemId, item]));
                this.items = this.items.map(item => {
                    const saved = responseMap.get(item.contractReferenceItemId);
                    return saved ? this.mapToEditableItem(saved) : item;
                });

                this.refreshModeItems();
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
        return item.status === 'ACTIVE' ? 'Ativo' : item.status;
    }

    getStatusSeverity(item: EditableLinkedReferenceItem): 'success' | 'warn' {
        return item.status === 'ACTIVE' ? 'success' : 'warn';
    }

    getStatusMessage(item: EditableLinkedReferenceItem): string {
        return this.requiresDependency(item)
            ? Utils.capitalize(item.type ?? '') + ' exige item de referencia vinculado.'
            : 'Este tipo nao exige item vinculado, mas a relacao pode ser mantida aqui.';
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
        this.refreshModeItems();
    }

    private buildLinkKey(contractReferenceItemId: number): string {
        return `id-${contractReferenceItemId}`;
    }

    private refreshModeItems(): void {
        this.itemModeItems = this.items.filter(item => this.requiresDependency(item));
        this.materialModeItems = this.items.filter(item => this.requiresMaterialDependency(item));
    }


    get guideOptions(): GuideStateOptions {
        const linkingItems = this.operationMode === 'item';
        const emptyItems = this.itemModeItems.length === 0;
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


    protected readonly Utils = Utils;
}
