import {Component, OnInit} from '@angular/core';
import {AsyncPipe, NgClass, NgOptimizedImage} from '@angular/common';
import {StockService} from '../../../stock/services/stock.service';
import {Router} from '@angular/router';
import {AuthService} from '../../../core/auth/auth.service';
import {User} from '../../../models/user.model';
import {Menubar} from 'primeng/menubar';
import {Avatar} from 'primeng/avatar';
import {MenuItem} from 'primeng/api';
import {Badge} from 'primeng/badge';
import {UtilsService} from '../../../core/service/utils.service';
import {PrimeBreadcrumbComponent} from '../prime-breadcrumb/prime-breadcrumb.component';
import {SharedState} from '../../../core/service/shared-state';

@Component({
    selector: 'app-header',
    standalone: true,
    imports: [
        AsyncPipe,
        NgOptimizedImage,
        Menubar,
        Avatar,
        Badge,
        PrimeBreadcrumbComponent,
        NgClass
    ],
    templateUrl: './header.component.html',
    styleUrl: './header.component.scss'
})
export class HeaderComponent implements OnInit {
    user: User | null = null;
    menuOpen = false; // Controle para o menu
    options: MenuItem[] | undefined;

    constructor(private estoqueService: StockService, protected authService: AuthService, private router: Router,
                private utils: UtilsService) {
        if (typeof window !== 'undefined' && window.localStorage) {
            const storedUser = window.localStorage.getItem('user');
            if (storedUser) {
                this.user = JSON.parse(storedUser); // Converte de volta para o objeto `User`
            }
        }
    }

    ngOnInit() {

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

    protected readonly SharedState = SharedState;
}
