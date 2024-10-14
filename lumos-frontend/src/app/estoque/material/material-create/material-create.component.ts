import {Component, inject, OnInit} from '@angular/core';
import {Material} from '../material.model';
import {MaterialService} from '../material.service';
import { CommonModule } from '@angular/common'; // Importando o CommonModule
import { RouterModule } from '@angular/router';
import {DeleteMaterialModalComponent} from '../components/modal-delete/delete.component'; // Importando o RouterModule
import {MatButton, MatButtonModule} from '@angular/material/button';
import {MatDialog} from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import {FloatLabelType, MatFormField, MatLabel} from '@angular/material/form-field';
import {MatOption, MatSelect} from '@angular/material/select';
import {MatIcon} from '@angular/material/icon';
import {MatInput} from '@angular/material/input';
import {FormBuilder, FormControl, ReactiveFormsModule} from '@angular/forms';
import {toSignal} from '@angular/core/rxjs-interop';
import {map} from 'rxjs';
import {MatCheckbox} from '@angular/material/checkbox';
import {MatRadioButton, MatRadioGroup} from '@angular/material/radio';
import {MatSlideToggle} from '@angular/material/slide-toggle';
import {MaterialFormComponent} from '../material-form/material-form.component'; // Importar MatSnackBar



@Component({
  selector: 'app-material-create',
  standalone: true,
  templateUrl: './material-create.component.html',
  styleUrls: ['./material-create.component.scss'],
  imports: [CommonModule, RouterModule, DeleteMaterialModalComponent, MatButton, MatFormField, MatSelect, MatOption, MatIcon, MatInput, ReactiveFormsModule, MatCheckbox, MatRadioGroup, MatRadioButton, MatLabel, MatSlideToggle, MaterialFormComponent] // Adicionando os módulos aqui
})
export class MaterialCreateComponent implements OnInit {
  materiais: Material[] = []; // Inicializando a lista de materiais
  readonly dialog = inject(MatDialog);



  constructor(private materialService: MaterialService, private snackBar: MatSnackBar) {}


  ngOnInit(): void {
    this.getMaterials(); // Chama o método ao inicializar
  }

  getMaterials(): void {
    this.materialService.getAll().subscribe(
      (data) => {
        this.materiais = data; // Armazena os materiais obtidos
      },
      (error) => {
        console.error('Erro ao obter materiais', error); // Tratamento de erro
      }
    );
  }

  deleteMaterial(id: number, name: string): void {
    const dialogRef = this.dialog.open(DeleteMaterialModalComponent, {
      data: { id, name }, // passa os dados para o modal
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.deleteItem(id); // chama a função de exclusão
      }
    });
  }

  deleteItem(id: number): void {
    this.materialService.deleteMaterial(id).subscribe(
      (response: any) => {  // Aqui você pode especificar um tipo mais adequado
        const message = response.message || `Item ${id} excluído com sucesso!`;
        this.openSnackBar(message, 'Fechar');
        this.getMaterials(); // Atualiza a lista de materiais após a exclusão
      },
      (error) => {
        // Aqui você pode tratar o erro e, se o servidor retornar uma mensagem de erro, você pode usá-la
        const errorMessage = error.error?.message || `Erro ao excluir o item ${id}.`;
        this.openSnackBar(errorMessage, 'Tentar Novamente');
      }
    );
  }

  openSnackBar(message: string, action: string): void {
    this.snackBar.open(message, action, {
      duration: 3000, // Duração em milissegundos
    });
  }

  updateMaterial(id: number) {
    // Implementar lógica para atualizar o material
  }

}
