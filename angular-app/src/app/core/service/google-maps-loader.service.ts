import { Injectable } from '@angular/core';
import { environment } from '../../../environments/environment';

@Injectable({
    providedIn: 'root'
})
export class GoogleMapsLoaderService {

    private loaded = false;
    private loadingPromise?: Promise<void>;

    /**
     * Carrega o script do Google Maps apenas uma vez
     */
    load(): Promise<void> {
        if (this.loaded) {
            return Promise.resolve();
        }

        if (this.loadingPromise) {
            return this.loadingPromise;
        }

        this.loadingPromise = new Promise((resolve, reject) => {
            const existingScript = document.getElementById('google-maps-script');

            if (existingScript) {
                this.loaded = true;
                resolve();
                return;
            }

            const script = document.createElement('script');

            script.id = 'google-maps-script';
            script.src = `https://maps.googleapis.com/maps/api/js?key=${environment.googleMapsApiKey}&v=weekly`;
            script.async = true;
            script.defer = true;

            script.onload = () => {
                this.loaded = true;
                resolve();
            };

            script.onerror = (error) => {
                reject('❌ Erro ao carregar Google Maps: ' + error);
            };

            document.head.appendChild(script);
        });

        return this.loadingPromise;
    }

    /**
     * Importa bibliotecas principais (maps + marker)
     */
    async importMapsLibraries() {
        await this.load();

        const mapsLib = await google.maps.importLibrary('maps') as google.maps.MapsLibrary;
        const markerLib = await google.maps.importLibrary('marker') as google.maps.MarkerLibrary;

        return {
            Map: mapsLib.Map,
            InfoWindow: mapsLib.InfoWindow,
            StreetViewPanorama: google.maps.StreetViewPanorama,
            AdvancedMarkerElement: markerLib.AdvancedMarkerElement
        };
    }

    /**
     * Importa Places sob demanda (evita custo desnecessário)
     */
    async importPlacesLibrary() {
        await this.load();

        return await google.maps.importLibrary('places') as google.maps.PlacesLibrary;
    }

    /**
     * Importa Geocoding (caso precise depois)
     */
    async importGeocodingLibrary() {
        await this.load();

        return await google.maps.importLibrary('geocoding') as google.maps.GeocodingLibrary;
    }
}
