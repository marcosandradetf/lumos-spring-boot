import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import {BehaviorSubject, Observable} from 'rxjs';
import { MaterialResponse } from '../material-response.dto';
import {Group} from '../../../core/models/grupo.model';
import {Type} from '../../../core/models/tipo.model';
import {Company} from '../../../core/models/empresa.model';
import {AuthService} from '../../../core/auth/auth.service';
import {StockMovementDTO} from '../stock-movement.dto';
import {SupplierDTO} from '../supplier.dto';


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

  postSupplier(supplier: SupplierDTO) {
    return this.http.post(this.endpoint + '/stock/create-supplier', supplier);
  }

  stockMovement(stockMovement: StockMovementDTO[]) {
    return this.http.post(this.endpoint + '/stock/stock-movement', stockMovement);
  }

  createSuppliers(sendSuppliers: any) {
    return this.http.post(this.endpoint + "/stock/create-supplier", sendSuppliers);
  }
}
