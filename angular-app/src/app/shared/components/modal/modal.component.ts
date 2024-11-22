import {booleanAttribute, Component, EventEmitter, Input, Output, SimpleChanges} from '@angular/core';
import {BehaviorSubject} from 'rxjs';
import {AsyncPipe, NgClass} from '@angular/common';
import {ButtonComponent} from '../button/button.component';

@Component({
  selector: 'app-modal',
  standalone: true,
  imports: [
    AsyncPipe,
    ButtonComponent,
    NgClass
  ],
  templateUrl: './modal.component.html',
  styleUrl: './modal.component.scss'
})
export class ModalComponent {
  @Input() modalOpen: boolean = false; // Recebe a variável do pai
  @Input() confirmation: boolean = false; // Recebe a variável do pai
  @Output() modalClose = new EventEmitter<void>(); // Emite evento quando o modal é fechado



  // Método para detectar clique fora do modal e fechar
  onOutsideClick(event: MouseEvent): void {
    const modalBox = event.target as HTMLElement;
    if (modalBox.classList.contains('modal')) {
      this.closeModal();  // Fecha o modal se o clique for fora da caixa do modal
    }
  }

  closeModal(): void {
    this.modalClose.emit(); // Emite o evento para o pai
  }

}
