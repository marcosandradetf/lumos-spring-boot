export const preMeasurementKeys = {
  all: ['pre-measurement'] as const,
  list: (status: string) => [...preMeasurementKeys.all, status] as const,
  detail: (id: string) => [...preMeasurementKeys.all, 'detail', id] as const,
  balance: (id: string, preMeasurementId?: number) => [...preMeasurementKeys.all, 'balance', id, preMeasurementId] as const,
};
