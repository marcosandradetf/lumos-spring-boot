import {Component, OnInit} from '@angular/core';
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
import {MenuItem} from 'primeng/api';
import {TableModule} from 'primeng/table';
import {Skeleton} from 'primeng/skeleton';
import {Toast} from 'primeng/toast';
import {DropdownModule} from 'primeng/dropdown';
import {PrimeBreadcrumbComponent} from '../../shared/components/prime-breadcrumb/prime-breadcrumb.component';
import {ActivatedRoute, Router} from '@angular/router';
import {InputText} from 'primeng/inputtext';
import {Button, ButtonDirective} from 'primeng/button';
import {ToggleButton} from 'primeng/togglebutton';
import {SpeedDial} from 'primeng/speeddial';
import {IconField} from 'primeng/iconfield';
import {InputIcon} from 'primeng/inputicon';
import {Toolbar} from 'primeng/toolbar';
import {QRCodeModule} from 'angularx-qrcode';
import {LoadingOverlayComponent} from '../../shared/components/loading-overlay/loading-overlay.component';

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
        SpeedDial,
        Button,
        IconField,
        InputIcon,
        Toolbar,
        QRCodeModule,
        LoadingOverlayComponent,
        ButtonDirective,
        ReactiveFormsModule,
    ],
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

    items: MenuItem[] = [
        {
            label: 'Selecionar itens',
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
    scannerMode = false;

    private validate(): boolean {
        for (let item of this.sendMovement) {
            if (item.inputQuantity.length === 0) {
                this.utils.showMessage("A quantidade não pode ser igual a 0 ou estar inválida.", 'warn', 'Atenção');
                return false; // Aqui retorna e sai da função
            } else if (item.quantityPackage.length === 0) {
                this.utils.showMessage("A quantidade por embalagem não pode ser igual a 0 ou estar inválida.", 'warn', 'Atenção');
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
    ) {
    }

    ngOnInit() {
        this.isMobile = window.innerWidth <= 768;
        this.titleService.setTitle("Movimentar Estoque");
        this.stockService.getDeposits().subscribe(d => this.deposits = d);
        const depositId = this.route.snapshot.queryParamMap.get('almoxarifado')
        if(depositId) {
            this.currentDeposit = this.deposits.find(d => d.idDeposit === Number(depositId));
        }
        this.scannerEnabled = this.isMobile;
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

    toggleSelection(item: MaterialStockResponse) {
        const index = this.sendMovement.findIndex(s => s.materialStockId === item.materialStockId);

        if (index === -1) {
            const newMovement: StockMovementDTO = {
                materialStockId: item.materialStockId,
                materialName: item.materialName,
                description: '',
                buyUnit: item.buyUnit,
                requestUnit: item.requestUnit,
                inputQuantity: '',
                priceTotal: '',
                quantityPackage: '',
                supplierId: '',
                totalQuantity: 0
            };
            this.sendMovement.push(newMovement);
        } else {
            this.sendMovement = this.sendMovement.filter(movement => movement.materialStockId !== item.materialStockId);
        }
    }

    submitDataMovement(): void {
        this.stockService.stockMovement(this.sendMovement).pipe(
            tap(response => {
                this.utils.showMessage(response, 'success', 'Movimentação salva');
                this.formSubmitted = false;
                this.closeConfirmationModal();
                this.closeMovementModal();
                this.clearSelection();
            }),
            catchError(err => {
                this.utils.showMessage(err.error.message, 'error', 'Erro ao salvar movimentação');
                this.formSubmitted = false;
                this.closeConfirmationModal();
                return throwError(() => err);
            })
        ).subscribe();
    }


    closeMovementModal() {
        this.openMovementModal = false;
    }

    handleOpenSupplierModal() {
        this.openSupplierModal = true;
    }


    closeConfirmationModal() {
        this.openConfirmationModal = false;
    }

    clearSelection(): void {
        this.sendMovement = [];
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


    submitFormMovement(form: NgForm) {
        this.formSubmitted = true;

        if (form.invalid) {
            console.log("invalid form");
            return;
        }

        this.handleConfirmMovement();
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
                    movement.totalQuantity = Number(movement.inputQuantity);
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
                return false;
            default:
                return true;
        }
    }

    shouldShowTooltip(buyUnit
                      :
                      string
    ):
        boolean | string {
        const unitsWithTooltip = ["CX", "ROLO"];
        return buyUnit && unitsWithTooltip.includes(buyUnit.toUpperCase());
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
        switch (movement.requestUnit.toUpperCase()) {
            case 'UN':
            case 'PÇ':
                movement.totalQuantity = Number(movement.inputQuantity) * Number(movement.quantityPackage);
                break;
            case 'CM':
            case 'KG':
                if (movement.buyUnit === 'CM' || movement.buyUnit === 'KG') {
                    movement.totalQuantity = Number(movement.inputQuantity) * 100;
                } else if (movement.buyUnit === 'Rolo') {
                    movement.totalQuantity = Number(movement.inputQuantity) * (Number(movement.quantityPackage) * 100);
                } else {
                    movement.totalQuantity = Number(movement.inputQuantity);
                }
                break;
            case 'M':
            case 'T':
                if (movement.buyUnit === 'CM' || movement.buyUnit === 'KG') {
                    movement.totalQuantity = Number(movement.inputQuantity) / 100;
                } else if (movement.buyUnit === 'Rolo') {
                    movement.totalQuantity = Number(movement.inputQuantity) * Number(movement.quantityPackage);
                } else {
                    movement.totalQuantity = Number(movement.inputQuantity);
                }
                break;
            default:
                movement.totalQuantity = Number(movement.inputQuantity);
                break;
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
    showQrCode= false;
    missingBarcodesCount: number = 0;

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
        this.showQrCode = true
    }
}
