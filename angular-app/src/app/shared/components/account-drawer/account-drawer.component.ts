import {Component} from '@angular/core';
import {Drawer} from 'primeng/drawer';
import {SharedState} from '../../../core/service/shared-state';
import {AsyncPipe, NgForOf, NgIf, NgOptimizedImage} from '@angular/common';
import {SidebarComponent} from '../sidebar/sidebar.component';
import {Router} from '@angular/router';
import {Avatar} from 'primeng/avatar';
import {Badge} from 'primeng/badge';
import {Menu} from 'primeng/menu';
import {Ripple} from 'primeng/ripple';
import {MenuItem} from 'primeng/api';
import {AuthService} from '../../../core/auth/auth.service';

@Component({
    selector: 'app-account-drawer',
    standalone: true,
    imports: [
        Drawer,
        AsyncPipe,
        SidebarComponent,
        NgOptimizedImage,
        Avatar,
        Badge,
        Menu,
        NgForOf,
        NgIf,
        Ripple
    ],
    templateUrl: './account-drawer.component.html',
    styleUrl: './account-drawer.component.scss'
})
export class AccountDrawerComponent {
    options: MenuItem[] = [
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

    protected readonly SharedState = SharedState;

    constructor(protected router: Router, protected authService: AuthService,) {

    }

    onDrawerChange(open: boolean) {
        SharedState.showAccountDrawer$.next(open);
    }

    logout() {
        this.authService.logout().subscribe();
        SharedState.showAccountDrawer$.next(false);
    }

    navigate(path: string) {
        void this.router.navigate([path]);
        SharedState.showAccountDrawer$.next(false);
    }
}
