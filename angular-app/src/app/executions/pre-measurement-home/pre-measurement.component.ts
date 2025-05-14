import {Component, ElementRef, OnInit, ViewChild} from '@angular/core';
import {PreMeasurementService} from './premeasurement-service.service';
import {NgForOf, NgIf} from '@angular/common';
import {FormsModule} from '@angular/forms';
import {UtilsService} from '../../core/service/utils.service';
import {ActivatedRoute, Router} from '@angular/router';
import {ScreenMessageComponent} from '../../shared/components/screen-message/screen-message.component';
import {ModalComponent} from '../../shared/components/modal/modal.component';
import {PreMeasurementResponseDTO} from '../../models/pre-measurement-response-d-t.o';

@Component({
  selector: 'app-pre-measurement-home',
  standalone: true,
  imports: [
    NgForOf,
    FormsModule,
    NgIf,
    ScreenMessageComponent,
    ModalComponent
  ],
  templateUrl: './pre-measurement.component.html',
  styleUrl: './pre-measurement.component.scss'
})
export class PreMeasurementComponent implements OnInit {
  preMeasurements: PreMeasurementResponseDTO[] = [];

  private loading: boolean = false;
  protected status: string = "";
  openModal: boolean = false;
  preMeasurementId: number = 0;
  city: string = '';

  constructor(
    private preMeasurementService: PreMeasurementService,
    public utils: UtilsService,
    protected router: Router,
    private route: ActivatedRoute
  ) {
  }

  ngOnInit() {
    this.route.paramMap.subscribe(params => {
      const status = params.get('status');
      if (!status) {
        void this.router.navigate(['/']);
        return;
      }

      this.status = status;
      this.loadPreMeasurements();
    });
  }

  private loadPreMeasurements() {
    switch (this.status) {
      case 'pendente':
        this.preMeasurementService.getPreMeasurements('pending').subscribe(preMeasurements => {
          this.preMeasurements = preMeasurements;
          this.city = this.preMeasurements[0].streets[0].city;
        });
        break;
      case 'aguardando-retorno':
        this.preMeasurementService.getPreMeasurements('waiting').subscribe(preMeasurements => {
          this.preMeasurements = preMeasurements;
          this.city = this.preMeasurements[0].streets[0].city;
        });
        break;
      case 'validando':
        this.preMeasurementService.getPreMeasurements('validating').subscribe(preMeasurements => {
          this.preMeasurements = preMeasurements;
          this.city = this.preMeasurements[0].streets[0].city;
        });
        break;
      case 'disponivel':
        this.preMeasurementService.getPreMeasurements('available').subscribe(preMeasurements => {
          this.preMeasurements = preMeasurements;
          this.city = this.preMeasurements[0].streets[0].city;
        });
        break;
    }
  }

  navigateTo(preMeasurementId: number) {
    this.preMeasurementId = preMeasurementId;
    switch (this.status) {
      case 'pendente':
        void this.router.navigate(['pre-medicao/relatorio/' + preMeasurementId]);
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
    this.preMeasurementService.evolveStatus(this.preMeasurementId).subscribe({
      error: (error: any) => {
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
      ['execucao/pre-medicao', this.preMeasurementId],
      { queryParams: { multiTeam: isMultiTeam } }
    );
  }

}
