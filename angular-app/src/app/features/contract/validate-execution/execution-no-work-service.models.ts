export type ValidationStep = 'CONTRACT' | 'MAPPING' | 'REVIEW' | 'FINISHED';

export interface DirectExecution {
    directExecutionId: number;
    description?: string | null;
    instructions?: string | null;
    contractId?: number | null;
    directExecutionStatus: string;
    responsible?: string | null;
    startedAt?: string | null;   // ISO datetime
    finishedAt?: string | null;  // ISO datetime
    availableAt?: string | null; // ISO datetime
    externalId?: string | null;
    teamName?: string | null;
    streets: DirectExecutionStreet[];
}

export interface DirectExecutionStreet {
    directExecutionStreetId: number;
    address: string;
    pointNumber?: string | null;
    comment?: string | null;
    streetStatus: string;
    finishedAt?: string | null; // ISO datetime
    items: DirectExecutionStreetItem[];
}

export interface DirectExecutionStreetItem {
    directExecutionStreetItemId: number;
    directExecutionStreetId: number;
    executedQuantity: number;
    materialStockId?: number | null;
    contractItemId?: number | null;

    materialId: number;
    material: Material;

    suggestedContractReferenceItemId?: number | null;
    suggestedContractItemId?: number | null;

    selectedContractItemId?: number | null;
    selectedContractReferenceItemId?: number | null;
}
export interface Material {
    idMaterial: number;
    materialName: string;
    unitBase?: string | null;
    buyUnit?: string | null;
    requestUnit?: string | null;
    isGeneric?: boolean | null;
    truckStockControl?: boolean | null;
    referenceItemsIds: number[]
}

export interface Contract {
    contractId: number
    contractNumber: string
    contractor : string
    cnpj : string
    address : string
    phone : string | null
    creationDate : string
    createdBy: string | null
    contractFile : string | null
    status : string
    companyId: number
    lastUpdatedBy: string
    items: ContractItem[]
}

export interface ContractItem {
    contractItemId: number;
    contractedQuantity: number;
    quantityExecuted: number;
    factor: number;
    unitPrice?: number | null;
    totalPrice?: number | null;
    referenceItemId: number;
    referenceItem: ContractReferenceItem;
    executedQuantity: {
        installationId: number,
        step: number,
        quantity: number,
    }[];
    reservedQuantity: {
        installationId: number,
        step: number,
        quantity: number,
    }[];
}


export interface ContractReferenceItem {
    contractReferenceItemId: number;
    description: string;
    type?: string | null;
    itemDependency?: string | null;
    linking?: string | null;
    factor?: number | null;
    truckStockControl?: boolean | null;
    referenceMaterialsIds: number[]
}
