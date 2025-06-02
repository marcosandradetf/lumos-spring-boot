import {Component, EventEmitter, Input, Output} from '@angular/core';
import {NgClass} from '@angular/common';
import {MaterialService} from '../../../stock/services/material.service';

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
  @Output() onSearchApplied: EventEmitter<string> = new EventEmitter(); // <- Aqui é o callback


  // Atualiza o filtro de pesquisa e aplica os filtros combinados
  filterSearch(value: string): void {
    if (value.length > 2) {
      this.onSearchApplied.emit(value); // <-- chama o callback com valor
    }

    if (value.length === 0) {
      this.onSearchApplied.emit(""); // <-- avisa o pai que a busca foi limpa
    }
  }

}
