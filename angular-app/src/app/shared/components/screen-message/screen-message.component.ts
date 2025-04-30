import {Component, Input, OnChanges, OnDestroy, OnInit, SimpleChanges} from '@angular/core';
import {NgClass, NgForOf, NgIf} from "@angular/common";

@Component({
  selector: 'app-screen-message',
  standalone: true,
  imports: [
    NgIf,
    NgClass,
    NgForOf
  ],
  templateUrl: './screen-message.component.html',
  styleUrl: './screen-message.component.scss'
})
export class ScreenMessageComponent {
  @Input() message: string | null  = null;
  @Input() alertType:string | null  = null;
}
