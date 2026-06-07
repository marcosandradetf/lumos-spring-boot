import api from '@/core/auth/api';
import type { TeamModel } from '@/features/manage/types/manageTypes';

export const manageApi = {
  async getTeams() {
    const { data } = await api.get<TeamModel[]>('/api/teams/get-teams');
    return data;
  },

  async updateTeams(teams: TeamModel[]) {
    const { data } = await api.post<TeamModel[]>('/api/teams/update-teams', teams);
    return data;
  },

  async changePassword(currentPassword: string, newPassword: string) {
    await api.post('/api/user/change-password', { currentPassword, newPassword });
  },
};
