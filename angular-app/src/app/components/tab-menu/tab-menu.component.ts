import {Component, OnInit} from '@angular/core';
import {TabMenuModule} from 'primeng/tabmenu';
import {MenuItem} from 'primeng/api';

@Component({
  selector: 'app-tab-menu',
  standalone: true,
  imports: [
    TabMenuModule
  ],
  templateUrl: './tab-menu.component.html',
  styleUrl: './tab-menu.component.scss'
})
export class TabMenuComponent implements OnInit {

  items: MenuItem[] | undefined;

  scrollableItems: MenuItem[] | undefined;

  activeItem: MenuItem | undefined;

  activeItem2: MenuItem | undefined;

  ngOnInit() {
    this.items = [
      {label: 'Home', icon: 'pi pi-fw pi-home'},
      {label: 'Calendar', icon: 'pi pi-fw pi-calendar'},
      {label: 'Edit', icon: 'pi pi-fw pi-pencil'},
      {label: 'Documentation', icon: 'pi pi-fw pi-file'},
      {label: 'Settings', icon: 'pi pi-fw pi-cog'}
    ];

    this.scrollableItems = Array.from({ length: 50 }, (_, i) => ({ label: `Tab ${i + 1}`}));

    this.activeItem = this.items[0];

    this.activeItem2 = this.scrollableItems[0];
  }
}
