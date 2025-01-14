import {Component} from '@angular/core';
import {SidebarComponent} from "../../shared/components/sidebar/sidebar.component";
import {Router} from '@angular/router';
import {TableComponent} from '../../shared/components/table/table.component';
import {ButtonComponent} from '../../shared/components/button/button.component';
import {FormsModule, NgForm, ReactiveFormsModule} from '@angular/forms';
import {NgForOf, NgIf} from '@angular/common';
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
    role: string[],
    status: boolean
  }[] = [];

  private message: string = '';
  add: boolean = false;

  constructor(protected router: Router, private userService: UserService) {
    this.userService.getUsers().subscribe(
      users => {
        this.users = users;
        this.usersBackup = users;
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
}
