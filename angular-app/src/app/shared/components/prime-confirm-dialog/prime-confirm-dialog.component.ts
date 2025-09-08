import {Component, EventEmitter, Input, OnChanges, Output, SimpleChanges} from '@angular/core';
import {ConfirmationService, MessageService} from 'primeng/api';
import {ConfirmDialog} from 'primeng/confirmdialog';
import {ToastModule} from 'primeng/toast';
import {ButtonModule} from 'primeng/button';

@Component({
  selector: 'app-prime-confirm-dialog',
  standalone: true,
  imports: [ConfirmDialog, ToastModule, ButtonModule],
  providers: [ConfirmationService, MessageService],
  templateUrl: './prime-confirm-dialog.component.html',
  styleUrl: './prime-confirm-dialog.component.scss'
})
export class PrimeConfirmDialogComponent implements OnChanges {
  @Input() message: string = '';
  @Input() type: 'confirm' | '' = 'confirm';

  @Output() action = new EventEmitter<'accept' | 'reject'>();

  constructor(
    private confirmationService: ConfirmationService,
    private messageService: MessageService
  ) {}

  ngOnChanges(changes: SimpleChanges): void {
    if (this.type === 'confirm') {
      this.showConfirm();
    }
  }

  private showConfirm() {
    this.confirmationService.confirm({
      message: this.message,
      header: 'Confirmação',
      icon: 'pi pi-exclamation-triangle',

      closable: false,
      closeOnEscape: false,
      rejectButtonProps: {
        label: 'Não',
        severity: 'secondary',
        outlined: true,
      },
      acceptButtonProps: {
        label: 'Sim',
      },
      accept: () => this.action.emit('accept'),
      reject: () => this.action.emit('reject'),
    });
  }

}
