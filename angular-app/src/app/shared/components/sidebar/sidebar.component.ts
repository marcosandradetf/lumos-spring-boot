import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
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
  @Output() menuToggle = new EventEmitter<boolean>();  // Emitir o estado do menu

  menuOpen = false;
  bToggleExecution = true;
  bToggleStock = true;
  bToggleRequest = true;

  constructor() {}

  ngOnInit(): void {
    // Verifica se existe algum valor salvo no localStorage
    let savedMenuState = localStorage.getItem('menuOpen');
    if (savedMenuState !== null) {
      this.menuOpen = JSON.parse(savedMenuState); // Converte de volta para booleano
    }
    savedMenuState = localStorage.getItem('toggleStock');
    if (savedMenuState !== null) {
      this.bToggleStock = JSON.parse(savedMenuState); // Converte de volta para booleano
    }
    savedMenuState = localStorage.getItem('toggleExecution');
    if (savedMenuState !== null) {
      this.bToggleExecution = JSON.parse(savedMenuState); // Converte de volta para booleano
    }
    savedMenuState = localStorage.getItem('toggleRequest');
    if (savedMenuState !== null) {
      this.bToggleRequest = JSON.parse(savedMenuState); // Converte de volta para booleano
    }
  }

  toggleMenu() {
    this.menuOpen = !this.menuOpen;
    this.menuToggle.emit(this.menuOpen);
    localStorage.setItem('menuOpen', JSON.stringify(this.menuOpen));
  }

  toggleStock() {
    this.bToggleStock = !this.bToggleStock;
    localStorage.setItem('toggleStock', JSON.stringify(this.bToggleStock));
  }

  toggleExecution() {
    this.bToggleExecution = !this.bToggleExecution;
    localStorage.setItem('toggleExecution', JSON.stringify(this.bToggleExecution));
  }

  toggleRequest() {
    this.bToggleRequest = !this.bToggleRequest;
    localStorage.setItem('toggleRequest', JSON.stringify(this.bToggleRequest));
  }

}
