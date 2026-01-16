import {Component, OnInit} from '@angular/core';
import {CurrencyPipe, DatePipe, NgForOf, NgIf} from '@angular/common';
import {StockMovementResponse} from '../dto/stock-movement-response.dto';
import {StockService} from '../services/stock.service';
import {Title} from '@angular/platform-browser';
import {Steps} from 'primeng/steps';
import {MenuItem, PrimeTemplate} from 'primeng/api';
import {PrimeBreadcrumbComponent} from '../../shared/components/prime-breadcrumb/prime-breadcrumb.component';
import {TableModule} from 'primeng/table';
import {Toast} from 'primeng/toast';

@Component({
    selector: 'app-stock-movement-approvated',
    standalone: true,
    imports: [
        NgForOf,
        Steps,
        CurrencyPipe,
        DatePipe,
        NgIf,
        PrimeBreadcrumbComponent,
        PrimeTemplate,
        TableModule,
        Toast
    ],
    templateUrl: './stock-movement-approvated.component.html',
    styleUrl: './stock-movement-approvated.component.scss'
})
export class StockMovementApprovatedComponent implements OnInit {
    sidebarLinks = [
        {title: 'Gerenciar', path: '/estoque/materiais', id: 'opt1'},
        {title: 'Movimentar Estoque', path: '/estoque/movimento', id: 'opt2'},
        {title: 'Entrada de Nota Fiscal', path: '/estoque/entrada', id: 'opt3'},
        {title: 'Importar Material (.xlsx)', path: '/estoque/importar', id: 'opt4'},
        {title: 'Sugestão de Compra', path: '/estoque/sugestao', id: 'opt5'}
    ];

    stockMovement: StockMovementResponse[] = [];
    items: MenuItem[] | undefined;

    constructor(private stockService: StockService, private title: Title) {
        this.title.setTitle('Estoque - Aprovado');
        this.stockService.getStockMovementApproved().subscribe(
            stockMovement => {
                this.stockMovement = stockMovement;
            }
        );
    }

    ngOnInit() {
        this.items = [
            {
                label: 'Selecionar itens',
                routerLink: '/estoque/movimentar-estoque',
                routerLinkActiveOptions: {exact: true}
            },
            {
                label: 'Pendente de Aprovação',
                routerLink: '/estoque/movimentar-estoque-pendente'
            },
            {
                label: 'Aprovado',
                routerLink: '/estoque/movimentar-estoque-aprovado',
            },
        ];
        this.isMobile = window.innerWidth <= 1024;
    }

    protected readonly Number = Number;
    isMobile = false;
}
