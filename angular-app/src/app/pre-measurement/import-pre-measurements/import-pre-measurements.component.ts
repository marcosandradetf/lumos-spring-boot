import {Component, OnInit} from '@angular/core';
import {
  PreMeasurementDTO,
  PreMeasurementStreetDTO,
  PreMeasurementStreetItemDTO,
  PreMeasurementStreetItemsDTO
} from '../pre-measurement-models';
import {NgForOf, NgIf} from '@angular/common';
import {MaterialService} from '../../stock/services/material.service';
import {Title} from '@angular/platform-browser';
import {ActivatedRoute, Router} from '@angular/router';
import {FileServerService} from '../../file-server.service';
import * as XLSX from 'xlsx';
import {PreMeasurementService} from '../../executions/pre-measurement-home/premeasurement-service.service';
import {TableComponent} from '../../shared/components/table/table.component';
import {MaterialResponse} from '../../models/material-response.dto';
import {IconAAlertComponent, IconArrowDropDownComponent, IconErrorComponent} from '../../shared/icons/icons.component';
import {AuthService} from '../../core/auth/auth.service';

@Component({
  selector: 'app-import-pre-measurements',
  standalone: true,
  imports: [
    NgIf,
    NgForOf,
    TableComponent,
    IconAAlertComponent
  ],
  templateUrl: './import-pre-measurements.component.html',
  styleUrl: './import-pre-measurements.component.scss'
})
export class ImportPreMeasurementsComponent implements OnInit {
  materialsFromApi: MaterialResponse[] = [];
  preMeasurements: PreMeasurementDTO = {
    contractId: 0,
    streets: []
  }

  contractId: number = 0;

  loading: boolean = false;
  showTable: boolean = false;
  fileName: string = '';
  responseMessage: string = '';
  responseClass: string = "";
  userUUID: string = '';

  errors: string[] = [];
  columnRules = [
    {columnName: 'Endereço', required: true},
    {columnName: 'POTÊNCIA ATUAL', required: false},
    {columnName: 'Braços de 1,5', required: false},
    {columnName: 'Braços de 2,5', required: false},
    {columnName: 'Braços\n de 3,5', required: false},
    {columnName: 'LED 50W', required: false},
    {columnName: 'LED 60W', required: false},
    {columnName: 'LED 70W', required: false},
    {columnName: 'LED 80W', required: false},
    {columnName: 'LED 100W', required: false},
    {columnName: 'LED 110W', required: false},
    {columnName: 'LED 120W', required: false},
    {columnName: 'LED 150W', required: false},
    {columnName: 'LED 200W', required: false},
    {columnName: 'CINTAS', required: false},
    {columnName: 'PARAF. E ARRUELAS', required: false},
    {columnName: 'PERFUR', required: false},
    {columnName: 'POSTE', required: false},

  ];

  constructor(
    private title: Title, protected router: Router,
    private fileServerService: FileServerService,
    private preMeasurementService: PreMeasurementService,
    private materialService: MaterialService,
    private route: ActivatedRoute,
    private authService: AuthService) {

    this.title.setTitle('Importar Pré-Medições');
    this.materialService.getMaterials().subscribe(materials => {
      this.materialsFromApi = materials;
    });

    const uuid = this.authService.getUser().uuid;
    if(uuid.length > 0) {
      this.userUUID = uuid;
    }
  }

  ngOnInit(): void {
    const contractId = this.route.snapshot.paramMap.get('id');
    if (contractId == null) {
      return
    }
    this.preMeasurements.contractId = Number(contractId);
    this.contractId = Number(contractId);
  }


  onFileChange(event: any): void {
    const file = event.target.files[0];  // Pega o primeiro arquivo selecionado
    if (!file) {
      this.fileName = '';
      return;
    }

    this.fileName = file.name;

    // Lê o arquivo Excel usando a biblioteca XLSX
    this.loading = true;
    this.errors = [];

    const reader = new FileReader();
    reader.onload = (e: any) => {
      const pData = e.target.result;
      const workbook = XLSX.read(pData, {type: 'array'});

      // Pegue a primeira aba (sheet) do arquivo
      const sheetName = workbook.SheetNames[0];
      const worksheet = workbook.Sheets[sheetName];

      // Converte a planilha em um array de objetos
      const jsonData = XLSX.utils.sheet_to_json(worksheet, {header: 1});  // 'header: 1' define que a primeira linha contém os cabeçalhos das colunas

      // Processa as linhas da planilha para o formato desejado
      const data = this.processData(jsonData);
      if (!Array.isArray(data)) {
        this.loading = true;
        this.preMeasurements = data;
        this.preMeasurementService.importData(data, this.userUUID).subscribe({
          next: (res: {message: string}) => {
            this.loading = false;
            void this.router.navigate(['pre-medicao/relatorio/' + res.message], {queryParams: {reason: 'importPreMeasurement'}});
          },
          error: (error: any) => {
            this.loading = false;
            this.responseMessage = error.error.error;
            console.log(error);
          }
        });
      } else {
        data.forEach((error, i) => {
          this.errors.push(`${i+ 1} - Coluna não reconhecida: ${error}`);
        });
        this.loading = false;
      }
    };

    reader.readAsArrayBuffer(file);
  }

  processData(pData: any[]): PreMeasurementDTO | string[] {
    const header = pData[0];
    const colIndex: { [key: string]: number } = {};
    header.forEach((colName: string, index: number) => {
      const key = this.normalizeHeader(colName);
      colIndex[key] = index;
    });

    const materialMap: { [key: string]: number } = {};

    this.materialsFromApi.forEach(material => {
      if (material.materialName !== null) {
        const key = this.normalizeHeader(material.materialName);
        materialMap[key] = material.idMaterial;
      }
    });

    const knownColumns = [
      'cabo',
      'troca de ponto',
      'rele',
      'projeto',
      'servico',

      'potencia atual',
      'latitude',
      'longitude',
      'endereco',
      'numero',
      'bairro',
      'cidade',
      'estado',

      ...Object.keys(materialMap) // inclui materiais normalizados
    ];

    const unknownColumns = Object.keys(colIndex).filter(
      key => !knownColumns.includes(key)
    );

    if (unknownColumns.length > 0) {
      console.warn('⚠️ Colunas não reconhecidas no Excel:', unknownColumns);
      return unknownColumns
    }


    const streets: PreMeasurementStreetItemsDTO[] = [];

    for (let i = 1; i < pData.length; i++) {
      const row = pData[i];
      if (row.length !== header.length) continue;
      // if (!this.validateRowByPrefix(row, i)) continue;
      if (pData.length > 501) {
        this.showTable = false;
        return {contractId: 0, streets: []};
      }

      const street: PreMeasurementStreetDTO = {
        lastPower: row[colIndex['potencia atual']] || '',
        latitude: row[colIndex['latitude']] || '',
        longitude: row[colIndex['longitude']] || '',
        street: row[colIndex['endereco']] || '',
        number: row[colIndex['numero']] || '',
        neighborhood: row[colIndex['bairro']] || '',
        city: row[colIndex['cidade']] || '',
        state: row[colIndex['estado']] || '',
      };

      const items: PreMeasurementStreetItemDTO[] = [];

      for (const key in materialMap) {
        const materialId = materialMap[key];
        const index = colIndex[key];
        if (index !== undefined) {
          const quantity = Number(row[index]) || 0;
          if (quantity > 0) {
            items.push({
              materialId,
              materialQuantity: quantity
            });
          }
        }
      }

      streets.push({
        street,
        items
      });
    }

    return {
      contractId: this.contractId, // você pode definir esse ID conforme necessário
      streets
    };
  }

  normalizeHeader(header: string): string {
    return header
      .toString()
      .normalize("NFD")                     // Remove acentos
      .replace(/[\u0300-\u036f]/g, "")     // Remove marcas de acento
      .toLowerCase()                       // Tudo minúsculo
      .trim()                              // Remove espaços extras
      .replace(/\s+/g, " ");               // Substitui todos espaços em branco (incluindo \n, \t, etc.) por espaço simples
    // .replace(/\s+/g, "_");            // (Opcional) Substitui por underscore se for essa a convenção
  }


  protected readonly location = location;
}
