import { NgIf, NgOptimizedImage } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule, NgForm } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Button } from 'primeng/button';
import { Card } from 'primeng/card';
import { InputText } from 'primeng/inputtext';
import { Message } from 'primeng/message';
import { Password } from 'primeng/password';
import { AuthService } from '../../auth.service';

@Component({
    selector: 'app-first-access',
    standalone: true,
    imports: [
        NgIf,
        NgOptimizedImage,
        FormsModule,
        RouterLink,
        Button,
        Card,
        InputText,
        Message,
        Password
    ],
    templateUrl: './first-access.component.html',
    styleUrl: './first-access.component.scss'
})
export class FirstAccessComponent implements OnInit {
    cpf = '';
    activationCode = '';
    newPassword = '';
    confirmPassword = '';
    loading = false;
    errorMessage: string | null = null;

    constructor(
        private readonly authService: AuthService,
        private readonly route: ActivatedRoute,
        private readonly router: Router
    ) {}

    ngOnInit(): void {
        const cpf = this.route.snapshot.queryParamMap.get('cpf');
        if (cpf) {
            this.cpf = cpf.replace(/\D/g, '');
        }
    }

    activate(form: NgForm) {
        this.errorMessage = null;

        if (form.invalid) {
            this.errorMessage = 'Preencha CPF, código de ativação e uma nova senha válida.';
            return;
        }

        if (this.newPassword !== this.confirmPassword) {
            this.errorMessage = 'As senhas informadas não conferem.';
            return;
        }

        this.loading = true;
        this.authService.activateFirstAccess({
            cpf: this.cpf.replace(/\D/g, ''),
            activationCode: this.activationCode.trim(),
            newPassword: this.newPassword
        }).subscribe({
            next: () => {
                this.loading = false;
                void this.router.navigate(['/auth/login'], {
                    queryParams: { activated: '1' }
                });
            },
            error: error => {
                this.loading = false;
                this.errorMessage = error?.error?.message ?? 'Não foi possível concluir a ativação. Revise os dados e tente novamente.';
            }
        });
    }
}
