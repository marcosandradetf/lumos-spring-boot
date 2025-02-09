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
      idTeam: string;
      teamName: string;
      driver: { driverId: string; driverName: string };
      electrician: { electricianId: string; electricianName: string };
      othersMembers: { memberId: string; memberName: string }[];
      UFName: string;
      cityName: string;
      regionName: string;
      plate: string;
      sel: boolean;
    }[]>(`${this.endpoint}/get-teams`);
  }

  public getUsers() {
    return this.http.get<{
      userId: string,
      name: string,
      lastname: string,
      role: string[],
    }[]>(`${environment.springboot}/api/user/get-users`);
  }

  public updateTeams(user: {
    idTeam: string;
    teamName: string;
    driver: { driverId: string; driverName: string };
    electrician: { electricianId: string; electricianName: string };
    othersMembers: { memberId: string; memberName: string }[];
    UFName: string;
    cityName: string;
    regionName: string;
    plate: string;
    sel: boolean;
  }[]) {
    return this.http.post<{
      idTeam: string;
      teamName: string;
      driver: { driverId: string; driverName: string };
      electrician: { electricianId: string; electricianName: string };
      othersMembers: { memberId: string; memberName: string }[];
      UFName: string;
      cityName: string;
      regionName: string;
      plate: string;
      sel: boolean;
    }[]>(`${this.endpoint}/update-teams`, user);
  }

  public insertTeams(user: {
    idTeam: string;
    teamName: string;
    driver: { driverId: string; driverName: string };
    electrician: { electricianId: string; electricianName: string };
    othersMembers: { memberId: string; memberName: string }[];
    UFName: string;
    cityName: string;
    regionName: string;
    plate: string;
    sel: boolean;
  }[]) {
    return this.http.post<{
      idTeam: string;
      teamName: string;
      driver: { driverId: string; driverName: string };
      electrician: { electricianId: string; electricianName: string };
      othersMembers: { memberId: string; memberName: string }[];
      UFName: string;
      cityName: string;
      regionName: string;
      plate: string;
      sel: boolean;
    }[]>(`${this.endpoint}/post-teams`, user);
  }
}
