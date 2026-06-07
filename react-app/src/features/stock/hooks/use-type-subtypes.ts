import { useQuery } from '@tanstack/react-query';
import { stockApi } from '@/features/stock/api/stock-api';
import { stockKeys } from '@/features/stock/api/query-keys';

export function useTypeSubtypes() {
  return useQuery({
    queryKey: stockKeys.typesSubtype(),
    queryFn: stockApi.findAllTypeSubtype,
  });
}
