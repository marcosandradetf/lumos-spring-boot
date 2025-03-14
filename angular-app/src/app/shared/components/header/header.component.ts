import {Component, OnInit} from '@angular/core';
import {AsyncPipe, NgClass, NgIf, NgOptimizedImage} from '@angular/common';
import {catchError, of, tap} from 'rxjs';
import {EstoqueService} from '../../../stock/services/estoque.service';
import {Router, RouterLink, RouterLinkActive} from '@angular/router';
import {AuthService} from '../../../core/auth/auth.service';
import {User} from '../../../models/user.model';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [
    RouterLink,
    RouterLinkActive,
    AsyncPipe,
    NgOptimizedImage
  ],
  templateUrl: './header.component.html',
  styleUrl: './header.component.scss'
})
export class HeaderComponent implements OnInit{
  user: User | null = null;
  navOpen = false; // Controle para o menu
  accountMenuOpen = false; // Controle para o menu da conta
  onPath: string = ''; // Caminho atual

  constructor(private estoqueService: EstoqueService, protected authService: AuthService, private router: Router) {
    if (typeof window !== 'undefined' && window.localStorage) {
      const storedUser = window.localStorage.getItem('user');
      if (storedUser) {
        this.user = JSON.parse(storedUser); // Converte de volta para o objeto `User`
      }
    }
  }

  ngOnInit(): void {
    this.estoqueService.onPath$.subscribe(path => {
      this.onPath = path; // Atualiza o caminho atual com base nas mudanças
    });
  }

  toggleNav() {
    this.navOpen = !this.navOpen;
  }

  toggleAccountMenu() {
    this.accountMenuOpen = !this.accountMenuOpen;
  }

  closeAccountMenu() {
    this.accountMenuOpen = false;
  }

  logout() {
    this.authService.logout().subscribe();
  }
}
