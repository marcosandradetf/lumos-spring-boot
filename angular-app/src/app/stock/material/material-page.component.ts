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
import {MaterialFormDTO} from '../../models/material-response.dto';
import {Router} from '@angular/router';
import {ConfirmationService, MessageService} from 'primeng/api';
import {StockService} from '../services/stock.service';


@Component({
  selector: 'app-material-page',
  standalone: true,
  templateUrl: './material-page.component.html',
  styleUrls: ['./material-page.component.scss'],
  providers: [ConfirmationService, MessageService],
  imports: [CommonModule, ReactiveFormsModule, ButtonDirective, LoadingOverlayComponent, Toast, TableModule, InputText, DropdownModule, PrimeBreadcrumbComponent, FormsModule] // Adicionando os módulos aqui
})
export class MaterialPageComponent implements OnInit {
  materials: MaterialFormDTO[] = [];
  filteredMaterials: MaterialFormDTO[] = [];
  materialTypes: any[] = [];
  loading: boolean = false;

  filters = {
    materialName: '',
    barcode: '',
    materialType: null
  };

  constructor(
    private router: Router,
    private confirmationService: ConfirmationService,
    private messageService: MessageService,
    private stockService: StockService,
  ) {
  }

  ngOnInit(): void {
    this.loadMaterials();
    this.loadMaterialTypes();
  }

  // Simula carregamento de materiais
  loadMaterials() {
    this.loading = true;
    setTimeout(() => {
      // Exemplo de dados
      this.materials = [
        {
          materialId: 1,
          materialBaseName: 'Parafuso',
          materialName: 'Parafuso M16 Rosca',
          materialType: 1,
          materialSubtype: 1,
          materialFunction: null,
          materialModel: null,
          materialBrand: null,
          materialAmps: null,
          materialLength: 50,
          materialWidth: null,
          materialPower: null,
          materialGauge: null,
          materialWeight: 0.1,
          barcode: '1234567890123',
          inactive: false,
          buyUnit: 'PC',
          requestUnit: 'PC',
          truckStockControl: false,
          contractItems: []
        },
        {
          materialId: 2,
          materialBaseName: 'Cabo',
          materialName: 'Cabo 240MM',
          materialType: 2,
          materialSubtype: 2,
          materialFunction: null,
          materialModel: null,
          materialBrand: null,
          materialAmps: 16,
          materialLength: 240,
          materialWidth: 10,
          materialPower: null,
          materialGauge: 2.5,
          materialWeight: 0.2,
          barcode: '2345678901234',
          inactive: false,
          buyUnit: 'MT',
          requestUnit: 'MT',
          truckStockControl: false,
          contractItems: []
        }
      ];
      this.filteredMaterials = [...this.materials];
      this.loading = false;
    }, 500);
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
      const matchesCode = this.filters.barcode ? m.barcode.includes(this.filters.barcode) : true;
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
    void this.router.navigate(['/estoque/cadastrar-material'], {queryParams: {barcode: material.barcode}});
  }

  // Deleta material com confirmação
  deleteMaterial(material: MaterialFormDTO) {
    this.confirmationService.confirm({
      message: `Deseja realmente excluir o material ${material.materialName}?`,
      accept: () => {
        this.materials = this.materials.filter(m => m.barcode !== material.barcode);
        this.applyFilters(); // Atualiza a lista filtrada
        this.messageService.add({
          severity: 'success',
          summary: 'Deletado',
          detail: `${material.materialName} removido com sucesso`
        });
      }
    });
  }

  // Botão de cadastro
  navigateToCadastro() {
    void this.router.navigate(['/estoque/cadastrar-material']);
  }

  protected getType(materialType: number): string | null {
    return this.materialTypes.find(t => t.typeId === materialType)?.typeName;
  }
}
