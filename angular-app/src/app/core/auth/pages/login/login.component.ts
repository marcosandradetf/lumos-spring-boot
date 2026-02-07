import {Component, ElementRef, OnInit, ViewChild} from '@angular/core';
import {NgForOf, NgIf, NgOptimizedImage} from '@angular/common';
import { FormsModule, NgForm } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import {Meta, Title} from '@angular/platform-browser';
import { map } from 'rxjs';

import { AuthService } from '../../auth.service';
import { UtilsService } from '../../../service/utils.service';

import { MarkdownModule } from 'ngx-markdown';

import { Toast } from 'primeng/toast';
import { Dialog } from 'primeng/dialog';
import { Button } from 'primeng/button';
import { Card } from 'primeng/card';
import { Password } from 'primeng/password';
import { InputText } from 'primeng/inputtext';
import {Message} from 'primeng/message';


export type DocItem = {
    id: string;
    key: string;     // usado na URL: ?doc=key
    title: string;
    group: string;   // exibido na sidebar
    file: string;    // ex.: "overview.md" (dentro de assets/docs/)
};

@Component({
    selector: 'app-login',
    standalone: true,
    imports: [
        FormsModule,
        NgIf,
        NgForOf,
        Toast,
        Dialog,
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
    rememberMe = true;

    loading = false; // usado no *ngIf geral (QR login)
    authLoading = false;
    authError: string | null = null;

    finished = false;
    message: string = 'Foi enviada uma nova senha para seu email, por favor verifique sua caixa de entrada e spam.';

    redirectPath: string | null = null;

    // ================= MODAIS =================
    demoModal = false;
    subscriptionModal = false;

    // ================= DOCS =================
    docs: DocItem[] = [
        { id: 'overview',     key: 'overview',       title: 'Visão Geral',                 group: 'Comece aqui', file: 'overview.md' },
        { id: 'first-access', key: 'first-access',   title: 'Primeiro Acesso',             group: 'Comece aqui', file: 'first-access.md' },

        { id: 'access',       key: 'access',         title: 'Gestão de Acesso',            group: 'Módulos',     file: 'access.md' },
        { id: 'stock',        key: 'stock',          title: 'Estoque',                     group: 'Módulos',     file: 'stock.md' },
        { id: 'contracts',    key: 'contracts',      title: 'Contratos',                   group: 'Módulos',     file: 'contracts.md' },
        { id: 'execution',    key: 'execution',      title: 'Execução de Serviços',        group: 'Módulos',     file: 'execution.md' },
        { id: 'reports',      key: 'reports',        title: 'Relatórios',                  group: 'Módulos',     file: 'reports.md' },
    ];

    activeDoc = 'overview';
    markdownContent = '';
    docLoading = false;

    docQuery = '';
    filteredDocs: DocItem[] = [...this.docs];

    // ================= HELPERS (títulos no header do doc) =================
    get activeDocTitle(): string {
        return this.docs.find(d => d.id === this.activeDoc)?.title ?? '';
    }

    get activeDocSubtitle(): string {
        return this.docs.find(d => d.id === this.activeDoc)?.group ?? '';
    }

    constructor(
        private authService: AuthService,
        private router: Router,
        private title: Title,
        protected utils: UtilsService,
        private route: ActivatedRoute,
        private meta: Meta
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
            'Lumos – Sistema de Gestão de Iluminação Pública | Acesso'
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
        if (redirect) this.redirectPath = redirect;

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

        // docs: abre doc via query param ?doc=...
        this.route.queryParamMap.subscribe(async params => {
            const key = params.get('doc');
            const doc = this.docs.find(d => d.key === key) ?? this.docs[0];
            await this.selectDoc(doc, false);
        });
    }

    // ================= AUTH METHODS =================
    normalizeUsername() {
        if (!this.username) return;
        const digits = this.username.replace(/\D/g, '');
        if (digits.length === 11) this.username = digits; // CPF puro (backend decide máscara)
    }

    login(form: NgForm) {
        this.authError = null;
        if (!form.valid) return;

        this.authLoading = true;

        this.authService.login(this.username, this.password).subscribe({
            next: () => {
                this.authLoading = false;
                void this.router.navigate([this.redirectPath ?? '/']);
            },
            error: (error) => {
                this.authLoading = false;

                // se seu backend manda error.error.message, ótimo; senão cai no genérico
                const msg = error?.error?.message ?? error?.error ?? 'Usuário ou senha inválidos.';
                this.authError = typeof msg === 'string' ? msg : 'Usuário ou senha inválidos.';
                this.utils.showMessage(this.authError, 'error', 'Não foi possível fazer Login');
            }
        });
    }

    forgetPassword() {
        // aqui você pluga seu endpoint real no futuro
        this.finished = true;
        this.message = 'Uma nova senha foi enviada para seu e-mail (verifique também o spam).';
    }

    // ================= CTA / MODAIS =================
    openDemoModal() {
        this.utils.showMessage('A versão de demonstração estará disponível em breve, aguarde!', 'info', 'Lumos SaaS');
    }

    openSubscriptionModal() {
        window.open('https://api.whatsapp.com/send?phone=5531996808280&text=Olá, tenho interesse em saber mais sobre o Lumos.',
            '_blank');
    }

    startDemo() {
        this.demoModal = false;
        // ajuste para sua rota demo (ex.: /demo)
        // void this.router.navigate(['/demo']);
        this.utils.showMessage('Demonstração: implemente a navegação/token demo aqui.', 'info', 'Lumos™');
    }

    openContact() {
        // padrão: abre modal de assinatura (pode trocar por WhatsApp/email)
        this.subscriptionModal = true;
    }

    // ================= DOCS METHODS =================
    filterDocs() {
        const q = this.docQuery.trim().toLowerCase();

        this.filteredDocs = !q
            ? [...this.docs]
            : this.docs.filter(d =>
                (d.title + ' ' + d.group + ' ' + d.key + ' ' + d.file)
                    .toLowerCase()
                    .includes(q)
            );
    }

    selectDocByKey(key: string) {
        const doc = this.docs.find(d => d.key === key);
        if (doc) void this.selectDoc(doc);
    }

    async selectDoc(doc: DocItem, updateUrl: boolean = true): Promise<void> {
        this.activeDoc = doc.id;
        this.docLoading = true;

        try {
            this.markdownContent = await this.utils.loadFromAssets(doc.file);

            if (updateUrl) {
                void this.router.navigate([], {
                    queryParams: { doc: doc.key },
                    queryParamsHandling: 'merge',
                    replaceUrl: true,
                });
            }
        } catch (e) {
            this.markdownContent = `# Documento não encontrado\n\nNão foi possível carregar **${doc.file}** em \`assets/docs/\`.\n`;
        } finally {
            this.docLoading = false;
        }
    }

    copyDocLink() {
        const key = this.docs.find(d => d.id === this.activeDoc)?.key ?? '';
        const url = `${location.origin}${location.pathname}?doc=${encodeURIComponent(key)}`;
        navigator.clipboard.writeText(url);
        this.utils.showMessage('Link copiado!', 'success', 'Lumos™');
    }

    @ViewChild('doc') docSection!: ElementRef<HTMLElement>;
    scrollToDocs() {
        this.docSection?.nativeElement.scrollIntoView({
            behavior: 'smooth',
            block: 'start'
        });
    }
}
