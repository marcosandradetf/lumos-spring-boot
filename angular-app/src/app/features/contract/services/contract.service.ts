import { Injectable } from '@angular/core';
import {Contract} from '../contract-response.dto';
import {HttpClient} from '@angular/common/http';
import {MaterialResponse} from '../../estoque/material-response.dto';
import {ItemRequest} from '../itens-request.dto';

@Injectable({
  providedIn: 'root'
})
export class ContractService {
  private endpoint:string = 'http://localhost:8080/api';

  constructor(private http: HttpClient) { }

  createContract(contract: Contract) {
    return this.http.post(this.endpoint + "/contrato", contract);
  }

  getAllItens(){
    return this.http.get<ItemRequest[]>(this.endpoint + "/material");
  }

}
