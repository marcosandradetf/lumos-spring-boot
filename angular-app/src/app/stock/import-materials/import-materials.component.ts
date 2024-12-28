import {Component} from '@angular/core';
import {SidebarComponent} from '../../shared/components/sidebar/sidebar.component';
import {EstoqueService} from '../services/estoque.service';
import {Title} from '@angular/platform-browser';
import {Router} from '@angular/router';
import {MaterialService} from '../services/material.service';
import {TableComponent} from '../../shared/components/table/table.component';
import * as XLSX from 'xlsx';
import {NgForOf, NgIf} from '@angular/common';
import {FileServerService} from '../../file-server.service';
import { saveAs } from 'file-saver'

@Component({
  selector: 'app-import-materials',
  standalone: true,
  imports: [
    SidebarComponent,
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
    power: '',
    buyUnit: '',
    buyRequest: '',
    materialType: '',
    materialGroup: '',
    company: '',
    deposit: '',
  }[] = [];

  constructor(private stockService: EstoqueService,
              private materialService: MaterialService,
              private title: Title, protected router: Router,
              private fileServerService: FileServerService) {
    this.title.setTitle('Importar Materiais');

  }

  onFileChange(event: any): void {
    const file = event.target.files[0];  // Pega o primeiro arquivo selecionado
    if (!file) {
      return;
    }

    // Lê o arquivo Excel usando a biblioteca XLSX
    this.loading = true;
    // this.showTable = true;

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
    {columnName: 'Nome do Material', prefixes: ['Material'], required: true},
    {columnName: 'Marca', prefixes: ['Marca'], required: true},
    {columnName: 'Potência', prefixes: ['Potência', 'Potencia'], required: false},
    {columnName: 'Unidade de Compra', prefixes: ['Unidade'], required: true},
    {columnName: 'Unidade de Requisição', prefixes: ['Requisição', 'Requisicao', 'Requisiçao'], required: true},
    {columnName: 'Tipo Material', prefixes: ['Tipo'], required: true},
    {columnName: 'Grupo Material', prefixes: ['Grupo'], required: true},
    {columnName: 'Empresa', prefixes: ['Empresa'], required: true},
    {columnName: 'Almoxarifado', prefixes: ['Depósito', 'Deposito'], required: true},
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

      console.log(value)
      console.log(value.split(' ')[1])

      if (value.split(' ')[1] === null || value.split(' ')[1] === '' || value.split(' ')[1] === undefined) {
        this.errors.push(`Linha ${line}: Por questão de segurança dados na coluna ${rule.columnName} deve começar pelo prefixo: "${rule.prefixes[0]}" + Descrição. Valor encontrado: "${value}".`);
        result = false;
      }

      // Verificar se o valor começa com algum dos prefixos permitidos
      const isValidPrefix = rule.prefixes.some((prefix) => value.startsWith(prefix));
      if (!isValidPrefix) {
        this.errors.push(
          `Linha ${line}: Por questão de segurança dados na coluna ${rule.columnName} deve começar pelo prefixo: "${rule.prefixes[0]}". Valor encontrado: "${value}".`
        );
        result = false;
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
        // Validar a linha inteira por prefixo
        if (!this.validateRowByPrefix(row, i)) {
          continue; // Pula a linha com erro
        }

        const material = {
          materialName: row[0] || '',
          materialBrand: row[1] || '',
          power: row[2] || '',
          buyUnit: row[3] || '',
          buyRequest: row[4] || '',
          materialType: row[5] || '',
          materialGroup: row[6] || '',
          company: row[7] || '',
          deposit: row[8] || ''
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
}
