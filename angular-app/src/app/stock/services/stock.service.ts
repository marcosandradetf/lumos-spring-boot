import { Injectable } from '@angular/core';
import {HttpClient, HttpParams} from '@angular/common/http';
import {BehaviorSubject, Observable} from 'rxjs';

import {StockMovementDTO} from '../../models/stock-movement.dto';
import {SupplierDTO} from '../../models/supplier.dto';
import {StockMovementResponse} from '../../models/stock-movement-response.dto';
import {AuthService} from '../../core/auth/auth.service';
import {Type} from '../../models/tipo.model';
import {Group} from '../../models/grupo.model';
import {Company} from '../../models/empresa.model';
import {Deposit} from '../../models/almoxarifado.model';
import {environment} from '../../../environments/environment';
import {DepositByStockist, StockistModel} from '../../executions/executions.model';


@Injectable({
  providedIn: 'root'
})
export class StockService {
  private endpoint = environment.springboot + '/api';

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
    return this.http.get<Deposit[]>(`${this.endpoint}/deposit`);
  }

  getTruckDeposits() {
    return this.http.get<Deposit[]>(`${this.endpoint}/deposit/truck-deposits`);
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
  deleteType(id: number) {
    return this.http.delete<Type[]>(`${this.endpoint}/type/${id}/delete`);
  }
  updateType(typeId: number, type: any) {
    return this.http.put<Type[]>(`${this.endpoint}/type/${typeId}/update`, type);
  }

  insertGroup(groupName: string) {
    return this.http.post<Group[]>(`${this.endpoint}/group/insert`, groupName);
  }
  updateGroup(id:number, groupName: string) {
    return this.http.put<Group[]>(`${this.endpoint}/group/${id}/update`, groupName);
  }
  deleteGroup(groupId: number){
    return this.http.delete<Group[]>(`${this.endpoint}/group/${groupId}/delete`);
  }

  insertDeposit(deposit: any) {
    return this.http.post<Deposit[]>(`${this.endpoint}/deposit/insert`, deposit);
  }
  updateDeposit(id:number, deposit: any) {
    return this.http.put<Deposit[]>(`${this.endpoint}/deposit/${id}/update`, deposit);
  }
  deleteDeposit(depositId: number) {
    return this.http.delete<Deposit[]>(`${this.endpoint}/deposit/${depositId}/delete`);
  }

  getStockists() {
    return this.http.get<StockistModel[]>(`${this.endpoint}/deposit/get-stockists`);
  }

  getDepositsByStockist(userId: string) {
    const params = new HttpParams()
      .set('userId', userId);
    return this.http.get<DepositByStockist[]>(`${this.endpoint}/deposit/get-deposits-by-stockist`, {params: params});
  }
}
