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
  noticeFile: string,
  contractFile: string,
  createdBy: string,
  createdAt: string,
  itemQuantity: number,
  contractStatus: string,
  contractValue: string,
  additiveFile: string
}

export interface ContractItemsResponse {
  number: number;
  contractItemId: number;
  description: string;
  unitPrice: string;
  contractedQuantity: number;
  linking: string;
  nameForImport: string;
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
