import {Component} from '@angular/core';
import {SidebarComponent} from '../../../shared/components/sidebar/sidebar.component';
import {FormsModule} from '@angular/forms';
import {NgClass, NgForOf, NgIf} from '@angular/common';
import {Contract} from '../../contract-response.dto';
import {ItemRequest} from '../../itens-request.dto';
import {Deposit} from '../../../models/almoxarifado.model';
import {Type} from '../../../models/tipo.model';
import {ufRequest} from '../../../core/uf-request.dto';
import {citiesRequest} from '../../../core/cities-request.dto';
import {ContractService} from '../../services/contract.service';
import {EstoqueService} from '../../../stock/services/estoque.service';
import {IbgeService} from '../../../core/service/ibge.service';
import {catchError, tap, throwError} from 'rxjs';
import {UtilsService} from '../../../core/service/utils.service';
import {ScreenMessageComponent} from '../../../shared/components/screen-message/screen-message.component';
import {ModalComponent} from '../../../shared/components/modal/modal.component';


@Component({
  selector: 'app-create',
  standalone: true,
  imports: [
    FormsModule,
    NgIf,
    NgForOf,
    NgClass,
    ScreenMessageComponent,
    ModalComponent
  ],
  templateUrl: './create.component.html',
  styleUrl: './create.component.scss'
})
export class CreateComponent {

  contract: Contract = {
    numeroContrato: '',
    contratante: '',
    city: '',
    uf: '',
    region: '',
    idMaterial: [],
    qtde: [],
    valor: []
  }

  items: {
    id: number;
    type: string;
    length: string;
    power: string;
    quantity: number;
    price: string;
    services: [];
  }[] = []
  totalValue: string = "0,00";
  totalItems: number = 0;
  removingIndex: number | null = null;
  changeValue: boolean = false;

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
      this.contract.valor[index] = ''; // ou "0,00" se preferir
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
    this.contract.valor[index] = formattedValue;
    (event.target as HTMLInputElement).value = formattedValue; // Exibe o valor formatado no campo de input
  }


  searchItem(value: string) {

  }

  addItem(
    item: { id: number; type: string; length: string; power: string; quantity: number; price: string; services: [] }
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
      this.totalItems += item.quantity;
      this.removingIndex = null;
      this.changeValue = false;
    }, 900); // Tempo igual à transição no CSS

  }
}
