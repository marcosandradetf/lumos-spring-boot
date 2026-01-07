import {Component, OnInit} from '@angular/core';
import {AbstractControl, FormBuilder, FormGroup, ReactiveFormsModule, Validators} from '@angular/forms';
import {DropdownModule} from 'primeng/dropdown';
import {InputText} from 'primeng/inputtext';
import {ButtonDirective} from 'primeng/button';
import {PrimeBreadcrumbComponent} from '../../shared/components/prime-breadcrumb/prime-breadcrumb.component';
import {UtilsService} from '../../core/service/utils.service';
import {NgIf} from '@angular/common';
import {MultiSelectModule} from 'primeng/multiselect';
import {ContractReferenceItemsDTO} from '../../contract/contract-models';
import {Title} from '@angular/platform-browser';
import {ContractService} from '../../contract/services/contract.service';
import {StockService} from '../services/stock.service';
import {MaterialService} from '../services/material.service';
import {Toast} from 'primeng/toast';
import {LoadingOverlayComponent} from '../../shared/components/loading-overlay/loading-overlay.component';

@Component({
  selector: 'app-material-form',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    DropdownModule,
    InputText,
    PrimeBreadcrumbComponent,
    ButtonDirective,
    NgIf,
    MultiSelectModule,
    Toast,
    LoadingOverlayComponent,
  ],
  templateUrl: './material-form.component.html',
  styleUrl: './material-form.component.scss'
})
export class MaterialFormComponent implements OnInit {
  form!: FormGroup;
  materialTypes: any[] = [];
  availableSubtypes: any[] = [];
  items: ContractReferenceItemsDTO[] = [];
  loading = false;

  constructor(private fb: FormBuilder,
              protected utils: UtilsService,
              private title: Title,
              private contractService: ContractService,
              private stockService: StockService,
              private materialService: MaterialService,
  ) {
  }

  ngOnInit(): void {
    this.form = this.fb.group({
      materialId: [null],
      materialBaseName: [{value: '', disabled: false}],
      materialName: [{value: '', disabled: false}],
      materialType: [null, Validators.required],
      materialSubtype: [
        null,
        this.subtypeValidatorFactory(() => this.availableSubtypes)
      ],
      materialFunction: [null],
      materialModel: [null],
      materialBrand: [null, Validators.required],
      materialAmps: [null],
      materialLength: [null],
      materialWidth: [null],
      materialPower: [null],
      materialGauge: [null],
      materialWeight: [null],
      barcode: ['', [Validators.required, this.barcodeValidator]],
      inactive: [false],
      buyUnit: [null, Validators.required],
      requestUnit: [null, Validators.required],
      contractItems: [[], Validators.required], // multiselect
    });

    this.form.valueChanges.subscribe(() => {
      this.generateMaterialName();
    });

    this.title.setTitle('Cadastrar Material');
    this.contractService.getContractReferenceItems().subscribe(items => {
      this.items = items.filter(i =>
        !['SERVIÇO', 'CEMIG', 'PROJETO', 'MANUTENÇÃO'].includes(
          (i.type ?? '').toUpperCase()
        )
      );
    });

    this.stockService.findAllTypeSubtype().subscribe(types => {
      this.materialTypes = types;
    });

  }

  lastTypeId = 0;
  buyUnits: any[] = [];
  requestUnits: any[] = [];

  onTypeChange(typeId: number) {
    if(typeId !== this.lastTypeId) {
      this.lastTypeId = typeId;
      this.availableSubtypes = this.materialTypes.filter(t => t.typeId === typeId).map(s => s.subtypes);
      if (this.availableSubtypes.length > 0) this.availableSubtypes = this.availableSubtypes[0];
      this.form.patchValue({materialSubtype: null});

      this.stockService.findUnitsByTypeId(typeId).subscribe({
        next: (data) => {
          this.buyUnits = data.buyUnits;
          this.requestUnits = data.requestUnits;
          if (this.buyUnits.length === 1) {
            this.form.patchValue({code: this.buyUnits[0].code});
          }
          if (this.requestUnits.length === 1) {
            this.form.patchValue({code: this.requestUnits[0].code, truckStockControl: this.requestUnits[0].truckStockControl});
          }
        },
        error: (err) => {
          this.utils.showMessage(err.error.message ?? err.error.error, 'error', "Erro ao buscar Unidades para o tipo atual");
        },
      });
    }
  }

  generateMaterialName() {
    const v = this.form.getRawValue();
    const materialType = this.materialTypes.find(t => t.typeId === v.materialType)?.typeName ?? '';
    const materialSubType = this.availableSubtypes.find(t => t.subtypeId === v.materialSubtype)?.subtypeName ?? '';

    const partsBase = [
      materialType,
      materialSubType,
    ];

    const parts = [
      materialType,
      materialSubType,
      v.materialFunction,
      v.materialModel,
      v.materialBrand,
      v.materialAmps,
      v.materialLength,
      v.materialWidth,
      v.materialPower,
      v.materialGauge,
      v.materialWeight
    ];

    const baseName = partsBase
      .filter(Boolean)
      .join(' ')
      .replace(/\s+/g, ' ')
      .trim()
      .toUpperCase();

    const name = parts
      .filter(Boolean)
      .join(' ')
      .replace(/\s+/g, ' ')
      .trim()
      .toUpperCase();

    this.form.patchValue(
      {
        materialBaseName: baseName,
        materialName: name
      },
      {emitEvent: false}
    );
  }

  submit() {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    console.log(this.form.getRawValue());
  }

  barcodeValidator(control: AbstractControl) {
    const value = control.value;
    if (!value) return null;

    const onlyNumbers = /^\d+$/;
    if (!onlyNumbers.test(value)) {
      return {barcodeInvalid: true};
    }

    if (![8, 12, 13, 14].includes(value.length)) {
      return {barcodeLength: true};
    }

    return null;
  }

  subtypeValidatorFactory(getAvailableSubtypes: () => any[]) {
    return (control: AbstractControl) => {
      const value = control.value;
      const subtypes = getAvailableSubtypes();

      if (!subtypes || subtypes.length === 0) {
        return null; // não exige subtipo
      }

      if (!value) {
        return {subtypeRequired: true};
      }

      return null;
    };
  }


  protected findMaterial(event: FocusEvent) {
    const input = event.target as HTMLInputElement;
    const value = input.value;

    if (![8, 12, 13, 14].includes(value.length)) {
      return;
    }

    this.loading = true;
    this.materialService.findByBarCode(value).subscribe({
      next: (data) => {
        this.form = this.fb.group({
          materialId: [data.materialId],
          materialBaseName: [{value: data.materialBaseName, disabled: false}],
          materialName: [{value: data.materialBaseName, disabled: false}],
          materialType: [data.materialType, Validators.required],
          materialSubtype: [
            data.materialSubtype,
            this.subtypeValidatorFactory(() => this.availableSubtypes)
          ],
          materialFunction: [data.materialFunction],
          materialModel: [data.materialModel],
          materialBrand: [data.materialBrand, Validators.required],
          materialAmps: [data.materialAmps],
          materialLength: [data.materialLength],
          materialWidth: [data.materialWidth],
          materialPower: [data.materialPower],
          materialGauge: [data.materialGauge],
          materialWeight: [data.materialWeight],
          barcode: [data.barcode, [Validators.required, this.barcodeValidator]],
          inactive: [data.inactive],
          buyUnit: [data.buyUnit, Validators.required],
          requestUnit: [data.requestUnit, Validators.required],
          contractItems: [data.contractItems, Validators.required], // multiselect
        });
      },
      error: err => {
        this.loading = false;
        const v = this.form.getRawValue();
        if(v.materialId !== null) {
          this.form = this.fb.group({
            materialId: [null],
            materialBaseName: [{value: '', disabled: false}],
            materialName: [{value: '', disabled: false}],
            materialType: [null, Validators.required],
            materialSubtype: [
              null,
              this.subtypeValidatorFactory(() => this.availableSubtypes)
            ],
            materialFunction: [null],
            materialModel: [null],
            materialBrand: [null, Validators.required],
            materialAmps: [null],
            materialLength: [null],
            materialWidth: [null],
            materialPower: [null],
            materialGauge: [null],
            materialWeight: [null],
            barcode: [value, [Validators.required, this.barcodeValidator]],
            inactive: [false],
            buyUnit: [null, Validators.required],
            requestUnit: [null, Validators.required],
            contractItems: [[], Validators.required], // multiselect
          });
        }

        this.utils.showMessage(
          'Nenhum material foi encontrado com este código de barras. Você pode continuar o cadastro normalmente.',
          'info',
          'Busca por código de barras'
        );
      },
      complete: () => {
        this.generateMaterialName();
        this.loading = false;
        this.utils.showMessage(
          'Já existe um material cadastrado com este código de barras. Caso precise, você pode ajustar as informações antes de salvar.',
          'info',
          'Busca por código de barras'
        );

      }
    });

  }
}
