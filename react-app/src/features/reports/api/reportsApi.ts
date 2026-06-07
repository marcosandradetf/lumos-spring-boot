import api from '@/core/auth/api';

export const reportsApi = {
  async getFinishedMaintenances(startDate: Date, endDate: Date) {
    const { data } = await api.get<unknown[]>('/api/maintenance/get-finished', {
      params: { startDate: startDate.toISOString(), endDate: endDate.toISOString() },
    });
    return data;
  },

  async getFinishedInstallations(startDate: Date, endDate: Date) {
    const { data } = await api.get<unknown[]>('/api/execution/get-finished', {
      params: { startDate: startDate.toISOString(), endDate: endDate.toISOString() },
    });
    return data;
  },

  async generateMaintenancePdf(maintenanceId: string, type: string) {
    const { data } = await api.get<Blob>(
      `/api/maintenance/generate-report/${type}/${maintenanceId}`,
      { responseType: 'blob' },
    );
    return data;
  },

  async generateInstallationPdf(installationId: number, installationType: string, type: string) {
    const { data } = await api.get<Blob>(
      `/api/execution/generate-report/${type}/${installationId}/${installationType}`,
      { responseType: 'blob' },
    );
    return data;
  },

  async getContracts() {
    const { data } = await api.get<unknown[]>('/api/report/execution/get-contracts');
    return data;
  },

  async getExecutionReport(filters: Record<string, unknown>) {
    const { data } = await api.post<unknown[]>('/api/report/execution/generate-report', filters);
    return data;
  },

  async generateExecutionGroupedReport(filters: Record<string, unknown>) {
    const { data } = await api.post<Blob>(
      '/api/report/execution/generate-report',
      filters,
      { responseType: 'blob' },
    );
    return data;
  },

  async generateOperationalReport(filters: Record<string, unknown>) {
    const { data } = await api.post<Blob>(
      '/api/report/execution/generate-operational-report', filters,
      { responseType: 'blob' },
    );
    return data;
  },

  async generateMaterialReservationReport(filters: Record<string, unknown>) {
    const { data } = await api.post<Blob>(
      '/api/report/stock/generate-material-reservation-report', filters,
      { responseType: 'blob' },
    );
    return data;
  },
};
