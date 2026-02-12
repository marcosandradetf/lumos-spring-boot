import {Component, OnInit} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {CurrencyPipe, DecimalPipe, formatNumber, NgClass, NgForOf, NgIf} from '@angular/common';
import {ContractService} from '../../services/contract.service';
import {UtilsService} from '../../../core/service/utils.service';
import {FileService} from '../../../core/service/file-service.service';
import {Router} from '@angular/router';
import {ContractReferenceItemsDTO, ContractResponse, CreateContractDTO} from '../../contract-models';
import {Toast} from 'primeng/toast';
import {Title} from '@angular/platform-browser';
import {Step, StepList, StepPanel, StepPanels, Stepper} from 'primeng/stepper';
import {Button} from 'primeng/button';
import {InputText} from 'primeng/inputtext';
import {
    PrimeConfirmDialogComponent
} from '../../../shared/components/prime-confirm-dialog/prime-confirm-dialog.component';
import {Select} from 'primeng/select';
import {Dialog} from 'primeng/dialog';
import {LoadingOverlayComponent} from '../../../shared/components/loading-overlay/loading-overlay.component';
import {CompanyService} from '../../../company/service/company.service';
import {CompanyResponse} from '../../../company/dto/company.dto';
import {SharedState} from '../../../core/service/shared-state';
import {InputNumber} from 'primeng/inputnumber';


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
        InputNumber
    ],
    templateUrl: './create.component.html',
    styleUrl: './create.component.scss'
})
export class CreateComponent implements OnInit {
    selectedIndex: number | null = null;

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

    totalValue: number = 0;
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
                private title: Title,) {
    }

    currentUrl = "";

    ngOnInit() {
        this.currentUrl = this.router.url;

        if (this.currentUrl === '/contratos/editar') {
            const state = history.state as {
                contract?: ContractResponse;
                items?: ContractReferenceItemsDTO[];
            };

            if (state?.contract && state?.items) {
                const contract = state.contract;
                const items = state.items;

                this.contract = {
                    contractId: contract.contractId,
                    number: contract.number,
                    contractor: contract.contractor,
                    address: contract.address,
                    phone: contract.phone,
                    cnpj: contract.cnpj,
                    unifyServices: false,
                    noticeFile: null,
                    contractFile: contract.contractFile,
                    items: items,
                    companyId: contract.companyId,
                };

            } else {
                void this.router.navigate(['/contratos/listar'], {
                    queryParams: {for: 'view'}
                });
            }

            SharedState.setCurrentPath(['Contratos', 'Editar']);
        } else {
            SharedState.setCurrentPath(['Contratos', 'Novo']);
        }


        this.title.setTitle('Cadastrar Contrato');

        this.contractService.getContractReferenceItems()
            .subscribe({
                next: result => {
                    result.forEach(item => {
                        const index = this.contract.items.findIndex(i => i.contractReferenceItemId === item.contractReferenceItemId);
                        const ci = this.contract.items[index];

                        if (index !== -1) {
                            this.items.push({
                                contractReferenceItemId: item.contractReferenceItemId,
                                description: item.description,
                                nameForImport: item.nameForImport,
                                type: item.type,
                                linking: item.linking,
                                itemDependency: item.itemDependency,
                                quantity: item.quantity,
                                price: item.price,
                                executedQuantity: ci.executedQuantity,
                                contractItemId: ci.contractItemId
                            });
                        } else {
                            this.items.push(item);
                        }
                    });
                }
            });

        this.companyService.getCompanies()
            .subscribe(companies => this.companies = companies);
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
        item: ContractReferenceItemsDTO
        , index: number) {
        if (item.price === '0,00' || item.quantity === 0) {
            this.utils.showMessage("Para adicionar este item preencha o valor e a quantidade.", 'warn', "Atenção");
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
        if ((item.executedQuantity ?? 0) > 0) {
            this.utils.showMessage('Não é permitido remover um item com registro de execução.', 'warn', "Atenção");
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
            const price = parseFloat(
                item.price
            );

            const quantity = item.quantity || 0;

            // Convertendo para centavos e somando
            const itemTotal = Math.round(price * 100) * quantity;

            return sum + itemTotal;
        }, 0);

        // Depois de tudo, converter de volta para reais
        this.totalValue = (totalInCents / 100);
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


    setQuantity(input: HTMLInputElement, contractReferenceItemId: number) {
        const index = this.contract.items.findIndex(i => i.contractReferenceItemId === contractReferenceItemId);

        if (index !== -1) {
            const item = this.contract.items[index];
            const oldValue = item.quantity;
            const value = Number(input.value.replace(/^0+/, ''));

            if ((item.executedQuantity ?? 0) > 0 && value < (item.executedQuantity ?? 0)) {
                this.utils.showMessage(`A nova quantidade do item ${item.description} não pode ser menor que a quantidade executada.`, 'warn', 'Atenção');
                input.value = oldValue.toString();
                return;
            }

            this.contract.items[index].quantity = value
        }
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


    styleField(unify: HTMLSpanElement) {
        if (unify.classList.contains('btn-outline')) {
            unify.classList.remove('btn-outline');
            unify.innerText = 'Desativar Serviço Unificado';
            this.contract.unifyServices = true;
        } else {
            unify.classList.add('btn-outline');
            this.contract.unifyServices = false;
            unify.innerText = 'Clique para Ativar Serviço Unificado';
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
        const {number, contractor, address, phone, cnpj, companyId} = this.contract;

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

    validateContractItems(): boolean {
        const hasInvalidItem = this.contract.items.some(item =>
            !item.quantity || item.quantity === 0 ||
            !item.price || item.price.toString().trim() === ''
        );

        if (hasInvalidItem) {
            this.utils.showMessage(
                'Existem itens no contrato com quantidade zero ou valor vazio. Corrija esses dados antes de continuar.',
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
}
