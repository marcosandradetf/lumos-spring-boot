import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  CheckBalanceRequest,
  ListPreMeasurementRequest,
  PreMeasurementDTO,
  PreMeasurementResponseDTO
} from '../pre-measurement-models';
import { ContractAndItemsResponse } from '../../contract/contract-models';

@Injectable({
  providedIn: 'root'
})
export class PreMeasurementService {
  private endpoint = environment.springboot + '/api/execution';

  constructor(private http: HttpClient) {
  }

  getPreMeasurement(preMeasurementId: string) {
    return this.http.get<PreMeasurementResponseDTO>(`${this.endpoint + `/get-pre-measurement/${preMeasurementId}`}`);
  }

  getPreMeasurements(status: string): Observable<ListPreMeasurementRequest[]> {
    return this.http.get<ListPreMeasurementRequest[]>(`${this.endpoint + `/get-pre-measurements/${status}`}`);
  }

  getContract(contractId: number) {
    return this.http.get<ContractAndItemsResponse>(`${environment.springboot + `/api/contracts/get-contract/${contractId}`}`);
  }

  markAsAvailable(preMeasurementId: number) {
    return this.http.post(environment.springboot + `/api/pre-measurement/mark-as-available/${preMeasurementId}`, null);
  }

  sendModifications(modifications: {
    cancelledStreets: { streetId: number }[];
    cancelledItems: { streetId: number; itemId: number }[];
    changedItems: { streetId: number; itemId: number; quantity: number }[]
  }) {
    return this.http.post<{
      message: string
    }>(environment.springboot + "/api/pre-measurement/send-modifications", modifications);
  }

  getStockAvailable(preMeasurementId: number, teamId: number) {
    const params = new HttpParams()
      .set('preMeasurementId', preMeasurementId)
      .set('teamId', teamId);

    return this.http.get<
      {
        streetId: number;
        materialsInStock: {
          materialId: number;
          materialName: string;
          materialPower: string;
          materialAmp: string;
          materialLength: string;
          deposit: string;
          itemQuantity: number;
          availableQuantity: number
        }[];
        materialsInTruck: {
          materialId: number;
          materialName: string;
          materialPower: string;
          materialAmp: string;
          materialLength: string;
          deposit: string;
          itemQuantity: number;
          availableQuantity: number
        }[];
      }[]>(environment.springboot + "/api/execution/get-available-stock", { params });
  }

  importData(preMeasurement: PreMeasurementDTO, userUUID: string) {
    const header = new HttpHeaders({ 'UUID': userUUID });
    return this.http.post<{ message: string }>(environment.springboot + "/api/pre-measurement/import",
      preMeasurement,
      { headers: header }
    );
  }

  delegateExecution(delegateDTO: {
    preMeasurementId: number;
    description: string,
    stockistId: string,
    stockistName: string,
    stockistPhone: string,
    stockistDepositName: string,
    stockistDepositAddress: string,
    preMeasurementStep: number,
    teamId: number;
    comment: string;

    street: {
      preMeasurementStreetId: number;
      teamName: string,
      truckDepositName: string;
      prioritized: boolean;
    }[]
  }
  ) {
    return this.http.post(environment.springboot + "/api/execution/delegate", delegateDTO);
  }

  checkBalance(preMeasurementId: number) {
    return this.http.get<CheckBalanceRequest[]>(environment.springboot + '/api/execution/check-balance-pre-measurement/' + preMeasurementId);
  }
}
