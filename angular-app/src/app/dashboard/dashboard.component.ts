import {Component, OnInit} from '@angular/core';
import {Title} from '@angular/platform-browser';
import {PrimeBreadcrumbComponent} from "../shared/components/prime-breadcrumb/prime-breadcrumb.component";
import {SharedState} from '../core/service/shared-state';
import {NgClass, NgForOf, NgIf} from '@angular/common';
import {RouterLink} from '@angular/router';
import {DashboardService} from './dashboard.service';

@Component({
    selector: 'app-dashboard',
    standalone: true,
    imports: [PrimeBreadcrumbComponent, NgForOf, NgIf, RouterLink, NgClass],
    templateUrl: './dashboard.component.html',
    styleUrl: './dashboard.component.scss'
})
export class DashboardComponent implements OnInit {
    loading = false;
    actions: any[] = [
        {
            label: 'Relatórios de Manutenções',
            description: 'Estatísticas detalhadas',
            routerLink: '/relatorios/manutencoes'
        },
        {
            label: 'Relatórios de Instalações',
            description: 'Estatísticas detalhadas',
            routerLink: '/relatorios/instalacoes'
        },
        {
            label: 'Mapa das instalações',
            description: 'Visualização geográfica',
            routerLink: '/mapa/instalacoes'
        },
    ];

    metrics: any[] = [
        {
            label: 'Pré-medições',
            value: 0,
            description: 'Em aberto',
            routerLink: '/pre-medicao/pendente',
            queryParams: null
        },
        {
            label: 'Ordens de Serviço',
            value: 0,
            description: 'Em aberto',
            routerLink: '/requisicoes/instalacoes/gerenciamento-estoque',
            queryParams: null
        },
        {
            label: 'Instalações',
            value: 0,
            description: 'Em curso',
            routerLink: null,
            queryParams: null
        },
        {
            label: 'Contratos com baixo saldo',
            value: 0,
            description: 'Precisam de atenção',
            routerLink: '/contratos/listar',
            queryParams: {for: 'view'}
        },
        {
            label: 'Materiais com baixo estoque',
            value: 0,
            description: 'Precisam de atenção',
            routerLink: '/relatorios/manutencoes',
            queryParams: null
        },
        {
            label: 'Relatórios gerados',
            value: 321,
            description: 'Últimos 30 dias',
            routerLink: '/relatorios/manutencoes',
            queryParams: null
        },
    ];

    classificationClass: Record<string, string> = {
        'Ação imediata':
            'border-fuchsia-400 bg-fuchsia-50 text-fuchsia-800 ' +
            'dark:border-fuchsia-500/40 dark:bg-fuchsia-950/40 dark:text-fuchsia-300',

        'Crítico':
            'border-rose-400 bg-rose-50 text-rose-800 ' +
            'dark:border-rose-500/40 dark:bg-rose-950/40 dark:text-rose-300',

        'Atenção':
            'border-amber-400 bg-amber-50 text-amber-800 ' +
            'dark:border-amber-500/40 dark:bg-amber-950/40 dark:text-amber-300',

        'Monitorar':
            'border-cyan-400 bg-cyan-50 text-cyan-800 ' +
            'dark:border-cyan-500/40 dark:bg-cyan-950/40 dark:text-cyan-300',

        'Últimos 30 dias':
            'border-zinc-400 bg-zinc-50 text-zinc-800 ' +
            'dark:border-zinc-600/40 dark:bg-zinc-900/50 dark:text-zinc-300',
    };


    labelClasses: Record<string, string> = {
        'Ação imediata':
            'bg-fuchsia-600 text-white dark:bg-fuchsia-500',

        'Crítico':
            'bg-rose-600 text-white dark:bg-rose-500',

        'Atenção':
            'bg-amber-500 text-black dark:bg-amber-400 dark:text-black',

        'Monitorar':
            'bg-cyan-600 text-white dark:bg-cyan-500',

        'Últimos 30 dias':
            'bg-zinc-600 text-white dark:bg-zinc-500',
    };


    constructor(
        private titleService: Title,
        private service: DashboardService,
    ) {


    }

    ngOnInit() {
        this.titleService.setTitle("Lumos");
        SharedState.setCurrentPath(['Dashboard']);
        this.loading = true;
        this.loadMetrics();
    }

    loadMetrics() {
        this.service.getMetrics().subscribe({
            next: (data) => {
                this.metrics = data;
            },
            error: (error) => {
                this.loading = false;
            },
            complete: () => {
                this.loading = false;
            }
        })
    }

}
