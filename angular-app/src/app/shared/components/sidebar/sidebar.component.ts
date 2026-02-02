import {Component, EventEmitter, Input, model, OnInit, Output} from '@angular/core';
import {AsyncPipe, NgIf, NgOptimizedImage} from '@angular/common';
import {Router} from '@angular/router';
import {PanelMenu} from 'primeng/panelmenu';
import {MenuItem} from 'primeng/api';
import {UtilsService} from '../../../core/service/utils.service';
import {SharedState} from '../../../core/service/shared-state';

@Component({
    selector: 'app-sidebar',
    standalone: true,
    imports: [
        PanelMenu,
        NgIf,
        NgOptimizedImage,
    ],
    templateUrl: './sidebar.component.html',
    styleUrls: ['./sidebar.component.scss']
})
export class SidebarComponent implements OnInit {
    @Input() title: string = '';
    @Input() links: { title: string; path: string; id: string }[] = [];
    @Output() menuToggle = new EventEmitter<boolean>();  // Emitir o estado do menu

    menuOpen = false;
    bTogglePreMeasurement = false;
    bToggleExecution = false;
    bToggleStock = true;
    bToggleRequest = false;
    bToggleSettings: boolean = true;
    bToggleContracts: boolean = true;
    bToggleReports: boolean = true;
    items: MenuItem[] | undefined;
    showDrawer = false;

    constructor(private utils: UtilsService, protected router: Router) {
    }

    ngOnInit(): void {
        const isMobile = window.innerWidth <= 1024;

        SharedState.showMenuDrawer$.subscribe((open) => {
            this.showDrawer = open;
        });

        this.utils.menuState$.subscribe((isOpen: boolean) => {
            this.menuOpen = isOpen;
        });

        // Verifica se existe algum valor salvo no localStorage
        let savedMenuState = localStorage.getItem('menuOpen');
        if (savedMenuState !== null && !isMobile) {
            this.menuOpen = JSON.parse(savedMenuState); // Converte de volta para booleano
        }

        savedMenuState = localStorage.getItem('toggleStock');
        if (savedMenuState !== null) {
            this.bToggleStock = JSON.parse(savedMenuState); // Converte de volta para booleano
        }
        savedMenuState = localStorage.getItem('togglePreMeasurement');
        if (savedMenuState !== null) {
            this.bTogglePreMeasurement = JSON.parse(savedMenuState); // Converte de volta para booleano
        }
        savedMenuState = localStorage.getItem('toggleExecution');
        if (savedMenuState !== null) {
            this.bToggleExecution = JSON.parse(savedMenuState); // Converte de volta para booleano
        }
        savedMenuState = localStorage.getItem('toggleRequest');
        if (savedMenuState !== null) {
            this.bToggleRequest = JSON.parse(savedMenuState); // Converte de volta para booleano
        }
        savedMenuState = localStorage.getItem('toggleSettings');
        if (savedMenuState !== null) {
            this.bToggleSettings = JSON.parse(savedMenuState); // Converte de volta para booleano
        }

        savedMenuState = localStorage.getItem('toggleContracts');
        if (savedMenuState !== null) this.bToggleContracts = JSON.parse(savedMenuState);

        savedMenuState = localStorage.getItem('toggleport');
        if (savedMenuState !== null) this.bToggleReports = JSON.parse(savedMenuState);


        this.items = [
            {
                style: {
                    border: ''
                },
                label: 'Pré-Medições',
                expanded: this.bTogglePreMeasurement,
                command: () => {
                    this.togglePreMeasurement(!this.bTogglePreMeasurement);
                },
                items: [
                    {
                        label: 'Aguardando Análise',
                        icon: 'pi pi-inbox text-neutral-800 dark:text-neutral-200',
                        routerLink: 'pre-medicao/pendente',
                        command: () => {
                            SharedState.showMenuDrawer$.next(false);
                        }
                    },
                    {
                        label: 'Disponível para execução',
                        icon: 'pi pi-check-circle text-neutral-800 dark:text-neutral-200',
                        routerLink: 'pre-medicao/disponivel',
                        command: () => {
                            SharedState.showMenuDrawer$.next(false);
                        }
                    },
                    {
                        label: 'Importar pré-medição (.xlx)',
                        icon: 'pi pi-file-excel text-neutral-800 dark:text-neutral-200',
                        routerLink: ['/contratos/listar'],
                        queryParams: {for: 'preMeasurement'},
                        command: () => {
                            SharedState.showMenuDrawer$.next(false);
                        }
                    },
                ]
            },
            {
                style: {
                    border: ''
                },
                label: 'Execuções',
                expanded: this.bToggleExecution,
                command: () => {
                    this.toggleExecution();
                },
                items: [
                    {
                        label: 'Execução Sem Pré-Medição',
                        icon: 'pi pi-lightbulb text-neutral-800 dark:text-neutral-200',
                        routerLink: ['/contratos/listar'],
                        queryParams: {for: 'execution'},
                        routerLinkActiveOptions: {exact: true},
                        command: () => {
                            SharedState.showMenuDrawer$.next(false);
                        }
                    },
                    {
                        label: 'Em progresso',
                        icon: 'pi pi-spinner  text-neutral-800 dark:text-neutral-200',
                        routerLink: ['/execucoes/em-progresso'],
                        routerLinkActiveOptions: {exact: true},
                        command: () => {
                            SharedState.showMenuDrawer$.next(false);
                        }
                    },
                    {
                        label: 'Finalizada',
                        icon: 'pi pi-check text-neutral-800 dark:text-neutral-200',
                        routerLink: ['/execucoes/finalizadas'],
                        command: () => {
                            SharedState.showMenuDrawer$.next(false);
                        }
                    },
                ]
            },
            {
                style: {
                    border: ''
                },
                label: 'Solicitações ao Estoquista',
                expanded: this.bToggleRequest,
                command: () => {
                    this.toggleRequest();
                },
                items: [
                    {
                        label: 'Gerenciamento de Estoque – Pré-instalação',
                        icon: 'pi pi-box text-black text-neutral-800 dark:text-neutral-200',
                        routerLink: ['/requisicoes/instalacoes/gerenciamento-estoque'],
                        command: () => {
                            SharedState.showMenuDrawer$.next(false);
                        }
                    },
                    {
                        label: 'Materiais pendentes de Aprovação',
                        icon: 'fa-solid fa-hourglass-half text-base text-neutral-800 dark:text-neutral-200',
                        routerLink: ['/requisicoes'],
                        queryParams: {status: 'PENDING'},
                        command: () => {
                            SharedState.showMenuDrawer$.next(false);
                        }
                    },
                    {
                        label: 'Materiais disponíveis para Coleta',
                        icon: 'fa-solid fa-list-check text-base text-neutral-800 dark:text-neutral-200',
                        routerLink: ['/requisicoes'],
                        queryParams: {status: 'APPROVED'},
                        command: () => {
                            SharedState.showMenuDrawer$.next(false);
                        }
                    },
                ]
            },
            {
                style: {
                    border: ''
                },
                label: 'Execuções Realizadas',
                expanded: this.bToggleReports,
                command: () => {
                    this.toggleReports(!this.bToggleReports);
                },
                items: [
                    {
                        disabled: false,
                        label: 'Relatórios Personalizados',
                        icon: 'pi pi-chart-bar text-black text-neutral-800 dark:text-neutral-200',
                        routerLink: ['/relatorios/gerenciamento'],
                        command: () => {
                            SharedState.showMenuDrawer$.next(false);
                        }
                    },
                    {
                        label: 'Manutenções (últimos 30 dias)',
                        icon: 'fa-solid fa-wrench text-base text-neutral-800 dark:text-neutral-200',
                        routerLink: ['/relatorios/manutencoes'],
                        command: () => {
                            SharedState.showMenuDrawer$.next(false);
                        }
                    },
                    {
                        label: 'Instalações (últimos 30 dias)',
                        icon: 'pi pi-lightbulb text-black text-neutral-800 dark:text-neutral-200',
                        routerLink: ['/relatorios/instalacoes'],
                        command: () => {
                            SharedState.showMenuDrawer$.next(false);
                        }
                    },
                ]
            },
            {
                style: {
                    border: ''
                },
                label: 'Contratos',
                expanded: this.bToggleContracts,
                command: () => {
                    this.toggleContracts(!this.bToggleContracts);
                },
                items: [
                    {
                        label: 'Novo contrato',
                        icon: 'pi pi-plus-circle text-neutral-800 dark:text-neutral-200',
                        routerLink: ['/contratos/criar'],
                        command: () => {
                            SharedState.showMenuDrawer$.next(false);
                        }
                    },
                    {
                        label: 'Exibir Contratos',
                        icon: 'pi pi-folder-open text-neutral-800 dark:text-neutral-200',
                        routerLink: ['/contratos/listar'],
                        queryParams: {for: 'view'},
                        command: () => {
                            SharedState.showMenuDrawer$.next(false);
                        }
                    },
                ]
            },
            {
                style: {
                    border: ''
                },
                label: 'Estoque',
                expanded: this.bToggleStock,
                command: () => {
                    this.toggleStock(!this.bToggleStock);
                },
                items: [
                    {
                        label: 'Movimentação de Estoque',
                        icon: 'pi pi-barcode text-neutral-800 dark:text-neutral-200',
                        routerLink: ['/estoque/movimentar-estoque'],
                        command: () => {
                            SharedState.showMenuDrawer$.next(false);
                        }
                    },
                    {
                        label: 'Movimentações Pendentes',
                        icon: 'pi pi-clock text-neutral-800 dark:text-neutral-200',
                        routerLink: ['/estoque/movimentar-estoque-pendente'],
                        command: () => {
                            SharedState.showMenuDrawer$.next(false);
                        }
                    },
                    {
                        label: 'Cadastro de Materiais',
                        icon: 'pi pi-plus-circle text-neutral-800 dark:text-neutral-200',
                        routerLink: ['/estoque/cadastrar-material'],
                        command: () => {
                            SharedState.showMenuDrawer$.next(false);
                        }
                    },
                    {
                        label: 'Gerenciar Almoxarifados',
                        icon: 'pi pi-warehouse text-neutral-800 dark:text-neutral-200',
                        routerLink: ['/estoque/almoxarifados'],
                        command: () => {
                            SharedState.showMenuDrawer$.next(false);
                        }
                    },
                    {
                        label: 'Gerenciar Caminhões',
                        icon: 'pi pi-truck text-neutral-800 dark:text-neutral-200',
                        routerLink: ['/estoque/caminhoes'],
                        command: () => {
                            SharedState.showMenuDrawer$.next(false);
                        }
                    },
                    {
                        label: 'Catálogo de Materiais',
                        icon: 'pi pi-table text-neutral-800 dark:text-neutral-200',
                        routerLink: ['/estoque/catalogo-materiais'],
                        command: () => {
                            SharedState.showMenuDrawer$.next(false);
                        }
                    },
                    {
                        label: 'Tipos / Subtipos (configuração)',
                        icon: 'pi pi-cog text-neutral-800 dark:text-neutral-200',
                        routerLink: ['/estoque/cadastrar-material'],
                        disabled: true,
                        command: () => {
                            SharedState.showMenuDrawer$.next(false);
                        }
                    },
                ]
            },
            {
                style: {
                    border: ''
                },
                label: 'Configurações',
                expanded: false,
                items: [
                    {
                        label: 'Usuários',
                        icon: 'pi pi-users text-neutral-800 dark:text-neutral-200',
                        routerLink: ['/configuracoes/usuarios'],
                        command: () => {
                            SharedState.showMenuDrawer$.next(false);
                        }
                    },
                    {
                        label: 'Minha Conta',
                        icon: 'pi pi-user text-neutral-800 dark:text-neutral-200',
                        routerLink: ['/configuracoes/conta'],
                        command: () => {
                            SharedState.showMenuDrawer$.next(false);
                        }
                    },
                    {
                        label: 'Equipes Operacionais',
                        icon: 'pi pi-sitemap  text-neutral-800 dark:text-neutral-200',
                        routerLink: ['/configuracoes/equipes'],
                        command: () => {
                            SharedState.showMenuDrawer$.next(false);
                        }
                    },
                    {
                        label: 'Estoquistas',
                        icon: 'pi pi-warehouse text-neutral-800 dark:text-neutral-200',
                    },
                ]
            }
        ];

    }

    toggleStock(open: boolean) {
        localStorage.setItem('toggleStock', JSON.stringify(open));
    }

    togglePreMeasurement(open: boolean) {
        localStorage.setItem('togglePreMeasurement', JSON.stringify(open));
    }

    toggleExecution() {
        this.bToggleExecution = !this.bToggleExecution;
        localStorage.setItem('toggleExecution', JSON.stringify(this.bToggleExecution));
    }

    toggleRequest() {
        this.bToggleRequest = !this.bToggleRequest;
        localStorage.setItem('toggleRequest', JSON.stringify(this.bToggleRequest));
    }

    toggleSettings(open: boolean) {
        localStorage.setItem('toggleSettings', JSON.stringify(open));
    }

    toggleContracts(open: boolean) {
        localStorage.setItem('toggleContracts', JSON.stringify(open));
    }

    toggleReports(open: boolean) {
        localStorage.setItem('toggleport', JSON.stringify(open));
    }


    protected readonly model = model;
    protected readonly SharedState = SharedState;
}
