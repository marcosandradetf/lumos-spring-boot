import {Component} from '@angular/core';
import {SidebarComponent} from '../../shared/components/sidebar/sidebar.component';
import {StockService} from '../services/stock.service';
import {Title} from '@angular/platform-browser';
import {Router} from '@angular/router';
import {MaterialService} from '../services/material.service';
import {TableComponent} from '../../shared/components/table/table.component';
import * as XLSX from 'xlsx';
import {NgForOf, NgIf} from '@angular/common';
import {FileServerService} from '../../file-server.service';
import {saveAs} from 'file-saver'
import {catchError, tap, throwError} from 'rxjs';

@Component({
  selector: 'app-import-materials',
  standalone: true,
  imports: [
    TableComponent,
    NgIf,
    NgForOf
  ],
  templateUrl: './import-materials.component.html',
  styleUrl: './import-materials.component.scss'
})
export class ImportMaterialsComponent {
  sidebarLinks = [
    {title: 'Gerenciar', path: '/estoque/materiais', id: 'opt1'},
    {title: 'Movimentar Estoque', path: '/estoque/movimento', id: 'opt2'},
    {title: 'Entrada de Nota Fiscal', path: '/estoque/entrada', id: 'opt3'},
    {title: 'Importar Material (.xlsx)', path: '/estoque/importar', id: 'opt4'},
    {title: 'Sugestão de Compra', path: '/estoque/sugestao', id: 'opt5'}
  ];
  loading: boolean = false;
  showTable: boolean = false;
  materials: {
    materialName: '',
    materialBrand: '',
    materialPower: '',
    materialAmps: '',
    materialLength: '',
    buyUnit: '',
    requestUnit: '',
    materialTypeName: '',
    materialGroupName: '',
    companyName: '',
    depositName: '',
  }[] = [];
  fileName: string = '';
  responseMessage: string = '';
  responseClass: string = "";

  constructor(
              private materialService: MaterialService,
              private title: Title, protected router: Router,
              private fileServerService: FileServerService) {
    this.title.setTitle('Importar Materiais');

  }

  onFileChange(event: any): void {
    const file = event.target.files[0];  // Pega o primeiro arquivo selecionado
    if (!file) {
      this.fileName = '';
      return;
    }

    if (this.materials.length > 0)
      this.materials = [];

    this.fileName = file.name;

    // Lê o arquivo Excel usando a biblioteca XLSX
    this.loading = true;
    this.showTable = true;

    const reader = new FileReader();
    reader.onload = (e: any) => {
      const data = e.target.result;
      const workbook = XLSX.read(data, {type: 'array'});

      // Pegue a primeira aba (sheet) do arquivo
      const sheetName = workbook.SheetNames[0];
      const worksheet = workbook.Sheets[sheetName];

      // Converte a planilha em um array de objetos
      const jsonData = XLSX.utils.sheet_to_json(worksheet, {header: 1});  // 'header: 1' define que a primeira linha contém os cabeçalhos das colunas

      // Processa as linhas da planilha para o formato desejado
      this.materials = this.processMaterialData(jsonData);
    };

    reader.readAsArrayBuffer(file);
    this.loading = false;
  }

  // Função para processar os dados e transformar em objetos conforme o modelo
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


  processMaterialData(data: any[]): any[] {
    // Considera que a primeira linha (data[0]) é o cabeçalho com os nomes das colunas
    const header = data[0];
    const materials = [];

    // Itera sobre as linhas de dados (ignorando o cabeçalho)
    for (let i = 1; i < data.length; i++) {
      const row = data[i];
      if (row.length === header.length) {  // Verifica se a linha tem o número correto de colunas
        if(data.length > 501) {
          this.showTable = false
          return [];
        }

        if (!this.validateRowByPrefix(row, i)) {
          continue; // Pula a linha com erro
        }

        const material = {
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

        // Adiciona o objeto material no array de materiais
        materials.push(material);
      }
    }

    return materials;
  }


  onDownloadClick(filename: string) {
    this.fileServerService.downloadFile(filename).subscribe({
      next: (fileBlob) => {
        // Salvar o arquivo no cliente usando FileSaver.js
        saveAs(fileBlob, filename);
      },
      error: (err) => {
        console.error('Erro ao baixar o arquivo:', err);
      }
    });
  }

  confirmImport() {
    if (this.materials.length === 0) {
      return;
    }

    this.loading = true;

    this.materialService.importData(this.materials).pipe(
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

  protected readonly Window = Window;
  protected readonly location = location;
}
