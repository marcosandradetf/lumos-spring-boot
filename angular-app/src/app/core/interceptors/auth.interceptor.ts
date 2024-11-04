import { Injectable } from '@angular/core';
import {HttpInterceptor, HttpRequest, HttpHandler, HttpEvent, HttpErrorResponse} from '@angular/common/http';
import { AuthService } from '../service/auth.service';
import { Observable, switchMap, catchError, throwError } from 'rxjs';
import {User} from '../../models/user.model';

@Injectable()
export class AuthInterceptor implements HttpInterceptor {
  constructor(private authService: AuthService) {}

  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    // Verifique se o token está expirado e tente renovar
    if (!this.authService.hasTokens()) {
      return this.authService.refreshToken().pipe(
        switchMap(() => {
          // Após a renovação, clone a requisição com o novo token
          const authReq = req.clone({
            setHeaders: { Authorization: `Bearer ${this.authService.getAccessToken()}` }
          });
          return next.handle(authReq);
        }),
        catchError((error) => {
          console.error("Erro ao interceptar e renovar token:", error);
          return throwError(() => error);
        })
      );
    } else {
      // Se o token não expirou, prossiga com a requisição normalmente
      const authReq = req.clone({
        setHeaders: { Authorization: `Bearer ${this.authService.getAccessToken()}` }
      });
      return next.handle(authReq);
    }
  }
}
