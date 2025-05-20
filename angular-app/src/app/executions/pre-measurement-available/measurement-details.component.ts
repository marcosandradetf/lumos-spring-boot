import {Component, OnInit} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';
import {NgClass, NgForOf, NgIf} from '@angular/common';
import * as L from 'leaflet';
import {PreMeasurementService} from '../../pre-measurement/pre-measurement-home/premeasurement-service.service';
import {ModalComponent} from '../../shared/components/modal/modal.component';
import {TeamsModel} from '../../models/teams.model';
import {TeamService} from '../../manage/team/team-service.service';
import {TableComponent} from '../../shared/components/table/table.component';
import {UtilsService} from '../../core/service/utils.service';
import {ScreenMessageComponent} from '../../shared/components/screen-message/screen-message.component';

import {
  trigger,
  state,
  style,
  animate,
  transition
} from '@angular/animations';
import {FormsModule} from '@angular/forms';
import {EstoqueService} from '../../stock/services/estoque.service';
import {Deposit} from '../../models/almoxarifado.model';
import {PreMeasurementResponseDTO} from '../../pre-measurement/pre-measurement-models';
import {Toast} from 'primeng/toast';
import {DelegateDTO, StockistModel} from '../executions.model';
import {User} from '../../models/user.model';

@Component({
  selector: 'app-pre-measurement-available',
  standalone: true,
  imports: [
    NgForOf,
    ModalComponent,
    TableComponent,
    NgIf,
    NgClass,
    FormsModule,
    Toast
  ],
  templateUrl: './measurement-details.component.html',
  styleUrl: './measurement-details.component.scss',
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
export class MeasurementDetailsComponent implements OnInit {
  isMultiTeam: boolean = false;

  scrollLeft(slider: HTMLElement) {
    slider.scrollBy({left: -300, behavior: 'smooth'});
  }

  scrollRight(slider: HTMLElement) {
    slider.scrollBy({left: 300, behavior: 'smooth'});
  }

  preMeasurement: PreMeasurementResponseDTO = {
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

  localStockStreet: {
    streetId: number;
    materialsInStock: {
      materialId: number;
      materialName: string;
      materialPower: string;
      materialAmp: string;
      materialLength: string;
      deposit: string;
      itemQuantity: number;
      availableQuantity: number
    }[];
    materialsInTruck: {
      materialId: number;
      materialName: string;
      materialPower: string;
      materialAmp: string;
      materialLength: string;
      deposit: string;
      itemQuantity: number;
      availableQuantity: number
    }[];
  }[] = [];

  teams: TeamsModel[] = [];

  reserveDTO: {
    preMeasurementId: number;
    stockistId: string,
    stockistName: string,
    preMeasurementStep: number,
    street: {
      preMeasurementStreetId: number;
      teamId: number;
      teamName: string,
      truckDepositName: string;
      prioritized: boolean;
      comment: string;
    }[]
  } = {
    preMeasurementId: 0,
    stockistId: '',
    stockistName: '',
    preMeasurementStep: 0,
    street: []
  };

  delegateDTO: DelegateDTO = {
    description: '',
    stockistId: '',
    stockistName: '',
    preMeasurementId: 0,
    preMeasurementStep: 0,
    teamId: 0,
  }


  constructor(private route: ActivatedRoute, protected router: Router, private preMeasurementService: PreMeasurementService,
              private teamService: TeamService, private executionService: PreMeasurementService, protected utils: UtilsService,
              private stockService: EstoqueService) {
  }

  ngOnInit() {
    this.route.queryParams.subscribe(params => {
      this.isMultiTeam = params['multiTeam'] === 'true';
    });
    const preMeasurementId = this.route.snapshot.paramMap.get('id');
    const step = this.route.snapshot.paramMap.get('step');
    if (preMeasurementId == null || step == null) {
      return
    }
    this.reserveDTO.preMeasurementId = Number(preMeasurementId);
    this.loadPreMeasurement(preMeasurementId, Number(step));
  }

  loadPreMeasurement(id: string, step: number) {
    this.preMeasurementService.getPreMeasurement(id, step).subscribe(preMeasurement => {
      this.preMeasurement = preMeasurement;
    });

    this.teamService.getTeams().subscribe({
      next: (response) => {
        this.teams = response;
      },
      error: (error: { error: { message: string } }) => {
        this.utils.showMessage(error.error.message, 'error');
      }
    });

    this.stockService.getStockists().subscribe({
      next: (response) => {
        this.stockists = response;
      },
      error: (error: { error: { message: string } }) => {
        this.utils.showMessage(error.error.message, 'error');
        this.utils.showMessage("Erro ao carregar Estoquistas", 'error');
      }
    });
  }

  getStreet(id: number) {
    return this.preMeasurement.streets.find(s => s.preMeasurementStreetId === id);
  }


  protected initMap(street: PreMeasurementResponseDTO['streets'][0]): void {
    const latitude = street.latitude;
    const longitude = street.longitude;
    this.streetId = street.preMeasurementStreetId;

    if (this.map) {
      this.map.remove();
    }

    let reserve = this.reserveDTO.street.find(r => r.preMeasurementStreetId === this.streetId);

    if (!reserve) {
      reserve = {
        preMeasurementStreetId: street.preMeasurementStreetId,
        teamId: 0,
        teamName: '',
        truckDepositName: '',
        prioritized: false,
        comment: ''
      };
      this.reserveDTO.street.push(reserve);
    }

    const team = this.getTeam(reserve.teamId);
    this.teamName = team ? "EQUIPE " + team.teamName.toUpperCase() : 'Nenhuma equipe selecionada'.toUpperCase();
    this.truckDepositName = this.getTeam(reserve.teamId)?.depositName || '';


    setTimeout(() => {
      this.map = L.map('map').setView([latitude, longitude], 15); // Coordenadas iniciais

      L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
        maxZoom: 19,
        attribution: '&copy; <a href="http://www.openstreetmap.org/copyright">OpenStreetMap</a>'
      }).addTo(this.map);

      const defaultIcon = L.icon({
        iconUrl: 'https://unpkg.com/leaflet@1.7.1/dist/images/marker-icon.png', // URL do ícone padrão
        iconSize: [25, 41], // Tamanho padrão do ícone
        iconAnchor: [12, 41], // Posição do ponto de ancoragem (base do ícone)
        popupAnchor: [1, -34], // Posição do popup
        shadowUrl: 'https://unpkg.com/leaflet@1.7.1/dist/images/marker-shadow.png', // Sombra do ícone
        shadowSize: [41, 41] // Tamanho da sombra
      });

      // Adicionar o marcador com o ícone SVG
      L.marker([latitude, longitude], {icon: defaultIcon}).addTo(this.map)
        .bindPopup('Localização da pré-medição')
        .openPopup();

    }, 200);

  }

  openModal: boolean = false;
  teamName: string = "";
  truckDepositName: string = "";

  selectTeam(team: TeamsModel) {
    const streetIndex = this.reserveDTO.street
      .findIndex(r => r.preMeasurementStreetId === this.streetId);
    if (streetIndex === -1) {
      this.openModal = false;
      return;
    }

    this.reserveDTO.street[streetIndex].teamId = Number(team.idTeam);
    this.reserveDTO.street[streetIndex].teamName = team.teamName;
    this.reserveDTO.street[streetIndex].truckDepositName = team.depositName;
    this.teamName = "EQUIPE " + team.teamName.toUpperCase();
    this.openModal = false;
  }

  getReserve() {
    return this.reserveDTO.street.find(r => r.preMeasurementStreetId === this.streetId) ||
      {
        preMeasurementStreetId: this.streetId,
        teamId: 0,
        teamName: '',
        truckDepositName: '',
        prioritized: false,
        comment: ''
      };
  }

  getTeam(teamId: number) {
    return this.teams.find(t => t.idTeam.toString() === teamId.toString());
  }

  getTruckMaterials(streetId: number) {
    if (!this.localStockStreet) return [];
    return this.localStockStreet.find(s => s.streetId === streetId)?.materialsInTruck || [];
  }

  finish: boolean = false;

  finishStreet() {
    const streetIndex = this.reserveDTO.street.findIndex(r => r.preMeasurementStreetId === this.streetId);
    if (streetIndex === -1) {
      return;
    }

    if (this.reserveDTO.street[streetIndex].teamId === 0) {
      this.utils.showMessage('Selecione uma equipe para continuar', 'warn');
      return;
    }

    this.preMeasurement.streets[streetIndex].status = 'VALIDATED';
    this.streetId = this.preMeasurement.streets[streetIndex + 1]?.preMeasurementStreetId || 0;
    this.utils.showMessage("Rua pendente salva com sucesso", 'success');
    this.utils.playSound("pop");
    if (this.streetId === 0) {
      this.finish = true;
      if (this.reserveDTO.stockistId.length === 0) {
        this.toggleSideBar();
      }
      return;
    }
    const street = this.getStreet(this.streetId);
    if (street) this.initMap(street);

  }

  isFullStock(): boolean {
    return this.localStockStreet.find(l => l.streetId == this.streetId)
      ?.materialsInTruck.every(t => t.availableQuantity >= t.itemQuantity) || false;
  }

  getReserveByStreetId(preMeasurementStreetId: number) {
    return this.reserveDTO.street.find(r => r.preMeasurementStreetId === preMeasurementStreetId) ||
      {
        preMeasurementStreetId: this.streetId,
        teamId: 0,
        teamName: '',
        truckDepositName: '',
        prioritized: false,
        comment: ''
      }
  }


  stockists: StockistModel[] = [];

  openDepositModal: boolean = false;

  toggleSideBar() {
    this.openDepositModal = !this.openDepositModal;

    const audio = new Audio('sci.mp3');
    audio.play().catch(err => {
      console.warn('Erro ao tentar tocar o som:', err);
    });
  }


  selectDeposits(stockist: string) {
    if (stockist === "Selecione") {
      this.utils.showMessage("A seleção do estoquista que irá gerenciar as reservas de materiais é obrigatória", 'warn');
      return;
    }
    this.reserveDTO.stockistId = stockist;
    this.reserveDTO.stockistName = this.stockists.find(s => s.userId === stockist)?.name || '';

    this.utils.showMessage("Estoquista responsável pelo gerenciamento definido com sucesso", 'info');
    this.openDepositModal = false;
  }


  togglePriority() {
    const streetIndex = this.reserveDTO.street.findIndex(r => r.preMeasurementStreetId === this.streetId);
    if (streetIndex === -1) {
      return;
    }

    this.utils.playSound("select");

    this.reserveDTO.street[streetIndex].prioritized = !this.reserveDTO.street[streetIndex].prioritized;
    let message: string;
    if (this.reserveDTO.street[streetIndex].prioritized) {
      message = "Prioridade definida para essa rua";
    } else {
      message = "Prioridade removida para essa rua";
    }
    this.utils.showMessage(message, 'info');
  }

  insertComment($event: Event) {
    const streetIndex = this.reserveDTO.street.findIndex(r => r.preMeasurementStreetId === this.streetId);
    if (streetIndex === -1) {
      return;
    }

    this.reserveDTO.street[streetIndex].comment = ($event.target as HTMLInputElement).value;
  }

  getComment() {
    const streetIndex = this.reserveDTO.street.findIndex(r => r.preMeasurementStreetId === this.streetId);
    if (streetIndex === -1) {
      return;
    }

    return this.reserveDTO.street[streetIndex].comment;
  }

  isPriority() {
    const streetIndex = this.reserveDTO.street.findIndex(r => r.preMeasurementStreetId === this.streetId);
    if (streetIndex === -1) {
      return;
    }

    return this.reserveDTO.street[streetIndex].prioritized;
  }
}
