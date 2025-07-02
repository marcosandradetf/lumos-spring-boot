import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {environment} from '../../../environments/environment';
import {
  ContractItemsResponse, ContractItemsResponseWithExecutionsSteps,
  ContractReferenceItemsDTO,
  ContractResponse,
  CreateContractDTO
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

  getAllContracts() {
    return this.http.get<ContractResponse[]>(this.endpoint + "/get-AllContracts");
  }

  getContractItems(contractId: number) {
    return this.http.get<ContractItemsResponse[]>(this.endpoint + "/get-contract-items/" + contractId);
  }

  getContractItemsWithExecutionsSteps(contractId: number) {
    return this.http.get<ContractItemsResponseWithExecutionsSteps[]>(this.endpoint + "/get-contract-items-with-executions-steps/" + contractId);
  }



}
