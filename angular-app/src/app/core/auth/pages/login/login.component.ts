import { Component } from '@angular/core';
import {NgOptimizedImage} from '@angular/common';
import {FormsModule, NgForm} from '@angular/forms';
import {AuthService} from '../../auth.service';
import { Router } from '@angular/router';
import {Title} from '@angular/platform-browser';
import {map} from 'rxjs';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [
    NgOptimizedImage,
    FormsModule
  ],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss',
})
export class LoginComponent {
  username: string = '';
  password: string = '';

  constructor(private authService: AuthService, private router: Router, private titleService:Title) {
    this.titleService.setTitle("Lumos - Login");

    this.authService.isLoggedIn$.pipe(
      map(isLoggedIn => {
        if (isLoggedIn) {
          this.router.navigate(['/']);
        }
        return isLoggedIn;
      })
    ).subscribe();

  }

  login(form: NgForm) {
    if (form.valid) {
      this.authService.login(this.username, this.password).subscribe(
        response => {
          if (response) {
            console.log('Login realizado com sucesso:', response);
            this.router.navigate(['/']); // Redireciona após o login bem-sucedido
          } else {
            console.error('Falha no login: resposta vazia');
          }
        },
        error => {
          console.error('Erro no login:', error);
        }
      );
    }
  }
}
