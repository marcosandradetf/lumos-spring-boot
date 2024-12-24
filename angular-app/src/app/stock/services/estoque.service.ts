import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import {BehaviorSubject, Observable} from 'rxjs';

import {StockMovementDTO} from '../stock-movement.dto';
import {SupplierDTO} from '../supplier.dto';
import {StockMovementResponse} from '../stock-movement-response.dto';
import {AuthService} from '../../core/auth/auth.service';
import {Type} from '../../core/models/tipo.model';
import {Group} from '../../core/models/grupo.model';
import {Company} from '../../core/models/empresa.model';


@Injectable({
  providedIn: 'root'
})
export class EstoqueService {
  private endpoint = 'http://localhost:8080/api';

  private onPathSubject = new BehaviorSubject<string>(''); // Inicializa o caminho
  onPath$ = this.onPathSubject.asObservable(); // Observable para se inscrever

  constructor(private http: HttpClient, private authService: AuthService) { }

  getTypes() {
    return this.http.get<Type[]>(`${this.endpoint}/type`);
  }

  getGroups() {
    return this.http.get<Group[]>(`${this.endpoint}/group`);
  }

  getCompanies() {
    return this.http.get<Company[]>(`${this.endpoint}/company`);
  }

  getDeposits() {
    return this.http.get<any[]>(`${this.endpoint}/deposit`);
  }

  getSuppliers() {
    return this.http.get<SupplierDTO[]>(`${this.endpoint}/stock/get-suppliers`);
  }

  stockMovement(stockMovement: StockMovementDTO[]) {
    return this.http.post(this.endpoint + '/stock/stock-movement/create', stockMovement, { responseType: "text" });
  }

  getStockMovement() {
    return this.http.get<StockMovementResponse[]>(`${this.endpoint}/stock/stock-movement/get`);
  }

  approveStockMovement(id: number) {
    return this.http.post(this.endpoint + '/stock/stock-movement/approve/'+ id, null, { responseType: "text" });
  }

  rejectStockMovement(id: number) {
    return this.http.post(this.endpoint + '/stock/stock-movement/reject/' + id, null, { responseType: "text" });
  }


  createSuppliers(sendSuppliers: any) {
    return this.http.post(this.endpoint + "/stock/create-supplier", sendSuppliers);
  }

  getStockMovementApproved() {
    return this.http.get<StockMovementResponse[]>(`${this.endpoint}/stock/stock-movement/get-approved`);
  }

  insertType(type: any) {
    return this.http.post<Type[]>(`${this.endpoint}/type/insert`, type);
  }
  deleteType(type: any) {
    return this.http.post(`${this.endpoint}/type/delete`, type);
  }
  updateType(type: any) {
    return this.http.post(`${this.endpoint}/type/update`, type);
  }

  insertGroup(group: any) {
    return this.http.post(`${this.endpoint}/type/insert`, group);
  }
  updateGroup(group: any) {
    return this.http.post(`${this.endpoint}/type/update`, group);
  }
  deleteGroup(group: any) {
    return this.http.post(`${this.endpoint}/type/delete`, group);
  }

  insertDeposit(deposit: any) {
    return this.http.post(`${this.endpoint}/deposit/insert`, deposit);
  }
  updateDeposit(deposit: any) {
    return this.http.post(`${this.endpoint}/deposit/update`, deposit);
  }
  deleteDeposit(deposit: any) {
    return this.http.post(`${this.endpoint}/deposit/delete`, deposit);
  }

}
