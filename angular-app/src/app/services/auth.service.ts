import { Injectable } from '@angular/core';
import {BehaviorSubject, catchError, of, tap} from 'rxjs';
import {HttpClient} from '@angular/common/http';
import { Router } from '@angular/router';
import {User} from '../models/user.model';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private apiUrl = 'http://localhost:8080';
  private user: User | null = null;
  private isLoggedInSubject: BehaviorSubject<boolean> = new BehaviorSubject<boolean>(this.hasTokens());

  constructor(private http: HttpClient, private router: Router) { }

  private hasTokens(): boolean {
    return !!localStorage.getItem("accessToken") && !!localStorage.getItem("refreshToken");
  }

  get isLoggedIn$() {
    return this.isLoggedInSubject.asObservable();
  }

  login(username: string, password: string) {
    return this.http.post<any>('/login', { username, password }).pipe(
      tap(response => {
        this.user = new User(username, response.accessToken, response.refreshToken, response.scopes);
        localStorage.setItem('accessToken', response.accessToken);
        localStorage.setItem('refreshToken', response.refreshToken);
        this.isLoggedInSubject.next(true);
      })
    )
  }

  logout() {
    return this.http.post('/logout', { refreshToken: this.user?.refreshToken }).pipe(
      tap(() => {
        this.user?.clearTokens();
        localStorage.removeItem('accessToken');
        localStorage.removeItem('refreshToken');
        this.isLoggedInSubject.next(false);
        this.router.navigate(['/login']).then(
          (navigated: boolean) => {
            if (navigated) {
              console.log('Navegação para /login realizada com sucesso.');
            } else {
              console.error('Falha na navegação para /login.');
            }
          }
        ).catch(error => {
          console.error('Erro durante a navegação:', error);
        });
      })
    );
  }

  refreshToken() {
    if (!this.user) return of(null);

    return this.http.post<any>('/refresh-token', { refreshToken: this.user.refreshToken }).pipe(
      tap(response => {
        this.user?.setTokens(response.accessToken, this.user.refreshToken);
        localStorage.setItem('accessToken', response.accessToken);
      }),
      catchError(() => {
        this.logout().subscribe(); // Se a renovação falhar, desloga o usuário
        return of(null);
      })
    );
  }

  getAccessToken() {
    return this.user?.accessToken || localStorage.getItem('accessToken');
  }

  // Método para verificar se o usuário possui um papel específico
  hasRole(role: string): boolean {
    return this.user ? this.user.hasRole(role) : false;
  }

}
