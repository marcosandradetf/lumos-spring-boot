import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {environment} from '../../../environments/environment';
import {TeamsModel} from '../../models/teams.model';

@Injectable({
  providedIn: 'root'
})
export class TeamService {
  private endpoint = environment.springboot + '/api/teams';

  constructor(private http: HttpClient) {
  }

  public getTeams() {
    return this.http.get<TeamsModel[]>(`${this.endpoint}/get-teams`);
  }

  public getUsers() {
    return this.http.get<{
      userId: string,
      name: string,
      lastname: string,
      role: string[],
    }[]>(`${environment.springboot}/api/user/get-users`);
  }

  public updateTeams(user: TeamsModel[]) {
    return this.http.post<TeamsModel[]>(`${this.endpoint}/update-teams`, user);
  }

  public insertTeams(user: TeamsModel[]) {
    return this.http.post<{
      idTeam: string;
      teamName: string;
      driver: { driverId: string; driverName: string };
      electrician: { electricianId: string; electricianName: string };
      othersMembers: { memberId: string; memberName: string }[];
      UFName: string;
      cityName: string;
      regionName: string;
      depositName: string;
      plate: string;
      sel: boolean;
    }[]>(`${this.endpoint}/post-teams`, user);
  }
}
