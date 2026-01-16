import {Component, OnInit} from '@angular/core';
import {NgIf, NgOptimizedImage} from '@angular/common';
import {FormsModule, NgForm} from '@angular/forms';
import {AuthService} from '../../auth.service';
import {ActivatedRoute, Router} from '@angular/router';
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
export class LoginComponent implements OnInit {
    username: string = '';
    password: string = '';
    showPassword: boolean = false;
    message: string = 'Foi enviada uma nova senha para seu email, por favor verifique sua caixa de entrada e spam.';
    redirectPath: string | null = null;

    constructor(private authService: AuthService,
                private router: Router,
                private titleService: Title,
                protected utils: UtilsService,
                private route: ActivatedRoute,
    ) {


        this.authService.isLoggedIn$.pipe(
            map(isLoggedIn => {
                if (isLoggedIn) {
                    void this.router.navigate(['/']);
                }
                return isLoggedIn;
            })
        ).subscribe();
    }

    ngOnInit(): void {
        this.titleService.setTitle("Lumos - Login");

        const redirect = this.route.snapshot.queryParamMap.get('redirect');
        const token = this.route.snapshot.queryParamMap.get('token');

        if (redirect) {
            this.redirectPath = redirect;
        }

        if (token) {
            this.loading = true;
            this.authService.loginWithQrCodeToken(token).subscribe({
                    next: () => {
                        this.loading = false;
                        if (this.redirectPath) {
                            void this.router.navigateByUrl(this.redirectPath); // Redireciona após o login bem-sucedido
                        } else {
                            void this.router.navigate(['/']);
                        }
                    },
                    error: () => {
                        this.loading = false;
                    }
                }
            );
        }
    }

    login(form: NgForm) {
        if (form.valid) {
            this.authService.login(this.username, this.password).subscribe({
                    next: () => {
                        void this.router.navigate([this.redirectPath ?? '/']); // Redireciona após o login bem-sucedido
                    },
                    error: (error) => {
                        this.utils.showMessage(error.error.message ?? error.error, 'error', "Não foi possível fazer Login");
                    }
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
                this.utils.showMessage("Serviço fora do ar - Solicite ao Administrador", 'error', "Lumos™");
                this.loading = false;
            }
        });
    }
}
