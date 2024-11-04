import { Injectable } from '@angular/core';
import {BehaviorSubject, catchError, of, tap} from 'rxjs';
import {HttpClient, HttpHeaders} from '@angular/common/http';
import { Router } from '@angular/router';
import {User} from '../../models/user.model';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  public user: User | null = null;
  private apiUrl = 'http://localhost:8080/api/auth';
  public isLoggedInSubject: BehaviorSubject<boolean> = new BehaviorSubject<boolean>(this.hasTokens());
  public isLoading$ = new BehaviorSubject<boolean>(true); // status de carregamento

  constructor(private http: HttpClient, private router: Router) {
    if (typeof window !== 'undefined' && localStorage) {
      const storedUser = localStorage.getItem('user');
      if (storedUser) {
        const userData = JSON.parse(storedUser);
        this.user = new User(userData.username, userData.accessToken, userData.roles); // Converte para uma instância de User
      }
      this.initializeAuthStatus();
    }
  }

  private initializeAuthStatus() {
    // Simula a verificação do token, por exemplo:
    if (this.user && !this.user.isAccessTokenExpired()) {
      this.isLoggedInSubject.next(true);
    } else {
      this.refreshToken();
    }
    this.isLoading$.next(false); // Atualiza o status de carregamento
  }

  hasTokens(): boolean {
    return this.user ? !this.user.isAccessTokenExpired() : false;
  }

  get isLoggedIn$() {
    return this.isLoggedInSubject.asObservable();
  }

  login(username: string, password: string) { // Retorna um Observable
    return this.http.post<any>(`${this.apiUrl}/login`, { username, password }, { withCredentials: true }).pipe(
      tap(response => {
        this.user = new User(username, response.accessToken, response.scopes);
        localStorage.setItem('user', JSON.stringify(this.user));
        this.isLoggedInSubject.next(true);
      }),
      catchError(error => {
        console.error("Erro no login:", error);
        return of(null); // Retorna um observable nulo em caso de erro
      })
    );
  }

  logout() {
    console.log(localStorage.getItem("refreshToken"));
    return this.http.post(this.apiUrl + '/logout', { }, { withCredentials: true }).pipe(
      tap(() => {
        this.user?.clearToken();
        localStorage.removeItem('user');
        this.isLoggedInSubject.next(false);
      })
    );
  }

  refreshToken() {
    if (!this.user) return of(null);
    return this.http.post<any>(this.apiUrl + '/refresh-token', {}, { withCredentials: true }).pipe(
      tap(response => {
        this.user?.setToken(response.accessToken);
        localStorage.setItem('user', JSON.stringify(this.user));
      }),
      catchError(() => {
        this.logout().subscribe(); // Se a renovação falhar, desloga o usuário
        return of(null);
      })
    );
  }

  setAccessToken(newToken: string) {
    if (this.user) {
      this.user.setToken(newToken);
      localStorage.setItem('user', JSON.stringify(this.user)); // Atualiza o `user` no localStorage
    }
  }

  getAccessToken() {
    if (typeof window !== 'undefined' && localStorage) {
      return this.user?.accessToken;
    }
    return false;
  }

  // Método para verificar se o usuário possui um papel específico
  hasRole(role: string): boolean {
    return this.user ? this.user.hasRole(role) : false;
  }

  public getUser() {
    return this.user;
  }

  getHeaders(): HttpHeaders {
    const token = this.getAccessToken();
    return new HttpHeaders({
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`
    });
  }

}
