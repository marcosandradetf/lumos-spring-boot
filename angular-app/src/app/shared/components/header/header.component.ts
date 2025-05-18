import {Component, OnInit} from '@angular/core';
import {AsyncPipe, NgForOf, NgIf, NgOptimizedImage} from '@angular/common';
import {EstoqueService} from '../../../stock/services/estoque.service';
import {Router} from '@angular/router';
import {AuthService} from '../../../core/auth/auth.service';
import {User} from '../../../models/user.model';
import {Menubar} from 'primeng/menubar';
import {Avatar} from 'primeng/avatar';
import {MenuItem} from 'primeng/api';
import {Badge} from 'primeng/badge';
import {Ripple} from 'primeng/ripple';
import {Menu} from 'primeng/menu';
import {UtilsService} from '../../../core/service/utils.service';
import {Divider} from 'primeng/divider';
import {StyleClass} from 'primeng/styleclass';
import {Popover} from 'primeng/popover';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [
    AsyncPipe,
    NgOptimizedImage,
    Menubar,
    Avatar,
    Badge,
    NgIf,
    Ripple,
    Menu,
    NgForOf,
    Divider,
    StyleClass,
    Popover
  ],
  templateUrl: './header.component.html',
  styleUrl: './header.component.scss'
})
export class HeaderComponent implements OnInit {
  user: User | null = null;
  menuOpen = false; // Controle para o menu
  items: MenuItem[] | undefined;
  options: MenuItem[] | undefined;

  constructor(private estoqueService: EstoqueService, protected authService: AuthService, private router: Router,
              private utils: UtilsService) {
    if (typeof window !== 'undefined' && window.localStorage) {
      const storedUser = window.localStorage.getItem('user');
      if (storedUser) {
        this.user = JSON.parse(storedUser); // Converte de volta para o objeto `User`
      }
    }
  }

  ngOnInit() {
    this.items = [
      {
        label: 'Home',
        icon: 'pi pi-home',
      },
      {
        label: 'Projects',
        icon: 'pi pi-search',
        badge: '3',
        items: [
          {
            label: 'Core',
            icon: 'pi pi-bolt',
            shortcut: '⌘+S',
          },
          {
            label: 'Blocks',
            icon: 'pi pi-server',
            shortcut: '⌘+B',
          },
          {
            separator: true,
          },
          {
            label: 'UI Kit',
            icon: 'pi pi-pencil',
            shortcut: '⌘+U',
          },
        ],
      },
    ];

    this.options = [
      {
        label: 'Perfil',
        icon: 'pi pi-search',
        badge: '3',
        items: [
          {
            label: 'Configurações',
            icon: 'pi pi-cog',
            command: () => {
              void this.router.navigate(['/configuracoes/conta']);
            }
          },
          {
            label: 'Sair',
            icon: 'pi pi-sign-out',
            command: () => {
              this.logout();
            }
          },
          {
            separator: true,
          },
        ],
      },
    ];

    let savedMenuState = localStorage.getItem('menuOpen');
    if (savedMenuState !== null) {
      this.menuOpen = JSON.parse(savedMenuState); // Converte de volta para booleano
    }

  }

  logout() {
    this.authService.logout().subscribe();
  }

  toggleMenu() {
    this.menuOpen = !this.menuOpen;
    this.utils.toggleMenu(this.menuOpen);
    localStorage.setItem('menuOpen', JSON.stringify(this.menuOpen));
  }

}
