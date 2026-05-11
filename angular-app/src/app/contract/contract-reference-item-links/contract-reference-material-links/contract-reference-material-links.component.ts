import {
    Component,
    EventEmitter,
    Input,
    OnChanges,
    Output,
    SimpleChanges
} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {ButtonDirective} from 'primeng/button';
import {ListboxModule} from 'primeng/listbox';
import {NgClass, NgForOf, NgIf} from '@angular/common';
import {FilterService, PrimeTemplate} from 'primeng/api';
import {Tag} from 'primeng/tag';
import {Tooltip} from 'primeng/tooltip';
import type {
    EditableLinkedReferenceItem,
    MaterialOption
} from '../contract-reference-item-links.component';
import {RouterLink} from '@angular/router';

interface ItemMaterialSelection {
    itemId: number;
    selectedMaterialIds: number[];
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
        Tooltip,
        NgClass,
        RouterLink
    ],
  templateUrl: './contract-reference-material-links.component.html',
  styleUrl: './contract-reference-material-links.component.scss'
})
export class ContractReferenceMaterialLinksComponent implements OnChanges {
    private static readonly CUSTOM_FILTER_MODE = 'containsAllTermsIgnoreAccent';

    @Input() items: EditableLinkedReferenceItem[] = [];
    @Input() materials: MaterialOption[] = [];
    @Input() loading: boolean = false;
    @Input() saving: boolean = false;

    @Output() itemMaterialsChange = new EventEmitter<ItemMaterialSelection>();
    @Output() saveRequested = new EventEmitter<void>();
    @Output() createMaterialRequested = new EventEmitter<void>();

    itemFilters: Record<number, string> = {};
    customFilterMatchMode: string = ContractReferenceMaterialLinksComponent.CUSTOM_FILTER_MODE;

    itemSelections: Record<number, number[]> = {};
    activeItemId: number | null = null;

    constructor(private readonly filterService: FilterService) {
        this.filterService.register(
            ContractReferenceMaterialLinksComponent.CUSTOM_FILTER_MODE,
            (value: unknown, filter: unknown): boolean => this.containsAllTermsIgnoreAccent(value, filter)
        );
    }

    ngOnChanges(changes: SimpleChanges): void {
        if (changes['items'] || changes['materials']) {
            this.itemSelections = this.items.reduce<Record<number, number[]>>((acc, item) => {
                acc[item.contractReferenceItemId] = item.materialLinks.map(link => link.materialId);
                return acc;
            }, {});

            if (this.activeItemId !== null && !this.items.some(item => item.contractReferenceItemId === this.activeItemId)) {
                this.activeItemId = null;
            }
        }
    }

    getLinkedMaterialsCount(itemId: number): number {
        return this.itemSelections[itemId]?.length ?? 0;
    }

    getLinkedMaterialsLabel(itemId: number): string {
        const count = this.getLinkedMaterialsCount(itemId);
        return count === 0
            ? 'Nenhum material vinculado'
            : `${count} ${count === 1 ? 'material vinculado' : 'materiais vinculados'}`;
    }


    toggleEditor(item: EditableLinkedReferenceItem): void {
        const isClosing = this.activeItemId === item.contractReferenceItemId;

        this.activeItemId = isClosing ? null : item.contractReferenceItemId;

        if (!isClosing) {
            this.itemFilters[item.contractReferenceItemId] = item.type ?? '';
        } else {
            this.itemFilters[item.contractReferenceItemId] = '';
        }
    }

    isEditing(itemId: number): boolean {
        return this.activeItemId === itemId;
    }

    trackByItemId(_: number, item: EditableLinkedReferenceItem): number {
        return item.contractReferenceItemId;
    }

    onSelectionChange(itemId: number, selectedMaterialIds: number[] | null): void {
        const nextValue = selectedMaterialIds ?? [];
        this.itemSelections[itemId] = nextValue;
        this.itemMaterialsChange.emit({itemId, selectedMaterialIds: nextValue});
    }

    private containsAllTermsIgnoreAccent(value: unknown, filter: unknown): boolean {
        const normalizedValue = this.normalizeForSearch(value);
        const normalizedFilter = this.normalizeForSearch(filter);

        if (!normalizedFilter) {
            return true;
        }

        if (!normalizedValue) {
            return false;
        }

        const terms = normalizedFilter.split(' ').filter(Boolean);
        return terms.every(term => normalizedValue.includes(term));
    }

    private normalizeForSearch(input: unknown): string {
        return String(input ?? '')
            .normalize('NFD')
            .replace(/[\u0300-\u036f]/g, '')
            .toLowerCase()
            .trim()
            .replace(/\s+/g, ' ');
    }
}
