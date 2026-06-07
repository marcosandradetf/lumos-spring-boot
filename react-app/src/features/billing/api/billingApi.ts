import api from '@/core/auth/api';
import type {
  PaymentProcessResult,
  PaymentRequest,
  RenewRequest,
  SubscriptionMutationResponse,
  SubscriptionStatusResponse,
  UpgradeRequest,
} from '@/features/billing/types/types';

export const billingApi = {
  async getSubscriptionStatus() {
    const { data } = await api.get<SubscriptionStatusResponse>('/api/billing/subscription-status');
    return data;
  },

  async renewSubscription(request: RenewRequest) {
    const { data } = await api.post<SubscriptionMutationResponse>('/api/billing/renew', request);
    return data;
  },

  async upgradeSubscription(request: UpgradeRequest) {
    const { data } = await api.post<SubscriptionMutationResponse>('/api/billing/upgrade', request);
    return data;
  },

  async processPayment(request: PaymentRequest) {
    const { data } = await api.post<PaymentProcessResult>('/api/billing/payment', request);
    return data;
  },
};
