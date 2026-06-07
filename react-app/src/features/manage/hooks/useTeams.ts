import { useQuery } from '@tanstack/react-query';
import { manageApi } from '@/features/manage/api/manageApi';
import { manageKeys } from '@/features/manage/api/queryKeys';

export function useTeams() {
  return useQuery({
    queryKey: manageKeys.teams(),
    queryFn: manageApi.getTeams,
  });
}
