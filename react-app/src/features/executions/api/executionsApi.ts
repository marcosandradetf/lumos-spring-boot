import api from '@/core/auth/api';
import type { DirectExecutionDTO } from '@/features/executions/types';

export const executionsApi = {
  async getExecutions(status: string, contractId?: number) {
    const params = contractId ? { contractId } : {};
    const { data } = await api.get<any[]>(`/api/service-order/get-executions/${status}`, { params });
    return data;
  },

  async updateManagement(reservationManagementId: number, userId: string, teamId: number) {
    await api.put('/api/service-order/update-management', null, {
      params: { reservationManagementId, userId, teamId },
    });
  },

  async deleteManagement(status: string, reservationManagementId: number) {
    await api.delete('/api/service-order/delete-management', {
      params: { status, reservationManagementId },
    });
  },

  async getInstallationsWaitingValidation() {
    const { data } = await api.get<any[]>('/api/execution/get-installations-waiting-validation');
    return data;
  },

  async delegateDirectExecution(execution: DirectExecutionDTO) {
    await api.post('/api/execution/delegate-direct-execution', execution);
  },
};
