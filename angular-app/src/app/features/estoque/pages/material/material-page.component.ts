import {Component, inject, OnInit} from '@angular/core';
import {MaterialResponse} from '../../material-response.dto';
import {MaterialService} from '../../services/material.service';
import { CommonModule } from '@angular/common'; // Importando o CommonModule
import { RouterModule } from '@angular/router';
import {DeleteMaterialModalComponent} from '../../../../shared/components/modal-delete/delete.component'; // Importando o RouterModule

import {FormBuilder, FormControl, ReactiveFormsModule} from '@angular/forms';
import {toSignal} from '@angular/core/rxjs-interop';
import {map} from 'rxjs';
import {MaterialFormComponent} from '../../../../shared/components/material-form/material-form.component';
import {SidebarComponent} from "../../../../shared/components/sidebar/sidebar.component";
import {TabelaComponent} from '../../../../shared/components/tabela/tabela.component';
import {HeaderComponent} from '../../../../shared/components/header/header.component';
import {Router} from 'express';
import {EstoqueService} from '../../services/estoque.service'; // Importar MatSnackBar



@Component({
  selector: 'app-material-page',
  standalone: true,
  templateUrl: './material-page.component.html',
  styleUrls: ['./material-page.component.scss'],
  imports: [CommonModule, RouterModule, DeleteMaterialModalComponent, ReactiveFormsModule, MaterialFormComponent, SidebarComponent, TabelaComponent] // Adicionando os módulos aqui
})
export class MaterialPageComponent {
  materiais: MaterialResponse[] = []; // Inicializando a lista de materiais


  constructor(private materialService: MaterialService, private estoque: EstoqueService,) {}



}
