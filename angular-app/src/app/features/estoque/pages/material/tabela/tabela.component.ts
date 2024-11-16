import {Component, OnInit} from '@angular/core';
import {NgClass, NgForOf} from "@angular/common";
import {DeleteMaterialModalComponent} from '../../../../../shared/components/modal-delete/delete.component';
import {MaterialResponse} from '../../../material-response.dto';
import {MaterialService} from '../../../services/material.service';

@Component({
  selector: 'app-tabela',
  standalone: true,
  imports: [
    NgForOf,
    NgClass
  ],
  templateUrl: './tabela.component.html',
  styleUrl: './tabela.component.scss'
})
export class TabelaComponent implements OnInit {
  materials: MaterialResponse[] = [];
  currentPage: string = "0";


  constructor(protected materialService: MaterialService) {
  }

  ngOnInit() {
    this.loadMateriais()
  }

  loadMateriais(): void {
    this.materialService.materials$.subscribe((materiais: MaterialResponse[]) => {
      this.materials = materiais;
    });

    this.materialService.getFetch(this.currentPage, "20");
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

  changePage(page: number): void {
    if (page.toString() !== this.currentPage) {
      this.currentPage = page.toString();
      this.materialService.getFetch(this.currentPage, "20");
    }
  }

  protected readonly parseInt = parseInt;
}
