import {Component, OnInit} from '@angular/core';
import {NgClass} from '@angular/common';
import {RouterLink} from '@angular/router';
import {EstoqueService} from '../../services/estoque.service';

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [
    NgClass,
    RouterLink
  ],
  templateUrl: './sidebar.component.html',
  styleUrl: './sidebar.component.scss'
})
export class SidebarComponent implements OnInit {
  menuOpen = false;
  onPath: string = '';

  constructor(private estoqueService: EstoqueService) {
  }

  ngOnInit(): void {
    this.estoqueService.onPathSideBar$.subscribe(path => {
      this.onPath = path; // Atualiza o caminho atual com base nas mudanças
    });
  }

  toggleMenu() {
    this.menuOpen = !this.menuOpen;
  }
}
