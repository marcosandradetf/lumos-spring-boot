import {Injectable} from '@angular/core';
import {HttpClient, HttpParams} from '@angular/common/http';
import {OrdersByCaseResponse} from './reservation.models';
import {environment} from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class RequestService {
  private baseUrl = environment.springboot + "/api";

  constructor(private http: HttpClient) {
  }

  getReservation(depositId: number, status: string) {
    const params = new HttpParams()
      .set('depositId', depositId)
      .set('status', status);
    return this.http.get<OrdersByCaseResponse[]>(
      this.baseUrl + "/order/get-orders-by-status-and-stockist", {params}
    );
  }

  reply(
    replies: {
      approved: { reserveId: number | null, order: { orderId: string | null, materialId: number, quantity: string | null } }[],
      rejected: { reserveId: number | null, order: { orderId: string | null, materialId: number, quantity: string | null } }[],
    }
  ) {
    return this.http.post(this.baseUrl + "/order/reply", replies);
  }

  markAsCollected(orders:  { reserveId: number | null, order: { orderId: string | null, materialId: number, quantity: string | null } }[]) {
    return this.http.post<void>(
      this.baseUrl + "/order/mark-as-collected",
      orders
    );
  }

}
