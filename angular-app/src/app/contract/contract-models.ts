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
}

export interface ContractItemsResponse {
  number: number;
  contractItemId: number;
  description: string;
  unitPrice: string;
  contractedQuantity: number;
  executedQuantity: number;
  linking: string | null;
  nameForImport: string | null;
  type: string;
}

export interface CreateContractDTO {
  number: string,
  contractor: string,
  address: string,
  phone: string,
  cnpj: string,
  unifyServices: boolean;
  noticeFile: string;
  contractFile: string;
  userUUID: string;
  items: ContractReferenceItemsDTO[]
}

export interface ContractReferenceItemsDTO {
  contractReferenceItemId: number;
  description: string;
  nameForImport: string;
  type: string;
  linking: string;
  itemDependency: string;
  quantity: number;
  price: string;
}
