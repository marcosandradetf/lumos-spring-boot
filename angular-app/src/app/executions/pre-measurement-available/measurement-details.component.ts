import {AfterViewInit, Component} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';
import {KeyValuePipe, NgForOf} from '@angular/common';
import * as L from 'leaflet';
import {TableComponent} from '../../shared/components/table/table.component';
import {PreMeasurementModel} from '../../models/pre-measurement.model';

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
export class MeasurementDetailsComponent implements AfterViewInit {
  cards = [
    { title: 'Card 1', description: 'Descrição do card 1' },
    { title: 'Card 2', description: 'Descrição do card 2' },
    { title: 'Card 3', description: 'Descrição do card 3' },
    { title: 'Card 4', description: 'Descrição do card 4' },
    { title: 'Card 5', description: 'Descrição do card 5' },
  ];

  scrollLeft(slider: HTMLElement) {
    slider.scrollBy({ left: -300, behavior: 'smooth' });
  }

  scrollRight(slider: HTMLElement) {
    slider.scrollBy({ left: 300, behavior: 'smooth' });
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

  constructor(private route: ActivatedRoute, protected router: Router) {
    const navigation = this.router.getCurrentNavigation();
    const id = navigation?.extras.state?.['id'];
  }

  private initMap(): void {
    // const latitude = this.preMeasurement.measurement.latitude;
    // const longitude = this.preMeasurement.measurement.longitude;

    // this.map = L.map('map').setView([latitude, longitude], 17); // Coordenadas iniciais


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
    // L.marker([latitude, longitude], { icon: defaultIcon }).addTo(this.map)
    //   .bindPopup('Localização da medição')
    //   .openPopup();
  }

  ngAfterViewInit(): void {
    this.initMap();
  }

  toggleButton(warning: HTMLSpanElement, warningText: HTMLSpanElement) {
    if(warning.innerHTML !== 'warning') {
      warning.innerHTML = 'warning';
      warning.classList.add('text-orange-500');
      warningText.innerHTML = 'Com prioridade';
    }
    else {
      warning.classList.remove('text-orange-500');
      warning.innerHTML = 'warning_amber';
      warningText.innerHTML = 'Sem prioridade';
    }
  }
}
