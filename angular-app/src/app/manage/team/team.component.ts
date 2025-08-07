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
import {TeamsModel} from '../../models/teams.model';
import {Toast} from 'primeng/toast';

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
    Toast
  ],
  templateUrl: './team.component.html',
  styleUrl: './team.component.scss'
})
export class TeamComponent {
  add: boolean = false;
  change: boolean = false;
  loading: boolean = false;
  formSubmitted: boolean = false;

  teams: TeamsModel[] = [
    {
      idTeam: '',
      teamName: '',
      driver: {driverId: '', driverName: ''},
      electrician: {electricianId: '', electricianName: ''},
      UFName: '',
      cityName: '',
      regionName: '',
      plate: '',
      depositName: '',
      sel: false,
    }
  ];

  teamsBackup: {
    idTeam: string;
    teamName: string;
    driver: { driverId: string; driverName: string };
    electrician: { electricianId: string; electricianName: string };
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
          }
          if (Array.isArray(user.role) && user.role.includes('ELETRICISTA')) {
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
        UFName: '',
        cityName: '',
        regionName: '',
        plate: '',
        depositName: '',
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

    this.teamService.insertTeams(this.teams).subscribe({
      next: () => {
        this.utils.showMessage("Equipes criadas com sucesso!", 'success', 'Sucesso');
        this.alertType = "alert-success";

      },
      error: err => {
        this.utils.showMessage(err.error.message, 'error', 'Erro');
      },
    });

  }


  updateTeams() {
    // Verifica se nenhum usuário foi selecionado
    const noneSelected = this.teams.every(t => !t.sel);

    if (noneSelected) {
      this.utils.showMessage("Nenhuma equipe foi selecionada.", 'warn', 'Atenção');
      this.alertType = "alert-error";
      this.loading = false;
      return;
    }

    this.teamService.updateTeams(this.teams).subscribe({
      next: (r) => {
        this.utils.showMessage("Equipes atualizadas com sucesso!", 'success', 'Sucesso');
        this.alertType = "alert-success";
        this.teams = r;
        this.teamsBackup = JSON.parse(JSON.stringify(this.teams));
        this.change = false;
      },
      error: err => {
        this.utils.showMessage(err.error.message, 'error', 'Erro');
        console.log(err);
      },
    });

  }



}
