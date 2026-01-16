import {Injectable} from '@angular/core';
import {environment} from '../../environments/environment';
import {HttpClient} from '@angular/common/http';
import {DirectExecutionDTO, MaterialInStockDTO, ReserveRequest} from './executions.model';

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
    return this.http.get<ReserveRequest[]>(this.baseUrl + `/execution/get-reservations/${userUUID}`);
  }

  public findMaterialsByContractReference(contractReferenceItemId: number, teamId: number) {
    return this.http.get<MaterialInStockDTO[]>(this.baseUrl + `/stock/find-materials-by-contract-reference/${contractReferenceItemId}/${teamId}`);
  }

  reserveMaterialsForExecution(currentStreet: ReserveRequest, userUUID: string) {
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
