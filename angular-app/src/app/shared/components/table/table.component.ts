import { Component, Input, OnChanges, SimpleChanges } from '@angular/core';
import {NgClass, NgForOf} from '@angular/common';
import {HttpClient} from '@angular/common/http';
import {MaterialService} from '../../../features/stock/services/material.service';
import {MaterialResponse} from '../../../features/stock/material-response.dto';

@Component({
  selector: 'app-table',
  standalone: true,
  imports: [
    NgClass
  ],
  templateUrl: './table.component.html',
  styleUrls: ['./table.component.scss']
})
export class TableComponent {
  @Input() search: boolean = false;
  @Input() filter: boolean = false;
  @Input() large: boolean = false;

  constructor(private materialService: MaterialService) { }

  // Atualiza o filtro de pesquisa e aplica os filtros combinados
  filterSearch(value: string): void {
    if (value.length > 2) {
      this.materialService.getBySearch("0", "25", value);
    }

    if (value.length === 0) {
      this.materialService.getFetch("0", "25");
    }

  }


}
