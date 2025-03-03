import {Component, OnInit} from '@angular/core';
import {PreMeasurementService} from './premeasurement-service.service';
import {TableComponent} from '../../shared/components/table/table.component';
import {KeyValuePipe, NgForOf, NgIf} from '@angular/common';
import {FormsModule} from '@angular/forms';
import {ModalComponent} from '../../shared/components/modal/modal.component';
import * as Util from 'node:util';
import {UtilsService} from '../../core/service/utils.service';
import {startWith} from 'rxjs';

@Component({
  selector: 'app-pre-measurement',
  standalone: true,
  imports: [
    NgForOf,
    FormsModule,
    TableComponent,
    NgIf,
    ModalComponent
  ],
  templateUrl: './pre-measurement.component.html',
  styleUrl: './pre-measurement.component.scss'
})
export class PreMeasurementComponent implements OnInit {
  preMeasurements: {
    preMeasurementId: number;
    city: string;
    createdBy: string;
    createdAt: string;
    preMeasurementType: string;
    preMeasurementStyle: string;
    teamName: string;

    streets: {
      preMeasurementStreetId: number;
      lastPower: string;
      latitude: number;
      longitude: number;
      address: string;

      items: {
        preMeasurementStreetItemId: number;
        materialId: number;
        materialName: string;
        materialType: string;
        materialPower: string;
        materialLength: string;
        materialQuantity: number;
      }[]

    }[];

  }[] = [];


  preMeasurementId: number = 0;
  preMeasurementName: string = '';
  serviceQuantity: number = 0.0;
  armsQuantity: number = 0.0;

  formula: {
    leds:
      {
        description: string;
        quantity: number;
        price: string;
        priceTotal: string;
      }[];
    ledService: [
      {
        description: string;
        quantity: number;
        price: string;
        priceTotal: string;
      }
    ];
    piService: [
      {
        description: string;
        quantity: number;
        price: string;
        priceTotal: string;
      }
    ];
    arms:
      {
        description: string;
        quantity: number;
        price: string;
        priceTotal: string;
      }[];
    armService: [
      {
        description: string;
        quantity: number;
        price: string;
        priceTotal: string;
      }
    ];
    screws: [
      {
        description: string;
        quantity: number;
        price: string;
        priceTotal: string;
      }
    ];
    straps: [
      {
        description: string;
        quantity: number;
        price: string;
        priceTotal: string;
      }
    ];
    relays: [
      {
        description: string;
        quantity: number;
        price: string;
        priceTotal: string;
      }
    ];
    connectors: [
      {
        description: string;
        quantity: number;
        price: string;
        priceTotal: string;
      }
    ];
    cables: [
      {
        description: string;
        quantity: number;
        price: string;
        priceTotal: string;
      }
    ];
    posts: [
      {
        description: string;
        quantity: number;
        price: string;
        priceTotal: string;
      }
    ];
  } = {
    leds: [
      {description: '', quantity: 0, price: '0,00', priceTotal: '0,00'},
    ],
    ledService: [
      {description: '', quantity: 0, price: '0,00', priceTotal: '0,00'}
    ],
    piService: [
      {description: '', quantity: 0, price: '0,00', priceTotal: '0,00'}
    ],
    arms: [
      {description: '', quantity: 0, price: '0,00', priceTotal: '0,00'},
    ],
    armService: [
      {description: '', quantity: 0, price: '0,00', priceTotal: '0,00'}
    ],
    screws: [
      {description: '', quantity: 0, price: '0,00', priceTotal: '0,00'}
    ],
    straps: [
      {description: '', quantity: 0, price: '0,00', priceTotal: '0,00'}
    ],
    relays: [
      {description: '', quantity: 0, price: '0,00', priceTotal: '0,00'}
    ],
    connectors: [
      {description: '', quantity: 0, price: '0,00', priceTotal: '0,00'}
    ],
    cables: [
      {description: '', quantity: 0, price: '0,00', priceTotal: '0,00'}
    ],
    posts: [
      {description: '', quantity: 0, price: '0,00', priceTotal: '0,00'}
    ],
  };
  private loading: boolean = false;

  ngOnInit() {
    this.getServiceQuantity();
    this.getArmsQuantity();
  }


  constructor(private preMeasurementService: PreMeasurementService, public utils: UtilsService) {
    preMeasurementService.getPreMeasurements().subscribe(preMeasurements => {
      this.preMeasurements = preMeasurements;
      console.log(this.preMeasurements);
    });
  }


  protected readonly parseInt = parseInt;
  lineNumber = 1;

  resetLineNumber() {
    this.lineNumber = 1; // Reseta o contador sempre que necessário
  }

  nextLine() {
    return this.lineNumber++
  }

  post: boolean = false;
  openModal: boolean = false;

  getServiceQuantity() {
    this.formula.leds.forEach((l) => {
      this.serviceQuantity += l.quantity;
    });
  }

  getArmsQuantity() {
    this.formula.arms.forEach((b) => {
      this.armsQuantity += b.quantity;
    });
  }

  formatValue(event: Event, index: number, attributeName: keyof typeof this.formula) {
    // Obtém o valor diretamente do evento e remove todos os caracteres não numéricos
    let targetValue = (event.target as HTMLInputElement).value.replace(/\D/g, '');

    // Verifica se targetValue está vazio e define um valor padrão
    if (!targetValue) {
      this.formula[attributeName][index].price = '0,00';
      this.formula[attributeName][index].priceTotal = '0,00';
      (event.target as HTMLInputElement).value = '0,00'; // Atualiza o valor no campo de input
      return;
    }

    const value = this.utils.formatValue(targetValue);
    this.formula[attributeName][index].price = value;
    this.formula[attributeName][index].priceTotal = this.utils.multiplyValue(targetValue, this.formula[attributeName][index].quantity).toString();
    (event.target as HTMLInputElement).value = value; // Exibe o valor formatado no campo de input

  }


  getItemsQuantity(preMeasurementId: number) {
    let quantity: number = 0;
    this.preMeasurements.find(p => p.preMeasurementId === preMeasurementId)
      ?.streets.forEach((street) => {
      quantity += street.items.length;
    });

    return quantity;
  }

  provideValues(preMeasurementId: number) {
    const preMeasurement = this.preMeasurements.find(p => p.preMeasurementId === preMeasurementId);
    if (!preMeasurement) {
      return;
    }

    this.openModal = true;
    this.loading = true;
    this.preMeasurementId = preMeasurementId;
    this.preMeasurementName = preMeasurement.city;

    this.preMeasurementService.getFields(preMeasurementId).subscribe(fields => {
      this.formula = fields;
    });




  }
}
