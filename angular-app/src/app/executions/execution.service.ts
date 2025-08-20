import {Injectable} from '@angular/core';
import {environment} from '../../environments/environment';
import {HttpClient} from '@angular/common/http';
import * as http from 'node:http';
import {DirectExecutionDTO, MaterialInStockDTO, ReserveDTOResponse, ReserveStreetDTOResponse} from './executions.model';

@Injectable({
  providedIn: 'root'
})
export class ExecutionService {
  private baseUrl = environment.springboot + "/api";

  constructor(private http: HttpClient) {
  }

  public getStockAvailable() {
    return this.http.get<
      {
        streetId: number;
        materialsInStock: {
          materialId: number;
          materialName: string;
          deposit: string;
          availableQuantity: number
        }[];
        materialsInTruck: {
          materialId: number;
          materialName: string;
          deposit: string;
          availableQuantity: number
        }[];
      }[]
    >(this.baseUrl + '/execution/get-available-stock');
  }

  public getStockAvailableForStreet() {
    return this.http.get<
      {
        streetId: number;
        materialsInStock: {
          materialId: number;
          materialName: string;
          deposit: string;
          availableQuantity: number
        }[];
        materialsInTruck: {
          materialId: number;
          materialName: string;
          deposit: string;
          availableQuantity: number
        }[];
      }
    >(this.baseUrl + '/api/execution/get-available-stock');
  }

  public getPendingReservesForStockist(userUUID: string) {
    return this.http.get<ReserveDTOResponse[]>(this.baseUrl + `/execution/get-reservations/${userUUID}`);
  }

  public getStockMaterialForLinking(linking: string, type: string, teamId: number) {
    return this.http.get<MaterialInStockDTO[]>(this.baseUrl + `/execution/get-stock-materials/${linking}/${type}/${teamId}`);
  }

  reserveMaterialsForExecution(currentStreet: ReserveStreetDTOResponse, userUUID: string) {
    return this.http.post(this.baseUrl + `/execution/reserve-materials-for-execution/${userUUID}`, currentStreet);
  }

  delegateDirectExecution(execution: DirectExecutionDTO) {
    return this.http.post(this.baseUrl + "/execution/delegate-direct-execution", execution);
  }

  cancelStep(currentIds: number[], type: string | null) {
    return this.http.post(this.baseUrl + "/execution/cancel-step", {
      currentIds: currentIds,
      type: type
    });
  }

}
