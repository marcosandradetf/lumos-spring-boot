import {Component, ElementRef, ViewChild} from '@angular/core';
import {SidebarComponent} from '../../shared/components/sidebar/sidebar.component';
import {StockService} from '../services/stock.service';
import {Title} from '@angular/platform-browser';
import {Router} from '@angular/router';
import {FormsModule, NgForm} from '@angular/forms';
import {NgClass, NgIf} from '@angular/common';
import {Company} from '../../models/empresa.model';
import {TableComponent} from '../../shared/components/table/table.component';
import {Deposit} from '../../models/almoxarifado.model';
import {catchError, tap, throwError} from 'rxjs';
import {State} from '../services/material.service';
import {ButtonComponent} from '../../shared/components/button/button.component';
import {ModalComponent} from '../../shared/components/modal/modal.component';
import {AlertMessageComponent} from '../../shared/components/alert-message/alert-message.component';
import {ufRequest} from '../../core/uf-request.dto';
import {IbgeService} from '../../core/service/ibge.service';
import {citiesRequest} from '../../core/cities-request.dto';

@Component({
  selector: 'app-deposits',
  standalone: true,
  imports: [
    FormsModule,
    NgIf,
    TableComponent,
    ButtonComponent,
    ModalComponent,
    AlertMessageComponent,
    NgClass,
  ],
  templateUrl: './deposits.component.html',
  styleUrl: './deposits.component.scss'
})
export class TruckDepositComponent {
  sidebarLinks = [
    {title: 'Gerenciar', path: '/estoque/materiais', id: 'opt1'},
    {title: 'Movimentar Estoque', path: '/estoque/movimento', id: 'opt2'},
    {title: 'Entrada de Nota Fiscal', path: '/estoque/entrada', id: 'opt3'},
    {title: 'Importar Material (.xlsx)', path: '/estoque/importar', id: 'opt4'},
    {title: 'Sugestão de Compra', path: '/estoque/sugestao', id: 'opt5'}
  ];
  formOpen: boolean = false;
  deposit = {
    depositName: "",
    companyId: "",
    depositAddress: "",
    depositDistrict: "",
    depositCity: "",
    depositState: "",
    depositRegion: "",
    depositPhone: "",
  }
  formSubmitted: null | boolean = false;
  companies: Company[] = []
  deposits: Deposit[] = [];
  ufs: ufRequest[] = [];
  cities: citiesRequest[] = [];
  selectedRegion: string = '';

  message: string = '';
  state: State = State.create;
  depositId: number = 0;
  @ViewChild('collapseDiv') collapseDiv!: ElementRef;
  @ViewChild('top') top!: ElementRef;

  constructor(private stockService: StockService,
              private title: Title, protected router: Router,
              private ibgeService: IbgeService) {
    this.title.setTitle('Gerenciar - Caminhões');

    this.stockService.getCompanies().subscribe(
      c => this.companies = c
    );
    this.stockService.getDeposits().subscribe(
      d => this.deposits = d.filter(d => d.isTruck)
    );

    this.ibgeService.getUfs().subscribe((ufs: ufRequest[]) => {
      this.ufs = ufs;
    });
  }

  setOpen() {
    this.formOpen = !this.formOpen;
    if (this.state === State.update && !this.formOpen) {
      this.state = State.create;
      this.deposit = {
        depositName: "",
        companyId: "",
        depositAddress: "",
        depositDistrict: "",
        depositCity: "",
        depositState: "",
        depositRegion: "",
        depositPhone: "",
      }
      this.formSubmitted = false;
      this.message = '';
    }
  }

  onSubmit(myForm: NgForm) {
    this.formSubmitted = true;
    if (myForm.invalid) {
      return;
    }

    if (this.depositId === 0 && this.state === State.update) {
      this.message = 'Selecione outro almoxarifado para atualizar ou feche essa opção.';
      return;
    }

    if (this.state === State.create) {
      this.stockService.insertDeposit(this.deposit).pipe(
        tap(response => {
          this.deposit = {
            depositName: "",
            companyId: "",
            depositAddress: "",
            depositDistrict: "",
            depositCity: "",
            depositState: "",
            depositRegion: "",
            depositPhone: "",
          }
          this.formSubmitted = false
          this.message = 'Almoxarifado foi criado com sucesso.';
          this.deposits = response;
        }), catchError(err => {
          console.log(err);
          this.message = err.error.message;
          return throwError(() => throwError(() => this.message));
        })
      ).subscribe();
    } else if (this.state === State.update) {
      this.stockService.updateDeposit(this.depositId, this.deposit).pipe(
        tap(response => {
          this.deposit = {
            depositName: "",
            companyId: "",
            depositAddress: "",
            depositDistrict: "",
            depositCity: "",
            depositState: "",
            depositRegion: "",
            depositPhone: "",
          }
          this.formSubmitted = false
          this.depositId = 0;
          this.message = 'Almoxarifado foi atualizado com sucesso.';
          this.deposits = response;
        }), catchError(err => {
          console.log(err);
          this.message = err.error.message;
          return throwError(() => throwError(() => this.message));
        })
      ).subscribe();
    }

  }

  protected readonly State = State;

  updateDeposit(d: Deposit) {
    if (this.collapseDiv) {
      this.state = State.update;
      if (!this.formOpen) this.collapseDiv.nativeElement.click();


      this.getCities(d.depositState);

      this.deposit.depositName = d.depositName;
      this.deposit.companyId = '';
      this.deposit.depositState = d.depositState;
      this.deposit.depositCity = d.depositCity;
      this.deposit.depositRegion = d.depositRegion;
      this.deposit.depositAddress = d.depositAddress;
      this.deposit.depositDistrict = d.depositDistrict;
      this.deposit.depositPhone = d.depositPhone;
      this.depositId = d.idDeposit;
      if (this.top)
        this.top.nativeElement.scrollIntoView({behavior: 'smooth', block: 'start'});
    }
  }

  showConfirmation: boolean = false;
  serverMessage: string = '';
  alertType: string = '';

  deleteDeposit() {
    this.serverMessage = '';

    this.stockService.deleteDeposit(this.depositId).pipe(
      tap(response => {
        this.showConfirmation = false;
        this.serverMessage = 'Almoxarifado excluído com sucesso.';
        this.alertType = 'alert-success';
        this.deposits = response;
      }), catchError(err => {
        this.showConfirmation = false;
        this.serverMessage = err.error.message;
        this.alertType = 'alert-error';
        return throwError(() => err);
      })
    ).subscribe();
  }

  getCities(uf: string) {
    this.ibgeService.getCities(uf).subscribe(cities => {
      this.cities = cities;
    })
  }

  updateRegion(selectedCityName: string): void {
    const selectedCity = this.cities.find(city => city.nome === selectedCityName);
    this.deposit.depositRegion = selectedCity ? selectedCity.microrregiao.mesorregiao.nome : '';
  }

  formatTel(event: any): void {
    let value = event.target.value;

    // Remove qualquer caractere que não seja número
    value = value.replace(/\D/g, '');

    // Formata para (XX) XXXXX-XXXX ou (XX) XXXX-XXXX
    if (value.length > 10) {
      value = value.replace(/(\d{2})(\d{5})(\d{4})/, '($1) $2-$3');
    } else if (value.length > 5) {
      value = value.replace(/(\d{2})(\d{4})(\d{0,4})/, '($1) $2-$3');
    } else if (value.length > 2) {
      value = value.replace(/(\d{2})(\d{0,5})/, '($1) $2');
    }

    event.target.value = value; // Atualiza o valor do input
  }

  formatPhone(phone: string): string {
    return phone.replace(/(\d{2})(\d{5})(\d{4})/, '($1) $2-$3');
  }

  formatPhoneView(phone: string): string {
    if (!phone) {
      return ''; // Retorna vazio se o telefone for null, undefined ou vazio
    }

    // Remove quaisquer caracteres que não sejam números
    phone = phone.replace(/\D/g, '');

    // Formata corretamente com base no tamanho do telefone
    if (phone.length === 11) {
      // Exemplo: 11999999999 -> (11) 99999-9999
      return phone.replace(/(\d{2})(\d{5})(\d{4})/, '($1) $2-$3');
    } else if (phone.length === 10) {
      // Exemplo: 1199999999 -> (11) 9999-9999
      return phone.replace(/(\d{2})(\d{4})(\d{4})/, '($1) $2-$3');
    } else if (phone.length > 2) {
      // Exemplo: números parciais (119) ou inválidos
      return phone.replace(/(\d{2})/, '($1) ') + phone.substring(2);
    }

    // Retorna o valor original para casos inesperados
    return phone;
  }

  // Atualiza o modelo com o valor limpo
  updatePhone(formattedPhone: string): void {
    this.deposit.depositPhone = formattedPhone.replace(/\D/g, ''); // Remove caracteres especiais
  }
}
