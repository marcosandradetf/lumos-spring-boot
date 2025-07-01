import {Injectable} from '@angular/core';
import {HttpClient, HttpParams} from '@angular/common/http';
import {ReservationsByCaseDtoResponse} from './reservation.models';
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
    return this.http.get<ReservationsByCaseDtoResponse[]>(this.baseUrl + "/execution/get-reservations-by-status-and-stockist", {params});
  }

  reply(replies: {
    approved: { reserveId: number }[],
    rejected: { reserveId: number }[],
  }) {
    return this.http.post(this.baseUrl + "/reservation/reply", replies);
  }

  markAsCollected(reservationIds: number[]) {
    return this.http.post<void>(
      this.baseUrl + "/reservation/mark-as-collected",
      reservationIds
    );
  }

}
