import {Component, OnInit} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';
import {NgClass, NgForOf, NgIf} from '@angular/common';
import * as L from 'leaflet';
import {PreMeasurementResponseDTO} from '../../models/pre-measurement-response-d-t.o';
import {PreMeasurementService} from '../pre-measurement-home/premeasurement-service.service';
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

@Component({
  selector: 'app-pre-measurement-available',
  standalone: true,
  imports: [
    NgForOf,
    ModalComponent,
    TableComponent,
    NgIf,
    NgClass,
    ScreenMessageComponent,
    FormsModule
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
    createdAt: '',
    createdBy: '',
    preMeasurementId: 0,
    preMeasurementStyle: '',
    preMeasurementType: '',
    status: '',
    teamName: '',
    totalPrice: '',
    streets: []
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
    firstDepositCityId: number;
    firstDepositCityName: string;
    secondDepositCityId: number;
    secondDepositCityName: string;
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
    firstDepositCityId: 0,
    firstDepositCityName: '',
    secondDepositCityId: 0,
    secondDepositCityName: '',
    street: []
  };


  constructor(private route: ActivatedRoute, protected router: Router, private preMeasurementService: PreMeasurementService,
              private teamService: TeamService, private executionService: PreMeasurementService, protected utils: UtilsService,
              private stockService: EstoqueService) {
  }

  ngOnInit() {
    this.route.queryParams.subscribe(params => {
      this.isMultiTeam = params['multiTeam'] === 'true';
    });
    const preMeasurementId = this.route.snapshot.paramMap.get('id');
    if (preMeasurementId == null) {
      return
    }
    this.reserveDTO.preMeasurementId = Number(preMeasurementId);
    this.loadPreMeasurement(preMeasurementId);
  }

  loadPreMeasurement(id: string) {
    this.preMeasurementService.getPreMeasurement(id).subscribe(preMeasurement => {
      this.preMeasurement = preMeasurement;
    });

    this.teamService.getTeams().subscribe({
      next: (response) => {
        this.teams = response;
      },
      error: (error: { error: { message: string } }) => {
        this.utils.showMessage(error.error.message, true);
      }
    });

    this.stockService.getDeposits().subscribe({
      next: (response) => {
        this.deposits = response;
      },
      error: (error: { error: { message: string } }) => {
        this.utils.showMessage(error.error.message, true);
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

    this.executionService.getStockAvailable(this.preMeasurement.preMeasurementId, Number(team.idTeam)).subscribe({
      next: (response) => {
        this.localStockStreet = response;
        this.truckDepositName = team.depositName;
      },
      error: (error: { message: string }) => {
        this.utils.showMessage(error.message, true);
      }
    });

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
      this.utils.showMessage('Selecione uma equipe para continuar', true);
      return;
    }

    this.preMeasurement.streets[streetIndex].status = 'VALIDATED';
    this.streetId = this.preMeasurement.streets[streetIndex + 1]?.preMeasurementStreetId || 0;
    this.utils.showMessage("Rua pendente salva com sucesso", false);
    this.utils.playSound("pop");
    if (this.streetId === 0) {
      this.finish = true;
      if (this.reserveDTO.firstDepositCityId === 0 || this.reserveDTO.secondDepositCityId === 0) {
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


  deposits: Deposit[] = [];

  openDepositModal: boolean = false;

  toggleSideBar() {
    this.openDepositModal = !this.openDepositModal;

    const audio = new Audio('sci.mp3');
    audio.play().catch(err => {
      console.warn('Erro ao tentar tocar o som:', err);
    });
  }


  selectDeposits(firstDeposit: string, secondDeposit: string) {
    if (firstDeposit === "Selecione" || secondDeposit === "Selecione") {
      this.utils.showMessage("A seleção dos dois almoxarifados reservas são obrigatórias", true);
      return;
    }
    this.reserveDTO.firstDepositCityId = Number(firstDeposit)
    this.reserveDTO.firstDepositCityName = this.deposits.find(d => d.idDeposit === Number(firstDeposit))?.depositName || '';
    this.reserveDTO.secondDepositCityId = Number(secondDeposit)
    this.reserveDTO.secondDepositCityName = this.deposits.find(d => d.idDeposit === Number(firstDeposit))?.depositName || '';

    this.utils.showMessage("Almoxarifados da cidade definidos com sucesso", false);
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
    this.utils.showMessage(message, false);
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
