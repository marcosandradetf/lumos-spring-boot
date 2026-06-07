export interface SubscriptionStatusResponse {
  planName: string | null;
  status: string | null;
  trialEndsAt: string | null;
  accessAllowed: boolean;
  message: string | null;
}

export type BillingCycle = 'MONTHLY' | 'YEARLY';

export interface RenewRequest {
  planName?: string | null;
  billingCycle: BillingCycle;
}

export interface UpgradeRequest {
  targetPlanName: string;
}

export interface PaymentRequest {
  billingEmail?: string | null;
  invoiceId?: string | null;
}

export interface SubscriptionMutationResponse {
  subscriptionId: string | null;
  planName: string | null;
  status: string | null;
  message: string;
}

export interface PaymentProcessResult {
  paymentProviderCustomerId: string;
  paymentReference: string;
  status: string;
  invoiceId: string | null;
}
