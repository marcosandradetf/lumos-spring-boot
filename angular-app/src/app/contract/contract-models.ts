export interface ContractAndItemsResponse {
    contractId: number,
    number: string,
    contractor: string,
    address: string,
    phone: string,
    cnpj: string,
    noticeFile: string,
    contractFile: string,
    createdBy: string,
    createdAt: string,
    itemQuantity: number,
    contractStatus: string,
    contractValue: string,
    additiveFile: string
    items: ContractItemsResponse[]
}

export interface ContractResponse {
    contractId: number,
    number: string,
    contractor: string,
    address: string,
    phone: string,
    cnpj: string,
    noticeFile: string | null,
    contractFile: string | null,
    createdBy: string,
    createdAt: string,
    itemQuantity: number,
    contractStatus: string,
    contractValue: string,
    additiveFile: string | null,
    companyId: number,
    lastUpdatedBy: string | null,
}

export interface ContractItemsResponse {
    number: number;
    contractItemId: number;
    description: string;
    unitPrice: string;
    contractedQuantity: number;
    executedQuantity: number;
    reservedQuantity: number;
    linking: string | null;
    nameForImport: string | null;
    type: string;
}

export interface CreateContractDTO {
    contractId: number | null,
    number: string | null,
    contractor: string | null,
    address: string | null,
    phone: string | null,
    cnpj: string | null,
    unifyServices: boolean;
    noticeFile: string | null;
    contractFile: string | null;
    items: ContractReferenceItemsDTO[];
    companyId: number | null
}

export interface ContractReferenceItemsDTO {
    contractReferenceItemId: number;
    description: string;
    nameForImport: string;
    type: string;
    linking: string;
    itemDependency: string;
    quantity: number;
    price: number;
    contractItemId: number | null;
    executedQuantity: {
        installationId: number,
        step: number,
        quantity: number,
    }[];
    totalExecuted: number | null;
    reservedQuantity: {
        installationId: number,
        step: number,
        quantity: number,
    }[];
}

export interface ContractItemsResponseWithExecutionsSteps {
    contractReferenceItemId: number;
    number: number;
    contractItemId: number;
    description: string;
    unitPrice: number;
    contractedQuantity: number;
    executedQuantity: {
        installationId: number,
        step: number,
        quantity: number,
    }[];
    totalExecuted: number,
    reservedQuantity: {
        installationId: number,
        step: number,
        quantity: number,
    }[];
    linking: string | null;
    nameForImport: string | null;
    type: string;
    showSteps: boolean | null;
}
