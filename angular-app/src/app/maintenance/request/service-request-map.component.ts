import {
    AfterViewInit,
    Component,
    ElementRef,
    ViewChild,
    signal, OnInit, OnDestroy
} from '@angular/core';
import {CommonModule} from '@angular/common';
import {ButtonModule} from 'primeng/button';
import {CardModule} from 'primeng/card';
import {ProgressSpinnerModule} from 'primeng/progressspinner';
import {GoogleMapsLoaderService} from '../../core/service/google-maps-loader.service';
import {InputGroup} from 'primeng/inputgroup';
import {InputGroupAddon} from 'primeng/inputgroupaddon';
import {InputText} from 'primeng/inputtext';
import {FormsModule} from '@angular/forms';
import {ActivatedRoute} from '@angular/router';
import {LoadingOverlayComponent} from '../../shared/components/loading-overlay/loading-overlay.component';
import {IbgeService} from '../../core/service/ibge.service';
import {ufRequest} from '../../core/uf-request.dto';
import {citiesRequest} from '../../core/cities-request.dto';
import {Select} from 'primeng/select';
import {ServiceRequestService} from './service-request.service';

interface LightPost {
    id: string;
    latitude: number;
    longitude: number;
}

type LocationState =
    'checking'
    | 'idle'
    | 'loading'
    | 'map'
    | 'denied'
    | 'manual'
    | 'auto'
    | 'searching'
    | 'verification';

type PlatformType = 'ios' | 'android' | 'windows' | 'mac' | 'other';
type BrowserType = 'safari' | 'chrome' | 'edge' | 'firefox' | 'other';

export interface SelectedPoint {
    id: string;
    lat: number;
    lng: number;
    pointId?: string;
    issue: string | null;
    notes?: string;
}

interface MaintenanceFormModel {
    pointId: string;
    issue: string | null;
    typeOption: string | null;
    notes: string;
    lat: number | null;
    lng: number | null;
}

@Component({
    selector: 'app-service-request-map',
    standalone: true,
    imports: [CommonModule, ButtonModule, CardModule, ProgressSpinnerModule, InputGroup, InputGroupAddon, InputText, FormsModule, LoadingOverlayComponent, Select],
    templateUrl: './service-request-map.component.html'
})
export class ServiceRequestMapComponent implements OnInit, OnDestroy {
    @ViewChild('mapContainer') mapRef?: ElementRef<HTMLDivElement>;

    private map?: google.maps.Map;
    private markers: google.maps.marker.AdvancedMarkerElement[] = [];
    private debounceTimer?: ReturnType<typeof setTimeout>;
    private lastLoadedBounds?: google.maps.LatLngBounds;
    private watchId?: number;
    maintenanceSheetOpen = signal(false);
    maintenanceFormError = signal<string | null>(null);
    temporaryMarker?: google.maps.marker.AdvancedMarkerElement;

    // Estado visual da tela
    state = signal<LocationState>('idle');

    // Dados da interação
    userPosition = signal<{ lat: number; lng: number } | null>(null);
    selectedPosition = signal<{ lat: number; lng: number } | null>(null);
    selectedPositions = signal<SelectedPoint[]>([]);
    nearestPost = signal<LightPost | null>(null);
    visiblePostsCount = signal(0);

    selectedIbgeCode= signal<string | null>(null);
    selectedCity= signal<string | null>(null);
    mode = signal<'manual' | 'auto' | 'round'>('auto');

    platform = signal<PlatformType>('other');
    browser = signal<BrowserType>('other');

    // Mock local para protótipo
    private allPosts: LightPost[] = this.generateMockPosts();

    typeOptions = [
        { value: 'LED' },
        { value: 'Lâmpada' },
        { value: 'Não identificado' },
        { value: 'Outros' },
    ] as const;

    issueOptions = [
        { value: 'Apagado' },
        { value: 'Aceso de dia' },
        { value: 'Piscando' },
        { value: 'Danificado' },
        { value: 'Acesso prejudicado' },
        { value: 'Outros' }
    ] as const;

    maintenanceForm: MaintenanceFormModel = {
        pointId: '',
        typeOption: null,
        issue: null,
        notes: '',
        lat: null,
        lng: null
    };

    constructor(
        private mapsLoader: GoogleMapsLoaderService,
        private route: ActivatedRoute,
        private ibgeService: IbgeService,
        private service: ServiceRequestService,
    ) {
    }


    ibgeStateError = signal(false);
    ibgeCitiesError = signal(false);
    loadingCities = signal(false);
    loadingContracts = signal(false);
    uf: any;
    ufData = signal<ufRequest[]>([]);
    citiesData = signal<citiesRequest[]>([])
    async ngOnInit() {
        const mode = this.route.snapshot.data['mode'];

        if (mode) {
            this.setMode(mode);
        }

        this.platform.set(this.detectPlatform());
        this.browser.set(this.detectBrowser());
        this.state.set('verification');
        this.ibgeService.getUfs().subscribe({
            next: data => {
                this.ufData.set(data);
            },
            error: () => {
                this.ibgeStateError.set(true);
            }
        });
    }

    getCities() {
        if (!this.uf) return;

        this.loadingCities.set(true);
        this.ibgeService.getCities(this.uf).subscribe({
            next: data => {
                this.citiesData.set(data);
                this.loadingCities.set(false);
            },
            error: () => {
                this.ibgeCitiesError.set(true);
                this.loadingCities.set(false);
            }
        });
    }

    async initOperation() {
        this.state.set('checking');
        await this.tryGetLocation();
    }

    // Permite abrir o mapa mesmo sem localização
    async openMapWithAddress(): Promise<void> {
        this.state.set('map');

        const defaultCenter = {lat: -18.9186, lng: -48.2772};

        setTimeout(async () => {
            await this.initializeMap(this.userPosition() ?? defaultCenter);
        }, 0);
    }

    // Geolocalização funciona em celular e também no desktop, se o navegador permitir
    private getUserLocation(): Promise<GeolocationPosition> {
        return new Promise((resolve, reject) => {
            if (!navigator.geolocation) {
                reject(new Error('Geolocalização não suportada'));
                return;
            }

            navigator.geolocation.getCurrentPosition(resolve, reject, {
                enableHighAccuracy: true,
                timeout: 10000,
                maximumAge: 30000
            });
        });
    }


    // Inicializa o mapa apenas uma vez
    private async initializeMap(center: { lat: number; lng: number }): Promise<void> {
        if (!this.mapRef?.nativeElement) return;

        if (this.map) {
            this.map.setCenter(center);
            google.maps.event.trigger(this.map, 'resize');
            this.handleBoundsChange();

            // 🔥 garante comportamento correto ao trocar modo
            this.handleModeAfterMapReady();

            return;
        }

        const {Map} = await this.mapsLoader.importMapsLibraries();

        this.map = new Map(this.mapRef.nativeElement, {
            center,
            zoom: 17,
            clickableIcons: false,
            streetViewControl: false,
            mapTypeControl: false,
            fullscreenControl: false,
            cameraControl: false,
            gestureHandling: 'greedy',
            mapId: 'd6b3107bf9c324459fa898a5',
        });

        this.setupMapEvents();
        this.handleBoundsChange();
        this.handleModeAfterMapReady();
    }

    private handleModeAfterMapReady() {
        const mode = this.mode();

        // 🛑 sempre limpa antes
        this.stopRondaMode();

        if (mode === 'round') {
            this.startRondaMode();
        }
    }

    // Eventos principais do mapa
    private setupMapEvents(): void {
        if (!this.map) return;

        // Clique/tap no mapa para selecionar o local do chamado


        // Sempre que o usuário parar de mover o mapa, atualiza os pontos visíveis
        this.map.addListener('idle', () => {
            this.handleBoundsChange();
        });
    }

    // Debounce para evitar excesso de processamento
    private handleBoundsChange(): void {
        if (!this.map) return;

        if (this.debounceTimer) {
            clearTimeout(this.debounceTimer);
        }

        this.debounceTimer = setTimeout(() => {
            this.loadPostsByBounds();
        }, 300);
    }

    // Carrega apenas os postes da área visível
    private loadPostsByBounds(): void {
        if (!this.map) return;

        const bounds = this.map.getBounds();
        if (!bounds) return;

        // Evita recarga desnecessária quando o centro ainda está dentro da última área carregada
        if (this.lastLoadedBounds && this.lastLoadedBounds.contains(bounds.getCenter())) {
            return;
        }

        this.lastLoadedBounds = bounds;

        const visiblePosts = this.allPosts.filter((post) =>
            bounds.contains(new google.maps.LatLng(post.latitude, post.longitude))
        );

        this.visiblePostsCount.set(visiblePosts.length);
        void this.renderMarkers(visiblePosts);
    }

    // Renderiza os marcadores visíveis
    private async renderMarkers(posts: LightPost[]): Promise<void> {
        if (!this.map) return;

        const {AdvancedMarkerElement} = await this.mapsLoader.importMapsLibraries();

        for (const marker of this.markers) {
            marker.map = null;
        }
        this.markers = [];

        for (const post of posts) {
            const marker = new AdvancedMarkerElement({
                map: this.map,
                position: {
                    lat: post.latitude,
                    lng: post.longitude
                },
                title: `Poste ${post.id}`
            });


            this.markers.push(marker);
        }
    }

    // Ação mock do chamado
    submitServiceRequest(): void {
        const selected = this.selectedPosition();

        if (!selected) {
            alert('Selecione um ponto no mapa antes de abrir o chamado.');
            return;
        }

        const nearest = this.nearestPost();

        console.log('Chamado mock criado', {
            latitude: selected.lat,
            longitude: selected.lng,
            nearestPostId: nearest?.id ?? null,
            problemType: 'Luminária queimada'
        });

        alert('Chamado mock criado com sucesso.');
    }

    // Mock de postes espalhados na região
    protected address?: string;

    private generateMockPosts(): LightPost[] {
        const posts: LightPost[] = [];

        const baseLat = -19.9167; // Belo Horizonte
        const baseLng = -43.9345;

        for (let i = 0; i < 500; i++) {
            posts.push({
                id: String(i),
                latitude: baseLat + (Math.random() - 0.5) * 0.02,
                longitude: baseLng + (Math.random() - 0.5) * 0.02
            });
        }

        return posts;
    }

    protected startFromAddress() {

    }

    async tryGetLocation() {
        this.state.set('checking');

        try {
            const position = await this.getUserLocation();

            this.userPosition.set({
                lat: position.coords.latitude,
                lng: position.coords.longitude
            });

            this.state.set('map');

            setTimeout(async () => {
                await this.initializeMap(this.userPosition()!);
            });
        } catch (error: any) {
            if (this.mode() === 'round') {
                this.state.set('denied');
            } else {
                this.state.set('manual');
            }
        }
    }


    protected setMode(mode: 'manual' | 'auto' | 'round') {
        this.mode.set(mode);
    }

    startRondaMode() {
        if (!navigator.geolocation) {
            console.warn('Geolocalização não suportada');
            return;
        }

        this.stopRondaMode(); // evita duplicar

        this.watchId = navigator.geolocation.watchPosition(
            (position) => {
                const coords = {
                    lat: position.coords.latitude,
                    lng: position.coords.longitude
                };

                this.userPosition.set(coords);

                // 🔥 move o mapa suavemente
                this.map?.panTo(coords);
            },
            (error) => {
                console.error('Erro ao obter localização:', error);
            },
            {
                enableHighAccuracy: true,
                maximumAge: 2000,
                timeout: 10000
            }
        );
    }

    stopRondaMode() {
        if (this.watchId !== undefined) {
            navigator.geolocation.clearWatch(this.watchId);
            this.watchId = undefined;
        }
    }

    coords: { lat: number, lng: number } | null = null;
    async captureCurrentPosition() {
        if (!this.map) return;

        const center = this.map.getCenter();
        if (!center) return;

        this.coords = {
            lat: center.lat(),
            lng: center.lng()
        };

        const nearest = this.findNearestPost();

        if (!this.map) return;

        const {AdvancedMarkerElement} =
            await google.maps.importLibrary('marker') as google.maps.MarkerLibrary;

        this.temporaryMarker = new AdvancedMarkerElement({
            map: this.map,
            position: this.coords,
            title: 'Novo ponto'
        });

        this.openMaintenanceSheet(nearest?.id);
    }

    private selectedMarkers = new Map<string, google.maps.marker.AdvancedMarkerElement>();
    private activeInfoWindow?: google.maps.InfoWindow;

    private async openReadOnlyPointInfo(pointId: string): Promise<void> {
        if (!this.map) return;

        const point = this.selectedPositions().find(p => p.id === pointId);
        const marker = this.selectedMarkers.get(pointId);

        if (!point || !marker) return;

        const {InfoWindow} =
            await google.maps.importLibrary('maps') as google.maps.MapsLibrary;

        if (this.activeInfoWindow) {
            this.activeInfoWindow.close();
        }

        const container = document.createElement('div');
        container.style.minWidth = '220px';
        container.innerHTML = `
        <div style="display:flex;flex-direction:column;gap:6px;">
            <strong>Ponto marcado</strong>
            <div><b>ID do ponto:</b> ${point.pointId ?? 'Não informado'}</div>
            <div><b>Comentário:</b> ${point.notes}</div>
            <div><b>Lat:</b> ${point.lat.toFixed(6)}</div>
            <div><b>Lng:</b> ${point.lng.toFixed(6)}</div>
        </div>
    `;

        const infoWindow = new InfoWindow({
            content: container
        });

        this.activeInfoWindow = infoWindow;

        infoWindow.open({
            map: this.map,
            anchor: marker
        });
    }

    recenterMap() {
        const position = this.userPosition();

        if (!position || !this.map) return;

        this.map.panTo(position);
    }

    findNearestPost(): LightPost | null {
        if (!this.allPosts.length || !this.coords) return null;

        let minDist = Infinity;
        let closest: LightPost | null = null;

        for (const post of this.allPosts) {
            const dist = Math.sqrt(
                Math.pow(post.latitude - this.coords.lat, 2) +
                Math.pow(post.longitude - this.coords.lng, 2)
            );

            if (dist < minDist) {
                minDist = dist;
                closest = post;
            }
        }

        return closest;
    }

    showSuccessFeedback() {
        // simples por enquanto
        alert('Chamado registrado com sucesso!');
    }

    ngOnDestroy() {
        this.stopRondaMode();
    }

    setupMapInteraction() {
        this.map?.addListener('dragstart', () => {
            this.stopRondaMode();
        });
    }


    private detectPlatform(): PlatformType {
        const ua = navigator.userAgent || '';
        const platform = navigator.platform || '';

        const isIOS =
            /iPhone|iPad|iPod/i.test(ua) ||
            (platform === 'MacIntel' && navigator.maxTouchPoints > 1);

        if (isIOS) return 'ios';
        if (/Android/i.test(ua)) return 'android';
        if (/Win/i.test(platform)) return 'windows';
        if (/Mac/i.test(platform)) return 'mac';

        return 'other';
    }

    private detectBrowser(): BrowserType {
        const ua = navigator.userAgent || '';

        const isEdge = /Edg/i.test(ua);
        const isFirefox = /Firefox|FxiOS/i.test(ua);
        const isChrome = /Chrome|CriOS/i.test(ua) && !isEdge;
        const isSafari = /Safari/i.test(ua) && !isChrome && !isEdge && !isFirefox;

        if (isSafari) return 'safari';
        if (isChrome) return 'chrome';
        if (isEdge) return 'edge';
        if (isFirefox) return 'firefox';

        return 'other';
    }

    get locationHelpTitle(): string {
        const platform = this.platform();
        const browser = this.browser();

        if (platform === 'ios' && browser === 'safari') {
            return 'Como ativar a localização no Safari';
        }

        if (platform === 'ios' && browser === 'chrome') {
            return 'Como ativar a localização no Chrome';
        }

        if (platform === 'android' && browser === 'chrome') {
            return 'Como ativar a localização no Chrome';
        }

        if (platform === 'windows' && browser === 'chrome') {
            return 'Como ativar a localização no Chrome';
        }

        if (platform === 'windows' && browser === 'edge') {
            return 'Como ativar a localização no Edge';
        }

        if (platform === 'mac' && browser === 'safari') {
            return 'Como ativar a localização no Safari';
        }

        if (platform === 'mac' && browser === 'chrome') {
            return 'Como ativar a localização no Chrome';
        }

        return 'Como ativar a localização';
    }

    get locationHelpSteps(): string[] {
        const platform = this.platform();
        const browser = this.browser();

        if (platform === 'ios' && browser === 'safari') {
            return [
                'Abra os Ajustes do iPhone.',
                'Vá em Privacidade e Segurança > Serviços de Localização > Safari Websites.',
                'Selecione "Durante o Uso do App".',
                'Se a localização do iPhone já estiver ativa, toque no botão à esquerda da barra de endereço e revise a permissão de localização deste site.',
                'Depois, volte para a página e toque em "Tentar novamente".'
            ];
        }

        if (platform === 'ios' && browser === 'chrome') {
            return [
                'Abra os Ajustes do iPhone.',
                'Vá em Privacidade e Segurança > Serviços de Localização > Chrome.',
                'Selecione "Durante o Uso do App".',
                'Se a localização do iPhone já estiver ativa, toque no último botão da barra inferior e revise a permissão de localização deste site.',
                'Depois, volte para a página e toque em "Tentar novamente".'
            ];
        }

        if (platform === 'android' && browser === 'chrome') {
            return [
                'Verifique se a localização do aparelho está ativada.',
                'No Chrome, toque no ícone ao lado da barra de endereço.',
                'Abra as permissões do site e permita o acesso à localização.',
                'Se necessário, revise também a permissão do navegador nas configurações do Android.',
                'Depois, volte para a página e toque em "Tentar novamente".'
            ];
        }

        if (platform === 'android' && browser === 'edge') {
            return [
                'Verifique se a localização do aparelho está ativada.',
                'No Edge, toque no ícone ao lado da barra de endereço.',
                'Abra as permissões do site e permita o acesso à localização.',
                'Se necessário, revise também a permissão do navegador nas configurações do Android.',
                'Depois, volte para a página e toque em "Tentar novamente".'
            ];
        }

        if (platform === 'windows' && browser === 'chrome') {
            return [
                'Verifique se a localização do Windows está ativada.',
                'No Chrome, clique no ícone ao lado da barra de endereço.',
                'Em localização, selecione "Permitir" para este site.',
                'Depois, recarregue a página ou clique em "Tentar novamente".'
            ];
        }

        if (platform === 'windows' && browser === 'edge') {
            return [
                'Verifique se a localização do Windows está ativada.',
                'No Edge, clique no ícone ao lado da barra de endereço.',
                'Permita o acesso à localização para este site.',
                'Depois, recarregue a página ou clique em "Tentar novamente".'
            ];
        }

        if (platform === 'mac' && browser === 'safari') {
            return [
                'Abra os Ajustes do Sistema do Mac.',
                'Vá em Privacidade e Segurança > Serviços de Localização.',
                'Confirme que o Safari pode acessar sua localização.',
                'Revise também a permissão deste site no navegador.',
                'Depois, volte para a página e toque em "Tentar novamente".'
            ];
        }

        if (platform === 'mac' && browser === 'chrome') {
            return [
                'Abra os Ajustes do Sistema do Mac.',
                'Vá em Privacidade e Segurança > Serviços de Localização.',
                'Confirme que o Chrome pode acessar sua localização.',
                'Revise também a permissão deste site no navegador.',
                'Depois, volte para a página e toque em "Tentar novamente".'
            ];
        }

        return [
            'Verifique se a localização do dispositivo está ativada.',
            'Confirme se o navegador e este site têm permissão para acessar sua localização.',
            'Depois, volte para a página e tente novamente.'
        ];
    }

    protected streetView() {
        if (!this.map) return;

        const center = this.map.getCenter();
        if (!center) return;

        const panorama = this.map.getStreetView();

        panorama.setPosition({lat: center.lat(), lng: center.lng()});
        panorama.setPov({
            heading: 120,
            pitch: 0,
        });
        panorama.setVisible(true);
    }

    checkContractMessage = signal<string | null>(null);
    contractId = signal<number | null>(null);
    verifyContracts() {
        if (!this.selectedIbgeCode()) return;

        console.log(this.selectedIbgeCode());
        this.selectedCity.set(this.citiesData().find(c => c.id === this.selectedIbgeCode())?.nome ?? '');
        console.log(this.selectedCity());

        this.loadingContracts.set(true);
        this.service.hasContractActive(
            this.selectedIbgeCode()!
        ).subscribe({
            next: data => {
                if(data.hasValidContract) {
                    this.checkContractMessage.set(null);
                    this.contractId.set(data.contractId);
                    this.initOperation();
                } else {
                    this.checkContractMessage.set(
                        'No momento, este município não possui atendimento ativo de iluminação pública no sistema. Para mais informações, verifique diretamente com o órgão público responsável.'
                    );
                }
                this.loadingContracts.set(false);
            },
            error: err => {
                this.checkContractMessage.set(
                    "Algo deu errado! Tente novamente mais tarde."
                );
                this.loadingContracts.set(false);

                this.contractId.set(1); // apagar
                this.initOperation(); // debug
            }
        });
    }

    openMaintenanceSheet(
        suggestedPointId?: string
    ): void {
        if(!this.coords) return;

        this.maintenanceFormError.set(null);
        this.maintenanceForm = {
            pointId: suggestedPointId ?? '',
            typeOption: null,
            issue: null,
            notes: '',
            lat: this.coords.lat,
            lng: this.coords.lng
        };
        this.maintenanceSheetOpen.set(true);
    }

    closeMaintenanceSheet(): void {
        this.maintenanceSheetOpen.set(false);
        this.maintenanceFormError.set(null);

        if (this.temporaryMarker) {
            this.temporaryMarker.map = null;
            this.temporaryMarker = undefined;
        }
    }

    canSaveMaintenanceForm(): boolean {
        return !!this.maintenanceForm.issue;
    }

    saveMaintenancePoint(): void {
        if (!this.maintenanceForm.issue) {
            this.maintenanceFormError.set('Selecione a situação.');
            return;
        }

        if (this.maintenanceForm.issue === 'outros' && !this.maintenanceForm.notes.trim()) {
            this.maintenanceFormError.set('Descreva rapidamente a situação em observação.');
            return;
        }

        const point: SelectedPoint = {
            id: crypto.randomUUID(),
            lat: this.maintenanceForm.lat!,
            lng: this.maintenanceForm.lng!,
            pointId: this.maintenanceForm.pointId.trim() || undefined,
            issue: this.maintenanceForm.issue,
            notes: this.maintenanceForm.notes.trim() || undefined,
        };

        this.selectedPositions.set([
            ...this.selectedPositions(),
            point
        ]);

        if (this.temporaryMarker) {
            this.selectedMarkers.set(point.id, this.temporaryMarker);
            this.temporaryMarker.title = point.pointId
                ? `Ponto ${point.pointId}`
                : 'Ponto marcado';

            this.temporaryMarker.addListener('click', () => {
                this.openReadOnlyPointInfo(point.id);
            });

            this.temporaryMarker = undefined;
        }

        this.maintenanceSheetOpen.set(false);
        this.maintenanceFormError.set(null);
    }


}
