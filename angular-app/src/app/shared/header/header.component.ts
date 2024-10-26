import {Component, OnInit} from '@angular/core';
import {NgClass, NgIf} from '@angular/common';
import {Tipo} from '../../models/tipo.model';
import {Empresa} from '../../models/empresa.model';
import {Almoxarifado} from '../../models/almoxarifado.model';
import {BehaviorSubject} from 'rxjs';
import {EstoqueService} from '../../services/estoque.service';
import {RouterLink, RouterLinkActive} from '@angular/router';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [
    NgClass,
    NgIf,
    RouterLink,
    RouterLinkActive
  ],
  templateUrl: './header.component.html',
  styleUrl: './header.component.scss'
})
export class HeaderComponent implements OnInit{
  navOpen = false; // Controle para o menu
  accountMenuOpen = false; // Controle para o menu da conta
  onPath: string = ''; // Caminho atual

  constructor(private estoqueService: EstoqueService) {
  }

  ngOnInit(): void {
    this.estoqueService.onPath$.subscribe(path => {
      this.onPath = path; // Atualiza o caminho atual com base nas mudanças
    });
  }

  toggleNav() {
    this.navOpen = !this.navOpen;
  }

  toggleAccountMenu() {
    this.accountMenuOpen = !this.accountMenuOpen;
  }

  closeAccountMenu() {
    this.accountMenuOpen = false;
  }

}
