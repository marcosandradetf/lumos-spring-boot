import { Injectable } from '@angular/core';
import {Contract} from '../contract-response.dto';
import {HttpClient, HttpParams} from '@angular/common/http';
import {MaterialResponse} from '../../models/material-response.dto';
import {ItemRequest} from '../itens-request.dto';
import {environment} from '../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class ContractService {
  private endpoint:string = environment.springboot + '/api';
  sidebarLinks = [
    { title: 'Dashboard', path: '/contratos/dashboard', id: 'opt1' },
    { title: 'Criar novo', path: '/contratos/criar', id: 'opt2' },
    { title: 'Executar', path: '/estoque/importar', id: 'opt3' },
    { title: 'Sugestão de Compra', path: '/estoque/sugestao', id: 'opt4' }
  ];

  constructor(private http: HttpClient) { }

  createContract(contract: Contract) {
    return this.http.post(this.endpoint + "/contrato", contract);
  }

  getAllItens(page: string, size: string){
    let params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<{content: any[], totalPages: number, currentPage: number }>(this.endpoint + "/material", { params });
  }

  getSidebarLinks() {
    return this.sidebarLinks;
  }

}
