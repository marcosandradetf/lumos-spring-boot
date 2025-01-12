import { Component } from '@angular/core';
import {SidebarComponent} from "../../shared/components/sidebar/sidebar.component";

@Component({
  selector: 'app-user',
  standalone: true,
    imports: [
        SidebarComponent
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

}
