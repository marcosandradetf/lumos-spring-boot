import {Component} from '@angular/core';
import {FormsModule} from '@angular/forms';
import { NgForOf, NgIf} from '@angular/common';
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
      id: number;
      type: string;
      length: string;
      power: string;
      quantity: number;
      price: string;
    }[],
    services: {
      id: number;
      name: string;
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
    services: []
  }

  items: {
    id: number;
    type: string;
    length: string;
    power: string;
    quantity: number;
    price: string;
    services: {
      id: number;
      name: string;
      quantity: number;
      price: string;
    }[];
  }[] = [];

  totalValue: string = "0,00";
  totalItems: number = 0;
  removingIndex: number | null = null;
  changeValue: boolean = false;
  openModal: boolean = false;
  services: { id: number; name: string; quantity: number; price: string; type: string }[] = [];

  constructor(protected contractService: ContractService, protected utils: UtilsService) {
    this.contractService.getItems().subscribe(
      items => {
        this.items = items;
        items.forEach(item => {
          if (item.services !== null) {
            item.services.forEach(s => {
              this.services.push(
                {
                  id: s.id,
                  name: s.name,
                  quantity: s.quantity,
                  price: s.price,
                  type: item.type
                }
              );
            })
            console.log(item.services)
          }
        })
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
      id: number;
      type: string;
      length: string;
      power: string;
      quantity: number;
      price: string;
      services: { id: number; name: string; quantity: number; price: string }[]
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
      this.items = this.items.filter(i => i.id !== item.id);
      this.totalValue = this.utils.sumValue(this.totalValue, this.utils.multiplyValue(item.price, item.quantity));
      this.totalItems += 1;
      this.removingIndex = null;
      this.changeValue = false;
    }, 900); // Tempo igual à transição no CSS
    this.contract.items.push(item);

  }

  addService(
      service: { id: number; name: string; quantity: number; price: string },
      index: number) {
    if (service.price === '0,00' || service.quantity === 0) {
      this.utils.showMessage("Para adicionar este serviço preencha o valor.", true);
      return;
    }



    this.removingIndex = index;
    this.changeValue = true;
    // Aguarda a animação antes de remover o item
    setTimeout(() => {
      this.services = this.services.filter(s => s.id !== service.id);
      this.totalValue = this.utils.sumValue(this.totalValue, this.utils.multiplyValue(service.price, service.quantity));
      this.totalItems += 1;
      this.removingIndex = null;
      this.changeValue = false;
    }, 900); // Tempo igual à transição no CSS
    this.contract.services.push(service);

  }

  protected readonly open = open;
  showItems: boolean = false;


  removingIndexContract: number | null = null;

  removeItem(ci: {
    id: number;
    type: string;
    length: string;
    power: string;
    quantity: number;
    price: string
  }, index: number) {
    this.removingIndexContract = index;
    const item = {
      id: ci.id,
      type: ci.type,
      length: ci.length,
      power: ci.power,
      quantity: ci.quantity,
      price: ci.price,
      services: []
    };
    setTimeout(() => {
      this.removingIndexContract = null;
      this.items.push(item);
      this.contract.items = this.contract.items.filter(i => i.id !== ci.id);
    }, 900); // Tempo igual à transição no CSS
  }
  removeService(service: {id: number; name: string; quantity: number; price: string; type: string}, index: number) {
    this.removingIndexContract = index;
    setTimeout(() => {
      this.removingIndexContract = null;
      this.services.push(service);
      this.contract.services = this.contract.services.filter(s => s.id !== service.id);
    }, 900); // Tempo igual à transição no CSS
  }

  setServiceQuantity(item: {id: number; type: string; length: string; power: string; quantity: number; price: string; services: {id: number; name: string; quantity: number; price: string}[]}) {
    this.services.filter(s => s.type = item.type)
      .forEach((i) => {
        console.log(item.type)
        console.log(i)
        i.quantity = item.quantity;
      });
  }
}
