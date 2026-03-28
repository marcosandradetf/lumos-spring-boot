import {Injectable, Injector} from '@angular/core';
import {HttpErrorResponse, HttpEvent, HttpHandler, HttpInterceptor, HttpRequest} from '@angular/common/http';
import {BehaviorSubject, catchError, filter, Observable, switchMap, take, tap, throwError} from 'rxjs';
import {AuthService} from '../auth/auth.service';
import {Router} from '@angular/router';
import {EMPTY} from 'rxjs';


@Injectable()
export class AuthInterceptor implements HttpInterceptor {
    private tryingRefreshing = false;
    private refreshTokenSubject: BehaviorSubject<any> = new BehaviorSubject<any>(null);

    constructor(
        private injector: Injector,
    ) {
    }

    intercept(request: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
        const authService = this.injector.get(AuthService);
        const token = authService.getAccessToken();
        let isAdmin = false;

        if(token) {
            isAdmin = authService.getUser().getRoles().includes('ADMIN');
        }

        const router = this.injector.get(Router);
        let isLoggedIn = false;

        localStorage.removeItem('isLocked');

        // Verifique se a requisição é para o login, por exemplo, verificando a URL
        if (request.url.includes('/login')
            || request.url.includes('/logout')
            || request.url.includes('/refresh-token')
            || request.url.includes('https://servicodados.ibge.gov.br')
            || request.url.includes('/forgot-password')
            || request.url.includes('/public')) {
            // Não adicionar token e não interceptar requisição de login
            return next.handle(request);
        }


        request = this.addAuthorization(request, token, isLoggedIn);
        return next.handle(request).pipe(
            tap(() => isLoggedIn = true),
            catchError(error => {
                if (error instanceof HttpErrorResponse && error.status === 401) {
                    //const tokenExpired = error.headers.get('token-expired');
                    // if (tokenExpired) {
                    //   return this.handle401Error(request, next, isLoggedIn);
                    // }
                    return this.handle401Error(request, next, isLoggedIn);

                    // logout.subscribe();
                    // return throwError(() => error);
                } else if (error instanceof HttpErrorResponse && error.status === 403) {
                    const currentUrl = router.url;

                    // 🚫 não intercepta rotas de billing/status
                    if (
                        currentUrl.startsWith('/cobranca') ||
                        currentUrl.startsWith('/acesso-indisponivel') ||
                        currentUrl.startsWith('/status')
                    ) {
                        return throwError(() => error);
                    }

                    const billingRoutes: Record<string, string> = {
                        TRIAL_EXPIRED: 'teste_finalizado',
                        SUBSCRIPTION_EXPIRED: 'expirado',
                        SUBSCRIPTION_CANCELED: 'cancelado',
                        SUBSCRIPTION_INACTIVE: 'expirado',
                    };
                    const errorCode = error?.error?.code;
                    const motivo = billingRoutes[errorCode as keyof typeof billingRoutes];

                    if(motivo) {
                        localStorage.setItem('isLocked', 'true');
                        if (!isAdmin) {
                            this.navigate(router, '/cobranca', {motivo, action: 'upgrade'});
                        } else {
                            this.navigate(router, '/acesso-indisponivel');
                        }
                    } else {
                        this.navigate(router, '/status/403');
                    }
                    return EMPTY;

                } else {
                    return throwError(() => error);
                }
            })
        );

    }

    private navigate(
        router: Router,
        path: string,
        queryParams?: any
    ) {
        const currentUrl = router.url;

        if (!currentUrl.startsWith(path)) {
            void router.navigate([path], { queryParams });
        }
    }

    private handle401Error(request: HttpRequest<any>, next: HttpHandler, isLoggedIn: boolean) {
        if (!this.tryingRefreshing) {
            this.tryingRefreshing = true;
            this.refreshTokenSubject.next(null);

            const refreshToken = this.injector.get(AuthService).refreshToken();

            return refreshToken.pipe(
                switchMap((token: any) => {
                    this.tryingRefreshing = false;
                    this.refreshTokenSubject.next(token);
                    return next.handle(this.addAuthorization(request, token, isLoggedIn));
                }));

        } else {
            return this.refreshTokenSubject.pipe(
                filter(token => token != null),
                take(1),
                switchMap(jwt => {
                    return next.handle(this.addAuthorization(request, jwt, isLoggedIn));
                }));
        }
    }

    addAuthorization(httpRequest: HttpRequest<any>, token: string, isLoggedIn: boolean) {
        return httpRequest.clone({
            setHeaders: {
                Authorization: `Bearer ${token}`
            },
            withCredentials: true
        });

    }
}
