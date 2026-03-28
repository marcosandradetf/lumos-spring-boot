import { TestBed } from '@angular/core/testing';
import { BillingService } from '../services/billing.service';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { RenewRequest, UpgradeRequest } from '../../models/billing.model';

describe('BillingService', () => {
  let service: BillingService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [BillingService]
    });
    service = TestBed.inject(BillingService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should call getSubscriptionStatus endpoint', () => {
    const mockResponse = {
      planName: 'Pro',
      status: 'ACTIVE',
      trialEndsAt: null,
      accessAllowed: true,
      message: null
    };

    service.getSubscriptionStatus().subscribe(result => {
      expect(result).toEqual(mockResponse);
    });

    const req = httpMock.expectOne('/api/billing/subscription-status');
    expect(req.request.method).toBe('GET');
    req.flush(mockResponse);
  });

  it('should call renewSubscription endpoint', () => {
    const request: RenewRequest = {
      billingCycle: 'MONTHLY',
      planName: null
    };

    const mockResponse = {
      subscriptionId: '123',
      planName: 'Pro',
      status: 'ACTIVE',
      message: 'Assinatura renovada com sucesso.'
    };

    service.renewSubscription(request).subscribe(result => {
      expect(result).toEqual(mockResponse);
    });

    const req = httpMock.expectOne('/api/billing/renew');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(request);
    req.flush(mockResponse);
  });

  it('should call upgradeSubscription endpoint', () => {
    const request: UpgradeRequest = {
      targetPlanName: 'Enterprise'
    };

    const mockResponse = {
      subscriptionId: '123',
      planName: 'Enterprise',
      status: 'ACTIVE',
      message: 'Plano atualizado com sucesso.'
    };

    service.upgradeSubscription(request).subscribe(result => {
      expect(result).toEqual(mockResponse);
    });

    const req = httpMock.expectOne('/api/billing/upgrade');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(request);
    req.flush(mockResponse);
  });

  it('should call processPayment endpoint', () => {
    const mockResponse = {
      paymentProviderCustomerId: 'cus_123',
      paymentReference: 'pi_456',
      status: 'COMPLETED',
      invoiceId: null
    };

    service.processPayment({}).subscribe(result => {
      expect(result).toEqual(mockResponse);
    });

    const req = httpMock.expectOne('/api/billing/payment');
    expect(req.request.method).toBe('POST');
    req.flush(mockResponse);
  });
});
