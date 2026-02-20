import {AfterViewInit, Component, OnDestroy, OnInit} from '@angular/core';
import {CommonModule} from '@angular/common';
import {FormsModule} from '@angular/forms';

import {CardModule} from 'primeng/card';
import {DropdownModule} from 'primeng/dropdown';
import {MultiSelectModule} from 'primeng/multiselect';
import {InputTextModule} from 'primeng/inputtext';
import {TagModule} from 'primeng/tag';
import {ButtonModule} from 'primeng/button';

import * as L from 'leaflet';
import {TeamService} from '../../manage/team/team-service.service';
import {forkJoin} from 'rxjs';
import {DashboardService} from '../home/dashboard.service';

type ExecutionType = 'INSTALLATION' | 'MAINTENANCE';
type ExecutionStatus = 'IN_PROGRESS' | 'FINISHED' | 'BLOCKED';

interface GeoExecution {
    id: number;
    title: string;
    type: ExecutionType;
    status: ExecutionStatus;
    teamId: number;
    teamName: string;
    lat: number;
    lng: number;
    address: string;
}

@Component({
    selector: 'app-map',
    standalone: true,
    imports: [
        CommonModule,
        FormsModule,
        CardModule,
        DropdownModule,
        MultiSelectModule,
        InputTextModule,
        TagModule,
        ButtonModule
    ],
    templateUrl: './map.component.html'
})
export class MapComponent implements AfterViewInit, OnDestroy, OnInit {

    // ================= MOCK =================
    teams = [
        {id: 1, name: 'Equipe Alpha'},
        {id: 2, name: 'Equipe Beta'},
        {id: 3, name: 'Equipe Gama'}
    ];

    executions: GeoExecution[] = [
        {
            id: 1,
            title: 'Poste 18 - Centro',
            type: 'INSTALLATION',
            status: 'IN_PROGRESS',
            teamId: 1,
            teamName: 'Equipe Alpha',
            lat: -23.55052,
            lng: -46.633308,
            address: 'Centro'
        },
        {
            id: 2,
            title: 'Praça Norte',
            type: 'MAINTENANCE',
            status: 'IN_PROGRESS',
            teamId: 2,
            teamName: 'Equipe Beta',
            lat: -23.5596,
            lng: -46.6584,
            address: 'Zona Norte'
        },
        {
            id: 3,
            title: 'Av. Sul 120',
            type: 'INSTALLATION',
            status: 'FINISHED',
            teamId: 3,
            teamName: 'Equipe Gama',
            lat: -23.5652,
            lng: -46.6511,
            address: 'Zona Sul'
        }
    ];

    // ================= FILTERS =================
    searchText = '';
    selectedTeamId: number | null = null;

    selectedTypes: ExecutionType[] = ['INSTALLATION', 'MAINTENANCE'];
    selectedStatuses: ExecutionStatus[] = ['IN_PROGRESS', 'FINISHED'];

    typeOptions = [
        {label: 'Instalação', value: 'INSTALLATION'},
        {label: 'Manutenção', value: 'MAINTENANCE'}
    ];

    statusOptions = [
        {label: 'Em execução', value: 'IN_PROGRESS'},
        {label: 'Concluído', value: 'FINISHED'}
    ];

    // ================= UI =================
    selectedExecutionId: number | null = null;

    // ================= LEAFLET =================
    private map!: L.Map;
    private markersLayer!: L.LayerGroup;

    // ================= GETTERS =================
    get filteredExecutions(): GeoExecution[] {
        return this.executions.filter(e => {

            const matchText =
                !this.searchText ||
                e.title.toLowerCase().includes(this.searchText.toLowerCase()) ||
                e.address.toLowerCase().includes(this.searchText.toLowerCase());

            const matchTeam =
                !this.selectedTeamId || e.teamId === this.selectedTeamId;

            const matchType =
                this.selectedTypes.includes(e.type);

            const matchStatus =
                this.selectedStatuses.includes(e.status);

            return matchText && matchTeam && matchType && matchStatus;
        });
    }

    get kpis() {
        return {
            total: this.filteredExecutions.length,
            inProgress: this.filteredExecutions.filter(x => x.status === 'IN_PROGRESS').length,
            finished: this.filteredExecutions.filter(x => x.status === 'FINISHED').length
        };
    }

    constructor(
        private teamService: TeamService,
        private dashboardService: DashboardService,
        ) {

    }

    ngOnInit(): void {
        forkJoin({
            executions: this.dashboardService.getExecutions(),
            teams: this.teamService.getTeams()
        }).subscribe({
            next: ({executions, teams}) => {
                this.executions = executions;
                this.teams = teams.map(t =>
                    ({
                        id: Number(t.idTeam), name: t.teamName
                    })
                );

                if (this.map) {
                    this.renderMarkers();
                }
            }
        })
    }

    // ================= LIFECYCLE =================
    ngAfterViewInit(): void {
        this.initMap();
    }

    ngOnDestroy(): void {
        this.map?.remove();
    }

    // ================= ACTIONS =================
    applyFilters(): void {
        this.renderMarkers();
    }

    clearFilters(): void {
        this.searchText = '';
        this.selectedTeamId = null;
        this.selectedTypes = ['INSTALLATION', 'MAINTENANCE'];
        this.selectedStatuses = ['IN_PROGRESS', 'FINISHED', 'BLOCKED'];
        this.renderMarkers();
    }

    selectExecution(execution: GeoExecution): void {
        this.selectedExecutionId = execution.id;
        this.map.setView([execution.lat, execution.lng], 15);
    }

    // ================= MAP =================
    private initMap(): void {

        this.map = L.map('geo-map').setView(
            [-23.55052, -46.633308],
            12
        );

        L.tileLayer(
            'https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png',
            {
                maxZoom: 19,
                attribution: '© Lumos'
            }
        ).addTo(this.map);

        this.markersLayer = L.layerGroup().addTo(this.map);
    }

    private renderMarkers(): void {

        if (!this.map || !this.markersLayer) return;

        this.markersLayer.clearLayers();

        const validExecutions = this.filteredExecutions.filter(e => e.lat && e.lng);

        validExecutions.forEach(e => {

            const marker = L.marker([Number(e.lat), Number(e.lng)]);

            marker.bindPopup(`
            <strong>${e.title}</strong><br/>
            ${e.address ?? ''}<br/>
            Tipo: ${e.type}<br/>
            Status: ${e.status}<br/>
            Equipe: ${e.teamName}
        `);

            marker.addTo(this.markersLayer);
        });

        // 🔥 AQUI entra o fitBounds
        if (validExecutions.length > 0) {

            const bounds = L.latLngBounds(
                validExecutions.map(e => [Number(e.lat), Number(e.lng)] as [number, number])
            );

            this.map.fitBounds(bounds.pad(0.2));
        }
    }


}
