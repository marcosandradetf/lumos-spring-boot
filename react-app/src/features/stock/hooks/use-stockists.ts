import { useQuery } from '@tanstack/react-query';
import { stockKeys } from '@/features/stock/api/query-keys';
import { stockistApi } from '../api/stockist-api';

export function useStockists() {
  return useQuery({
    queryKey: stockKeys.stockists(),
    queryFn: stockistApi.getStockists,
  });
}
