import {Component, ElementRef, ViewChild} from '@angular/core';
import {FormsModule, NgForm} from '@angular/forms';
import {NgIf} from '@angular/common';
import {SidebarComponent} from '../../shared/components/sidebar/sidebar.component';
import {TableComponent} from '../../shared/components/table/table.component';
import {Group} from '../../models/grupo.model';
import {Router} from '@angular/router';
import {EstoqueService} from '../services/estoque.service';
import {Title} from '@angular/platform-browser';
import {ButtonComponent} from '../../shared/components/button/button.component';
import {ModalComponent} from '../../shared/components/modal/modal.component';
import {State} from '../services/material.service';
import {catchError, tap, throwError} from 'rxjs';
import {AlertMessageComponent} from '../../shared/components/alert-message/alert-message.component';


@Component({
  selector: 'app-groups',
  standalone: true,
  imports: [
    FormsModule,
    NgIf,
    SidebarComponent,
    TableComponent,
    ButtonComponent,
    ModalComponent,
    AlertMessageComponent
  ],
  templateUrl: './groups.component.html',
  styleUrl: './groups.component.scss'
})
export class GroupsComponent {
  sidebarLinks = [
    { title: 'Gerenciar', path: '/estoque/materiais', id: 'opt1' },
    { title: 'Movimentar Estoque', path: '/estoque/movimento', id: 'opt2' },
    { title: 'Entrada de Nota Fiscal', path: '/estoque/entrada', id: 'opt3' },
    { title: 'Importar Material (.xlsx)', path: '/estoque/importar', id: 'opt4' },
    { title: 'Sugestão de Compra', path: '/estoque/sugestao', id: 'opt5' }
  ];
  formOpen: boolean = false;
  group = {
    groupName: '',
  }
  gps: Group[] = [];
  formSubmitted: boolean = false;
  message: string  = '';
  state: State = State.create;
  groupId: number = 0;
  @ViewChild('collapseDiv') collapseDiv!: ElementRef;
  @ViewChild('top') top!: ElementRef;

  constructor(protected router: Router, private stockService: EstoqueService,
              private title: Title,) {
    this.title.setTitle('Gerenciar - Grupos');
    this.stockService.getGroups().subscribe(
      g => this.gps = g
    );
  }

  setOpen() {
    this.formOpen = !this.formOpen;
    if (this.state === State.update && !this.formOpen) {
      this.state = State.create;
      this.group = {
        groupName: '',
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

    if (this.groupId === 0  && this.state === State.update) {
      this.message = 'Selecione outro grupo para atualizar ou feche essa opção.';
      return;
    }

    if (this.state === State.create) {
      this.stockService.insertGroup(this.group.groupName).pipe(
        tap(response => {
          this.group = {
            groupName: '',
          }
          this.formSubmitted = false;
          this.message = 'Grupo criado com sucesso.';
          this.gps = response;
        }),
        catchError(err => {
          console.log(err)
          this.message = err.error.message;
          return throwError(() => err);
        })
      ).subscribe();
    } else if (this.state === State.update) {
      this.stockService.updateGroup(this.groupId, this.group.groupName).pipe(
        tap(response => {
          this.group = {
            groupName: '',
          }
          this.formSubmitted = false;
          this.groupId = 0;
          this.message = 'Tipo atualizado com sucesso.';
          this.gps = response;
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

  updateGroup(g: Group) {
    if (this.collapseDiv) {
      this.state = State.update;
      if(!this.formOpen) this.collapseDiv.nativeElement.click();

      this.group.groupName = g.groupName;
      this.groupId = g.idGroup;
      if (this.top)
        this.top.nativeElement.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }
  }

  showConfirmation: boolean = false;
    serverMessage: string = '';
  alertType: string = '';

  deleteGroup() {
    this.serverMessage = '';

    this.stockService.deleteGroup(this.groupId).pipe(
      tap(response => {
        this.showConfirmation = false;
        this.serverMessage = 'Grupo excluído com sucesso.';
        this.alertType = 'alert-success';
        this.gps = response;
      }),catchError(err => {
        this.showConfirmation = false;
        this.serverMessage = err.error.message;
        this.alertType = 'alert-error';
        return throwError(() => err);
      })
    ).subscribe();
  }

}
