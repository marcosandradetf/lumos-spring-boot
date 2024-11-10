import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import {HeaderComponent} from './shared/components/header/header.component';
import {FooterComponent} from './shared/components/footer/footer.component';
import {SidebarComponent} from './shared/components/sidebar/sidebar.component';
import {TopBarComponent} from './shared/components/top-bar/top-bar.component';
import {AuthService} from './core/auth/auth.service';
import {AsyncPipe} from '@angular/common';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, HeaderComponent, FooterComponent, SidebarComponent, TopBarComponent, AsyncPipe],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss',
})
export class AppComponent {
  constructor(public authService: AuthService) {
  }
}
