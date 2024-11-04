import {Component, inject, OnInit} from '@angular/core';
import {Material} from '../../../models/material.model';
import {MaterialService} from '../../../services/material.service';
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
import {MaterialFormComponent} from '../components/material-form/material-form.component';
import {SidebarComponent} from "../../components/sidebar/sidebar.component";
import {TabelaComponent} from '../components/tabela/tabela.component';
import {HeaderComponent} from '../../../shared/header/header.component';
import {Router} from 'express';
import {EstoqueService} from '../../../services/estoque.service'; // Importar MatSnackBar



@Component({
  selector: 'app-material-page',
  standalone: true,
  templateUrl: './material-page.component.html',
  styleUrls: ['./material-page.component.scss'],
  imports: [CommonModule, RouterModule, DeleteMaterialModalComponent, MatButton, MatFormField, MatSelect, MatOption, MatIcon, MatInput, ReactiveFormsModule, MatCheckbox, MatRadioGroup, MatRadioButton, MatLabel, MatSlideToggle, MaterialFormComponent, SidebarComponent, TabelaComponent] // Adicionando os módulos aqui
})
export class MaterialPageComponent {
  materiais: Material[] = []; // Inicializando a lista de materiais
  readonly dialog = inject(MatDialog);

  constructor(private materialService: MaterialService, private snackBar: MatSnackBar, private estoque: EstoqueService,) {}



}
