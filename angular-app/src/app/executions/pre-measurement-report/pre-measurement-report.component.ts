import { Component } from '@angular/core';

import {NgForOf, NgOptimizedImage} from '@angular/common';

@Component({
  selector: 'app-pre-measurement-report',
  standalone: true,
  imports: [
    NgForOf,
    NgOptimizedImage
  ],
  templateUrl: './pre-measurement-report.component.html',
  styleUrl: './pre-measurement-report.component.scss'
})
export class PreMeasurementReportComponent {
  items = [
    { material: 'Luminária LED', quantity: 10, location: 'Estoque 1' },
    { material: 'Cabo Elétrico', quantity: 5, location: 'Estoque 2' },
    { material: 'Transformador', quantity: 2, location: 'Estoque 3' }
  ];


  generatePDF(content: HTMLDivElement): void {

  }

}
