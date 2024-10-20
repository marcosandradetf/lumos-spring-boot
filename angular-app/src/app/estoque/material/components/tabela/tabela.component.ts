import { Component } from '@angular/core';
import {NgForOf} from "@angular/common";
import {DeleteMaterialModalComponent} from '../modal-delete/delete.component';
import {Material} from '../../material.model';

@Component({
  selector: 'app-tabela',
  standalone: true,
    imports: [
        NgForOf
    ],
  templateUrl: './tabela.component.html',
  styleUrl: './tabela.component.scss'
})
export class TabelaComponent {
  materiais: Material[] = [];

  deleteMaterial(idMaterial: number, nomeMaterial: string): void {

  }

  updateMaterial(idMaterial: number) {

  }

}
