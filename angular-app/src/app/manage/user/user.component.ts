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
    DatePipe
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
  }[] = [];

  usersBackup: {
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

  constructor(protected router: Router, private userService: UserService) {
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
        this.usersBackup = users;
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
      status: true
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
      roles.push({ userId: id, role: nomeRole });
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

}
