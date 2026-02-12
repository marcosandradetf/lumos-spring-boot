import {Component, OnInit} from '@angular/core';
import {NavigationEnd, Router, RouterOutlet} from '@angular/router';
import {HeaderComponent} from './shared/components/header/header.component';
import {FooterComponent} from './shared/components/footer/footer.component';
import {AuthService} from './core/auth/auth.service';
import {AsyncPipe, NgClass, NgIf} from '@angular/common';
import {SidebarComponent} from './shared/components/sidebar/sidebar.component';
import {filter} from 'rxjs';
import {UtilsService} from './core/service/utils.service';
import {SidebarDrawerComponent} from './shared/components/sidebar-drawer/sidebar-drawer.component';
import {SharedState} from './core/service/shared-state';
import {NotificationDrawerComponent} from './shared/components/notification-drawer/notification-drawer.component';
import {AccountDrawerComponent} from './shared/components/account-drawer/account-drawer.component';

@Component({
    selector: 'app-root',
    standalone: true,
    imports: [RouterOutlet, HeaderComponent, FooterComponent, AsyncPipe, SidebarComponent, NgClass, NgIf, SidebarDrawerComponent, NotificationDrawerComponent, AccountDrawerComponent],
    templateUrl: './app.component.html',
    styleUrl: './app.component.scss',
})
export class AppComponent implements OnInit {
    currentUrl: string = '';

    constructor(public authService: AuthService, private router: Router, private utils: UtilsService) {
        this.router.events.pipe(
            filter(event => event instanceof NavigationEnd)
        ).subscribe((event: NavigationEnd) => {
            this.currentUrl = event.urlAfterRedirects;
        });
    }


    menuOpen = false;  // Definir o estado do menu no componente pai

    ngOnInit(): void {
        this.utils.menuState$.subscribe((isOpen: boolean) => {
            this.menuOpen = isOpen;
        });
        let savedMenuState = localStorage.getItem('menuOpen');
        if (savedMenuState !== null) {
            this.menuOpen = JSON.parse(savedMenuState); // Converte de volta para booleano
        }

        const saved = localStorage.getItem('sidebarWidth');
        if (saved) this.sidebarWidth = +saved;
    }

    protected readonly SharedState = SharedState;

    sidebarWidth = 400;

    private resizing = false;

    startResizing(event: MouseEvent) {

        this.resizing = true;

        const startX = event.clientX;
        const startWidth = this.sidebarWidth;

        const mouseMove = (moveEvent: MouseEvent) => {

            if (!this.resizing) return;

            const newWidth = startWidth + (moveEvent.clientX - startX);

            // limites profissionais
            if (newWidth >= 280 && newWidth <= 400) {
                this.sidebarWidth = newWidth;
            }
        };

        const mouseUp = () => {
            this.resizing = false;
            document.body.style.cursor = 'default';
            window.removeEventListener('mousemove', mouseMove);
            window.removeEventListener('mouseup', mouseUp);

            // opcional: salvar tamanho
            localStorage.setItem('sidebarWidth', this.sidebarWidth.toString());
        };

        document.body.style.cursor = 'col-resize';

        window.addEventListener('mousemove', mouseMove);
        window.addEventListener('mouseup', mouseUp);
    }

}
