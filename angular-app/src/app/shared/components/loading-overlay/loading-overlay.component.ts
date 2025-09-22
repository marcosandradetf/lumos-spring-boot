import {Component, Input} from '@angular/core';
import {ProgressSpinner} from "primeng/progressspinner";
import {NgIf} from '@angular/common';

@Component({
  selector: 'app-loading-overlay',
  standalone: true,
  imports: [
    ProgressSpinner,
    NgIf
  ],
  templateUrl: './loading-overlay.component.html',
  styleUrl: './loading-overlay.component.scss'
})
export class LoadingOverlayComponent {
  @Input() loading = false;

}
