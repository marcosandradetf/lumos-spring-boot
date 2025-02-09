import {Component} from '@angular/core';
import {Router} from '@angular/router';
import {UserService} from '../user/user-service.service';
import {UtilsService} from '../../core/service/utils.service';
import {AuthService} from '../../core/auth/auth.service';
import {Title} from '@angular/platform-browser';
import {ButtonComponent} from '../../shared/components/button/button.component';
import {FormsModule, NgForm, ReactiveFormsModule} from '@angular/forms';
import {NgClass, NgForOf, NgIf} from '@angular/common';
import {TableComponent} from '../../shared/components/table/table.component';
import {ufRequest} from '../../core/uf-request.dto';
import {IbgeService} from '../../core/service/ibge.service';
import {citiesRequest} from '../../core/cities-request.dto';
import {TeamService} from './team-service.service';
import {catchError, tap} from 'rxjs';
import {AlertMessageComponent} from '../../shared/components/alert-message/alert-message.component';

@Component({
  selector: 'app-team',
  standalone: true,
  imports: [
    ButtonComponent,
    FormsModule,
    NgForOf,
    NgIf,
    ReactiveFormsModule,
    TableComponent,
    NgClass,
    AlertMessageComponent
  ],
  templateUrl: './team.component.html',
  styleUrl: './team.component.scss'
})
export class TeamComponent {
  add: boolean = false;
  change: boolean = false;
  loading: boolean = false;
  formSubmitted: boolean = false;

  teams: {
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
  }[] = [
    {
      idTeam: '',
      teamName: '',
      driver: {driverId: '', driverName: ''},
      electrician: {electricianId: '', electricianName: ''},
      othersMembers: [
      ],
      UFName: '',
      cityName: '',
      regionName: '',
      plate: '',
      sel: false,
    }
  ];

  teamsBackup: {
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
  }[] = [
    {
      idTeam: '',
      teamName: '',
      driver: {driverId: '', driverName: ''},
      electrician: {electricianId: '', electricianName: ''},
      othersMembers: [
      ],
      UFName: '',
      cityName: '',
      regionName: '',
      plate: '',
      sel: false,
    }
  ];

  ufs: ufRequest[] = [];
  cities: citiesRequest[] = [];

  users: {
    userId: string,
    name: string,
    lastname: string,
    role: string[]
  }[] = [];

  drivers: {
    driverId: string,
    driverName: string,
  }[] = [];

  electricians: {
    electricianId: string,
    electricianName: string,
  }[] = [];

  serverMessage: string | null = null;
  alertType: string = '';
  isMultiSelectVisible: boolean = false;

  constructor(protected router: Router, protected utils: UtilsService,
              protected authService: AuthService, private titleService: Title, private ibgeService: IbgeService,
              private teamService: TeamService,) {
    this.titleService.setTitle("Configurações - Equipes");
    this.ibgeService.getUfs().subscribe(
      response => {
        this.ufs = response;
      }
    );

    this.teamService.getUsers().subscribe(
      response => {
        response.forEach((user) => {
          if (Array.isArray(user.role) && (user.role.includes('MOTORISTA') || user.role.includes('ELETRICISTA'))) {
            this.users.push(user);
          }
        });

        this.users.forEach((user) => {
          if (Array.isArray(user.role) && user.role.includes('MOTORISTA')) {
            this.drivers.push({
              driverId: user.userId,
              driverName: `${user.name} ${user.lastname}`,
            });
          } else if (Array.isArray(user.role) && user.role.includes('ELETRICISTA')) {
            this.electricians.push({
              electricianId: user.userId,
              electricianName: `${user.name} ${user.lastname}`,
            });
          }
        });
      }
    );

    this.teamService.getTeams().subscribe(
      response => {
        this.teams = response;
        this.teamsBackup = JSON.parse(JSON.stringify(this.teams));
      }
    );

  }

  getCities(uf: string) {
    this.ibgeService.getCities(uf).subscribe(cities => {
      this.cities = cities;
    })
  }

  updateRegion(selectedCityName: string, i: number): void {
    const selectedCity = this.cities.find(city => city.nome === selectedCityName);
    this.teams[i].regionName = selectedCity ? selectedCity.microrregiao.mesorregiao.nome : '';
    console.log(this.teams[i]);
  }

  submitTeams(form
              :
              NgForm
  ) {
    this.formSubmitted = true;

    if (form.invalid) {
      console.log('Formulário inválido');
      return;
    }

    this.loading = true;

    const insert = this.teams.some(t => t.idTeam === '');
    const update = this.teams.every(t => t.idTeam !== '');
    const updateCheckSel = this.teams.some(t => t.sel);

    if (insert && this.teams.length !== this.teamsBackup.length) {
      this.insertTeams();
    } else if (update && updateCheckSel) {
      this.updateTeams();
    }

    this.loading = false;
  }

  resetView() {
    return () => {
      this.change = false;
      this.add = false;
      this.teams = JSON.parse(JSON.stringify(this.teamsBackup));
    }
  }

  newTeam() {
    const team = {
      idTeam: '',
        teamName: '',
        driver: {driverId: '', driverName: ''},
      electrician: {electricianId: '', electricianName: ''},
      othersMembers: [
      ],
        UFName: '',
        cityName: '',
        regionName: '',
        plate: '',
        sel: false,
    };
    this.teams.push(team);
  }

  removeTeam() {
    const lastElement = this.teams[this.teams.length - 1];
    if (lastElement.idTeam === '') {
      this.teams.pop();
    }
  }


  insertTeams() {

    this.teamService.insertTeams(this.teams).pipe(
      tap(r => {
        this.showMessage("Equipes criadas com sucesso!");
        this.alertType = "alert-success";
        this.teams = r;
        this.teamsBackup = JSON.parse(JSON.stringify(this.teams));
        this.add = false;
      }),
      catchError(err => {
        this.showMessage(err.error.message);
        this.alertType = "alert-error";
        throw err;
      })
    ).subscribe();

  }


  updateTeams() {
    // Verifica se nenhum usuário foi selecionado
    const noneSelected = this.teams.every(t => !t.sel);

    if (noneSelected) {
      this.showMessage("Nenhuma equipe foi selecionada.");
      this.alertType = "alert-error";
      this.loading = false;
      return;
    }

    this.teamService.updateTeams(this.teams)
      .pipe(tap(r => {
          this.showMessage("Equipes atualizadas com sucesso.");
          this.alertType = "alert-success";
          this.teams = r;
          this.teamsBackup = JSON.parse(JSON.stringify(this.teams));
          this.change = false;
        }),
        catchError(err => {
          this.showMessage(err.error.message);
          this.alertType = "alert-error";
          throw err;
        })
      ).subscribe();

  }


  showMessage(message
              :
              string, timeout = 3000
  ) {
    this.serverMessage = message;
    setTimeout(() => {
      this.serverMessage = null;
    }, timeout);
  }


  changeMember(index
               :
               number, memberId
               :
               string
  ) {
    if (index === -1) {
      console.log('Equipe não encontrada');
      return;
    }

    let teams = this.teams[index];

    let members = teams.othersMembers;
    let memberExist = members.some(member => member.memberId === memberId)


    if (memberExist) {
      members = members.filter(m => m.memberId !== memberId);
    } else {
      members.push({memberId: memberId, memberName: ''});
    }

    teams.othersMembers = members;
    this.teams[index] = teams;
    console.log(this.teams);
  }

  verifyMember(index
               :
               number, memberId
               :
               string
  ) {
    let teams = this.teams[index];
    let members = teams.othersMembers;

    // Verifica se alguma role no array corresponde ao nomeRole fornecido
    return members.some(m => m.memberId === memberId);

  }

  debug(team: any, teams: any) {
    console.log(team)
    console.log(teams)
  }
}
