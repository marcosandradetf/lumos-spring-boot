import { CommonModule } from '@angular/common';
import { Component, computed, signal } from '@angular/core';
import {
    FormBuilder,
    FormControl,
    FormGroup,
    ReactiveFormsModule,
    Validators,
} from '@angular/forms';

import { InputTextModule } from 'primeng/inputtext';
import { InputNumberModule } from 'primeng/inputnumber';
import { DropdownModule } from 'primeng/dropdown';
import { AutoCompleteModule } from 'primeng/autocomplete';
import { CheckboxModule } from 'primeng/checkbox';
import { DialogModule } from 'primeng/dialog';
import { TagModule } from 'primeng/tag';
import { DividerModule } from 'primeng/divider';
import { ToastModule } from 'primeng/toast';
import { MessageService } from 'primeng/api';
import { TextareaModule } from 'primeng/textarea';

import * as XLSX from 'xlsx';
import {TableModule} from 'primeng/table';
import {Button, ButtonModule} from 'primeng/button';
import {Sidebar} from 'primeng/sidebar';
import {ActivatedRoute, Router, RouterOutlet} from '@angular/router';

interface ContractReferenceItemTypeOption {
    label: string;
    value: string;
    requiresMaterial: boolean;
}

interface MaterialOption {
    idMaterial: number;
    materialName: string;
    nameForImport?: string;
    unitBase?: string;
    stockQuantity?: number;
    truckStockControl?: boolean;
}

@Component({
    selector: 'app-contract-reference-item-form',
    standalone: true,
    imports: [
        CommonModule,
        ReactiveFormsModule,
        InputTextModule,
        InputNumberModule,
        DropdownModule,
        AutoCompleteModule,
        CheckboxModule,
        DialogModule,
        ButtonModule,
        TagModule,
        DividerModule,
        ToastModule,
        TextareaModule,
        TableModule,
        Button,
        Sidebar,
        RouterOutlet,
    ],
    providers: [MessageService],
    templateUrl: './contract-reference-item-form.component.html',
})
export class ContractReferenceItemFormComponent {
    readonly typeOptions: ContractReferenceItemTypeOption[] = [
        { label: 'BRAÇO', value: 'BRAÇO', requiresMaterial: true },
        { label: 'REFLETOR', value: 'REFLETOR', requiresMaterial: true },
        { label: 'LED', value: 'LED', requiresMaterial: true },
        { label: 'PORCA', value: 'PORCA', requiresMaterial: true },
        { label: 'FITA ISOLANTE ADESIVO', value: 'FITA ISOLANTE ADESIVO', requiresMaterial: true },
        { label: 'POSTE', value: 'POSTE', requiresMaterial: true },
        { label: 'EXTENSÃO DE REDE', value: 'EXTENSÃO DE REDE', requiresMaterial: false },
        { label: 'CINTA', value: 'CINTA', requiresMaterial: true },
        { label: 'PARAFUSO', value: 'PARAFUSO', requiresMaterial: true },
        { label: 'FITA ISOLANTE AUTOFUSÃO', value: 'FITA ISOLANTE AUTOFUSÃO', requiresMaterial: true },
        { label: 'CONECTOR', value: 'CONECTOR', requiresMaterial: true },
        { label: 'POSTE GALVANIZADO', value: 'POSTE GALVANIZADO', requiresMaterial: true },
        { label: 'CABO', value: 'CABO', requiresMaterial: true },
        { label: 'RELÉ', value: 'RELÉ', requiresMaterial: true },
        { label: 'SERVIÇO', value: 'SERVIÇO', requiresMaterial: false },
        { label: 'PROJETO', value: 'PROJETO', requiresMaterial: false },
        { label: 'CIMENTO', value: 'CIMENTO', requiresMaterial: true },
        { label: 'POSTE CIMENTO', value: 'POSTE CIMENTO', requiresMaterial: true },
        { label: 'MANUTENÇÃO', value: 'MANUTENÇÃO', requiresMaterial: false },
    ];

    readonly materialFreeTypes = new Set([
        'SERVIÇO',
        'EXTENSÃO DE REDE',
        'CEMIG',
        'PROJETO',
        'MANUTENÇÃO',
    ]);

    private readonly allMaterials: MaterialOption[] = [
        {
            idMaterial: 101,
            materialName: 'BRAÇO GALVANIZADO PADRÃO CEMIG ATÉ 1,5M',
            nameForImport: 'BRAÇO DE 1,5',
            unitBase: 'UN',
            stockQuantity: 25,
            truckStockControl: true,
        },
        {
            idMaterial: 102,
            materialName: 'LUMINÁRIA LED 150W',
            nameForImport: 'LED 150W',
            unitBase: 'UN',
            stockQuantity: 18,
            truckStockControl: true,
        },
    ];

    filteredMaterials: MaterialOption[] = [];
    materialDialogVisible = false;

    readonly selectedType = signal<string | null>(null);
    readonly selectedMaterial = signal<MaterialOption | null>(null);

    readonly requiresMaterial = computed(() => {
        const type = this.selectedType();
        return !!type && !this.materialFreeTypes.has(type);
    });

    importedItems: any[] = [];

    constructor(
        private readonly fb: FormBuilder,
        private readonly messageService: MessageService,
        private router: Router,
        private route: ActivatedRoute
    ) {
        this.form = this.fb.group({
            description: ['', [Validators.required]],
            type: [null, Validators.required],
            linking: [''],
            itemDependency: [''],
            nameForImport: ['', [Validators.required]],
            factor: [1, [Validators.required]],
            truckStockControl: [true],
            material: [null],
        });

        this.materialCreateForm = this.fb.group({
            materialName: ['', Validators.required],
            nameForImport: ['', Validators.required],
            unitBase: ['UN'],
            defaultQuantity: [1],
            truckStockControl: [true],
        });

        this.form.get('type')?.valueChanges.subscribe((value) => {
            this.selectedType.set(value);
            this.selectedMaterial.set(null);
            this.form.get('material')?.setValue(null);
            this.syncMaterialValidator();
        });
    }

    readonly form: FormGroup;

    readonly materialCreateForm: FormGroup;

    get f() {
        return this.form.controls;
    }

    private syncMaterialValidator(): void {
        const control = this.form.get('material') as FormControl;

        if (this.requiresMaterial()) {
            control.setValidators([Validators.required]);
        } else {
            control.clearValidators();
        }

        control.updateValueAndValidity();
    }

    searchMaterials(event: any): void {
        const query = (event.query || '').toLowerCase();

        this.filteredMaterials = this.allMaterials.filter(m =>
            m.materialName.toLowerCase().includes(query)
        );
    }

    onMaterialSelect(material: MaterialOption): void {
        this.selectedMaterial.set(material);

        this.form.patchValue({
            material,
            nameForImport: material.nameForImport || material.materialName,
            truckStockControl: material.truckStockControl
        });
    }

    materialFrameVisible = false;

    openMaterialScreen() {
        this.materialFrameVisible = true;
        void this.router.navigate(['material-create'], { relativeTo: this.route });
    }

    createMaterial(): void {
        if (this.materialCreateForm.invalid) return;

        const raw = this.materialCreateForm.value;

        const newMaterial: MaterialOption = {
            idMaterial: Date.now(),
            materialName: raw.materialName,
            nameForImport: raw.nameForImport,
            unitBase: raw.unitBase,
            stockQuantity: 0,
            truckStockControl: raw.truckStockControl
        };

        this.allMaterials.unshift(newMaterial);
        this.onMaterialSelect(newMaterial);
        this.materialDialogVisible = false;
    }

    // 🔥 IMPORTAÇÃO
    importFile(event: any): void {
        const file = event.target.files[0];
        if (!file) return;

        const reader = new FileReader();

        reader.onload = (e: any) => {
            const wb = XLSX.read(e.target.result, { type: 'binary' });
            const ws = wb.Sheets[wb.SheetNames[0]];
            const data = XLSX.utils.sheet_to_json(ws);

            this.importedItems = data.map((row: any, index: number) => {
                const description = (row['descricao'] || '').trim();

                const suggestedType = this.suggestType(description);

                return {
                    rowNumber: index + 1,
                    description,
                    nameForImport: description,
                    type: suggestedType,
                    factor: 1,
                    material: null,

                    errors: [
                        !description && 'Descrição obrigatória',
                    ].filter(Boolean),
                };
            });

            this.messageService.add({
                severity: 'info',
                summary: 'Importação realizada',
                detail: `${this.importedItems.length} itens carregados`,
            });
        };

        reader.readAsBinaryString(file);
    }

    suggestType(description: string): string | null {
        const text = description.toUpperCase();

        if (text.includes('BRAÇO')) return 'BRAÇO';
        if (text.includes('REFLETOR')) return 'REFLETOR';
        if (text.includes('LED')) return 'LED';
        if (text.includes('PORCA')) return 'LED';
        if (text.includes('FITA ISOLANTE ADESIVO')) return 'FITA ISOLANTE ADESIVO';
        if (text.includes('POSTE')) return 'POSTE';
        if (text.includes('EXTENSÃO DE REDE')) return 'EXTENSÃO DE REDE';
        if (text.includes('CEMIG')) return 'CEMIG';
        if (text.includes('CINTA')) return 'CINTA';
        if (text.includes('PARAFUSO')) return 'PARAFUSO';
        if (text.includes('FITA ISOLANTE AUTOFUSÃO')) return 'FITA ISOLANTE AUTOFUSÃO';
        if (text.includes('CONECTOR')) return 'CONECTOR';
        if (text.includes('POSTE GALVANIZADO')) return 'POSTE GALVANIZADO';
        if (text.includes('CABO')) return 'CABO';
        if (text.includes('RELÉ')) return 'RELÉ';
        if (text.includes('SERVIÇO')) return 'SERVIÇO';
        if (text.includes('PROJETO')) return 'PROJETO';
        if (text.includes('CIMENTO')) return 'CIMENTO';
        if (text.includes('POSTE CIMENTO')) return 'POSTE CIMENTO';
        if (text.includes('MANUTENÇÃO')) return 'MANUTENÇÃO';

        return null;
    }

    submit(): void {
        if (this.form.invalid) return;

        const raw = this.form.value;

        const payload = {
            description: raw.description,
            itemDependency: raw.itemDependency,
            linking: raw.linking,
            type: raw.type,
            nameForImport: raw.nameForImport,
            factor: raw.factor,
            truckStockControl: raw.truckStockControl,
            materialId: raw.material?.idMaterial ?? null,
        };

        console.log(payload);
    }


    modelDownload(): void {
        const rows = [
            { descricao: 'Preencha apenas esta coluna com a descrição do item' },
            { descricao: 'BRAÇO GALVANIZADO 1,5M' },
            { descricao: 'LUMINÁRIA LED 150W' },
            { descricao: 'PARAFUSO M16' },
            { descricao: 'CINTA INOX PARA POSTE' },
            { descricao: 'CONECTOR PERFURANTE' },
        ];

        const worksheet: XLSX.WorkSheet = XLSX.utils.json_to_sheet(rows);

        worksheet['!cols'] = [
            { wch: 55 },
        ];

        const workbook: XLSX.WorkBook = XLSX.utils.book_new();
        XLSX.utils.book_append_sheet(workbook, worksheet, 'Modelo');

        XLSX.writeFile(workbook, 'modelo_importacao_itens.xlsx');
    }
}
