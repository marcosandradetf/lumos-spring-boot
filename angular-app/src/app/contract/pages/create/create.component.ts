import {Component} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {NgForOf, NgIf} from '@angular/common';
import {ContractService} from '../../services/contract.service';
import {UtilsService} from '../../../core/service/utils.service';
import {ScreenMessageComponent} from '../../../shared/components/screen-message/screen-message.component';
import {ModalComponent} from '../../../shared/components/modal/modal.component';
import {TableComponent} from '../../../shared/components/table/table.component';
import {DomUtil} from 'leaflet';


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
    edital: string;
    unifyServices: boolean;
    items: {
      contractReferenceItemId: number;
      description: string;
      completeDescription: string;
      type: string;
      linking: string;
      itemDependency: string;
      quantity: number;
      price: string;
    }[]
  } = {
    number: '',
    socialReason: '',
    address: '',
    phone: '',
    cnpj: '',
    edital: '',
    unifyServices: false,
    items: [],
  }

  items: {
    contractReferenceItemId: number;
    description: string;
    completeDescription: string;
    type: string;
    linking: string;
    itemDependency: string;
    quantity: number;
    price: string;
  }[] = [];

  totalValue: string = "0,00";
  totalItems: number = 0;
  removingIndex: number | null = null;
  openModal: boolean = false;

  constructor(protected contractService: ContractService, protected utils: UtilsService) {
    this.contractService.getItems().subscribe(
      items => {
        this.items = items;
      }
    )
  }


  submitContrato(form: any, contractData: HTMLDivElement, contractItems: HTMLDivElement) {

    if (form.invalid) {
      return;
    }

    if (this.contract.items.length === 0) {
      contractData.classList.add('hidden');
      contractItems.classList.remove('hidden')
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
      itemDependency: string;
      quantity: number;
      price: string;
    }
    , index: number) {
    if (item.price === '0,00' || item.quantity === 0) {
      this.utils.showMessage("Para adicionar este item preencha o valor e a quantidade.", true);
      return;
    }

    this.removingIndex = index;
    // Aguarda a animação antes de remover o item
    setTimeout(() => {
      this.items = this.items.filter(i => i.contractReferenceItemId !== item.contractReferenceItemId);
      this.totalValue = this.utils.sumValue(this.totalValue, this.utils.multiplyValue(item.price, item.quantity));
      this.totalItems += 1;
      this.removingIndex = null;
    }, 900); // Tempo igual à transição no CSS
    this.contract.items.push(item);

  }

  protected readonly open = open;

  removingIndexContract: number | null = null;
  textContent: string = 'Clique para selecionar';

  removeItem(item: {
    contractReferenceItemId: number;
    description: string;
    completeDescription: string;
    type: string;
    linking: string;
    itemDependency: string;
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
    itemDependency: string;
    quantity: number;
    price: string;
  }) {
    const quantity = this.items
      .filter(s => s.type === item.type && !s.description.includes("SERVIÇO"))
      .reduce((sum, i) => sum + i.quantity, 0);

    this.items
      .filter(s => (s.itemDependency === item.type))
      .forEach(i => i.quantity = quantity);
  }

  removeLeadingZeros(input: HTMLInputElement) {
    input.value = input.value.replace(/^0+/, '');
  }

  reviewItems(contractItems: HTMLDivElement, steepFinal: HTMLDivElement) {
    if (this.contract.items.length > 0) {
      contractItems.classList.add('hidden');
      steepFinal.classList.remove('hidden');
    } else {
      this.utils.showMessage("Para revisar os itens do contrato, é necessário adicionar pelo menos um item", true)
    }
  }

  setCableQuantity(item: {
    contractReferenceItemId: number;
    description: string;
    completeDescription: string;
    type: string;
    linking: string;
    itemDependency: string;
    quantity: number;
    price: string
  }) {
    if (item.type !== "BRAÇO") {
      return;
    }
    let quantity = 0.0;

    this.items
      .filter(s => s.type === item.type)
      .forEach((i) => {
        if (i.linking) { // Verifica se linking é uma string válida
          if (i.linking.startsWith('1')) {
            console.log(i.quantity);
            quantity += i.quantity * 2.5;
          } else if (i.linking.startsWith('2')) {
            quantity += i.quantity * 8.5;
          } else if (i.linking.startsWith('3')) {
            quantity += i.quantity * 12.5;
          }
        }
      });

    const cable = this.items.find(s => s.type === "CABO");
    if (cable) {
      cable.quantity = quantity;
    }
  }

  onFileChange(event: any) {
    const file = event.target.files[0]; // Obtém o arquivo selecionado
    if (!file) return;

    this.textContent = file.name;

    // Converte o arquivo para Base64 (opcional, pode ser apenas o nome do arquivo)
    const reader = new FileReader();
    reader.readAsDataURL(file);
    reader.onload = () => {
      this.contract.edital = reader.result as string; // Armazena o Base64 no atributo 'edital'
    };

    // Caso prefira apenas armazenar o nome do arquivo:
    // this.contract.edital = file.name;
  }

  styleField(unify: HTMLSpanElement) {
    if(unify.classList.contains('btn-outline')) {
      unify.classList.remove('btn-outline');
      unify.classList.add('btn-primary');
      unify.innerText = 'Desativar Serviço Unificado';
      this.contract.unifyServices = true;
    } else {
      unify.classList.add('btn-outline');
      unify.classList.remove('btn-primary');
      this.contract.unifyServices = false;
      unify.innerText = 'Clique para Ativar Serviço Unificado';
    }
  }
}
