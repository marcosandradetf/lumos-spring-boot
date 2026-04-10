import {NgClass, NgForOf, NgIf} from '@angular/common';
import {Component} from '@angular/core';
import {FormsModule, NgForm} from '@angular/forms';
import {DomSanitizer, SafeResourceUrl, Title} from '@angular/platform-browser';
import {Router} from '@angular/router';
import {catchError, finalize, forkJoin, map, of, switchMap, tap, throwError} from 'rxjs';
import {Toast} from 'primeng/toast';
import {Select} from 'primeng/select';
import {Tag} from 'primeng/tag';
import {StockService} from '../services/stock.service';
import {Deposit} from '../dto/almoxarifado.model';
import {
    CreateStockistRequest,
    StockistManagement,
    UpdateStockistRequest
} from '../dto/stockist.model';
import {UserService} from '../../manage/user/user-service.service';
import {SharedState} from '../../core/service/shared-state';
import {Utils} from '../../core/service/utils';
import {UtilsService} from '../../core/service/utils.service';
import {AuthService} from '../../core/auth/auth.service';
import {LoadingComponent} from '../../shared/components/loading/loading.component';
import {ModalComponent} from '../../shared/components/modal/modal.component';
import {ButtonComponent} from '../../shared/components/button/button.component';
import { GuideStateComponent } from '../../guide-state/guide-state.component';
import { LoadingOverlayComponent } from '../../shared/components/loading-overlay/loading-overlay.component';

interface UserOption {
    userId: string;
    name: string;
    email: string;
    username: string;
    role: string[];
    status: boolean;
}

interface StockistFormValue {
    stockistId: number | null;
    depositIdDeposit: number | null;
    userIdUser: string | null;
    notificationCode: string;
}

@Component({
    selector: 'app-stockists',
    standalone: true,
    imports: [
        FormsModule,
        NgIf,
        NgForOf,
        NgClass,
        Toast,
        Select,
        Tag,
        LoadingComponent,
        ModalComponent,
        ButtonComponent,
        GuideStateComponent,
        LoadingOverlayComponent,
        LoadingComponent
    ],
    templateUrl: './stockists.component.html',
    styleUrl: './stockists.component.scss'
})
export class StockistsComponent {
    formOpen = false;
    formSubmitted = false;
    loading = false;
    isInitialLoading = true;
    saving = false;
    showConfirmation = false;
    isFallbackData = false;
    embeddedDocOpen = false;
    embeddedDocTitle = '';
    embeddedDocDescription = '';
    embeddedDocUrl: SafeResourceUrl | null = null;

    deposits: Deposit[] = [];
    availableUsers: UserOption[] = [];
    stockists: StockistManagement[] = [];
    selectedStockist: StockistManagement | null = null;
    readonly documentationUrl = 'https://lumosip.com.br/como-usar/04-stock/01-stockist-management/';

    stockist: StockistFormValue = this.createEmptyForm();

    constructor(
        private stockService: StockService,
        private userService: UserService,
        private authService: AuthService,
        private utils: UtilsService,
        private title: Title,
        protected router: Router,
        private sanitizer: DomSanitizer
    ) {
        this.title.setTitle('Configurações - Estoquistas');
        SharedState.setCurrentPath(['Configurações', 'Estoquistas']);
        this.loadPageData();
    }

    openDocumentation() {
        this.embeddedDocTitle = 'Cadastro de Estoquistas';
        this.embeddedDocDescription = 'Guia para cadastrar responsáveis por almoxarifados e estruturar o controle de estoque.';
        this.embeddedDocUrl = this.sanitizer.bypassSecurityTrustResourceUrl(this.documentationUrl);
        this.embeddedDocOpen = true;
    }

    closeDocumentation() {
        this.embeddedDocOpen = false;
        this.embeddedDocTitle = '';
        this.embeddedDocDescription = '';
        this.embeddedDocUrl = null;
    }

    setOpen(force?: boolean) {
        this.formOpen = force ?? !this.formOpen;

        if (!this.formOpen) {
            this.resetForm();
        }
    }

    onSubmit(form: NgForm) {
        this.formSubmitted = true;

        if (form.invalid || !this.stockist.depositIdDeposit || !this.stockist.userIdUser) {
            return;
        }

        if (this.hasDuplicateAssociation()) {
            this.utils.showMessage(
                'Esse usuário já está vinculado ao almoxarifado selecionado.',
                'warn',
                'Vínculo duplicado'
            );
            return;
        }

        this.saving = true;

        const payload = this.buildPayload();
        const request$ = this.stockist.stockistId
            ? this.stockService.updateStockist(payload as UpdateStockistRequest)
            : this.stockService.insertStockist(payload as CreateStockistRequest);

        request$
            .pipe(
                switchMap(() => this.loadStockists()),
                tap(stockists => {
                    this.stockists = stockists;
                    this.utils.showMessage(
                        this.stockist.stockistId ? 'Estoquista atualizado com sucesso.' : 'Estoquista cadastrado com sucesso.',
                        'success',
                        'Estoque'
                    );
                    this.setOpen(false);
                }),
                catchError(err => {
                    this.utils.showMessage(err?.error?.message ?? 'Não foi possível salvar o estoquista.', 'error', 'Atenção');
                    return throwError(() => err);
                }),
                finalize(() => this.saving = false)
            )
            .subscribe();
    }

    editStockist(stockist: StockistManagement) {
        this.stockist = {
            stockistId: stockist.stockistId,
            depositIdDeposit: stockist.depositIdDeposit,
            userIdUser: stockist.userIdUser,
            notificationCode: stockist.notificationCode || this.generateUuid()
        };
        this.formSubmitted = false;
        this.formOpen = true;
    }

    confirmDelete(stockist: StockistManagement) {
        if (!stockist.stockistId) {
            this.utils.showMessage(
                'Esse registro veio do endpoint legado de consulta e não possui identificador para exclusão direta.',
                'info',
                'Ação indisponível'
            );
            return;
        }

        this.selectedStockist = stockist;
        this.showConfirmation = true;
    }

    deleteStockist() {
        if (!this.selectedStockist?.stockistId) {
            this.showConfirmation = false;
            return;
        }

        this.saving = true;
        this.stockService.deleteStockist(this.selectedStockist.stockistId)
            .pipe(
                switchMap(() => this.loadStockists()),
                tap(stockists => {
                    this.stockists = stockists;
                    this.showConfirmation = false;
                    this.selectedStockist = null;
                    this.utils.showMessage('Estoquista removido com sucesso.', 'success', 'Estoque');
                }),
                catchError(err => {
                    this.showConfirmation = false;
                    this.utils.showMessage(err?.error?.message ?? 'Não foi possível remover o estoquista.', 'error', 'Atenção');
                    return throwError(() => err);
                }),
                finalize(() => this.saving = false)
            )
            .subscribe();
    }

    getUserLabel(userId: string | null): string {
        if (!userId) {
            return '';
        }

        const user = this.availableUsers.find(item => item.userId === userId);
        return user ? `${user.name} (${user.email})` : '';
    }

    getDepositLabel(depositId: number | null): string {
        if (!depositId) {
            return '';
        }

        return this.deposits.find(item => item.idDeposit === depositId)?.depositName ?? '';
    }

    getSeverity(roles: string[]): 'success' | 'info' | 'contrast' | 'warn' {
        if (roles.includes('ADMIN')) {
            return 'contrast';
        }

        if (roles.includes('ESTOQUISTA_CHEFE')) {
            return 'success';
        }

        if (roles.includes('ESTOQUISTA')) {
            return 'info';
        }

        return 'warn';
    }

    private loadPageData() {
        this.isInitialLoading = true;

        forkJoin({
            deposits: this.stockService.getDeposits().pipe(
                map(deposits => deposits.filter(deposit => !deposit.isTruck))
            ),
            users: this.userService.getUsers().pipe(
                map(users => users
                    .filter(user => user.status === true || user.status === 'ACTIVE')
                    .map(user => ({
                        userId: user.userId,
                        name: `${user.name} ${user.lastname}`.trim(),
                        email: user.email,
                        username: user.username,
                        role: user.role ?? [],
                        status: user.status
                    }))
                    .sort((a, b) => a.name.localeCompare(b.name, 'pt-BR'))
                )
            ),
            stockists: this.loadStockists()
        }).subscribe({
            next: ({deposits, users, stockists}) => {
                this.deposits = deposits;
                this.availableUsers = users;
                this.stockists = stockists;
                this.isInitialLoading = false;
            },
            error: err => {
                this.isInitialLoading = false;
                Utils.handleHttpError(err, this.router);
            }
        });
    }

    private loadStockists() {
        return this.stockService.getStockists().pipe(
            map(stockists => {
                this.isFallbackData = false;
                return stockists.map(stockist => ({
                        stockistId: stockist.stockistId,
                        depositIdDeposit: stockist.depositId,
                        userIdUser: stockist.userId,
                        notificationCode: this.generateUuid(),
                        userName: stockist.name,
                        userEmail: '',
                        userRoles: [],
                        depositName: stockist.depositName,
                        depositAddress: stockist.depositAddress,
                        depositPhone: stockist.depositPhone,
                        depositRegion: stockist.region
                    } satisfies StockistManagement));
            }),
            catchError(() => this.stockService.getStockists().pipe(
                map(stockists => {
                    this.isFallbackData = true;
                    return stockists.map(stockist => ({
                        stockistId: stockist.stockistId,
                        depositIdDeposit: stockist.depositId,
                        userIdUser: stockist.userId,
                        notificationCode: this.generateUuid(),
                        userName: stockist.name,
                        userEmail: '',
                        userRoles: [],
                        depositName: stockist.depositName,
                        depositAddress: stockist.depositAddress,
                        depositPhone: stockist.depositPhone,
                        depositRegion: stockist.region
                    } satisfies StockistManagement));
                })
            ))
        );
    }

    private createEmptyForm(): StockistFormValue {
        return {
            stockistId: null,
            depositIdDeposit: null,
            userIdUser: null,
            notificationCode: this.generateUuid(),
        };
    }

    private resetForm() {
        this.formSubmitted = false;
        this.stockist = this.createEmptyForm();
    }

    private buildPayload(): CreateStockistRequest | UpdateStockistRequest {
        return {
            depositIdDeposit: this.stockist.depositIdDeposit!,
            userIdUser: this.stockist.userIdUser!,
        };
    }

    private hasDuplicateAssociation(): boolean {
        return this.stockists.some(existing =>
            existing.userIdUser === this.stockist.userIdUser &&
            existing.depositIdDeposit === this.stockist.depositIdDeposit &&
            existing.stockistId !== this.stockist.stockistId
        );
    }

    private generateUuid(): string {
        if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
            return crypto.randomUUID();
        }

        return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, character => {
            const random = Math.floor(Math.random() * 16);
            const value = character === 'x' ? random : (random & 0x3) | 0x8;
            return value.toString(16);
        });
    }
}
