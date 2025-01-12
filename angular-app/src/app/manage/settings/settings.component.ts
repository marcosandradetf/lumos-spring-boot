import { Component } from '@angular/core';
import {SidebarComponent} from "../../shared/components/sidebar/sidebar.component";

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
    { title: 'Início', path: '/configuracoes', id: 'opt1' },
    { title: 'Usuários', path: '/configuracoes/usuarios', id: 'opt2' },
    { title: 'Equipes', path: '/configuracoes/equipes', id: 'opt3' },
    { title: 'Minha Empresa', path: '/configuracoes/empresa', id: 'opt4' },
  ];

}
