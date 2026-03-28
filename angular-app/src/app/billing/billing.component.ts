import {Component, OnInit, OnDestroy} from '@angular/core';
import {CommonModule} from '@angular/common';
import {FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators} from '@angular/forms';
import {ActivatedRoute, Router} from '@angular/router';
import {BillingService} from './services/billing.service';
import {
    SubscriptionStatusResponse,
    BillingCycle,
    SubscriptionStatus
} from '../models/billing.model';
import {Subject, takeUntil} from 'rxjs';

// PrimeNG
import { ButtonModule } from 'primeng/button';
import { InputGroupModule } from 'primeng/inputgroup';
import { InputTextModule } from 'primeng/inputtext';
import { DropdownModule } from 'primeng/dropdown';
import { CardModule } from 'primeng/card';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { ToastModule } from 'primeng/toast';
import { MessageService } from 'primeng/api';
import { TabViewModule } from 'primeng/tabview';
import { DividerModule } from 'primeng/divider';
import { TagModule } from 'primeng/tag';
import {DatePipe} from '@angular/common';

@Component({
    selector: 'app-billing',
    standalone: true,
    imports: [
        CommonModule,
        FormsModule,
        ReactiveFormsModule,
        ButtonModule,
        InputGroupModule,
        InputTextModule,
        DropdownModule,
        CardModule,
        ProgressSpinnerModule,
        ToastModule,
        TabViewModule,
        DividerModule,
        TagModule,
        DatePipe
    ],
    providers: [MessageService],
    templateUrl: './billing.component.html',
    styleUrls: ['./billing.component.scss']
})
export class BillingComponent implements OnInit, OnDestroy {
  subscriptionStatus: SubscriptionStatusResponse | null = null;
  loading = true;
  error: string | null = null;
  actionInProgress = false;
  showRenewForm = false;
  showUpgradeForm = false;

  renewForm: FormGroup;
  upgradeForm: FormGroup;

  billingCycleOptions = [
    { label: 'Mensal', value: BillingCycle.MONTHLY },
    { label: 'Anual', value: BillingCycle.YEARLY }
  ];

  reasonCode: string | null = null;
  reasonMap: Record<string, string> = {
    'teste_finalizado': 'Seu período de teste foi finalizado',
    'expirado': 'Sua assinatura expirou',
    'cancelado': 'Sua assinatura foi cancelada'
  };

  private destroy$ = new Subject<void>();

  constructor(
    private billingService: BillingService,
    private route: ActivatedRoute,
    private router: Router,
    private fb: FormBuilder,
    private messageService: MessageService
  ) {
    this.renewForm = this.fb.group({
      billingCycle: [BillingCycle.MONTHLY, Validators.required],
      planName: [null]
    });

    this.upgradeForm = this.fb.group({
      targetPlanName: ['', Validators.required]
    });
  }

  ngOnInit(): void {
    this.route.queryParams.pipe(takeUntil(this.destroy$)).subscribe(params => {
      this.reasonCode = params['motivo'] || null;
    });

    this.loadSubscriptionStatus();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  loadSubscriptionStatus(): void {
    this.loading = true;
    this.error = null;

    this.billingService.getSubscriptionStatus()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (data) => {
          this.subscriptionStatus = data;
          this.loading = false;
        },
        error: (err) => {
          console.error('Erro ao carregar status da assinatura:', err);
          this.error = 'Não foi possível carregar o status da sua assinatura. Tente novamente.';
          this.messageService.add({
            severity: 'error',
            summary: 'Erro',
            detail: this.error
          });
          this.loading = false;
        }
      });
  }

  onRenewSubmit(): void {
    if (this.renewForm.invalid) return;

    this.actionInProgress = true;
    const request = {
      billingCycle: this.renewForm.get('billingCycle')?.value,
      planName: this.renewForm.get('planName')?.value
    };

    // this.billingService.renewSubscription(request)
    //   .pipe(takeUntil(this.destroy$))
    //   .subscribe({
    //     next: (response) => {
    //       this.subscriptionStatus = {
    //         ...this.subscriptionStatus,
    //         ...response
    //       };
    //       this.messageService.add({
    //         severity: 'success',
    //         summary: 'Sucesso',
    //         detail: 'Assinatura renovada com sucesso!'
    //       });
    //       this.actionInProgress = false;
    //     },
    //     error: (err) => {
    //       console.error('Erro ao renovar assinatura:', err);
    //       const errorMsg = err.error?.message || 'Erro ao renovar assinatura. Tente novamente.';
    //       this.messageService.add({
    //         severity: 'error',
    //         summary: 'Erro',
    //         detail: errorMsg
    //       });
    //       this.actionInProgress = false;
    //     }
    //   });
  }

  onUpgradeSubmit(): void {
    if (this.upgradeForm.invalid) return;

    this.actionInProgress = true;
    const request = {
      targetPlanName: this.upgradeForm.get('targetPlanName')?.value
    };

    // this.billingService.upgradeSubscription(request)
    //   .pipe(takeUntil(this.destroy$))
    //   .subscribe({
    //     next: (response) => {
    //       this.subscriptionStatus = {
    //         ...this.subscriptionStatus,
    //         ...response
    //       };
    //       this.messageService.add({
    //         severity: 'success',
    //         summary: 'Sucesso',
    //         detail: 'Plano atualizado com sucesso!'
    //       });
    //       this.actionInProgress = false;
    //     },
    //     error: (err) => {
    //       console.error('Erro ao fazer upgrade:', err);
    //       const errorMsg = err.error?.message || 'Erro ao fazer upgrade. Tente novamente.';
    //       this.messageService.add({
    //         severity: 'error',
    //         summary: 'Erro',
    //         detail: errorMsg
    //       });
    //       this.actionInProgress = false;
    //     }
    //   });
  }

  processPayment(): void {
    this.actionInProgress = true;
    this.billingService.processPayment({})
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (response) => {
          console.log('Pagamento processado:', response);
          this.messageService.add({
            severity: 'success',
            summary: 'Sucesso',
            detail: 'Pagamento processado com sucesso!'
          });
          this.actionInProgress = false;
        },
        error: (err) => {
          console.error('Erro ao processar pagamento:', err);
          const errorMsg = err.error?.message || 'Erro ao processar pagamento. Tente novamente.';
          this.messageService.add({
            severity: 'error',
            summary: 'Erro',
            detail: errorMsg
          });
          this.actionInProgress = false;
        }
      });
  }

  goToDashboard(): void {
    this.router.navigate(['/dashboard']);
  }

  getReason(): string {
    return this.reasonCode ? this.reasonMap[this.reasonCode] || 'Acesso indisponível' : '';
  }

  getStatusSeverity(): "success" | "info" | "danger" | "secondary" | "warn" | "contrast"  {
    const status = this.subscriptionStatus?.status?.toLowerCase() || 'unknown';
    const severityMap: Record<string, "success" | "info" | "danger" | "secondary" | "warn" | "contrast"> = {
      'active': 'success',
      'trial': 'info',
      'past_due': 'warn',
      'canceled': 'danger',
      'expired': 'danger',
      'unknown': 'secondary'
    };
    return severityMap[status] || 'secondary';
  }

  getAccessSeverity(): 'success' | 'danger' {
    return this.subscriptionStatus?.accessAllowed ? 'success' : 'danger';
  }

  toggleRenewForm(): void {
    this.showRenewForm = !this.showRenewForm;
    this.showUpgradeForm = false;
  }

  toggleUpgradeForm(): void {
    this.showUpgradeForm = !this.showUpgradeForm;
    this.showRenewForm = false;
  }
}

