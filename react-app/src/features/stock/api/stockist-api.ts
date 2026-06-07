import api from '@/core/auth/api';
import type { StockistModel as ReservationStockistModel } from '@/features/requests/types/reservation';
import type {
  CreateStockistRequest,
  StockistModel as StockStockistModel,
  UpdateStockistRequest,
} from '../types/types';

const STOCKIST_API_BASE = '/api/stockist';

export const stockistApi = {
  async getStockists() {
    const { data } = await api.get<StockStockistModel[]>(`${STOCKIST_API_BASE}`);
    const normalized: ReservationStockistModel[] = data.map((stockist) => ({
      ...stockist,
      depositAddress: null,
      depositPhone: null,
      region: null,
    }));
    return normalized;
  },

  async insertStockist(stockist: CreateStockistRequest) {
    const { data } = await api.post(`${STOCKIST_API_BASE}`, stockist);
    return data;
  },

  async updateStockist(stockist: UpdateStockistRequest) {
    const { data } = await api.put(`${STOCKIST_API_BASE}`, stockist);
    return data;
  },

  async deleteStockist(stockistId: number) {
    await api.delete(`${STOCKIST_API_BASE}/${stockistId}`);
  },
};
