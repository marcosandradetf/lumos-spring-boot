import {Component, inject, OnInit} from '@angular/core';
import {FormBuilder, FormControl, FormGroup, FormsModule, ReactiveFormsModule} from '@angular/forms';
import {MaterialService} from '../material.service';
import {MatSnackBar} from '@angular/material/snack-bar';
import {FloatLabelType, MatFormField, MatLabel} from '@angular/material/form-field';
import {MatIcon} from '@angular/material/icon';
import {MatInput} from '@angular/material/input';
import {MatOption} from '@angular/material/core';
import {MatRadioButton, MatRadioGroup} from '@angular/material/radio';
import {MatSelect} from '@angular/material/select';
import {MatSlideToggle, MatSlideToggleModule} from '@angular/material/slide-toggle';
import {toSignal} from '@angular/core/rxjs-interop';
import {map} from 'rxjs';

@Component({
  selector: 'app-material-form',
  standalone: true,
  imports: [
    FormsModule,
    MatFormField,
    MatIcon,
    MatInput,
    MatLabel,
    MatOption,
    MatRadioButton,
    MatRadioGroup,
    MatSelect,
    MatSlideToggleModule,
    ReactiveFormsModule
  ],
  templateUrl: './material-form.component.html',
  styleUrl: './material-form.component.scss'
})
export class MaterialFormComponent implements OnInit {
  readonly hideRequiredControl = new FormControl(false);
  readonly floatLabelControl = new FormControl('auto' as FloatLabelType);
  readonly options = inject(FormBuilder).group({
    hideRequired: this.hideRequiredControl,
    floatLabel: this.floatLabelControl,
  });
  protected readonly hideRequired = toSignal(this.hideRequiredControl.valueChanges);
  protected readonly floatLabel = toSignal(
    this.floatLabelControl.valueChanges.pipe(map(v => v || 'auto')),
    {initialValue: 'auto'},
  );

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
      // definição dos controles
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
