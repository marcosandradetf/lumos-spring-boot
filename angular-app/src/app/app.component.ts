import {Component, OnInit} from '@angular/core';
import {Router, RouterOutlet} from '@angular/router';
import {HeaderComponent} from './shared/components/header/header.component';
import {AuthService} from './core/auth/auth.service';
import {AsyncPipe, NgClass, NgIf} from '@angular/common';
import {SidebarComponent} from './shared/components/sidebar/sidebar.component';
import {Observable} from 'rxjs';
import {UtilsService} from './core/service/utils.service';
import {SidebarDrawerComponent} from './shared/components/sidebar-drawer/sidebar-drawer.component';
import {SharedState} from './core/service/shared-state';
import {NotificationDrawerComponent} from './shared/components/notification-drawer/notification-drawer.component';
import {AccountDrawerComponent} from './shared/components/account-drawer/account-drawer.component';
import {FcmService} from './core/service/fcm.service';
import {Toast} from 'primeng/toast';
import {NotificationPopupComponent} from './shared/components/notification-popup/notification-popup.component';
import {DialogService} from 'primeng/dynamicdialog';
import {MessageService, PrimeTemplate} from 'primeng/api';
import {forkJoin} from 'rxjs';
import {ContractService} from './contract/services/contract.service';
import {UserService} from './manage/user/user-service.service';
import {TeamService} from './manage/team/team-service.service';
import {StockService} from './stock/services/stock.service';
import {MaterialService} from './stock/services/material.service';
import {Utils} from './core/service/utils';
import {DomSanitizer, SafeResourceUrl} from '@angular/platform-browser';
import {ShareModalComponent} from './shared/components/share-modal/share-modal.component';
import {Button} from 'primeng/button';

@Component({
    selector: 'app-root',
    standalone: true,
    imports: [
        RouterOutlet, HeaderComponent, AsyncPipe, SidebarComponent, NgClass, NgIf, SidebarDrawerComponent, NotificationDrawerComponent, AccountDrawerComponent, Toast, PrimeTemplate, ShareModalComponent, Button],
    templateUrl: './app.component.html',
    styleUrl: './app.component.scss',
})
export class AppComponent implements OnInit {
    logoutLoading$: Observable<boolean>;
    loading = false;
    embeddedDocOpen = false;
    embeddedDocTitle = '';
    embeddedDocDescription = '';
    embeddedDocUrl: SafeResourceUrl | null = null;
    readonly notificationGuideUrl = 'https://lumosip.com.br/como-usar/15-web-config/01-enable-notifications/';
    notificationStatus: 'granted' | 'denied' | 'default' = 'default';

    constructor(
        public authService: AuthService,
        private router: Router,
        private utils: UtilsService,
        private fcmService: FcmService,
        protected messageService: MessageService,
        private dialogService: DialogService,
        private contractService: ContractService,
        private userService: UserService,
        private teamService: TeamService,
        private stockService: StockService,
        private materialService: MaterialService,
        private sanitizer: DomSanitizer,
    ) {

        this.logoutLoading$ = this.authService.isLoading$;
        this.fcmService.notificationStatus$.subscribe(s => this.notificationStatus = s);
    }


    menuOpen = false;  // Definir o estado do menu no componente pai

    async ngOnInit() {
        this.utils.menuState$.subscribe((isOpen: boolean) => {
            this.menuOpen = isOpen;
        });
        let savedMenuState = localStorage.getItem('menuOpen');
        if (savedMenuState !== null) {
            this.menuOpen = JSON.parse(savedMenuState); // Converte de volta para booleano
        }

        const saved = localStorage.getItem('sidebarWidth');
        if (saved) this.sidebarWidth = +saved;

        const userStorage = localStorage.getItem('user');
        if (!userStorage) return;

        if (!localStorage.getItem('onboarding')) {
            this.checkState();
        }

        if (this.notificationStatus === 'granted' && !localStorage.getItem('fcmToken')) {
            void this.fcmService.getPermission(this.authService.getUser().getRoles());
        } else if (this.notificationStatus === 'default') {
            setTimeout(() => {
                this.messageService.add({
                    key: 'notifications',
                    severity: 'info',
                    summary: 'Notificações desativadas',
                    detail: 'Para não perder nenhuma atualização importante, é essencial permitir as notificações do Lumos no seu dispositivo.',
                    life: 50000,
                    data: {
                        buttonClick: {
                            acceptDescription: 'Ativar agora',
                            declineDescription: 'Ignorar',
                            clickAction: 'enable-notification'
                        },
                        type: 'BELL'
                    }
                });
            }, 0);
        } else if (this.notificationStatus === 'denied') {
            setTimeout(() => {
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
            }, 0);
        }

        this.fcmService.initListen();
        const bannerData = await this.fcmService.getBanner("ALERT_BANNER");
        if (bannerData) {
            this.utils.playSound('open');
            this.dialogService.open(NotificationPopupComponent, {
                data: bannerData,
                width: '720px',
                breakpoints: {
                    '768px': '95vw',
                    '480px': '100vw'
                },
                showHeader: false,
                modal: true,
                dismissableMask: true,
                baseZIndex: 10000,
                styleClass: 'custom-corporate-dialog',
                contentStyle: {
                    padding: '0',
                    background: 'transparent',
                    overflow: 'visible'
                }
            });
        }
    }

    protected readonly SharedState = SharedState;

    sidebarWidth = 350;

    private resizing = false;

    startResizing(event: MouseEvent) {

        this.resizing = true;

        const startX = event.clientX;
        const startWidth = this.sidebarWidth;

        const mouseMove = (moveEvent: MouseEvent) => {

            if (!this.resizing) return;

            const newWidth = startWidth + (moveEvent.clientX - startX);

            // limites profissionais
            if (newWidth >= 250 && newWidth <= 350) {
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

    getIcon(type: string): string {
        switch (type) {
            case 'ALERT':
                return 'pi pi-exclamation-triangle text-orange-500';
            case 'ERROR':
                return 'pi pi-times-circle text-red-500';
            case 'SUCCESS':
                return 'pi pi-check-circle text-green-500';
            case 'BELL':
                return 'pi pi-bell text-blue-500';
            case 'NOT_BELL':
                return 'pi pi-bell-slash text-orange-500';
            default:
                return 'pi pi-info-circle text-blue-500';
        }
    }

    onToastClick(notification: any) {
        this.messageService.clear("notifications");
        if (notification.uri) {
            void this.router.navigate(notification.uri);
        } else if (notification.openGuide === 'notification') {
            this.openNotificationGuide();
        }
    }

    async onButtonClick(actionDescription: string) {
        this.messageService.clear("notifications");
        if (actionDescription === 'guide-notification') {
            this.openNotificationGuide();
        } else if (actionDescription === 'enable-notification') {
            await this.fcmService.getPermission(this.authService.getUser().getRoles());
        }
    }

    checkState() {
        this.loading = true;
        forkJoin({
            referenceContractItems: this.contractService.getContractReferenceItems(),
            contracts: this.contractService.getAllContracts(
                {
                    contractor: null,
                    startDate: new Date(new Date().setMonth(new Date().getMonth() - 6)),
                    endDate: new Date(),
                    status: null,
                }
            ),
            users: this.userService.getUsers(),
            teams: this.teamService.getTeams(),
            stockists: this.stockService.getStockists(),
            deposits: this.stockService.getDeposits(),
            materials: this.materialService.getCatalogue(),
        }).subscribe({
            next: ({referenceContractItems, contracts, users, teams, stockists, deposits, materials}) => {
                const a = referenceContractItems.length;
                const b = contracts.length;
                const c = users.length;
                const d = users.filter(user => {
                    const roles = user.role.map(r => r.roleName);
                    return roles.includes('ELETRICISTA') || roles.includes('MOTORISTA')
                }).length;

                const e = teams.length;
                const f = stockists.length;
                const g = deposits.filter(deposit => !deposit.isTruck).length;
                const h = deposits.filter(deposit => deposit.isTruck).length;
                const i = materials.length;

                if (a === 0
                    || b === 0
                    || c === 0
                    || d === 0
                    || e === 0
                    || f === 0
                    || g === 0
                    || h === 0
                    || i === 0
                ) {
                    void this.router.navigate(['/configuracoes/onboarding']);
                } else {
                    localStorage.setItem('onboarding', 'finished');
                    this.utils.setOnboarding(false);
                }
                this.loading = false;
            },
            error: (err) => {
                Utils.handleHttpError(err, this.router);
                this.loading = false;
            }
        });
    }

    openNotificationGuide() {
        this.embeddedDocTitle = 'Ativando as Notificações';
        this.embeddedDocDescription = 'Guia para permitir notificações no navegador e receber alertas operacionais do Lumos.';
        this.embeddedDocUrl = this.sanitizer.bypassSecurityTrustResourceUrl(this.notificationGuideUrl);
        this.embeddedDocOpen = true;
    }

    closeEmbeddedDoc() {
        this.embeddedDocOpen = false;
        this.embeddedDocTitle = '';
        this.embeddedDocDescription = '';
        this.embeddedDocUrl = null;
    }

}
