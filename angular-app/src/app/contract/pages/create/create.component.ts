import {Component} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {NgForOf, NgIf} from '@angular/common';
import {ContractService} from '../../services/contract.service';
import {UtilsService} from '../../../core/service/utils.service';
import {ScreenMessageComponent} from '../../../shared/components/screen-message/screen-message.component';
import {ModalComponent} from '../../../shared/components/modal/modal.component';
import {TableComponent} from '../../../shared/components/table/table.component';
import {DomUtil} from 'leaflet';
import {FileService} from '../../../core/service/file-service.service';
import {forkJoin} from 'rxjs';
import {AuthService} from '../../../core/auth/auth.service';
import {Router} from '@angular/router';
import {ContractReferenceItemsDTO, CreateContractDTO} from '../../contract-models';
import {Toast} from 'primeng/toast';
import {Title} from '@angular/platform-browser';


@Component({
  selector: 'app-create',
  standalone: true,
  imports: [
    FormsModule,
    NgIf,
    NgForOf,
    ModalComponent,
    TableComponent,
    Toast
  ],
  templateUrl: './create.component.html',
  styleUrl: './create.component.scss'
})
export class CreateComponent {
  contract: CreateContractDTO = {
    number: '',
    contractor: '',
    address: '',
    phone: '',
    cnpj: '',
    unifyServices: false,
    noticeFile: '',
    contractFile: '',
    userUUID: '',
    items: [],
  }

  noticeFile: File | null = null;
  contractFile: File | null = null;

  items: ContractReferenceItemsDTO[] = [];

  totalValue: string = "0,00";
  totalItems: number = 0;
  removingIndex: number | null = null;
  openModal: boolean = false;

  constructor(protected contractService: ContractService,
              protected utils: UtilsService,
              private fileService: FileService,
              private auth: AuthService,
              protected router: Router,
              private title: Title) {
    this.title.setTitle('Cadastrar Contrato');
    this.contract.userUUID = this.auth.getUser().uuid;
    this.contractService.getContractReferenceItems().subscribe(
      items => {
        this.items = items;
      }
    )
  }


  submitContract(form: any, contractData: HTMLDivElement, contractItems: HTMLDivElement) {
    if (form.invalid) return;
    if(this.loading) return;

    if (this.contract.items.length === 0) {
      contractData.classList.add('hidden');
      contractItems.classList.remove('hidden')
    } else {
      this.loading = true;
      const files: File[] = [];

      if (this.contractFile) {
        files.push(this.contractFile);
      }

      if (this.noticeFile) {
        files.push(this.noticeFile);
      }

      if (files.length > 0) {
        this.fileService.sendFiles(files).subscribe({
          next: responses => {
            let responseIndex = 0;

            if (this.contractFile && responseIndex < responses.length) {
              this.contract.contractFile = responses[responseIndex];
              responseIndex++;
            }

            if (this.noticeFile && responseIndex < responses.length) {
              this.contract.noticeFile = responses[responseIndex];
            }

            this.sendContract(); // Envia o formulário após o envio dos arquivos
          },
          error: error => {
            this.loading = false;
            this.utils.showMessage(error as string, 'error');
          }
        });
      } else {
        this.loading = true;
        this.sendContract();
      }
    }
  }

  sendContract() {
    this.contractService.createContract(this.contract).subscribe({
      next: response => this.resetForm(),
      error: error => {
        this.openModal = false;
        this.utils.showMessage(error.message as string, 'error');
        this.loading = false;
      },
    });
  }

  resetForm() {
    this.openModal = false;
    this.contract = {
      number: '',
      contractor: '',
      address: '',
      phone: '',
      cnpj: '',
      unifyServices: false,
      noticeFile: '',
      contractFile: '',
      userUUID: '',
      items: [],
    };
    this.loading = false;
    this.finish = true;
  }

  loading: boolean = false;
  finish: boolean = false;


  formatValue(event: Event, itemId: number) {
    // Obtém o valor diretamente do evento e remove todos os caracteres não numéricos
    let targetValue = (event.target as HTMLInputElement).value.replace(/\D/g, '');
    const item = this.items.find(i => i.contractReferenceItemId === itemId);
    if (!item) {
      return;
    }

    // Verifica se targetValue está vazio e define um valor padrão
    if (!targetValue) {
      item.price = '0,00';
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
    item.price = formattedValue;
    (event.target as HTMLInputElement).value = formattedValue; // Exibe o valor formatado no campo de input
    console.log(this.items);
  }


  addItem(
    item: ContractReferenceItemsDTO
    , index: number) {
    if (item.price === '0,00' || item.quantity === 0) {
      this.utils.showMessage("Para adicionar este item preencha o valor e a quantidade.", 'warn');
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
  typeSelect: string = '';

  removeItem(item: ContractReferenceItemsDTO, index: number) {

    this.removingIndexContract = index;
    setTimeout(() => {
      this.removingIndexContract = null;
      this.items.push(item);
      this.contract.items = this.contract.items.filter(i => i.contractReferenceItemId !== item.contractReferenceItemId);
      this.totalValue = this.utils.subValue(this.totalValue, this.utils.multiplyValue(item.price, item.quantity));
      this.totalItems -= 1;
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
      this.utils.showMessage("Para revisar os itens do contrato, é necessário adicionar pelo menos um item", 'warn');
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

  onFileSelected(event: any, fileType: string) {
    const file = event.target.files[0];
    if (fileType === 'notice') {
      this.noticeFile = file;
    } else if (fileType === 'contract') {
      this.contractFile = file;
    }
  }

  styleField(unify: HTMLSpanElement) {
    if (unify.classList.contains('btn-outline')) {
      unify.classList.remove('btn-outline');
      unify.innerText = 'Desativar Serviço Unificado';
      this.contract.unifyServices = true;
    } else {
      unify.classList.add('btn-outline');
      this.contract.unifyServices = false;
      unify.innerText = 'Clique para Ativar Serviço Unificado';
    }
  }
}
