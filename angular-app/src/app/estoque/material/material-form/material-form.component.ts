import {Component, OnInit} from '@angular/core';
import {FormBuilder, FormGroup, FormsModule, NgForm, ReactiveFormsModule} from '@angular/forms';
import {MaterialService} from '../material.service';
import {MatSnackBar} from '@angular/material/snack-bar';
import {MatIcon, MatIconModule} from '@angular/material/icon';
import {MatInput, MatInputModule} from '@angular/material/input';
import {MatOption} from '@angular/material/core';
import {MatRadioButton, MatRadioGroup} from '@angular/material/radio';
import {MatSelectModule} from '@angular/material/select';
import {MatSlideToggleModule} from '@angular/material/slide-toggle';
import {NgForOf, NgIf} from '@angular/common';
import {MatCheckbox} from '@angular/material/checkbox';
import {MatFormFieldModule} from '@angular/material/form-field';
import {NgOptionComponent, NgSelectComponent} from '@ng-select/ng-select';
import {Tipos} from '../material.model';


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
    MatFormFieldModule, MatInputModule, MatIconModule, NgIf, NgSelectComponent, NgOptionComponent
  ],
  templateUrl: './material-form.component.html',
  styleUrl: './material-form.component.scss'
})
export class MaterialFormComponent implements OnInit {
  tipos: Tipos[] = [];
  grupos: any[] = [];
  empresas: any[] = [];
  almoxarifados: any[] = [];

  nomeMaterial: string = '';
  marcaMaterial: string = '';
  unidCompra: string = '';
  unidRequisicao: string = '';
  tipoMaterial: number = 0;
  grupoMaterial: number = 0;
  qtdeEstoque: number = 0;
  empresaMaterial: number = 0;
  materialInativo: boolean = false;
  formSubmitted: boolean = false;


  unidades: any[] = [
    { Value: "CX" },
    { Value: "PÇ" },
    { Value: "UN" },
    { Value: "M" },
    { Value: "CM" }
  ];



  constructor(
    private materialService: MaterialService,
  ) {  }

  ngOnInit(): void {
    this.materialService.getTipos().subscribe(tipos => this.tipos = tipos);
    this.materialService.getGrupos().subscribe(grupos => this.grupos = grupos);
    this.materialService.getEmpresas().subscribe(empresas => this.empresas = empresas);
    this.materialService.getAlmoxarifados().subscribe(almoxarifados => this.almoxarifados = almoxarifados);
  }

  onSubmit(form: NgForm) {
    this.formSubmitted = true;
    if (form.valid) {
      // Processar a submissão do formulário
      console.log('Formulário enviado com sucesso:', form.value);

      // Resetar o formulário após o envio bem-sucedido
      form.resetForm();

      // Redefinir a variável para falso após a submissão
      this.formSubmitted = false;
    }
  }

}
