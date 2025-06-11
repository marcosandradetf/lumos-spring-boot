import {Component, OnInit} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';
import {PreMeasurementService} from '../../pre-measurement/pre-measurement-home/premeasurement-service.service';
import {TeamService} from '../../manage/team/team-service.service';
import {UtilsService} from '../../core/service/utils.service';
import {animate, style, transition, trigger} from '@angular/animations';
import {FormsModule} from '@angular/forms';
import {EstoqueService} from '../../stock/services/estoque.service';
import {Toast} from 'primeng/toast';
import {TableModule} from 'primeng/table';
import {MenuItem, MessageService} from 'primeng/api';
import {AuthService} from '../../core/auth/auth.service';
import {LoadingComponent} from '../../shared/components/loading/loading.component';
import {FloatLabel} from 'primeng/floatlabel';
import {InputText} from 'primeng/inputtext';
import {Select} from 'primeng/select';
import {Breadcrumb} from 'primeng/breadcrumb';
import {NgForOf, NgIf} from '@angular/common';
import {PreMeasurementResponseDTO} from '../../pre-measurement/pre-measurement-models';
import * as L from 'leaflet';
import {Carousel, CarouselResponsiveOptions} from 'primeng/carousel';
import {Button, ButtonDirective} from 'primeng/button';
import {Ripple} from 'primeng/ripple';
import {Skeleton} from 'primeng/skeleton';
import {Tag} from 'primeng/tag';
import {Tooltip} from 'primeng/tooltip';

@Component({
  selector: 'app-execution-without-pre-measurement',
  standalone: true,
  imports: [
    FormsModule,
    Toast,
    TableModule,
    LoadingComponent,
    FloatLabel,
    InputText,
    Select,
    Breadcrumb,
    NgForOf,
    NgIf,
    Carousel,
    Button,
    ButtonDirective,
    Ripple,
    Skeleton,
    Tag,
    Tooltip
  ],
  templateUrl: './execution-without-pre-measurement.component.html',
  styleUrl: './execution-without-pre-measurement.component.scss',
  animations: [
    trigger('fadeSlide', [
      transition(':enter', [ // Aparecendo
        style({opacity: 0, transform: 'translateY(-10px)'}),
        animate('500ms ease-out', style({opacity: 1, transform: 'translateY(0)'}))
      ]),
      transition(':leave', [ // Sumindo
        animate('500ms ease-in', style({opacity: 0, transform: 'translateY(-10px)'}))
      ])
    ])
  ]
})
export class ExecutionWithoutPreMeasurementComponent implements OnInit {
  states = [
    {name: 'AC'},
    {name: 'AL'},
    {name: 'AM'},
    {name: 'AP'},
    {name: 'BA'},
    {name: 'CE'},
    {name: 'DF'},
    {name: 'ES'},
    {name: 'GO'},
    {name: 'MA'},
    {name: 'MG'},
    {name: 'MS'},
    {name: 'MT'},
    {name: 'PA'},
    {name: 'PB'},
    {name: 'PE'},
    {name: 'PI'},
    {name: 'PR'},
    {name: 'RJ'},
    {name: 'RN'},
    {name: 'RO'},
    {name: 'RR'},
    {name: 'RS'},
    {name: 'SC'},
    {name: 'SE'},
    {name: 'SP'},
    {name: 'TO'}
  ];
  home: MenuItem = {icon: 'pi pi-home', routerLink: '/'};
  items: MenuItem[] = [
    {label: 'Execuções'},
    {label: 'Iniciar sem pré-medição'},
  ];

  showMessage: string = '';

  loading: boolean = false;
  private contractId: number = 0;
  streetName: string = '';
  streetNumber: number | null = null;
  hood: string | null = '';
  stateName: string | null = '';
  city: string = '';

  executions: PreMeasurementResponseDTO = {
    city: '',
    contractId: 0,
    preMeasurementId: 0,
    preMeasurementStyle: '',
    preMeasurementType: '',
    status: '',
    teamName: '',
    totalPrice: '',
    depositName: '',
    step: 0,
    streets: [],
  }
  private map!: L.Map;
  streetId: number = 0;


  constructor(private route: ActivatedRoute, protected router: Router, private preMeasurementService: PreMeasurementService,
              private teamService: TeamService, private executionService: PreMeasurementService, protected utils: UtilsService,
              private stockService: EstoqueService, private messageService: MessageService, private authService: AuthService,) {
  }

  ngOnInit() {
    const contractId = this.route.snapshot.paramMap.get('id');
    if (contractId == null) {
      return
    }
    this.contractId = Number(contractId);
  }

  initMap(): void {
    this.map = L.map('map').setView([-23.55052, -46.633308], 13); // Posição inicial (São Paulo)

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution: '© OpenStreetMap contributors',
      maxZoom: 18
    }).addTo(this.map);
  }

  private locationIQKey = "";
  private customIcon = L.icon({
    iconUrl: 'https://cdn-icons-png.flaticon.com/512/684/684908.png',
    iconSize: [40, 40],
    iconAnchor: [20, 40],
    popupAnchor: [0, -35]
  });
  showItems: boolean = false;
  responsiveOptions: CarouselResponsiveOptions[] = [
    {
      breakpoint: '1400px',
      numVisible: 5,
      numScroll: 1
    },
    {
      breakpoint: '1199px',
      numVisible: 3,
      numScroll: 1
    },
    {
      breakpoint: '767px',
      numVisible: 2,
      numScroll: 1
    },
    {
      breakpoint: '575px',
      numVisible: 1,
      numScroll: 1
    }
  ];

  sleep(ms: number) {
    return new Promise(resolve => setTimeout(resolve, ms));
  }
  async searchAddress() {
    this.showItems = true;
    await this.sleep(1000);
    this.initMap();

    const address = `${this.streetName}, ${this.streetNumber} - ${this.hood}, ${this.city}`;
    const url = `https://us1.locationiq.com/v1/search?key=${this.locationIQKey}&q=${encodeURIComponent(address)}&format=json`;

    fetch(url)
      .then(res => res.json())
      .then(data => {
        if (data && data.length > 0) {
          const lat = parseFloat(data[0].lat);
          const lon = parseFloat(data[0].lon);

          if (this.map) {
            this.map.flyTo([lat, lon], 15, {duration: 1.5});

            L.marker([lat, lon], {icon: this.customIcon}).addTo(this.map)
              .bindPopup(`
                <div class="text-xs">
                  ${address}
                </div>
              `)
              .openPopup();
          }
        } else {
          alert('Endereço não encontrado!');
        }
      })
      .catch(err => {
        console.error('Erro ao buscar endereço:', err);
        alert('Erro na busca');
      });
  }



}
