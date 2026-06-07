import api from '@/core/auth/api';

export interface DashboardMetric {
  label: string;
  value: number;
  description: string;
  classification?: string;
  routerLink: string | null;
  queryParams: Record<string, string> | null;
}

export type ExecutionType = 'INSTALLATION' | 'MAINTENANCE';
export type ExecutionStatus = 'IN_PROGRESS' | 'FINISHED' | 'BLOCKED';

export interface GeoExecution {
  id: string;
  executionId: string;
  executionType: string | null;
  title: string;
  type: ExecutionType;
  status: ExecutionStatus;
  lat: number;
  lng: number;
  address: string;
  finishedAt: string | null;
  teamId: number;
  teamName: string;
  photoUri: string | null;
  pointNumber: number | null;
}

export const dashboardApi = {
  async getMetrics() {
    const { data } = await api.get<DashboardMetric[]>('/api/dashboard/metrics/get-metrics');
    return data;
  },

  async getExecutions() {
    const { data } = await api.get<GeoExecution[]>('/api/dashboard/map/get-executions');
    return data;
  },

  async getPhoto(uri: string) {
    const { data } = await api.get<Blob>('/api/s3/get-photo', {
      params: { uri },
      responseType: 'blob',
    });
    return data;
  },
};
