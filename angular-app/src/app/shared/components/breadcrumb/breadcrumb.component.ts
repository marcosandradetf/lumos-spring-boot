import {Component, Input, OnChanges} from '@angular/core';
import {Breadcrumb} from 'primeng/breadcrumb';
import {MenuItem} from 'primeng/api';

@Component({
  selector: 'app-breadcrumb',
  standalone: true,
  imports: [
    Breadcrumb
  ],
  templateUrl: './breadcrumb.component.html',
  styleUrl: './breadcrumb.component.scss'
})
export class BreadcrumbComponent implements OnChanges {
  @Input() currentTitle: string = '';
  @Input() previousPath: string = '';
  @Input() previousTitle: string = '';

  items: MenuItem[] = [];
  home: MenuItem = { icon: 'pi pi-home', routerLink: '/' };

  ngOnChanges() {
    this.items = [
      { label: this.previousTitle, routerLink: this.previousPath },
      { label: this.currentTitle },
    ];
  }
}

