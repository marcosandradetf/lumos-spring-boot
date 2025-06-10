import {Component, OnInit} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';
import {PreMeasurementService} from '../../pre-measurement/pre-measurement-home/premeasurement-service.service';
import {TeamService} from '../../manage/team/team-service.service';
import {UtilsService} from '../../core/service/utils.service';
import {animate, style, transition, trigger} from '@angular/animations';
import {FormsModule} from '@angular/forms';
import {EstoqueService} from '../../stock/services/estoque.service';
import {Toast} from 'primeng/toast';
import {TableModule} from 'primeng/table';
import {MessageService} from 'primeng/api';
import {AuthService} from '../../core/auth/auth.service';
import {LoadingComponent} from '../../shared/components/loading/loading.component';
import {FloatLabel} from 'primeng/floatlabel';
import {InputText} from 'primeng/inputtext';

@Component({
  selector: 'app-execution-without-pre-measurement',
  standalone: true,
  imports: [
    FormsModule,
    Toast,
    TableModule,
    LoadingComponent,
    FloatLabel,
    InputText
  ],
  templateUrl: './execution-without-pre-measurement.component.html',
  styleUrl: './execution-without-pre-measurement.component.scss',
  animations: [
    trigger('fadeSlide', [
      transition(':enter', [ // Aparecendo
        style({opacity: 0, transform: 'translateY(-10px)'}),
        animate('500ms ease-out', style({opacity: 1, transform: 'translateY(0)'}))
      ]),
      transition(':leave', [ // Sumindo
        animate('500ms ease-in', style({opacity: 0, transform: 'translateY(-10px)'}))
      ])
    ])
  ]
})
export class ExecutionWithoutPreMeasurementComponent implements OnInit {
  showMessage: string = '';
  loading: boolean = false;

  private contractId: number = 0;
  streetName: string = '';


  constructor(private route: ActivatedRoute, protected router: Router, private preMeasurementService: PreMeasurementService,
              private teamService: TeamService, private executionService: PreMeasurementService, protected utils: UtilsService,
              private stockService: EstoqueService, private messageService: MessageService, private authService: AuthService,) {
  }

  ngOnInit() {
    const contractId = this.route.snapshot.paramMap.get('id');
    if (contractId == null ) {
      return
    }
    this.contractId = Number(contractId);

  }




}
