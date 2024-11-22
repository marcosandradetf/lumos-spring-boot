import { Component } from '@angular/core';
import {MaterialService} from '../../services/material.service';
import {EstoqueService} from '../../services/estoque.service';
import {MaterialResponse} from '../../material-response.dto';
import {BehaviorSubject, catchError, Observable, of, tap, throwError} from 'rxjs';
import {MaterialFormComponent} from '../material/material-form/material-form.component';
import {SidebarComponent} from '../../../../shared/components/sidebar/sidebar.component';
import {TabelaComponent} from '../material/tabela/tabela.component';
import {TableComponent} from '../../../../shared/components/table/table.component';
import {PaginationComponent} from '../../../../shared/components/pagination/pagination.component';
import {ButtonComponent} from '../../../../shared/components/button/button.component';
import {ModalComponent} from '../../../../shared/components/modal/modal.component';
import {DynamicTableComponent} from '../../../../shared/components/dynamic-table/dynamic-table.component';
import {NgForOf, NgIf} from '@angular/common';
import {FormsModule, NgForm} from '@angular/forms';
import {ItemRequest} from '../../../contract/itens-request.dto';
import {Contract} from '../../../contract/contract-response.dto';
import {AlertMessageComponent} from '../../../../shared/components/alert-message/alert-message.component';
import {StockMovementDTO} from '../../stock-movement.dto';
import {SupplierDTO} from '../../supplier.dto';
import {UtilsService} from '../../../../core/service/utils.service';

@Component({
  selector: 'app-stock-movement',
  standalone: true,
  imports: [
    SidebarComponent,
    TableComponent,
    PaginationComponent,
    ButtonComponent,
    ModalComponent,
    NgForOf,
    FormsModule,
    AlertMessageComponent,
    NgIf
  ],
  templateUrl: './stock-movement.component.html',
  styleUrl: './stock-movement.component.scss'
})
export class StockMovementComponent {
  sidebarLinks = [
    { title: 'Gerenciar', path: '/estoque/materiais', id: 'opt1' },
    { title: 'Movimentar Estoque', path: '/estoque/movimento', id: 'opt2' },
    { title: 'Entrada de Nota Fiscal', path: '/estoque/entrada', id: 'opt3' },
    { title: 'Importar Material (.xlsx)', path: '/estoque/importar', id: 'opt4' },
    { title: 'Sugestão de Compra', path: '/estoque/sugestao', id: 'opt5' }
  ];
  materials: MaterialResponse[] = [];
  suppliers: any = [];

  sendSuppliers: any[] = [];
  sendMovement: StockMovementDTO[] = [];

  private currentPage: string = "0";
  openMovementModal: boolean = false;
  openConfirmationModal: boolean = false;
  openSupplierModal: boolean = false;
  serverMessage: string | null = null;
  alertType: string | null = null;
  formSubmitted: boolean = false;

  private validate(): boolean {
    for (let i = 0; i < this.sendMovement.length; i++) {
      if(this.sendMovement[i].inputQuantity === 0) {
        this.serverMessage = "A quantidade não pode ser igual a 0."
        this.alertType = "alert-error";
        return false;
      } else if (this.sendMovement[i].quantityPackage === 0) {
        this.serverMessage = "A quantidade por embalagem não pode ser igual a 0."
        this.alertType = "alert-error";
        return false;
      }
    }
    return true;
  };

  constructor(protected materialService: MaterialService,
              private estoqueService: EstoqueService,
              private utils: UtilsService,) {
    this.loadMaterials();
  }


  private loadMaterials() {
    // Chama o serviço para buscar os materiais apenas se o array estiver vazio
    if (this.materials.length === 0) {
      this.materialService.getFetch(this.currentPage, "25");
    }

    this.materialService.materials$.subscribe((materials: MaterialResponse[]) => {
      this.materials = materials;
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
      // this.getSomeState(true).subscribe(state => {
      //   this.modalOpenMovement = state;
      // });
      this.openMovementModal = true;
    }

  }


  getSomeState(value: boolean): Observable<boolean> {
    return of(value);
  }

  handleConfirmMovement() {
    if (this.validate()) {
      this.openConfirmationModal = true;
    }
  }

  toggleSelection(item: MaterialResponse) {
    if (item.selected) {
      const newMovement: StockMovementDTO = {
        buyUnit: item.buyUnit,
        description: '',
        inputQuantity: 0,
        materialId: item.idMaterial,
        pricePerItem: '',
        quantityPackage: 0,
        supplierId: item.idMaterial
      };
      this.sendMovement.push(newMovement);
      console.log(this.sendMovement);


    } else {
      // const index = this.sendMovement;
      // if (index !== -1) {
      //   this.movement.idMaterial.splice(index, 1);
      //
      // }
    }
  }

  submitDataMovement(): void {
    this.estoqueService.stockMovement(this.sendMovement).pipe(
      tap(response => {
        console.log(response);
        this.serverMessage = response.toString();
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

  }

  closeSupplierModal() {
    this.openSupplierModal = false;
  }



  submitDataSupplier(form: any) {
    this.formSubmitted = true;
    console.log(this.sendSuppliers);

    if (form.invalid) {
      console.log('Formulário inválido');
      return;
    }

    this.estoqueService.createSuppliers(this.sendSuppliers).pipe(
      tap(response => {
        console.log(response);
        this.serverMessage = "Fornecedor criado com Sucesso.";
        this.alertType = 'alert-success';
        this.suppliers = response;
        this.formSubmitted = false;
      }),
      catchError(err => {
        this.serverMessage = err.message;
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
    const material = this.materials.find(m => m.idMaterial === id);
    return material ? material.materialName : '';
  }

  submitFormMovement(form: NgForm) {
    this.formSubmitted = true;

    if (form.invalid) {
      return;
    }

    this.handleConfirmMovement();
  }

  formatValue(event: Event, index: number) {
    // Obtém o valor diretamente do evento e remove todos os caracteres não numéricos
    let targetValue = (event.target as HTMLInputElement).value.replace(/\D/g, '');

    // Verifica se targetValue está vazio e define um valor padrão
    if (!targetValue) {
      this.sendMovement[index].pricePerItem = ''; // ou "0,00" se preferir
      (event.target as HTMLInputElement).value = ''; // Atualiza o valor no campo de input
      return;
    }

    // Atualiza o valor no modelo e no campo de input
    console.log(this.getDescription(this.sendMovement[index].materialId));
    const value = this.utils.formatValue(targetValue);
    this.sendMovement[index].pricePerItem = value;
    (event.target as HTMLInputElement).value = value; // Exibe o valor formatado no campo de input

  }

}
