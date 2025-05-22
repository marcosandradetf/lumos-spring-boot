import { Component } from '@angular/core';
import {ProgressSpinner} from 'primeng/progressspinner';

@Component({
  selector: 'app-loading',
  standalone: true,
  imports: [
    ProgressSpinner
  ],
  templateUrl: './loading.component.html',
  styleUrl: './loading.component.scss'
})
export class LoadingComponent {

}
