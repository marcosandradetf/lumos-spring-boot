import { Component } from '@angular/core';
import {MaterialService} from '../../services/material.service';
import {EstoqueService} from '../../services/estoque.service';
import {MaterialResponse} from '../../material-response.dto';
import {tap} from 'rxjs';

@Component({
  selector: 'app-stock-movement',
  standalone: true,
  imports: [],
  templateUrl: './stock-movement.component.html',
  styleUrl: './stock-movement.component.scss'
})
export class StockMovementComponent {
  sidebarLinks = [
    { title: 'Gerenciar', path: '/estoque/materiais', id: 'opt1' },
    { title: 'Movimentar Estoque', path: '/estoque/movimento', id: 'opt2' },
    { title: 'Entrada de Nota Fiscal', path: '/estoque/entrada', id: 'opt3' },
    { title: 'Importar Material (.xlsx)', path: '/estoque/importar', id: 'opt4' },
    { title: 'Sugestão de Compra', path: '/estoque/sugestao', id: 'opt5' }
  ];
  materials: MaterialResponse[] = [];

  constructor(private materialService: MaterialService,
              private estoqueService: EstoqueService,) {
    this.loadMaterials();
  }

  private loadMaterials() {
    this.materialService.materials$.subscribe((materials: MaterialResponse[]) => {
      this.materials = materials;
    });
  }
}
