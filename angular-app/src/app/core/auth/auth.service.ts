import { Injectable } from '@angular/core';
import {BehaviorSubject, catchError, map, Observable, of, tap} from 'rxjs';
import {HttpClient, HttpHeaders} from '@angular/common/http';
import { Router } from '@angular/router';
import {User} from '../models/user.model';
import {routes} from '../../app.routes';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private apiUrl = 'http://localhost:8080/api/auth';
  public isLoggedInSubject: BehaviorSubject<boolean> = new BehaviorSubject<boolean>(this.hasTokens());
  public isLoading$ = new BehaviorSubject<boolean>(true); // status de carregamento

  constructor(private http: HttpClient, private router: Router, public user: User) {
    if (typeof window !== 'undefined' && localStorage) {
      const storedUser = localStorage.getItem('user');
      if (storedUser) {
        const userData = JSON.parse(storedUser);
        this.user.initialize(userData.username, userData.accessToken, userData.roles); // Converte para uma instância de User
      }
      this.initializeAuthStatus();
    }
  }

  private initializeAuthStatus() {
    // Simula a verificação do token, por exemplo:
    //if (this.user && !this.user.isAccessTokenExpired()) {
    if (this.user.accessToken) {
      this.isLoggedInSubject.next(true);
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
        this.user.initialize(username, response.accessToken, response.scopes);
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
    return this.http.post(this.apiUrl + '/logout', {}, { withCredentials: true }).pipe(
      tap(() => {
        this.user?.clearToken();
        localStorage.removeItem('user');
        this.isLoggedInSubject.next(false);
      })
    );
  }

  refreshToken(): Observable<string | null> {
    console.log('atualizando token auto');
    if (!this.user) {
      console.warn("Usuário não encontrado, renovação de token cancelada.");
      return of(null); // Retorna Observable nulo se não houver usuário logado
    }

    return this.http.post<{ accessToken: string }>(`${this.apiUrl}/refresh-token`, {}, {withCredentials: true}).pipe(
      map(response => {
        if (response && response.accessToken) {
          this.user?.setToken(response.accessToken); // Atualiza o token do usuário
          localStorage.setItem('user', JSON.stringify(this.user)); // Salva no localStorage
          return response.accessToken; // Retorna o novo token
        } else {
          console.warn("Nenhum token de acesso retornado na resposta.");
          return null;
        }
      }),
      catchError(error => {
        console.error("Erro ao tentar renovar o token:", error);
        // O AuthInterceptor pode decidir o que fazer ao receber null
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
      return this.user.accessToken;
  }

  // Método para verificar se o usuário possui um papel específico
  hasRole(role: string): boolean {
    return this.user ? this.user.hasRole(role) : false;
  }

  public getUser() {
    return this.user;
  }

}
