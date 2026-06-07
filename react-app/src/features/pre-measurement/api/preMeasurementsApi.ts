import api from '@/core/auth/api';
import type {
  AvailableStockByStreet,
  CheckBalanceRequest,
  DelegateExecutionDTO,
  ListPreMeasurementRequest,
  PreMeasurementResponseDTO,
} from '@/features/pre-measurement/types/types';

const BASE = '/api/execution';

export const preMeasurementsApi = {
  async getPreMeasurements(status: string) {
    const { data } = await api.get<ListPreMeasurementRequest[]>(`${BASE}/get-pre-measurements/${status}`);
    return data;
  },

  async getPreMeasurement(id: string | number) {
    const { data } = await api.get<PreMeasurementResponseDTO>(`${BASE}/get-pre-measurement/${id}`);
    return data;
  },

  async markAsAvailable(preMeasurementId: number) {
    await api.post(`/api/pre-measurement/mark-as-available/${preMeasurementId}`, null);
  },

  async checkBalance(preMeasurementId: number) {
    const { data } = await api.get<CheckBalanceRequest[]>(
      `/api/execution/check-balance-pre-measurement/${preMeasurementId}`,
    );
    return data;
  },

  async getStockAvailable(preMeasurementId: number, teamId: number) {
    const { data } = await api.get<AvailableStockByStreet[]>(
      '/api/execution/get-available-stock',
      {
        params: { preMeasurementId, teamId },
      },
    );
    return data;
  },

  async delegateExecution(delegateDTO: DelegateExecutionDTO) {
    await api.post('/api/execution/delegate', delegateDTO);
  },
};
