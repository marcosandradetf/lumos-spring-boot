import {Component, ElementRef, ViewChild} from '@angular/core';
import {SidebarComponent} from '../../shared/components/sidebar/sidebar.component';
import {Router, RouterLinkActive} from '@angular/router';
import {PreMeasurementService} from './premeasurement-service.service';
import {TableComponent} from '../../shared/components/table/table.component';
import {NgClass, NgForOf} from '@angular/common';
import {FormsModule, NgForm} from '@angular/forms';
import {Deposit} from '../../models/almoxarifado.model';
import {ufRequest} from '../../core/uf-request.dto';
import {IbgeService} from '../../core/service/ibge.service';
import {citiesRequest} from '../../core/cities-request.dto';
import {ModalComponent} from '../../shared/components/modal/modal.component';
import {ButtonComponent} from '../../shared/components/button/button.component';

@Component({
  selector: 'app-pre-measurement',
  standalone: true,
  imports: [
    TableComponent,
    NgClass,
    NgForOf,
    FormsModule,
    ModalComponent,
    ButtonComponent
  ],
  templateUrl: './pre-measurement.component.html',
  styleUrl: './pre-measurement.component.scss'
})
export class PreMeasurementComponent {
  sidebarLinks: { title: string; path: string; id: string }[] = [];
  drag: boolean = false;
  items: { materialId: string; materialName: string; materialQuantity: string }[] = [];
  depositSelected: boolean = false;
  depositName: string = "";
  p: {
    description: string,
    uf: string,
    city: string,
    region: string,
    idDeposit: string,
    streets: { name: string, items: { itemName: string, itemQuantity: string }[] }[]
  } = {
    description: '',
    uf: '',
    city: '',
    region: '',
    idDeposit: '',
    streets: [
      {
        name: '',
        items: [
          {
            itemName: '',
            itemQuantity: ''
          }
        ]
      }
    ]
  };

  deposits: Deposit[] = [];
  ufs: ufRequest[] = [];
  cities: citiesRequest[] = [];
  @ViewChild('almox') almox: any;

  constructor(private preMeasurementService: PreMeasurementService,
              public router: Router, private ibgeService: IbgeService) {

    this.preMeasurementService.getDeposits().subscribe(deposits => {
      this.deposits = deposits;
    });

    this.ibgeService.getUfs().subscribe(ufs => {
      this.ufs = ufs;
    });

  }

  getCities(uf: string) {
    this.ibgeService.getCities(uf).subscribe(cities => {
      this.cities = cities;
    });
  }

  submit(PreMeasurementForm: NgForm) {

  }

  toggleDeposit(depositId: string) {
    const depositName = this.deposits.find(d => d.idDeposit === Number(depositId))?.depositName;
    this.depositName = depositName ? depositName : '';
    if (depositId) {
      this.preMeasurementService.getItemsByDeposit("1").subscribe(items => {
        this.items = items;
      });
    }
  }


  protected readonly alert = alert;
  openModalItens: boolean = false;
  depositMessage: string  = '';

  getRegion(selectedCityName: string) {
    const selectedCity = this.cities.find(city => city.nome === selectedCityName);
    this.p.region = selectedCity ? selectedCity.microrregiao.mesorregiao.nome : 'Região não encontrada';
    const depositId = this.deposits.find(d => d.depositRegion === this.p.region)?.idDeposit;
    if (depositId) {
      this.p.idDeposit = depositId.toString();
      this.almox.nativeElement.value = depositId;
      const depositName = this.deposits.find(d => d.idDeposit = depositId)?.depositName;
      this.depositName = depositName ? depositName : '';
      this.depositSelected = true;
    }

  }

  addStreet() {
    const street: { name: string, items: { itemName: string, itemQuantity: string }[]} = {
      name: '',
      items: [
        {itemName: '', itemQuantity: ''}
      ]
    }

    this.p.streets.push(street);
  }

  removeStreet(index: number) {
    this.p.streets.splice(index, 1);
  }

  removeLastSteet() {
    this.p.streets.pop();
  }

  choseItems() {
    this.depositMessage = this.p.idDeposit ? '' : 'Selecione o almoxarifado';
    this.openModalItens = !!this.p.idDeposit;

  }


}
