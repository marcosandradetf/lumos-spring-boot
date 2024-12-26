import {Component, ElementRef, ViewChild} from '@angular/core';
import {SidebarComponent} from '../../shared/components/sidebar/sidebar.component';
import {EstoqueService} from '../services/estoque.service';
import {Title} from '@angular/platform-browser';
import {Router} from '@angular/router';
import {FormsModule, NgForm} from '@angular/forms';
import {NgIf} from '@angular/common';
import {Company} from '../../core/models/empresa.model';
import {TableComponent} from '../../shared/components/table/table.component';
import {Deposit} from '../../core/models/almoxarifado.model';
import {catchError, tap, throwError} from 'rxjs';
import {State} from '../services/material.service';
import {ButtonComponent} from '../../shared/components/button/button.component';
import {ModalComponent} from '../../shared/components/modal/modal.component';
import {Type} from '../../core/models/tipo.model';

@Component({
  selector: 'app-deposits',
  standalone: true,
  imports: [
    SidebarComponent,
    FormsModule,
    NgIf,
    TableComponent,
    ButtonComponent,
    ModalComponent
  ],
  templateUrl: './deposits.component.html',
  styleUrl: './deposits.component.scss'
})
export class DepositsComponent {
  sidebarLinks = [
    { title: 'Gerenciar', path: '/estoque/materiais', id: 'opt1' },
    { title: 'Movimentar Estoque', path: '/estoque/movimento', id: 'opt2' },
    { title: 'Entrada de Nota Fiscal', path: '/estoque/entrada', id: 'opt3' },
    { title: 'Importar Material (.xlsx)', path: '/estoque/importar', id: 'opt4' },
    { title: 'Sugestão de Compra', path: '/estoque/sugestao', id: 'opt5' }
  ];
  formOpen: boolean = false;
  deposit= {
    depositName: "",
    companyId: ""
  }
  formSubmitted: null | boolean = false;
  companies: Company[] = []
  deposits: Deposit[] = [];
  message: string = '';
  state: State = State.create;
  depositId: number = 0;
  @ViewChild('collapseDiv') collapseDiv!: ElementRef;
  @ViewChild('top') top!: ElementRef;

  constructor(private stockService: EstoqueService,
              private title: Title, protected router: Router) {
    this.title.setTitle('Gerenciar - Almoxarifados');

    this.stockService.getCompanies().subscribe(
      c => this.companies = c
    );
    this.stockService.getDeposits().subscribe(
      d => this.deposits = d
    )
  }

  setOpen() {
    this.formOpen = !this.formOpen;
    if (this.state === State.update && !this.formOpen) {
      this.state = State.create;
      this.deposit = {
        depositName: '',
        companyId: ''
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

    if(this.depositId === 0 && this.state === State.update) {
      this.message = 'Selecione outro almoxarifado para atualizar ou feche essa opção.';
      return;
    }

    if(this.state === State.create) {
      this.stockService.insertDeposit(this.deposit).pipe(
        tap(response => {
          this.deposit = {
            depositName: '',
            companyId: ''
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
            depositName: '',
            companyId: ''
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
      if(!this.formOpen) this.collapseDiv.nativeElement.click();

      this.deposit.depositName = d.depositName;
      this.deposit.companyId = '';
      this.depositId = d.idDeposit;
      if (this.top)
        this.top.nativeElement.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }
  }

  showConfirmation: boolean = false;

  deleteDeposit() {
    this.stockService.deleteDeposit(this.depositId).pipe(
      tap(response => {
        this.message = 'Almoxarifado excluído com sucesso.';
        this.deposits = response;
      }),catchError(err => {
        this.message = err.error.message;
        return throwError(() => err);
      })
    ).subscribe();
  }
  
}
