import {Injectable} from '@angular/core';
import {HttpClient, HttpParams} from '@angular/common/http';
import {environment} from '../../../../environments/environment';
import {
    ContractItemsResponse, ContractItemsResponseWithExecutionsSteps,
    ContractReferenceItemsDTO,
    ContractReferenceItemBaseManagementDTO,
    ContractReferenceItemManagementDTO,
    ContractResponse,
    CreateContractDTO,
    SaveContractReferenceItemBaseDTO,
    SaveContractReferenceItemLinksDTO
} from '../contract-models';

@Injectable({
    providedIn: 'root'
})
export class ContractService {
    private endpoint: string = environment.springboot + '/api/contracts';

    constructor(private http: HttpClient) {
    }

    createContract(contract: CreateContractDTO) {
        return this.http.post(this.endpoint + "/insert-contract", contract);
    }

    deleteById(contractId: number) {
        return this.http.post(this.endpoint + "/delete-by-id", contractId);
    }

    getContractReferenceItems() {
        return this.http.get<ContractReferenceItemsDTO[]>(this.endpoint + "/get-items");
    }

    getReferenceItemBaseManagement() {
        return this.http.get<ContractReferenceItemBaseManagementDTO[]>(this.endpoint + "/reference-items-management/base");
    }

    saveReferenceItemsBase(items: SaveContractReferenceItemBaseDTO[]) {
        return this.http.post<ContractReferenceItemBaseManagementDTO[]>(this.endpoint + "/reference-items-management/base", items);
    }

    getReferenceItemLinkManagement() {
        return this.http.get<ContractReferenceItemManagementDTO[]>(this.endpoint + "/reference-items-management/links");
    }

    saveReferenceItemLinks(items: SaveContractReferenceItemLinksDTO[]) {
        return this.http.post<ContractReferenceItemManagementDTO[]>(this.endpoint + "/reference-items-management/links", items);
    }

    checkIfHasPendingLink() {
        return this.http.get<any>(this.endpoint + "/reference-items-management/check-if-pending-link");
    }

    getAllContracts(filters: {
        contractor: string | null
        startDate: Date | null
        endDate: Date | null
        status: "ACTIVE" | "ARCHIVED" | null
    }) {
        return this.http.post<ContractResponse[]>(this.endpoint + "/get-AllContracts", filters);
    }

    getContractItems(contractId: number) {
        return this.http.get<ContractItemsResponse[]>(this.endpoint + "/get-contract-items/" + contractId);
    }

    getContractItemsWithExecutionsSteps(contractId: number) {
        return this.http.get<ContractItemsResponseWithExecutionsSteps[]>(this.endpoint + "/get-contract-items-with-executions-steps/" + contractId);
    }

    archiveById(contractId: number) {
        return this.http.post(this.endpoint + "/archive-by-id", contractId);
    }

    updateItems(items: ContractReferenceItemsDTO[], contractId: number) {
        const param = new HttpParams()
            .set("contractId", contractId);

        return this.http.put(this.endpoint + "/update-items", items, {params: param});
    }
}
