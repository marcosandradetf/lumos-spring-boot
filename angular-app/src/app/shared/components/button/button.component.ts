import {Component, Input} from '@angular/core';

@Component({
  selector: 'app-button',
  standalone: true,
  imports: [
  ],
  templateUrl: './button.component.html',
  styleUrl: './button.component.scss'
})
export class ButtonComponent {
  @Input() title: string = "";
  @Input() action: () => void = () => {};
  @Input() class: string = "";
  @Input() textColor: string = "";
  @Input() typeButton: string | null = null;
  @Input() loading: boolean = false;

}
