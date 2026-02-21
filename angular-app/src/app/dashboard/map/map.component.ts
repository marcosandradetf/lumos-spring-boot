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
import 'leaflet.markercluster';

type ExecutionType = 'INSTALLATION' | 'MAINTENANCE';
type ExecutionStatus = 'IN_PROGRESS' | 'FINISHED' | 'BLOCKED';

interface GeoExecution {
    id: number;
    title: string;
    type: ExecutionType;
    status: ExecutionStatus;
    lat: number;
    lng: number;
    address: string;
    finishedAt: Date | null;
    teamId: number;
    teamName: string;
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
    templateUrl: './map.component.html',
    styleUrls: ['./map.component.scss'],
})
export class MapComponent implements AfterViewInit, OnDestroy, OnInit {
    // No topo da classe
    private markersMap = new Map<number | string, L.Marker>();

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
            lat: -19.92,
            lng: -43.94,
            address: 'Centro',
            finishedAt: null
        },
        {
            id: 2,
            title: 'Praça Norte',
            type: 'MAINTENANCE',
            status: 'IN_PROGRESS',
            teamId: 2,
            teamName: 'Equipe Beta',
            lat: -18.91,
            lng: -48.26,
            address: 'Zona Norte',
            finishedAt: null
        },
        {
            id: 3,
            title: 'Av. Sul 120',
            type: 'INSTALLATION',
            status: 'FINISHED',
            teamId: 3,
            teamName: 'Equipe Gama',
            lat: -20.39,
            lng: -43.50,
            address: 'Zona Sul',
            finishedAt: null
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

        const marker = this.markersMap.get(execution.id);

        if (marker) {
            // 1. FORÇA o fechamento de qualquer popup que o mapa esteja exibindo no momento
            this.map.closePopup();

            const clusterGroup = this.markersLayer.getLayers()[0] as any;

            // 2. Garante que o marcador esteja visível (abre o cluster se necessário)
            clusterGroup.zoomToShowLayer(marker, () => {
                // 3. Reposiciona a câmera
                this.map.setView([execution.lat, execution.lng], 18);

                // 4. Abre o novo popup
                marker.openPopup();
            });
        }
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

    // private renderMarkers(): void {
    //
    //     if (!this.map || !this.markersLayer) return;
    //
    //     this.markersLayer.clearLayers();
    //
    //     const validExecutions = this.filteredExecutions.filter(e => e.lat && e.lng);
    //
    //     validExecutions.forEach(e => {
    //
    //         const marker = L.marker([Number(e.lat), Number(e.lng)]);
    //
    //         marker.bindPopup(`
    //         <strong>${e.title}</strong><br/>
    //         ${e.address ?? ''}<br/>
    //         Tipo: ${e.type}<br/>
    //         Status: ${e.status}<br/>
    //         Equipe: ${e.teamName}
    //     `);
    //
    //         marker.addTo(this.markersLayer);
    //     });
    //
    //     // 🔥 AQUI entra o fitBounds
    //     if (validExecutions.length > 0) {
    //
    //         const bounds = L.latLngBounds(
    //             validExecutions.map(e => [Number(e.lat), Number(e.lng)] as [number, number])
    //         );
    //
    //         this.map.fitBounds(bounds.pad(0.2));
    //     }
    // }

    private createCustomIcon(type: ExecutionType, status: ExecutionStatus) {
        // Cor: Finalizado = Verde, Em Execução = Azul
        const colorClass = status === 'FINISHED' ? 'bg-emerald-600' : 'bg-blue-600';

        // Forma: Instalação = Quadrado, Manutenção = Redondo
        const shapeClass = type === 'INSTALLATION' ? 'rounded-none' : 'rounded-full';

        return L.divIcon({
            className: 'custom-marker',
            // Criamos uma div interna com as classes do Tailwind
            html: `<div class="w-4 h-4 shadow-lg border-2 border-white ${colorClass} ${shapeClass} transition-transform hover:scale-125"></div>`,
            iconSize: [16, 16],
            iconAnchor: [8, 8] // Centraliza o ícone na coordenada
        });
    }

    private renderMarkers(): void {
        if (!this.map) return;

        this.markersLayer.clearLayers();

        // Configurações do Cluster
        const clusterGroup = (L as any).markerClusterGroup({
            disableClusteringAtZoom: 17,
            maxClusterRadius: 50,
            iconCreateFunction: (cluster: any) => {
                const count = cluster.getChildCount();

                // Estilização usando as cores do seu sistema (Blue-600)
                return L.divIcon({
                    html: `
                <div class="flex items-center justify-center rounded-full bg-blue-600 text-white font-bold shadow-lg border-2 border-white"
                     style="width: 40px; height: 40px;">
                    ${count}
                </div>`,
                    className: 'custom-cluster-icon',
                    iconSize: [40, 40]
                });
            }
        });

        this.filteredExecutions.forEach(e => {
            if (e.lat && e.lng) {
                const marker = L.marker([Number(e.lat), Number(e.lng)], {
                    icon: this.createCustomIcon(e.type, e.status)
                });

                marker.bindPopup(`
                  <div class="p-3 min-w-[240px] space-y-3 font-sans text-slate-800">
                    <div class="flex flex-col gap-1 border-b border-slate-100 pb-2">
                      <div class="flex items-center justify-between">
                         <span class="text-[10px] font-bold uppercase tracking-wider text-slate-400">
                            ${e.type === 'INSTALLATION' ? 'Instalação' : 'Manutenção'}
                         </span>
                         <span class="flex h-2 w-2 rounded-full ${e.status === 'FINISHED' ? 'bg-emerald-500' : 'bg-blue-500'}"></span>
                      </div>
                      <strong class="text-sm leading-tight text-slate-900 ">${e.title}</strong>
                    </div>

                    <div class="space-y-2 text-xs">
                      <div class="flex items-start gap-2">
                        <i class="pi pi-map-marker mt-0.5 text-slate-400"></i>
                        <span>${e.address || 'Sem endereço registrado'}</span>
                      </div>

                      <div class="flex items-center gap-2">
                        <i class="pi pi-users text-slate-400"></i>
                        <span>Equipe: <b class="text-slate-700 ">${e.teamName}</b></span>
                      </div>

                      ${e.finishedAt ? `
                        <div class="flex items-center gap-2">
                          <i class="pi pi-calendar-check text-emerald-500"></i>
                          <span>Finalizado em: <b>${new Date(e.finishedAt).toLocaleString('pt-BR')}</b></span>
                        </div>
                      ` : ''}
                    </div>
                  </div>
            `, {
                    maxWidth: 300,
                    className: 'lumos-custom-popup' // Classe para remover as bordas feias do Leaflet
                });
                this.markersMap.set(e.id, marker);
                clusterGroup.addLayer(marker);
            }
        });

        const bounds = L.latLngBounds(
            this.filteredExecutions.map(e => [Number(e.lat), Number(e.lng)] as [number, number])
        );
        this.map.fitBounds(bounds.pad(0.014));
        this.markersLayer.addLayer(clusterGroup);
    }


}
