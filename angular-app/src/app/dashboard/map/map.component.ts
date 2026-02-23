import * as L from 'leaflet';
import {AfterViewInit, Component, OnDestroy, OnInit} from '@angular/core';
import {CommonModule} from '@angular/common';
import {FormsModule} from '@angular/forms';

import {CardModule} from 'primeng/card';
import {DropdownModule} from 'primeng/dropdown';
import {MultiSelectModule} from 'primeng/multiselect';
import {InputTextModule} from 'primeng/inputtext';
import {TagModule} from 'primeng/tag';
import {ButtonModule} from 'primeng/button';

import {TeamService} from '../../manage/team/team-service.service';
import {forkJoin} from 'rxjs';
import {DashboardService} from '../home/dashboard.service';
import Supercluster from 'supercluster';
import type { Feature, Point } from 'geojson';
import {UtilsService} from '../../core/service/utils.service';
import {Title} from '@angular/platform-browser';
import {Skeleton} from 'primeng/skeleton';

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
    imageUrl: string | null;
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
        ButtonModule,
        Skeleton
    ],
    templateUrl: './map.component.html',
    styleUrls: ['./map.component.scss'],
})
export class MapComponent implements AfterViewInit, OnDestroy, OnInit {
    private markersMap = new Map<number | string, L.Marker>();
    loading = false;

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
            finishedAt: null,
            imageUrl: null,
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
            finishedAt: null,
            imageUrl: null,
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
            finishedAt: null,
            imageUrl: null,
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
        private utils: UtilsService,
        private titleService: Title
    ) {

    }

    ngOnInit(): void {
        this.loading = true;
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
                    this.buildClusterIndex();
                    this.renderMarkers();
                    this.fitToData();
                }

                this.loading = false;
            },
            error: err => {
                this.loading = false;
                this.utils.showMessage(err.error.message ?? err.error.error, 'error')
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
        this.buildClusterIndex();
        this.renderMarkers();
        this.fitToData();
    }

    clearFilters(): void {
        this.searchText = '';
        this.selectedTeamId = null;
        this.selectedTypes = ['INSTALLATION', 'MAINTENANCE'];
        this.selectedStatuses = ['IN_PROGRESS', 'FINISHED', 'BLOCKED'];
        this.renderMarkers();
        this.fitToData();
    }

    selectExecution(execution: GeoExecution): void {
        if (!this.clusterIndex || !this.map) return;

        const zoom = this.map.getZoom();

        const clusters = this.clusterIndex.getClusters(
            [execution.lng, execution.lat, execution.lng, execution.lat],
            zoom
        );

        const cluster = clusters[0];

        let targetZoom = 18;

        if (
            cluster &&
            (cluster.properties as any).cluster &&
            typeof cluster.id === 'number'
        ) {
            targetZoom =
                this.clusterIndex.getClusterExpansionZoom(cluster.id);
        }

        // Usa flyTo para animação suave
        this.map.flyTo([execution.lat, execution.lng], targetZoom, {
            duration: 0.4
        });

        // Aguarda o fim do movimento do mapa
        this.map.once('moveend', () => {
            this.renderMarkers();

            const marker = this.markersMap.get(execution.id);
            marker?.openPopup();
        });
    }

    // ================= MAP =================
    private initMap(): void {

        this.map = L.map('geo-map').setView(
            [-18.91, -48.26],
            4
        );

        this.map.on('moveend zoomend', () => {
            this.renderMarkers();
        });

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

    private clusterIndex!: Supercluster<
        { executionId: number; execution: GeoExecution },
        any
    >;

    private renderMarkers(): void {
        if (!this.map || !this.clusterIndex) return;

        this.markersLayer.clearLayers();
        this.markersMap.clear();

        const bounds = this.map.getBounds();
        const zoom = this.map.getZoom();

        const clusters = this.clusterIndex.getClusters([
            bounds.getWest(),
            bounds.getSouth(),
            bounds.getEast(),
            bounds.getNorth()
        ], zoom);

        clusters.forEach(cluster => {

            const [lng, lat] = cluster.geometry.coordinates;

            if ((cluster.properties as any).cluster) {

                const count = (cluster.properties as any).point_count;

                const marker = L.marker([lat, lng], {
                    icon: L.divIcon({
                        html: `
                        <div class="flex items-center justify-center rounded-full bg-blue-600 text-white font-bold shadow-lg border-2 border-white"
                             style="width: 40px; height: 40px;">
                            ${count}
                        </div>`,
                        className: '',
                        iconSize: [40, 40]
                    })
                });

                marker.on('click', () => {

                    if (
                        (cluster.properties as any).cluster &&
                        typeof cluster.id === 'number'
                    ) {
                        const expansionZoom =
                            this.clusterIndex.getClusterExpansionZoom(cluster.id);

                        this.map.setView([lat, lng], expansionZoom);
                    }
                });

                marker.addTo(this.markersLayer);

            } else {

                const execution = (cluster.properties as any).execution;

                const marker = L.marker([lat, lng], {
                    icon: this.createCustomIcon(execution.type, execution.status)
                });

                marker.bindPopup(this.buildPopup(execution), {
                    maxWidth: 300,
                    className: 'lumos-custom-popup',
                    autoPanPadding: [20, 20]
                });
                this.markersMap.set(execution.id, marker);
                marker.addTo(this.markersLayer);
            }
        });

    }

    private fitToData(): void {
        if (!this.filteredExecutions.length) return;

        const bounds = L.latLngBounds(
            this.filteredExecutions.map(e =>
                [Number(e.lat), Number(e.lng)] as [number, number]
            )
        );

        this.map.fitBounds(bounds.pad(0.2));
    }

    private buildPopup(e: GeoExecution): string {
        return `
    <div id="popup-${e.id}" class="p-3 min-w-[240px] font-sans text-slate-800 transition-all duration-300">

        <div class="hide-on-expand space-y-3">
            <div class="flex flex-col gap-1 border-b border-slate-100 pb-2">
                <div class="flex items-center justify-between">
                    <span class="text-[10px] font-bold uppercase tracking-wider text-slate-400">
                        ${e.type === 'INSTALLATION' ? 'Instalação' : 'Manutenção'}
                    </span>
                    <span class="flex h-2 w-2 rounded-full ${e.status === 'FINISHED' ? 'bg-emerald-500' : 'bg-blue-500'}"></span>
                </div>
                <strong class="text-sm leading-tight text-slate-900">${e.title}</strong>
            </div>

            <div class="space-y-2 text-xs">
                <div class="flex items-start gap-2">
                    <i class="pi pi-map-marker mt-0.5 text-slate-400"></i>
                    <span>${e.address || 'Sem endereço registrado'}</span>
                </div>
                <div class="flex items-center gap-2">
                    <i class="pi pi-users text-slate-400"></i>
                    <span>Equipe: <b class="text-slate-700">${e.teamName}</b></span>
                </div>
            </div>
        </div>

        ${e.imageUrl ? `
            <div
                class="img-container relative mt-3 overflow-hidden rounded-lg border border-slate-100 shadow-sm group cursor-pointer transition-all duration-300">
                <div class="absolute top-2 left-2 z-10 transition-opacity duration-300 group-hover:opacity-0 pointer-events-none">
                    <span class="px-2 py-0.5 text-[10px] font-bold bg-slate-900/70 text-white backdrop-blur-md rounded-md">
                        ID do Ponto #5550123
                    </span>
                </div>
                <img src="${e.imageUrl}"
                    alt="Ponto"
                    class="w-full h-32 object-cover transition-all duration-500 hover:scale-105">
            </div>
        ` : ''}

        ${e.finishedAt ? `
            <div class="hide-on-expand mt-3 text-xs flex items-center gap-2">
                <i class="pi pi-calendar-check text-emerald-500"></i>
                <span>Finalizado em: <b>${new Date(e.finishedAt).toLocaleString('pt-BR')}</b></span>
            </div>
        ` : ''}
    </div>
    `;
    }

    private buildClusterIndex(): void {
        const points: Feature<
            Point,
            { executionId: number; execution: GeoExecution }
        >[] = this.filteredExecutions
            .filter(e => e.lat && e.lng)
            .map(e => ({
                type: 'Feature' as const,
                properties: {
                    executionId: e.id,
                    execution: e
                },
                geometry: {
                    type: 'Point' as const,
                    coordinates: [Number(e.lng), Number(e.lat)]
                }
            }));

        this.clusterIndex = new Supercluster({
            radius: 60,
            maxZoom: 17
        });

        this.clusterIndex.load(points);
    }

}
