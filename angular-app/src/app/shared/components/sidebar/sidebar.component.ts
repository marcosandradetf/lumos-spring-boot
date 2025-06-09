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
    Toast,
    PanelMenu,
    NgIf,
    Menu,
    Badge,
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


    this.items = [
      {
        style: {
          border: 'none'
        },
        label: 'Pré-Medições',
        icon: 'pi pi-chart-line',
        expanded: this.bTogglePreMeasurement,
        command: () => {
          this.togglePreMeasurement(!this.bTogglePreMeasurement);
        },
        items: [
          {
            label: 'Pendente',
            icon: 'pi pi-clock',
            command: () => {
              void this.router.navigate(['pre-medicao/pendente']);
            },
          },
          {
            label: 'Aguardando Retorno',
            icon: 'pi pi-inbox',
            command: () => {
              void this.router.navigate(['pre-medicao/aguardando-retorno']);
            },
          },
          {
            label: 'Validando',
            icon: 'pi pi-sync',
            command: () => {
              void this.router.navigate(['pre-medicao/validando']);
            },
          },
          {
            label: 'Disponível para execução',
            icon: 'pi pi-check-circle',
            command: () => {
              void this.router.navigate(['pre-medicao/disponivel']);
            },
          },
          {
            label: 'Importar pré-medição (.xlx)',
            icon: 'pi pi-file-excel',
            command: () => {
              void this.router.navigate(['contratos/listar'], {queryParams: {for: 'preMeasurement'}});
            },
          },
        ]
      },
      {
        style: {
          border: 'none'
        },
        label: 'Execuções',
        icon: 'pi pi-lightbulb',
        expanded: this.bToggleExecution,
        command: () => {
          this.toggleExecution();
        },
        items: [
          {
            label: 'Execução Sem Pré-Medição',
            icon: 'pi pi-chart-line',
            routerLink: ['/execucoes/iniciar-sem-pre-medicao'],
            routerLinkActiveOptions: { exact: true },
          },
          {
            label: 'Em progresso',
            icon: 'pi pi-spinner',
            routerLink: ['/execucoes/em-progresso'],
            routerLinkActiveOptions: { exact: true },
          },
          {
            label: 'Finalizada',
            icon: 'pi pi-check',
            command: () => {
              void this.router.navigate(['execucoes/finalizadas']);
            },
          },
        ]
      },
      {
        style: {
          border: 'none'
        },
        label: 'Requisições',
        icon: 'pi  pi-list',
        expanded: this.bToggleRequest,
        command: () => {
          this.toggleRequest();
        },
        items: [
          {
            label: 'Gerenciamento de Reservas',
            icon: 'pi pi-box',
            routerLink: ['/requisicoes/execucoes/reservas/gerenciamento'],
          },
        ]
      },
      {
        style: {
          border: 'none'
        },
        label: 'Contratos',
        icon: 'pi pi-folder',
        expanded: this.bToggleContracts,
        command: () => {
          this.toggleContracts(!this.bToggleContracts);
        },
        items: [
          {
            label: 'Novo contrato',
            icon: 'pi pi-plus-circle',
            command: () => {
              void this.router.navigate(['contratos/criar']);
            },
          },
          {
            label: 'Exibir Contratos',
            icon: 'pi pi-folder-open',
            command: () => {
              void this.router.navigate(['contratos/listar'], {queryParams: {for: 'view'}});
            },
          },
        ]
      },
      {
        style: {
          border: 'none'
        },
        label: 'Estoque',
        icon: 'pi pi-briefcase',
        expanded: this.bToggleStock,
        command: () => {
          this.toggleStock(!this.bToggleStock);
        },
        items: [
          {
            label: 'Movimentar Estoque',
            icon: 'pi pi-building',
            command: () => {
              void this.router.navigate(['estoque/movimento']);
            },
          },
          {
            label: 'Movimentações Pendentes',
            icon: 'pi pi-building',
            command: () => {
              void this.router.navigate(['estoque/movimento-pendente']);
            },
          },
          {
            label: 'Gerenciar Materiais',
            icon: 'pi pi-box',
            command: () => {
              void this.router.navigate(['estoque/materiais']);
            },
          },
          {
            label: 'Gerenciar Almoxarifados',
            icon: 'pi pi-warehouse',
            command: () => {
              void this.router.navigate(['estoque/almoxarifados']);
            },
          },
        ]
      },
      {
        style: {
          border: 'none'
        },
        label: 'Configurações',
        icon: 'pi pi-cog',
        expanded: false,
        items: [
          {
            label: 'Usuários',
            icon: 'pi pi-users',
            command: () => {
              void this.router.navigate(['configuracoes/usuarios']);
            },
          },
          {
            label: 'Minha Conta',
            icon: 'pi pi-user',
            command: () => {
              void this.router.navigate(['configuracoes/conta']);
            },
          },
          {
            label: 'Equipes Operacionais',
            icon: 'pi pi-sitemap',
            command: () => {
              void this.router.navigate(['configuracoes/equipes']);
            },
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


  protected readonly model = model;
}
