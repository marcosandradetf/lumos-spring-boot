import {Component, OnInit} from '@angular/core';
import {PreMeasurementService} from '../pre-measurement-home/premeasurement-service.service';
import {CheckBalanceRequest, PreMeasurementResponseDTO} from '../pre-measurement-models';
import {Title} from '@angular/platform-browser';
import {ActivatedRoute, Router} from '@angular/router';
import {PrimeBreadcrumbComponent} from '../../shared/components/prime-breadcrumb/prime-breadcrumb.component';
import {LoadingOverlayComponent} from '../../shared/components/loading-overlay/loading-overlay.component';
import {NgClass, NgForOf, NgStyle} from '@angular/common';
import {UtilsService} from '../../core/service/utils.service';
import {TableModule} from 'primeng/table';
import {Toast} from 'primeng/toast';
import {Button} from 'primeng/button';
import {Badge} from 'primeng/badge';

@Component({
  selector: 'app-pre-measurement-view',
  standalone: true,
  imports: [
    PrimeBreadcrumbComponent,
    LoadingOverlayComponent,
    TableModule,
    NgClass,
    Toast,
    Button,
    Badge,
    NgStyle
  ],
  templateUrl: './pre-measurement-view.component.html',
  styleUrl: './pre-measurement-view.component.scss'
})
export class PreMeasurementViewComponent implements OnInit {
  loading = false;
  preMeasurement: {
    preMeasurementId: number;
    city: string;
    step: number;
    streets: number;
  } | undefined;

  items: CheckBalanceRequest[] = [];

  constructor(private preMeasurementService: PreMeasurementService,
              private utils: UtilsService,
              private router: Router,
              private route: ActivatedRoute,
              private titleService: Title,) {
  }

  ngOnInit() {
    this.titleService.setTitle('Visualizar Pré-Medição');
    this.loading = true;

    this.route.queryParams.subscribe(params => {
      this.preMeasurement = {
        preMeasurementId: params['id'],
        city: params['description'],
        step: params['step'],
        streets: params['streets'],
      };
    });

    if (this.preMeasurement) {
      this.preMeasurementService.checkBalance(this.preMeasurement.preMeasurementId).subscribe({
        next: (result) => {
          this.items = result;
        },
        error: (error) => {
          this.loading = false;
          this.utils.showMessage(error.error.message, 'error', "Ops, erro ao tentar carregar os itens. Tente novamente!");
        },
        complete: () => {
          this.loading = false;
          if(this.items.some(item => Number(item.totalBalance) < 0)) {
            this.utils.showMessage(
              'Existem 1 ou mais itens sem saldo contratual, revise os itens destacados na tabela acima.',
              'contrast',
              'Atenção',
              true
            )
          }
        }
      });
    } else {
      // void this.router.navigate(['/pre-medicao/pendente']);
    }


  }

  rowClass(item: CheckBalanceRequest) {
    const totalBalance = Number(item.totalBalance);

    if(totalBalance < 0) {
      return 'text-error';
    }

    return '';
  }

  rowStyle(item: CheckBalanceRequest) {

    if (Number(item.totalCurrentBalance) < 0) {
      return { fontWeight: 'bold', fontStyle: 'italic' };
    } else if (Number(item.totalCurrentBalance) < 10) {
      return { fontWeight: 'bold', fontStyle: 'italic' };
    }

    return null;
  }

  getDescription() {
    if (this.preMeasurement) {
      const streets = this.preMeasurement.streets;
      return `${this.preMeasurement.step}ª Etapa - ${this.preMeasurement.city} (${streets > 1 ? streets + ' ruas' : streets + ' rua'})`;
    }

    return null;
  }


  itemSeverity(item: CheckBalanceRequest): "success" | "danger" | "info" | "warn" | "help" | "primary" | "secondary" | "contrast" {
    const totalBalance = Number(item.totalBalance);
    if(totalBalance < 0) {
      return 'danger';
    } else if(totalBalance < 10) {
      return 'info';
    }

    return 'success';
  }

  itemStatus(item: CheckBalanceRequest) {
    const totalBalance = Number(item.totalBalance);
    if(totalBalance < 0) {
      return 'Sem Saldo';
    } else if(totalBalance < 10) {
      return 'Saldo Baixo';
    }

    return 'Saldo Disponivel';
  }
}
