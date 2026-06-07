import api from '@/core/auth/api';
import type { ActivationCodeResponse, ManagedUser } from '@/features/manage/types/manageTypes';

export const usersApi = {
  async getUsers() {
    const { data } = await api.get<ManagedUser[]>('/api/user/get-users');
    return data;
  },

  async generateActivationCode(userId: string) {
    const { data } = await api.post<ActivationCodeResponse>(`/api/user/${userId}/generate-activation-code`, {});
    return data;
  },

  async resetActivation(userId: string) {
    const { data } = await api.post<ActivationCodeResponse>(`/api/user/${userId}/reset-activation`, {});
    return data;
  },
};
