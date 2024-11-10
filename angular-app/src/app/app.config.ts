import { ApplicationConfig, provideZoneChangeDetection } from '@angular/core';
import { provideRouter } from '@angular/router';
import { routes } from './app.routes';
import { provideClientHydration } from '@angular/platform-browser';
import {
  HTTP_INTERCEPTORS, HttpHandlerFn, HttpInterceptorFn, HttpRequest,
  provideHttpClient,
  withFetch,
  withInterceptors,
  withInterceptorsFromDi
} from '@angular/common/http';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import {AuthInterceptor} from './core/interceptors/auth.interceptor';
import {AuthService} from './core/auth/auth.service';



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
  ],
};


