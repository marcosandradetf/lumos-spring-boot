import { Component, HostListener, inject, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AsyncPipe, CurrencyPipe, DecimalPipe, formatNumber, NgClass, NgForOf, NgIf } from '@angular/common';
import { ContractService } from '../../services/contract.service';
import { UtilsService } from '../../../core/service/utils.service';
import { FileService } from '../../../core/service/file-service.service';
import { Router } from '@angular/router';
import { ContractReferenceItemsDTO, ContractResponse, CreateContractDTO } from '../../contract-models';
import { Toast } from 'primeng/toast';
import { DomSanitizer, SafeResourceUrl, Title } from '@angular/platform-browser';
import { Step, StepList, StepPanel, StepPanels, Stepper } from 'primeng/stepper';
import { Button } from 'primeng/button';
import { InputText } from 'primeng/inputtext';
import {
    PrimeConfirmDialogComponent
} from '../../../shared/components/prime-confirm-dialog/prime-confirm-dialog.component';
import { Select } from 'primeng/select';
import { Dialog } from 'primeng/dialog';
import { LoadingOverlayComponent } from '../../../shared/components/loading-overlay/loading-overlay.component';
import { CompanyService } from '../../../company/service/company.service';
import { CompanyResponse } from '../../../company/dto/company.dto';
import { SharedState } from '../../../core/service/shared-state';
import { InputNumber } from 'primeng/inputnumber';
import { TableModule } from 'primeng/table';
import { Popover } from 'primeng/popover';
import { GuideStateComponent } from '../../../guide-state/guide-state.component';
import { LoadingComponent } from '../../../shared/components/loading/loading.component';
import { forkJoin } from 'rxjs';
import { Utils } from '../../../core/service/utils';
import { Divider } from 'primeng/divider';
import { FcmService } from '../../../core/service/fcm.service';


@Component({
    selector: 'app-create',
    standalone: true,
    imports: [
        FormsModule,
        NgIf,
        NgForOf,
        Toast,
        StepPanel,
        Button,
        Stepper,
        StepList,
        Step,
        StepPanels,
        InputText,
        NgClass,
        CurrencyPipe,
        PrimeConfirmDialogComponent,
        Select,
        Dialog,
        LoadingOverlayComponent,
        DecimalPipe,
        InputNumber,
        TableModule,
        Popover,
        GuideStateComponent,
        LoadingComponent,
        Divider,
        AsyncPipe
    ],
    templateUrl: './create.component.html',
    styleUrl: './create.component.scss'
})
export class CreateComponent implements OnInit {
    selectedIndex: number | null = null;
    isMobileView = false;
    embeddedDocOpen = false;
    embeddedDocTitle = '';
    embeddedDocDescription = '';
    embeddedDocUrl: SafeResourceUrl | null = null;
    readonly notificationGuideUrl = 'https://lumosip.com.br/como-usar/15-web-config/01-enable-notifications/';
    protected hasNotifications$ = inject(FcmService).hasNotifications$;

    contract: CreateContractDTO = {
        contractId: null,
        number: null,
        contractor: null,
        address: null,
        phone: null,
        cnpj: null,
        unifyServices: false,
        noticeFile: null,
        contractFile: null,
        items: [],
        companyId: null,
    }

    logoFile: File | null = null;

    items: ContractReferenceItemsDTO[] = [];

    totalItems: number = 0;
    removingIndex: number | null = null;
    openModal: boolean = false;
    companies: CompanyResponse[] = [];
    company = {
        idCompany: 0,
        socialReason: '',
        fantasyName: '',
        companyCnpj: '',
        companyContact: '',
        companyPhone: '',
        companyEmail: '',
        companyAddress: ''
    };

    constructor(protected contractService: ContractService,
        protected utils: UtilsService,
        private fileService: FileService,
        protected router: Router,
        private companyService: CompanyService,
        private title: Title,
        private sanitizer: DomSanitizer,) {
    }

    currentUrl = "";
    step = 1;

    ngOnInit() {
        this.loading = true;
        this.updateViewportFlags();
        this.currentUrl = this.router.url;

        if (this.currentUrl === '/contratos/editar') {
            const state = history.state as {
                contract?: ContractResponse;
                items?: ContractReferenceItemsDTO[];
                step?: number | undefined | null
            };

            if (state?.contract && state?.items) {
                const contract = state.contract;
                const items = state.items;
                const unifyServices = items.some(i => i.factor !== 1);

                this.contract = {
                    contractId: contract.contractId,
                    number: contract.number,
                    contractor: contract.contractor,
                    address: contract.address,
                    phone: contract.phone,
                    cnpj: contract.cnpj,
                    unifyServices: unifyServices,
                    noticeFile: null,
                    contractFile: contract.contractFile,
                    items: items,
                    companyId: contract.companyId,
                };

                if (state?.step) {
                    this.step = state?.step;
                }

            } else {
                void this.router.navigate(['/contratos/listar'], {
                    queryParams: { for: 'view' }
                });
            }

            this.normalizeFactors();

            SharedState.setCurrentPath(['Contratos', 'Editar']);
        } else {
            SharedState.setCurrentPath(['Contratos', 'Novo']);
        }


        this.title.setTitle('Cadastrar Contrato');


        forkJoin({
            referenceItems: this.contractService.getContractReferenceItems(),
            companies: this.companyService.getCompanies()
        }).subscribe({
            next: (data) => {
                data.referenceItems.forEach(item => {
                    const index = this.contract.items.findIndex(i => i.contractReferenceItemId === item.contractReferenceItemId);

                    if (index !== -1) {
                        return;
                    } else {
                        this.items.push(item);
                    }
                });

                this.companies = data.companies;
                this.loading = false;
            },
            error: (err) => {
                Utils.handleHttpError(err, this.router);
                this.loading = false;
            }
        });
    }

    @HostListener('window:resize')
    onWindowResize() {
        this.updateViewportFlags();
    }

    private updateViewportFlags() {
        this.isMobileView = window.innerWidth < 768;
    }


    async submitContract() {
        if (this.loading) return;
        this.loading = true;
        this.openModal = false;

        try {
            if (this.selectedFiles.length > 0) {
                this.contract.contractFile = await this.fileService.sendFile(this.selectedFiles);
            }

            this.sendContract(); // envia formulário após upload
        } catch (error: any) {
            this.utils.showMessage(error?.error?.message || 'Erro ao enviar arquivo', 'error');
        } finally {
            this.loading = false;
        }
    }

    onLogoSelected(event: any) {
        this.logoFile = event.target.files[0];
    }


    sendContract() {
        this.contractService.createContract(this.contract).subscribe({
            next: response => this.resetForm(),
            error: error => {
                this.openModal = false;
                this.utils.showMessage(error.error.message, 'error');
                this.loading = false;
            },
        });
    }

    resetForm() {
        this.openModal = false;
        this.contract = {
            contractId: null,
            number: null,
            contractor: null,
            address: null,
            phone: null,
            cnpj: null,
            unifyServices: false,
            noticeFile: null,
            contractFile: null,
            items: [],
            companyId: null
        };
        this.normalizeFactors();
        this.loading = false;
        this.finish = true;
    }

    resetCompanyForm() {
        this.company = {
            idCompany: 0,
            socialReason: '',
            fantasyName: '',
            companyCnpj: '',
            companyContact: '',
            companyPhone: '',
            companyEmail: '',
            companyAddress: ''
        };
    }

    loading: boolean = false;
    btnLoading: boolean = false;
    finish: boolean = false;


    addItem(
        item: ContractReferenceItemsDTO,
        index: number
    ) {
        console.log(item);
        if (!this.isItemValid(item)) {
            const message = this.contract.unifyServices
                ? 'Para adicionar este item preencha o valor unitário, a quantidade e um fator maior que zero.'
                : 'Para adicionar este item preencha o valor unitário e a quantidade.';
            this.utils.showMessage(message, 'warn', "Atenção");
            return;
        }

        this.removingIndex = index;
        // Aguarda a animação antes de remover o item
        setTimeout(() => {
            this.items = this.items.filter(i => i.contractReferenceItemId !== item.contractReferenceItemId);
            this.totalItems += 1;
            this.removingIndex = null;
        }, 900); // Tempo igual à transição no CSS
        this.contract.items.push(item);

    }

    protected readonly open = open;

    removingIndexContract: number | null = null;
    textContent: string = 'Clique para selecionar';
    typeSelect: string = '';
    visible = false;
    companyFormSubmit = false;

    removeItem(item: ContractReferenceItemsDTO, index: number) {
        const minValue = this.getTotalReservedAndExecutedValue(item);

        if (minValue > 0) {
            this.utils.showMessage(
                'Por motivo de segurança de dados, não é permitido excluir um item com registro de instalação ou O.S. no sistema.',
                'warn',
                "Atenção");
            return;
        }

        this.removingIndexContract = index;
        setTimeout(() => {
            this.removingIndexContract = null;
            this.items.push(item);
            this.contract.items = this.contract.items.filter(i => i.contractReferenceItemId !== item.contractReferenceItemId);
            this.totalItems -= 1;
        }, 900); // Tempo igual à transição no CSS
    }

    getTotalValue() {
        let totalInCents = this.contract.items.reduce((sum, item) => {
            const price = item.price || 0;
            const quantity = this.getCalculatedQuantity(item);

            // Convertendo para centavos e somando
            const itemTotal = Math.round(price * 100) * quantity;

            return sum + itemTotal;
        }, 0);

        // Depois de tudo, converter de volta para reais
        return (totalInCents / 100);
    }

    getEffectiveFactor(item: ContractReferenceItemsDTO): number {
        if (!this.contract.unifyServices) {
            return 1;
        }

        const factor = Number(item.factor ?? 1);
        return factor > 0 ? factor : 1;
    }

    getCalculatedQuantity(item: ContractReferenceItemsDTO): number {
        const quantity = Number(item.quantity ?? 0);
        return quantity * this.getEffectiveFactor(item);
    }

    getItemTotalValue(item: ContractReferenceItemsDTO): number {
        const price = Number(item.price ?? 0);
        return price * this.getCalculatedQuantity(item);
    }

    toggleUnifyServices() {
        this.contract.unifyServices = !this.contract.unifyServices;
        this.normalizeFactors();
    }

    private normalizeFactors() {
        const normalize = (items: ContractReferenceItemsDTO[]) => {
            items.forEach(item => {
                if (!this.contract.unifyServices) {
                    item.factor = 1;
                    return;
                }

                const factor = Number(item.factor ?? 1);
                item.factor = factor > 0 ? factor : 1;
            });
        };

        normalize(this.items);
        normalize(this.contract.items);
    }

    private isItemValid(item: ContractReferenceItemsDTO): boolean {
        const hasPrice = Number(item.price ?? 0) > 0;
        const hasQuantity = Number(item.quantity ?? 0) > 0;
        const hasFactor = !this.contract.unifyServices || Number(item.factor ?? 0) > 0;

        return hasPrice && hasQuantity && hasFactor;
    }


    setServiceQuantity(item: {
        contractReferenceItemId: number;
        description: string;
        completeDescription: string;
        type: string;
        linking: string;
        itemDependency: string;
        quantity: number;
        price: string;
    }) {
        const quantity = this.items
            .filter(s => s.type === item.type && !s.description.includes("SERVIÇO"))
            .reduce((sum, i) => sum + i.quantity, 0);

        this.items
            .filter(s => (s.itemDependency === item.type))
            .forEach(i => i.quantity = quantity);
    }

    reviewItems(contractItems: HTMLDivElement, steepFinal: HTMLDivElement) {
        if (this.contract.items.length > 0) {
            contractItems.classList.add('hidden');
            steepFinal.classList.remove('hidden');
        } else {
            this.utils.showMessage("Para revisar os itens do contrato, é necessário adicionar pelo menos um item", 'warn', "Atenção");
        }
    }

    setCableQuantity(item: {
        contractReferenceItemId: number;
        description: string;
        completeDescription: string;
        type: string;
        linking: string;
        itemDependency: string;
        quantity: number;
        price: string
    }) {
        if (item.type !== "BRAÇO") {
            return;
        }
        let quantity = 0.0;

        this.items
            .filter(s => s.type === item.type)
            .forEach((i) => {
                if (i.linking) { // Verifica se linking é uma string válida
                    if (i.linking.startsWith('1')) {
                        quantity += i.quantity * 2.5;
                    } else if (i.linking.startsWith('2')) {
                        quantity += i.quantity * 8.5;
                    } else if (i.linking.startsWith('3')) {
                        quantity += i.quantity * 12.5;
                    }
                }
            });

        const cable = this.items.find(s => s.type === "CABO");
        if (cable) {
            cable.quantity = quantity;
        }
    }

    openConfirmModal() {
        if (!this.validateContractItems()) return;
        if (!this.validateContractFields()) return;

        this.openModal = true;
    }

    handleAction($event: "accept" | "reject") {
        switch ($event) {
            case 'reject':
                this.openModal = false;
                this.utils.showMessage('Operação cancelada com sucesso', 'info', 'Feito');
                break;
            case 'accept':
                this.submitContract();
                break;
        }
    }

    validateContractFields(): boolean {
        const { number, contractor, address, phone, cnpj, companyId } = this.contract;

        if (!number && !contractor && !address && !phone && !cnpj) {
            this.utils.showMessage(
                'Nenhum dado do contrato foi preenchido. Volte para o passo 1 e preencha as informações obrigatórias(*) antes de finalizar.',
                'warn',
                'Atenção',
                true
            );
            return false;
        }


        if (!number) {
            this.utils.showMessage('Número do contrato é obrigatório.', 'warn', 'Atenção');
            return false;
        }

        if (!contractor) {
            this.utils.showMessage('Contratante é obrigatório.', 'warn', 'Atenção');
            return false;
        }

        if (!address) {
            this.utils.showMessage('Endereço é obrigatório.', 'warn', 'Atenção');
            return false;
        }

        if (!phone) {
            this.utils.showMessage('Telefone é obrigatório.', 'warn', 'Atenção');
            return false;
        }

        if (!cnpj) {
            this.utils.showMessage('CNPJ é obrigatório.', 'warn', 'Atenção');
            return false;
        }

        if (!companyId) {
            this.utils.showMessage('Empresa prestadora é obrigatório.', 'warn', 'Atenção');
            return false;
        }

        return true;
    }

    private validateContractItems(): boolean {
        const hasInvalidItem = this.contract.items.some(item => !this.isItemValid(item));

        if (hasInvalidItem) {
            this.utils.showMessage(
                this.contract.unifyServices
                    ? 'Existem itens no contrato com quantidade, fator ou valor unitário inválidos. Corrija esses dados antes de continuar.'
                    : 'Existem itens no contrato com quantidade ou valor unitário igual a zero ou vazio. Corrija esses dados antes de continuar.',
                'warn',
                'Atenção'
            );
            return false;
        }

        return true;
    }


    protected saveCompany() {
        this.companyFormSubmit = true;
        if (!this.validateNewCompanyFields()) return;

        this.btnLoading = true;
        this.companyService.create(this.company, this.logoFile!!).subscribe({
            next: companyId => this.company.idCompany = companyId,
            error: err => {
                this.btnLoading = false;
                this.utils.showMessage(err.error.error || err.error.message || err.error, "error");
            },
            complete: () => {
                this.companies.push(this.company);
                this.visible = false;
                this.resetCompanyForm();
                this.btnLoading = false;
                this.utils.showMessage(
                    "A nova empresa foi cadastrada e já pode ser vinculada a este contrato.",
                    "success",
                    "Cadastro realizado com sucesso"
                );
            }
        });
    }

    validateNewCompanyFields(): boolean {
        const {
            socialReason,
            fantasyName,
            companyCnpj,
            companyContact,
            companyPhone,
            companyEmail,
            companyAddress
        } = this.company;

        if (socialReason === '' || fantasyName === '' || companyCnpj === '' || companyContact === '' || companyPhone === ''
            || companyEmail === '' || companyAddress === '' || !this.logoFile) {
            this.utils.showMessage(
                'Para salvar, preencha todos os campos obrigatórios(*).',
                'warn',
                'Atenção',
            );
            return false;
        }

        return true;
    }


    selectedFiles: File[] = [];
    maxFileSize = 10 * 1024 * 1024; // 10MB

    onFilesSelected(event: Event) {
        const input = event.target as HTMLInputElement;
        if (!input.files) {
            this.selectedFiles = [];
            return;
        }

        // filtra arquivos por tamanho e tipo
        const allowedTypes = ['application/pdf', 'application/zip'];
        const files = Array.from(input.files).filter(file => {
            const isAllowedType = allowedTypes.includes(file.type);
            const isAllowedSize = file.size <= this.maxFileSize;
            return isAllowedType && isAllowedSize;
        });

        this.selectedFiles = files;

        if (files.length < input.files.length) {
            // opcional: avisar que algum arquivo foi rejeitado
            alert('Alguns arquivos foram ignorados por tipo ou tamanho.');
        }
    }


    protected readonly formatNumber = formatNumber;

    openNotificationGuide() {
        this.embeddedDocTitle = 'Ativando as Notificações';
        this.embeddedDocDescription = 'Guia para permitir notificações no navegador e receber alertas operacionais do Lumos.';
        this.embeddedDocUrl = this.sanitizer.bypassSecurityTrustResourceUrl(this.notificationGuideUrl);
        this.embeddedDocOpen = true;
    }

    closeEmbeddedDoc() {
        this.embeddedDocOpen = false;
        this.embeddedDocTitle = '';
        this.embeddedDocDescription = '';
        this.embeddedDocUrl = null;
    }

    checkMin(item: any, model: any, pop: any, input: any) {
        const min = this.getTotalReservedAndExecutedValue(item, 1);

        if ((item.quantity ?? 0) < min) {
            model.control.setErrors({ minCustom: true });
            pop.show(null, input.input.nativeElement);
        } else {
            model.control.setErrors(null);
            pop.hide();
        }
    }

    protected getTotalReserved(contractItemId: number): number {
        const contractItem = this.contract.items
            .find(i => i.contractItemId === contractItemId);

        if (!contractItem) return 0;

        return (contractItem.reservedQuantity ?? [])
            .reduce((sum, r) => sum + (r.quantity ?? 0), 0);
    }

    getTotalReservedAndExecutedValue(item: ContractReferenceItemsDTO, defaultValue = 0): number {
        const reservedQuantity = this.getTotalReserved(item.contractItemId ?? -1);
        const value = (item.totalExecuted ?? 0) + reservedQuantity;
        if (value === 0) return defaultValue;
        return value;
    }
}
