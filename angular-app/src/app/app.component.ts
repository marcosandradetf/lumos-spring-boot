import {Component, OnInit} from '@angular/core';
import { RouterOutlet } from '@angular/router';
import {HeaderComponent} from './shared/components/header/header.component';
import {FooterComponent} from './shared/components/footer/footer.component';
import {AuthService} from './core/auth/auth.service';
import {AsyncPipe, NgClass} from '@angular/common';
import {SidebarComponent} from './shared/components/sidebar/sidebar.component';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, HeaderComponent, FooterComponent, AsyncPipe, SidebarComponent, NgClass],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss',
})
export class AppComponent implements OnInit {
  constructor(public authService: AuthService) {
  }

  menuOpen = false;  // Definir o estado do menu no componente pai

  onMenuToggle(menuState: boolean) {
    this.menuOpen = menuState;  // Atualizar o estado do menu
  }

  ngOnInit(): void {
    // Verifica se existe algum valor salvo no localStorage
    const savedMenuState = localStorage.getItem('menuOpen');
    if (savedMenuState !== null) {
      this.menuOpen = JSON.parse(savedMenuState); // Converte de volta para booleano
    }
  }


}
