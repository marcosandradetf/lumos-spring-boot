import {Component, Input, OnChanges, SimpleChanges} from '@angular/core';
import {NgClass, NgIf} from "@angular/common";

@Component({
  selector: 'app-alert-message',
  standalone: true,
  imports: [
    NgIf,
    NgClass
  ],
  templateUrl: './alert-message.component.html',
  styleUrl: './alert-message.component.scss'
})
export class AlertMessageComponent implements OnChanges {
  @Input() message: string | null = null;
  @Input() timeout: number = 3000;  // O tempo de exibição da mensagem
  @Input() alertType:string | null = null;

  serverMessage: string | null = null;


  ngOnChanges(changes: SimpleChanges): void {
    if (changes['message'] && this.message) {
      this.serverMessage = this.message;
      setTimeout(() => {
        this.serverMessage = null;
      }, this.timeout);
    }
  }

}
