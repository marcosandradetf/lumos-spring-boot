import { Component, OnInit } from '@angular/core';
import { NgClass, NgForOf, NgIf } from '@angular/common';
import { ActivatedRoute, NavigationEnd, Router, RouterOutlet } from '@angular/router';
import { forkJoin } from 'rxjs';
import { filter } from 'rxjs/operators';
import { DomSanitizer, SafeResourceUrl, Title } from '@angular/platform-browser';
import { ButtonDirective } from 'primeng/button';
import { ProgressBar } from 'primeng/progressbar';
import { Tag } from 'primeng/tag';
import { Skeleton } from 'primeng/skeleton';
import { UserService } from '../user/user-service.service';
import { UtilsService } from '../../core/service/utils.service';
import { AuthService } from '../../core/auth/auth.service';
import { TeamService } from '../team/team-service.service';
import { StockService } from '../../stock/services/stock.service';
import { MaterialService } from '../../stock/services/material.service';
import { SharedState } from '../../core/service/shared-state';
import { Utils } from '../../core/service/utils';
import { ContractService } from '../../contract/services/contract.service';

@Component({
    selector: 'app-settings',
    standalone: true,
    imports: [
        NgIf,
        NgForOf,
        NgClass,
        ButtonDirective,
        ProgressBar,
        Tag,
        Skeleton,
        RouterOutlet
    ],
    templateUrl: './settings.component.html',
    styleUrl: './settings.component.scss'
})
export class SettingsComponent implements OnInit {
    loading = true;

    referenceContractItems = 0;
    totalContracts = 0;
    totalUsers = 0;
    operationalUsers = 0;
    stockistUsers = 0;
    adminUsers = 0;
    teamsCount = 0;
    stockistsCount = 0;
    depositsCount = 0;
    trucksCount = 0;
    materialsCount = 0;

    onboardingSteps: OnboardingStep[] = [];
    activeStepIndex = 0;
    fieldActivationDone = localStorage.getItem('isFinishedTeamInstruction') === 'true';
    embeddedViewActive = false;
    embeddedExternalUrl: SafeResourceUrl | null = null;
    embeddedExternalUrlRaw: string | null = null;

    private readonly embeddedRouteMap: Record<string, string> = {
        '/configuracoes/usuarios': 'usuarios',
        '/configuracoes/equipes': 'equipes',
        '/configuracoes/estoquistas': 'estoquistas',
        '/estoque/almoxarifados': 'almoxarifados',
        '/estoque/caminhoes': 'caminhoes',
        '/estoque/cadastrar-material': 'cadastrar-material',
        '/contratos/itens-contratuais/cadastro': 'itens-contratuais',
        '/contratos/criar': 'criar-contrato',
    };

    constructor(
        protected router: Router,
        private route: ActivatedRoute,
        private sanitizer: DomSanitizer,
        private userService: UserService,
        protected utils: UtilsService,
        protected authService: AuthService,
        private titleService: Title,
        private teamService: TeamService,
        private stockService: StockService,
        private materialService: MaterialService,
        private contractService: ContractService
    ) {
        this.titleService.setTitle('Configurações - Onboarding');
        SharedState.setCurrentPath(['Configurações', 'Onboarding']);
    }

    ngOnInit() {
        this.loadOnboarding();
        this.syncEmbeddedViewState();

        this.router.events
            .pipe(filter(event => event instanceof NavigationEnd))
            .subscribe(() => this.syncEmbeddedViewState());
    }

    private loadOnboarding() {
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
            next: ({ referenceContractItems, contracts, users, teams, stockists, deposits, materials }) => {
                this.referenceContractItems = referenceContractItems.length;
                this.totalContracts = contracts.length;

                this.totalUsers = users.length;
                this.adminUsers = users.filter(user => Array.isArray(user.role) && user.role.includes('ADMIN')).length;
                this.operationalUsers = users.filter(user => {
                    const roles = user.role.map(r => r.roleName);
                    return roles.includes('ELETRICISTA') || roles.includes('MOTORISTA')
                }).length;

                this.stockistUsers = users.filter(user => {
                    const roles = user.role.map(r => r.roleName);
                    return roles.includes('ESTOQUISTA') || roles.includes('ESTOQUISTA_CHEFE')
                }
                ).length;

                this.teamsCount = teams.length;
                this.stockistsCount = stockists.length;
                this.depositsCount = deposits.filter(deposit => !deposit.isTruck).length;
                this.trucksCount = deposits.filter(deposit => deposit.isTruck).length;
                this.materialsCount = materials.length;

                this.onboardingSteps = this.buildSteps();
                this.activeStepIndex = this.getInitialStepIndex();
                this.loading = false;
            },
            error: (err) => {
                Utils.handleHttpError(err, this.router);
                this.loading = false;
            }
        });
    }

    private buildSteps(): OnboardingStep[] {
        return [
            {
                id: 'users',
                title: 'Estruture os acessos',
                description: 'Cadastre perfis administrativos e operacionais para destravar os vínculos das próximas etapas.',
                helper: 'Sua operação precisa de estoquistas, eletricistas e motoristas previamente cadastrados.',
                icon: 'pi pi-users',
                route: '/configuracoes/usuarios',
                ctaLabel: this.totalUsers === 0 ? 'Cadastrar usuários' : 'Revisar usuários',
                ctaIcon: 'pi pi-user-plus',
                done: this.totalUsers > 0 && this.operationalUsers > 0 && this.stockistUsers > 0,
                metric: `Usuários: ${this.totalUsers}`,
                blockers: [
                    this.totalUsers === 0 ? 'Nenhum usuário cadastrado.' : '',
                    this.operationalUsers === 0 ? 'Cadastrar eletricistas e motoristas.' : '',
                    this.stockistUsers === 0 ? 'Cadastrar estoquistas.' : ''
                ].filter(Boolean),
                disabled: false
            },
            {
                id: 'teams',
                title: 'Monte a equipe operacional',
                description: 'Crie equipes com colaboradores, região de atuação e vínculo de caminhão para liberar a operação em campo.',
                helper: 'Essa etapa destrava ordens de serviço e também reflete nos caminhões do estoque.',
                icon: 'pi pi-user-edit',
                route: '/configuracoes/equipes',
                ctaLabel: this.teamsCount === 0 ? 'Cadastrar equipes' : 'Gerenciar equipes',
                ctaIcon: 'pi pi-users',
                done: this.teamsCount > 0,
                metric: `Equipes: ${this.teamsCount}`,
                blockers: [
                    this.operationalUsers === 0 ? 'Cadastre usuários operacionais antes de montar a equipe.' : '',
                    this.teamsCount === 0 ? 'Nenhuma equipe operacional cadastrada.' : ''
                ].filter(Boolean),
                disabled: this.totalUsers === 0 || this.operationalUsers === 0
            },

            {
                id: 'field-activation',
                title: 'Coloque a equipe em campo',
                description: 'Garanta que a equipe operacional tenha acesso ao aplicativo e esteja pronta para iniciar as atividades em campo.',
                helper: this.teamsCount === 0
                    ? 'Crie uma equipe operacional antes de iniciar a operação.'
                    : 'Peça para a equipe baixar o app, fazer login e seguir as instruções iniciais.',
                icon: 'pi pi-mobile',
                route: 'https://lumosip.com.br/como-usar/07-operation/01-android-app-admin/', // ou onde fizer sentido
                externalUrl: true,
                ctaLabel: 'Ver instruções',
                ctaIcon: 'pi pi-external-link',
                done: this.fieldActivationDone,
                metric: this.fieldActivationDone
                    ? 'Equipe pronta para operação'
                    : 'Ativação pendente',
                blockers: [
                    this.teamsCount === 0
                        ? 'Você precisa criar uma equipe operacional primeiro.'
                        : '',
                    !this.fieldActivationDone
                        ? 'Oriente a equipe a baixar o aplicativo e acessar o sistema.'
                        : ''
                ].filter(Boolean),
                disabled: false
            },


            {
                id: 'stockists',
                title: 'Defina os estoquistas',
                description: 'Associe responsáveis pelo estoque para liberar o gerenciamento de reservas e expedições.',
                helper: 'Essa etapa é obrigatória para criação e acompanhamento de ordens com movimentação de material.',
                icon: 'pi pi-briefcase',
                route: '/configuracoes/estoquistas',
                ctaLabel: this.stockistsCount === 0 ? 'Cadastrar estoquistas' : 'Gerenciar estoquistas',
                ctaIcon: 'pi pi-briefcase',
                done: this.stockistsCount > 0,
                metric: `Estoquistas: ${this.stockistsCount}`,
                blockers: [
                    this.stockistsCount === 0 ? 'Nenhum estoquista configurado.' : ''
                ].filter(Boolean),
                disabled: false
            },
            {
                id: 'deposits',
                title: 'Prepare a base logística',
                description: 'Configure almoxarifados e caminhões para garantir origem, destino e rastreabilidade do material.',
                helper: 'Movimentação de estoque depende de pelo menos um almoxarifado e um caminhão.',
                icon: 'pi pi-truck',
                route: this.depositsCount === 0 ? '/estoque/almoxarifados' : '/estoque/caminhoes',
                ctaLabel: this.depositsCount === 0 ? 'Cadastrar almoxarifados' : this.trucksCount === 0 ? 'Cadastrar caminhões' : 'Revisar logística',
                ctaIcon: this.depositsCount === 0 ? 'pi pi-home' : 'pi pi-truck',
                done: this.depositsCount > 0 && this.trucksCount > 0,
                metric: `Almoxarifados: ${this.depositsCount} • Caminhões: ${this.trucksCount}`,
                blockers: [
                    this.depositsCount === 0 ? 'Ainda não existe almoxarifado cadastrado.' : '',
                    this.trucksCount === 0 ? 'Ainda não existe caminhão operacional vinculado.' : ''
                ].filter(Boolean),
                disabled: false
            },
            {
                id: 'materials',
                title: 'Abasteça o catálogo',
                description: 'Cadastre materiais para que estoque, contratos e execuções consigam operar sem bloqueios.',
                helper: 'Sem catálogo não há movimentação, separação e apontamento de itens.',
                icon: 'pi pi-box',
                route: '/estoque/cadastrar-material',
                ctaLabel: this.materialsCount === 0 ? 'Cadastrar materiais' : 'Ver catálogo',
                ctaIcon: 'pi pi-box',
                done: this.materialsCount > 0,
                metric: `Materiais: ${this.materialsCount}`,
                blockers: [
                    this.materialsCount === 0 ? 'Catálogo de materiais ainda vazio.' : ''
                ].filter(Boolean),
                disabled: false
            },
            {
                id: 'reference-items',
                title: 'Cadastre os itens contratuais',
                description: 'Crie os itens contratuais que serão usados como base para vincular contratos nas próximas etapas.',
                helper: 'Sem itens contratuais cadastrados, não é possível avançar na configuração de contratos.',
                icon: 'pi pi-file-edit',
                route: '/contratos/itens-contratuais/cadastro',
                ctaLabel: this.referenceContractItems === 0
                    ? 'Cadastrar itens contratuais'
                    : 'Gerenciar itens',
                ctaIcon: this.referenceContractItems === 0
                    ? 'pi pi-plus'
                    : 'pi pi-pencil',
                done: this.referenceContractItems > 0,
                metric: `Itens contratuais: ${this.referenceContractItems}`,
                blockers: [
                    this.referenceContractItems === 0
                        ? 'Você ainda não cadastrou nenhum item contratual.'
                        : ''
                ].filter(Boolean),
                disabled: false
            },

            {
                id: 'contracts',
                title: 'Cadastre os contratos',
                description: 'Crie contratos para organizar e vincular seus itens contratuais e iniciar a operação.',
                helper: this.referenceContractItems === 0
                    ? 'Antes de cadastrar contratos, você precisa criar pelo menos um item contratual.'
                    : 'Com os itens contratuais cadastrados, você já pode criar seus contratos.',

                icon: 'pi pi-briefcase',
                route: '/contratos/criar',
                ctaLabel: this.totalContracts === 0
                    ? 'Cadastrar contratos'
                    : 'Gerenciar contratos',
                ctaIcon: this.totalContracts === 0
                    ? 'pi pi-plus'
                    : 'pi pi-pencil',
                done: this.totalContracts > 0,
                metric: `Contratos: ${this.totalContracts}`,
                blockers: [
                    this.referenceContractItems === 0
                        ? 'Cadastre pelo menos um item contratual antes de criar contratos.'
                        : ''
                ].filter(Boolean),
                disabled: false
            },

        ];
    }

    private getInitialStepIndex(): number {
        const firstPendingIndex = this.onboardingSteps.findIndex(step => !step.done);
        return firstPendingIndex >= 0 ? firstPendingIndex : 0;
    }

    get completedSteps(): number {
        return this.onboardingSteps.filter(step => step.done).length;
    }

    get totalSteps(): number {
        return this.onboardingSteps.length;
    }

    get completionPercent(): number {
        if (this.totalSteps === 0) {
            return 0;
        }

        return Math.round((this.completedSteps / this.totalSteps) * 100);
    }

    get pendingSteps(): OnboardingStep[] {
        return this.onboardingSteps.filter(step => !step.done);
    }

    get activeStep(): OnboardingStep | undefined {
        return this.onboardingSteps[this.activeStepIndex];
    }

    get isComplete(): boolean {
        return this.pendingSteps.length === 0;
    }

    get hasPreviousStep(): boolean {
        return this.activeStepIndex > 0;
    }

    get hasNextStep(): boolean {
        return this.activeStepIndex < this.totalSteps - 1;
    }

    get nextPendingStep(): OnboardingStep | undefined {
        return this.pendingSteps[0];
    }

    get heroTitle(): string {
        return this.isComplete
            ? 'Tudo pronto para começar 🚀'
            : 'Configure o sistema e comece a usar';
    }

    get heroSubtitle(): string {
        return this.isComplete
            ? 'Tudo pronto! Agora é só colocar sua equipe em campo e começar a usar o sistema. Dica: gere sua primeira ordem de serviço para iniciar a operação.'
            : 'Vamos finalizar sua configuração inicial. Comece pelo próximo passo.';
    }

    get progressLabel(): string {
        return `${this.completedSteps} de ${this.totalSteps} etapas concluídas`;
    }

    get activeStepNumber(): number {
        return this.activeStepIndex + 1;
    }

    get activeStepActionLabel(): string {
        if (this.isComplete) {
            return 'Ir para o dashboard';
        }

        return this.activeStep?.ctaLabel ?? 'Continuar';
    }

    get cursorClass(): string {
        if (this.isComplete || !this.activeStep?.disabled) {
            return 'cursor-pointer';
        }

        return 'cursor-not-allowed';
    }

    get isDisabled(): boolean {
        if (this.isComplete) {
            return false;
        }

        return this.activeStep?.disabled ?? false;
    }

    get descriptionLabel(): string {
        if (this.isComplete || !this.activeStep?.disabled) {
            return '';
        }

        return this.activeStep?.blockers[0];
    }

    get activeStepActionRoute(): string {
        if (this.isComplete) {
            return '/dashboard';
        }

        return this.activeStep?.route ?? '/configuracoes/onboarding';
    }

    get isExternalUrl(): boolean {
        if (this.isComplete) {
            return false;
        }

        return this.activeStep?.externalUrl ?? false;
    }

    set isFinishedTeamInstruction(value: string) {
        localStorage.setItem('isFinishedTeamInstruction', value);
    }

    selectStep(index: number) {
        this.activeStepIndex = index;
    }

    goToPreviousStep() {
        if (this.hasPreviousStep) {
            this.activeStepIndex -= 1;
        }
    }

    goToNextStep() {
        if (this.hasNextStep) {
            this.activeStepIndex += 1;
        }
    }

    navigate(path: string, externalUrl: boolean) {
        if (externalUrl) {
            this.embeddedExternalUrlRaw = path;
            this.embeddedExternalUrl = this.sanitizer.bypassSecurityTrustResourceUrl(path);
            this.syncEmbeddedViewState();
            void this.router.navigate(['/configuracoes/onboarding']);
            this.isFinishedTeamInstruction = "true";
            this.fieldActivationDone = true;
            this.buildSteps();
            return;
        }

        this.embeddedExternalUrl = null;
        this.embeddedExternalUrlRaw = null;

        const embeddedChildPath = this.embeddedRouteMap[path];

        if (embeddedChildPath) {
            void this.router.navigate([embeddedChildPath], { relativeTo: this.route });
        } else {
            void this.router.navigate([path]);
        }
    }

    closeEmbeddedView() {
        this.embeddedExternalUrl = null;
        this.embeddedExternalUrlRaw = null;
        this.syncEmbeddedViewState();
        void this.router.navigate(['/configuracoes/onboarding']);
    }

    private syncEmbeddedViewState() {
        this.embeddedViewActive = !!this.route.firstChild || !!this.embeddedExternalUrl;
    }
}

interface OnboardingStep {
    id: string;
    title: string;
    description: string;
    helper: string;
    icon: string;
    route: string;
    ctaLabel: string;
    ctaIcon: string;
    done: boolean;
    metric: string;
    blockers: string[];
    externalUrl?: boolean;
    disabled: boolean;
}
