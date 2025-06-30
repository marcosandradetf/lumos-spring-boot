import {Component, ElementRef, ViewChild} from '@angular/core';
import {Router} from '@angular/router';
import {
  ItemResponseDTO,
  MaterialInStockDTO,
  ReserveDTOResponse,
  ReserveStreetDTOResponse
} from '../../executions/executions.model';
import {BreadcrumbComponent} from '../../shared/components/breadcrumb/breadcrumb.component';
import {CurrencyPipe, NgForOf, NgIf} from '@angular/common';
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
import {Dialog} from 'primeng/dialog';
import {LoadingComponent} from '../../shared/components/loading/loading.component';
import {NgxMaskPipe, provideNgxMask} from 'ngx-mask';


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
    Ripple,
    FormsModule,
    ButtonDirective,
    Toast,
    Skeleton,
    Tooltip,
    Dialog,
    NgForOf,
    LoadingComponent,
    NgxMaskPipe,
  ],
  providers: [provideNgxMask()],
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
  ];
  reserve: ReserveDTOResponse = {
    description: '',
    streets: []
  };
  description: string = "";

  streetId: number | null = null;
  directExecutionId: number | null = null;
  currentItemId: number = 0;

  street: ReserveStreetDTOResponse = {
    preMeasurementStreetId: null,
    directExecutionId: null,
    streetName: '',
    latitude: 0,
    longitude: 0,
    prioritized: false,
    comment: '',
    assignedBy: '',
    teamId: 0,
    teamName: '',
    truckDepositName: '',
    items: []
  }


  materials: MaterialInStockDTO[] = [];
  filteredMaterials: MaterialInStockDTO[] = [];

  @ViewChild('table_parent') table!: Table;

  @ViewChild('table_collapse') tableCollapse: Table | undefined;
  @ViewChild('qtyInput') qtyInput!: ElementRef;

  selectedMaterial!: MaterialInStockDTO | null;
  currentMaterialId: number = 0;
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
      if (this.reserve.streets[0].comment === 'DIRECT_EXECUTION') {
        this.streetId = null;
        this.street = this.reserve.streets[0];
        this.directExecutionId = this.street.directExecutionId;
        this.currentTeamId = this.street.teamId;
      }
    } else {
      void this.router.navigate(['/requisicoes/execucoes/reservas/gerenciamento']);
    }

    this.userUUID = this.authService.getUser().uuid;

  }

  startReservation(street: ReserveStreetDTOResponse) {
    this.streetId = street.preMeasurementStreetId;
    this.description = "Execução na " + street.streetName;
    this.street = street;
    this.currentTeamId = street.teamId;
  }


  quantity: number | null = null;
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
              m.materialStockId === n.materialStockId && m.deposit === n.deposit
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
    material: MaterialInStockDTO, rowElement: HTMLTableRowElement) {
    if (this.currentItemId === 0) {
      this.utils.showMessage("1 - Erro ao reservar material , tente novamente", 'error');
      return;
    }

    const currentItemIndex = this.street.items.findIndex(i =>
      i.itemId === this.currentItemId
    );
    if (currentItemIndex === -1) {
      this.utils.showMessage("2 - Erro ao reservar material, tente novamente", 'error');
      return;
    }

    const quantity = Number(this.quantity);
    if (quantity == 0) {
      this.utils.showMessage("Informe a quantidade desejada.", 'warn', 'Atenção');
      return;
    }

    if (material.availableQuantity < quantity) {
      this.utils.showMessage("O Material informado não possuí estoque disponível, faça a movimentação de estoque.", 'error', "Material sem estoque");
      return;
    }

    const truckMaterialStockId = this.filteredMaterials.find(m => m.materialId === material.materialId && m.deposit === this.street.truckDepositName)?.materialStockId
    if (!truckMaterialStockId) {
      this.utils.showMessage("Referência do material do caminhão não encontrada", "error", "Erro ao salvar referência")
      return;
    }

    const newMaterial = material.deposit.includes("CAMINHÃO")
      ? {
        centralMaterialStockId: null,
        truckMaterialStockId: material.materialStockId,
        materialQuantity: Number(this.quantity),
        materialId: material.materialId,
      }
      : {
        centralMaterialStockId: material.materialStockId,
        truckMaterialStockId: null,
        materialQuantity: Number(this.quantity),
        materialId: material.materialId,
      };

    const materialStockId = material.deposit.includes("CAMINHÃO") ? newMaterial.truckMaterialStockId : newMaterial.centralMaterialStockId;
    if (materialStockId == null) {
      this.utils.showMessage("Id do material não encontrado", "error", "Erro ao salvar referência")
      return;
    }

    if (!this.existsMaterial(materialStockId, material.deposit)) {
      const materials = this.street.items[currentItemIndex].materials;
      if (!materials) this.street.items[currentItemIndex].materials = [];
      this.street.items[currentItemIndex].materials.push(newMaterial);
      this.utils.showMessage(`QUANTIDADE: ${this.quantity}\nDESCRIÇÃO: ${material.materialName} ${material.materialPower ?? material.materialLength ?? ''}`, 'success', 'Reserva realizada com sucesso');
    } else {
      const propToCompare = material.deposit.includes("CAMINHÃO") ? 'truckMaterialStockId' : 'centralMaterialStockId';
      const matIndex = this.street.items[currentItemIndex].materials
        .findIndex(i => i[propToCompare] === materialStockId);

      if (matIndex === -1) {
        this.utils.showMessage("Erro ao editar item, tente novamente", 'error');
      }
      this.street.items[currentItemIndex].materials[matIndex].materialQuantity = newMaterial.materialQuantity;
      this.utils.showMessage(`NOVA QUANTIDADE: ${this.quantity}\nDESCRIÇÃO: ${material.materialName} ${material.materialPower ?? material.materialLength ?? ''}`, 'success', 'Reserva alterada com sucesso');
    }

    this.tableCollapse?.saveRowEdit(material, rowElement);
    this.selectedMaterial = null;
    this.quantity = 0;
    this.currentMaterialId = 0;
  }

  sendData() {
    const hasUndefinedItems = this.street.items.some(i => i.materials === undefined);
    if (hasUndefinedItems) {
      this.utils.showMessage("Existem itens pendentes", 'error', 'Não foi possível salvar');
      return;
    }

    const hasPendingItems = this.street.items.some(i => i.materials.length === 0);
    if (hasPendingItems) {
      this.utils.showMessage("Existem itens pendentes", 'error', 'Não foi possível salvar');
      return;
    }

    this.loading = true;
    this.executionService.reserveMaterialsForExecution(this.street, this.userUUID).subscribe({
      next: (response: any) => {
        if (this.reserve.streets[0].comment === 'DIRECT_EXECUTION') {
          this.reserve.streets.filter(s => s.preMeasurementStreetId !== this.streetId);
          this.directExecutionId = null;
        } else {
          this.reserve.streets = this.reserve.streets.filter(s => s.preMeasurementStreetId !== this.streetId);
          this.streetId = null;
        }

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

  getQuantity(materialStockId: number, deposit: string) {
    if (deposit.toUpperCase().includes("CAMINH")) {
      return this.street.items.find(i => i.itemId === this.currentItemId)
        ?.materials?.find(m => m.truckMaterialStockId == materialStockId)?.materialQuantity || null;
    } else {
      return this.street.items.find(i => i.itemId === this.currentItemId)
        ?.materials?.find(m => m.centralMaterialStockId == materialStockId)?.materialQuantity || null;
    }
  }

  existsMaterial(materialStockId: number, deposit: any): boolean {
    if (deposit.toUpperCase().includes("CAMINH")) {
      return this.street.items.find(i => i.itemId === this.currentItemId)
        ?.materials?.some(m => m.truckMaterialStockId == materialStockId) || false;
    } else {
      return this.street.items.find(i => i.itemId === this.currentItemId)
        ?.materials?.some(m => m.centralMaterialStockId == materialStockId) || false;
    }
  }

  Cancel(material: MaterialInStockDTO, rowElement: HTMLTableRowElement) {
    const index = this.street.items.findIndex(i => i.itemId === this.currentItemId)
    if (index !== -1 && this.existsMaterial(material.materialStockId, material.deposit)) {
      if (material.deposit.includes("CAMINHÃO")) {
        this.street.items[index].materials = this.street.items[index].materials
          .filter(m => m.truckMaterialStockId !== material.materialStockId);
      } else {
        this.street.items[index].materials = this.street.items[index].materials
          .filter(m => m.centralMaterialStockId !== material.materialStockId);
      }

      this.utils.showMessage("Material " + material.materialName + " removido com sucesso", "success", 'Material Removido');
    }

    this.tableCollapse?.saveRowEdit(material, rowElement);
    this.selectedMaterial = null;
    this.quantity = 0.0;
    this.currentMaterialId = 0;
  }

  onRowClick(event: MouseEvent, material: any) {
    // Ignora o clique se foi em um botão (ou dentro de um botão)
    const target = event.target as HTMLElement;
    if (target.closest('button')) return;

    // Se já tem uma linha em edição e não é essa, bloqueia abrir outra
    if (this.currentMaterialId !== 0 && this.currentMaterialId !== material.materialStockId) {
      // Aqui pode até mostrar mensagem alertando o usuário
      this.utils.showMessage('Por favor, conclua ou cancele a edição atual antes de editar outra linha.', 'warn', 'Atenção');
      return; // bloqueia abrir outra linha
    }

    if (this.tableCollapse) {
      this.currentMaterialId = material.materialStockId;
      this.quantity = this.getQuantity(material.materialStockId, material.deposit);
      this.tableCollapse.initRowEdit(material);
      setTimeout(() => {
        this.qtyInput?.nativeElement?.focus();
      }, 0);
    }

  }


  currentTeamId: number = 0;
  showModalTeam: boolean = false;
  users: {
    name: string;
    last_name: string;
    phone_number: string;
    team_id: number;
  }[] = [];

  filteredUsers: {
    name: string;
    last_name: string;
    phone_number: string;
    team_id: number;
  }[] = [];

  verifyTeamData() {
    this.showModalTeam = true;
    this.loading = true;
    this.filteredUsers = [];

    const existingUsers = this.users.filter(user => user.team_id === this.currentTeamId);
    if (existingUsers.length > 0) {
      this.filteredUsers = existingUsers;
      this.loading = false;
      return;
    }

    this.utils.getObject<Array<{ driver_id: string; electrician_id: string }>>({
      fields: ['driver_id', 'electrician_id'],
      table: 'team',
      where: 'id_team',
      equal: [this.currentTeamId]
    }).subscribe({
      next: (teamData) => {
        const uuid: string[] = [];

        teamData.forEach(user => {
          if (user.driver_id) uuid.push(user.driver_id);
          if (user.electrician_id) uuid.push(user.electrician_id);
        });

        if (uuid.length === 0) {
          this.loading = false;
          return;
        }

        this.utils.getObject<Array<{ name: string; last_name: string, phone_number: string }>>({
          fields: ['name', 'last_name', 'phone_number'],
          table: 'app_user',
          where: 'user_id',
          equal: uuid
        }).subscribe({
          next: (userData) => {
            const newUsers = userData.map(user => ({
              name: user.name,
              last_name: user.last_name,
              phone_number: user.phone_number,
              team_id: this.currentTeamId
            }));

            this.users.push(...newUsers);
            this.filteredUsers = newUsers;
          },
          error: (error) => {
            this.utils.showMessage(error.error.message, 'error', 'Erro ao buscar dados da equipe');
          },
          complete: () => {
            this.loading = false;
          }
        });
      },
      error: (error) => {
        this.utils.showMessage(error.error.message, 'error', 'Erro ao buscar equipe');
        this.loading = false;
      }
    });
  }

  getTotalQuantity(materials: {
    centralMaterialStockId: number | null,
    truckMaterialStockId: number | null,
    materialId: number | null,
    materialQuantity: number,
  }[] | undefined): number {
    if (materials) {
      return materials.reduce((total, m) => total + m.materialQuantity, 0);
    } else return 0;

  }

}
