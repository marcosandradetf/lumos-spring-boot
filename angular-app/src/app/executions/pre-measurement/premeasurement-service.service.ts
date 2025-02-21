import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {User} from '../../models/user.model';
import {Observable} from 'rxjs';
import * as http from 'node:http';
import {Deposit} from '../../models/almoxarifado.model';
import {environment} from '../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class PreMeasurementService {
  private endpoint = environment.springboot + '/api/execution';

  constructor(private http: HttpClient) {
  }

  getPreMeasurements(): Observable<{ [cityName: string]: [string, string] }> {
    return this.http.get<{ [cityName: string]: [string, string] }>(`${this.endpoint + '/pre-measurement/get-cities'}`);
  }

  getDeposits() {
    return this.http.get<Deposit[]>(`${environment.springboot + '/api/deposit'}`);
  }

  getItemsByDeposit(depositId: string): Observable<{ materialId: string; materialName: string; materialQuantity: string }[]> {
    return this.http.get<{ materialId: string; materialName: string; materialQuantity: string }[]>(`${this.endpoint}/itens/${depositId}`);
  }

  public getTeams() {
    return this.http.get<{
      idTeam: string,
      teamName: string,
      userId: string,
      username: string,
      UFName: string,
      cityName: string,
      regionName: string,
      sel: boolean,
    }[]>(`${this.endpoint}/get-teams`);
  }

  public getUsers() {
    return this.http.get<{
      userId: string,
      username: string,
    }[]>(`${environment.springboot}/api/user/get-users`);
  }

  public updateTeams(user: {
    idTeam: string,
    teamName: string,
    userId: string,
    username: string,
    UFName: string,
    cityName: string,
    regionName: string,
    sel: boolean,
  }[]) {
    return this.http.post<{
      idTeam: string,
      teamName: string,
      userId: string,
      username: string,
      UFName: string,
      cityName: string,
      regionName: string,
      sel: boolean,
    }[]>(`${this.endpoint}/update-teams`, user);
  }

  public insertTeams(user: {
    idTeam: string,
    teamName: string,
    userId: string,
    username: string,
    UFName: string,
    cityName: string,
    regionName: string,
    sel: boolean,
  }[]) {
    return this.http.post<{
      idTeam: string,
      teamName: string,
      userId: string,
      username: string,
      UFName: string,
      cityName: string,
      regionName: string,
      sel: boolean,
    }[]>(`${this.endpoint}/post-teams`, user);
  }




}
