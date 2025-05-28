import {Component, ViewChild} from '@angular/core';
import {Router} from '@angular/router';
import {
  ItemResponseDTO,
  MaterialInStockDTO,
  ReserveDTOResponse,
  ReserveStreetDTOResponse
} from '../../executions/executions.model';
import {BreadcrumbComponent} from '../../shared/components/breadcrumb/breadcrumb.component';
import {CurrencyPipe, NgIf} from '@angular/common';
import {Button, ButtonDirective} from 'primeng/button';
import {InputText} from 'primeng/inputtext';
import {Carousel, CarouselResponsiveOptions} from 'primeng/carousel';
import {Table, TableModule} from 'primeng/table';
import {Tag} from 'primeng/tag';
import {Ripple} from 'primeng/ripple';
import {FormsModule} from '@angular/forms';
import {UtilsService} from '../../core/service/utils.service';
import {Toast} from 'primeng/toast';
import {Skeleton} from 'primeng/skeleton';
import {Tooltip} from 'primeng/tooltip';
import {ExecutionService} from '../../executions/execution.service';
import {AuthService} from '../../core/auth/auth.service';

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
  private userUUID: string = '';

  constructor(private router: Router,
              protected utils: UtilsService,
              private authService: AuthService,
              private executionService: ExecutionService) {


    const navigation = this.router.getCurrentNavigation();
    const state = navigation?.extras.state as { reserve: ReserveDTOResponse };

    if (state?.reserve) {
      this.reserve = state.reserve;
      this.description = this.reserve.description;
    } else {
      void this.router.navigate(['/requisicoes/execucoes/reservas/gerenciamento']);
    }

    this.userUUID = this.authService.getUser().uuid;

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
    quantity: string;
  } = {
    quantity: ""
  }

  loading: boolean = false;

  onRowExpand(item: ItemResponseDTO) {
    let linking = item.linking ? item.linking : item.type;
    linking = linking?.toLowerCase();

    let type = item.type.toLowerCase();

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
        [...this.filteredMaterials,
          ...this.materials.filter(m =>
            m.deposit === this.street.truckDepositName &&
            (m.materialLength?.toLowerCase() === linking ||
              m.materialType?.toLowerCase() === linking ||
              m.materialPower?.toLowerCase() === linking))];

      this.loading = false;
    } else {
      this.executionService.getStockMaterialForLinking(item.linking ?? 'NULL', item.type ?? 'NULL', this.street.truckDepositName).subscribe({
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
    if (this.currentItemId === 0) {
      this.utils.showMessage("Erro ao reservar material, tente novamente", 'error');
      return;
    }

    const currentItemIndex = this.street.items.findIndex(i =>
      i.itemId === this.currentItemId
    );
    if (currentItemIndex === -1) {
      this.utils.showMessage("Erro ao reservar material, tente novamente", 'error');
      return;
    }

    if (this.street.items[currentItemIndex].quantity > material.availableQuantity) {
      this.utils.showMessage("Erro ao reservar material, tente novamente", 'error');
      return;
    }

    const newMaterial = {
      materialId: material.materialId,
      materialQuantity: Number(this.setQuantity.quantity)
    }

    if (!this.existsMaterial(newMaterial.materialId)) {
      this.street.items[currentItemIndex].materials.push(newMaterial);
      this.utils.showMessage(`QUANTIDADE: ${this.setQuantity.quantity}\nDESCRIÇÃO: ${material.materialName} ${material.materialPower ?? material.materialLength ?? ''}`, 'success', 'Reserva realizada com sucesso');
    } else {
      const matIndex = this.street.items[currentItemIndex].materials
        .findIndex(i => i.materialId === newMaterial.materialId);

      if (matIndex === -1) {
        this.utils.showMessage("Erro ao editar item, tente novamente", 'error');
      }
      this.street.items[currentItemIndex].materials[matIndex].materialQuantity = newMaterial.materialQuantity;
      this.utils.showMessage(`NOVA QUANTIDADE: ${this.setQuantity.quantity}\nDESCRIÇÃO: ${material.materialName} ${material.materialPower ?? material.materialLength ?? ''}`, 'success', 'Reserva alterada com sucesso');
    }

    // 2. Colapsa a linha expandida
    const item = this.street.items.find(i => i.itemId === this.currentItemId);
    if (item) {
      this.table.toggleRow(item); // agora sim, passando o objeto certo
    }

    // 3. Limpa variáveis de estado
    this.selectedMaterial = null;
    this.setQuantity = {
      quantity: ''
    };
    this.currentItemId = 0;
    this.currentMaterialId = 0;
  }


  sendData() {
    for (const i of this.street.items) {
      if (i.materials.length === 0) {
        this.utils.showMessage("Existem itens pendentes", 'error', 'Não foi possível salvar');
        return;
      }
    }

    this.loading = true;
    this.executionService.reserveMaterialsForExecution(this.street, this.userUUID).subscribe({
      next: (response: any) => {
        this.reserve.streets = this.reserve.streets.filter(s => s.preMeasurementStreetId !== this.streetId);
        this.streetId = 0;
        this.utils.showMessage(response.message, 'success', 'Reserva realizada com sucesso', true);
      },
      error: (error) => {
        this.utils.showMessage(error.error.message, 'error', 'Erro ao salvar');
      },
      complete: () => {
        this.loading = false;
      }
    });

  }

  getQuantity(materialId: number) {
    return this.street.items.find(i => i.itemId === this.currentItemId)
      ?.materials.find(m => m.materialId == materialId)?.materialQuantity || 0;
  }

  existsMaterial(materialId: number): boolean {
    return this.street.items.find(i => i.itemId === this.currentItemId)
      ?.materials.some(m => m.materialId == materialId) || false;
  }

  Cancel(material: MaterialInStockDTO) {
    const index = this.street.items.findIndex(i => i.itemId === this.currentItemId)
    if (index === -1) {
      this.utils.showMessage("Não foi possível cancelar o material atual", "error", 'Erro');
      return;
    }

    this.street.items[index].materials = this.street.items[index].materials
      .filter(m => m.materialId !== material.materialId);

    // 2. Colapsa a linha expandida
    const item = this.street.items.find(i => i.itemId === this.currentItemId);
    if (item) {
      this.table.toggleRow(item); // agora sim, passando o objeto certo
    }
    this.utils.showMessage("Material " + material.materialName + " removido com sucesso", "success", 'Material Removido');

    this.selectedMaterial = null;
    this.setQuantity = {
      quantity: ''
    };
    this.currentItemId = 0;
    this.currentMaterialId = 0;
  }

}
