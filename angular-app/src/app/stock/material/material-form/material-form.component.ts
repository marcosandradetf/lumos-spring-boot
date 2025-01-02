import {Component, OnDestroy, OnInit} from '@angular/core';
import {FormsModule, NgForm, ReactiveFormsModule} from '@angular/forms';

import {catchError, of, Subject, takeUntil, tap} from 'rxjs';

import {NgClass, NgIf} from '@angular/common';
import {Group} from '../../../core/models/grupo.model';
import {Company} from '../../../core/models/empresa.model';
import {Deposit} from '../../../core/models/almoxarifado.model';
import {CreateMaterialRequest} from '../../create-material-request.dto';
import {Type} from '../../../core/models/tipo.model';
import {MaterialService, State} from '../../services/material.service';
import {EstoqueService} from '../../services/estoque.service';
import {AuthService} from '../../../core/auth/auth.service';
import {UtilsService} from '../../../core/service/utils.service';


@Component({
  selector: 'app-material-form',
  standalone: true,
  imports: [
    FormsModule,
    ReactiveFormsModule,
    NgClass,
    NgIf
  ],
  templateUrl: './material-form.component.html',
  styleUrl: './material-form.component.scss'
})
export class MaterialFormComponent implements OnInit, OnDestroy {
  types: Type[] = [];
  filterTypes: Type[] = []; // Tipos filtrados com base no grupo selecionado
  groups: Group[] = [];
  companies: Company[] = [];
  deposits: Deposit[] = [];
  material: CreateMaterialRequest = {
    buyUnit: '',
    company: 0,
    deposit: 0,
    inactive: false,
    materialBrand: '',

    materialPower: '',
    materialAmps: '',
    materialLength: '',

    materialName: '',
    materialType: 0,
    requestUnit: '',
  };

  selectGroup: boolean = false;
  formSubmitted: boolean = false;
  serverMessage: string | null = null;

  private unsubscribe$ = new Subject<void>();

  units: any[] = [
    {Value: "CX"},
    {Value: "PÇ"},
    {Value: "UN"},
    {Value: "M"},
    {Value: "CM"}
  ];

  constructor(protected materialService: MaterialService,
              private estoqueService: EstoqueService, private authService: AuthService,
              protected utils: UtilsService) {
    this.materialService.getMaterialObservable().subscribe(material => {
      this.material = material;
    });
  }


  ngOnInit(): void {
    if (this.authService.isLoggedIn$) {
      this.loadTypes();
      this.loadGroups();
      this.loadCompanies();
      this.loadDeposits();
    }

  }

  private loadTypes() {
    this.estoqueService.getTypes()
      .pipe(takeUntil(this.unsubscribe$))
      .subscribe(tipos => this.types = tipos);
  }


  private loadGroups() {
    this.estoqueService.getGroups()
      .pipe(takeUntil(this.unsubscribe$))
      .subscribe(grupos => this.groups = grupos);
  }

  private loadCompanies() {
    this.estoqueService.getCompanies()
      .pipe(takeUntil(this.unsubscribe$))
      .subscribe(empresas => this.companies = empresas);
  }

  private loadDeposits() {
    this.estoqueService.getDeposits()
      .pipe(takeUntil(this.unsubscribe$))
      .subscribe(almoxarifados => this.deposits = almoxarifados);
  }

  onGroupChange(selectedGroupId: number) {
    // Filtra os tipos com base no id_grupo do grupo selecionado
    this.selectGroup = true;
    this.filterTypes = this.types.filter(type => type.group.idGroup === parseInt(selectedGroupId.toString(), 10));
  }

  private showMessage(message: string, timeout = 3000) {
    this.serverMessage = message;
    setTimeout(() => {
      this.serverMessage = null;
    }, timeout);
  }

  onSubmit(form: NgForm) {
    this.formSubmitted = true;
    if (form.valid && this.material !== null) {
      if (this.materialService.getState() === State.create) {
        this.materialService.create(this.material).pipe(
          tap(res => {
            this.materialService.addMaterialFetch(res);
            console.log(res);
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
      } else if (this.materialService.getState() === State.update) {
        this.materialService.updateMaterial(this.material).pipe(
          tap(res => {
            this.materialService.updateMaterialFetch(res);
            this.resetForm(form);
            this.showMessage("Material atualizado com sucesso!");
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
        ).subscribe();
      }

    }
  }

  private resetForm(form: NgForm) {
    this.formSubmitted = false;
    form.reset();
    this.materialService.resetObject();
  }

  ngOnDestroy() {
    this.unsubscribe$.next();
    this.unsubscribe$.complete();
  }

  protected readonly State = State;
}
