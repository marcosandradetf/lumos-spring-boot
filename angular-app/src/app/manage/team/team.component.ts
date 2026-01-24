import {Component} from '@angular/core';
import {Router} from '@angular/router';
import {UtilsService} from '../../core/service/utils.service';
import {AuthService} from '../../core/auth/auth.service';
import {Title} from '@angular/platform-browser';
import {FormsModule, NgForm, ReactiveFormsModule} from '@angular/forms';
import {NgClass, NgForOf, NgIf} from '@angular/common';
import {TableComponent} from '../../shared/components/table/table.component';
import {ufRequest} from '../../core/uf-request.dto';
import {IbgeService} from '../../core/service/ibge.service';
import {citiesRequest} from '../../core/cities-request.dto';
import {TeamService} from './team-service.service';
import {TeamsModel} from '../../models/teams.model';
import {Toast} from 'primeng/toast';
import {MultiSelect} from 'primeng/multiselect';
import {SharedState} from '../../core/service/shared-state';

@Component({
    selector: 'app-team',
    standalone: true,
    imports: [
        FormsModule,
        NgForOf,
        NgIf,
        ReactiveFormsModule,
        TableComponent,
        NgClass,
        Toast,
        MultiSelect
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
            memberIds: [],
            memberNames: [],
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

    serverMessage: string | null = null;
    alertType: string = '';

    constructor(protected router: Router, protected utils: UtilsService,
                protected authService: AuthService, private titleService: Title, private ibgeService: IbgeService,
                private teamService: TeamService,) {
        SharedState.setCurrentPath(["Configurações", "Equipes"]);
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

        const updateCheckSel = this.teams.some(t => t.sel);

        if (updateCheckSel) {
            this.updateTeams();
        }
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
            memberIds: [] = [],
            memberNames: [] = [],
            UFName: '',
            cityName: '',
            regionName: '',
            plate: '',
            depositName: '',
            sel: true,
        };
        this.teams.splice(0, 0, team);
    }

    removeTeam() {
        const lastElement = this.teams[0];
        if (lastElement.idTeam === '') {
            this.teams = this.teams.filter(t => t.idTeam !== lastElement.idTeam);
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
                this.loading = false;
            },
            error: err => {
                this.loading = false;
                this.utils.showMessage(err.error.error, 'error', 'Erro');
                console.log(err);
            },
        });

    }

    test(t: any) {
        console.log(t);
    }

}
