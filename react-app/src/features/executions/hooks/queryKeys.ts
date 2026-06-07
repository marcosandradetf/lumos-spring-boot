export const executionKeys = {
  all: ['executions'] as const,
  byStatus: (status: string) => [...executionKeys.all, status] as const,
  waitingValidation: () => [...executionKeys.all, 'waiting-validation'] as const,
};
