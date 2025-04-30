import { Component } from '@angular/core';
import {Title} from '@angular/platform-browser';

@Component({
  selector: 'app-request-items',
  standalone: true,
  imports: [],
  templateUrl: './request-items.component.html',
  styleUrl: './request-items.component.scss'
})
export class RequestItemsComponent {

  constructor(private titleService: Title) {
    this.titleService.setTitle("Requisições - Itens");
  }
}
