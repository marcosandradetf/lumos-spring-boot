import {Component, OnInit, ViewChild} from '@angular/core';
import {catchError, tap, throwError} from 'rxjs';
import {NgClass, NgForOf, NgIf} from '@angular/common';
import {FormsModule, NgForm, ReactiveFormsModule} from '@angular/forms';
import {Title} from '@angular/platform-browser';
import {TableComponent} from '../../shared/components/table/table.component';
import {ButtonComponent} from '../../shared/components/button/button.component';
import {ModalComponent} from '../../shared/components/modal/modal.component';
import {Deposit} from '../dto/almoxarifado.model';
import {MaterialStockResponse} from '../dto/material-response.dto';
import {StockMovementDTO} from '../dto/stock-movement.dto';
import {MaterialService} from '../services/material.service';
import {StockService} from '../services/stock.service';
import {UtilsService} from '../../core/service/utils.service';
import {Steps} from 'primeng/steps';
import {ConfirmationService, MenuItem} from 'primeng/api';
import {TableModule} from 'primeng/table';
import {Skeleton} from 'primeng/skeleton';
import {Toast} from 'primeng/toast';
import {DropdownModule} from 'primeng/dropdown';
import {PrimeBreadcrumbComponent} from '../../shared/components/prime-breadcrumb/prime-breadcrumb.component';
import {ActivatedRoute, Router} from '@angular/router';
import {InputText} from 'primeng/inputtext';
import {Button} from 'primeng/button';
import {ToggleButton} from 'primeng/togglebutton';
import {IconField} from 'primeng/iconfield';
import {InputIcon} from 'primeng/inputicon';
import {Toolbar} from 'primeng/toolbar';
import {QRCodeModule} from 'angularx-qrcode';
import {LoadingOverlayComponent} from '../../shared/components/loading-overlay/loading-overlay.component';
import {AuthService} from '../../core/auth/auth.service';
import {Message} from 'primeng/message';
import {Divider} from 'primeng/divider';
import {Tooltip} from 'primeng/tooltip';
import {Dialog} from 'primeng/dialog';
import {ZXingScannerModule} from '@zxing/ngx-scanner';
import {BarcodeFormat} from '@zxing/library';
import {StyleClass} from 'primeng/styleclass';
import {ConfirmPopup, ConfirmPopupModule} from 'primeng/confirmpopup';
import {SharedState} from '../../core/service/shared-state';

@Component({
    selector: 'app-stock-movement',
    standalone: true,
    imports: [
        TableComponent,
        ButtonComponent,
        ModalComponent,
        NgForOf,
        FormsModule,
        NgIf,
        NgClass,
        Steps,
        TableModule,
        Skeleton,
        Toast,
        DropdownModule,
        PrimeBreadcrumbComponent,
        InputText,
        ToggleButton,
        Button,
        IconField,
        InputIcon,
        Toolbar,
        QRCodeModule,
        LoadingOverlayComponent,
        ReactiveFormsModule,
        Message,
        Divider,
        Tooltip,
        Dialog,
        ZXingScannerModule,
        ConfirmPopupModule,
    ],
    providers:[ConfirmationService],
    templateUrl: './stock-movement.component.html',
    styleUrl: './stock-movement.component.scss'
})
export class StockMovementComponent implements OnInit {
    totalRecords: number = 0;
    currentPage: number = 0;
    lastPage: number = 0;
    rows: number = 15;
    loading: boolean = true;
    loadingOverlay: boolean = false;
    deposits: Deposit[] = [];
    materials: MaterialStockResponse[] = [];

    suppliers: any = [];
    sendSuppliers: any[] = [];
    sendMovement: StockMovementDTO[] = [];
    openMovementModal: boolean = false;
    openConfirmationModal: boolean = false;
    openSupplierModal: boolean = false;
    formSubmitted: boolean = false;
    isMobile = false;
    items: MenuItem[] | undefined = undefined;

    private validate(): boolean {
        for (let item of this.sendMovement) {
            if (item.inputQuantity.length === 0) {
                this.utils.showMessage("A quantidade não pode ser igual a 0 ou estar inválida.", 'warn', 'Atenção');
                return false; // Aqui retorna e sai da função
            } else if (item.requestUnit !== item.buyUnit && item.quantityPackage.length === 0) {
                this.utils.showMessage("A quantidade por embalagem não pode ser igual a 0 ou estar inválida no item " + item.description, 'warn', 'Atenção');
                return false; // Aqui também retorna e sai da função
            }
        }
        return true; // Se não encontrou erro, retorna true
    }


    constructor(
        protected materialService: MaterialService,
        private stockService: StockService,
        protected utils: UtilsService,
        private titleService: Title,
        protected router: Router,
        protected route: ActivatedRoute,
        private authService: AuthService,
        private confirmationService: ConfirmationService
    ) {
    }

    ngOnInit() {
        this.isMobile = window.innerWidth <= 1024;
        this.titleService.setTitle("Movimentar Estoque");
        SharedState.setCurrentPath(['Estoque', 'Movimentar Estoque']);
        this.items = [
            {
                label: this.isMobile ? 'Escanear itens' : 'Selecionar itens',
                routerLink: '/estoque/movimentar-estoque'
            },
            {
                label: 'Pendente de Aprovação',
                routerLink: '/estoque/movimentar-estoque-pendente'
            },
            {
                label: 'Aprovado',
                routerLink: '/estoque/movimentar-estoque-aprovado'
            },
        ];

        const depositId = this.route.snapshot.queryParamMap.get('almoxarifado');
        this.stockService.getDeposits().subscribe({
            next: (d) => {
                this.deposits = d;
                if (depositId) {
                    this.currentDeposit = this.deposits.find(d => d.idDeposit === Number(depositId));
                    this.loadMaterials();
                    this.scannerEnabled = this.isMobile;
                }
            },
            error: (err) => {
                this.utils.showMessage(err.error.message ?? err.error.error ?? err.error, "error");
            }
        });
    }

    loadMaterials() {
        if (!this.currentDeposit) return;

        this.loading = true;
        if (this.searchValue !== null) {
            this.materialService.getBySearch(this.materialService.currentPage, this.rows, this.currentDeposit?.idDeposit ?? 0, this.searchValue)
                .subscribe(response => {
                    this.materials = response.data;  // Dados dos materiais
                    this.totalRecords = response.totalRecords;  // Total de registros no banco
                    this.loading = false;
                });
        } else {
            this.materialService.getMaterials(this.currentPage, this.rows, this.currentDeposit?.idDeposit ?? 0)
                .subscribe(response => {
                    this.materials = response.data;  // Dados dos materiais
                    this.totalRecords = response.totalRecords;  // Total de registros no banco
                    this.loading = false;
                });
        }
    }

    currentDeposit: Deposit | undefined;

    handleConfirmMovement() {
        if (this.validate()) {
            this.openConfirmationModal = true;
        }
    }

    addMovement(item: MaterialStockResponse, toggle: boolean) {
        const index = this.sendMovement.findIndex(s => s.materialStockId === item.materialStockId);

        if (index === -1) {
            const newMovement: StockMovementDTO = {
                materialStockId: item.materialStockId,
                materialName: item.materialName,
                barcode: item.barcode,
                description: '',
                buyUnit: item.buyUnit,
                requestUnit: item.requestUnit,
                inputQuantity: '',
                priceTotal: '',
                quantityPackage: '',
                totalQuantity: '',
                hidden: false,
                invalid: false,
            };
            this.sendMovement.push(newMovement);
        } else if (toggle) {
            this.sendMovement = this.sendMovement.filter(movement => movement.materialStockId !== item.materialStockId);
        }
    }

    successMessage = false;
    submitDataMovement(): void {
        this.stockService.stockMovement(this.sendMovement).pipe(
            tap(() => {
                this.formSubmitted = false;
                this.closeConfirmationModal();
                this.openMovementModal = false;
                this.sendMovement = [];
                this.showFinishOption = false;
                this.successMessage = true;
            }),
            catchError(err => {
                this.utils.showMessage(err.error.message, 'error', 'Erro ao salvar movimentação');
                this.formSubmitted = false;
                this.closeConfirmationModal();
                return throwError(() => err);
            })
        ).subscribe();
    }

    handleOpenSupplierModal() {
        this.openSupplierModal = true;
    }

    closeConfirmationModal() {
        this.openConfirmationModal = false;
    }


    closeSupplierModal() {
        this.openSupplierModal = false;
    }


    submitDataSupplier(form: any) {
        this.formSubmitted = true;

        if (form.invalid) {
            return;
        }

        this.stockService.createSuppliers(this.sendSuppliers).pipe(
            tap(response => {
                this.utils.showMessage('Fornecedor Criado com Sucesso!', 'success', 'Sucesso');
                this.suppliers = response;
                this.formSubmitted = false;
                this.sendSuppliers = [];
            }),
            catchError(err => {
                this.utils.showMessage(err.error.message, 'error', 'Erro ao salvar fornecedor');
                this.formSubmitted = false;
                return throwError(() => err);
            })
        ).subscribe();

    }

    addSupplier() {
        const newSupplier = {
            name: '',
            cnpj: '',
            contact: '',
            address: '',
            phone: '',
            email: ''
        };
        this.sendSuppliers.push(newSupplier);
    }

    removeRow(index: number) {
        this.sendSuppliers.splice(index, 1); // Remove o fornecedor pelo índice
    }


    // submitFormMovement(form: NgForm) {
    //     this.formSubmitted = true;
    //
    //     if (form.invalid) {
    //         console.log("invalid form");
    //         return;
    //     }
    //
    //     this.handleConfirmMovement();
    // }
    @ViewChild('confirmPopup') confirmPopup!: ConfirmPopup;
    submitFormMovement(form: NgForm, event: Event) {
        this.formSubmitted = true;

        if (form.invalid) {
            console.log('invalid form');
            return;
        }
        this.confirm(event);
    }

    confirm(event: Event) {
        this.confirmationService.confirm({
            target: event.target as EventTarget,
            message: 'Deseja confirmar a movimentação de estoque?',
            accept: () => {
                this.submitDataMovement();
            },
        });
    }

    formatValue(event: Event, index: number) {
        // Obtém o valor diretamente do evento e remove todos os caracteres não numéricos
        let targetValue = (event.target as HTMLInputElement).value.replace(/\D/g, '');

        // Verifica se targetValue está vazio e define um valor padrão
        if (!targetValue) {
            this.sendMovement[index].priceTotal = ''; // ou "0,00" se preferir
            (event.target as HTMLInputElement).value = ''; // Atualiza o valor no campo de input
            return;
        }

        const value = this.utils.formatValue(targetValue);
        this.sendMovement[index].priceTotal = value;
        (event.target as HTMLInputElement).value = value; // Exibe o valor formatado no campo de input

    }


    updateQuantityPackage() {
        this.sendMovement.forEach(movement => {
            switch (movement.buyUnit.toUpperCase()) {
                case 'UN':
                case 'PÇ':
                case 'PAR':
                case 'KIT':
                case 'M':
                case 'CM':
                case 'KG':
                case 'T':
                case 'L':
                case 'ML':
                    movement.quantityPackage = '1';
                    this.calculateQuantity(movement);
                    movement.totalQuantity = Number(movement.inputQuantity).toString();
                    movement.requestUnit = this.filterUnits(movement.buyUnit.toUpperCase())[0];
                    console.log(movement);
                    break;
                default:
                    movement.requestUnit = this.filterUnits(movement.buyUnit.toUpperCase())[0];
                    this.calculateQuantity(movement);
                    console.log(movement);
                    break;
            }
        });
    }

    getOption(movement: StockMovementDTO) {
        return movement.buyUnit !== movement.requestUnit;
    }

    getTooltipText(buyUnit
                   :
                   string
    ):
        string {
        switch (buyUnit) {
            case 'CX':
                return 'Informe a quantidade de itens por Caixa. Exemplo: 5 itens por caixa.';
            case 'Rolo':
                return 'Informe o tamanho por Rolo em Metro. Exemplo: 5 metros por rolo.';
            default:
                return '';
        }
    }

    calculateQuantity(movement: StockMovementDTO) {
        if (['CX', 'PCT', 'M', 'SACO', 'L', 'PAR', 'ROLO'].includes(movement.buyUnit) && ['UN', 'PÇ', 'CM', 'KG', 'ML', 'M'].includes(movement.requestUnit)) {
            movement.totalQuantity = (Number(movement.inputQuantity) * Number(movement.quantityPackage)).toString();
        } else {
            movement.totalQuantity = Number(movement.inputQuantity).toString();
        }
    }

    filterUnits(selectedUnit: string): string[] {
        switch (selectedUnit.toUpperCase()) {
            case 'CX':
                return ["CX", "UN", "PÇ"]
            case 'ROLO':
                return ["Rolo", "M", "CM"]
            default:
                return [selectedUnit];
        }
    }


    onPageChange(event: any) {
        this.rows = event.rows;
        const first = event.first ?? 0;
        this.currentPage = Math.floor(first / this.rows);
        this.loadMaterials();  // Recarrega os materiais com a nova página
    }

    searchValue: string | null = null;

    handleSearch(value: string): void {
        if (value.length > 2) {
            this.searchValue = value;
            this.loadMaterials();
        } else if (value.length === 0) {
            this.searchValue = null;
            this.lastPage = this.currentPage;
            this.loadMaterials();
        }
    }

    skeleton: any[] = Array.from({length: 7}).map((_, i) => `Item #${i}`);
    onlySelected = false;
    scannerEnabled = false;
    formats = [
        BarcodeFormat.EAN_8,
        BarcodeFormat.UPC_A,
        BarcodeFormat.EAN_13,
        BarcodeFormat.ITF
    ];
    qrExpired = false;
    showQrCode = false;
    missingBarcodesCount: number = 0;
    endpoint = "";
    showFormMobile = false;

    protected isChecked(materialStockId: number) {
        return this.sendMovement.findIndex(s => s.materialStockId === materialStockId) !== -1;
    }

    goToCatalog(row: MaterialStockResponse) {
        console.log(row)
        void this.router.navigate(['/estoque/editar-material'], {
            queryParams: {materialId: row.materialId}
        });
    }

    clearFilters() {
        if (this.searchValue != null) {
            this.searchValue = null;
            this.lastPage = this.currentPage;
            this.loadMaterials();
        }
    }

    checkBarcode() {
        this.loadingOverlay = true;
        this.qrExpired = false;
        this.authService.getQrcodeToken().subscribe({
            next: (data) => {
                const origin = window.location.origin;
                const redirect = `/estoque/movimentar-estoque?almoxarifado=${this.currentDeposit?.idDeposit}`;
                this.endpoint =
                    `${origin}/auth/login` +
                    `?token=${data.token}` +
                    `&redirect=${encodeURIComponent(redirect)}`;
                console.log(this.endpoint);

                let expiresIn = data.expiresIn--;
                const interval = setInterval(() => {
                    expiresIn--;
                    if (expiresIn <= 0) {
                        this.qrExpired = true;
                        clearInterval(interval);
                    }
                }, 1000);
            },
            error: (err) => {
                this.loadingOverlay = false;
                this.utils.showMessage(
                    err.error.message ?? err.error.error ?? err.error,
                    'error'
                );
            },
            complete: () => {
                this.loadingOverlay = false;
                this.showQrCode = true;
            }
        })
        this.showQrCode = true
    }


    scanState: ScanState = 'idle';
    private isProcessing = false;

    protected onScanSuccess(code: string) {
        if (this.isProcessing) {
            return;
        }

        this.isProcessing = true;
        this.scanState = 'processing';

        this.materialService
            .findByBarCodeAndDepositId(code, this.currentDeposit?.idDeposit ?? 0)
            .subscribe({
                next: (material) => {
                    this.utils.playSound('bip');
                    this.addMovement(material, false);

                    this.scanState = 'idle';
                    this.isProcessing = false;
                },
                error: () => {
                    this.scanState = 'error';
                    this.utils.playSound('error');
                    this.utils.showMessage(
                        'O código lido não está cadastrado.',
                        'error',
                        'Código inválido'
                    );

                    setTimeout(() => {
                        this.scanState = 'idle';
                        this.isProcessing = false;
                    }, 1200);
                }
            });
    }

    handleContinue() {
        const message: string = this.isMobile ? 'Para prosseguir, pelo menos um item deve ser escaneado com sucesso.'
            : 'Para prosseguir, pelo menos um item deve ser selecionado.';

        if (this.sendMovement.length === 0) {
            this.utils.showMessage(message, 'warn', 'Atenção');
            return;
        }

        if (this.isMobile) {
            this.showFormMobile = true;
            this.scannerEnabled = false;
        } else {
            this.openMovementModal = true;
        }
    }

    selectDeposit() {
        if (this.isMobile) {
            this.loading = false;
            this.scannerEnabled = true;
        } else {
            this.loadMaterials();
        }
    }

    showFinishOption = false;

    hideMovement(i: number) {
        const movement = this.sendMovement[i];
        const lastHidden = this.sendMovement.filter(m => !m.hidden).length === 1;

        if (movement.inputQuantity === "" || Number(movement.inputQuantity) <= 0) {
            this.sendMovement[i].invalid = true;
        } else if (movement.priceTotal === "" || Number(movement.priceTotal) <= 0) {
            this.sendMovement[i].invalid = true;
        } else if (this.getOption(movement) && (movement.quantityPackage === "" || Number(movement.quantityPackage) <= 0)) {
            this.sendMovement[i].invalid = true;
        } else if (lastHidden) {
            this.sendMovement[i].hidden = true;
            this.showFormMobile = false;
            this.showFinishOption = true;
        } else {
            this.sendMovement[i].hidden = true;
        }
    }

    deleteMovement(i: number) {
        this.sendMovement.splice(i, 1);
    }

    reShowItems() {
        this.sendMovement.forEach((item) => {
            item.hidden = false;
        });
        this.showFinishOption = false;
        this.showFormMobile = true;
    }

    restart() {
        this.currentDeposit = undefined;
        this.materials = [];
        this.successMessage = false;
        this.loading = true;
    }
}

type ScanState = 'idle' | 'processing' | 'error';
