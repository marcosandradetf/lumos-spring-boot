import { Component } from '@angular/core';
import {NgClass, NgIf} from '@angular/common';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [
    NgClass,
    NgIf
  ],
  templateUrl: './header.component.html',
  styleUrl: './header.component.scss'
})
export class HeaderComponent {
  navOpen = false; // Controle para o menu
  accountMenuOpen = false; // Controle para o menu da conta

  toggleNav() {
    this.navOpen = !this.navOpen;
  }

  toggleAccountMenu() {
    this.accountMenuOpen = !this.accountMenuOpen;
  }

  closeAccountMenu() {
    this.accountMenuOpen = false;
  }
}
