import {AfterViewInit, Component} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';
import {KeyValuePipe, NgForOf} from '@angular/common';
import * as L from 'leaflet';
import {TableComponent} from '../../shared/components/table/table.component';
import {PreMeasurementModel} from '../../models/pre-measurement.model';
import {PreMeasurementService} from '../pre-measurement-home/premeasurement-service.service';

@Component({
  selector: 'app-pre-measurement-available',
  standalone: true,
  imports: [
    KeyValuePipe,
    NgForOf,
    TableComponent
  ],
  templateUrl: './measurement-details.component.html',
  styleUrl: './measurement-details.component.scss'
})
export class MeasurementDetailsComponent {
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

  localStockAll: {
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
  }[] = []

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
  } = {
    streetId: 0,
    materialsInStock: [],
    materialsInTruck: []
  }

  constructor(private route: ActivatedRoute, protected router: Router, private preMeasurementService: PreMeasurementService) {
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
  }

  getStreet(id: number) {
    return this.preMeasurement.streets.find(s => s.preMeasurementStreetId === id);
  }

  protected initMap(latitude: number, longitude: number): void {
    this.map = L.map('map').setView([latitude, longitude], 17); // Coordenadas iniciais

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
      .bindPopup('Localização da medição')
      .openPopup();
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
}
