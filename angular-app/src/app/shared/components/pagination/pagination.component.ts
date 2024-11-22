import {Component, Input} from '@angular/core';
import {NgClass, NgForOf} from "@angular/common";
import {MaterialService} from '../../../features/estoque/services/material.service';

@Component({
  selector: 'app-pagination',
  standalone: true,
  imports: [
    NgForOf,
    NgClass
  ],
  templateUrl: './pagination.component.html',
  styleUrl: './pagination.component.scss'
})
export class PaginationComponent {
  currentPage: string = "0";
  @Input() service: any;


  changePage(page: number): void {
    if (page.toString() !== this.currentPage) {
      this.currentPage = page.toString();
      this.service.getFetch(this.currentPage, "25");
    }
  }

  protected readonly parseInt = parseInt;
}
