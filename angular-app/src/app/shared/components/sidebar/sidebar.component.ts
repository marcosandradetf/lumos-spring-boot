import {Component, EventEmitter, Input, model, OnInit, Output} from '@angular/core';
import {NgClass, NgForOf, NgIf} from '@angular/common';
import {Router, RouterLink, RouterLinkActive} from '@angular/router';
import {Menubar} from 'primeng/menubar';
import {Toast} from 'primeng/toast';
import {PanelMenu} from 'primeng/panelmenu';
import {MenuItem, MenuItemCommandEvent} from 'primeng/api';
import {UtilsService} from '../../../core/service/utils.service';
import {Menu} from 'primeng/menu';
import {Badge} from 'primeng/badge';
import {Ripple} from 'primeng/ripple';

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [
    PanelMenu,
    NgIf,
    NgForOf,
    RouterLinkActive,
    RouterLink
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
  itemsOnlyIcons: MenuItem[] | undefined;

  constructor(private utils: UtilsService, private router: Router) {
  }

  ngOnInit(): void {
    this.utils.menuState$.subscribe((isOpen: boolean) => {
      this.menuOpen = isOpen;
    });

    // Verifica se existe algum valor salvo no localStorage
    let savedMenuState = localStorage.getItem('menuOpen');
    if (savedMenuState !== null) {
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
          border: 'none'
        },
        label: 'Pré-Medições',
        expanded: this.bTogglePreMeasurement,
        command: () => {
          this.togglePreMeasurement(!this.bTogglePreMeasurement);
        },
        items: [
          {
            label: 'Aguardando Confirmação',
            icon: 'pi pi-inbox text-neutral-800 dark:text-neutral-200',
            routerLink: 'pre-medicao/aguardando-retorno',
          },
          {
            label: 'Disponível para execução',
            icon: 'pi pi-check-circle text-neutral-800 dark:text-neutral-200',
            routerLink: 'pre-medicao/disponivel',
          },
          {
            label: 'Importar pré-medição (.xlx)',
            icon: 'pi pi-file-excel text-neutral-800 dark:text-neutral-200',
            routerLink: ['/contratos/listar'],
            queryParams: { for: 'preMeasurement' }
          },
        ]
      },
      {
        style: {
          border: 'none'
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
            queryParams: { for: 'execution' },
            routerLinkActiveOptions: { exact: true },
          },
          {
            label: 'Em progresso',
            icon: 'pi pi-spinner  text-neutral-800 dark:text-neutral-200',
            routerLink: ['/execucoes/em-progresso'],
            routerLinkActiveOptions: { exact: true },
          },
          {
            label: 'Finalizada',
            icon: 'pi pi-check text-neutral-800 dark:text-neutral-200',
            routerLink: ['/execucoes/finalizadas'],
          },
        ]
      },
      {
        style: {
          border: 'none'
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
          },
          {
            label: 'Materiais pendentes de Aprovação',
            icon: 'fa-solid fa-hourglass-half text-base text-neutral-800 dark:text-neutral-200',
            routerLink: ['/requisicoes'],
            queryParams: { status: 'PENDING' },
          },
          {
            label: 'Materiais disponíveis para Coleta',
            icon: 'fa-solid fa-list-check text-base text-neutral-800 dark:text-neutral-200',
            routerLink: ['/requisicoes'],
            queryParams: { status: 'APPROVED' },
          },
        ]
      },
      {
        style: {
          border: 'none'
        },
        label: 'Relatórios',
        expanded: this.bToggleReports,
        command: () => {
          this.toggleReports(!this.bToggleReports);
        },
        items: [
          {
            disabled: true,
            label: 'Pré-medição',
            icon: 'pi pi-chart-bar text-black text-neutral-800 dark:text-neutral-200',
            routerLink: ['/relatorios/manutencoes'],
          },
          {
            label: 'Manutenções',
            icon: 'fa-solid fa-wrench text-base text-neutral-800 dark:text-neutral-200',
            routerLink: ['/relatorios/manutencoes'],
          },
          {
            label: 'Instalações',
            icon: 'pi pi-lightbulb text-black text-neutral-800 dark:text-neutral-200',
            routerLink: ['/relatorios/instalacoes'],
          },
        ]
      },
      {
        style: {
          border: 'none'
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
          },
          {
            label: 'Exibir Contratos',
            icon: 'pi pi-folder-open text-neutral-800 dark:text-neutral-200',
            routerLink: ['/contratos/listar'],
            queryParams: { for: 'view' },
          },
        ]
      },
      {
        style: {
          border: 'none'
        },
        label: 'Estoque',
        expanded: this.bToggleStock,
        command: () => {
          this.toggleStock(!this.bToggleStock);
        },
        items: [
          {
            label: 'Movimentar Estoque',
            icon: 'pi pi-box text-neutral-800 dark:text-neutral-200',
            routerLink: ['/estoque/movimento'],
          },
          {
            label: 'Movimentações Pendentes',
            icon: 'pi pi-clock text-neutral-800 dark:text-neutral-200',
            routerLink: ['/estoque/movimento-pendente'],
          },
          {
            label: 'Gerenciar Materiais',
            icon: 'pi pi-briefcase text-neutral-800 dark:text-neutral-200',
            routerLink: ['/estoque/materiais'],
          },
          {
            label: 'Gerenciar Almoxarifados',
            icon: 'pi pi-warehouse text-neutral-800 dark:text-neutral-200',
            routerLink: ['/estoque/almoxarifados'],
          },
          {
            label: 'Gerenciar Caminhões',
            icon: 'pi pi-truck text-neutral-800 dark:text-neutral-200',
            routerLink: ['/estoque/caminhoes'],
          },
        ]
      },
      {
        style: {
          border: 'none'
        },
        label: 'Configurações',
        expanded: false,
        items: [
          {
            label: 'Usuários',
            icon: 'pi pi-users text-neutral-800 dark:text-neutral-200',
            routerLink: ['/configuracoes/usuarios'],
          },
          {
            label: 'Minha Conta',
            icon: 'pi pi-user text-neutral-800 dark:text-neutral-200',
            routerLink: ['/configuracoes/conta'],
          },
          {
            label: 'Equipes Operacionais',
            icon: 'pi pi-sitemap  text-neutral-800 dark:text-neutral-200',
            routerLink: ['/configuracoes/equipes'],
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
}
