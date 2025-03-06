import {Component} from '@angular/core';
import {PreMeasurementService} from './premeasurement-service.service';
import { NgForOf, NgIf} from '@angular/common';
import {FormsModule} from '@angular/forms';
import {ModalComponent} from '../../shared/components/modal/modal.component';
import {UtilsService} from '../../core/service/utils.service';
import {catchError, tap, throwError} from 'rxjs';
import {Router} from '@angular/router';

@Component({
  selector: 'app-pre-measurement-pending',
  standalone: true,
  imports: [
    NgForOf,
    FormsModule,
    NgIf,
    ModalComponent
  ],
  templateUrl: './pre-measurement.component.html',
  styleUrl: './pre-measurement.component.scss'
})
export class PreMeasurementComponent {
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
  totalPrice: string = '0,00';

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


  constructor(private preMeasurementService: PreMeasurementService, public utils: UtilsService, private router: Router) {
    preMeasurementService.getPreMeasurements('pending').subscribe(preMeasurements => {
      this.preMeasurements = preMeasurements;
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


  setPrice(event: Event, index: number, attributeName: keyof typeof this.formula) {
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

  removeTotalPrice(index: number, attributeName: keyof typeof this.formula) {
    if(this.formula[attributeName][index].priceTotal !== undefined) {
      this.totalPrice = this.utils.subValue(this.totalPrice, this.formula[attributeName][index].priceTotal);
    }

  }

  setTotalPrice(index: number, attributeName: keyof typeof this.formula) {
    if(this.formula[attributeName][index].priceTotal !== undefined) this.totalPrice = this.utils.sumValue(this.formula[attributeName][index].priceTotal, this.totalPrice);
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
      console.log(this.formula);
    });

  }

  sendValues() {
    const hasEmptyPrice = Object.keys(this.formula).some(attributeName =>
      this.formula[attributeName as keyof typeof this.formula]
        .some((item) => item.price === "" || item.price === undefined)
    );

    if (hasEmptyPrice) {
      console.log("tropa");
      return;
    }


    this.preMeasurementService.savePremeasurementValues(this.formula, this.preMeasurementId).pipe(
      tap(preMeasurement => {
        this.router.navigate(['pre-medicao/relatorio/' + this.preMeasurementId]);
      }),
      catchError((err) => {
        console.error('Erro ao salvar valores:', err);
        return throwError(() => err); // Correção: use throwError para lidar com o erro corretamente
      })
    ).subscribe();

  }

  autoTabTimeout: any;

  startAutoTab(event: FocusEvent) {
    this.clearAutoTab(); // Evita múltiplos timeouts
    this.autoTabTimeout = setTimeout(() => {
      (event.target as HTMLInputElement).blur(); // Remove o foco do input
      this.focusNextInput(event.target as HTMLInputElement); // Foca no próximo input
    }, 3000); // Tempo em milissegundos (2 segundos)
  }

  clearAutoTab() {
    clearTimeout(this.autoTabTimeout); // Cancela o timeout se necessário
  }

  focusNextInput(currentInput: HTMLInputElement) {
    const inputs = Array.from(document.querySelectorAll('.auto-tab-input')) as HTMLInputElement[];
    const currentIndex = inputs.indexOf(currentInput);

    if (currentIndex !== -1 && currentIndex < inputs.length - 1) {
      inputs[currentIndex + 1].focus(); // Foca no próximo input
    } else {
      document.getElementById('submitBtn')?.focus(); // Foca no botão se for o último input
    }
  }


}
