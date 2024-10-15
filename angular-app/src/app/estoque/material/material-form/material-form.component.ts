import {Component, inject, OnInit} from '@angular/core';
import {FormBuilder, FormControl, FormGroup, FormsModule, ReactiveFormsModule} from '@angular/forms';
import {MaterialService} from '../material.service';
import {MatSnackBar} from '@angular/material/snack-bar';
import {MatIcon, MatIconModule} from '@angular/material/icon';
import {MatInput, MatInputModule} from '@angular/material/input';
import {MatOption} from '@angular/material/core';
import {MatRadioButton, MatRadioGroup} from '@angular/material/radio';
import {MatSelectModule} from '@angular/material/select';
import {MatSlideToggleModule} from '@angular/material/slide-toggle';
import {NgForOf} from '@angular/common';
import {MatCheckbox} from '@angular/material/checkbox';
import {MatFormFieldModule} from '@angular/material/form-field';

@Component({
  selector: 'app-material-form',
  standalone: true,
  imports: [
    FormsModule,
    MatIcon,
    MatInput,
    MatOption,
    MatRadioButton,
    MatRadioGroup,
    MatSlideToggleModule,
    ReactiveFormsModule,
    NgForOf,
    MatSelectModule,
    MatCheckbox,
    MatFormFieldModule, MatInputModule, MatIconModule
  ],
  templateUrl: './material-form.component.html',
  styleUrl: './material-form.component.scss'
})
export class MaterialFormComponent implements OnInit {
  materialForm: FormGroup;
  tipos: any[] = [];
  grupos: any[] = [];
  empresas: any[] = [];
  almoxarifados: any[] = [];

  constructor(
    private fb: FormBuilder,
    private materialService: MaterialService,
    private snackBar: MatSnackBar
  ) {
    this.materialForm = this.fb.group({
      nomeMaterial: [''],
      tipoMaterial: [''],
      inativo: [false],  // Controle para o slide toggle
      outroSelect: ['']
    });
  }

  ngOnInit(): void {
    this.materialService.getTipos().subscribe(tipos => this.tipos = tipos);
    this.materialService.getGrupos().subscribe(grupos => this.grupos = grupos);
    this.materialService.getEmpresas().subscribe(empresas => this.empresas = empresas);
    this.materialService.getAlmoxarifados().subscribe(almoxarifados => this.almoxarifados = almoxarifados);
  }

  onSubmit() {

  }
}
