import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  SubscriptionStatusResponse,
  RenewRequest,
  UpgradeRequest,
  PaymentRequest,
  SubscriptionMutationResponse,
  PaymentProcessResult
} from '../../models/billing.model';

@Injectable({
  providedIn: 'root'
})
export class BillingService {
  private apiUrl = '/api/billing';

  constructor(private http: HttpClient) { }

  /**
   * Get the current subscription status
   */
  getSubscriptionStatus(): Observable<SubscriptionStatusResponse> {
    return this.http.get<SubscriptionStatusResponse>(`${this.apiUrl}/subscription-status`);
  }

  /**
   * Renew or convert subscription to a new billing cycle
   */
  renewSubscription(request: RenewRequest): Observable<SubscriptionMutationResponse> {
    return this.http.post<SubscriptionMutationResponse>(`${this.apiUrl}/renew`, request);
  }

  /**
   * Upgrade to a different subscription plan
   */
  upgradeSubscription(request: UpgradeRequest): Observable<SubscriptionMutationResponse> {
    return this.http.post<SubscriptionMutationResponse>(`${this.apiUrl}/upgrade`, request);
  }

  /**
   * Process a payment for subscription
   */
  processPayment(request: PaymentRequest): Observable<PaymentProcessResult> {
    return this.http.post<PaymentProcessResult>(`${this.apiUrl}/payment`, request);
  }
}
