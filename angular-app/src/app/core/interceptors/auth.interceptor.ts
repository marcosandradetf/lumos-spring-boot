import {Injectable, Injector} from '@angular/core';
import {HttpErrorResponse, HttpEvent, HttpHandler, HttpInterceptor, HttpRequest} from '@angular/common/http';
import {BehaviorSubject, catchError, filter, Observable, switchMap, take, throwError} from 'rxjs';
import {AuthService} from '../service/auth.service';

@Injectable()
export class AuthInterceptor implements HttpInterceptor {
  private tryingRefreshing = false;
  private refreshTokenSubject: BehaviorSubject<any> = new BehaviorSubject<any>(null);

  constructor(private injector: Injector) { }

  intercept(request: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    const token = this.injector.get(AuthService).getAccessToken();
    const logout = this.injector.get(AuthService).logout();


    request = this.addAuthorization(request, token);
    return next.handle(request).pipe(catchError(error => {
      if (error instanceof HttpErrorResponse && error.status === 401) {
        const tokenExpired = error.headers.get('token-expired');
        if (tokenExpired) {
          return this.handle401Error(request, next);
        }

        logout.subscribe();
        return throwError(() => error);
      } else {
        return throwError(() => error);
      }
    }));
  }

  private handle401Error(request: HttpRequest<any>, next: HttpHandler) {
    if (!this.tryingRefreshing) {
      this.tryingRefreshing = true;
      this.refreshTokenSubject.next(null);

      const refreshToken = this.injector.get(AuthService).refreshToken();

      return refreshToken.pipe(
        switchMap((token: any) => {
          this.tryingRefreshing = false;
          this.refreshTokenSubject.next(token);
          return next.handle(this.addAuthorization(request, token));
        }));

    } else {
      return this.refreshTokenSubject.pipe(
        filter(token => token != null),
        take(1),
        switchMap(jwt => {
          return next.handle(this.addAuthorization(request, jwt));
        }));
    }
  }

  addAuthorization(httpRequest: HttpRequest<any>, token: string) {
    return httpRequest.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`
      },
      withCredentials: true
    });
  }
}