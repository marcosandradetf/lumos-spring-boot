import {Component, ViewChild} from '@angular/core';
import {Router} from '@angular/router';
import {
  ItemResponseDTO,
  MaterialInStockDTO,
  ReserveDTOResponse,
  ReserveStreetDTOResponse
} from '../../executions/executions.model';
import {BreadcrumbComponent} from '../../shared/components/breadcrumb/breadcrumb.component';
import {CurrencyPipe, NgForOf, NgIf} from '@angular/common';
import {Toolbar} from 'primeng/toolbar';
import {Button, ButtonDirective} from 'primeng/button';
import {SplitButton} from 'primeng/splitbutton';
import {IconField} from 'primeng/iconfield';
import {InputIcon} from 'primeng/inputicon';
import {MenuItem} from 'primeng/api';
import {InputText} from 'primeng/inputtext';
import {Menubar} from 'primeng/menubar';
import {Carousel, CarouselResponsiveOptions} from 'primeng/carousel';
import {Table, TableModule} from 'primeng/table';
import {MaterialResponse} from '../../models/material-response.dto';
import {MaterialService} from '../../stock/services/material.service';
import {Tag} from 'primeng/tag';
import {Ripple} from 'primeng/ripple';
import {FormsModule} from '@angular/forms';
import {KeyFilter} from 'primeng/keyfilter';
import {UtilsService} from '../../core/service/utils.service';
import {Toast} from 'primeng/toast';
import {Skeleton} from 'primeng/skeleton';
import {Tooltip} from 'primeng/tooltip';
import {ExecutionService} from '../../executions/execution.service';

@Component({
  selector: 'app-reservation-management-select',
  standalone: true,
  imports: [
    BreadcrumbComponent,
    NgIf,
    Button,
    InputText,
    Carousel,
    TableModule,
    Tag,
    CurrencyPipe,
    Ripple,
    FormsModule,
    ButtonDirective,
    Toast,
    Skeleton,
    Tooltip,
  ],
  templateUrl: './reservation-management-select.component.html',
  styleUrl: './reservation-management-select.component.scss'
})
export class ReservationManagementSelectComponent {
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
  reserve: ReserveDTOResponse = {
    description: '',
    streets: []
  };
  description: string = "";

  streetId: number = 0;
  currentItemId: number = 0;
  street: ReserveStreetDTOResponse = {
    preMeasurementStreetId: 0,
    streetName: '',
    latitude: 0,
    longitude: 0,
    prioritized: false,
    comment: '',
    assignedBy: '',
    teamName: '',
    truckDepositName: '',
    items: []
  }


  materials: MaterialInStockDTO[] = [];
  filteredMaterials: MaterialInStockDTO[] = [];

  @ViewChild('table_parent') table!: Table;
  selectedMaterial!: MaterialInStockDTO | null;
  currentMaterialId!: Number;

  constructor(private router: Router,
              protected utils: UtilsService,
              private executionService: ExecutionService) {


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
    this.street = street;
  }


  setQuantity: {
    quantity: number;
  } = {
    quantity: 0
  }

  loading: boolean = false;

  onRowExpand(item: ItemResponseDTO) {
    let linking = item.linking ? item.linking : item.type;
    linking = linking?.toLowerCase();
    this.filteredMaterials = [];
    this.loading = true;
    this.expandedRows = {}; // fecha tudo
    this.expandedRows[item.itemId] = true; // abre apenas o item atual

    this.currentItemId = item.itemId;

    const alreadyLoaded = this.materials.some(m =>
      m.deposit === this.street.truckDepositName &&
      (m.materialLength?.toLowerCase() === linking ||
        m.materialPower?.toLowerCase() === linking ||
        m.materialType?.toLowerCase() === linking)
    );
    if (alreadyLoaded) {
      this.filteredMaterials = this.materials.filter(m =>
       (m.materialLength?.toLowerCase() === linking ||
         m.materialPower?.toLowerCase() === linking ||
         m.materialType?.toLowerCase() === linking) && !m.deposit.toLowerCase().includes("caminhão")
      );
      
      this.filteredMaterials =
        [...this.filteredMaterials ,
          ...this.materials.filter(m =>
            m.deposit === this.street.truckDepositName &&
            (m.materialLength?.toLowerCase() === linking ||
              m.materialType?.toLowerCase() === linking ||
              m.materialPower?.toLowerCase() === linking))];

      this.loading = false;
    } else {
      this.executionService.getStockMaterialForLinking(linking, this.street.truckDepositName).subscribe({
        next: (response) => {
          const news = response.filter(n =>
            !this.materials.some(m =>
              m.materialId === n.materialId && m.deposit === n.deposit
            )
          );

          this.materials = [...this.materials, ...news];
          this.filteredMaterials = news;
        },
        error: (error) => {
          this.utils.showMessage(error.error.message, 'error', 'Erro ao carregar materiais');
        },
        complete: () => {
          this.loading = false;
        }
      });
    }
  }

  expandedRows: { [key: number]: boolean } = {};
  tableSk: any[] = Array.from({length: 5}).map((_, i) => `Item #${i}`);

  Confirm(
    material: MaterialInStockDTO
  ) {
    if (this.currentItemId === 0) this.utils.showMessage("Erro ao editar item, tente novamente", 'error')

    const currentItemIndex = this.street.items.findIndex(i =>
      i.itemId === this.currentItemId
    );
    if (currentItemIndex === -1) this.utils.showMessage("Erro ao editar item, tente novamente", 'error')


    this.street.items[currentItemIndex].materialId = material.materialId;
    this.street.items[currentItemIndex].materialQuantity = this.setQuantity.quantity;


    // 2. Colapsa a linha expandida
    const item = this.street.items.find(i => i.itemId === this.currentItemId);
    if (item) {
      this.table.toggleRow(item); // agora sim, passando o objeto certo
    }

    this.utils.showMessage(`QUANTIDADE: ${this.setQuantity.quantity}\nDESCRIÇÃO: ${material.materialName} ${material.materialPower ?? material.materialLength ?? ''}`, 'success', 'Reserva realizada com sucesso');

    // 3. Limpa variáveis de estado
    this.selectedMaterial = null;
    this.setQuantity = {
      quantity: 0
    };
    this.currentItemId = 0;
  }


  sendData() {
    for (const i of this.street.items) {
      if (i.materialId === 0 || i.materialId === null || i.materialId === undefined) {
        this.utils.showMessage("Existem itens pendentes", 'error', 'Não foi possível salvar');
        return;
      }
    }


    this.streetId = 0;
    // continua com o envio...
  }


  getQuantity(materialId: number) {
    return this.street.items.find(i => i.materialId === materialId)?.materialQuantity || 0;
  }

}
