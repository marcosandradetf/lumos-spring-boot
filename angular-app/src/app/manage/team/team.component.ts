import {Component} from '@angular/core';
import {Router} from '@angular/router';
import {UtilsService} from '../../core/service/utils.service';
import {AuthService} from '../../core/auth/auth.service';
import {DomSanitizer, SafeResourceUrl, Title} from '@angular/platform-browser';
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
import {LoadingComponent} from '../../shared/components/loading/loading.component';
import {forkJoin} from 'rxjs';
import {Utils} from '../../core/service/utils';
import {GuideStateComponent} from '../../guide-state/guide-state.component';

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
        MultiSelect,
        LoadingComponent,
        GuideStateComponent
    ],
    templateUrl: './team.component.html',
    styleUrl: './team.component.scss'
})
export class TeamComponent {
    quickAddMenuOpen = false;
    readonly quickAddOptions = [1, 3];
    add: boolean = false;
    change: boolean = false;
    loading: boolean = true;
    formSubmitted: boolean = false;
    hasCollaborator = false;
    searchTerm: string = '';


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
    embeddedDocOpen = false;
    embeddedDocTitle = '';
    embeddedDocDescription = '';
    embeddedDocUrl: SafeResourceUrl | null = null;
    docUrl: string | null = null;

    readonly docs: {
        key: 'setup-teams' | 'field-app' | 'day-to-day';
        title: string;
        description: string;
        url: string
    }[] = [
        {
            key: 'setup-teams',
            title: 'Como estruturar equipes de campo',
            description: 'Veja como montar equipes, vincular caminhão e preparar a operação para uso no app.',
            url: 'https://lumosip.com.br/como-usar/02-access-management/04-team-management/'
        },
        {
            key: 'field-app',
            title: 'Como orientar a equipe a baixar o app',
            description: 'Abra o guia para explicar download e primeiro acesso.',
            url: 'https://lumosip.com.br/como-usar/03-operation/01-android-app-admin/'
        },
        {
            key: 'day-to-day',
            title: 'Como orientar a equipe a usar o app',
            description: 'Guia prático para equipes operacionais usarem o app durante a rotina em campo.',
            url: 'https://lumosip.com.br/como-usar/03-operation/03-day-to-day-app/'
        }
    ];

    constructor(protected router: Router, protected utils: UtilsService,
                protected authService: AuthService, private titleService: Title, private ibgeService: IbgeService,
                private teamService: TeamService,
                private sanitizer: DomSanitizer) {
        SharedState.setCurrentPath(["Configurações", "Equipes"]);
        this.titleService.setTitle("Configurações - Equipes");
        this.ibgeService.getUfs().subscribe(
            response => {
                this.ufs = response;
            }
        );

        forkJoin({
            users: this.teamService.getUsers(),
            teams: teamService.getTeams()
        }).subscribe({
            next: (res) => {
                res.users.forEach((user) => {
                    const roles = user.role.map(r => r.roleName);
                    if (roles.includes('MOTORISTA') || roles.includes('ELETRICISTA')) {
                        this.users.push(user);
                    }
                });

                this.teams = res.teams;
                this.teamsBackup = JSON.parse(JSON.stringify(this.teams));
                this.applySearch();
                this.loading = false;
            },
            error: (err) => {
                Utils.handleHttpError(err, this.router);
                this.loading = false;
            }
        });
    }

    getCities(uf: string) {
        this.ibgeService.getCities(uf).subscribe(cities => {
            this.cities = cities;
        })
    }

    get totalTeamsCount(): number {
        return this.teamsBackup.filter(team => team.idTeam !== '').length;
    }

    get draftTeamsCount(): number {
        return this.teamsBackup.filter(team => team.idTeam === '').length;
    }

    get hasDraftTeams(): boolean {
        return this.draftTeamsCount > 0;
    }

    toggleQuickAddMenu() {
        this.quickAddMenuOpen = !this.quickAddMenuOpen;
    }

    addTeamsBatch(count: number) {
        for (let index = 0; index < count; index += 1) {
            const team = this.createDraftTeam();
            this.teams.unshift(team);
            this.teamsBackup.unshift(JSON.parse(JSON.stringify(team)));
        }

        this.quickAddMenuOpen = false;
        this.applySearch();
    }

    openDocumentation(docKey: 'setup-teams' | 'field-app' | 'day-to-day') {
        const doc = this.docs.find(item => item.key === docKey);
        if (!doc) {
            return;
        }

        this.embeddedDocTitle = doc.title;
        this.embeddedDocDescription = doc.description;
        this.embeddedDocUrl = this.sanitizer.bypassSecurityTrustResourceUrl(doc.url);
        this.docUrl = doc.url;
        this.embeddedDocOpen = true;
    }

    closeDocumentation() {
        this.embeddedDocOpen = false;
        this.embeddedDocTitle = '';
        this.embeddedDocDescription = '';
        this.embeddedDocUrl = null;
        this.docUrl = null;
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
        this.addTeamsBatch(1);
    }

    removeTeam() {
        if (!this.hasDraftTeams) {
            return;
        }

        this.teams = this.teams.filter(team => team.idTeam !== '');
        this.teamsBackup = this.teamsBackup.filter(team => team.idTeam !== '');
        this.quickAddMenuOpen = false;
        this.applySearch();
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
                this.applySearch();
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

    filterTeams(event: Event) {
        this.searchTerm = (event.target as HTMLInputElement).value ?? '';
        this.applySearch();
    }

    private applySearch() {
        const value = this.searchTerm.trim().toLowerCase();

        if (!value) {
            this.teams = JSON.parse(JSON.stringify(this.teamsBackup));
            return;
        }

        this.teams = this.teamsBackup.filter(team => {
            const searchableFields = [
                team.teamName,
                team.plate,
                team.UFName,
                team.cityName,
                team.regionName,
            ];

            return searchableFields.some(field =>
                (field ?? '').toString().toLowerCase().includes(value)
            );
        }).map(team => JSON.parse(JSON.stringify(team)));
    }

    private createDraftTeam(): TeamsModel {
        return {
            idTeam: '',
            teamName: '',
            memberIds: [],
            memberNames: [],
            UFName: '',
            cityName: '',
            regionName: '',
            plate: '',
            depositName: '',
            sel: true,
        };
    }

    async shareDocumentation() {
        const shareText = 'Teste 123';

        try {
            await Utils.shareMessage(shareText, {
                title: `${this.embeddedDocTitle} • Lumos IP™`,
                subject: `${this.embeddedDocTitle} • Lumos IP™`
            });
        } catch (error: any) {
            if (error?.name !== 'AbortError') {
                this.utils.showMessage('Não foi possível compartilhar a documentação.', 'error', 'Lumos™');
            }
        }
    }

}
