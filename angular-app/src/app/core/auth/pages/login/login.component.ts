import {Component} from '@angular/core';
import {NgIf, NgOptimizedImage} from '@angular/common';
import {FormsModule, NgForm} from '@angular/forms';
import {AuthService} from '../../auth.service';
import {Router} from '@angular/router';
import {Title} from '@angular/platform-browser';
import {map} from 'rxjs';
import {UtilsService} from '../../../service/utils.service';
import {ScreenMessageComponent} from '../../../../shared/components/screen-message/screen-message.component';
import {Toast} from 'primeng/toast';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [
    FormsModule,
    NgIf,
    Toast
  ],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss',
})
export class LoginComponent {
  username: string = '';
  password: string = '';
  showPassword: boolean = false;
  message: string = 'Foi enviada uma nova senha para seu email, por favor verifique sua caixa de entrada e spam.';

  constructor(private authService: AuthService, private router: Router, private titleService: Title, protected utils: UtilsService) {
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
            void this.router.navigate(['/']); // Redireciona após o login bem-sucedido
          } else {
            this.utils.showMessage("Email/Usuário ou senha incorretos", 'error');
          }
        },
        error => {
          this.utils.showMessage("Email/Usuário ou senha incorretos",  'error');
        }
      );
    }
  }

  togglePasswordVisibility() {
    this.showPassword = !this.showPassword;
  }

  loading = false;
  finished = false;

  forgetPassword(error: HTMLElement) {
    if (this.username.length == 0) {
      error.innerText = 'Campo obrigatório.';
      return;
    }

    this.loading = true;
    this.authService.forgetPassword(this.username).subscribe({
      next: (response) => {
        this.message = response.message;
        error.innerText = '';
        this.finished = true;
        this.loading = false;
      },
      error: error => {
        error.innerText = '';
        this.utils.showMessage(error.message, 'error');
        this.loading = false;
      }
    });
  }
}
