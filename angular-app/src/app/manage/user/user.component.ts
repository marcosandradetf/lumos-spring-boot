import {Component} from '@angular/core';
import {SidebarComponent} from "../../shared/components/sidebar/sidebar.component";
import {Router} from '@angular/router';
import {TableComponent} from '../../shared/components/table/table.component';
import {ButtonComponent} from '../../shared/components/button/button.component';
import {FormsModule, NgForm, ReactiveFormsModule} from '@angular/forms';
import {DatePipe, NgForOf, NgIf} from '@angular/common';
import {UserService} from './user-service.service';
import {catchError, tap, throwError} from 'rxjs';
import {HttpErrorResponse} from '@angular/common/http';
import {UtilsService} from '../../core/service/utils.service';
import {AlertMessageComponent} from '../../shared/components/alert-message/alert-message.component';
import {ModalComponent} from '../../shared/components/modal/modal.component';
import {AuthService} from '../../core/auth/auth.service';
import {NoAccessComponent} from '../../shared/components/no-access/no-access.component';
import {Title} from '@angular/platform-browser';

@Component({
  selector: 'app-user',
  standalone: true,
  imports: [
    SidebarComponent,
    TableComponent,
    ButtonComponent,
    FormsModule,
    ReactiveFormsModule,
    NgForOf,
    DatePipe,
    NgIf,
    AlertMessageComponent,
    ModalComponent,
    NoAccessComponent
  ],
  templateUrl: './user.component.html',
  styleUrl: './user.component.scss'
})

export class UserComponent {
  sidebarLinks = [
    {title: 'Início', path: '/configuracoes/dashboard', id: 'opt1'},
    {title: 'Usuários', path: '/configuracoes/usuarios', id: 'opt2'},
    {title: 'Equipes', path: '/configuracoes/equipes', id: 'opt3'},
    {title: 'Minha Empresa', path: '/configuracoes/empresa', id: 'opt4'},
  ];

  change: boolean = false;
  users: {
    userId: string,
    username: string,
    name: string,
    lastname: string,
    email: string,
    year: string;
    month: string;
    day: string;
    role: string[],
    status: boolean
    sel: boolean
  }[] = [];

  usersBackup: {
    userId: string,
    username: string,
    name: string,
    lastname: string,
    email: string,
    year: string;
    month: string;
    day: string;
    role: string[],
    status: boolean
    sel: boolean
  }[] = [];


  rolesUser: {
    userId: string,
    role: string,
  }[] = [];

  roles: {
    selected: boolean,
    roleId: string,
    roleName: string,
  }[] = [];

  add: boolean = false;

  months: {
    number: string,
    name: string,
  }[] = [
    {number: '1', name: "Janeiro"},
    {number: '2', name: "Fevereiro"},
    {number: '3', name: "Março"},
    {number: '4', name: "Abril"},
    {number: '5', name: "Maio"},
    {number: '6', name: "Junho"},
    {number: '7', name: "Julho"},
    {number: '8', name: "Agosto"},
    {number: '9', name: "Setembro"},
    {number: '10', name: "Outubro"},
    {number: '11', name: "Novembro"},
    {number: '12', name: "Dezembro"},
  ];

  formSubmitted: boolean = false;
  validation: string = '';
  serverMessage: string | null = null;
  alertType: string | null = null;
  loading: boolean = false;
  userId: string = '';
  openConfirmationModal: boolean = false;

  getMonth(monthNumber: string) {
    return this.months.find(m => m.number === monthNumber);
  }


  constructor(protected router: Router, private userService: UserService, protected utils: UtilsService,
              protected authService: AuthService, private titleService: Title) {

    this.titleService.setTitle("Configurações - Usuários");

    this.userService.getUsers().subscribe(
      users => {
        this.users = users;
        this.usersBackup = users;

        this.rolesUser = users.flatMap(user =>
          user.role.map(role => ({
            userId: user.userId,
            role: role
          }))
        );
      }
    );

    this.userService.getRoles().subscribe(
      roles => {
        this.roles = roles;
      }
    );

  }


  changeUser() {
    return () => {
      this.change = true;
    };
  }

  addUser() {
    return () => {
      this.add = true;
    }
  }

  resetView() {
    return () => {
      this.change = false;
      this.add = false;
      this.users = JSON.parse(JSON.stringify(this.usersBackup));
    }
  }

  submitUsers(form: NgForm) {
    this.formSubmitted = true;

    if (form.invalid) {
      console.log('Formulário inválido');
      return;
    }

    this.loading = true;

    const insert = this.users.some(u => u.userId === '');
    const update = this.users.every(u => u.userId !== '');
    const updateCheckSel = this.users.some(u => u.sel);

    if (insert && this.users.length !== this.usersBackup.length) {
      this.insertUsers();
    } else if (update && updateCheckSel) {
      this.updateUsers();
    }

    this.validation = "";
  }

  private insertUsers() {

    this.userService.insertUsers(this.users).pipe(
      tap(r => {
        this.showMessage("Usuários criados com sucesso.");
        this.alertType = "alert-success";
        this.users = r;
        this.usersBackup = r;
        this.resetView();
      }),
      catchError(err => {
        this.showMessage(err.error.message);
        this.alertType = "alert-error";
        throw err;
      })
    ).subscribe();

    this.loading = false;
  }

  private updateUsers() {
    // Verifica se nenhum usuário foi selecionado
    const noneSelected = this.users.every(u => !u.sel);

    if (noneSelected) {
      // this.serverMessage = "Nenhum usuário foi selecionado."
      this.showMessage("Nenhum usuário foi selecionado.");
      this.alertType = "alert-error";
      this.loading = false;
      return;
    }

    this.userService.updateUser(this.users)
      .pipe(tap(r => {
          this.showMessage("Usuários atualizados com sucesso.");
          this.alertType = "alert-success";
          this.users = r;
          this.usersBackup = r;
          this.change = false;
        }),
        catchError(err => {
          this.showMessage(err.error.message);
          this.alertType = "alert-error";
          throw err;
        })
      ).subscribe();

    this.loading = false;

  }

  private showMessage(message: string, timeout = 3000) {
    this.serverMessage = message;
    setTimeout(() => {
      this.serverMessage = null;
    }, timeout);
  }


  resetPassword() {
    if (this.userId !== '') {
      this.userService.resetPassword(this.userId).pipe(
        tap(r => {
        }),
        catchError(err => {
          console.log(err)
          return throwError(() => err);

        })
      ).subscribe();
    }

  }

  newUser() {
    const user = {
      userId: "",
      username: "",
      name: "",
      lastname: "",
      email: "",
      year: "",
      month: "",
      day: "",
      role: [],
      status: true,
      sel: false
    };
    this.users.push(user);
  }


  removeUser() {
    const lastElement = this.users[this.users.length - 1];
    if (lastElement.userId === '') {
      this.users.pop();
    }
  }

  changeRole(id: string, nomeRole: string) {
    const userIndex = this.users.findIndex(u => u.userId === id);
    if (userIndex === -1) {
      console.log('Usuário não encontrado');
      return;
    }
    const user = this.users.find(u => u.userId === id);
    if (!user) {
      console.log('Usuário não encontrado');
      return;
    }

    // Obter todas as roles associadas ao usuário
    let roles = this.rolesUser.filter(u => u.userId === id);

    // Verificar se a role já existe
    const roleExists = roles.some(role => role.role === nomeRole);

    if (roleExists) {
      // Se a role já existe, removê-la
      roles = roles.filter(role => role.role !== nomeRole);
    } else {
      // Caso contrário, adicionar a nova role
      roles.push({userId: id, role: nomeRole});
    }

    // Atualizar as roles do usuário
    this.rolesUser = this.rolesUser.filter(u => u.userId !== id);
    user.role = [];
    roles.forEach(role => {
      this.rolesUser.push(role);
      user.role.push(role.role);
    });


    this.users[userIndex] = user;
  }


  filterRolesByUserId(userId: string) {
    return this.rolesUser.filter(role => role.userId === userId);
  }

  verifyRole(userId: string, nomeRole: string): boolean {
    const userRoles = this.filterRolesByUserId(userId); // Obtém as roles do usuário filtrado

    // Verifica se alguma role no array corresponde ao nomeRole fornecido
    return userRoles.some(role => role.role === nomeRole);
  }


  handleClick(dropdown: HTMLDetailsElement, month: string, userId: string) {
    // Fecha o dropdown
    dropdown.open = false;
    console.log(month)

    const userIndex = this.users.findIndex(u => u.userId === userId);
    if (userIndex === -1) {
      return;
    }

    console.log(userIndex);
    console.log(this.users)

    this.users[userIndex].month = month;

    console.log(this.users);

  }

  getMaxDay(month: string, year: string): number {
    const daysInMonth: Record<number, number> = {
      1: 31, // Janeiro
      2: this.isLeapYear(year) ? 29 : 28, // Fevereiro
      3: 31, // Março
      4: 30, // Abril
      5: 31, // Maio
      6: 30, // Junho
      7: 31, // Julho
      8: 31, // Agosto
      9: 30, // Setembro
      10: 31, // Outubro
      11: 30, // Novembro
      12: 31, // Dezembro
    };
    return daysInMonth[parseInt(month, 10)] || 31; // Valor padrão para meses inválidos
  }


  isLeapYear(strYear: string): boolean {
    const year = parseInt(strYear, 10);
    return (year % 4 === 0 && year % 100 !== 0) || year % 400 === 0;
  }

  confirmResetPassword(userId: string) {
    return () => {
      this.openConfirmationModal = true;
      this.userId = userId;
    };
  }
}
