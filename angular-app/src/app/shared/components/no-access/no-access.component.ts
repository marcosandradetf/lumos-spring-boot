import {Component, OnInit} from '@angular/core';
import {SidebarComponent} from '../sidebar/sidebar.component';
import {ActivatedRoute} from '@angular/router';

@Component({
  selector: 'app-no-access',
  standalone: true,
  imports: [
    SidebarComponent
  ],
  templateUrl: './no-access.component.html',
  styleUrl: './no-access.component.scss'
})
export class NoAccessComponent implements OnInit {
  sidebarLinks: {title:string, path: string, id: string}[] = [];
  title: string = '';

  constructor(private route: ActivatedRoute) {
  }

  ngOnInit(): void {
    this.route.paramMap.subscribe(params => {
      switch (params.get('section')) {
        case 'estoque':
          this.title = "Estoque";
          this.sidebarLinks =  [
            {title: 'Gerenciar', path: '/estoque/materiais', id: 'opt1'},
            {title: 'Movimentar Estoque', path: '/estoque/movimento', id: 'opt2'},
            {title: 'Entrada de Nota Fiscal', path: '/estoque/entrada', id: 'opt3'},
            {title: 'Importar Material (.xlsx)', path: '/estoque/importar', id: 'opt4'},
            {title: 'Sugestão de Compra', path: '/estoque/sugestao', id: 'opt5'}
          ];
          break;
        case 'configuracoes':
          this.title = "Configurações";
          this.sidebarLinks = [
            {title: 'Início', path: '/configuracoes', id: 'opt1'},
            {title: 'Usuários', path: '/configuracoes/usuarios', id: 'opt2'},
            {title: 'Equipes', path: '/configuracoes/equipes', id: 'opt3'},
            {title: 'Minha Empresa', path: '/configuracoes/empresa', id: 'opt4'},
          ];
          break;
        default:
          break;
      }
    });
  }

}
