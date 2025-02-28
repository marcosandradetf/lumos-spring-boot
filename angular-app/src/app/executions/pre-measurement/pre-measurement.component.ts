import {Component, ElementRef, OnInit, ViewChild} from '@angular/core';
import {SidebarComponent} from '../../shared/components/sidebar/sidebar.component';
import {Router, RouterLinkActive} from '@angular/router';
import {PreMeasurementService} from './premeasurement-service.service';
import {TableComponent} from '../../shared/components/table/table.component';
import {KeyValuePipe, NgClass, NgForOf, NgIf} from '@angular/common';
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
    NgForOf,
    FormsModule,
    KeyValuePipe,
    TableComponent,
    NgIf,
    ModalComponent
  ],
  templateUrl: './pre-measurement.component.html',
  styleUrl: './pre-measurement.component.scss'
})
export class PreMeasurementComponent implements OnInit {
  cities: { [cityName: string]: [string, string] } = {};
  serviceQuantity: number = 0.0;
  armsQuantity: number = 0.0;

  formula: {
    leds:
      {
        description: string;
        quantity: number;
      }[];
    arms:
      {
        description: string;
        quantity: number;
      }[];
    screws: [
      {
        description: string;
        quantity: number;
      }
    ];
    straps: [
      {
        description: string;
        quantity: number;
      }
    ];
    relays: [
      {
        description: string;
        quantity: number;
      }
    ];
    sockets: [
      {
        description: string;
        quantity: number;
      }
    ];
    cables: [
      {
        description: string;
        quantity: number;
      }
    ];
  } = {
    leds: [
      {description: '60W', quantity: 3},
      {description: '70W', quantity: 4},
    ],
    arms: [
      {description: '2,5M', quantity: 1},
      {description: '3,6M', quantity: 2}
    ],
    screws: [
      {description: '', quantity: 1}
    ],
    straps: [
      {description: '', quantity: 1}
    ],
    relays: [
      {description: '', quantity: 1}
    ],
    sockets: [
      {description: '', quantity: 1}
    ],
    cables: [
      {description: '', quantity: 1}
    ],
  };

  ngOnInit() {
    console.log(this.formula.arms); // Verifique se os braços estão sendo carregados corretamente
  }


  constructor(preMeasurementService: PreMeasurementService) {
    preMeasurementService.getPreMeasurements().subscribe(preMeasurements => {
      this.cities = preMeasurements;
    });
  }


  protected readonly parseInt = parseInt;
  lineNumber: number = 1;
  post: boolean = false;
  openModal: boolean = false;

  getServiceQuantity() {
    this.formula.leds.forEach((l) => {
      this.serviceQuantity += l.quantity;
    });
    return this.serviceQuantity;
  }

  getArmsQuantity() {
    this.formula.arms.forEach((b) => {
      this.armsQuantity += b.quantity;
    });
    return this.armsQuantity;
  }

  nextLine() {
    this.lineNumber++;
    return this.lineNumber;
  }

  protected readonly alert = alert;
}
