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
    this.loadMateriais()
  }

  loadMateriais(): void {
    this.materialService.materiais$.subscribe((materiais: Material[]) => {
      this.materiais = materiais;
    });

    this.materialService.getAll();
  }

  deleteMaterial(idMaterial: number, nomeMaterial: string): void {
    this.materialService.deleteMaterial(idMaterial).subscribe(() => {
      this.materialService.deleteMaterialFetch(idMaterial);
      this.showMessage("Material removido com sucesso!");
    }, error => {
      this.showMessage(error.message || "Erro ao remover material.");
    });
  }

  updateMaterial(idMaterial: number, material: Material): void {
    this.materialService.updateMaterial(idMaterial ,material).subscribe((materialAtualizado: Material) => {
      this.materialService.updateMaterialFetch(materialAtualizado);
      this.showMessage("Material removido com sucesso!");
    }, error => {
      this.showMessage(error.message || "Erro ao atualizar  material.");
    });
  }

  private showMessage(message: string) {

  }

}
