import {Injectable} from '@angular/core';
import {environment} from '../../environments/environment';
import {HttpClient, HttpParams} from '@angular/common/http';
import {DirectExecutionDTO, MaterialInStockDTO, ReserveRequest} from './executions.model';

@Injectable({
    providedIn: 'root'
})
export class ExecutionService {
    private baseUrl = environment.springboot + "/api";

    constructor(private http: HttpClient) {
    }


    public findMaterialsByContractReference(contractReferenceItemId: number, teamId: number) {
        return this.http.get<MaterialInStockDTO[]>(this.baseUrl + `/stock/find-materials-by-contract-reference/${contractReferenceItemId}/${teamId}`);
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

    // service orders
    public getPendingReservesForStockist() {
        return this.http.get<ReserveRequest[]>(`${this.baseUrl }/service-order/get-reservations`);
    }

    reserveMaterialsForExecution(currentStreet: ReserveRequest) {
        return this.http.post(this.baseUrl + `/service-order/reserve-materials-for-execution`, currentStreet);
    }

    getExecutions(status: string, contractId: number | null = null) {
        if(contractId) {
            const param = new HttpParams()
                .set('contractId', contractId);

            return this.http.get<any[]>(`${this.baseUrl}/service-order/get-executions/${status}`, {params: param});
        }
        return this.http.get<any[]>(`${this.baseUrl}/service-order/get-executions/${status}`);
    }

    updateManagement(
        reservationManagementId: number,
        userId: string,
        teamId: number,
    ) {
        const param = new HttpParams()
            .set('reservationManagementId', reservationManagementId)
            .set('userId', userId)
            .set('teamId', teamId);
        return this.http.put(`${this.baseUrl}/service-order/update-management`, {params: param});
    }

    deleteManagement(
        status: string,
        reservationManagementId: number,
    ) {
        const param = new HttpParams()
            .set('status', status)
            .set('reservationManagementId', reservationManagementId);
        return this.http.delete(`${this.baseUrl}/service-order/delete-management`, {params: param});
    }
}
