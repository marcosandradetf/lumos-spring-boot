import {Component} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {NgForOf, NgIf} from '@angular/common';
import {ContractService} from '../../services/contract.service';
import {UtilsService} from '../../../core/service/utils.service';
import {ScreenMessageComponent} from '../../../shared/components/screen-message/screen-message.component';
import {ModalComponent} from '../../../shared/components/modal/modal.component';
import {TableComponent} from '../../../shared/components/table/table.component';


@Component({
  selector: 'app-create',
  standalone: true,
  imports: [
    FormsModule,
    NgIf,
    NgForOf,
    ScreenMessageComponent,
    ModalComponent,
    TableComponent
  ],
  templateUrl: './create.component.html',
  styleUrl: './create.component.scss'
})
export class CreateComponent {

  contract: {
    number: string,
    socialReason: string,
    address: string,
    phone: string,
    cnpj: string,
    items: {
      contractReferenceItemId: number;
      description: string;
      completeDescription: string;
      type: string;
      linking: string;
      quantity: number;
      price: string;
    }[]
  } = {
    number: '',
    socialReason: '',
    address: '',
    phone: '',
    cnpj: '',
    items: [],
  }

  items: {
    contractReferenceItemId: number;
    description: string;
    completeDescription: string;
    type: string;
    linking: string;
    quantity: number;
    price: string;
  }[] = [];

  totalValue: string = "0,00";
  totalItems: number = 0;
  removingIndex: number | null = null;
  changeValue: boolean = false;
  openModal: boolean = false;

  constructor(protected contractService: ContractService, protected utils: UtilsService) {
    this.contractService.getItems().subscribe(
      items => {
        this.items = items;
      }
    )
  }


  submitContrato(form: any) {

    if (form.invalid) {
      console.log('Formulário inválido');
      return;
    }


  }


  formatValue(event: Event, index: number) {
    // Obtém o valor diretamente do evento e remove todos os caracteres não numéricos
    let targetValue = (event.target as HTMLInputElement).value.replace(/\D/g, '');

    // Verifica se targetValue está vazio e define um valor padrão
    if (!targetValue) {
      // this.items[index].price = ''; // ou "0,00" se preferir
      (event.target as HTMLInputElement).value = ''; // Atualiza o valor no campo de input
      return;
    }

    // Divide o valor por 100 para inserir as casas decimais
    const formattedValue = new Intl.NumberFormat('pt-BR', {
      //style: 'currency',
      currency: 'BRL',
      minimumFractionDigits: 2,
      maximumFractionDigits: 2,
    }).format(parseFloat(targetValue) / 100);

    // Atualiza o valor no modelo e no campo de input
    // this.contract.valor[index] = formattedValue;
    (event.target as HTMLInputElement).value = formattedValue; // Exibe o valor formatado no campo de input
  }


  searchItem(value: string) {

  }

  addItem(
    item: {
      contractReferenceItemId: number;
      description: string;
      completeDescription: string;
      type: string;
      linking: string;
      quantity: number;
      price: string;
    }
    , index: number) {
    if (item.price === '0,00' || item.quantity === 0) {
      this.utils.showMessage("Para adicionar este item preencha o valor e a quantidade.", true);
      return;
    }

    this.removingIndex = index;
    this.changeValue = true;
    // Aguarda a animação antes de remover o item
    setTimeout(() => {
      this.items = this.items.filter(i => i.contractReferenceItemId !== item.contractReferenceItemId);
      this.totalValue = this.utils.sumValue(this.totalValue, this.utils.multiplyValue(item.price, item.quantity));
      this.totalItems += 1;
      this.removingIndex = null;
      this.changeValue = false;
    }, 900); // Tempo igual à transição no CSS
    this.contract.items.push(item);

  }

  protected readonly open = open;
  showItems: boolean = false;

  removingIndexContract: number | null = null;

  removeItem(item: {
    contractReferenceItemId: number;
    description: string;
    completeDescription: string;
    type: string;
    linking: string;
    quantity: number;
    price: string;
  }, index: number) {

    this.removingIndexContract = index;
    setTimeout(() => {
      this.removingIndexContract = null;
      this.items.push(item);
      this.contract.items = this.contract.items.filter(i => i.contractReferenceItemId !== item.contractReferenceItemId);
    }, 900); // Tempo igual à transição no CSS
  }


  setServiceQuantity(item: {
    contractReferenceItemId: number;
    description: string;
    completeDescription: string;
    type: string;
    linking: string;
    quantity: number;
    price: string;
  }) {
    this.items.filter(s => s.type === item.type && s.description.includes('SERVIÇO'))
      .forEach((i) => {
        i.quantity += item.quantity;
      });
  }
}
