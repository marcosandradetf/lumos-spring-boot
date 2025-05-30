import {Component, OnInit} from '@angular/core';
import {catchError, tap, throwError} from 'rxjs';
import {NgClass, NgForOf, NgIf} from '@angular/common';
import {FormsModule, NgForm} from '@angular/forms';
import {Title} from '@angular/platform-browser';
import {TableComponent} from '../../shared/components/table/table.component';
import {PaginationComponent} from '../../shared/components/pagination/pagination.component';
import {ButtonComponent} from '../../shared/components/button/button.component';
import {ModalComponent} from '../../shared/components/modal/modal.component';
import {AlertMessageComponent} from '../../shared/components/alert-message/alert-message.component';
import {Deposit} from '../../models/almoxarifado.model';
import {MaterialResponse} from '../../models/material-response.dto';
import {StockMovementDTO} from '../../models/stock-movement.dto';
import {MaterialService} from '../services/material.service';
import {EstoqueService} from '../services/estoque.service';
import {UtilsService} from '../../core/service/utils.service';
import {SupplierDTO} from '../../models/supplier.dto';
import {Steps} from 'primeng/steps';
import {MenuItem} from 'primeng/api';
import {Select} from 'primeng/select';
import {Paginator, PaginatorState} from 'primeng/paginator';


@Component({
  selector: 'app-stock-movement',
  standalone: true,
  imports: [
    TableComponent,
    PaginationComponent,
    ButtonComponent,
    ModalComponent,
    NgForOf,
    FormsModule,
    AlertMessageComponent,
    NgIf,
    NgClass,
    Steps,
    Select,
    Paginator,
  ],
  templateUrl: './stock-movement.component.html',
  styleUrl: './stock-movement.component.scss'
})
export class StockMovementComponent implements OnInit {
  items: MenuItem[] | undefined;

  sidebarLinks = [
    {title: 'Gerenciar', path: '/estoque/materiais', id: 'opt1'},
    {title: 'Movimentar Estoque', path: '/estoque/movimento', id: 'opt2'},
    {title: 'Entrada de Nota Fiscal', path: '/estoque/entrada', id: 'opt3'},
    {title: 'Importar Material (.xlsx)', path: '/estoque/importar', id: 'opt4'},
    {title: 'Sugestão de Compra', path: '/estoque/sugestao', id: 'opt5'}
  ];

  units: any[] = [
    {Value: "CX"},   // Caixa
    {Value: "Rolo"}, // Rolo de cabo ou fita
    {Value: "PÇ"},   // Peça
    {Value: "UN"},   // Unidade
    {Value: "M"},    // Metro
    {Value: "CM"},   // Centímetro
    {Value: "KG"},   // Quilograma
    {Value: "T"},    // Tonelada
    {Value: "L"},    // Litro
    {Value: "ML"},   // Mililitro
    {Value: "KIT"},  // Conjunto de materiais
    {Value: "PAR"},  // Par (ex: lâmpadas)
  ];


  deposits: Deposit[] = [];

  materials: MaterialResponse[] = [];
  suppliers: any = [];
  selectedDeposits: string[] = [];

  sendSuppliers: any[] = [];
  sendMovement: StockMovementDTO[] = [];
  selectedMaterials: any[] = [];

  private currentPage: string = "0";
  openMovementModal: boolean = false;
  openConfirmationModal: boolean = false;
  openSupplierModal: boolean = false;
  serverMessage: string | null = null;
  alertType: string | null = null;
  formSubmitted: boolean = false;

  private validate(): boolean {
    for (let item of this.sendMovement) {
      if (item.inputQuantity.length === 0) {
        this.serverMessage = "A quantidade não pode ser igual a 0 ou estar inválida.";
        this.alertType = "alert-error";
        return false; // Aqui retorna e sai da função
      } else if (item.quantityPackage.length === 0) {
        this.serverMessage = "A quantidade por embalagem não pode ser igual a 0 ou estar inválida.";
        this.alertType = "alert-error";
        return false; // Aqui também retorna e sai da função
      }
    }
    return true; // Se não encontrou erro, retorna true
  }


  constructor(protected materialService: MaterialService,
              private estoqueService: EstoqueService,
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
        routerLink: '/estoque/movimento/aprovado'
      },
    ];
  }


  private loadMaterials() {
    if (!this.currentDeposit) return;

    // Chama o serviço para buscar os materiais apenas se o array estiver vazio
    if (this.materials.length === 0) {
      console.log(this.currentDeposit);
      this.materialService.getFetch(0, this.currentDeposit.idDeposit);
    }

    this.materialService.materials$.subscribe((materials: MaterialResponse[]) => {
      this.materials = materials;

      this.materials = this.materials.filter(movement => !movement.inactive);

      // Itera sobre os materiais recebidos
      this.materials.forEach((material) => {
        // Verifica se o ID do material está presente em sendMovement
        const matchedMovement = this.sendMovement.find(movement => movement.materialId === material.idMaterial);
        // Se houver correspondência, marque o material como selecionado
        material.selected = !!matchedMovement;
      });


    });

    this.estoqueService.getSuppliers().subscribe((suppliers: SupplierDTO[]) => {
      this.suppliers = suppliers;
    });
  }

  handleClick = () => {
    if (this.sendMovement.length === 0) {
      this.alertType = 'alert-warning';
      this.serverMessage = "Antes de continuar você deve selecionar os itens desejados.";
    } else {
      this.openMovementModal = true;
    }

  }
  currentDeposit: Deposit | undefined;

  handleConfirmMovement() {
    if (this.validate()) {
      this.openConfirmationModal = true;
    }
  }

  toggleSelection(item: MaterialResponse) {
    if (item.selected) {
      this.selectedMaterials.push({
        idMaterial: item.idMaterial,
        materialName: item.materialName,
        materialPower: item.materialPower,
        materialAmps: item.materialAmps,
        materialLength: item.materialLength
      });
      const newMovement: StockMovementDTO = {
        buyUnit: '',
        requestUnit: '',
        description: '',
        inputQuantity: '',
        materialId: item.idMaterial,
        priceTotal: '',
        quantityPackage: '',
        supplierId: "",
        totalQuantity: 0
      };
      this.sendMovement.push(newMovement);
    } else {
      // Remove o item filtrando pelo `materialId`
      this.selectedMaterials = this.selectedMaterials.filter(m => m.materialId !== item.idMaterial);
      this.sendMovement = this.sendMovement.filter(movement => movement.materialId !== item.idMaterial);
    }
  }

  submitDataMovement(): void {
    this.estoqueService.stockMovement(this.sendMovement).pipe(
      tap(response => {
        this.serverMessage = response;
        this.alertType = 'alert-success';
        this.formSubmitted = false;
        this.closeConfirmationModal();
        this.closeMovementModal();
        this.clearSelection();
      }),
      catchError(err => {
        this.serverMessage = err.message;
        this.alertType = 'alert-error';
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
    this.materials.forEach((material: MaterialResponse) => {
      if (material.selected) {
        material.selected = false;
      }
    });

    this.sendMovement = [];
  }

  closeSupplierModal() {
    this.openSupplierModal = false;
  }


  submitDataSupplier(form: any) {
    this.formSubmitted = true;

    if (form.invalid) {
      console.log('Formulário inválido');
      return;
    }

    this.estoqueService.createSuppliers(this.sendSuppliers).pipe(
      tap(response => {
        this.serverMessage = "Fornecedor Criado com Sucesso!";
        this.alertType = 'alert-success';
        this.suppliers = response;
        this.formSubmitted = false;
        this.sendSuppliers = [];
      }),
      catchError(err => {
        this.serverMessage = err.error;
        this.alertType = 'alert-error';
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


  getDescription(id: number): string {
    let material = this.selectedMaterials.find(m => m.idMaterial === id);

    if (!material) {
      return ''; // Retorna string vazia se o material não for encontrado
    }

    console.log(material);

    if (material.materialPower !== undefined) {
      return `${material.materialName} - ${material.materialPower}`;
    }

    if (material.materialAmps !== undefined) {
      return `${material.materialName} - ${material.materialAmps}`;
    }

    if (material.materialLength !== undefined) {
      return `${material.materialName} - ${material.materialLength}`;
    }

    return material.materialName; // Garante que a função sempre retorna uma string
  }


  submitFormMovement(form: NgForm) {
    this.formSubmitted = true;

    if (form.invalid) {
      console.log("invalid form");
      return;
    }

    console.log(this.sendMovement);
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
          break;
        default:
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
    console.log(selectedUnit);
    switch (selectedUnit.toUpperCase()) {
      case 'CX':
        return ["CX", "UN", "PÇ"]
      case 'ROLO':
        return ["Rolo", "M", "CM"]
      default:
        return [selectedUnit];
    }
  }

  startNewMovement() {
    this.loadMaterials()
  }


  search: boolean = false;
  value: string = '';

  onPageChange(event: PaginatorState) {

    if (!this.search)
      this.materialService.getFetch(event.page!!, this.currentDeposit?.idDeposit);
    else if (this.search)
      this.materialService.getBySearch(event.page!!, this.value, this.currentDeposit?.idDeposit);

  }

  handleSearch(value: string): void {
    if (value.length > 2) {
      this.value = value;
      this.search = true;
      this.materialService.getBySearch(this.materialService.currentPage, value, this.currentDeposit?.idDeposit);
    }

    if (value.length === 0) {
      this.value = '';
      this.search = false;
      this.materialService.getFetch(this.materialService.currentPage, this.currentDeposit?.idDeposit);
    }

  }


}
