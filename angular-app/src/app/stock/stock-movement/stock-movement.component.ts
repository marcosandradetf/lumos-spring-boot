import {Component, OnInit} from '@angular/core';
import {catchError, tap, throwError} from 'rxjs';
import {NgClass, NgForOf, NgIf} from '@angular/common';
import {FormsModule, NgForm} from '@angular/forms';
import {Title} from '@angular/platform-browser';
import {TableComponent} from '../../shared/components/table/table.component';
import {ButtonComponent} from '../../shared/components/button/button.component';
import {ModalComponent} from '../../shared/components/modal/modal.component';
import {Deposit} from '../../models/almoxarifado.model';
import {MaterialStockResponse} from '../../models/material-response.dto';
import {StockMovementDTO} from '../../models/stock-movement.dto';
import {MaterialService} from '../services/material.service';
import {StockService} from '../services/stock.service';
import {UtilsService} from '../../core/service/utils.service';
import {SupplierDTO} from '../../models/supplier.dto';
import {Steps} from 'primeng/steps';
import {MenuItem} from 'primeng/api';
import {Paginator} from 'primeng/paginator';
import {TableModule} from 'primeng/table';
import {Skeleton} from 'primeng/skeleton';
import {Toast} from 'primeng/toast';
import {DropdownModule} from 'primeng/dropdown';
import {PrimeBreadcrumbComponent} from '../../shared/components/prime-breadcrumb/prime-breadcrumb.component';


@Component({
  selector: 'app-stock-movement',
  standalone: true,
  imports: [
    TableComponent,
    ButtonComponent,
    ModalComponent,
    NgForOf,
    FormsModule,
    NgIf,
    NgClass,
    Steps,
    TableModule,
    Skeleton,
    Toast,
    DropdownModule,
    PrimeBreadcrumbComponent,
  ],
  templateUrl: './stock-movement.component.html',
  styleUrl: './stock-movement.component.scss'
})
export class StockMovementComponent implements OnInit {
  totalRecords: number = 0;
  currentPage: number = 0;
  lastPage: number = 0;
  rows: number = 15;
  loading: boolean = true;

  items: MenuItem[] | undefined;

  deposits: Deposit[] = [
    {
      idDeposit: 1,
      depositName: "Depósito Central",
      depositAddress: "Rua das Flores, 123",
      depositDistrict: "Centro",
      depositCity: "São Paulo",
      depositState: "SP",
      depositRegion: "Sudeste",
      depositPhone: "(11) 99999-1111",
      isTruck: true,
      teamName: "Equipe Alfa",
      plateVehicle: "ABC-1A23"
    },
    {
      idDeposit: 2,
      depositName: "Depósito Zona Norte",
      depositAddress: "Avenida Brasil, 456",
      depositDistrict: "Santana",
      depositCity: "São Paulo",
      depositState: "SP",
      depositRegion: "Sudeste",
      depositPhone: "(11) 98888-2222",
      isTruck: false,
      teamName: "Equipe Beta",
      plateVehicle: "DEF-4B56"
    },
    {
      idDeposit: 3,
      depositName: "Depósito Regional Sul",
      depositAddress: "Rua das Araucárias, 789",
      depositDistrict: "Centro",
      depositCity: "Curitiba",
      depositState: "PR",
      depositRegion: "Sul",
      depositPhone: "(41) 97777-3333",
      isTruck: true,
      teamName: "Equipe Gama",
      plateVehicle: "GHI-7C89"
    }
  ];

  materials: MaterialStockResponse[] = [
    {
      materialStockId: 201,
      materialName: "Lâmpada LED Bulbo 9W Branca Fria 6500K Marca Philips",
      barcode: "7891234000001",
      buyUnit: "UN",
      requestUnit: "UN",
      stockQt: 180,
      depositName: "Depósito Central"
    },
    {
      materialStockId: 202,
      materialName: "Disjuntor Termomagnético Monopolar 20A Curva C Marca Siemens",
      barcode: "7891234000002",
      buyUnit: "UN",
      requestUnit: "UN",
      stockQt: 95,
      depositName: "Depósito Central"
    },
    {
      materialStockId: 203,
      materialName: "Cabo Elétrico Flexível 2,5mm² 750V Antichama Marca Prysmian",
      barcode: "7891234000003",
      buyUnit: "ROLO",
      requestUnit: "ROLO",
      stockQt: 1200,
      depositName: "Depósito Zona Norte"
    },
    {
      materialStockId: 204,
      materialName: "Tomada Elétrica 2P+T 10A Padrão Brasileiro Marca Tramontina",
      barcode: "7891234000004",
      buyUnit: "UN",
      requestUnit: "UN",
      stockQt: 340,
      depositName: "Depósito Zona Norte"
    },
    {
      materialStockId: 205,
      materialName: "Interruptor Simples 10A Branco Linha Lux Marca Schneider",
      barcode: "7891234000005",
      buyUnit: "UN",
      requestUnit: "UN",
      stockQt: 260,
      depositName: "Depósito Regional Sul"
    },
    {
      materialStockId: 206,
      materialName: "Contator Tripolar 25A Bobina 220V Marca WEG",
      barcode: "7891234000006",
      buyUnit: "UN",
      requestUnit: "UN",
      stockQt: 42,
      depositName: "Depósito Regional Sul"
    }
  ];

  suppliers: any = [];
  sendSuppliers: any[] = [];
  sendMovement: StockMovementDTO[] = [];
  openMovementModal: boolean = false;
  openConfirmationModal: boolean = false;
  openSupplierModal: boolean = false;
  formSubmitted: boolean = false;

  private validate(): boolean {
    for (let item of this.sendMovement) {
      if (item.inputQuantity.length === 0) {
        this.utils.showMessage("A quantidade não pode ser igual a 0 ou estar inválida.", 'warn', 'Atenção');
        return false; // Aqui retorna e sai da função
      } else if (item.quantityPackage.length === 0) {
        this.utils.showMessage("A quantidade por embalagem não pode ser igual a 0 ou estar inválida.", 'warn', 'Atenção');
        return false; // Aqui também retorna e sai da função
      }
    }
    return true; // Se não encontrou erro, retorna true
  }


  constructor(protected materialService: MaterialService,
              private estoqueService: StockService,
              protected utils: UtilsService,
              private titleService: Title) {
    this.loadMaterials();
    this.titleService.setTitle("Movimentar Estoque");
    this.estoqueService.getDeposits().subscribe(d => this.deposits = d);
  }

  ngOnInit() {
    this.items = [
      {
        label: 'Selecionar itens',
        routerLink: '/estoque/movimento'
      },
      {
        label: 'Pendente de Aprovação',
        routerLink: '/estoque/movimento-pendente'
      },
      {
        label: 'Aprovado',
        routerLink: '/estoque/movimento-aprovado'
      },
    ];
  }

  loadMaterials() {
    if (!this.currentDeposit) return;

    this.loading = true;

    this.materialService.getMaterials(this.currentPage, this.rows).subscribe(response => {
      this.materials = response.data;  // Dados dos materiais
      this.totalRecords = response.totalRecords;  // Total de registros no banco
    });
  }

  currentDeposit: Deposit | undefined;

  handleConfirmMovement() {
    if (this.validate()) {
      this.openConfirmationModal = true;
    }
  }

  toggleSelection(item: MaterialStockResponse) {
    const index = this.sendMovement.findIndex(s => s.materialStockId === item.materialStockId);

    if (index === -1) {
      const newMovement: StockMovementDTO = {
        materialStockId: item.materialStockId,
        materialName: item.materialName,
        description: '',
        buyUnit: item.buyUnit,
        requestUnit: item.requestUnit,
        inputQuantity: '',
        priceTotal: '',
        quantityPackage: '',
        supplierId: '',
        totalQuantity: 0
      };
      this.sendMovement.push(newMovement);
    } else {
      this.sendMovement = this.sendMovement.filter(movement => movement.materialStockId !== item.materialStockId);
    }
  }

  submitDataMovement(): void {
    this.estoqueService.stockMovement(this.sendMovement).pipe(
      tap(response => {
        this.utils.showMessage(response, 'success', 'Movimentação salva');
        this.formSubmitted = false;
        this.closeConfirmationModal();
        this.closeMovementModal();
        this.clearSelection();
      }),
      catchError(err => {
        this.utils.showMessage(err.error.message, 'error', 'Erro ao salvar movimentação');
        this.formSubmitted = false;
        this.closeConfirmationModal();
        return throwError(() => err);
      })
    ).subscribe();
  }


  closeMovementModal() {
    this.openMovementModal = false;
  }

  handleOpenSupplierModal() {
    this.openSupplierModal = true;
  }


  closeConfirmationModal() {
    this.openConfirmationModal = false;
  }

  clearSelection(): void {
    this.sendMovement = [];
  }

  closeSupplierModal() {
    this.openSupplierModal = false;
  }


  submitDataSupplier(form: any) {
    this.formSubmitted = true;

    if (form.invalid) {
      return;
    }

    this.estoqueService.createSuppliers(this.sendSuppliers).pipe(
      tap(response => {
        this.utils.showMessage('Fornecedor Criado com Sucesso!', 'success', 'Sucesso');
        this.suppliers = response;
        this.formSubmitted = false;
        this.sendSuppliers = [];
      }),
      catchError(err => {
        this.utils.showMessage(err.error.message, 'error', 'Erro ao salvar fornecedor');
        this.formSubmitted = false;
        return throwError(() => err);
      })
    ).subscribe();

  }

  addSupplier() {
    const newSupplier = {
      name: '',
      cnpj: '',
      contact: '',
      address: '',
      phone: '',
      email: ''
    };
    this.sendSuppliers.push(newSupplier);
  }

  removeRow(index: number) {
    this.sendSuppliers.splice(index, 1); // Remove o fornecedor pelo índice
  }


  submitFormMovement(form: NgForm) {
    this.formSubmitted = true;

    if (form.invalid) {
      console.log("invalid form");
      return;
    }

    this.handleConfirmMovement();
  }

  formatValue(event: Event, index: number) {
    // Obtém o valor diretamente do evento e remove todos os caracteres não numéricos
    let targetValue = (event.target as HTMLInputElement).value.replace(/\D/g, '');

    // Verifica se targetValue está vazio e define um valor padrão
    if (!targetValue) {
      this.sendMovement[index].priceTotal = ''; // ou "0,00" se preferir
      (event.target as HTMLInputElement).value = ''; // Atualiza o valor no campo de input
      return;
    }

    const value = this.utils.formatValue(targetValue);
    this.sendMovement[index].priceTotal = value;
    (event.target as HTMLInputElement).value = value; // Exibe o valor formatado no campo de input

  }


  updateQuantityPackage() {
    this.sendMovement.forEach(movement => {
      switch (movement.buyUnit.toUpperCase()) {
        case 'UN':
        case 'PÇ':
        case 'PAR':
        case 'KIT':
        case 'M':
        case 'CM':
        case 'KG':
        case 'T':
        case 'L':
        case 'ML':
          movement.quantityPackage = '1';
          this.calculateQuantity(movement);
          movement.totalQuantity = Number(movement.inputQuantity);
          movement.requestUnit = this.filterUnits(movement.buyUnit.toUpperCase())[0];
          console.log(movement);
          break;
        default:
          movement.requestUnit = this.filterUnits(movement.buyUnit.toUpperCase())[0];
          this.calculateQuantity(movement);
          console.log(movement);
          break;
      }
    });
  }

  getOption(movement: StockMovementDTO) {
    switch (movement.buyUnit.toUpperCase()) {
      case 'UN':
      case 'PÇ':
      case 'PAR':
      case 'KIT':
      case 'M':
      case 'CM':
      case 'KG':
      case 'T':
      case 'L':
      case 'ML':
        return false;
      default:
        return true;
    }
  }

  shouldShowTooltip(buyUnit
                    :
                    string
  ):
    boolean | string {
    const unitsWithTooltip = ["CX", "ROLO"];
    return buyUnit && unitsWithTooltip.includes(buyUnit.toUpperCase());
  }

  getTooltipText(buyUnit
                 :
                 string
  ):
    string {
    switch (buyUnit) {
      case 'CX':
        return 'Informe a quantidade de itens por Caixa. Exemplo: 5 itens por caixa.';
      case 'Rolo':
        return 'Informe o tamanho por Rolo em Metro. Exemplo: 5 metros por rolo.';
      default:
        return '';
    }
  }

  calculateQuantity(movement: StockMovementDTO) {
    switch (movement.requestUnit.toUpperCase()) {
      case 'UN':
      case 'PÇ':
        movement.totalQuantity = Number(movement.inputQuantity) * Number(movement.quantityPackage);
        break;
      case 'CM':
      case 'KG':
        if (movement.buyUnit === 'CM' || movement.buyUnit === 'KG') {
          movement.totalQuantity = Number(movement.inputQuantity) * 100;
        } else if (movement.buyUnit === 'Rolo') {
          movement.totalQuantity = Number(movement.inputQuantity) * (Number(movement.quantityPackage) * 100);
        } else {
          movement.totalQuantity = Number(movement.inputQuantity);
        }
        break;
      case 'M':
      case 'T':
        if (movement.buyUnit === 'CM' || movement.buyUnit === 'KG') {
          movement.totalQuantity = Number(movement.inputQuantity) / 100;
        } else if (movement.buyUnit === 'Rolo') {
          movement.totalQuantity = Number(movement.inputQuantity) * Number(movement.quantityPackage);
        } else {
          movement.totalQuantity = Number(movement.inputQuantity);
        }
        break;
      default:
        movement.totalQuantity = Number(movement.inputQuantity);
        break;
    }
  }

  filterUnits(selectedUnit: string): string[] {
    switch (selectedUnit.toUpperCase()) {
      case 'CX':
        return ["CX", "UN", "PÇ"]
      case 'ROLO':
        return ["Rolo", "M", "CM"]
      default:
        return [selectedUnit];
    }
  }


  onPageChange(event: any) {
    this.currentPage = event.page;
    this.rows = event.rows;
    this.loadMaterials();  // Recarrega os materiais com a nova página
  }

  handleSearch(value: string): void {
    this.loading = true;
    if (value.length > 2) {
      this.materialService.getBySearch(this.materialService.currentPage, this.currentDeposit?.idDeposit!!, value);
    } else if (value.length === 0) {
      this.lastPage = this.currentPage;
      this.materialService.getMaterials(this.lastPage, this.rows).subscribe(response => {
        this.materials = response.data;  // Dados dos materiais
        this.totalRecords = response.totalRecords;  // Total de registros no banco
      });
    }

  }

  skeleton: any[] = Array.from({length: 7}).map((_, i) => `Item #${i}`);

  protected isChecked(materialStockId: number) {
    return this.sendMovement.findIndex(s => s.materialStockId === materialStockId) !== -1;
  }
}
