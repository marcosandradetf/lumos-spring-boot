import {Component, ElementRef, OnInit, ViewChild} from '@angular/core';
import {NgIf, NgOptimizedImage} from '@angular/common';
import {FormsModule, NgForm} from '@angular/forms';
import {ActivatedRoute, Router} from '@angular/router';
import {Meta, Title} from '@angular/platform-browser';
import {forkJoin, map} from 'rxjs';
import {AuthService} from '../../auth.service';
import {UtilsService} from '../../../service/utils.service';
import {MarkdownModule} from 'ngx-markdown';
import {Toast} from 'primeng/toast';
import {Button} from 'primeng/button';
import {Card} from 'primeng/card';
import {Password} from 'primeng/password';
import {InputText} from 'primeng/inputtext';
import {Message} from 'primeng/message';
import {Utils} from '../../../service/utils';
import {ContractService} from '../../../../features/contract/services/contract.service';
import {UserService} from '../../../../features/manage/user/user-service.service';
import {TeamService} from '../../../../features/manage/team/team-service.service';
import {StockService} from '../../../../features/stock/services/stock.service';
import {MaterialService} from '../../../../features/stock/services/material.service';



@Component({
    selector: 'app-login',
    standalone: true,
    imports: [
        FormsModule,
        NgIf,
        Toast,
        Button,
        Card,
        Password,
        InputText,
        MarkdownModule,
        Message,
        NgOptimizedImage,
    ],
    templateUrl: './login.component.html',
    styleUrl: './login.component.scss',
})
export class LoginComponent implements OnInit {
    // ================= AUTH =================
    username: string = '';
    password: string = '';

    loading = false; // usado no *ngIf geral (QR login)
    authLoading = false;
    authError: string | null = null;

    // ================= UI STATE =================
    activeTab: 'login' | 'signup' | 'demo' = 'login';

    finished = false;
    message: string = 'Foi enviada uma nova senha para seu email, por favor verifique sua caixa de entrada e spam.';

    redirectPath: string | null = null;

    // ================= MODAIS =================
    demoModal = false;
    subscriptionModal = false;

    constructor(
        private authService: AuthService,
        public router: Router,
        private title: Title,
        protected utils: UtilsService,
        private route: ActivatedRoute,
        private meta: Meta,
        private contractService: ContractService,
        private userService: UserService,
        private teamService: TeamService,
        private stockService: StockService,
        private materialService: MaterialService,
    ) {
        // se já estiver logado, manda pra home
        this.authService.isLoggedIn$.pipe(
            map(isLoggedIn => {
                if (isLoggedIn) void this.router.navigate(['/']);
                return isLoggedIn;
            })
        ).subscribe();
    }

    async ngOnInit(): Promise<void> {
        this.title.setTitle(
            'Lumos IP - Sistema de Gestão de Iluminação Pública | Acesso'
        );

        this.meta.addTags([
            {
                name: 'description',
                content: 'Lumos é um sistema de gestão operacional para iluminação pública, com controle de contratos, estoque, equipes, ordens de serviço e relatórios.'
            },
            {
                name: 'keywords',
                content: 'iluminação pública, sistema de gestão, contratos públicos, estoque, ordem de serviço, manutenção, iluminação LED'
            }
        ]);

        // redirect e token (seu fluxo atual)
        const redirect = this.route.snapshot.queryParamMap.get('redirect');
        const token = this.route.snapshot.queryParamMap.get('token');
        const activated = this.route.snapshot.queryParamMap.get('activated');
        if (redirect) this.redirectPath = redirect;
        if (activated === '1') {
            this.finished = true;
            this.message = 'Conta ativada com sucesso. Faça login com sua nova senha.';
        }

        if (token) {
            this.loading = true;
            this.authService.loginWithQrCodeToken(token).subscribe({
                next: () => {
                    this.loading = false;
                    void this.router.navigateByUrl(this.redirectPath ?? '/');
                },
                error: () => {
                    this.loading = false;
                }
            });
            return;
        }

    }

    // ================= AUTH METHODS =================
    normalizeUsername() {
        if (!this.username) return;
        const digits = this.username.replace(/\D/g, '');
        if (digits.length === 11) this.username = digits; // CPF puro (backend decide máscara)
    }

    login(form: NgForm) {
        this.authError = null;
        this.finished = false;
        this.normalizeUsername();
        if (!form.valid) return;

        this.authLoading = true;

        this.authService.login(this.username, this.password).subscribe({
            next: () => {
                const roles = this.authService.getUser().getRoles();
                const hasPermission = roles.includes('ADMIN') || roles.includes('ANALISTA') || roles.includes('RESPONSAVEL_TECNICO');
                localStorage.setItem('isSupport', this.authService.getUser().support ? 'true' : 'false');
                if (localStorage.getItem('onboarding') || !hasPermission) {

                    if (window.matchMedia('(min-width: 1280px)').matches) {
                        localStorage.setItem('menuOpen', 'true');
                        this.utils.toggleMenu(true);
                    }

                    this.authLoading = false;
                    void this.router.navigate([this.redirectPath ?? '/']);
                } else {
                    this.checkState(this.redirectPath ?? '/');
                }
            },
            error: (error) => {
                this.authLoading = false;
                console.warn(error);

                const errorCode = error?.error?.error ?? error?.error?.code;
                if (errorCode === 'USER_NOT_ACTIVATED') {
                    void this.router.navigate(['/first-access'], {
                        queryParams: {
                            cpf: /^\d{11}$/.test(this.username) ? this.username : null
                        }
                    });
                    return;
                }

                // se seu backend manda error.error.message, ótimo; senão cai no genérico
                const msg = error?.error?.message ?? error?.error ?? 'Usuário ou senha inválidos.';
                this.authError = typeof msg === 'string' ? msg : 'Usuário ou senha inválidos.';
            }
        });
    }

    forgetPassword() {
        // aqui você pluga seu endpoint real no futuro
        this.authError = null;
        this.finished = true;
        this.message = 'Uma nova senha foi enviada para seu e-mail (verifique também o spam).';
    }

    // ================= CTA / MODAIS =================
    openDemoModal() {
        window.location.href = 'https://lumosip.com.br/demonstracao';
    }

    openSubscriptionModal() {
        window.location.href = 'https://lumosip.com.br/teste-gratis';
    }

    checkState(redirectPath: string) {
        this.loading = true;
        forkJoin({
            referenceContractItems: this.contractService.getContractReferenceItems(),
            contracts: this.contractService.getAllContracts(
                {
                    contractor: null,
                    startDate: new Date(new Date().setMonth(new Date().getMonth() - 6)),
                    endDate: new Date(),
                    status: null,
                }
            ),
            users: this.userService.getUsers(),
            teams: this.teamService.getTeams(),
            stockists: this.stockService.getStockists(),
            deposits: this.stockService.getDeposits(),
            materials: this.materialService.getCatalogue(),
        }).subscribe({
            next: ({referenceContractItems, contracts, users, teams, stockists, deposits, materials}) => {
                const a = referenceContractItems.length;
                const b = contracts.length;
                const c = users.length;
                const d = users.filter(user => {
                    const roles = user.role.map(r => r.roleName);
                    return roles.includes('ELETRICISTA') || roles.includes('MOTORISTA')
                }).length;

                const e = teams.length;
                const f = stockists.length;
                const g = deposits.filter(deposit => !deposit.isTruck).length;
                const h = deposits.filter(deposit => deposit.isTruck).length;
                const i = materials.length;

                if (a === 0
                    || b === 0
                    || c === 0
                    || d === 0
                    || e === 0
                    || f === 0
                    || g === 0
                    || h === 0
                    || i === 0
                ) {
                    void this.router.navigate(['/configuracoes/onboarding']);
                } else {
                    localStorage.setItem('onboarding', 'finished');
                    this.utils.setOnboarding(false);
                    void this.router.navigate([redirectPath]);
                }
                if (window.matchMedia('(min-width: 1280px)').matches) {
                    localStorage.setItem('menuOpen', 'true');
                    this.utils.toggleMenu(true);
                }
                this.authLoading = false;
            },
            error: (err) => {
                Utils.handleHttpError(err, this.router);
                this.authLoading = false;
            }
        });
    }
}
