import {Component, OnInit} from '@angular/core';
import {NavigationEnd, Router, RouterOutlet} from '@angular/router';
import {HeaderComponent} from './shared/components/header/header.component';
import {FooterComponent} from './shared/components/footer/footer.component';
import {AuthService} from './core/auth/auth.service';
import {AsyncPipe, NgClass, NgIf} from '@angular/common';
import {SidebarComponent} from './shared/components/sidebar/sidebar.component';
import {filter} from 'rxjs';
import {UtilsService} from './core/service/utils.service';
import {SidebarDrawerComponent} from './shared/components/sidebar-drawer/sidebar-drawer.component';

@Component({
  selector: 'app-root',
  standalone: true,
    imports: [RouterOutlet, HeaderComponent, FooterComponent, AsyncPipe, SidebarComponent, NgClass, NgIf, SidebarDrawerComponent],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss',
})
export class AppComponent implements OnInit {
  currentUrl: string = '';

  constructor(public authService: AuthService, private router: Router, private utils: UtilsService) {
    this.router.events.pipe(
      filter(event => event instanceof NavigationEnd)
    ).subscribe((event: NavigationEnd) => {
      this.currentUrl = event.urlAfterRedirects;
    });
  }



  menuOpen = false;  // Definir o estado do menu no componente pai

  ngOnInit(): void {
    this.utils.menuState$.subscribe((isOpen: boolean) => {
      this.menuOpen = isOpen;
    });
    let savedMenuState = localStorage.getItem('menuOpen');
    if (savedMenuState !== null) {
      this.menuOpen = JSON.parse(savedMenuState); // Converte de volta para booleano
    }
  }

}
