import api from '@/core/auth/api';
import type {
  ContractFilters,
  ContractItemsResponse,
  ContractItemsResponseWithExecutionsSteps,
  ContractReferenceItemBaseManagementDTO,
  ContractReferenceItemManagementDTO,
  ContractReferenceItemsDTO,
  ContractResponse,
  CreateContractDTO,
  SaveContractReferenceItemBaseDTO,
  SaveContractReferenceItemLinksDTO,
} from '@/features/contract/types';

const BASE = '/api/contracts';

export const contractsApi = {
  async getAllContracts(filters: ContractFilters) {
    const { data } = await api.post<ContractResponse[]>(`${BASE}/get-AllContracts`, filters);
    return data;
  },

  async createContract(contract: CreateContractDTO) {
    const { data } = await api.post<ContractResponse>(`${BASE}/insert-contract`, contract);
    return data;
  },

  async deleteById(contractId: number) {
    await api.delete(`${BASE}/delete-by-id/${contractId}`);
  },

  async archiveById(contractId: number) {
    await api.put(`${BASE}/archive-by-id/${contractId}`);
  },

  async getContractItems(contractId: number) {
    const { data } = await api.get<ContractItemsResponse[]>(`${BASE}/get-contract-items/${contractId}`);
    return data;
  },

  async getContractItemsWithExecutionsSteps(contractId: number) {
    const { data } = await api.get<ContractItemsResponseWithExecutionsSteps[]>(`${BASE}/get-contract-items-with-executions-steps/${contractId}`);
    return data;
  },

  async updateItems(items: ContractReferenceItemsDTO[], contractId: number) {
    await api.put(`${BASE}/update-items`, items, { params: { contractId } });
  },

  async getContractReferenceItems() {
    const { data } = await api.get<ContractReferenceItemsDTO[]>(`${BASE}/get-items`);
    return data;
  },

  async getReferenceItemBaseManagement() {
    const { data } = await api.get<ContractReferenceItemBaseManagementDTO[]>(`${BASE}/reference-items-management/base`);
    return data;
  },

  async saveReferenceItemsBase(items: SaveContractReferenceItemBaseDTO[]) {
    const { data } = await api.post<ContractReferenceItemBaseManagementDTO[]>(`${BASE}/reference-items-management/base`, items);
    return data;
  },

  async getReferenceItemLinkManagement() {
    const { data } = await api.get<ContractReferenceItemManagementDTO[]>(`${BASE}/reference-items-management/links`);
    return data;
  },

  async saveReferenceItemLinks(items: SaveContractReferenceItemLinksDTO[]) {
    const { data } = await api.post<ContractReferenceItemManagementDTO[]>(`${BASE}/reference-items-management/links`, items);
    return data;
  },

  async checkIfHasPendingLink() {
    const { data } = await api.get(`${BASE}/reference-items-management/check-if-pending-link`);
    return data;
  },
};
