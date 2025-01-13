import { Component } from '@angular/core';
import {SidebarComponent} from "../../shared/components/sidebar/sidebar.component";
import {Router} from '@angular/router';
import {TableComponent} from '../../shared/components/table/table.component';
import {ButtonComponent} from '../../shared/components/button/button.component';
import {FormsModule, NgForm, ReactiveFormsModule} from '@angular/forms';
import {NgForOf} from '@angular/common';

@Component({
  selector: 'app-user',
  standalone: true,
  imports: [
    SidebarComponent,
    TableComponent,
    ButtonComponent,
    FormsModule,
    ReactiveFormsModule,
    NgForOf
  ],
  templateUrl: './user.component.html',
  styleUrl: './user.component.scss'
})
export class UserComponent {
  sidebarLinks = [
    { title: 'Início', path: '/configuracoes', id: 'opt1' },
    { title: 'Usuários', path: '/configuracoes/usuarios', id: 'opt2' },
    { title: 'Equipes', path: '/configuracoes/equipes', id: 'opt3' },
    { title: 'Minha Empresa', path: '/configuracoes/empresa', id: 'opt4' },
  ];

  change: boolean = false;
  users: {
    userId: string,
    username: string,
    name: string,
    lastname: string,
    email: string,
    dateOfBirth: string,
    role: string,
    status: boolean
  }[] = []

  constructor(protected router: Router) {
  }


  changeUser() {
    return () => {
      this.change = true;
    };
  }

  submitUsers(usersForm: NgForm) {

  }
}
