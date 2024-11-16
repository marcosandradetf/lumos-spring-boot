import {Component, OnInit} from '@angular/core';
import {AsyncPipe, NgClass, NgIf, NgOptimizedImage} from '@angular/common';
import {Type} from '../../../core/models/tipo.model';
import {Company} from '../../../core/models/empresa.model';
import {Deposit} from '../../../core/models/almoxarifado.model';
import {BehaviorSubject, catchError, of, tap} from 'rxjs';
import {EstoqueService} from '../../../features/estoque/services/estoque.service';
import {Router, RouterLink, RouterLinkActive} from '@angular/router';
import {AuthService} from '../../../core/auth/auth.service';
import {User} from '../../../core/models/user.model';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [
    NgClass,
    NgIf,
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
    if (typeof window !== 'undefined' && localStorage) {
      const storedUser = localStorage.getItem('user');
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
    this.authService.logout().pipe(
      tap(response => {
        console.log('logout');
        this.router.navigate(['/auth/login']);
      }),
      catchError(err => {
        console.log(err);
        return of(null);
      })
    ).subscribe();
  }
}
