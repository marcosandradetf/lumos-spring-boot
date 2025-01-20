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
    NgClass
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
  selectedRegion: string = '';
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

  constructor(protected router: Router, private userService: UserService, protected utils: UtilsService,
              protected authService: AuthService, private titleService: Title, private ibgeService: IbgeService,
              private teamService : TeamService,) {
    this.titleService.setTitle("Configurações - Equipes");
    this.ibgeService.getUfs().subscribe(
      response => {
        this.ufs = response;
      }
    );

    this.userService.getUsersSelect().subscribe(
      response => {
        this.users = response;
      }
    );

    this.teamService.getTeams().subscribe(
      response => {
        this.teams = response;
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

}
