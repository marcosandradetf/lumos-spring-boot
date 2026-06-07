import api from '@/core/auth/api';
import type {
  OrderHistoryItem,
  ReserveRequest,
  OrdersByCaseResponse,
  OrderActionPayload,
  ReplyPayload,
} from '../types/reservation';

export const requestApi = {
  async getReservation(depositId: number, status: string) {
    const { data } = await api.get<OrdersByCaseResponse[]>(
      '/api/order/get-orders-by-status-and-stockist',
      {
        params: { depositId, status },
      },
    );

    return data;
  },

  async reply(replies: ReplyPayload) {
    await api.post('/api/order/reply', replies);
  },

  async markAsCollected(orders: OrderActionPayload[]) {
    await api.post('/api/order/mark-as-collected', orders);
  },

  async getOrderHistoryByStatus(
    teamId: number,
    status: string,
    contractReferenceItemId: number,
  ) {
    const { data } = await api.get<OrderHistoryItem[]>('/api/order/get-order-history-by-status', {
      params: {
        teamId,
        status,
        contractReferenceItemId,
      },
    });

    return data;
  },

  async reserveMaterialsForExecution(reserve: ReserveRequest) {
    const { data } = await api.post<{ message: string }>(
      '/api/service-order/reserve-materials-for-execution',
      reserve,
    );
    return data;
  },
};
