import { useQuery } from '@tanstack/react-query';
import { materialApi } from '@/features/stock/api/material-api';
import { stockKeys } from '@/features/stock/api/query-keys';

export function useCatalogue(generic = false) {
  return useQuery({
    queryKey: stockKeys.catalogue(generic),
    queryFn: () => materialApi.getCatalogue(generic),
  });
}
