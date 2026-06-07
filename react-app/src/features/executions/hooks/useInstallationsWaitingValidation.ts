import { useQuery } from '@tanstack/react-query';
import { executionsApi } from '@/features/executions/api/executionsApi';
import { executionKeys } from '@/features/executions/hooks/queryKeys';

export function useInstallationsWaitingValidation() {
  return useQuery({
    queryKey: executionKeys.waitingValidation(),
    queryFn: executionsApi.getInstallationsWaitingValidation,
  });
}
