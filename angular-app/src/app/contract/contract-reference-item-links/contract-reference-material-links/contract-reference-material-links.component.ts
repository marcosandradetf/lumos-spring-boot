import {Component, EventEmitter, Input, OnChanges, Output, SimpleChanges} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {ButtonDirective} from 'primeng/button';
import {ListboxModule} from 'primeng/listbox';
import {NgClass, NgForOf, NgIf} from '@angular/common';
import {PrimeTemplate} from 'primeng/api';
import {Tag} from 'primeng/tag';
import {Tooltip} from 'primeng/tooltip';
import type {
    EditableLinkedReferenceItem,
    MaterialOption
} from '../contract-reference-item-links.component';

interface MaterialRowSelection {
    materialId: number;
    selectedItemIds: number[];
}

@Component({
  selector: 'app-contract-reference-material-links',
  standalone: true,
    imports: [
        FormsModule,
        ButtonDirective,
        ListboxModule,
        NgForOf,
        NgIf,
        PrimeTemplate,
        Tag,
        Tooltip,
        NgClass
    ],
  templateUrl: './contract-reference-material-links.component.html',
  styleUrl: './contract-reference-material-links.component.scss'
})
export class ContractReferenceMaterialLinksComponent implements OnChanges {
    @Input() items: EditableLinkedReferenceItem[] = [];
    @Input() materials: MaterialOption[] = [];
    @Input() loading: boolean = false;
    @Input() saving: boolean = false;

    @Output() materialItemsChange = new EventEmitter<MaterialRowSelection>();
    @Output() saveRequested = new EventEmitter<void>();
    @Output() createMaterialRequested = new EventEmitter<void>();

    materialSelections: Record<number, number[]> = {};
    activeMaterialId: number | null = null;

    ngOnChanges(changes: SimpleChanges): void {
        if (changes['items'] || changes['materials']) {
            this.materialSelections = this.materials.reduce<Record<number, number[]>>((acc, material) => {
                acc[material.materialId] = this.items
                    .filter(item => item.materialLinks.some(link => link.materialId === material.materialId))
                    .map(item => item.contractReferenceItemId);
                return acc;
            }, {});

            if (this.activeMaterialId !== null && !this.materials.some(material => material.materialId === this.activeMaterialId)) {
                this.activeMaterialId = null;
            }
        }
    }

    get linkedItemOptions(): EditableLinkedReferenceItem[] {
        return this.items;
    }

    getLinkedItemsCount(materialId: number): number {
        return this.materialSelections[materialId]?.length ?? 0;
    }

    getLinkedItemsLabel(materialId: number): string {
        const count = this.getLinkedItemsCount(materialId);
        return count === 0
            ? 'Nenhum item referencial vinculado'
            : `${count} ${count === 1 ? 'item referencial vinculado' : 'itens referenciais vinculados'}`;
    }

    getStatusLabel(materialId: number): string {
        return this.getLinkedItemsCount(materialId) > 0 ? 'Vinculado' : 'Sem vinculo';
    }

    getStatusSeverity(materialId: number): 'success' | 'warn' {
        return this.getLinkedItemsCount(materialId) > 0 ? 'success' : 'warn';
    }

    toggleEditor(materialId: number): void {
        this.activeMaterialId = this.activeMaterialId === materialId ? null : materialId;
    }

    isEditing(materialId: number): boolean {
        return this.activeMaterialId === materialId;
    }

    trackByMaterialId(_: number, material: MaterialOption): number {
        return material.materialId;
    }

    onSelectionChange(materialId: number, selectedItemIds: number[] | null): void {
        const nextValue = selectedItemIds ?? [];
        this.materialSelections[materialId] = nextValue;
        this.materialItemsChange.emit({materialId, selectedItemIds: nextValue});
    }
}
