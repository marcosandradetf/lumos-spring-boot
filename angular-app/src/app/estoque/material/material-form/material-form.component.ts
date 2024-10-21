import {Component, OnDestroy, OnInit} from '@angular/core';
import {FormBuilder, FormGroup, FormsModule, NgForm, ReactiveFormsModule} from '@angular/forms';
import {MaterialService} from '../../../services/material.service';
import {MatSnackBar} from '@angular/material/snack-bar';
import {MatIcon, MatIconModule} from '@angular/material/icon';
import {MatInput, MatInputModule} from '@angular/material/input';
import {MatOption} from '@angular/material/core';
import {MatRadioButton, MatRadioGroup} from '@angular/material/radio';
import {MatSelectModule} from '@angular/material/select';
import {MatSlideToggleModule} from '@angular/material/slide-toggle';
import {NgClass, NgForOf, NgIf} from '@angular/common';
import {MatCheckbox} from '@angular/material/checkbox';
import {MatFormFieldModule} from '@angular/material/form-field';
import {NgOptionComponent, NgSelectComponent} from '@ng-select/ng-select';
import {Material} from '../../../models/material.model';
import {Tipo} from '../../../models/tipo.model';
import {Grupo} from '../../../models/grupo.model';
import {Empresa} from '../../../models/empresa.model';
import {Almoxarifado} from '../../../models/almoxarifado.model';
import {catchError, of, Subject, takeUntil, tap} from 'rxjs';


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
    MatFormFieldModule, MatInputModule, MatIconModule, NgIf, NgSelectComponent, NgOptionComponent, NgClass
  ],
  templateUrl: './material-form.component.html',
  styleUrl: './material-form.component.scss'
})
export class MaterialFormComponent implements OnInit, OnDestroy {
  tipos: Tipo[] = [];
  tiposFiltrados: Tipo[] = []; // Tipos filtrados com base no grupo selecionado
  grupos: Grupo[] = [];
  empresas: Empresa[] = [];
  almoxarifados: Almoxarifado[] = [];
  material: Material = new Material();

  formSubmitted: boolean = false;
  serverMessage: string | null = null;

  private unsubscribe$ = new Subject<void>();

  unidades: any[] = [
    { Value: "CX" },
    { Value: "PÇ" },
    { Value: "UN" },
    { Value: "M" },
    { Value: "CM" }
  ];

  constructor(private materialService: MaterialService) {}

  ngOnInit(): void {
    this.loadTipos();
    this.loadGrupos();
    this.loadEmpresas();
    this.loadAlmoxarifados();
  }

  private loadTipos() {
    this.materialService.getTipos()
      .pipe(takeUntil(this.unsubscribe$))
      .subscribe(tipos => this.tipos = tipos);
  }

  private loadGrupos() {
    this.materialService.getGrupos()
      .pipe(takeUntil(this.unsubscribe$))
      .subscribe(grupos => this.grupos = grupos);
  }

  private loadEmpresas() {
    this.materialService.getEmpresas()
      .pipe(takeUntil(this.unsubscribe$))
      .subscribe(empresas => this.empresas = empresas);
  }

  private loadAlmoxarifados() {
    this.materialService.getAlmoxarifados()
      .pipe(takeUntil(this.unsubscribe$))
      .subscribe(almoxarifados => this.almoxarifados = almoxarifados);
  }

  onGroupChange(selectedGroupId: number) {
    // Filtra os tipos com base no id_grupo do grupo selecionado
    this.tiposFiltrados = this.tipos.filter(tipo => tipo.grupo.idGrupo === parseInt(selectedGroupId.toString(), 10));
  }

  private showMessage(message: string, timeout = 3000) {
    this.serverMessage = message;
    setTimeout(() => {
      this.serverMessage = null;
    }, timeout);
  }


  onSubmit(form: NgForm) {
    console.log('submit');
    this.formSubmitted = true;
    if (form.valid) {
      this.materialService.create(this.material).pipe(
        tap(() => {
          this.resetForm(form);
          this.showMessage("Material cadastrado com sucesso!");
        }),
        catchError(error => {
          let errorMessage = "Erro ao cadastrar material.";

          if (error.status === 500) {
            errorMessage = "Erro interno no servidor. Por favor, tente novamente mais tarde.";
          } else if (error?.error?.message) {
            errorMessage = error.error.message;
          } else if (error.message) {
            errorMessage = error.message;
          }
          this.showMessage(errorMessage);
          return of(null);
        })
      ).subscribe(); // Apenas se inscreve sem precisar passar funções de sucesso e erro
    }
  }

  private resetForm(form: NgForm) {
    this.formSubmitted = false;
    form.reset();
    this.material = new Material(); // Reseta a instância do material
  }

  ngOnDestroy() {
    this.unsubscribe$.next();
    this.unsubscribe$.complete();
  }
}
