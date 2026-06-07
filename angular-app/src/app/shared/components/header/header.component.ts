import {Component, ElementRef, HostListener, OnInit, ViewChild} from '@angular/core';
import {AsyncPipe, NgClass, NgForOf, NgIf, NgOptimizedImage} from '@angular/common';
import {AuthService} from '../../../core/auth/auth.service';
import {User} from '../../models/user.model';
import {Menubar} from 'primeng/menubar';
import {Avatar} from 'primeng/avatar';
import {MenuItem, MessageService} from 'primeng/api';
import {UtilsService} from '../../../core/service/utils.service';
import {PrimeBreadcrumbComponent} from '../prime-breadcrumb/prime-breadcrumb.component';
import {SharedState} from '../../../core/service/shared-state';
import {FcmService} from '../../../core/service/fcm.service';
import {Divider} from 'primeng/divider';
import {NavigationService} from '../../service/navigation.service';
import { Router, NavigationEnd, ActivatedRoute } from '@angular/router';
import { filter, map, mergeMap } from 'rxjs/operators';
import {SearchService} from '../../service/search.service';

@Component({
    selector: 'app-header',
    standalone: true,
    imports: [
        AsyncPipe,
        NgOptimizedImage,
        Menubar,
        Avatar,
        PrimeBreadcrumbComponent,
        NgClass,
        NgIf,
        Divider,
        NgForOf
    ],
    templateUrl: './header.component.html',
    styleUrl: './header.component.scss'
})
export class HeaderComponent implements OnInit {
    @ViewChild('searchInput') searchInput!: ElementRef<HTMLInputElement>;

    hideBackButton = false;
    searchQuery = '';
    searchResults: any[] = [];
    selectedIndex = -1;

    user: User | null = null;
    menuOpen = false; // Controle para o menu
    options: MenuItem[] | undefined;
    notificationStatus = 'default';
    notificationCount = 0;
    isApple = false;

    constructor(protected authService: AuthService,
                protected router: Router,
                private utils: UtilsService,
                private messageService: MessageService,
                private fcmService: FcmService,
                private navService: NavigationService,
                private activatedRoute: ActivatedRoute,
                private searchService: SearchService
    ) {
        if (typeof window !== 'undefined' && window.localStorage) {
            const storedUser = window.localStorage.getItem('user');
            if (storedUser) {
                this.user = JSON.parse(storedUser); // Converte de volta para o objeto `User`
            }

            const ua = navigator.userAgent;
            this.isApple = /iPhone|iPad|iPod|Macintosh/i.test(ua);
        }

        this.router.events.pipe(
            // 1. Filtra apenas quando a navegação termina com sucesso
            filter(event => event instanceof NavigationEnd),
            // 2. Começa a busca pela rota raiz
            map(() => this.activatedRoute),
            // 3. Percorre os filhos até chegar na rota ativa (a última na árvore)
            map(route => {
                while (route.firstChild) {
                    route = route.firstChild;
                }
                return route;
            }),
            // 4. Garante que estamos pegando dados da rota ativa
            filter(route => route.outlet === 'primary'),
            mergeMap(route => route.data)
        ).subscribe(data => {
            // 5. Aqui você acessa a propriedade que definiu no Routes
            this.hideBackButton = !!data['hideBackButton'];
        });

    }

    ngOnInit() {
        this.utils.menuState$.subscribe((isOpen: boolean) => {
            this.menuOpen = isOpen;
        });
        this.fcmService.notificationStatus$.subscribe(s => this.notificationStatus = s);
        this.fcmService.notifications$.subscribe(c => this.notificationCount = c);

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

    protected showBlockedNotificationInfo() {
        this.messageService.add({
            key: 'notifications',
            severity: 'warn',
            summary: 'Notificações bloqueadas',
            detail: 'Você bloqueou notificações do navegador. Para não perder nenhuma atualização importante, é essencial permitir as notificações do Lumos no seu dispositivo.',
            life: 25000,
            data: {
                buttonClick: {
                    acceptDescription: 'Como ativar',
                    declineDescription: 'Ignorar',
                    clickAction: 'guide-notification'
                },
                type: 'NOT_BELL'
            }
        });
    }

    goBack() {
        this.navService.pop();
    }


    @ViewChild('spotlightInput') spotlightInput!: ElementRef<HTMLInputElement>;
    isSpotlightOpen = false;

// Abre o Spotlight e foca o input após o Angular renderizar
    openSpotlight() {
        this.isSpotlightOpen = true;
        this.searchResults = []; // Limpa resultados anteriores
        setTimeout(() => {
            this.spotlightInput?.nativeElement.focus();
        }, 10);
    }

    closeSpotlight() {
        this.isSpotlightOpen = false;
        this.searchQuery = '';
        this.searchResults = [];
    }

    // Escuta a tecla ESC para fechar
    @HostListener('window:keydown.escape')
    onEsc() {
        this.closeSpotlight();
    }

    // Atualiza o atalho de teclado para abrir o Spotlight
    @HostListener('window:keydown.meta.k', ['$event'])
    @HostListener('window:keydown.ctrl.k', ['$event'])
    onShortcut(event: KeyboardEvent) {
        event.preventDefault();
        this.openSpotlight();
    }

    // No selectResult, lembre-se de fechar o spotlight
    selectResult(res: any) {
        void this.router.navigate([res.path], { queryParams: res.queryParams });
        this.closeSpotlight();
    }

    onSearch(event: any) {
        this.searchQuery = event.target.value;
        this.searchResults = this.searchService.searchRoutes(this.searchQuery);
        this.selectedIndex = this.searchResults.length > 0 ? 0 : -1; // Foca o primeiro por padrão
    }

    @HostListener('window:keydown', ['$event'])
    handleKeyboardEvents(event: KeyboardEvent) {
        if (!this.isSpotlightOpen) return;

        if (event.key === 'ArrowDown') {
            event.preventDefault();
            this.selectedIndex = (this.selectedIndex + 1) % this.searchResults.length;
        }
        else if (event.key === 'ArrowUp') {
            event.preventDefault();
            this.selectedIndex = (this.selectedIndex - 1 + this.searchResults.length) % this.searchResults.length;
        }
        else if (event.key === 'Enter') {
            event.preventDefault();
            if (this.selectedIndex !== -1 && this.searchResults[this.selectedIndex]) {
                this.selectResult(this.searchResults[this.selectedIndex]);
            }
        }

        setTimeout(() => {
            const activeElement = document.querySelector('.bg-indigo-50');
            activeElement?.scrollIntoView({ block: 'nearest', behavior: 'smooth' });
        });
    }

}
