import {Component, ElementRef, ViewChild} from '@angular/core';
import {SidebarComponent} from '../../shared/components/sidebar/sidebar.component';
import {Router, RouterLinkActive} from '@angular/router';
import {PreMeasurementService} from './premeasurement-service.service';
import {TableComponent} from '../../shared/components/table/table.component';
import {KeyValuePipe, NgClass, NgForOf} from '@angular/common';
import {FormsModule, NgForm} from '@angular/forms';
import {Deposit} from '../../models/almoxarifado.model';
import {ufRequest} from '../../core/uf-request.dto';
import {IbgeService} from '../../core/service/ibge.service';
import {citiesRequest} from '../../core/cities-request.dto';
import {ModalComponent} from '../../shared/components/modal/modal.component';
import {ButtonComponent} from '../../shared/components/button/button.component';
import {Title} from '@angular/platform-browser';

@Component({
  selector: 'app-pre-measurement',
  standalone: true,
    imports: [
        TableComponent,
        NgClass,
        NgForOf,
        FormsModule,
        ModalComponent,
        ButtonComponent,
        KeyValuePipe
    ],
  templateUrl: './pre-measurement.component.html',
  styleUrl: './pre-measurement.component.scss'
})
export class PreMeasurementComponent {
  cities: { [cityName: string]: [string, string] } = {};


  constructor(preMeasurementService: PreMeasurementService) {
    preMeasurementService.getPreMeasurements().subscribe(preMeasurements => {
      this.cities = preMeasurements;
    });

  }


  protected readonly parseInt = parseInt;
}
