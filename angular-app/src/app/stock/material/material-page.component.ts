import {Component, OnInit} from '@angular/core';
import {CommonModule} from '@angular/common';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {ButtonDirective} from 'primeng/button';
import {LoadingOverlayComponent} from '../../shared/components/loading-overlay/loading-overlay.component';
import {Toast} from 'primeng/toast';
import {TableModule} from 'primeng/table';
import {InputText} from 'primeng/inputtext';
import {DropdownModule} from 'primeng/dropdown';
import {PrimeBreadcrumbComponent} from '../../shared/components/prime-breadcrumb/prime-breadcrumb.component';
import {MaterialFormDTO} from '../dto/material-response.dto';
import {Router} from '@angular/router';
import {ConfirmationService, MessageService} from 'primeng/api';
import {StockService} from '../services/stock.service';
import {MaterialService} from '../services/material.service';
import {UtilsService} from '../../core/service/utils.service';
import {StyleClass} from 'primeng/styleclass';
import {SharedState} from '../../core/service/shared-state';


@Component({
    selector: 'app-material-page',
    standalone: true,
    templateUrl: './material-page.component.html',
    styleUrls: ['./material-page.component.scss'],
    providers: [],
    imports: [CommonModule, ReactiveFormsModule, ButtonDirective, LoadingOverlayComponent, Toast, TableModule, InputText, DropdownModule, PrimeBreadcrumbComponent, FormsModule, StyleClass] // Adicionando os módulos aqui
})
export class MaterialPageComponent implements OnInit {
    materials: any[] = [];
    filteredMaterials: any[] = [];
    materialTypes: any[] = [];
    loading: boolean = false;

    filters = {
        materialName: '',
        barcode: '',
        materialType: null
    };

    constructor(
        private router: Router,
        private materialService: MaterialService,
        private stockService: StockService,
        private utils: UtilsService,
    ) {
    }

    ngOnInit(): void {
        this.loading = true;
        this.loadMaterialTypes();
        this.loadMaterials();
        SharedState.setCurrentPath(['Estoque', 'Catálogo']);
    }

    // Simula carregamento de materiais
    loadMaterials() {
        this.loading = true;
        this.materialService.getCatalogue().subscribe({
            next: (data) => {
                this.materials = data;
                this.filteredMaterials = data;
            },
            error: (error) => {
                this.utils.showMessage(error.error.message ?? error.error, 'error', 'Não foi possível carregar Materiais')
                this.loading = false;
            },
            complete: () => {
                this.loading = false;
            }
        })
    }

    // Simula carregamento de tipos
    loadMaterialTypes() {
        this.stockService.findAllTypeSubtype().subscribe(types => {
            this.materialTypes = types;
        });
    }

    // Aplica os filtros da tela
    applyFilters() {
        this.filteredMaterials = this.materials.filter(m => {
            const matchesName = this.filters.materialName ? m.materialName.toLowerCase().includes(this.filters.materialName.toLowerCase()) : true;
            const matchesCode = this.filters.barcode ? m.barcode?.includes(this.filters.barcode) : true;
            const matchesType = this.filters.materialType ? m.materialType === this.filters.materialType : true;
            return matchesName && matchesCode && matchesType;
        });
    }

    // Limpa filtros e mostra todos os materiais
    clearFilters() {
        this.filters = {materialName: '', barcode: '', materialType: null};
        this.filteredMaterials = [...this.materials];
    }

    // Redireciona para tela de cadastro/edição
    editMaterial(material: MaterialFormDTO) {
        void this.router.navigate(['/estoque/cadastrar-material'], {queryParams: {materialId: material.materialId}});
    }

    // Deleta material com confirmação
    deleteMaterial(material: any) {
        this.utils.showMessage('Ação não implementada.', 'info', 'Lumos');
    }

    // Botão de cadastro
    navigateToCadastro() {
        void this.router.navigate(['/estoque/cadastrar-material']);
    }

    protected getType(materialType: number): string | null {
        return this.materialTypes.find(t => t.typeId === materialType)?.typeName;
    }
}
