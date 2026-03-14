import {Injectable} from '@angular/core';
import {environment} from '../../environments/environment';
import {HttpClient, HttpParams} from '@angular/common/http';
import {DirectExecutionDTO, MaterialInStockDTO, ReserveRequest} from './executions.model';
import {
    Contract,
    DirectExecution,
    DirectExecutionStreetItem
} from '../contract/validate-execution/execution-no-work-service.models';
import {Observable} from 'rxjs';

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
        return this.http.get<ReserveRequest[]>(`${this.baseUrl}/service-order/get-reservations`);
    }

    reserveMaterialsForExecution(currentStreet: ReserveRequest) {
        return this.http.post(this.baseUrl + `/service-order/reserve-materials-for-execution`, currentStreet);
    }

    getExecutions(status: string, contractId: number | null = null) {
        if (contractId) {
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

    getExecutionWaitingValidation(id: number): Observable<DirectExecution> {
        return this.http.get<DirectExecution>(
            `${this.baseUrl}/execution/${id}/waiting-validation`
        );
    }

    getContractItemsForLink(contractId: number): Observable<Contract> {
        return this.http.get<Contract>(
            `${this.baseUrl}/execution/contract/${contractId}/items-for-link`
        );
    }

    preValidateExecution(payload: {
        directExecutionId: number;
        contractId: number;
        items: { directExecutionStreetItemId: number; contractItemId: number | null | undefined }[]
    }) {
        return this.http.post<DirectExecutionStreetItem[]>(this.baseUrl + `/direct-execution/pre-validate-execution`, payload);
    }

    validateExecution(directExecutionId: number) {
        return this.http.put(`${this.baseUrl}/direct-execution/validate-execution/${directExecutionId}`, {});
    }

    deleteItem(streetItemId: number) {
        return this.http.delete(`${this.baseUrl}/direct-execution/delete-item/${streetItemId}`)
    }

    cancelValidation(directExecutionId: number, streetItemIds: number[]) {
        const params = new HttpParams()
            .set('executionId', directExecutionId)
            .set('streetItemsIds', streetItemIds.join(','))

        return this.http.put(`${this.baseUrl}/direct-execution/cancel-validation`, params);
    }

    getInstallationsWaitingValidation() {
        return this.http.get<any[]>(`${this.baseUrl}/execution/get-installations-waiting-validation`)
    }
}
