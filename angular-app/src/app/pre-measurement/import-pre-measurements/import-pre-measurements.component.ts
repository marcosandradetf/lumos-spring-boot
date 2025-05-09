import {Component} from '@angular/core';
import {PreMeasurementDTO} from '../pre-measurement-models';
import {LoadingComponent} from '../../shared/components/loading/loading.component';
import {NgForOf, NgIf} from '@angular/common';
import {MaterialService} from '../../stock/services/material.service';
import {Title} from '@angular/platform-browser';
import {Router} from '@angular/router';
import {FileServerService} from '../../file-server.service';
import * as XLSX from 'xlsx';
import {catchError, tap, throwError} from 'rxjs';
import {PreMeasurementService} from '../../executions/pre-measurement-home/premeasurement-service.service';
import {TableComponent} from '../../shared/components/table/table.component';

@Component({
  selector: 'app-import-pre-measurements',
  standalone: true,
  imports: [
    LoadingComponent,
    NgIf,
    NgForOf,
    TableComponent
  ],
  templateUrl: './import-pre-measurements.component.html',
  styleUrl: './import-pre-measurements.component.scss'
})
export class ImportPreMeasurementsComponent {
  preMeasurements: PreMeasurementDTO[] = []
  loading: boolean = false;
  showTable: boolean = false;
  fileName: string = '';
  responseMessage: string = '';
  responseClass: string = "";

  errors: string[] = [];
  columnRules = [
    {columnName: 'Nome do Material', required: true},
    {columnName: 'Marca', required: false},
    {columnName: 'Potência', required: false},
    {columnName: 'Corrente', required: false},
    {columnName: 'Tamanho', required: false},
    {columnName: 'Unidade de Compra', required: true},
    {columnName: 'Unidade de Requisição', required: true},
    {columnName: 'Tipo Material', required: true},
    {columnName: 'Grupo Material', required: true},
    {columnName: 'Empresa', required: true},
    {columnName: 'Almoxarifado', required: true},
  ];

  constructor(
    private title: Title, protected router: Router,
    private fileServerService: FileServerService, private preMeasurementService: PreMeasurementService) {
    this.title.setTitle('Importar Pré-Medições');

  }


  onFileChange(event: any): void {
    const file = event.target.files[0];  // Pega o primeiro arquivo selecionado
    if (!file) {
      this.fileName = '';
      return;
    }

    if (this.preMeasurements.length > 0)
      this.preMeasurements = [];

    this.fileName = file.name;

    // Lê o arquivo Excel usando a biblioteca XLSX
    this.loading = true;
    this.showTable = true;

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
      this.preMeasurements = this.processData(jsonData);
    };

    reader.readAsArrayBuffer(file);
    this.loading = false;
  }

  processData(pData: any[]): any[] {
    // Considera que a primeira linha (pData[0]) é o cabeçalho com os nomes das colunas
    const header = pData[0];
    const data = [];

    // Itera sobre as linhas de dados (ignorando o cabeçalho)
    for (let i = 1; i < pData.length; i++) {
      const row = pData[i];
      if (row.length === header.length) {  // Verifica se a linha tem o número correto de colunas
        if (pData.length > 501) {
          this.showTable = false
          return [];
        }

        if (!this.validateRowByPrefix(row, i)) {
          continue; // Pula a linha com erro
        }

        const given = {
          materialName: row[0] || '',
          materialBrand: row[1] || '',
          materialPower: row[2] || '',
          materialAmps: row[3] || '',
          materialLength: row[4] || '',
          buyUnit: row[5] || '',
          requestUnit: row[6] || '',
          materialTypeName: row[7] || '',
          materialGroupName: row[8] || '',
          companyName: row[9] || '',
          depositName: row[10] || ''
        };

        // Adiciona o objeto material no array de dados
        data.push(given);
      }
    }

    return data;
  }

  validateRowByPrefix(row: string[], line: number): boolean {
    let result = true;

    for (let i = 0; i < this.columnRules.length; i++) {
      const rule = this.columnRules[i];
      const value = row[i]?.trim();

      // Validação de campo obrigatório
      if (rule.required && (!value || value === '')) {
        this.errors.push(`Linha ${i}: Dado(s) na coluna ${rule.columnName} é(são) obrigatório(s) e está(ão) vazio(s).`);
        result = false;
      }

      if (i === 5 || i === 6) {
        if (value.length > 2) {
          this.errors.push(
            `Linha ${line}: Para a coluna ${rule.columnName}, após o prefixo é permitido até 2 caracteres.`
          );
          result = false;
        }
      }

    }
    return result;
  }

  confirmImport() {
    if (this.preMeasurements.length === 0) return;
    if (this.loading) return;

    this.loading = true;

    this.preMeasurementService.importData(this.preMeasurements).pipe(
      tap(res => {
        this.loading = false;
        this.responseClass = "bg-success text-white"
        this.responseMessage = ((res as any).message);
      }),
      catchError(err => {
        this.loading = false;
        console.log(err);
        this.responseClass = "bg-error text-white"
        this.responseMessage = err.error.error;
        return throwError(() => err);
      })
    ).subscribe();
  }


  protected readonly location = location;
}
