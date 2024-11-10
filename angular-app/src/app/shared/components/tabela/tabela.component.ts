import {Component, OnInit} from '@angular/core';
import {NgForOf} from "@angular/common";
import {DeleteMaterialModalComponent} from '../modal-delete/delete.component';
import {MaterialResponse} from '../../../features/estoque/material-response.dto';
import {MaterialService} from '../../../features/estoque/services/material.service';

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
  materiais: MaterialResponse[] = [];

  constructor(private materialService: MaterialService) {
  }

  ngOnInit() {
    this.loadMateriais()
  }

  loadMateriais(): void {
    this.materialService.materiais$.subscribe((materiais: MaterialResponse[]) => {
      this.materiais = materiais;
    });

    this.materialService.getFetch();
  }

  deleteMaterial(idMaterial: number, nomeMaterial: string): void {
    this.materialService.deleteMaterial(idMaterial).subscribe(() => {
      this.materialService.deleteMaterialFetch(idMaterial);
      this.showMessage("Material removido com sucesso!");
    }, error => {
      this.showMessage(error.message || "Erro ao remover material.");
    });
  }

  updateMaterial(idMaterial: number, material: MaterialResponse): void {
    this.materialService.updateMaterial(idMaterial ,material).subscribe((materialAtualizado: MaterialResponse) => {
      this.materialService.updateMaterialFetch(materialAtualizado);
      this.showMessage("Material removido com sucesso!");
    }, error => {
      this.showMessage(error.message || "Erro ao atualizar  material.");
    });
  }

  private showMessage(message: string) {

  }

}
