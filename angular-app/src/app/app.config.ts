import { ApplicationConfig, provideZoneChangeDetection } from '@angular/core';
import { provideRouter } from '@angular/router';
import { routes } from './app.routes';
import { provideClientHydration } from '@angular/platform-browser';
import {
  HTTP_INTERCEPTORS, provideHttpClient,
  withFetch,
  withInterceptorsFromDi
} from '@angular/common/http';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import {AuthInterceptor} from './core/interceptors/auth.interceptor';
import {LOCALE_ID, DEFAULT_CURRENCY_CODE} from '@angular/core';
import localePt from '@angular/common/locales/pt';
import {registerLocaleData} from '@angular/common';
import {providePrimeNG} from 'primeng/config';
import Aura from '@primeng/themes/aura';


registerLocaleData(localePt, 'pt');
export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes), provideClientHydration(),
    provideClientHydration(),
    provideHttpClient(
      withFetch(),
      withInterceptorsFromDi()
    ),
    provideAnimationsAsync(),
    {provide: HTTP_INTERCEPTORS, useClass: AuthInterceptor, multi: true},
    {
      provide: LOCALE_ID,
      useValue: 'pt'
    },

    /* if you don't provide the currency symbol in the pipe,
    this is going to be the default symbol (R$) ... */
    {
      provide:  DEFAULT_CURRENCY_CODE,
      useValue: 'BRL'
    },

    provideAnimationsAsync(),
    providePrimeNG({
      theme: {
        preset: Aura
      }
    })
  ],
};


