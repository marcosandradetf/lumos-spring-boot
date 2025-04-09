import {Component, OnInit} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';
import {NgClass, NgForOf, NgIf} from '@angular/common';
import * as L from 'leaflet';
import {PreMeasurementModel} from '../../models/pre-measurement.model';
import {PreMeasurementService} from '../pre-measurement-home/premeasurement-service.service';
import {ModalComponent} from '../../shared/components/modal/modal.component';
import {TeamsModel} from '../../models/teams.model';
import {TeamService} from '../../manage/team/team-service.service';
import {TableComponent} from '../../shared/components/table/table.component';
import {response} from 'express';
import {UtilsService} from '../../core/service/utils.service';
import {ScreenMessageComponent} from '../../shared/components/screen-message/screen-message.component';

@Component({
  selector: 'app-pre-measurement-available',
  standalone: true,
  imports: [
    NgForOf,
    ModalComponent,
    TableComponent,
    NgIf,
    NgClass,
    ScreenMessageComponent
  ],
  templateUrl: './measurement-details.component.html',
  styleUrl: './measurement-details.component.scss'
})
export class MeasurementDetailsComponent implements OnInit {
  isMultiTeam: boolean = false;

  scrollLeft(slider: HTMLElement) {
    slider.scrollBy({left: -300, behavior: 'smooth'});
  }

  scrollRight(slider: HTMLElement) {
    slider.scrollBy({left: 300, behavior: 'smooth'});
  }

  preMeasurement: PreMeasurementModel = {
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
      deposit: string;
      availableQuantity: number
    }[];
    materialsInTruck: {
      materialId: number;
      materialName: string;
      deposit: string;
      availableQuantity: number
    }[];
  }[] = [];

  teams: TeamsModel[] = [];

  reserveDTO: {
    preMeasurementStreetId: number;
    depositId: number;
    teamId: number;
    enjoyTuckDepositOfTeam: boolean;
  }[] = [];


  constructor(private route: ActivatedRoute, protected router: Router, private preMeasurementService: PreMeasurementService,
              private teamService: TeamService, private executionService: PreMeasurementService, protected utils: UtilsService,) {
  }

  ngOnInit() {
    this.route.queryParams.subscribe(params => {
      this.isMultiTeam = params['multiTeam'] === 'true';
    });
    const preMeasurementId = this.route.snapshot.paramMap.get('id');
    if (preMeasurementId == null) {
      return
    }
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
      error: (error: { error: { message: string} }) => {
        this.utils.showMessage(error.error.message, true);
      }
    });
  }

  getStreet(id: number) {
    return this.preMeasurement.streets.find(s => s.preMeasurementStreetId === id);
  }


  protected initMap(street: {
    number: number;
    preMeasurementStreetId: number;
    lastPower: string;
    latitude: number;
    longitude: number;
    street: string;
    hood: string;
    city: string;
    status: string;
    items: {
      preMeasurementStreetItemId: number;
      materialId: number;
      contractItemId: number;
      materialName: string;
      materialType: string;
      materialPower: string;
      materialLength: string;
      materialQuantity: number;
      status: string
    }[]
  }): void {
    const latitude = street.latitude;
    const longitude = street.longitude;
    this.streetId = street.preMeasurementStreetId;

    if (this.map) {
      this.map.remove();
    }

    let reserve = this.reserveDTO.find(r => r.preMeasurementStreetId === this.streetId);

    if (!reserve) {
      reserve = {
        preMeasurementStreetId: street.preMeasurementStreetId,
        depositId: 0,
        teamId: 0,
        enjoyTuckDepositOfTeam: false,
      };
      this.reserveDTO.push(reserve);
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

  toggleButton(warning: HTMLSpanElement, warningText: HTMLSpanElement) {
    if (warning.innerHTML !== 'warning') {
      warning.innerHTML = 'warning';
      warning.classList.add('text-orange-500');
      warningText.innerHTML = 'Com prioridade';
    } else {
      warning.classList.remove('text-orange-500');
      warning.innerHTML = 'warning_amber';
      warningText.innerHTML = 'Sem prioridade';
    }
  }

  openModal: boolean = false;
  teamName: string = "";
  truckDepositName: string = "";

  selectTeam(team: TeamsModel) {
    const streetIndex = this.reserveDTO.findIndex(r => r.preMeasurementStreetId === this.streetId);
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

    this.reserveDTO[streetIndex].teamId = Number(team.idTeam);
    this.teamName = "EQUIPE " + team.teamName.toUpperCase();
    this.openModal = false;
  }

  getReserve() {
    return this.reserveDTO.find(r => r.preMeasurementStreetId === this.streetId) || {
      preMeasurementStreetId: this.streetId,
      depositId: 0,
      teamId: 0,
      enjoyTuckDepositOfTeam: false,
    };
  }

  getTeam(teamId: number) {
    return this.teams.find(t => t.idTeam.toString() === teamId.toString());
  }

  getTruckMaterials(streetId: number) {
    if (!this.localStockStreet) return [];
    return this.localStockStreet.find(s => s.streetId === streetId)?.materialsInTruck || [];
  }

  finishStreet() {
    const streetIndex = this.reserveDTO.findIndex(r => r.preMeasurementStreetId === this.streetId);
    if (streetIndex === -1) {
      return;
    }

    if(this.reserveDTO[streetIndex].teamId === 0) {
      this.utils.showMessage('Selecione uma equipe para continuar', true);
      return;
    }

    this.preMeasurement.streets[streetIndex].status = 'VALIDATED';

  }
}
