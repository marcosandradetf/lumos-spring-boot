import {
    AfterViewInit,
    Component,
    ElementRef,
    ViewChild,
    signal, OnInit
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { GoogleMapsLoaderService } from '../../core/service/google-maps-loader.service';
import {InputGroup} from 'primeng/inputgroup';
import {InputGroupAddon} from 'primeng/inputgroupaddon';
import {InputText} from 'primeng/inputtext';
import {FormsModule} from '@angular/forms';

interface LightPost {
    id: string;
    latitude: number;
    longitude: number;
}

type LocationState = 'checking' | 'idle' | 'loading' | 'map' | 'denied' | 'manual';

@Component({
    selector: 'app-service-request-map',
    standalone: true,
    imports: [CommonModule, ButtonModule, CardModule, ProgressSpinnerModule, InputGroup, InputGroupAddon, InputText, FormsModule],
    templateUrl: './service-request-map.component.html'
})
export class ServiceRequestMapComponent implements AfterViewInit, OnInit {
    @ViewChild('mapContainer') mapRef?: ElementRef<HTMLDivElement>;

    private map?: google.maps.Map;
    private markers: google.maps.marker.AdvancedMarkerElement[] = [];
    private debounceTimer?: ReturnType<typeof setTimeout>;
    private lastLoadedBounds?: google.maps.LatLngBounds;

    // Estado visual da tela
    state = signal<LocationState>('idle');

    // Dados da interação
    userPosition = signal<{ lat: number; lng: number } | null>(null);
    selectedPosition = signal<{ lat: number; lng: number } | null>(null);
    nearestPost = signal<LightPost | null>(null);
    visiblePostsCount = signal(0);

    // Mock local para protótipo
    private allPosts: LightPost[] = this.generateMockPosts();

    constructor(private mapsLoader: GoogleMapsLoaderService) {}

    async ngAfterViewInit(): Promise<void> {
        // Não carregar o mapa automaticamente
    }

    async ngOnInit() {
        this.state.set('checking');

        try {
            const position = await this.getUserLocation();

            this.userPosition.set({
                lat: position.coords.latitude,
                lng: position.coords.longitude
            });

            this.state.set('map');

            setTimeout(() => {
                this.initializeMap(this.userPosition()!);
            });

        } catch {
            this.state.set('denied');
        }
    }

    // Inicia o fluxo principal usando geolocalização
    async startWithLocation(): Promise<void> {
        this.state.set('loading');

        try {
            const position = await this.getUserLocation();

            const coords = {
                lat: position.coords.latitude,
                lng: position.coords.longitude
            };

            this.userPosition.set(coords);
            this.state.set('map');

            // Aguarda o Angular renderizar o container do mapa
            setTimeout(async () => {
                await this.initializeMap(coords);
            }, 0);
        } catch {
            this.state.set('idle');
            alert('Não foi possível obter sua localização. Verifique a permissão do navegador.');
        }
    }

    // Permite abrir o mapa mesmo sem localização
    async openMapWithAddress(): Promise<void> {
        this.state.set('map');

        const defaultCenter = { lat: -18.9186, lng: -48.2772 };

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
            return;
        }

        const { Map } = await this.mapsLoader.importMapsLibraries();

        this.map = new Map(this.mapRef.nativeElement, {
            center,
            zoom: 17,
            clickableIcons: false,
            streetViewControl: true,
            mapTypeControl: false,
            fullscreenControl: false,
            gestureHandling: 'greedy'
        });

        this.setupMapEvents();
        this.handleBoundsChange();
    }

    // Eventos principais do mapa
    private setupMapEvents(): void {
        if (!this.map) return;

        // Clique/tap no mapa para selecionar o local do chamado
        this.map.addListener('click', (event: google.maps.MapMouseEvent) => {
            if (!event.latLng) return;

            const selected = {
                lat: event.latLng.lat(),
                lng: event.latLng.lng()
            };

            this.selectedPosition.set(selected);
            this.nearestPost.set(this.findNearestPost(selected.lat, selected.lng));
        });

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

        const { AdvancedMarkerElement } = await this.mapsLoader.importMapsLibraries();

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

            marker.addListener('click', () => {
                this.selectedPosition.set({
                    lat: post.latitude,
                    lng: post.longitude
                });
                this.nearestPost.set(post);
            });

            this.markers.push(marker);
        }
    }

    // Encontra o poste mais próximo ao ponto selecionado
    private findNearestPost(lat: number, lng: number): LightPost | null {
        let minDistance = Infinity;
        let closest: LightPost | null = null;

        for (const post of this.allPosts) {
            const distance = Math.sqrt(
                Math.pow(post.latitude - lat, 2) + Math.pow(post.longitude - lng, 2)
            );

            if (distance < minDistance) {
                minDistance = distance;
                closest = post;
            }
        }

        return closest;
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

    selectedCity = 'Belo Horizonte';

    protected startFromAddress() {

    }

    protected retryLocation() {

    }
}
