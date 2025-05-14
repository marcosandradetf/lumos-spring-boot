import {Component, OnInit} from '@angular/core';

import {CurrencyPipe, NgForOf, NgIf} from '@angular/common';
import {PreMeasurementService} from '../../executions/pre-measurement-home/premeasurement-service.service';
import {ActivatedRoute, Router} from '@angular/router';
import {UtilsService} from '../../core/service/utils.service';
import {ModalComponent} from '../../shared/components/modal/modal.component';
import {FormsModule} from '@angular/forms';
import {Title} from '@angular/platform-browser';
import {UserService} from '../../manage/user/user-service.service';
import {AuthService} from '../../core/auth/auth.service';
import {ReportService} from '../../core/service/report-service';
import {environment} from '../../../environments/environment';
import {PreMeasurementResponseDTO} from '../../models/pre-measurement-response-d-t.o';
import {ContractAndItemsResponse} from '../../contract/contract-models';

@Component({
  selector: 'app-pre-measurement-home-report',
  standalone: true,
  imports: [
    NgForOf,
    NgIf,
    ModalComponent,
    FormsModule,
    CurrencyPipe
  ],
  templateUrl: './pre-measurement-report.component.html',
  styleUrl: './pre-measurement-report.component.scss'
})
export class PreMeasurementReportComponent implements OnInit {
  preMeasurement: PreMeasurementResponseDTO = {
    preMeasurementId: 0,
    contractId: 0,
    city: '',
    createdBy: '',
    createdAt: '',
    depositName: '',
    preMeasurementType: '',
    preMeasurementStyle: '',
    teamName: '',
    totalPrice: '',
    status: '',
    streets: []
  };

  contract: ContractAndItemsResponse = {
    contractId: 0,
    number: "",
    contractor: "",
    address: "",
    phone: "",
    cnpj: "",
    contractFile: "",
    noticeFile: "",
    createdBy: '',
    createdAt: '',
    itemQuantity: 0,
    contractStatus: "",
    contractValue: "",
    additiveFile: "",
    items: []
  };
  openModal: boolean = false;
  loading: boolean = true;

  user: {
    username: string,
    name: string,
    lastname: string,
    email: string,
    role: string[],
    status: boolean
  } = {
    username: '',
    name: '',
    lastname: '',
    email: '',
    role: [],
    status: false,
  };

  constructor(protected router: Router, protected utils: UtilsService, private titleService: Title,
              private preMeasurementService: PreMeasurementService, private route: ActivatedRoute, authService: AuthService,
              private userService: UserService, private reportService: ReportService,) {

    const measurementId = this.route.snapshot.paramMap.get('id');
    this.titleService.setTitle("Relatório de Pré-medição");

    const uuid = authService.getUser().uuid;

    if (measurementId) {
      this.preMeasurementService.getPreMeasurement(measurementId).subscribe(preMeasurement => {
        this.preMeasurement = preMeasurement;
        this.preMeasurementService.getContract(preMeasurement.contractId).subscribe(contract => {
          this.contract = contract;
          this.userService.getUser(uuid).subscribe(
            user => {
              this.user = user;
              this.loading = false
            });
        });
      });
    }
  }

  reason: string = "";

  ngOnInit() {
    this.route.queryParams.subscribe(params => {
      this.reason = params['reason'];
    });
  }

  generatePDF(htmlContent: HTMLDivElement): void {
    this.loading = true;
    const contentText = htmlContent.outerHTML.trim();

    if (!contentText) {
      this.utils.showMessage("O conteúdo do relatório está vazio.", true);
      return;
    }

    this.utils.showMessage("Gerando PDF...", false); // Mensagem de carregamento

    this.reportService.generateReportPdf(contentText, this.contract.contractor).subscribe({
      next: (response) => {
        const blob = new Blob([response], {type: 'application/pdf'});
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `relatorio-${this.contract.contractor}.pdf`;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        window.URL.revokeObjectURL(url);

        this.utils.showMessage("PDF gerado com sucesso!", false);
      },
      error: (error) => {
        console.error("Erro ao gerar PDF:", error);
        this.utils.showMessage("Erro ao gerar PDF: " + (error.message || "Erro desconhecido"), true);
      },
      complete: () => {
        if (this.preMeasurement.status === "PENDING") {
          // Possível mudança de status ou exibição de modal
          this.preMeasurementService.evolveStatus(this.preMeasurement.preMeasurementId).subscribe({
            error: (error: any) => {
              this.utils.showMessage("Erro ao atualizar o status:", error);
            },
            complete: () => {
              this.loading = false;
              this.openModal = true;
            }
          });
        } else {
          this.loading = false;
        }
      }
    });
  }

  getTotalQuantity(contractItemId: number) {
    let quantity = 0;
    this.preMeasurement.streets.forEach((street) => {
      street.items.forEach((item) => {
        if (item.contractItemId === contractItemId) {
          quantity += item.measuredQuantity;
        }
      })
    });
    return quantity;
  }

  getTotalPreMeasured(contractItemId: number) {
    let quantity = 0;

    this.preMeasurement.streets.forEach((street) => {
      street.items.forEach((item) => {
        if (item.contractItemId === contractItemId) {
          quantity += item.measuredQuantity;
        }
      });
    });

    return quantity;
  }

  getTotalPrice(contractItemId: number, unitPrice: string) {
    let price = 0.00;

      this.preMeasurement.streets.forEach((street) => {
        street.items.forEach((item) => {
          if (item.contractItemId === contractItemId) {
            price += item.measuredQuantity * parseFloat(unitPrice);
          }
        });
      });

    return price;
  }

  protected readonly parseFloat = parseFloat;

  protected readonly environment = environment;
}
