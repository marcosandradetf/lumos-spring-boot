import {Injectable} from '@angular/core';
import {HttpClient, HttpParams} from '@angular/common/http';
import {User} from '../../models/user.model';
import {Observable} from 'rxjs';
import * as http from 'node:http';
import {Deposit} from '../../models/almoxarifado.model';
import {environment} from '../../../environments/environment';
import {PreMeasurementModel} from '../../models/pre-measurement.model';

@Injectable({
  providedIn: 'root'
})
export class PreMeasurementService {
  private endpoint = environment.springboot + '/api/execution';

  constructor(private http: HttpClient) {
  }

  getPreMeasurement(preMeasurementId: string) {
    return this.http.get<PreMeasurementModel>(`${this.endpoint + `/get-pre-measurement/${preMeasurementId}`}`);
  }

  getPreMeasurements(status: string): Observable<PreMeasurementModel[]> {
    return this.http.get<PreMeasurementModel[]>(`${this.endpoint + `/get-pre-measurements/${status}`}`);
  }

  getContract(contractId: number) {
    return this.http.get<
      {
        contractId: number,
        contractNumber: string,
        contractor: string,
        cnpj: string,
        phone: string,
        address: string,
        contractFile: string,
        createdBy: string,
        createdAt: string,
        items: {
          number: number,
          contractItemId: number,
          description: string,
          unitPrice: string,
          contractedQuantity: number,
          linking: string,
        }[]
      }
    >(`${environment.springboot + `/api/contracts/get-contract/${contractId}`}`);
  }

  evolveStatus(preMeasurementId: number) {
    return this.http.post(environment.springboot + "/api/pre-measurement/evolve-status/" + preMeasurementId, null);
  }

  sendModifications(modifications: {
    cancelledStreets: { streetId: number }[];
    cancelledItems: { streetId: number; itemId: number }[];
    changedItems: { streetId: number; itemId: number; quantity: number }[]
  }) {
    return this.http.post<{message: string}>(environment.springboot + "/api/pre-measurement/send-modifications", modifications);
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
}
