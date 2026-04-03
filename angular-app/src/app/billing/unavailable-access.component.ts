import { Component, OnInit } from '@angular/core';
import { CommonModule, Location } from '@angular/common';
import { Router } from '@angular/router';
import { AuthService } from '../core/auth/auth.service';

// PrimeNG
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { DividerModule } from 'primeng/divider';
import { SharedState } from '../core/service/shared-state';
import { Title } from '@angular/platform-browser';

@Component({
  selector: 'app-unavailable-access',
  standalone: true,
  imports: [CommonModule, ButtonModule, CardModule, DividerModule],
  templateUrl: './unavailable-access.component.html',
  styleUrls: ['./unavailable-access.component.scss']
})
export class UnavailableAccessComponent implements OnInit {
  isAdmin = false;
  loggingOut = false;

  constructor(
    protected router: Router,
    private authService: AuthService,
    private location: Location,
    private title: Title
  ) { }

  ngOnInit(): void {
    SharedState.setCurrentPath(["Acesso Indisponível"]);
    this.title.setTitle('Acesso Indisponível - Lumos');

    const isLocked = localStorage.getItem('isLocked') !== null;
    if (!isLocked) {
      if (window.history.length > 1) {
        this.location.back();
      } else {
        void this.router.navigate(['/dashboard']);
      }
    }
  }

  goToBilling(): void {
    void this.router.navigate(['/cobranca']);
  }

  logout(): void {
    this.loggingOut = true;
    this.authService.logout().subscribe({
      next: () => {
        void this.router.navigate(['/auth/login']);
      },
      error: (err) => {
        console.error('Erro ao fazer logout:', err);
        sessionStorage.clear();
        localStorage.clear();
        void this.router.navigate(['/auth/login']);
      }
    });
  }
}
