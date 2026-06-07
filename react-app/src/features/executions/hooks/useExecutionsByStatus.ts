import { useQuery } from '@tanstack/react-query';
import { executionsApi } from '@/features/executions/api/executionsApi';
import { executionKeys } from '@/features/executions/hooks/queryKeys';

export function useExecutionsByStatus(status: string) {
  return useQuery({
    queryKey: executionKeys.byStatus(status),
    queryFn: () => executionsApi.getExecutions(status),
    enabled: !!status,
  });
}
