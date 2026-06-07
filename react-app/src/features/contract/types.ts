export interface ContractResponse {
  contractId: number;
  number: string;
  contractor: string;
  address: string;
  phone: string;
  cnpj: string;
  noticeFile: string | null;
  contractFile: string | null;
  createdBy: string;
  createdAt: string;
  itemQuantity: number;
  contractStatus: 'ACTIVE' | 'ARCHIVED';
  contractValue: string;
  additiveFile: string | null;
  companyId: number;
  lastUpdatedBy: string | null;
  
  ibgeCode: string | null;
  contractionDate: Date | null;
  dueDate: Date | null;
  contractType: string | null;


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

export interface ContractItemsResponseWithExecutionsSteps {
  contractReferenceItemId: number;
  number: number;
  contractItemId: number;
  description: string;
  factor: number;
  unitPrice: number;
  contractedQuantity: number;
  totalExecuted: number;
  showSteps: boolean | null;
  executedQuantity: { installationId: number; step: number; quantity: number }[];
  reservedQuantity: { installationId: number; step: number; quantity: number }[];
  linking: string | null;
  nameForImport: string | null;
  type: string;
}

export interface CreateContractDTO {
  contractId: number | null;
  number: string | null;
  contractor: string | null;
  address: string | null;
  phone: string | null;
  cnpj: string | null;
  unifyServices: boolean;
  noticeFile: string | null;
  contractFile: string | null;
  items: ContractReferenceItemsDTO[];
  companyId: number | null;

  ibgeCode: string | null;
  contractionDate: Date | null;
  dueDate: Date | null;
  contractType: string | null;
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
  factor: number;
  contractItemId: number | null;
  totalExecuted: number | null;
  executedQuantity: { installationId: number; step: number; quantity: number }[];
  reservedQuantity: { installationId: number; step: number; quantity: number }[];
}

export interface ContractReferenceItemBaseManagementDTO {
  contractReferenceItemId: number | null;
  description: string;
  type: string | null;
  status: 'ACTIVE' | 'PENDING_VALIDATION';
}

export interface ContractReferenceItemMaterialLinkDTO {
  materialId: number;
  materialName: string;
  description: string | null;
}

export interface ContractReferenceItemManagementDTO {
  contractReferenceItemId: number | null;
  description: string;
  type: string | null;
  status: 'ACTIVE' | 'PENDING_VALIDATION';
  materialLinks: ContractReferenceItemMaterialLinkDTO[];
  dependencyLinks: Array<{ contractReferenceItemId: number; description: string; type: string | null }>;
}

export interface SaveContractReferenceItemBaseDTO {
  clientDraftId?: string | null;
  contractReferenceItemId: number | null;
  description: string;
  type: string | null;
}

export interface SaveContractReferenceItemLinksDTO {
  contractReferenceItemId: number;
  materialIds: number[];
  dependencyReferenceItemIds: number[];
}

export interface ContractFilters {
  contractor: string | null;
  startDate: Date | null;
  endDate: Date | null;
  status: 'ACTIVE' | 'ARCHIVED' | null;
}
