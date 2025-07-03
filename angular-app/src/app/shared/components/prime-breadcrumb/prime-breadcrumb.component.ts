import {Component, Input, OnChanges, SimpleChanges} from '@angular/core';
import {MenuItem} from 'primeng/api';
import {Breadcrumb} from 'primeng/breadcrumb';

@Component({
  selector: 'app-prime-breadcrumb',
  standalone: true,
  imports: [
    Breadcrumb
  ],
  templateUrl: './prime-breadcrumb.component.html',
  styleUrl: './prime-breadcrumb.component.scss'
})
export class PrimeBreadcrumbComponent implements OnChanges {
  @Input() title: string | null = null;
  @Input() path: string[] | null = null;

  home: MenuItem = {icon: 'pi pi-home', routerLink: '/'};
  items: MenuItem[] | undefined = undefined

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['path'] && this.path) {
      const label1 = this.path[0] || 'Padrão 1';
      const label2 = this.path[1] || 'Padrão 2';

      this.items = [
        {label: label1},
        {label: label2}
      ];
    } else {
      // fallback se path for null
      this.items = [
        {label: 'Não definido'},
        {label: 'Não definido'}
      ];
    }
  }


}
