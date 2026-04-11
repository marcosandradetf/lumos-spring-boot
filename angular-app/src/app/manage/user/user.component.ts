import { Component } from '@angular/core';
import { NgClass, NgForOf, NgIf } from '@angular/common';
import { DomSanitizer, SafeResourceUrl, Title } from '@angular/platform-browser';
import { FormsModule, NgForm } from '@angular/forms';
import { Router } from '@angular/router';
import { catchError, tap, throwError } from 'rxjs';
import { NgxMaskDirective, NgxMaskPipe, provideNgxMask } from 'ngx-mask';
import { Toast } from 'primeng/toast';
import { MultiSelect } from 'primeng/multiselect';
import { PrimeTemplate } from 'primeng/api';
import { ModalComponent } from '../../shared/components/modal/modal.component';
import { LoadingComponent } from '../../shared/components/loading/loading.component';
import { UtilsService } from '../../core/service/utils.service';
import { AuthService } from '../../core/auth/auth.service';
import { SharedState } from '../../core/service/shared-state';
import { Utils } from '../../core/service/utils';
import {
    ActivationCodeResponse,
    UserActivationStatus,
    UserManagementResponse,
    UserService
} from './user-service.service';

type ManagedUser = {
    userId: string;
    username: string;
    name: string;
    lastname: string;
    email: string;
    cpf: string;
    year: string;
    month: string;
    day: string;
    role: string[];
    status: UserActivationStatus;
    mustChangePassword: boolean;
    activationExpiresAt: string | null;
    sel: boolean;
    show: boolean;
};

type RoleOption = {
    selected: boolean;
    roleId: string;
    roleName: string;
    label: string;
    description: string;
};

type EmbeddedDoc = {
    key: 'invite-users' | 'first-access';
    title: string;
    description: string;
    url: string;
};

@Component({
    selector: 'app-user',
    standalone: true,
    imports: [
        FormsModule,
        NgForOf,
        NgIf,
        NgClass,
        NgxMaskDirective,
        NgxMaskPipe,
        Toast,
        MultiSelect,
        LoadingComponent,
        ModalComponent,
        PrimeTemplate
    ],
    providers: [provideNgxMask()],
    templateUrl: './user.component.html',
    styleUrl: './user.component.scss'
})
export class UserComponent {
    searchTerm = '';
    users: ManagedUser[] = [];
    usersBackup: ManagedUser[] = [];
    roles: RoleOption[] = [];
    quickAddMenuOpen = false;

    readonly quickAddOptions = [1, 3, 5];

    months = [
        { number: '1', name: 'Janeiro' },
        { number: '2', name: 'Fevereiro' },
        { number: '3', name: 'Março' },
        { number: '4', name: 'Abril' },
        { number: '5', name: 'Maio' },
        { number: '6', name: 'Junho' },
        { number: '7', name: 'Julho' },
        { number: '8', name: 'Agosto' },
        { number: '9', name: 'Setembro' },
        { number: '10', name: 'Outubro' },
        { number: '11', name: 'Novembro' },
        { number: '12', name: 'Dezembro' }
    ];

    formSubmitted = false;
    loading = false;
    activationCodeModalOpen = false;
    activationModalTitle = '';
    generatedActivationCode: string | null = null;
    generatedActivationExpiresAt: string | null = null;
    isOperational = false;

    embeddedDocOpen = false;
    embeddedDocTitle = '';
    embeddedDocDescription = '';
    embeddedDocUrl: SafeResourceUrl | null = null;

    readonly docs: EmbeddedDoc[] = [
        {
            key: 'invite-users',
            title: 'Como convidar usuários',
            description: 'Veja o passo a passo para cadastrar acessos, definir perfis e preparar o convite com segurança.',
            url: 'https://lumosip.com.br/como-usar/02-access-management/02-invite-users/'
        },
        {
            key: 'first-access',
            title: 'Como orientar o primeiro acesso',
            description: 'Abra a orientação que explica como o usuário deve ativar a conta, definir a senha e entrar no sistema.',
            url: 'https://lumosip.com.br/como-usar/02-access-management/03-first-access-activation/'
        }
    ];

    readonly statusOptions: { label: string; value: UserActivationStatus }[] = [
        { label: 'Pendente de ativação', value: 'PENDING_ACTIVATION' },
        { label: 'Ativo', value: 'ACTIVE' },
        { label: 'Bloqueado', value: 'BLOCKED' }
    ];

    constructor(
        protected router: Router,
        private readonly userService: UserService,
        protected utils: UtilsService,
        protected authService: AuthService,
        private readonly titleService: Title,
        private readonly sanitizer: DomSanitizer
    ) {
        this.titleService.setTitle('Configurações - Usuários');
        SharedState.setCurrentPath(['Configurações', 'Usuários']);

        this.userService.getUsers().subscribe({
            next: users => {
                this.users = users.map(user => this.normalizeUser(user));
                this.usersBackup = JSON.parse(JSON.stringify(this.users));
                this.applySearch();
            },
            error: err => {
                Utils.handleHttpError(err, this.router);
            }
        });

        this.userService.getRoles().subscribe({
            next: roles => {
                this.roles = roles;
            },
            error: err => {
                Utils.handleHttpError(err, this.router);
            }
        });
    }

    get totalUsersCount(): number {
        return this.usersBackup.length;
    }

    get pendingActivationCount(): number {
        return this.usersBackup.filter(user => user.status === 'PENDING_ACTIVATION').length;
    }

    get activeUsersCount(): number {
        return this.usersBackup.filter(user => user.status === 'ACTIVE').length;
    }

    get blockedUsersCount(): number {
        return this.usersBackup.filter(user => user.status === 'BLOCKED').length;
    }

    get editingUsersCount(): number {
        return this.users.filter(user => user.sel).length;
    }

    get draftUsersCount(): number {
        return this.usersBackup.filter(user => !user.userId).length;
    }

    get hasDraftUsers(): boolean {
        return this.draftUsersCount > 0;
    }

    get canShareActivationCode(): boolean {
        if (Utils.isMobileDevice()) {
            return Utils.isShareAvailable();
        }

        return true;
    }

    trackByUser(_: number, user: ManagedUser): string {
        return user.userId || `${user.username}-${user.email}-${user.cpf}`;
    }

    newUser() {
        this.addUsersBatch(1);
    }

    removeUser() {
        if (!this.hasDraftUsers) {
            return;
        }

        this.users = this.users.filter(user => user.userId !== '');
        this.usersBackup = this.usersBackup.filter(user => user.userId !== '');
        this.applySearch();
        this.quickAddMenuOpen = false;
    }

    toggleUserEdit(user: ManagedUser) {
        if (!user.userId) {
            this.removeUser();
            return;
        }

        user.sel = !user.sel;
    }

    filterUsers(event: Event) {
        this.searchTerm = (event.target as HTMLInputElement).value ?? '';
        this.applySearch();
    }

    toggleQuickAddMenu() {
        this.quickAddMenuOpen = !this.quickAddMenuOpen;
    }

    addUsersBatch(count: number) {
        for (let index = 0; index < count; index += 1) {
            const user = this.createDraftUser();
            this.users.unshift(user);
            this.usersBackup.unshift(JSON.parse(JSON.stringify(user)));
        }

        this.quickAddMenuOpen = false;
        this.applySearch();
    }

    submitUsers(form: NgForm) {
        this.formSubmitted = true;

        if (form.invalid) {
            return;
        }

        if (!this.users.some(user => user.sel)) {
            this.utils.showMessage('Selecione pelo menos um usuário para salvar as alterações.', 'info', 'Lumos™');
            return;
        }

        this.loading = true;
        this.updateUsers();
    }

    confirmGenerateActivation(user: ManagedUser) {
        if (!user.userId) {
            this.utils.showMessage('Salve o usuário antes de gerar o código de ativação.', 'info', 'Lumos™');
            return;
        }

        this.isOperational = this.isOperationalUser(user);
        this.loading = true;

        this.userService.generateActivationCode(user.userId).pipe(
            tap(response => this.openActivationCodeModal('Código de ativação gerado', user.userId, response)),
            catchError(err => {
                this.loading = false;
                this.utils.showMessage(err?.error?.message ?? 'Não foi possível gerar o código de ativação.', 'error', 'Lumos™');
                return throwError(() => err);
            })
        ).subscribe();
    }

    confirmResetActivation(user: ManagedUser) {
        if (!user.userId) {
            this.utils.showMessage('Salve o usuário antes de resetar a ativação.', 'info', 'Lumos™');
            return;
        }

        this.isOperational = this.isOperationalUser(user);
        this.loading = true;

        this.userService.resetActivation(user.userId).pipe(
            tap(response => this.openActivationCodeModal('Código de ativação redefinido', user.userId, response)),
            catchError(err => {
                this.loading = false;
                this.utils.showMessage(err?.error?.message ?? 'Não foi possível redefinir a ativação.', 'error', 'Lumos™');
                return throwError(() => err);
            })
        ).subscribe();
    }

    copyActivationCode() {
        if (!this.generatedActivationCode) {
            return;
        }

        Utils.copyToClipboard(this.generatedActivationCode).then(() => {
            this.utils.showMessage('Código de ativação copiado com sucesso.', 'success', 'Lumos™');
        });
    }

    async shareActivationCode() {
        if (!this.generatedActivationCode) {
            return;
        }

        const expiration = this.formatActivationExpiration(this.generatedActivationExpiresAt);

        try {
            await Utils.shareMessage(
                `Olá! Você foi convidado para acessar o Lumos IP™.\n\nCódigo de ativação: ${this.generatedActivationCode}\nValidade: ${expiration}\n\n${this.isOperational
                    ? 'Use o app Lumos OP, toque em "Primeiro acesso", informe seu CPF, o código acima e crie sua senha.'
                    : 'Acesse https://app.lumosip.com.br/primeiro-acesso, informe seu CPF, o código acima e crie sua senha.'
                }\n\nEste código é pessoal e expira automaticamente.`,
                {
                    title: 'Acesso ao Lumos IP™',
                    subject: 'Acesso ao Lumos IP™'
                }
            );
        } catch (error: any) {
            if (error?.name !== 'AbortError') {
                this.utils.showMessage('Não foi possível compartilhar o código de ativação.', 'error', 'Lumos™');
            }
        }
    }

    openDocumentation(docKey: EmbeddedDoc['key']) {
        const doc = this.docs.find(item => item.key === docKey);
        if (!doc) {
            return;
        }

        this.embeddedDocTitle = doc.title;
        this.embeddedDocDescription = doc.description;
        this.embeddedDocUrl = this.sanitizer.bypassSecurityTrustResourceUrl(doc.url);
        this.embeddedDocOpen = true;
    }

    closeDocumentation() {
        this.embeddedDocOpen = false;
        this.embeddedDocUrl = null;
        this.embeddedDocTitle = '';
        this.embeddedDocDescription = '';
    }

    closeActivationModal() {
        this.activationCodeModalOpen = false;
        this.generatedActivationCode = null;
        this.generatedActivationExpiresAt = null;
    }

    getMonth(monthNumber: string) {
        return this.months.find(month => month.number === monthNumber);
    }

    getRoleLabel(roleName: string): string {
        return this.roles.find(role => role.roleName === roleName)?.label ?? roleName;
    }

    getAllRoles(roles: string[]): string {
        return roles.map(role => this.getRoleLabel(role)).join(', ');
    }

    getUserDisplayName(user: ManagedUser): string {
        const fullName = `${user.name ?? ''} ${user.lastname ?? ''}`.trim();
        return fullName || user.username || 'Novo usuário';
    }

    getUserInitials(user: ManagedUser): string {
        return this.getUserDisplayName(user)
            .split(' ')
            .filter(Boolean)
            .slice(0, 2)
            .map(part => part[0]?.toUpperCase() ?? '')
            .join('');
    }

    getStatusLabel(status: UserActivationStatus): string {
        return this.statusOptions.find(option => option.value === status)?.label ?? status;
    }

    getStatusBadgeClass(status: UserActivationStatus): string {
        if (status === 'ACTIVE') {
            return 'hover:border-emerald-400 bg-emerald-100 text-emerald-700 dark:bg-emerald-950/40 dark:text-emerald-200';
        }

        if (status === 'BLOCKED') {
            return 'hover:border-red-400 bg-red-100 text-red-700 dark:bg-red-950/40 dark:text-red-200';
        }

        return 'hover:border-amber-400 bg-amber-100 text-amber-700 dark:bg-amber-950/40 dark:text-amber-200';
    }

    getActivationSummary(user: ManagedUser): string {
        if (user.status === 'ACTIVE') {
            return 'Acesso liberado';
        }

        if (user.status === 'BLOCKED') {
            return 'Acesso bloqueado';
        }

        if (user.activationExpiresAt) {
            return `Código ativo até ${this.formatActivationExpiration(user.activationExpiresAt)}`;
        }

        return 'Aguardando geração do código';
    }

    formatActivationExpiration(value: string | null): string {
        if (!value) {
            return 'Sem código ativo';
        }

        return new Intl.DateTimeFormat('pt-BR', {
            dateStyle: 'short',
            timeStyle: 'short'
        }).format(new Date(value));
    }

    protected getBadge(roleName: string): string {
        if (roleName === 'ADMIN') {
            return 'Acesso total';
        }

        if (['ANALISTA', 'RESPONSAVEL_TECNICO'].includes(roleName)) {
            return 'Acesso alto';
        }

        if (['ESTOQUISTA', 'ESTOQUISTA_CHEFE'].includes(roleName)) {
            return 'Acesso médio';
        }

        return 'Baixo acesso';
    }

    isOperationalUser(user: ManagedUser): boolean {
        return user.role.includes('MOTORISTA') || user.role.includes('ELETRICISTA');
    }

    private createDraftUser(): ManagedUser {
        return {
            userId: '',
            username: '',
            name: '',
            lastname: '',
            email: '',
            cpf: '',
            year: '',
            month: '',
            day: '',
            role: [],
            status: 'PENDING_ACTIVATION',
            mustChangePassword: false,
            activationExpiresAt: null,
            sel: true,
            show: false
        };
    }

    private applySearch() {
        const value = this.searchTerm.trim().toLowerCase();

        if (!value) {
            this.users = JSON.parse(JSON.stringify(this.usersBackup));
            return;
        }

        this.users = this.usersBackup.filter(user => {
            const roleNames = (user.role ?? []).map(role => this.getRoleLabel(role)).join(' ');

            const searchableFields = [
                user.username,
                user.name,
                user.lastname,
                `${user.name} ${user.lastname}`,
                user.email,
                user.cpf,
                roleNames,
                this.getStatusLabel(user.status)
            ];

            return searchableFields.some(field =>
                (field ?? '').toString().toLowerCase().includes(value)
            );
        }).map(user => JSON.parse(JSON.stringify(user)));
    }

    private updateUsers() {
        this.userService.updateUser(this.users).pipe(
            tap(response => {
                this.utils.showMessage('Usuários atualizados com sucesso.', 'success', 'Lumos™', true);
                this.users = response.map(user => this.normalizeUser(user));
                this.usersBackup = JSON.parse(JSON.stringify(this.users));
                this.applySearch();
                this.loading = false;
            }),
            catchError(err => {
                this.utils.showMessage(err?.error?.message ?? 'Não foi possível atualizar os usuários.', 'error', 'Lumos™');
                this.loading = false;
                return throwError(() => err);
            })
        ).subscribe();
    }

    private normalizeUser(user: UserManagementResponse): ManagedUser {
        const status = typeof user.status === 'boolean'
            ? (user.status ? 'ACTIVE' : 'BLOCKED')
            : user.status;

        return {
            userId: user.userId,
            username: user.username,
            name: user.name,
            lastname: user.lastname,
            email: user.email,
            cpf: user.cpf,
            year: `${user.year ?? ''}`,
            month: `${user.month ?? ''}`,
            day: `${user.day ?? ''}`,
            role: (user.role ?? []).map((role: any) => typeof role === 'string' ? role : role?.roleName).filter(Boolean),
            status,
            mustChangePassword: user.mustChangePassword ?? false,
            activationExpiresAt: user.activationExpiresAt ?? null,
            sel: user.sel ?? false,
            show: user.show ?? false
        };
    }

    private openActivationCodeModal(title: string, userId: string, response: ActivationCodeResponse) {
        this.loading = false;
        this.activationModalTitle = title;
        this.generatedActivationCode = response.activationCode;
        this.generatedActivationExpiresAt = response.expiresAt;
        this.activationCodeModalOpen = true;
        this.users = this.users.map(user => user.userId === userId ? {
            ...user,
            status: 'PENDING_ACTIVATION',
            mustChangePassword: true,
            activationExpiresAt: response.expiresAt
        } : user);
        this.usersBackup = this.usersBackup.map(user => user.userId === userId ? {
            ...user,
            status: 'PENDING_ACTIVATION',
            mustChangePassword: true,
            activationExpiresAt: response.expiresAt
        } : user);
        this.utils.showMessage(response.message, 'success', 'Lumos™');
    }
}
