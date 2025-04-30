import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {NgClass} from '@angular/common';
import {RouterLink, RouterLinkActive} from '@angular/router';

@Component({
  selector: 'app-sidebar',
  standalone: true,
    imports: [
        NgClass,
        RouterLink,
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
  bTogglePreMeasurement = false;
  bToggleExecution = false;
  bToggleStock = true;
  bToggleRequest = false;
  bToggleSettings: boolean = true;

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
    savedMenuState = localStorage.getItem('togglePreMeasurement');
    if (savedMenuState !== null) {
      this.bTogglePreMeasurement = JSON.parse(savedMenuState); // Converte de volta para booleano
    }
    savedMenuState = localStorage.getItem('toggleExecution');
    if (savedMenuState !== null) {
      this.bToggleExecution = JSON.parse(savedMenuState); // Converte de volta para booleano
    }
    savedMenuState = localStorage.getItem('toggleRequest');
    if (savedMenuState !== null) {
      this.bToggleRequest = JSON.parse(savedMenuState); // Converte de volta para booleano
    }
    savedMenuState = localStorage.getItem('toggleSettings');
    if (savedMenuState !== null) {
      this.bToggleSettings = JSON.parse(savedMenuState); // Converte de volta para booleano
    }
  }

  toggleMenu() {
    this.menuOpen = !this.menuOpen;
    this.menuToggle.emit(this.menuOpen);
    localStorage.setItem('menuOpen', JSON.stringify(this.menuOpen));
  }

  toggleStock(open: boolean) {
    localStorage.setItem('toggleStock', JSON.stringify(open));
  }

  togglePreMeasurement(open: boolean) {
    localStorage.setItem('togglePreMeasurement', JSON.stringify(open));
  }

  toggleExecution() {
    localStorage.setItem('toggleExecution', JSON.stringify(this.bToggleExecution));
  }

  toggleRequest() {
    localStorage.setItem('toggleRequest', JSON.stringify(this.bToggleRequest));
  }

  toggleSettings(open: boolean) {
    localStorage.setItem('toggleSettings', JSON.stringify(open));
  }

}
