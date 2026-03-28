// Billing Models & DTOs

export interface SubscriptionStatusResponse {
  planName: string | null;
  status: string | null;
  trialEndsAt: string | null;
  accessAllowed: boolean;
  message: string | null;
}

export interface RenewRequest {
  planName?: string | null;
  billingCycle: 'MONTHLY' | 'YEARLY';
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

export interface ErrorResponse {
  code: string;
  message: string;
}

export enum SubscriptionStatus {
  TRIAL = 'TRIAL',
  ACTIVE = 'ACTIVE',
  PAST_DUE = 'PAST_DUE',
  CANCELED = 'CANCELED',
  EXPIRED = 'EXPIRED'
}

export enum BillingCycle {
  MONTHLY = 'MONTHLY',
  YEARLY = 'YEARLY'
}

export enum BillingErrorCode {
  TRIAL_EXPIRED = 'TRIAL_EXPIRED',
  SUBSCRIPTION_EXPIRED = 'SUBSCRIPTION_EXPIRED',
  SUBSCRIPTION_CANCELED = 'SUBSCRIPTION_CANCELED'
}
