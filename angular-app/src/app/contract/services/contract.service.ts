import {Injectable} from '@angular/core';
import {Contract} from '../contract-response.dto';
import {HttpClient, HttpParams} from '@angular/common/http';
import {MaterialResponse} from '../../models/material-response.dto';
import {ItemRequest} from '../itens-request.dto';
import {environment} from '../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class ContractService {
  private endpoint: string = environment.springboot + '/api/contracts';

  constructor(private http: HttpClient) {
  }

  createContract(contract:  {
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
  } ) {
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


}
