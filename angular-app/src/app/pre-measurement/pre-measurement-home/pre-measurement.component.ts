import {Component, ElementRef, OnInit, ViewChild} from '@angular/core';
import {PreMeasurementService} from './premeasurement-service.service';
import {NgForOf, NgIf} from '@angular/common';
import {FormsModule} from '@angular/forms';
import {UtilsService} from '../../core/service/utils.service';
import {ActivatedRoute, Router} from '@angular/router';
import {ScreenMessageComponent} from '../../shared/components/screen-message/screen-message.component';
import {ModalComponent} from '../../shared/components/modal/modal.component';
import {LoadingComponent} from '../../shared/components/loading/loading.component';
import {PreMeasurementResponseDTO} from '../pre-measurement-models';
import {Toast} from 'primeng/toast';
import {Title} from '@angular/platform-browser';

@Component({
  selector: 'app-pre-measurement-home',
  standalone: true,
  imports: [
    NgForOf,
    FormsModule,
    NgIf,
    ModalComponent,
    LoadingComponent,
    Toast
  ],
  templateUrl: './pre-measurement.component.html',
  styleUrl: './pre-measurement.component.scss'
})
export class PreMeasurementComponent implements OnInit {
  preMeasurements: PreMeasurementResponseDTO[] = [];

  protected loading: boolean = false;
  protected status: string = "";
  openModal: boolean = false;
  preMeasurementId: number = 0;
  step: number = 0;

  constructor(
    private preMeasurementService: PreMeasurementService,
    public utils: UtilsService,
    protected router: Router,
    private route: ActivatedRoute,
    private titleService: Title
  ) {
  }

  ngOnInit() {
    this.loading = true;
    this.route.paramMap.subscribe(params => {
      const status = params.get('status');
      if (!status) {
        void this.router.navigate(['/']);
        return;
      }

      this.titleService.setTitle("Pré-Medições - " + status.charAt(0).toUpperCase() + status.slice(1));
      this.status = status;
      this.loadPreMeasurements();
    });
  }

  private loadPreMeasurements() {
    let statusParam: string;

    switch (this.status) {
      case 'pendente':
        statusParam = 'pending';
        break;
      case 'aguardando-retorno':
        statusParam = 'waiting';
        break;
      case 'validando':
        statusParam = 'validating';
        break;
      case 'disponivel':
        statusParam = 'available';
        break;
      default:
        return;
    }

    this.loading = true;

    this.preMeasurementService.getPreMeasurements(statusParam).subscribe({
      next: (preMeasurements) => {
        this.preMeasurements = preMeasurements;
      },
      error: (err) => {
        console.error('Erro ao carregar pré-medições:', err);
      },
      complete: () => {
        this.loading = false;
      }
    });
  }


  navigateTo(preMeasurementId: number, step: number) {
    this.preMeasurementId = preMeasurementId;
    this.step = step;
    switch (this.status) {
      case 'pendente':
        void this.router.navigate(['pre-medicao/relatorio/' + preMeasurementId, step], {queryParams: {reason: 'generate'}});
        break;
      case 'aguardando-retorno':
        this.openModal = true;
        break
      case 'validando':
        break;
      case  'disponivel':
        this.openModal = true;
        break;
    }
  }

  getItemsQuantity(preMeasurementId: number) {
    let quantity: number = 0;
    this.preMeasurements.find(p => p.preMeasurementId === preMeasurementId)
      ?.streets.forEach((street) => {
      quantity += street.items.length;
    });

    return quantity;
  }

  getPreMeasurement(preMeasurementId: number) {
    return this.preMeasurements.find(p => p.preMeasurementId === preMeasurementId);
  }

  hideContent = false;
  evolvePreMeasurement() {
    this.loading = true;
    if(this.step === -1) {
      this.utils.showMessage("Erro 01 ao confirmar", 'error');
      return;
    }

    this.preMeasurementService.evolveStatus(this.preMeasurementId, this.step).subscribe({
      error: (error: any) => {
        this.loading = false;
        this.utils.showMessage("Erro ao atualizar o status:", error);
      },
      complete: () => {
        this.loading = false;
        this.openModal = false;
        this.hideContent = true;
      }
    });
  }

  @ViewChild('step1') step1Ref!: ElementRef<HTMLDivElement>;
  @ViewChild('step2') step2Ref!: ElementRef<HTMLDivElement>;
  toggleSteps() {
    const step1 = this.step1Ref.nativeElement;
    const step2 = this.step2Ref.nativeElement;

    step1.classList.add('hidden');
    step2.classList.remove('hidden');
  }


  navigateToExecution(isMultiTeam: boolean) {
    void this.router.navigate(
      ['execucao/pre-medicao', this.preMeasurementId, this.step],
      { queryParams: { multiTeam: isMultiTeam } }
    );
  }

}
