import { Component } from '@angular/core';
import {SidebarComponent} from "../../shared/components/sidebar/sidebar.component";
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
    SidebarComponent,
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
  sidebarLinks = [
    {title: 'Início', path: '/configuracoes/dashboard', id: 'opt1'},
    {title: 'Usuários', path: '/configuracoes/usuarios', id: 'opt2'},
    {title: 'Equipes', path: '/configuracoes/equipes', id: 'opt3'},
    {title: 'Minha Empresa', path: '/configuracoes/empresa', id: 'opt4'},
  ];
  add: boolean = false;
  change: boolean = false;
  loading: boolean = false;
  formSubmitted: boolean = false;
  teams: {
    idTeam: string,
    teamName: string,
    userId: string,
    username: string,
    UFName: string,
    cityName: string,
    regionName: string,
    sel: boolean,
  }[] = [];
  teamsBackup: {
    idTeam: string,
    teamName: string,
    userId: string,
    username: string,
    UFName: string,
    cityName: string,
    regionName: string,
    sel: boolean,
  }[] = [];
  ufs: ufRequest[] = [];
  cities: citiesRequest[] = [];
  users: {
    userId: string,
    username: string,
  }[] = [];
  serverMessage: string | null = null;
  alertType: string = '';

  constructor(protected router: Router, protected utils: UtilsService,
              protected authService: AuthService, private titleService: Title, private ibgeService: IbgeService,
              private teamService : TeamService,) {
    this.titleService.setTitle("Configurações - Equipes");
    this.ibgeService.getUfs().subscribe(
      response => {
        this.ufs = response;
      }
    );

    this.teamService.getUsers().subscribe(
      response => {
        this.users = response;
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

  submitTeams(form: NgForm) {
    this.formSubmitted = true;

    if (form.invalid) {
      console.log('Formulário inválido');
      return;
    }

    this.loading = true;

    const insert = this.teams.some(t => t.userId === '');
    const update = this.teams.every(t => t.userId !== '');
    const updateCheckSel = this.teams.some(t => t.sel);

    if (insert && this.users.length !== this.teamsBackup.length) {
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
      userId: '',
      username: '',
      UFName: '',
      cityName: '',
      regionName: '',
      sel: false
    };
    this.teams.push(team);
  }

  removeTeam() {
    const lastElement = this.teams[this.teams.length - 1];
    if (lastElement.idTeam === '') {
      this.teams.pop();
    }
  }

  private insertTeams() {

    this.teamService.insertTeams(this.teams).pipe(
      tap(r => {
        this.showMessage("Equipes criadas com sucesso!");
        this.alertType = "alert-success";
        this.users = r;
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

  private updateTeams() {
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

  private showMessage(message: string, timeout = 3000) {
    this.serverMessage = message;
    setTimeout(() => {
      this.serverMessage = null;
    }, timeout);
  }
}
