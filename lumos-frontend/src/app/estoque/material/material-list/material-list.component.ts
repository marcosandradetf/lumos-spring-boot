import { Component, OnInit } from '@angular/core';
import {Material} from '../material.model';
import {MaterialService} from '../material.service';
import { CommonModule } from '@angular/common'; // Importando o CommonModule
import { RouterModule } from '@angular/router'; // Importando o RouterModule


@Component({
  selector: 'app-material-list',
  standalone: true,
  templateUrl: './material-list.component.html',
  styleUrls: ['./material-list.component.scss'],
  imports: [CommonModule, RouterModule, ] // Adicionando os módulos aqui
})
export class MaterialListComponent implements OnInit {
  materials: Material[] = []; // Inicializando a lista de materiais

  constructor(private materialService: MaterialService) {} // Injeta o serviço

  ngOnInit(): void {
    this.getMaterials(); // Chama o método ao inicializar
  }

  getMaterials(): void {
    this.materialService.getAll().subscribe(
      (data) => {
        this.materials = data; // Armazena os materiais obtidos
      },
      (error) => {
        console.error('Erro ao obter materiais', error); // Tratamento de erro
      }
    );
  }

  deleteMaterial(id: number) {
    if (confirm('Tem certeza de que deseja deletar este material?')) {
      this.materialService.deleteMaterial(id).subscribe(() => {
        this.getMaterials(); // Recarrega os materiais após a exclusão
      });
    }
  }

  updateMaterial(id: number) {
    // Implementar lógica para atualizar o material
  }
}
