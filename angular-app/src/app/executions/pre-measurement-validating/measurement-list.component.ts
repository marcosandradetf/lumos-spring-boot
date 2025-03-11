import {Component} from '@angular/core';
import {ExecutionService} from '../execution.service';
import {KeyValuePipe, NgForOf, NgIf} from '@angular/common';
import {Router} from '@angular/router';
import {PreMeasurementService} from '../pre-measurement-pending/premeasurement-service.service';
import {UtilsService} from '../../core/service/utils.service';
import {FormsModule} from "@angular/forms";
import {ModalComponent} from "../../shared/components/modal/modal.component";
import {Title} from '@angular/platform-browser';
import {UserService} from '../../manage/user/user-service.service';
import {AuthService} from '../../core/auth/auth.service';

@Component({
  selector: 'app-pre-measurement-validating',
  standalone: true,
    imports: [
        NgForOf,
        FormsModule,
        ModalComponent,
        NgIf,
    ],
  templateUrl: './measurement-list.component.html',
  styleUrl: './measurement-list.component.scss'
})
export class MeasurementListComponent {
  preMeasurements: {
    preMeasurementId: number;
    city: string;
    createdBy: string;
    createdAt: string;
    preMeasurementType: string;
    preMeasurementStyle: string;
    teamName: string;

    streets: {
      preMeasurementStreetId: number;
      lastPower: string;
      latitude: number;
      longitude: number;
      address: string;

      items: {
        preMeasurementStreetItemId: number;
        materialId: number;
        materialName: string;
        materialType: string;
        materialPower: string;
        materialLength: string;
        materialQuantity: number;
      }[]

    }[];

  }[] = [];
  openModal: boolean = false
  private preMeasurementId: number = 0;

  additionalData: {
    contract: {
      number: string;
      socialReason: string;
      cnpj: string;
      address: string;
      phone: string;
    },
    user: {
      name: string,
      lastname: string,
      email: string,
      phone: string,
      department: string,
    }
  } = {
    contract: {
      number: '',
      socialReason: '',
      cnpj: '',
      address: '',
      phone: ''
    },
    user: {
      name: '',
      lastname: '',
      email: '',
      phone: '',
      department: '',
    }
  }



  constructor(private preMeasurementService: PreMeasurementService, public utils: UtilsService, protected router: Router,
              private titleService: Title, private userService: UserService, protected authService: AuthService,) {
    preMeasurementService.getPreMeasurements('validating').subscribe(preMeasurements => {
      this.preMeasurements = preMeasurements;
    });
    this.titleService.setTitle("Pré-medição - Validando");
    const uuid = authService.getUser().uuid;

    this.userService.getUser(uuid).subscribe(
      user => {
        this.additionalData.user = user;
      });
  }

  getItemsQuantity(preMeasurementId: number) {
    let quantity: number = 0;
    this.preMeasurements.find(p => p.preMeasurementId === preMeasurementId)
      ?.streets.forEach((street) => {
      quantity += street.items.length;
    });

    return quantity;
  }

  submit() {
    this.router.navigate(['pre-medicao/relatorio/' + this.preMeasurementId])
  }


}
