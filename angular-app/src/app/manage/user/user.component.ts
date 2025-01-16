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
    NgIf
  ],
  templateUrl: './user.component.html',
  styleUrl: './user.component.scss'
})

export class UserComponent {
  sidebarLinks = [
    {title: 'Início', path: '/configuracoes', id: 'opt1'},
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
    dateOfBirth: string,
    day: string;
    month: string;
    year: string;
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
    idRole: string,
    nomeRole: string,
  }[] = [];

  private message: string = '';
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

  getMonth(monthNumber: string) {
    return this.months.find(m => m.number === monthNumber);
  }


  constructor(protected router: Router, private userService: UserService, protected utils: UtilsService) {
    this.userService.getUsers().subscribe(
      users => {
        this.users = users;
        this.users.forEach((u) => {
          const [year, month, day] = u.dateOfBirth.split("-");
          // Preencha as propriedades user.day, user.month e user.year
          u.day = String(+day);   // Usando + para converter em número
          u.month = String(+month);   // Usando + para converter em número
          u.year = String(+year);   // Usando + para converter em número
        })

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

  cancel() {
    return () => {
      this.change = false;
      this.add = false;
    }
  }

  submitUsers(usersForm: NgForm) {
    this.formSubmitted = true;

    this.users.forEach(user => {
      if (user.month === '0') {
        this.validation = "Mês inválido";
        return;
      } else if (user.month === '') {
        this.validation = 'Campo obrigatório';
        return;
      }
    });

    this.validation = "";
  }

  resetPassword(userId: string) {
    return () => {
      this.userService.resetPassword(userId).pipe(
        tap(r => {
          this.message = ((r as any).message);
        }),
        catchError(err => {
          console.log(err)
          this.message = err.error.message;
          return throwError(() => err);

        })
      ).subscribe();
    };
  }

  newUser() {
    const user = {
      userId: "",
      username: "",
      name: "",
      lastname: "",
      email: "",
      dateOfBirth: "",
      day: "",
      month: "",
      year: "",
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

}
