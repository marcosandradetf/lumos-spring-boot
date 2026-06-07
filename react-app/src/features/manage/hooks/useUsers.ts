import { useQuery } from '@tanstack/react-query';
import { usersApi } from '@/features/manage/api/usersApi';
import { manageKeys } from '@/features/manage/api/queryKeys';

export function useUsers() {
  return useQuery({
    queryKey: manageKeys.users(),
    queryFn: usersApi.getUsers,
  });
}
