import {Component, OnInit} from '@angular/core';
import {NgForOf} from "@angular/common";
import {DeleteMaterialModalComponent} from '../modal-delete/delete.component';
import {Material} from '../../../../models/material.model';
import {MaterialService} from '../../../../services/material.service';

@Component({
  selector: 'app-tabela',
  standalone: true,
    imports: [
        NgForOf
    ],
  templateUrl: './tabela.component.html',
  styleUrl: './tabela.component.scss'
})
export class TabelaComponent implements OnInit {
  materiais: Material[] = [];

  constructor(private materialService: MaterialService) {
  }

  ngOnInit() {
    this.LoadMateriais()
  }

  LoadMateriais(): void {
    this.materialService.getAll().subscribe((data: Material[]) => {
      this.materiais = data;
    }, error => {
      console.error('Erro ao carregar materiais:', error);
    });
  }


  deleteMaterial(idMaterial: number, nomeMaterial: string): void {

  }

  updateMaterial(idMaterial: number) {

  }

}
