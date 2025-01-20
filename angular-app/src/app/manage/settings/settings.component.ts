import { Component } from '@angular/core';
import {SidebarComponent} from "../../shared/components/sidebar/sidebar.component";
import {Router} from '@angular/router';
import {UserService} from '../user/user-service.service';
import {UtilsService} from '../../core/service/utils.service';
import {AuthService} from '../../core/auth/auth.service';
import {Title} from '@angular/platform-browser';

@Component({
  selector: 'app-settings',
  standalone: true,
    imports: [
        SidebarComponent
    ],
  templateUrl: './settings.component.html',
  styleUrl: './settings.component.scss'
})
export class SettingsComponent {
  sidebarLinks = [
    {title: 'Início', path: '/configuracoes/dashboard', id: 'opt1'},
    {title: 'Usuários', path: '/configuracoes/usuarios', id: 'opt2'},
    {title: 'Equipes', path: '/configuracoes/equipes', id: 'opt3'},
    {title: 'Minha Empresa', path: '/configuracoes/empresa', id: 'opt4'},
  ];

  constructor(protected router: Router, private userService: UserService, protected utils: UtilsService,
              protected authService: AuthService, private titleService: Title) {
    this.titleService.setTitle("Configurações - Dashboard");
  }

}
