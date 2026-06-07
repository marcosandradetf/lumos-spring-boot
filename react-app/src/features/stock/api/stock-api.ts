import api from '@/core/auth/api';
import type {
  Deposit,
  DepositByStockist,
} from '@/features/requests/types/reservation';
import type {
  Group,
  MaterialType,
  MaterialTypeSubtype,
  StockMovementDTO,
  StockMovementResponse,
  Supplier,
  UnitsResponse,
} from '../types/types';

export const stockApi = {
  // --- Deposits ---
  async getDeposits() {
    const { data } = await api.get<Deposit[]>('/api/deposit');
    return data;
  },

  async insertDeposit(deposit: Omit<Deposit, 'idDeposit'>) {
    const { data } = await api.post<Deposit>('/api/deposit/insert', deposit);
    return data;
  },

  async updateDeposit(id: number, deposit: Partial<Deposit>) {
    const { data } = await api.put<Deposit>(`/api/deposit/${id}/update`, deposit);
    return data;
  },

  async deleteDeposit(depositId: number) {
    await api.delete(`/api/deposit/${depositId}/delete`);
  },

  async getDepositsByStockist(userId: string) {
    const { data } = await api.get<DepositByStockist[]>('/api/deposit/get-deposits-by-stockist', {
      params: { userId },
    });
    return data;
  },

  // --- Types ---
  async getTypes() {
    const { data } = await api.get<MaterialType[]>('/api/type');
    return data;
  },

  async findAllTypeSubtype() {
    const { data } = await api.get<MaterialTypeSubtype[]>('/api/type/get-all-type-subtype');
    return data;
  },

  async findUnitsByTypeId(typeId: number) {
    const { data } = await api.get<UnitsResponse>(`/api/type/get-units-by-type/${typeId}`);
    return data;
  },

  async insertType(type: Partial<MaterialType>) {
    const { data } = await api.post<MaterialType[]>('/api/type/insert', type);
    return data;
  },

  async updateType(typeId: number, type: Partial<MaterialType>) {
    const { data } = await api.put<MaterialType[]>(`/api/type/${typeId}/update`, type);
    return data;
  },

  async deleteType(id: number) {
    await api.delete(`/api/type/${id}/delete`);
  },

  // --- Groups ---
  async getGroups() {
    const { data } = await api.get<Group[]>('/api/group');
    return data;
  },

  async insertGroup(groupName: string) {
    const { data } = await api.post<Group[]>('/api/group/insert', groupName);
    return data;
  },

  async updateGroup(id: number, groupName: string) {
    const { data } = await api.put<Group[]>(`/api/group/${id}/update`, groupName);
    return data;
  },

  async deleteGroup(groupId: number) {
    await api.delete(`/api/group/${groupId}/delete`);
  },

  // --- Suppliers ---
  async getSuppliers() {
    const { data } = await api.get<Supplier[]>('/api/stock/get-suppliers');
    return data;
  },

  async createSupplier(supplier: Partial<Supplier>) {
    const { data } = await api.post<Supplier>('/api/stock/create-supplier', supplier);
    return data;
  },

  // --- Stock Movements ---
  async getMovements() {
    const { data } = await api.get<StockMovementResponse[]>('/api/stock/stock-movement/get');
    return data;
  },

  async getMovementsApproved() {
    const { data } = await api.get<StockMovementResponse[]>('/api/stock/stock-movement/get-approved');
    return data;
  },

  async createMovement(movements: StockMovementDTO[]) {
    await api.post('/api/stock/stock-movement/create', movements, { responseType: 'text' });
  },

  async approveMovement(id: number) {
    await api.post(`/api/stock/stock-movement/approve/${id}`, null, { responseType: 'text' });
  },

  async rejectMovement(id: number) {
    await api.post(`/api/stock/stock-movement/reject/${id}`, null, { responseType: 'text' });
  },
};
