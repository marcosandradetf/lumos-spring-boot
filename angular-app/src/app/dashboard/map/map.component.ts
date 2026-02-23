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
import {SharedState} from '../../core/service/shared-state';
import {FileService} from '../../core/service/file-service.service';

type ExecutionType = 'INSTALLATION' | 'MAINTENANCE';

type ExecutionStatus = 'IN_PROGRESS' | 'FINISHED' | 'BLOCKED';

interface GeoExecution {
    id: string;
    executionId: string;
    executionType: string | null;
    title: string;
    type: ExecutionType;
    status: ExecutionStatus;
    lat: number;
    lng: number;
    address: string;
    finishedAt: Date | null;
    teamId: number;
    teamName: string;
    photoUri: string | null;
    pointNumber: number | null;
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
    private markersMap = new Map<string, L.Marker>();
    private iconCache = new Map<string, L.DivIcon>();
    loading = false;

    // ================= MOCK =================
    teams = [
        {id: 1, name: 'Equipe Alpha'},
        {id: 2, name: 'Equipe Beta'},
        {id: 3, name: 'Equipe Gama'}
    ];

    executions: GeoExecution[] = [];

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
    selectedExecutionId: string | null = null;

    // ================= LEAFLET =================
    private map!: L.Map;
    private markersLayer!: L.LayerGroup;

    // ================= GETTERS =================
    protected _filteredExecutions: GeoExecution[] = [];

    get kpis() {
        return {
            total: this._filteredExecutions.length,
            inProgress: this._filteredExecutions.filter(x => x.status === 'IN_PROGRESS').length,
            finished: this._filteredExecutions.filter(x => x.status === 'FINISHED').length
        };
    }

    constructor(
        private teamService: TeamService,
        private dashboardService: DashboardService,
        private utils: UtilsService,
        private titleService: Title,
        private fileService: FileService
    ) {

    }

    ngOnInit(): void {
        this.titleService.setTitle('Mapa de execuções');
        SharedState.setCurrentPath(['Dashboard', 'Mapa de execuções']);
        this.loading = true;
        window.addEventListener('open-photo', (event: any) => {
            this.openModal(event.detail);
        });
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
                    this.applyFilters();
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

        this.photoCache.forEach(url => {
            URL.revokeObjectURL(url);
        });

        this.photoCache.clear();
    }

    // ================= ACTIONS =================
    applyFilters(): void {
        this._filteredExecutions = this.executions.filter(e => {

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
        this.buildClusterIndex();
        this.renderMarkers();
        this.fitToData();
    }

    clearFilters(): void {
        this.searchText = '';
        this.selectedTeamId = null;
        this.selectedTypes = ['INSTALLATION', 'MAINTENANCE'];
        this.selectedStatuses = ['IN_PROGRESS', 'FINISHED', 'BLOCKED'];

        this.applyFilters(); // 👈 isso resolve tudo
    }

    selectExecution(execution: GeoExecution): void {
        if (!this.map) return;

        const latlng: [number, number] = [execution.lat, execution.lng];

        this.map.once('moveend', () => {
            this.map.closePopup();

            const key = this.getPointKey(execution);
            const marker = this.markersMap.get(key);

            marker?.openPopup();
        });

        this.map.flyTo(latlng, 18, { duration: 0.4 });
    }

    // ================= MAP =================
    private initMap(): void {

        this.map = L.map('geo-map', {
            preferCanvas: true
        }).setView([-18.91, -48.26], 4);

        this.map.on('moveend zoomend', () => {
            this.renderMarkers();
        });

        this.map.on('popupopen', (event: any) => {

            const popupElement = event.popup.getElement();
            if (!popupElement) return;

            const pointKey = popupElement.querySelector('[data-point-key]')
                ?.getAttribute('data-point-key');

            if (!pointKey) return;

            // Busca execution pelo pointKey
            const execution = this._filteredExecutions.find(e =>
                this.getPointKey(e) === pointKey
            );

            if (!execution) return;

            this.loadPhotoIfNeeded(execution);
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

    private createCustomIcon(type: ExecutionType, status: ExecutionStatus) {
        const key = `${type}_${status}`;

        if (this.iconCache.has(key)) {
            return this.iconCache.get(key)!;
        }

        const colorClass = status === 'FINISHED' ? 'bg-emerald-600' : 'bg-blue-600';
        const shapeClass = type === 'INSTALLATION' ? 'rounded-none' : 'rounded-full';

        const icon = L.divIcon({
            className: 'custom-marker',
            html: `<div class="w-4 h-4 shadow-lg border-2 border-white ${colorClass} ${shapeClass}"></div>`,
            iconSize: [16, 16],
            iconAnchor: [8, 8]
        });

        this.iconCache.set(key, icon);
        return icon;
    }

    private clusterIndex!: Supercluster<
        { id: string; execution: GeoExecution },
        any
    >;

    private clusterIconCache = new Map<number, L.DivIcon>();

    private createClusterIcon(count: number): L.DivIcon {
        if (this.clusterIconCache.has(count)) {
            return this.clusterIconCache.get(count)!;
        }

        const icon = L.divIcon({
            html: `
      <div class="flex items-center justify-center rounded-full bg-blue-600 text-white font-bold shadow-lg border-2 border-white"
           style="width: 40px; height: 40px;">
        ${count}
      </div>`,
            className: '',
            iconSize: [40, 40]
        });

        this.clusterIconCache.set(count, icon);
        return icon;
    }

    private getPointKey(e: GeoExecution): string {
        return `point_${e.executionType ?? 'default'}_${e.id}`;
    }

    private renderMarkers(): void {
        if (!this.map || !this.clusterIndex) return;

        const bounds = this.map.getBounds();
        const zoom = this.map.getZoom();

        const clusters = this.clusterIndex.getClusters([
            bounds.getWest(),
            bounds.getSouth(),
            bounds.getEast(),
            bounds.getNorth()
        ], zoom);

        const newIds = new Set<string>();

        clusters.forEach(cluster => {

            const isCluster = (cluster.properties as any).cluster === true;

            let id: string;
            let marker: L.Marker;

            const [lng, lat] = cluster.geometry.coordinates;

            if (isCluster) {

                id = `cluster_${cluster.id}`;

                newIds.add(id);
                if (this.markersMap.has(id)) return;

                const count = (cluster.properties as any).point_count;

                marker = L.marker([lat, lng], {
                    icon: this.createClusterIcon(count)
                });

                marker.on('click', () => {
                    const expansionZoom =
                        this.clusterIndex.getClusterExpansionZoom(cluster.id as number);

                    this.map.setView([lat, lng], expansionZoom);
                });

            } else {

                const execution = (cluster.properties as any).execution as GeoExecution;

                id = this.getPointKey(execution); // ✅ usa helper correto

                newIds.add(id);
                if (this.markersMap.has(id)) return;

                marker = L.marker([lat, lng], {
                    icon: this.createCustomIcon(execution.type, execution.status)
                });

                marker.bindPopup(this.buildPopup(execution), {
                    className: 'lumos-custom-popup',
                });
            }

            marker.addTo(this.markersLayer);
            this.markersMap.set(id, marker);
        });

        // Remove os que não estão mais visíveis
        for (const [id, marker] of this.markersMap.entries()) {
            if (!newIds.has(id)) {
                this.markersLayer.removeLayer(marker);
                this.markersMap.delete(id);
            }
        }
    }

    private fitToData(): void {
        if (!this._filteredExecutions.length) return;

        const bounds = L.latLngBounds(
            this._filteredExecutions.map(e =>
                [Number(e.lat), Number(e.lng)] as [number, number]
            )
        );

        this.map.fitBounds(bounds.pad(0.2));
    }

    private photoCache = new Map<string, string>();
    private loadPhotoIfNeeded(e: GeoExecution) {

        if (!e.photoUri) return;

        const container = document.getElementById(`photo-container-${e.id}`);
        if (!container) return;

        // 🔥 Se já tiver em cache → usa direto
        if (this.photoCache.has(e.id)) {
            const cachedUrl = this.photoCache.get(e.id)!;
            container.innerHTML = this.buildFinalImageHtml(cachedUrl, e);
            return;
        }

        this.fileService.getPhoto(e.photoUri).subscribe({
            next: (blob: Blob) => {

                const blobUrl = URL.createObjectURL(blob);

                this.photoCache.set(e.id, blobUrl);

                container.innerHTML = this.buildFinalImageHtml(blobUrl, e);
            },
            error: () => {
                container.innerHTML = `
                <div class="w-full h-32 flex items-center justify-center text-xs text-slate-400">
                    Erro ao carregar imagem
                </div>
            `;
            }
        });
    }

    private buildFinalImageHtml(url: string, e: GeoExecution): string {
        return `
        <div class="flex justify-center mt-3">

            <div class="relative max-h-40 overflow-hidden rounded-lg shadow-sm group cursor-pointer"
                 onclick="window.dispatchEvent(new CustomEvent('open-photo', { detail: '${url}' }))">

                ${e.pointNumber ? `
                    <div class="absolute top-2 left-2 z-20 transition-opacity duration-300 group-hover:opacity-0 pointer-events-none">
                        <span class="px-2 py-0.5 text-[10px] font-bold bg-slate-900/70 text-white backdrop-blur-md rounded-md">
                            ID do Ponto ${e.pointNumber}
                        </span>
                    </div>
                ` : ''}

                <img src="${url}"
                     alt="Ponto"
                     class="h-auto max-h-40 object-contain transition-transform duration-300 group-hover:scale-105">

                <!-- Overlay hover -->
                <div class="absolute inset-0 bg-black/40 opacity-0 group-hover:opacity-100 transition-opacity duration-300 flex items-center justify-center">
                    <div class="flex items-center gap-2 text-white text-sm font-medium">
                        <i class="pi pi-search text-2xl"></i>
                    </div>
                </div>

            </div>

        </div>
    `;
    }

    private buildPopup(e: GeoExecution): string {
        return `
    <div
        data-point-key="${this.getPointKey(e)}"
        id="popup-${e.id}"
        class="p-3 min-w-[240px] font-sans text-slate-800 transition-all duration-300">

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

         ${e.photoUri ? `
            <div
                id="photo-container-${e.id}"
                class="img-container relative mt-3 overflow-hidden rounded-lg border border-slate-100 shadow-sm group cursor-pointer transition-all duration-300">

                ${e.pointNumber ? `
                    <div class="absolute top-2 left-2 z-10 transition-opacity duration-300 group-hover:opacity-0 pointer-events-none">
                        <span class="px-2 py-0.5 text-[10px] font-bold bg-slate-900/70 text-white backdrop-blur-md rounded-md">
                            ID do Ponto ${e.pointNumber}
                        </span>
                    </div>
                ` : ''}

                <!-- Skeleton -->
                <div class="w-full h-32 bg-slate-200 animate-pulse"></div>

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
            { id: string; execution: GeoExecution }
        >[] = this._filteredExecutions
            .filter(e => e.lat && e.lng)
            .map(e => ({
                type: 'Feature' as const,
                properties: {
                    id: e.id,
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

    modalImage: string | null = null;

    openModal(url: string) {
        this.modalImage = url;
    }

    closeModal() {
        this.modalImage = null;
    }



}
