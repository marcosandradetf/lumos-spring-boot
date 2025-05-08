import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {environment} from '../../../environments/environment';
import {ContractResponse} from '../contract-models';

@Injectable({
  providedIn: 'root'
})
export class ContractService {
  private endpoint: string = environment.springboot + '/api/contracts';

  constructor(private http: HttpClient) {
  }

  createContract(contract: {
    number: string,
    contractor: string,
    address: string,
    phone: string,
    cnpj: string,
    unifyServices: boolean;
    noticeFile: string;
    contractFile: string;
    userUUID: string;
    items: {
      contractReferenceItemId: number;
      description: string;
      completeDescription: string;
      type: string;
      linking: string;
      itemDependency: string;
      quantity: number;
      price: string;
    }[]
  }) {
    return this.http.post(this.endpoint + "/insert-contract", contract);
  }

  getItems() {
    return this.http.get<{
      contractReferenceItemId: number;
      description: string;
      completeDescription: string;
      type: string;
      linking: string;
      itemDependency: string;
      quantity: number;
      price: string;
    }[]>(this.endpoint + "/get-items");
  }

  getAllContracts() {
    return this.http.get<ContractResponse[]>(this.endpoint + "/getAll-contracts");
  }

}
