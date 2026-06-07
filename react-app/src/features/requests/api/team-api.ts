import api from '@/core/auth/api';

export const teamApi = {
  async sendStockNotification(description: string, notificationCode: string, material: string) {
    await api.post('/api/teams/send-stock-notification', null, {
      params: {
        description,
        notificationCode,
        materialName: material,
      },
    });
  },
};
