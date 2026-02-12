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
                label: 'Ordens de Serviço',
                icon: 'pi pi-briefcase dark:text-neutral-200 text-gray-800',
                expanded: this.bToggleExecution,
                command: () => this.toggleExecution(),
                items: [

                    {
                        label: 'Nova Ordem de Serviço',
                        icon: 'pi pi-plus-circle text-green-500',
                        items: [
                            {
                                label: 'Criar sem pré-medição',
                                icon: 'pi pi-plus text-green-500',
                                routerLink: ['/contratos/listar'],
                                queryParams: { for: 'execution' },
                                command: () => SharedState.showMenuDrawer$.next(false)
                            },
                            {
                                label: 'Usar pré-medição analisada',
                                icon: 'pi pi-clipboard text-blue-500',
                                routerLink: 'pre-medicao/disponivel',
                                command: () => SharedState.showMenuDrawer$.next(false)
                            }
                        ]
                    },

                    {
                        label: 'Pré-medições para Análise',
                        icon: 'pi pi-exclamation-circle text-yellow-500',
                        badge: '3',
                        badgeStyleClass: 'p-badge-warning',
                        routerLink: 'pre-medicao/pendente',
                        command: () => SharedState.showMenuDrawer$.next(false)
                    },
                    {
                        label: 'Aguardando Estoque',
                        icon: 'pi pi-box text-orange-500',
                        routerLink: ['/execucoes/aguardando-estoque'],
                        command: () => SharedState.showMenuDrawer$.next(false)
                    },
                    {
                        label: 'Prontas para Execução',
                        icon: 'pi pi-play text-blue-500',
                        routerLink: ['/execucoes/prontas-para-execucao'],
                        command: () => SharedState.showMenuDrawer$.next(false)
                    },
                    {
                        label: 'Em Execução',
                        icon: 'pi pi-cog text-blue-600',
                        routerLink: ['/execucoes/em-execucao'],
                        command: () => SharedState.showMenuDrawer$.next(false)
                    },
                    {
                        label: 'Concluídas',
                        icon: 'pi pi-check-circle text-green-600',
                        routerLink: ['/execucoes/concluidas'],
                        command: () => SharedState.showMenuDrawer$.next(false)
                    }

                    // {
                    //     label: 'Importar pré-medição (.xlx)',
                    //     icon: 'pi pi-file-excel text-neutral-800 dark:text-neutral-200',
                    //     routerLink: ['/contratos/listar'],
                    //     queryParams: {for: 'preMeasurement'},
                    //     command: () => {
                    //         SharedState.showMenuDrawer$.next(false);
                    //     }
                    // },
                ]
            },

            {
                label: 'Solicitações ao Estoquista',
                icon: 'pi pi-box dark:text-neutral-200 text-gray-800',
                expanded: this.bToggleRequest,
                command: () => {
                    this.toggleRequest();
                },
                items: [
                    {
                        label: 'Pendentes de Aprovação',
                        icon: 'pi pi-clock text-yellow-500',
                        routerLink: ['/requisicoes'],
                        queryParams: { status: 'PENDING' },
                        command: () => {
                            SharedState.showMenuDrawer$.next(false);
                        }
                    },
                    {
                        label: 'Disponíveis para Coleta',
                        icon: 'pi pi-check-circle text-green-500',
                        routerLink: ['/requisicoes'],
                        queryParams: { status: 'APPROVED' },
                        command: () => {
                            SharedState.showMenuDrawer$.next(false);
                        }
                    },
                    {
                        label: 'Gerenciar Ordens de Serviço',
                        icon: 'pi pi-briefcase text-blue-500',
                        routerLink: ['/requisicoes/instalacoes/gerenciamento-estoque'],
                        command: () => {
                            SharedState.showMenuDrawer$.next(false);
                        }
                    }
                ]
            },

            {
                label: 'Relatórios',
                icon: 'pi pi-chart-line dark:text-neutral-200 text-gray-800',
                expanded: this.bToggleReports,
                command: () => {
                    this.toggleReports(!this.bToggleReports);
                },
                items: [
                    {
                        label: 'Personalizados',
                        icon: 'pi pi-sliders-h text-blue-500',
                        routerLink: ['/relatorios/gerenciamento'],
                        command: () => {
                            SharedState.showMenuDrawer$.next(false);
                        }
                    },
                    {
                        label: 'Manutenções (30 dias)',
                        icon: 'pi pi-wrench text-blue-500',
                        routerLink: ['/relatorios/manutencoes'],
                        command: () => {
                            SharedState.showMenuDrawer$.next(false);
                        }
                    },
                    {
                        label: 'Instalações (90 dias)',
                        icon: 'pi pi-lightbulb text-blue-500',
                        routerLink: ['/relatorios/instalacoes'],
                        command: () => {
                            SharedState.showMenuDrawer$.next(false);
                        }
                    }
                ]
            },

            {
                label: 'Contratos',
                icon: 'pi pi-file dark:text-neutral-200 text-gray-800',
                expanded: this.bToggleContracts,
                command: () => {
                    this.toggleContracts(!this.bToggleContracts);
                },
                items: [
                    {
                        label: 'Novo Contrato',
                        icon: 'pi pi-plus-circle text-green-500',
                        routerLink: ['/contratos/criar'],
                        command: () => {
                            SharedState.showMenuDrawer$.next(false);
                        }
                    },
                    {
                        label: 'Listar Contratos',
                        icon: 'pi pi-folder-open text-blue-500',
                        routerLink: ['/contratos/listar'],
                        queryParams: { for: 'view' },
                        command: () => {
                            SharedState.showMenuDrawer$.next(false);
                        }
                    }
                ]
            },

            {
                label: 'Estoque',
                icon: 'pi pi-warehouse dark:text-neutral-200 text-gray-800',
                expanded: this.bToggleStock,
                command: () => {
                    this.toggleStock(!this.bToggleStock);
                },
                items: [
                    {
                        label: 'Movimentar Estoque',
                        icon: 'pi pi-arrow-right-arrow-left text-blue-500',
                        routerLink: ['/estoque/movimentar-estoque'],
                        command: () => {
                            SharedState.showMenuDrawer$.next(false);
                        }
                    },
                    {
                        label: 'Movimentações Pendentes',
                        icon: 'pi pi-clock text-yellow-500',
                        routerLink: ['/estoque/movimentar-estoque-pendente'],
                        command: () => {
                            SharedState.showMenuDrawer$.next(false);
                        }
                    },
                    {
                        label: 'Cadastro de Materiais',
                        icon: 'pi pi-plus-circle text-green-500',
                        routerLink: ['/estoque/cadastrar-material'],
                        command: () => {
                            SharedState.showMenuDrawer$.next(false);
                        }
                    },
                    {
                        label: 'Almoxarifados',
                        icon: 'pi pi-home text-blue-500',
                        routerLink: ['/estoque/almoxarifados'],
                        command: () => {
                            SharedState.showMenuDrawer$.next(false);
                        }
                    },
                    {
                        label: 'Caminhões',
                        icon: 'pi pi-truck text-blue-500',
                        routerLink: ['/estoque/caminhoes'],
                        command: () => {
                            SharedState.showMenuDrawer$.next(false);
                        }
                    },
                    {
                        label: 'Catálogo de Materiais',
                        icon: 'pi pi-table text-neutral-500',
                        routerLink: ['/estoque/catalogo-materiais'],
                        command: () => {
                            SharedState.showMenuDrawer$.next(false);
                        }
                    }
                ]
            },

            {
                label: 'Configurações',
                icon: 'pi pi-cog dark:text-neutral-200 text-gray-800',
                expanded: false,
                items: [
                    {
                        label: 'Usuários',
                        icon: 'pi pi-users text-blue-500',
                        routerLink: ['/configuracoes/usuarios'],
                        command: () => {
                            SharedState.showMenuDrawer$.next(false);
                        }
                    },
                    {
                        label: 'Minha Conta',
                        icon: 'pi pi-user text-blue-500',
                        routerLink: ['/configuracoes/conta'],
                        command: () => {
                            SharedState.showMenuDrawer$.next(false);
                        }
                    },
                    {
                        label: 'Equipes Operacionais',
                        icon: 'pi pi-sitemap text-blue-500',
                        routerLink: ['/configuracoes/equipes'],
                        command: () => {
                            SharedState.showMenuDrawer$.next(false);
                        }
                    },
                    {
                        label: 'Estoquistas',
                        icon: 'pi pi-id-card text-blue-500'
                    }
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
