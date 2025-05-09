import {Component, ElementRef, OnInit, ViewChild} from '@angular/core';
import {ModalComponent} from "../../../../shared/components/modal/modal.component";
import {NgForOf, NgIf} from "@angular/common";
import {ScreenMessageComponent} from "../../../../shared/components/screen-message/screen-message.component";
import {PreMeasurementModel} from '../../../../models/pre-measurement.model';
import {PreMeasurementService} from '../../../../executions/pre-measurement-home/premeasurement-service.service';
import {UtilsService} from '../../../../core/service/utils.service';
import {ActivatedRoute, Router} from '@angular/router';
import {ContractService} from '../../../services/contract.service';
import {ContractResponse} from '../../../contract-models';
import {LoadingComponent} from '../../../../shared/components/loading/loading.component';

@Component({
  selector: 'app-contract-list',
  standalone: true,
  imports: [
    ModalComponent,
    NgForOf,
    NgIf,
    ScreenMessageComponent,
    LoadingComponent
  ],
  templateUrl: './contract-list.component.html',
  styleUrl: './contract-list.component.scss'
})
export class ContractListComponent implements OnInit {
  contracts: ContractResponse[] = [];

  loading: boolean = false;
  protected status: string = "";
  openModal: boolean = false;
  preMeasurementId: number = 0;
  city: string = '';

  constructor(
    private contractService: ContractService,
    public utils: UtilsService,
    protected router: Router,
  ) {
  }

  ngOnInit() {
    this.loading = true
    this.contractService.getAllContracts().subscribe(c => {
      this.contracts = c;
      this.loading = false;
    });
  }

  // private loadPreMeasurements() {
  //   switch (this.status) {
  //     case 'pendente':
  //       this.preMeasurementService.getPreMeasurements('pending').subscribe(preMeasurements => {
  //         this.preMeasurements = preMeasurements;
  //         this.city = this.preMeasurements[0].streets[0].city;
  //       });
  //       break;
  //     case 'aguardando-retorno':
  //       this.preMeasurementService.getPreMeasurements('waiting').subscribe(preMeasurements => {
  //         this.preMeasurements = preMeasurements;
  //         this.city = this.preMeasurements[0].streets[0].city;
  //       });
  //       break;
  //     case 'validando':
  //       this.preMeasurementService.getPreMeasurements('validating').subscribe(preMeasurements => {
  //         this.preMeasurements = preMeasurements;
  //         this.city = this.preMeasurements[0].streets[0].city;
  //       });
  //       break;
  //     case 'disponivel':
  //       this.preMeasurementService.getPreMeasurements('available').subscribe(preMeasurements => {
  //         this.preMeasurements = preMeasurements;
  //         this.city = this.preMeasurements[0].streets[0].city;
  //       });
  //       break;
  //   }
  // }


  showItems(dialog: HTMLDialogElement) {
    dialog.show()
  }
}
