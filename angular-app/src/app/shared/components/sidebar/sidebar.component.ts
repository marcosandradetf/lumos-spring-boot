import { Component, EventEmitter, Input, model, OnInit, Output } from '@angular/core';
import {AsyncPipe, NgClass, NgIf, NgOptimizedImage} from '@angular/common';
import { Router } from '@angular/router';
import { PanelMenu } from 'primeng/panelmenu';
import { MenuItem } from 'primeng/api';
import { UtilsService } from '../../../core/service/utils.service';
import { SharedState } from '../../../core/service/shared-state';

@Component({
    selector: 'app-sidebar',
    standalone: true,
    imports: [
        PanelMenu,
        NgIf,
        NgOptimizedImage,
        NgClass,
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
    configurationFinished = false;
    isSupport = false;

    constructor(
        private utils: UtilsService,
        protected router: Router,
    ) {

    }

    ngOnInit(): void {
        const isMobile = window.innerWidth <= 1024;
        const isSupport = localStorage.getItem('isSupport');
        this.isSupport = isSupport !== null && isSupport === 'true';

        SharedState.showMenuDrawer$.subscribe((open) => {
            this.showDrawer = open;
        });

        this.utils.menuState$.subscribe((isOpen: boolean) => {
            this.menuOpen = isOpen;
        });

        this.configurationFinished = localStorage.getItem('configurationFinished') !== null;

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

        if (!this.configurationFinished) {
            this.bToggleContracts = true;
            this.bToggleStock = true;
            this.bToggleSettings = true;
        }


        this.items = [
            {
                label: 'Primeiros passos', // título mais curto e direto
                title: 'Visualize a localização das execuções em campo',
                icon: 'pi pi-play', // azul = informação/visualização
                routerLink: ['/configuracoes/onboarding'],
                visible: !this.configurationFinished,
                command: () => {
                    SharedState.showMenuDrawer$.next(false);
                },
            },

            {
                label: 'Administração SaaS',
                visible: this.isSupport,
                icon: 'pi pi-sliders-v dark:text-neutral-200 text-gray-800',
                expanded: this.bToggleSettings,
                items: [
                    {
                        disabled: true,
                        label: 'Adicionar novo cliente',
                        icon: 'pi pi-plus text-blue-500',
                        routerLink: ['/configuracoes/usuarios'],
                        command: () => {
                            SharedState.showMenuDrawer$.next(false);
                        }
                    },
                    {
                        label: 'Contratos pendentes',
                        icon: 'pi pi-book text-blue-500',
                        routerLink: ['/configuracoes/conta'],
                        command: () => {
                            SharedState.showMenuDrawer$.next(false);
                        }
                    },
                    {
                        disabled: false,
                        label: 'Faturamento de Clientes',
                        icon: 'pi pi-wallet text-blue-500',
                        routerLink: ['/configuracoes/conta'],
                        command: () => {
                            SharedState.showMenuDrawer$.next(false);
                        }
                    },
                ]
            },

            {
                label: 'Dashboards',
                icon: 'pi pi-chart-bar dark:text-neutral-200 text-gray-800', // ícone mais relacionado a dashboards
                expanded: this.configurationFinished,
                items: [
                    {
                        label: 'Mapa de Execuções', // título mais curto e direto
                        title: 'Visualize a localização das execuções em campo',
                        icon: 'pi pi-map-marker text-blue-500', // azul = informação/visualização
                        routerLink: ['/dashboard/mapa-execucoes'],
                        command: () => {
                            SharedState.showMenuDrawer$.next(false);
                        },

                    },
                    {
                        disabled: false,
                        label: 'Visão Executiva', // título mais natural
                        title: 'Acompanhe a produtividade e tempo das equipes',
                        icon: 'pi pi-compass text-purple-700', // verde = performance/progresso
                        routerLink: ['/dashboard/visao-executiva'],
                        command: () => {
                            SharedState.showMenuDrawer$.next(false);
                        }
                    },
                    {
                        disabled: false,
                        label: 'Produtividade da Equipe', // título mais natural
                        title: 'Acompanhe a produtividade e tempo das equipes',
                        icon: 'pi pi-users text-green-500', // verde = performance/progresso
                        routerLink: ['/dashboard/produtividade-equipe'],
                        queryParams: { status: 'APPROVED' },
                        command: () => {
                            SharedState.showMenuDrawer$.next(false);
                        }
                    },
                ]
            },
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
                        label: 'Em Análise de Estoque',
                        icon: 'pi pi-search text-yellow-500',
                        routerLink: ['/execucoes/analise-estoque'],
                        command: () => SharedState.showMenuDrawer$.next(false)
                    },
                    {
                        label: 'Aguardando Coleta',
                        icon: 'pi pi-box text-orange-500',
                        routerLink: ['/execucoes/aguardando-coleta'],
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
                        label: 'Execuções',
                        icon: 'pi pi-wrench text-orange-400',
                        expanded: true,
                        items: [
                            {
                                label: 'Manutenções',
                                icon: 'pi pi-wrench text-blue-500',
                                routerLink: ['/relatorios/manutencoes'],
                                command: () => {
                                    SharedState.showMenuDrawer$.next(false);
                                }
                            },
                            {
                                label: 'Instalações',
                                icon: 'pi pi-lightbulb text-blue-500',
                                routerLink: ['/relatorios/instalacoes'],
                                command: () => {
                                    SharedState.showMenuDrawer$.next(false);
                                }
                            },
                                          {
                                label: 'Analítico de Operações',
                                icon: 'pi pi-sliders-h text-blue-500',
                                routerLink: ['/relatorios/execucoes/analitico-de-operacoes'],
                                command: () => {
                                    SharedState.showMenuDrawer$.next(false);
                                }
                            },
                            {
                                label: 'Agrupados',
                                icon: 'pi pi-box text-blue-500',
                                routerLink: ['/relatorios/gerenciamento'],
                                command: () => {
                                    SharedState.showMenuDrawer$.next(false);
                                }
                            },
                        ]
                    },
                    {
                        label: 'Estoque',
                        icon: 'pi pi-box text-indigo-500',
                        expanded: false,
                        items: [
                            {
                                label: 'Saída/Saldo por Instalação',
                                icon: 'pi pi-hammer text-orange-400',
                                routerLink: ['/relatorios/estoque/saida-saldo-instalacao'],
                                command: () => {
                                    SharedState.showMenuDrawer$.next(false);
                                },
                                title: 'Demonstrativo de saída e saldo de estoque por instalação'
                            },
                            {
                                label: 'Saída por Requisições',
                                icon: 'pi pi-briefcase text-orange-300',
                                disabled: true,
                                routerLink: ['/relatorios/estoque'],
                                command: () => {
                                    SharedState.showMenuDrawer$.next(false);
                                },
                                tooltip: 'Disponível em breve'
                            }
                        ]
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
                        },
                    },
                    {
                        label: 'Listar Contratos',
                        icon: 'pi pi-folder-open text-blue-500',
                        routerLink: ['/contratos/listar'],
                        queryParams: { for: 'view' },
                        command: () => {
                            SharedState.showMenuDrawer$.next(false);
                        },
                    },
                    {
                        label: 'Vincular Instalações', // O título que você escolheu
                        title: 'Vincule instalações concluídas sem ordem de serviço aos itens contratuais para cobrança',
                        icon: 'pi pi-link text-emerald-500', // Verde = pronto para cobrar / dinheiro
                        routerLink: ['/contratos/instalacoes-pendentes'],
                        badge: '15',
                        badgeStyleClass: 'p-badge-success', // Badge verde para indicar que é coisa pronta esperando só o vínculo
                        command: () => SharedState.showMenuDrawer$.next(false),
                    },
                    {
                        label: 'Catálogo de Itens Contratuais',
                        icon: 'pi pi-table text-neutral-500',
                        routerLink: ['/contratos/itens-contratuais/catalogo'],
                        command: () => {
                            SharedState.showMenuDrawer$.next(false);
                        },
                    },
                    {
                        label: 'Itens Contratuais',
                        icon: 'pi pi-list text-green-500',
                        routerLink: ['/contratos/itens-contratuais/cadastro'],
                        command: () => {
                            SharedState.showMenuDrawer$.next(false);
                        },
                    },
                    {
                        label: 'Vincular Itens Contratuais',
                        icon: 'pi pi-link text-cyan-500',
                        routerLink: ['/contratos/itens-contratuais/vinculos'],
                        queryParams: {
                            'operation': 'item'
                        },
                        command: () => {
                            SharedState.showMenuDrawer$.next(false);
                        },
                    },
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
                        },
                    },
                    {
                        label: 'Movimentações Pendentes',
                        icon: 'pi pi-clock text-yellow-500',
                        routerLink: ['/estoque/movimentar-estoque-pendente'],
                        command: () => {
                            SharedState.showMenuDrawer$.next(false);
                        },
                    },
                    {
                        label: 'Cadastro de Materiais',
                        icon: 'pi pi-plus-circle text-green-500',
                        routerLink: ['/estoque/cadastrar-material'],
                        command: () => {
                            SharedState.showMenuDrawer$.next(false);
                        },
                    },
                    {
                        label: 'Almoxarifados',
                        icon: 'pi pi-home text-blue-500',
                        routerLink: ['/estoque/almoxarifados'],
                        command: () => {
                            SharedState.showMenuDrawer$.next(false);
                        },
                    },
                    {
                        label: 'Caminhões',
                        icon: 'pi pi-truck text-blue-500',
                        routerLink: ['/estoque/caminhoes'],
                        command: () => {
                            SharedState.showMenuDrawer$.next(false);
                        },
                    },
                    {
                        label: 'Catálogo de Materiais',
                        icon: 'pi pi-table text-neutral-500',
                        routerLink: ['/estoque/catalogo-materiais'],
                        command: () => {
                            SharedState.showMenuDrawer$.next(false);
                        },
                    }
                ]
            },

            {
                label: 'Configurações',
                icon: 'pi pi-cog dark:text-neutral-200 text-gray-800',
                expanded: this.bToggleSettings,
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
                        icon: 'pi pi-id-card text-blue-500',
                        routerLink: ['/configuracoes/estoquistas'],
                        command: () => {
                            SharedState.showMenuDrawer$.next(false);
                        }
                    }
                ]
            },
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

    toggleMenu() {
        this.menuOpen = !this.menuOpen;
        this.utils.toggleMenu(this.menuOpen);
        localStorage.setItem('menuOpen', JSON.stringify(this.menuOpen));
    }
}
