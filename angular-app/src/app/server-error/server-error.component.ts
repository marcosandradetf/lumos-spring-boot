import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Title } from '@angular/platform-browser';
import {Location, NgClass, NgIf} from '@angular/common';
import {SharedState} from '../core/service/shared-state';

@Component({
    selector: 'app-server-error',
    standalone: true,
    templateUrl: './server-error.component.html',
    imports: [
        NgIf,
        NgClass
    ],
    styleUrl: './server-error.component.scss'
})
export class ServerErrorComponent implements OnInit {

    type: string = '500';

    config = {
        code: '500',
        title: 'O servidor não respondeu',
        message: 'Estamos enfrentando uma instabilidade temporária. Nossa equipe já foi notificada e está trabalhando para normalizar o sistema o mais rápido possível.',
        badge: 'Instabilidade detectada',
        colorClass: 'text-red-500',
        bgClass: 'bg-red-100 dark:bg-red-900/30',
        icon: 'pi pi-exclamation-triangle',
        glow: 'bg-red-500/20'
    };

    constructor(
        private route: ActivatedRoute,
        private router: Router,
        private title: Title,
        private location: Location
    ) {}

    ngOnInit() {
        this.type = this.route.snapshot.paramMap.get('type') || '500';

        this.resolveType();

        this.title.setTitle(this.config.title);
        SharedState.setCurrentPath([this.config.title]);
    }

    resolveType() {
        switch (this.type) {

            case 'offline':
                this.config = {
                    code: '0',
                    title: 'Sem conexão com o servidor',
                    message: 'Não foi possível conectar ao servidor. Verifique sua internet ou tente novamente.',
                    badge: 'Sem conexão',

                    colorClass: 'text-orange-500',
                    bgClass: 'bg-orange-100 dark:bg-orange-900/30',
                    icon: 'pi pi-wifi',

                    glow: 'bg-orange-500/20'
                };
                break;

            case '404':
                this.config = {
                    code: '404',
                    title: 'Página não encontrada',
                    message: 'A página que você tentou acessar não existe.',
                    badge: 'Recurso não encontrado',

                    colorClass: 'text-blue-500',
                    bgClass: 'bg-blue-100 dark:bg-blue-900/30',
                    icon: 'pi pi-search',

                    glow: 'bg-blue-500/20'
                };
                break;

            case '403':
                this.config = {
                    code: '403',
                    title: 'Acesso negado',
                    message: 'Você não possui permissão para acessar este recurso. Caso acredite que isso seja um erro, entre em contato com um administrador.',
                    badge: 'Permissão insuficiente',

                    colorClass: 'text-yellow-500',
                    bgClass: 'bg-yellow-100 dark:bg-yellow-900/30',
                    icon: 'pi pi-lock',

                    glow: 'bg-yellow-500/20'
                };
                break;

            default: // 500
                this.config = {
                    code: '500',
                    title: 'O servidor não respondeu',
                    message: 'Estamos enfrentando uma instabilidade temporária.',
                    badge: 'Instabilidade detectada',

                    colorClass: 'text-red-500',
                    bgClass: 'bg-red-100 dark:bg-red-900/30',
                    icon: 'pi pi-exclamation-triangle',

                    glow: 'bg-red-500/20'
                };
        }
    }



    goBack() {
        if (window.history.length > 1) {
            this.location.back();
        } else {
            void this.router.navigate(['/dashboard']);
        }
    }

    goHome() {
        void this.router.navigate(['/dashboard']);
    }
}
