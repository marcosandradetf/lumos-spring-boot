import { useQuery } from '@tanstack/react-query';
import { stockApi } from '@/features/stock/api/stock-api';
import { stockKeys } from '@/features/stock/api/query-keys';

export function useGroups() {
  return useQuery({
    queryKey: stockKeys.groups(),
    queryFn: stockApi.getGroups,
  });
}
