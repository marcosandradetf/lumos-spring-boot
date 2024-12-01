import { Component, Input, OnInit } from '@angular/core';
import {NgClass, NgForOf} from '@angular/common';
import {RouterLink, RouterLinkActive} from '@angular/router';

@Component({
  selector: 'app-sidebar',
  standalone: true,
    imports: [
        NgClass,
        RouterLink,
        NgForOf,
        RouterLinkActive
    ],
  templateUrl: './sidebar.component.html',
  styleUrls: ['./sidebar.component.scss']
})
export class SidebarComponent implements OnInit {
  @Input() title: string = '';
  @Input() links: { title: string; path: string; id: string }[] = [];
  menuOpen = false;

  constructor() {}

  ngOnInit(): void {

  }

  toggleMenu() {
    this.menuOpen = !this.menuOpen;
  }
}
