import api from '@/core/auth/api';
import type { MaterialInStockDTO, ReserveRequest } from '../types/reservation';

export const executionApi = {
  async getPendingReservesForStockist() {
    const { data } = await api.get<ReserveRequest[]>('/api/service-order/get-reservations');
    return data;
  },

  async findMaterialsByContractReference(contractReferenceItemId: number, teamId: number) {
    const { data } = await api.get<MaterialInStockDTO[]>(
      `/api/stock/find-materials-by-contract-reference/${contractReferenceItemId}/${teamId}`,
    );

    return data;
  },

  async cancelStep(currentIds: number[], type: string | null) {
    await api.post('/api/execution/cancel-step', {
      currentIds,
      type,
    });
  },
};
