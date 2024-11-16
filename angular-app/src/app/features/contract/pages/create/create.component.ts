import {Component} from '@angular/core';
import {SidebarComponent} from '../../../../shared/components/sidebar/sidebar.component';
import {FormsModule} from '@angular/forms';
import {NgClass, NgForOf, NgIf} from '@angular/common';
import {Contract} from '../../contract-response.dto';
import {catchError, tap, throwError} from 'rxjs';
import {ContractService} from '../../services/contract.service';
import {ItemRequest} from '../../itens-request.dto';
import {Deposit} from '../../../../core/models/almoxarifado.model';
import {EstoqueService} from '../../../estoque/services/estoque.service';
import {Type} from '../../../../core/models/tipo.model';
import {ufRequest} from '../../../../core/uf-request.dto';
import {citiesRequest} from '../../../../core/cities-request.dto';
import {IbgeService} from '../../../../core/service/ibge.service';

@Component({
  selector: 'app-create',
  standalone: true,
  imports: [
    SidebarComponent,
    FormsModule,
    NgIf,
    NgForOf,
    NgClass
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
  allItens: ItemRequest[] = [];
  itensRequest: ItemRequest[] = [];
  formSubmitted: boolean = false;
  almoxarifados: Deposit[] = [];
  tipos: Type[] = [];
  ufs: ufRequest[] = [];
  cities: citiesRequest[] = [];
  private searchFilter: string = '';
  private almoxarifadoFilter: string = '';
  private typeFilter: string = '';
  selectedRegion: string = '';
  currentPage: string = "0";
  totalPages: number = 0;
  pages: number[] = [];

  constructor(protected contractService: ContractService,
              private estoqueService: EstoqueService,
              private ibgeService: IbgeService,) {

    this.loadItens(this.currentPage);

    this.estoqueService.getDeposits().subscribe((almoxarifados: Deposit[]) => {
      this.almoxarifados = almoxarifados;
    });
    this.estoqueService.getTypes().subscribe((tipos: Type[]) => {
      this.tipos = tipos;
    });

    this.ibgeService.getUfs().subscribe((ufs: ufRequest[]) => {
      this.ufs = ufs;
    });
  }

  getCities(uf: string) {
    this.ibgeService.getCities(uf).subscribe(cities => {
      this.cities = cities;
    })
  }

  // Busca a cidade completa e atualiza a região
  updateRegion(selectedCityName: string): void {
    const selectedCity = this.cities.find(city => city.nome === selectedCityName);
    this.selectedRegion = selectedCity ? selectedCity.microrregiao.nome : '';
  }

  filterType(value: string) {
    this.typeFilter = value.toLowerCase();
    this.applyCombinedFilters();
  }

  // Atualiza o filtro de pesquisa e aplica os filtros combinados
  filterSearch(value: string): void {
    this.searchFilter = value.toLowerCase(); // Armazena o filtro de pesquisa
    this.applyCombinedFilters(); // Aplica ambos os filtros
  }

// Atualiza o filtro de almoxarifado e aplica os filtros combinados
  filterAlmoxarifado(value: string): void {
    this.almoxarifadoFilter = value.toLowerCase(); // Armazena o filtro de almoxarifado
    this.applyCombinedFilters(); // Aplica ambos os filtros
  }

// Aplica os filtros combinados (almoxarifado e pesquisa)
  applyCombinedFilters(): void {
    this.itensRequest = this.allItens.filter(item => {
      const matchesAlmoxarifado = this.almoxarifadoFilter
        ? item.almoxarifado.toLowerCase() === this.almoxarifadoFilter
        : true; // Retorna true se não houver filtro de almoxarifado

      const matchesSearch = this.searchFilter
        ? item.nomeMaterial.toLowerCase().includes(this.searchFilter) ||
        item.marcaMaterial.toLowerCase().includes(this.searchFilter)
        : true; // Retorna true se não houver filtro de pesquisa

      const matchesType = this.typeFilter
        ? item.tipoMaterial.toLowerCase().includes(this.typeFilter)
        : true;

      return matchesAlmoxarifado && matchesSearch && matchesType; // Retorna apenas se ambos os filtros corresponderem
    });
  }

  toggleSelection(item: ItemRequest) {
    // Atualiza os arrays de `idMaterial`, `qtde` e `valor` com base no status do checkbox
    if (item.selected) {
      this.contract.idMaterial.push(item.idMaterial);
      this.contract.qtde.push(item.qtdeEstoque || 0);
      this.contract.valor.push(item.valor);
    } else {
      const index = this.contract.idMaterial.indexOf(item.idMaterial);
      if (index !== -1) {
        this.contract.idMaterial.splice(index, 1);
        this.contract.qtde.splice(index, 1);
        this.contract.valor.splice(index, 1);
      }
    }
  }

  submitContrato(form: any) {
    this.formSubmitted = true;

    if (form.invalid) {
      console.log('Formulário inválido');
      return;
    }

    // Prepara o contrato para envio ao backend
    const contratoRequest: Contract = {
      ...this.contract,
      idMaterial: this.contract.idMaterial.filter((_, i) => this.itensRequest[i].selected),
      qtde: this.contract.qtde.filter((_, i) => this.itensRequest[i].selected),
      valor: this.contract.valor.filter((_, i) => this.itensRequest[i].selected)
    };

    this.contractService.createContract(contratoRequest).pipe(
      tap(response => {
        console.log(response);
      }),
      catchError(err => {
        console.log(err);
        return throwError(() => err);
      })
    ).subscribe()
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

  private loadItens(page: string) {
    this.contractService.getAllItens(page, "20")
      .subscribe(response => {
      this.allItens = response.content; // Armazena todos os itens recebidos
      this.itensRequest.filter(item => item.almoxarifado === '');
    });
  }
}
