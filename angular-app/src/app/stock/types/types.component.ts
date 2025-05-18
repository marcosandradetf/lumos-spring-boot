import {Component, ElementRef, ViewChild} from '@angular/core';
import {FormsModule, NgForm} from '@angular/forms';
import {NgIf} from '@angular/common';
import {SidebarComponent} from '../../shared/components/sidebar/sidebar.component';
import {TableComponent} from '../../shared/components/table/table.component';
import {EstoqueService} from '../services/estoque.service';
import {Title} from '@angular/platform-browser';
import {Router} from '@angular/router';
import {Type} from '../../models/tipo.model';
import {Group} from '../../models/grupo.model';
import {catchError, tap, throwError} from 'rxjs';
import {State} from '../services/material.service';
import {ButtonComponent} from '../../shared/components/button/button.component';
import {ModalComponent} from '../../shared/components/modal/modal.component';
import {AlertMessageComponent} from '../../shared/components/alert-message/alert-message.component';

@Component({
  selector: 'app-types',
  standalone: true,
  imports: [
    FormsModule,
    NgIf,
    TableComponent,
    ButtonComponent,
    ModalComponent,
    AlertMessageComponent
  ],
  templateUrl: './types.component.html',
  styleUrl: './types.component.scss'
})
export class TypesComponent {
    sidebarLinks = [
    { title: 'Gerenciar', path: '/estoque/materiais', id: 'opt1' },
    { title: 'Movimentar Estoque', path: '/estoque/movimento', id: 'opt2' },
    { title: 'Entrada de Nota Fiscal', path: '/estoque/entrada', id: 'opt3' },
    { title: 'Importar Material (.xlsx)', path: '/estoque/importar', id: 'opt4' },
    { title: 'Sugestão de Compra', path: '/estoque/sugestao', id: 'opt5' }
  ];
  formOpen: boolean = false;
  type = {
    typeName: '',
    groupId: ''
  }
  types: Type[] = [];
  gps: Group[] = [];
  formSubmitted: null | boolean = false;
  message: string  = '';
  state: State = State.create;
  typeId: number = 0;
  @ViewChild('collapseDiv') collapseDiv!: ElementRef;
  @ViewChild('top') top!: ElementRef;


  constructor(private stockService: EstoqueService,
              private title: Title, protected router: Router) {
    this.title.setTitle('Gerenciar - Tipos');
    this.stockService.getTypes().subscribe(
      t => this.types = t
    );
    this.stockService.getGroups().subscribe(
      g => this.gps = g
    );
  }

  setOpen() {
    this.formOpen = !this.formOpen;
    if (this.state === State.update && !this.formOpen) {
      this.state = State.create;
      this.type = {
        typeName: '',
        groupId: ''
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

    if (this.typeId === 0  && this.state === State.update) {
      this.message = 'Selecione outro tipo para atualizar ou feche essa opção.';
      return;
    }

    if (this.state === State.create) {
      this.stockService.insertType(this.type).pipe(
        tap(response => {
          this.type = {
            typeName: '',
            groupId: ''
          }
          this.formSubmitted = false;
          this.message = 'Tipo criado com sucesso.';
          this.types = response;
        }),
        catchError(err => {
          console.log(err)
          this.message = err.error.message;
          return throwError(() => err);
        })
      ).subscribe();
    } else if (this.state === State.update) {
      this.stockService.updateType(this.typeId, this.type).pipe(
        tap(response => {
          this.type = {
            typeName: '',
            groupId: ''
          }
          this.formSubmitted = false;
          this.typeId = 0;
          this.message = 'Tipo atualizado com sucesso.';
          this.types = response;
        }),
        catchError(err => {
          console.log(err)
          this.message = err.error.message;
          return throwError(() => err);
        })
      ).subscribe();
    }

  }

  protected readonly State = State;

  updateType(t: Type) {
    if (this.collapseDiv) {
      this.state = State.update;
      if(!this.formOpen) this.collapseDiv.nativeElement.click();

      this.type.typeName = t.typeName;
      this.type.groupId = t.group.idGroup.toString();
      this.typeId = t.idType;
      if (this.top)
        this.top.nativeElement.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }
  }

  showConfirmation: boolean = false;
  serverMessage: string = '';
  alertType: string = '';

  deleteType() {
    this.serverMessage = '';  // Reseta a mensagem para garantir que o *ngIf seja acionado

    this.stockService.deleteType(this.typeId).pipe(
      tap(response => {
        this.showConfirmation = false;
        this.serverMessage = 'Tipo excluído com sucesso.';
        this.alertType = 'alert-success';
        this.types = response;
      }),catchError(err => {
        this.showConfirmation = false;
        this.serverMessage = err.error.message;
        this.alertType = 'alert-error';
        return throwError(() => err);
      })
    ).subscribe();
  }

}
