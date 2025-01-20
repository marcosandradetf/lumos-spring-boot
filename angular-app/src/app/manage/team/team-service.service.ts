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
export class TeamService {
  private endpoint = environment.springboot + '/api/teams';

  constructor(private http: HttpClient) {
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

  public getUsersSelect() {
    return this.http.get<{
      userId: string,
      username: string,
    }[]>(`${this.endpoint}/get-users`);
  }

  public resetPassword(userId: string) {
    return this.http.post(`${this.endpoint}/${userId}/reset-password`, {});
  }

  public getRoles() {
    return this.http.get<{
      selected: boolean,
      roleId: string,
      roleName: string,
    }[]>(`${this.endpoint}/get-roles`);

  }

  public updateUser(user: {
    userId: string,
    username: string,
    name: string,
    lastname: string,
    email: string,
    year: string;
    month: string;
    day: string;
    role: string[],
    status: boolean
    sel: boolean
  }[]) {
    return this.http.post<{
      userId: string,
      username: string,
      name: string,
      lastname: string,
      email: string,
      year: string;
      month: string;
      day: string;
      role: string[],
      status: boolean
      sel: boolean
    }[]>(`${this.endpoint}/update-users`, user);
  }

  public insertUsers(user: {
    userId: string,
    username: string,
    name: string,
    lastname: string,
    email: string,
    year: string;
    month: string;
    day: string;
    role: string[],
    status: boolean
    sel: boolean
  }[]) {
    return this.http.post<{
      userId: string,
      username: string,
      name: string,
      lastname: string,
      email: string,
      year: string;
      month: string;
      day: string;
      role: string[],
      status: boolean
      sel: boolean
    }[]>(`${this.endpoint}/insert-users`, user);
  }
}
