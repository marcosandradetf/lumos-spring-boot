import {Component} from '@angular/core';
import {Router} from '@angular/router';
import {ReserveDTOResponse, ReserveStreetDTOResponse} from '../../executions/executions.model';
import {BreadcrumbComponent} from '../../shared/components/breadcrumb/breadcrumb.component';
import {NgForOf, NgIf} from '@angular/common';
import {Toolbar} from 'primeng/toolbar';
import {Button} from 'primeng/button';
import {SplitButton} from 'primeng/splitbutton';
import {IconField} from 'primeng/iconfield';
import {InputIcon} from 'primeng/inputicon';
import {MenuItem} from 'primeng/api';
import {InputText} from 'primeng/inputtext';
import {Menubar} from 'primeng/menubar';
import {Carousel, CarouselResponsiveOptions} from 'primeng/carousel';

@Component({
  selector: 'app-reservation-management-select',
  standalone: true,
  imports: [
    BreadcrumbComponent,
    NgIf,
    NgForOf,
    Toolbar,
    Button,
    SplitButton,
    IconField,
    InputIcon,
    InputText,
    Menubar,
    Carousel
  ],
  templateUrl: './reservation-management-select.component.html',
  styleUrl: './reservation-management-select.component.scss'
})
export class ReservationManagementSelectComponent {
  reserve: ReserveDTOResponse = {
    description: '',
    streets: []
  };
  streetId: number = 0;
  items: MenuItem[] = [
    {
      label: 'Almoxarifado Galpão BH',
      icon: 'pi pi-search',
      items: [
        {
          label: 'Components',
          icon: 'pi pi-bolt'
        },
        {
          label: 'Blocks',
          icon: 'pi pi-server'
        },
        {
          label: 'UI Kit',
          icon: 'pi pi-pencil'
        },
      ]
    },
  ];
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
  ]
  description: string = "";


  constructor(private router: Router) {
    const navigation = this.router.getCurrentNavigation();
    const state = navigation?.extras.state as { reserve: ReserveDTOResponse };

    if (state?.reserve) {
      this.reserve = state.reserve;
      this.description = this.reserve.description;
    } else {
      void this.router.navigate(['/requisicoes/execucoes/reservas/gerenciamento']);
    }
  }

  scrollLeft(slider: HTMLElement) {
    slider.scrollBy({left: -300, behavior: 'smooth'});
  }

  scrollRight(slider: HTMLElement) {
    slider.scrollBy({left: 300, behavior: 'smooth'});
  }

  startReservation(street: ReserveStreetDTOResponse) {
    this.streetId = street.preMeasurementStreetId;
    this.description = "Execução na " + street.streetName;

  }
}
