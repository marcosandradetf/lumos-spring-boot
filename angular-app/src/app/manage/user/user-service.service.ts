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
export class UserService {
  private endpoint = environment.springboot + '/api/user';

  constructor(private http: HttpClient) {
  }

  public getUsers(){
    return this.http.get<{
      userId: string,
      username: string,
      name: string,
      lastname: string,
      email: string,
      dateOfBirth: string,
      day: string;
      month: string;
      year: string;
      role: string[],
      status: boolean,
      sel: boolean
    }[]>(`${this.endpoint}/get-users`);
  }

  public resetPassword(userId: string) {
    return this.http.post(`${this.endpoint}/${userId}/reset-password`, {});
  }

  public getRoles() {
    return this.http.get<{
      selected: boolean,
      idRole: string,
      nomeRole: string,
    }[]>(`${this.endpoint}/get-roles`);

  }
}
