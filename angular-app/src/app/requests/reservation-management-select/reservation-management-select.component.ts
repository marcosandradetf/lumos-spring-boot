import { Component, ElementRef, ViewChild } from '@angular/core';
import { Router } from '@angular/router';
import {
  ReserveItemRequest,
  MaterialInStockDTO,
  ReserveRequest
} from '../../executions/executions.model';
import { NgForOf, NgIf } from '@angular/common';
import { Button } from 'primeng/button';
import { InputText } from 'primeng/inputtext';
import {  CarouselResponsiveOptions } from 'primeng/carousel';
import { Table, TableModule } from 'primeng/table';
import { Tag } from 'primeng/tag';
import { Ripple } from 'primeng/ripple';
import { FormsModule } from '@angular/forms';
import { UtilsService } from '../../core/service/utils.service';
import { Toast } from 'primeng/toast';
import { Skeleton } from 'primeng/skeleton';
import { Tooltip } from 'primeng/tooltip';
import { ExecutionService } from '../../executions/execution.service';
import { AuthService } from '../../core/auth/auth.service';
import { Dialog } from 'primeng/dialog';
import { LoadingComponent } from '../../shared/components/loading/loading.component';
import { NgxMaskPipe, provideNgxMask } from 'ngx-mask';
import { LoadingOverlayComponent } from '../../shared/components/loading-overlay/loading-overlay.component';
import { PrimeBreadcrumbComponent } from "../../shared/components/prime-breadcrumb/prime-breadcrumb.component";
import { Title } from '@angular/platform-browser';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-reservation-management-select',
  standalone: true,
  imports: [
    NgIf,
    Button,
    InputText,
    TableModule,
    Tag,
    Ripple,
    FormsModule,
    Toast,
    Skeleton,
    Tooltip,
    Dialog,
    NgForOf,
    LoadingComponent,
    NgxMaskPipe,
    LoadingOverlayComponent,
    PrimeBreadcrumbComponent,
    RouterLink
  ],
  providers: [provideNgxMask()],
  templateUrl: './reservation-management-select.component.html',
  styleUrl: './reservation-management-select.component.scss'
})
export class ReservationManagementSelectComponent {
  
  message: string | null = null;
  description: string = "";
  preMeasurementId: number | null = null;
  directExecutionId: number | null = null;
  currentItemId: number = 0;
  materials: MaterialInStockDTO[] = [];
  filteredMaterials: MaterialInStockDTO[] = [];

  selectedMaterial!: MaterialInStockDTO | null;
  currentMaterialId: number = 0;
  private userUUID: string = '';
  
  @ViewChild('table_parent') table!: Table;
  @ViewChild('table_collapse') tableCollapse: Table | undefined;
  @ViewChild('qtyInput') qtyInput!: ElementRef;

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

  reserve: ReserveRequest = {
    description: '',
    preMeasurementId: null,
    directExecutionId: null,
    comment: '',
    assignedBy: '',
    teamId: 0,
    teamName: '',
    truckDepositName: '',
    items: []
  };

  constructor(private router: Router,
    protected utils: UtilsService,
    private authService: AuthService,
    private title: Title,
    private executionService: ExecutionService) {

    this.title.setTitle('Gerenciamento de Estoque - Pré-instalação');


    const navigation = this.router.getCurrentNavigation();
    const state = navigation?.extras.state as { reserve: ReserveRequest };

    if (state?.reserve) {
      this.reserve = state.reserve;
      this.description = this.reserve.description;

      if (this.reserve.preMeasurementId !== null) {
        this.preMeasurementId = this.reserve.preMeasurementId;
      } else {
        this.directExecutionId = this.reserve.directExecutionId;
      }
      this.currentTeamId = this.reserve.teamId;
    } else {
      void this.router.navigate(['/requisicoes/instalacoes/gerenciamento-estoque']);
    }

    this.userUUID = this.authService.getUser().uuid;

  }


  quantity: string | null = null;
  loading: boolean = false;

  onRowExpand(item: ReserveItemRequest) {
    let linking = item.linking ? item.linking : item.type;
    linking = linking?.toLowerCase();

    this.filteredMaterials = [];
    this.loading = true;
    this.expandedRows = {}; // fecha tudo
    this.expandedRows[item.contractItemId] = true; // abre apenas o item atual

    this.currentItemId = item.contractItemId;

    const alreadyLoaded = this.materials.some(m =>
      m.deposit === this.reserve.truckDepositName &&
      (m.materialLength?.toLowerCase() === linking ||
        m.materialPower?.toLowerCase() === linking ||
        m.materialType?.toLowerCase() === linking)
    );

    console.log(this.materials);
    console.log(this.reserve);

    if (alreadyLoaded) {
      console.log('Já carregado, filtrando localmente');
      this.filteredMaterials = this.materials.filter(m =>
        (m.materialLength?.toLowerCase() === linking ||
          m.materialPower?.toLowerCase() === linking ||
          m.materialType?.toLowerCase() === linking) && !m.isTruck
      );

      this.filteredMaterials =
        [...this.filteredMaterials,
        ...this.materials.filter(m =>
          m.deposit === this.reserve.truckDepositName &&
          (m.materialLength?.toLowerCase() === linking ||
            m.materialType?.toLowerCase() === linking ||
            m.materialPower?.toLowerCase() === linking))]
          .sort((a, b) => {
            const depositOrder = a.deposit.localeCompare(b.deposit);
            if (depositOrder !== 0) return depositOrder;
            return Number(b.isTruck) - Number(a.isTruck);
          });

      this.loading = false;
    } else {
      this.executionService.getStockMaterialForLinking(item.linking ?? 'NULL', item.type ?? 'NULL', this.currentTeamId).subscribe({
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
  tableSk: any[] = Array.from({ length: 5 }).map((_, i) => `Item #${i}`);
  allowMeasuredMessage: boolean = true;

  Confirm(
    material: MaterialInStockDTO, rowElement: HTMLTableRowElement) {
    if (this.currentItemId === 0) {
      this.utils.showMessage("1 - Erro ao reservar material , tente novamente", 'error');
      return;
    }

    const currentItemIndex = this.reserve.items.findIndex(i =>
      i.contractItemId === this.currentItemId
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

    if (Number(material.availableQuantity) < quantity) {
      this.utils.showMessage("O Material informado não possuí estoque disponível, faça a movimentação de estoque.", 'error', "Material sem estoque");
      return;
    }

    const truckMaterialStockId = this.filteredMaterials.find(m => m.materialId === material.materialId && m.deposit === this.reserve.truckDepositName)?.materialStockId
    if (!truckMaterialStockId) {
      this.utils.showMessage("Referência do material do caminhão não encontrada", "error", "Erro ao salvar referência")
      return;
    }

    const currentBalance = this.reserve.items[currentItemIndex].currentBalance ?? 0;

    if (quantity > Number(currentBalance)) {
      this.utils.showMessage("A quantidade solicitada é maior que o saldo contratual, verifique.", 'error', 'Saldo Contratual Excedido');
      return;
    }

    const measuredQuanitity = this.reserve.items[currentItemIndex].quantity ?? 0;
    if (quantity < Number(measuredQuanitity) && this.allowMeasuredMessage) {
      const itemName = this.reserve.items[currentItemIndex].description ?? '';
      this.utils.showMessage("A quantidade solicitada é maior que a quantidade alocada, tem certeza que está correto?", 'info', `VERIFIQUE O ${itemName}`, true, 'quantityAlert');
    }

    const newMaterial = material.isTruck
      ? {
        centralMaterialStockId: null,
        truckMaterialStockId: material.materialStockId,
        materialQuantity: this.quantity!!,
        materialId: material.materialId,
      }
      : {
        centralMaterialStockId: material.materialStockId,
        truckMaterialStockId: null,
        materialQuantity: this.quantity!!,
        materialId: material.materialId,
      };

    const materialStockId = material.isTruck ? newMaterial.truckMaterialStockId : newMaterial.centralMaterialStockId;
    if (materialStockId == null) {
      this.utils.showMessage("Id do material não encontrado", "error", "Erro ao salvar referência")
      return;
    }

    if (!this.existsMaterial(material)) {
      const materials = this.reserve.items[currentItemIndex].materials;
      if (!materials) this.reserve.items[currentItemIndex].materials = [];
      this.reserve.items[currentItemIndex].materials.push(newMaterial);
      this.utils.showMessage(`QUANTIDADE: ${this.quantity}\nDESCRIÇÃO: ${material.materialName} ${material.materialPower ?? material.materialLength ?? ''}`, 'success', 'Material alocado com sucesso');
    } else {
      const propToCompare = material.isTruck ? 'truckMaterialStockId' : 'centralMaterialStockId';
      const matIndex = this.reserve.items[currentItemIndex].materials
        .findIndex(i => i[propToCompare] === materialStockId);

      if (matIndex === -1) {
        this.utils.showMessage("Erro ao editar item, tente novamente", 'error');
      }
      this.reserve.items[currentItemIndex].materials[matIndex].materialQuantity = newMaterial.materialQuantity!!;
      this.utils.showMessage(`NOVA QUANTIDADE: ${this.quantity}\nDESCRIÇÃO: ${material.materialName} ${material.materialPower ?? material.materialLength ?? ''}`, 'success', 'Material alterado com sucesso');
    }

    this.tableCollapse?.saveRowEdit(material, rowElement);
    this.selectedMaterial = null;
    this.quantity = "0";
    this.currentMaterialId = 0;
  }

  sendData() {
    const hasUndefinedItems = this.reserve.items.some(i => i.materials === undefined);
    if (hasUndefinedItems) {
      this.utils.showMessage("Existem itens pendentes", 'warn', 'Não foi possível salvar');
      return;
    }

    const hasPendingItems = this.reserve.items.some(i => i.materials.length === 0);
    if (hasPendingItems) {
      this.utils.showMessage("Existem itens pendentes", 'warn', 'Não foi possível salvar');
      return;
    }

    this.loading = true;
    this.executionService.reserveMaterialsForExecution(this.reserve, this.userUUID).subscribe({
      next: (response: any) => {
        this.message = response.message;
      },
      error: (error) => {
        this.utils.showMessage(error.error.error ?? error.message ?? error, 'info', 'Não foi possível salvar', true);
      },
      complete: () => {
        this.loading = false;
      }
    });

  }

  getQuantity(material: MaterialInStockDTO): string | null {
    if (material.isTruck) {
      return this.reserve.items.find(i => i.contractItemId === this.currentItemId)
        ?.materials?.find(m => m.truckMaterialStockId == material.materialStockId)?.materialQuantity || null;
    } else {
      return this.reserve.items.find(i => i.contractItemId === this.currentItemId)
        ?.materials?.find(m => m.centralMaterialStockId == material.materialStockId)?.materialQuantity || null;
    }
  }

  existsMaterial(material: MaterialInStockDTO): boolean {
    if (material.isTruck) {
      return this.reserve.items.find(i => i.contractItemId === this.currentItemId)
        ?.materials?.some(m => m.truckMaterialStockId == material.materialStockId) || false;
    } else {
      return this.reserve.items.find(i => i.contractItemId === this.currentItemId)
        ?.materials?.some(m => m.centralMaterialStockId == material.materialStockId) || false;
    }
  }

  Cancel(material: MaterialInStockDTO, rowElement: HTMLTableRowElement) {
    const index = this.reserve.items.findIndex(i => i.contractItemId === this.currentItemId)
    if (index !== -1 && this.existsMaterial(material)) {
      if (material.isTruck) {
        this.reserve.items[index].materials = this.reserve.items[index].materials
          .filter(m => m.truckMaterialStockId !== material.materialStockId);
      } else {
        this.reserve.items[index].materials = this.reserve.items[index].materials
          .filter(m => m.centralMaterialStockId !== material.materialStockId);
      }

      this.utils.showMessage("Material " + material.materialName + " removido com sucesso", "success", 'Material Removido');
    }

    this.tableCollapse?.saveRowEdit(material, rowElement);
    this.selectedMaterial = null;
    this.quantity = "0";
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
      this.quantity = this.getQuantity(material);
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

    this.utils.getObject<Array<{ user_id: string }>>({
      fields: ['user_id'],
      table: 'app_user',
      where: 'team_id',
      equal: [this.currentTeamId]
    }).subscribe({
      next: (teamData) => {
        const uuid: string[] = [];

        teamData.forEach(user => {
          uuid.push(user.user_id);
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
    materialQuantity: string,
  }[] | undefined): number {
    if (materials) {
      return materials.reduce((total, m) => total + Number(m.materialQuantity), 0);
    } else return 0;
  }

  showComment() {
    if(this.reserve.comment && this.reserve.comment.trim() !== '') {
      this.utils.clearToast('comment');
      this.utils.showMessage(this.reserve.comment, 'info', 'Adicionado por ' + this.reserve.assignedBy, true, 'comment');
    } else {
      this.utils.showMessage('Nenhum comentário adicionado.', 'info', 'Adicionado por ' + this.reserve.assignedBy);
    }

  }

}
