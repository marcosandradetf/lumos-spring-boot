import { Component } from '@angular/core';
import {SidebarComponent} from '../../shared/components/sidebar/sidebar.component';
import {EstoqueService} from '../services/estoque.service';
import {Title} from '@angular/platform-browser';
import {Router} from '@angular/router';
import {MaterialService} from '../services/material.service';
import {TableComponent} from '../../shared/components/table/table.component';
import * as XLSX from 'xlsx';

@Component({
  selector: 'app-import-materials',
  standalone: true,
  imports: [
    SidebarComponent,
    TableComponent
  ],
  templateUrl: './import-materials.component.html',
  styleUrl: './import-materials.component.scss'
})
export class ImportMaterialsComponent {
  sidebarLinks = [
    { title: 'Gerenciar', path: '/estoque/materiais', id: 'opt1' },
    { title: 'Movimentar Estoque', path: '/estoque/movimento', id: 'opt2' },
    { title: 'Entrada de Nota Fiscal', path: '/estoque/entrada', id: 'opt3' },
    { title: 'Importar Material (.xlsx)', path: '/estoque/importar', id: 'opt4' },
    { title: 'Sugestão de Compra', path: '/estoque/sugestao', id: 'opt5' }
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
              private title: Title, protected router: Router) {
    this.title.setTitle('Importar Materiais');

  }

  onFileChange(event: any): void {
    const file = event.target.files[0];  // Pega o primeiro arquivo selecionado
    if (!file) {
      return;
    }

    // Lê o arquivo Excel usando a biblioteca XLSX
    this.loading = true;
    this.showTable = true;

    const reader = new FileReader();
    reader.onload = (e: any) => {
      const data = e.target.result;
      const workbook = XLSX.read(data, { type: 'array' });

      // Pegue a primeira aba (sheet) do arquivo
      const sheetName = workbook.SheetNames[0];
      const worksheet = workbook.Sheets[sheetName];

      // Converte a planilha em um array de objetos
      const jsonData = XLSX.utils.sheet_to_json(worksheet, { header: 1 });  // 'header: 1' define que a primeira linha contém os cabeçalhos das colunas

      // Processa as linhas da planilha para o formato desejado
      this.materials = this.processMaterialData(jsonData);
    };

    reader.readAsArrayBuffer(file);
    this.loading = false;
  }

  // Função para processar os dados e transformar em objetos conforme o modelo
  processMaterialData(data: any[]): any[] {
    // Considera que a primeira linha (data[0]) é o cabeçalho com os nomes das colunas
    const header = data[0];
    const materials = [];

    // Itera sobre as linhas de dados (ignorando o cabeçalho)
    for (let i = 1; i < data.length; i++) {
      const row = data[i];
      if (row.length === header.length) {  // Verifica se a linha tem o número correto de colunas
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
}
