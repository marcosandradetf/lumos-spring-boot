// app/app.routes.ts
import {Routes} from '@angular/router';
import {AuthGuard} from './core/auth/auth.guard';
import {ServiceRequestMapComponent} from './features/maintenance/request/service-request-map.component';
import {
    ContractReferenceItemFormComponent
} from './features/contract/contract-reference-item-form/contract-reference-item-form.component';
import {
    ContractReferenceItemLinksComponent
} from './features/contract/contract-reference-item-links/contract-reference-item-links.component';
import {MaterialFormComponent} from './features/stock/material-form/material-form.component';
import {ServerErrorComponent} from './shared/components/server-error/server-error.component';
import {StockistsComponent} from './features/stock/stockists/stockists.component';


export const routes: Routes = [
    // start path login
    {
        path: 'auth/login',
        loadComponent: () => import('./core/auth/pages/login/login.component').then(m => m.LoginComponent),
        data: {hideBackButton: true} // Rota pai
    },
    {
        path: 'auth/auto-login',
        loadComponent: () => import('./core/auth/pages/auto-login/auto-login.component').then(m => m.AutoLoginComponent),
        data: {hideBackButton: true} // Rota pai
    },
    {
        path: 'primeiro-acesso',
        loadComponent: () => import('./core/auth/pages/first-access/first-access.component').then(m => m.FirstAccessComponent),
        data: {hideBackButton: true} // Rota pai
    },
    // end

    {
        path: 'acesso-negado/:section',
        loadComponent: () => import('./shared/components/no-access/no-access.component').then(n => n.NoAccessComponent),
        canActivate: [AuthGuard],
        data: {role: [''], path: ''},
    },

    // start billing paths
    {
        path: 'cobranca',
        loadComponent: () => import('./features/billing/billing.component').then(m => m.BillingComponent),
        canActivate: [AuthGuard],
        data: {role: ['ADMIN'], path: 'cobranca'},
    },
    {
        path: 'acesso-indisponivel',
        loadComponent: () => import('./features/billing/unavailable-access.component').then(m => m.UnavailableAccessComponent),
        canActivate: [AuthGuard],
        data: {role: [], path: 'acesso-indisponivel'},
    },
    // end billing paths

    // start paths stock
    {
        path: 'estoque/cadastrar-material',
        loadComponent: () => import('./features/stock/material-form/material-form.component').then(m => m.MaterialFormComponent),
        canActivate: [AuthGuard],
        data: {
            role: ['ADMIN', 'ESTOQUISTA', 'ESTOQUISTA_CHEFE', 'ANALISTA', 'RESPONSAVEL_TECNICO'],
            path: 'estoque',

            title: 'Cadastro de Materiais',
            icon: 'pi pi-plus-circle text-green-500',
        },
    },
    {
        path: 'estoque/editar-material',
        loadComponent: () => import('./features/stock/material-form/material-form.component').then(m => m.MaterialFormComponent),
        canActivate: [AuthGuard],
        data: {
            role: ['ADMIN', 'ESTOQUISTA', 'ESTOQUISTA_CHEFE', 'ANALISTA', 'RESPONSAVEL_TECNICO'], path: 'estoque'
        },
    },
    {
        path: 'estoque/catalogo-materiais',
        loadComponent: () => import('./features/stock/material/material-page.component').then(m => m.MaterialPageComponent),
        canActivate: [AuthGuard],
        data: {
            title: 'Catálogo de Materiais',
            icon: 'pi pi-table text-neutral-500',
            role: ['ADMIN', 'ESTOQUISTA', 'ESTOQUISTA_CHEFE', 'ANALISTA', 'RESPONSAVEL_TECNICO'], path: 'estoque'
        },
    },
    {
        path: 'estoque/grupos',
        loadComponent: () => import('./features/stock/groups/groups.component').then(m => m.GroupsComponent),
        canActivate: [AuthGuard],
        data: {role: ['ADMIN', 'ESTOQUISTA', 'ESTOQUISTA_CHEFE'], path: 'estoque'},
    },
    {
        path: 'estoque/tipos',
        loadComponent: () => import('./features/stock/types/types.component').then(m => m.TypesComponent),
        canActivate: [AuthGuard],
        data: {role: ['ADMIN'], path: 'estoque'},
    },
    {
        path: 'estoque/movimentar-estoque',
        loadComponent: () => import('./features/stock/stock-movement/stock-movement.component').then(m => m.StockMovementComponent),
        canActivate: [AuthGuard],
        data: {
            title: 'Movimentar Estoque',
            icon: 'pi pi-arrow-right-arrow-left text-blue-500',
            role: ['ADMIN', 'ESTOQUISTA', 'ESTOQUISTA_CHEFE'], path: 'estoque'
        },
    },
    {
        path: 'estoque/movimentar-estoque-pendente',
        loadComponent: () => import('./features/stock/stock-movement-pending/stock-movement-pending.component').then(m => m.StockMovementPendingComponent),
        canActivate: [AuthGuard],
        data: {
            title: 'Movimentações de Estoque Pendentes',
            icon: 'pi pi-clock text-yellow-500',
            role: ['ADMIN', 'ESTOQUISTA_CHEFE'], path: 'estoque'
        },
    },
    {
        path: 'estoque/movimentar-estoque-aprovado',
        loadComponent: () => import('./features/stock/stock-movement-approvated/stock-movement-approvated.component').then(m => m.StockMovementApprovatedComponent),
        canActivate: [AuthGuard],
        data: {
            role: ['ADMIN', 'ESTOQUISTA', 'ESTOQUISTA_CHEFE'], path: 'estoque'
        },
    },
    {
        path: 'estoque/almoxarifados',
        loadComponent: () => import('./features/stock/deposits/deposits.component').then(m => m.DepositsComponent),
        canActivate: [AuthGuard],
        data: {
            title: 'Almoxarifados',
            icon: 'pi pi-home text-blue-500',
            role: ['ADMIN', 'ANALISTA', 'ESTOQUISTA', 'ESTOQUISTA_CHEFE'], path: 'estoque'
        },
    },

    {
        path: 'estoque/caminhoes',
        loadComponent: () => import('./features/stock/truck-deposits/deposits.component').then(d => d.TruckDepositComponent),
        canActivate: [AuthGuard],
        data: {
            title: 'Caminhões / Veículos',
            icon: 'pi pi-truck text-blue-500',
            role: ['ADMIN', 'ANALISTA', 'ESTOQUISTA', 'ESTOQUISTA_CHEFE'], path: 'estoque'
        },
    },
    {
        path: 'configuracoes/importar-planilha',
        loadComponent: () => import('./features/stock/import-materials/import-materials.component').then(m => m.ImportMaterialsComponent),
        canActivate: [AuthGuard],
        data: {role: ['ADMIN', 'ESTOQUISTA', 'ESTOQUISTA_CHEFE'], path: 'estoque'},
    },


    {path: 'estoque', redirectTo: 'estoque/materiais'},
    // end

    // start contract paths
    {path: 'contratos', redirectTo: 'contratos/dashboard'},
    {
        path: 'contratos/dashboard',
        loadComponent: () => import('./features/contract/pages/dashboard/dashboard.component').then(m => m.DashboardComponent),
        canActivate: [AuthGuard],
        data: {role: ['ADMIN', 'ANALISTA'], path: 'contratos'},
    },
    {
        path: 'contratos/itens-contratuais/cadastro',
        component: ContractReferenceItemFormComponent,
        canActivate: [AuthGuard],
        data: {
            title: 'Catálogo de Itens Contratuais',
            icon: 'pi pi-database text-neutral-500',
            role: ['ADMIN', 'ANALISTA'], path: 'contratos'
        },
    },
    {
        path: 'contratos/itens-contratuais/vinculos',
        component: ContractReferenceItemLinksComponent,
        canActivate: [AuthGuard],
        data: {
            title: 'Vincular Itens Contratuais',
            icon: 'pi pi-link text-cyan-500',
            query: {operation: 'item'},
            role: ['ADMIN', 'ANALISTA'], path: 'contratos'
        },
    },
    {
        path: 'contratos/itens-contratuais/vinculos',
        component: ContractReferenceItemLinksComponent,
        canActivate: [AuthGuard],
        data: {
            title: 'Vincular Materiais aos Itens Contratuais',
            icon: 'pi pi-link text-cyan-500',
            query: {operation: 'material'},
            role: ['ADMIN', 'ANALISTA'], path: 'contratos'
        },
    },
    {
        path: 'contratos/criar',
        loadComponent: () => import('./features/contract/pages/create/create.component').then(m => m.CreateComponent),
        canActivate: [AuthGuard],
        data: {
            title: 'Novo Contrato',
            icon: 'pi pi-plus-circle text-green-500',
            role: ['ADMIN', 'ANALISTA'], path: 'contratos'
        },
    },
    {
        path: 'contratos/editar',
        loadComponent: () => import('./features/contract/pages/create/create.component').then(m => m.CreateComponent),
        canActivate: [AuthGuard],
        data: {role: ['ADMIN', 'ANALISTA'], path: 'contratos'},
    },
    {
        path: 'contratos/listar',
        loadComponent: () => import('./features/contract/pages/list/contract-list/contract-list.component').then(c => c.ContractListComponent),
        canActivate: [AuthGuard],
        data: {
            title: 'Listar Contratos',
            icon: 'pi pi-folder-open text-blue-500',
            role: ['ADMIN', 'ANALISTA'], path: 'contratos'
        },
    },

    {
        path: 'contratos/listar',
        loadComponent: () => import('./features/contract/pages/list/contract-list/contract-list.component').then(c => c.ContractListComponent),
        canActivate: [AuthGuard],
        data: {
            title: 'Nova Ordem de Serviço Sem Pré-medição',
            icon: 'pi pi-plus text-green-500',
            query: {for: 'execution'},
            role: ['ADMIN', 'ANALISTA'], path: 'contratos'
        },
    },


    {
        path: 'contratos/instalacoes-pendentes',
        loadComponent: () => import('./features/contract/pending-executions/pending-executions.component').then(e => e.PendingExecutionsComponent),
        canActivate: [AuthGuard],
        data: {
            title: 'Vincular Instalações',
            icon: 'pi pi-link text-emerald-500',
            role: ['ADMIN', 'ANALISTA', 'RESPONSAVEL_TECNICO'], path: 'execucoes'
        },
    },
    {
        path: 'contratos/validar-execucao/:id',
        loadComponent: () => import('./features/contract/validate-execution/execution-no-work-service.component').then(e => e.ExecutionNoWorkServiceComponent),
        canActivate: [AuthGuard],
        data: {role: ['ADMIN', 'ANALISTA', 'RESPONSAVEL_TECNICO'], path: 'execucoes'},
    },
    // end

    // start settings path
    {
        path: 'configuracoes/onboarding',
        loadComponent: () => import('./features/manage/settings/settings.component').then(s => s.SettingsComponent),
        children: [
            {
                path: 'usuarios',
                loadComponent: () => import('./features/manage/user/user.component').then(m => m.UserComponent),
            },
            {
                path: 'equipes',
                loadComponent: () => import('./features/manage/team/team.component').then(m => m.TeamComponent),
            },
            {
                path: 'estoquistas',
                component: StockistsComponent,
            },
            {
                path: 'almoxarifados',
                loadComponent: () => import('./features/stock/deposits/deposits.component').then(m => m.DepositsComponent),
            },
            {
                path: 'caminhoes',
                loadComponent: () => import('./features/stock/truck-deposits/deposits.component').then(d => d.TruckDepositComponent),
            },
            {
                path: 'cadastrar-material',
                loadComponent: () => import('./features/stock/material-form/material-form.component').then(m => m.MaterialFormComponent),
            },
            {
                path: 'itens-contratuais',
                component: ContractReferenceItemFormComponent,
            },
            {
                path: 'itens-contratuais-vinculos',
                component: ContractReferenceItemLinksComponent,
                children: [
                    {
                        path: 'material-create',
                        component: MaterialFormComponent
                    }
                ]
            },
            {
                path: 'criar-contrato',
                loadComponent: () => import('./features/contract/pages/create/create.component').then(m => m.CreateComponent),
            }
        ],
        canActivate: [AuthGuard],
        data: {role: ['ADMIN', 'ANALISTA', 'RESPONSAVEL_TECNICO'], path: 'configuracoes', hideBackButton: true},
    },
    {
        path: 'configuracoes/usuarios',
        loadComponent: () => import('./features/manage/user/user.component').then(m => m.UserComponent),
        canActivate: [AuthGuard],
        data: {
            title: 'Usuários',
            icon: 'pi pi-users text-blue-500',
            role: ['ADMIN'], path: 'configuracoes'
        },
    },
    {
        path: 'configuracoes/equipes',
        loadComponent: () => import('./features/manage/team/team.component').then(m => m.TeamComponent),
        canActivate: [AuthGuard],
        data: {
            title: 'Equipes Operacionais',
            icon: 'pi pi-sitemap text-blue-500',
            role: ['ADMIN', 'ANALISTA', 'RESPONSAVEL_TECNICO'], path: 'configuracoes'
        },
    },
    {
        path: 'configuracoes/estoquistas',
        component: StockistsComponent,
        canActivate: [AuthGuard],
        data: {
            title: 'Estoquistas',
            icon: 'pi pi-id-card text-blue-500',
            role: ['ADMIN', 'ANALISTA', 'RESPONSAVEL_TECNICO'], path: 'configuracoes'
        },
    },
    {
        path: 'configuracoes/empresa',
        loadComponent: () => import('./features/manage/team/team.component').then(m => m.TeamComponent),
        canActivate: [AuthGuard],
        data: {role: ['ADMIN'], path: 'configuracoes'},
    },
    {
        path: 'configuracoes/conta',
        loadComponent: () => import('./features/manage/account/account.component').then(a => a.AccountComponent),
        canActivate: [AuthGuard],
        data: {
            title: 'Minha Conta',
            icon: 'pi pi-user text-blue-500',
            role: [], path: 'conta'
        },
    },
    // end

    // start executions/pre-measurements path
    {
        path: 'pre-medicao/:status',
        loadComponent: () => import('./features/pre-measurement/pre-measurement-home/pre-measurement.component').then(p => p.PreMeasurementComponent),
        canActivate: [AuthGuard],
        data: {
            title: 'Pré-medições para Análise',
            icon: 'pi pi-exclamation-circle text-yellow-500',
            routeParams: {status: 'pendente'},
            role: ['ADMIN', 'ANALISTA', 'RESPONSAVEL_TECNICO'], path: 'execucoes'
        },
    },

    {
        path: 'pre-medicao/:status',
        loadComponent: () => import('./features/pre-measurement/pre-measurement-home/pre-measurement.component').then(p => p.PreMeasurementComponent),
        canActivate: [AuthGuard],
        data: {
            title: 'Nova Ordem de Serviço Com Pré-medição',
            icon: 'pi pi-plus-circle text-green-500',
            routeParams: {status: 'disponivel'},
            role: ['ADMIN', 'ANALISTA', 'RESPONSAVEL_TECNICO'], path: 'execucoes'
        },
    },
    {
        path: 'pre-medicao/relatorio/:id/:step',
        loadComponent: () => import('./features/pre-measurement/pre-measurement-report/pre-measurement-report.component').then(p => p.PreMeasurementReportComponent),
        canActivate: [AuthGuard],
        data: {role: ['ADMIN', 'ANALISTA', 'RESPONSAVEL_TECNICO'], path: 'execucoes'},
    },
    {
        path: 'pre_medicao/editar',
        loadComponent: () => import('./features/pre-measurement/pre-measurement-edit/pre-measurement-edit.component').then(p => p.PreMeasurementEditComponent),
        canActivate: [AuthGuard],
        data: {role: ['ADMIN', 'ANALISTA', 'RESPONSAVEL_TECNICO'], path: 'execucoes'},
    },
    {
        path: 'pre-medicao/importar/contrato/:id',
        loadComponent: () => import('./features/pre-measurement/import-pre-measurements/import-pre-measurements.component').then(p => p.ImportPreMeasurementsComponent),
        canActivate: [AuthGuard],
        data: {role: ['ADMIN', 'ANALISTA', 'RESPONSAVEL_TECNICO'], path: 'execucoes'},
    },
    {
        path: 'pre_medicao/visualizar',
        loadComponent: () => import('./features/pre-measurement/pre-measurement-view/pre-measurement-view.component').then(p => p.PreMeasurementViewComponent),
        canActivate: [AuthGuard],
        data: {role: ['ADMIN', 'ANALISTA', 'RESPONSAVEL_TECNICO'], path: 'execucoes'},
    },
    //

    // start executions/executions path
    {
        path: 'execucao/pre-medicao/:id',
        loadComponent: () => import('./features/executions/pre-measurement-available/measurement-details.component').then(m => m.MeasurementDetailsComponent),
        canActivate: [AuthGuard],
        data: {role: ['ADMIN', 'ANALISTA', 'RESPONSAVEL_TECNICO'], path: 'execucoes'},
    },

    {
        path: 'ordens-de-servico/nova',
        loadComponent: () => import('./features/executions/execution-no-pre-measurement/execution-no-pre-measurement.component').then(e => e.ExecutionNoPreMeasurementComponent),
        canActivate: [AuthGuard],
        data: {
            role: ['ADMIN', 'ANALISTA', 'RESPONSAVEL_TECNICO'], path: 'execucoes'
        },
    },

    {
        path: 'execucoes/:status',
        loadComponent: () => import('./features/executions/execution-progress/execution-progress.component').then(e => e.ExecutionProgressComponent),
        canActivate: [AuthGuard],
        data: {role: ['ADMIN', 'ANALISTA', 'RESPONSAVEL_TECNICO'], path: 'execucoes'},
    },
    //end

    // start request path
    {
        path: 'requisicoes/instalacoes/gerenciamento-estoque',
        loadComponent: () => import('./features/requests/reservation-management/reservation-management.component').then(r => r.ReservationManagementComponent),
        canActivate: [AuthGuard],
        data: {
            title: 'Gerenciar Ordens de Serviço',
            icon: 'pi pi-briefcase text-blue-500',
            role: ['ADMIN', 'ESTOQUISTA', 'ESTOQUISTA_CHEFE'], path: 'gerenciamento-reservas'
        },
    },
    {
        path: 'requisicoes/gerenciamento/execucao',
        loadComponent: () => import('./features/requests/reservation-management-select/reservation-management-select.component').then(r => r.ReservationManagementSelectComponent),
        canActivate: [AuthGuard],
        data: {role: ['ADMIN', 'ESTOQUISTA', 'ESTOQUISTA_CHEFE'], path: 'gerenciamento-reservas'},
    },
    {
        path: 'requisicoes',
        loadComponent: () => import('./features/requests/reservation-pending/reservation-pending.component').then(r => r.ReservationPendingComponent),
        canActivate: [AuthGuard],
        data: {
            title: 'Requisições Pendentes de Aprovação',
            icon: 'pi pi-clock text-yellow-500',
            query: { status: 'PENDING' },
            role: ['ADMIN', 'ESTOQUISTA', 'ESTOQUISTA_CHEFE'], path: 'gerenciamento-reservas'
        },
    },
    {
        path: 'requisicoes',
        loadComponent: () => import('./features/requests/reservation-pending/reservation-pending.component').then(r => r.ReservationPendingComponent),
        canActivate: [AuthGuard],
        data: {
            title: 'Requisições Disponíveis para Coleta',
            icon: 'pi pi-check-circle text-green-500',
            query: { status: 'APPROVED' },
            role: ['ADMIN', 'ESTOQUISTA', 'ESTOQUISTA_CHEFE'], path: 'gerenciamento-reservas'
        },
    },
    //end

    //reports
    {
        path: 'relatorios/gerenciamento',
        loadComponent: () => import('./features/reports/execution/manage/report-manage.component').then(r => r.ReportManageComponent),
        canActivate: [AuthGuard],
        data: {
            role: ['ADMIN', 'RESPONSAVEL_TECNICO', 'ANALISTA',],
            path: 'relatorios'
        },
    },
    {
        path: 'relatorios/manutencoes',
        loadComponent: () => import('./features/reports/execution/maintenance/maintenance.component').then(r => r.MaintenanceComponent),
        canActivate: [AuthGuard],
        data: {
            title: 'Relatórios de Manutenções',
            icon: 'pi pi-wrench text-blue-500',
            role: ['ADMIN', 'RESPONSAVEL_TECNICO', 'ANALISTA', 'ESTOQUISTA', 'ESTOQUISTA_CHEFE'],
            path: 'relatorios'
        },
    },
    {
        path: 'relatorios/instalacoes',
        loadComponent: () => import('./features/reports/execution/installation/installation.component').then(r => r.InstallationComponent),
        canActivate: [AuthGuard],
        data: {
            title: 'Relatórios de Instalacoes',
            icon: 'pi pi-lightbulb text-blue-500',
            role: ['ADMIN', 'RESPONSAVEL_TECNICO', 'ANALISTA',], path: 'relatorios'
        },
    },
    {
        path: 'relatorios/estoque/saida-saldo-instalacao',
        loadComponent: () => import('./features/reports/material-reservation/material-reservation.component').then(r => r.MaterialReservationComponent),
        canActivate: [AuthGuard],
        data: {
            title: 'Relatório de Saída/Saldo por Instalação',
            icon: 'pi pi-hammer text-orange-400',
            role: ['ADMIN', 'RESPONSAVEL_TECNICO', 'ANALISTA', 'ESTOQUISTA', 'ESTOQUISTA_CHEFE'],
            path: 'relatorios'
        },
    },
    {
        path: 'relatorios/execucoes/analitico-de-operacoes',
        loadComponent: () => import('./features/reports/execution/operation/operation.component').then(r => r.OperationComponent),
        canActivate: [AuthGuard],
        data: {
            title: 'Relatório Analítico de Operações',
            icon: 'pi pi-sliders-h text-blue-500',
            role: ['ADMIN', 'RESPONSAVEL_TECNICO', 'ANALISTA'], path: 'relatorios'
        },
    },

    // out

    {
        path: 'app/download',
        loadComponent: () => import('./app-download/app-download.component').then(a => a.AppDownloadComponent),
    },


    //dashboards
    {
        path: 'dashboard',
        loadComponent: () => import('./features/dashboard/home/dashboard.component').then(d => d.DashboardComponent),
        canActivate: [AuthGuard],
        data: {
            title: 'Dashboard',
            icon: 'pi pi-home text-blue-500',
            role: [], path: 'dashboard', hideBackButton: true
        },
    },

    {
        path: 'dashboard/mapa-execucoes',
        loadComponent: () => import('./features/dashboard/map/map.component').then(d => d.MapComponent),
        canActivate: [AuthGuard],
        data: {
            title: 'Mapa de Execuções',
            icon: 'pi pi-map-marker text-blue-500',
            role: ['ADMIN', 'ANALISTA', 'SUPPORT', 'RESPONSAVEL_TECNICO'], path: 'dashboard'
        },
    },
    {
        path: 'dashboard/visao-executiva',
        loadComponent: () => import('./features/dashboard/executive/executive.component').then(d => d.ExecutiveComponent),
        canActivate: [AuthGuard],
        data: {
            title: 'Dashboard de Visão Executiva',
            icon: 'pi pi-compass text-purple-700',
            role: ['ADMIN', 'ANALISTA', 'SUPPORT', 'RESPONSAVEL_TECNICO'], path: 'dashboard'
        },
    },
    {
        path: 'dashboard/produtividade-equipe',
        loadComponent: () => import('./features/dashboard/team/team-operational-dashboard.component').then(d => d.TeamOperationalDashboardComponent),
        canActivate: [AuthGuard],
        data: {
            title: 'Dashboard de Produtividade da Equipe',
            icon: 'pi pi-users text-green-500',
            role: ['ADMIN', 'ANALISTA', 'SUPPORT', 'RESPONSAVEL_TECNICO'], path: 'dashboard'
        },
    },

    {
        path: 'abrir-chamado',
        component: ServiceRequestMapComponent,
        data: {
            title: 'Abrir Chamado',
            icon: 'pi pi-phone text-blue-500',
            mode: 'manual'
        }
    },

    {
        path: 'modo-ronda',
        component: ServiceRequestMapComponent,
        data: {
            title: 'Modo Ronda',
            icon: 'pi pi-map text-blue-500',
            mode: 'round'
        }
    },


    {
        path: 'status/:type',
        component: ServerErrorComponent,
    },

    //

    {
        path: '',
        redirectTo: 'dashboard',
        pathMatch: 'full'
    },
    {
        path: '**',
        redirectTo: "status/404"
    },


];
