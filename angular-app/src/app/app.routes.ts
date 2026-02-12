// app/app.routes.ts
import {Routes} from '@angular/router';
import {AuthGuard} from './core/auth/auth.guard';
import {TruckDepositComponent} from './stock/truck-deposits/deposits.component';


export const routes: Routes = [
    // start path login
    {
        path: 'auth/login',
        loadComponent: () => import('./core/auth/pages/login/login.component').then(m => m.LoginComponent)
    },
    // end

    // doc
    {
        path: 'documentacao',
        loadComponent: () => import('./doc/doc.component').then(m => m.DocComponent)
    },

    {
        path: 'acesso-negado/:section',
        loadComponent: () => import('./shared/components/no-access/no-access.component').then(n => n.NoAccessComponent),
        canActivate: [AuthGuard],
        data: {role: [''], path: ''},
    },

    // start paths stock
    {
        path: 'estoque/cadastrar-material',
        loadComponent: () => import('./stock/material-form/material-form.component').then(m => m.MaterialFormComponent),
        canActivate: [AuthGuard],
        data: {role: ['ADMIN', 'ESTOQUISTA', 'ESTOQUISTA_CHEFE', 'ANALISTA', 'RESPONSAVEL_TECNICO'], path: 'estoque'},
    },
    {
        path: 'estoque/editar-material',
        loadComponent: () => import('./stock/material-form/material-form.component').then(m => m.MaterialFormComponent),
        canActivate: [AuthGuard],
        data: {role: ['ADMIN', 'ESTOQUISTA', 'ESTOQUISTA_CHEFE', 'ANALISTA', 'RESPONSAVEL_TECNICO'], path: 'estoque'},
    },
    {
        path: 'estoque/catalogo-materiais',
        loadComponent: () => import('./stock/material/material-page.component').then(m => m.MaterialPageComponent),
        canActivate: [AuthGuard],
        data: {role: ['ADMIN', 'ESTOQUISTA', 'ESTOQUISTA_CHEFE', 'ANALISTA', 'RESPONSAVEL_TECNICO'], path: 'estoque'},
    },
    {
        path: 'estoque/grupos',
        loadComponent: () => import('./stock/groups/groups.component').then(m => m.GroupsComponent),
        canActivate: [AuthGuard],
        data: {role: ['ADMIN', 'ESTOQUISTA', 'ESTOQUISTA_CHEFE'], path: 'estoque'},
    },
    {
        path: 'estoque/tipos',
        loadComponent: () => import('./stock/types/types.component').then(m => m.TypesComponent),
        canActivate: [AuthGuard],
        data: {role: ['ADMIN'], path: 'estoque'},
    },
    {
        path: 'estoque/movimentar-estoque',
        loadComponent: () => import('./stock/stock-movement/stock-movement.component').then(m => m.StockMovementComponent),
        canActivate: [AuthGuard],
        data: {role: ['ADMIN', 'ESTOQUISTA', 'ESTOQUISTA_CHEFE'], path: 'estoque'},
    },
    {
        path: 'estoque/movimentar-estoque-pendente',
        loadComponent: () => import('./stock/stock-movement-pending/stock-movement-pending.component').then(m => m.StockMovementPendingComponent),
        canActivate: [AuthGuard],
        data: {role: ['ADMIN', 'ESTOQUISTA_CHEFE'], path: 'estoque'},
    },
    {
        path: 'estoque/movimentar-estoque-aprovado',
        loadComponent: () => import('./stock/stock-movement-approvated/stock-movement-approvated.component').then(m => m.StockMovementApprovatedComponent),
        canActivate: [AuthGuard],
        data: {role: ['ADMIN', 'ESTOQUISTA', 'ESTOQUISTA_CHEFE'], path: 'estoque'},
    },
    {
        path: 'estoque/almoxarifados',
        loadComponent: () => import('./stock/deposits/deposits.component').then(m => m.DepositsComponent),
        canActivate: [AuthGuard],
        data: {role: ['ADMIN', 'ANALISTA', 'ESTOQUISTA', 'ESTOQUISTA_CHEFE'], path: 'estoque'},
    },

    {
        path: 'estoque/caminhoes',
        loadComponent: () => import('./stock/truck-deposits/deposits.component').then(d => d.TruckDepositComponent),
        canActivate: [AuthGuard],
        data: {role: ['ADMIN', 'ANALISTA', 'ESTOQUISTA', 'ESTOQUISTA_CHEFE'], path: 'estoque'},
    },
    {
        path: 'configuracoes/importar-planilha',
        loadComponent: () => import('./stock/import-materials/import-materials.component').then(m => m.ImportMaterialsComponent),
        canActivate: [AuthGuard],
        data: {role: ['ADMIN', 'ESTOQUISTA', 'ESTOQUISTA_CHEFE'], path: 'estoque'},
    },


    {path: 'estoque', redirectTo: 'estoque/materiais'},
    // end

    // start contract paths
    {path: 'contratos', redirectTo: 'contratos/dashboard'},
    {
        path: 'contratos/dashboard',
        loadComponent: () => import('./contract/pages/dashboard/dashboard.component').then(m => m.DashboardComponent),
        canActivate: [AuthGuard],
        data: {role: ['ADMIN', 'ANALISTA'], path: 'contratos'},
    },
    {
        path: 'contratos/criar',
        loadComponent: () => import('./contract/pages/create/create.component').then(m => m.CreateComponent),
        canActivate: [AuthGuard],
        data: {role: ['ADMIN', 'ANALISTA'], path: 'contratos'},
    },
    {
        path: 'contratos/editar',
        loadComponent: () => import('./contract/pages/create/create.component').then(m => m.CreateComponent),
        canActivate: [AuthGuard],
        data: {role: ['ADMIN', 'ANALISTA'], path: 'contratos'},
    },
    {
        path: 'contratos/listar',
        loadComponent: () => import('./contract/pages/list/contract-list/contract-list.component').then(c => c.ContractListComponent),
        canActivate: [AuthGuard],
        data: {role: ['ADMIN', 'ANALISTA'], path: 'contratos'},
    },
    // end

    // start settings path
    {
        path: 'configuracoes/dashboard',
        loadComponent: () => import('./manage/settings/settings.component').then(s => s.SettingsComponent),
        canActivate: [AuthGuard],
        data: {role: ['ADMIN', 'ANALISTA'], path: 'configuracoes'},
    },
    {
        path: 'configuracoes/usuarios',
        loadComponent: () => import('./manage/user/user.component').then(m => m.UserComponent),
        canActivate: [AuthGuard],
        data: {role: ['ADMIN', 'ANALISTA'], path: 'configuracoes'},
    },
    {
        path: 'configuracoes/equipes',
        loadComponent: () => import('./manage/team/team.component').then(m => m.TeamComponent),
        canActivate: [AuthGuard],
        data: {role: ['ADMIN', 'ANALISTA'], path: 'configuracoes'},
    },
    {
        path: 'configuracoes/empresa',
        loadComponent: () => import('./manage/team/team.component').then(m => m.TeamComponent),
        canActivate: [AuthGuard],
        data: {role: ['ADMIN', 'ANALISTA'], path: 'configuracoes'},
    },
    {
        path: 'configuracoes/conta',
        loadComponent: () => import('./manage/account/account.component').then(a => a.AccountComponent),
        canActivate: [AuthGuard],
        data: {role: [], path: 'conta'},
    },
    // end

    // start executions/pre-measurements path
    {
        path: 'pre-medicao/:status',
        loadComponent: () => import('./pre-measurement/pre-measurement-home/pre-measurement.component').then(p => p.PreMeasurementComponent),
        canActivate: [AuthGuard],
        data: {role: ['ADMIN', 'ANALISTA', 'RESPONSAVEL_TECNICO'], path: 'execucoes'},
    },
    {
        path: 'pre-medicao/relatorio/:id/:step',
        loadComponent: () => import('./pre-measurement/pre-measurement-report/pre-measurement-report.component').then(p => p.PreMeasurementReportComponent),
        canActivate: [AuthGuard],
        data: {role: ['ADMIN', 'ANALISTA', 'RESPONSAVEL_TECNICO'], path: 'execucoes'},
    },
    {
        path: 'pre_medicao/editar',
        loadComponent: () => import('./pre-measurement/pre-measurement-edit/pre-measurement-edit.component').then(p => p.PreMeasurementEditComponent),
        canActivate: [AuthGuard],
        data: {role: ['ADMIN', 'ANALISTA', 'RESPONSAVEL_TECNICO'], path: 'execucoes'},
    },
    {
        path: 'pre-medicao/importar/contrato/:id',
        loadComponent: () => import('./pre-measurement/import-pre-measurements/import-pre-measurements.component').then(p => p.ImportPreMeasurementsComponent),
        canActivate: [AuthGuard],
        data: {role: ['ADMIN', 'ANALISTA', 'RESPONSAVEL_TECNICO'], path: 'execucoes'},
    },
    {
        path: 'pre_medicao/visualizar',
        loadComponent: () => import('./pre-measurement/pre-measurement-view/pre-measurement-view.component').then(p => p.PreMeasurementViewComponent),
        canActivate: [AuthGuard],
        data: {role: ['ADMIN', 'ANALISTA', 'RESPONSAVEL_TECNICO'], path: 'execucoes'},
    },
    //

    // start executions/executions path
    {
        path: 'execucao/pre-medicao/:id',
        loadComponent: () => import('./executions/pre-measurement-available/measurement-details.component').then(m => m.MeasurementDetailsComponent),
        canActivate: [AuthGuard],
        data: {role: ['ADMIN', 'ANALISTA', 'RESPONSAVEL_TECNICO'], path: 'execucoes'},
    },

    {
        path: 'execucoes/iniciar-sem-pre-medicao',
        loadComponent: () => import('./executions/execution-no-pre-measurement/execution-no-pre-measurement.component').then(e => e.ExecutionNoPreMeasurementComponent),
        canActivate: [AuthGuard],
        data: {role: ['ADMIN', 'ANALISTA', 'RESPONSAVEL_TECNICO'], path: 'execucoes'},
    },

    {
        path: 'execucoes/:status',
        loadComponent: () => import('./executions/execution-progress/execution-progress.component').then(e => e.ExecutionProgressComponent),
        canActivate: [AuthGuard],
        data: {role: ['ADMIN', 'ANALISTA', 'RESPONSAVEL_TECNICO'], path: 'execucoes'},
    },
    //end

    // start request path
    {
        path: 'requisicoes/instalacoes/gerenciamento-estoque',
        loadComponent: () => import('./requests/reservation-management/reservation-management.component').then(r => r.ReservationManagementComponent),
        canActivate: [AuthGuard],
        data: {role: ['ESTOQUISTA', 'ESTOQUISTA_CHEFE'], path: 'gerenciamento-reservas'},
    },
    {
        path: 'requisicoes/gerenciamento/execucao',
        loadComponent: () => import('./requests/reservation-management-select/reservation-management-select.component').then(r => r.ReservationManagementSelectComponent),
        canActivate: [AuthGuard],
        data: {role: ['ESTOQUISTA', 'ESTOQUISTA_CHEFE'], path: 'gerenciamento-reservas'},
    },
    {
        path: 'requisicoes',
        loadComponent: () => import('./requests/reservation-pending/reservation-pending.component').then(r => r.ReservationPendingComponent),
        canActivate: [AuthGuard],
        data: {role: ['ESTOQUISTA', 'ESTOQUISTA_CHEFE'], path: 'gerenciamento-reservas'},
    },
    //end

    //start maintenance
    {
        path: 'manutencoes/nova',
        loadComponent: () => import('./maintenance/maintenance.create/maintenance.create.component').then(m => m.MaintenanceCreateComponent),
        canActivate: [AuthGuard],
        data: {role: ['ADMIN', 'RESPONSAVEL_TECNICO', 'ANALISTA',], path: 'gerenciamento-reservas'},
    },

    //reports
    {
        path: 'relatorios/gerenciamento',
        loadComponent: () => import('./reports/manage/report-manage.component').then(r => r.ReportManageComponent),
        canActivate: [AuthGuard],
        data: {
            role: ['ADMIN', 'RESPONSAVEL_TECNICO', 'ANALISTA', 'ESTOQUISTA', 'ESTOQUISTA_CHEFE'],
            path: 'relatorios'
        },
    },
    {
        path: 'relatorios/manutencoes',
        loadComponent: () => import('./reports/maintenance/maintenance.component').then(r => r.MaintenanceComponent),
        canActivate: [AuthGuard],
        data: {
            role: ['ADMIN', 'RESPONSAVEL_TECNICO', 'ANALISTA', 'ESTOQUISTA', 'ESTOQUISTA_CHEFE'],
            path: 'relatorios'
        },
    },
    {
        path: 'relatorios/instalacoes',
        loadComponent: () => import('./reports/installation/installation.component').then(r => r.InstallationComponent),
        canActivate: [AuthGuard],
        data: {role: ['ADMIN', 'RESPONSAVEL_TECNICO', 'ANALISTA',], path: 'relatorios'},
    },

    // out

    {
        path: 'app/download',
        loadComponent: () => import('./app-download/app-download.component').then(a => a.AppDownloadComponent),
    },


    {
        path: 'dashboard',
        loadComponent: () => import('./dashboard/dashboard.component').then(d => d.DashboardComponent),
        canActivate: [AuthGuard],
        data: {role: [], path: 'dashboard'},
    },
    {path: '**', redirectTo: 'dashboard'},


];
