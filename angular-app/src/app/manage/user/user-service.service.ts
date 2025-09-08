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

  public getUsers() {
    return this.http.get<{
      userId: string,
      username: string,
      name: string,
      lastname: string,
      email: string,
      cpf: string,
      dateOfBirth: string,
      day: string;
      month: string;
      year: string;
      role: string[],
      status: boolean,
      sel: boolean,
      show: boolean,
    }[]>(`${this.endpoint}/get-users`);
  }

  public getUser(uuid: string) {
    return this.http.get<{
      userId: string,
      username: string,
      name: string,
      lastname: string,
      email: string,
      cpf: string,
      dateOfBirth: string,
      day: string;
      month: string;
      year: string;
      role: string[],
      status: boolean,
      phone: string,
      department: string,
      sel: boolean,
    }>(`${this.endpoint}/get-user/${uuid}`);
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
    cpf: string,
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
      cpf: string,
      year: string;
      month: string;
      day: string;
      role: string[],
      status: boolean
      sel: boolean,
      show: boolean,
    }[]>(`${this.endpoint}/update-users`, user);
  }

  public insertUsers(user: {
    userId: string,
    username: string,
    name: string,
    lastname: string,
    email: string,
    cpf: string,
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
      cpf: string,
      year: string;
      month: string;
      day: string;
      role: string[],
      status: boolean
      sel: boolean,
      show: boolean,
    }[]>(`${this.endpoint}/insert-users`, user);
  }

  setPassword(uuid: string, password: {oldPassword: string; password: string; passwordConfirm: string}) {
    return this.http.post<{ message: string }>(`${this.endpoint}/${uuid}/set-password`, password);
  }
}
