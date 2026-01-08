import {Injectable} from '@angular/core';
import {BehaviorSubject, catchError, map, Observable, of, tap} from 'rxjs';
import {HttpClient, HttpHeaders} from '@angular/common/http';
import {Router} from '@angular/router';
import {User} from '../../models/user.model';
import {routes} from '../../app.routes';
import {environment} from '../../../environments/environment';
import {jwtDecode} from 'jwt-decode';

export interface DecodedToken {
  iss: string;
  sub: string;
  scope: string;
  jti: string;
  username: string;
  email: string;
  fullname: string;
  tenant: string;
  iat: number;
  exp: number;
}


@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private apiUrl = environment.springboot + "/api/auth";
  public isLoggedInSubject: BehaviorSubject<boolean> = new BehaviorSubject<boolean>(this.hasTokens());
  public isLoading$ = new BehaviorSubject<boolean>(true); // status de carregamento

  constructor(private http: HttpClient, private router: Router, public user: User) {
    if (typeof window !== 'undefined' && window.localStorage) {
      const storedUser = localStorage.getItem('user');
      if (storedUser) {
        const userData = JSON.parse(storedUser);
        const roles: string[] = userData.roles;
        const relatedNotificationTopics: string[] = userData.relatedNotificationTopics;

        this.user.initialize(
          userData.uuid,
          userData.username,
          userData.accessToken,
          roles,
          userData.fullName,
          userData.tenant,
          relatedNotificationTopics,
          userData.email
        ); // Converte para uma instância de User
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
    return this.http.post<any>(`${this.apiUrl}/login`, {username, password}, {withCredentials: true}).pipe(
      tap(response => {
        const decodedToken = jwtDecode<DecodedToken>(response.accessToken);

        const userId = decodedToken.sub;
        const roles = decodedToken.scope.split(' ');
        const fullName = decodedToken.fullname;
        const email = decodedToken.email;
        const tenant = decodedToken.tenant;
        const usernameDecoded = decodedToken.username;

        this.user.initialize(
          userId,
          usernameDecoded,
          response.accessToken,
          roles,
          fullName,
          tenant,
          [],
          email
        );
        if (typeof window !== 'undefined' && window.localStorage) localStorage.setItem('user', JSON.stringify(this.user));
        this.isLoggedInSubject.next(true);
      }),
      catchError(error => {
        throw error;
      })
    );
  }

  logout() {
    return this.http.post(this.apiUrl + '/logout', {}, {withCredentials: true}).pipe(
      tap(() => {
        this.user?.clearToken();
        if (typeof window !== 'undefined' && window.localStorage) localStorage.removeItem('user');
        this.isLoggedInSubject.next(false);
        this.router.navigate(['/auth/login']);
        this.isLoading$.next(false);
      }), catchError(error => {
        console.error("Erro no logout:", error);
        this.user?.clearToken();
        if (typeof window !== 'undefined' && window.localStorage) localStorage.removeItem('user');
        this.isLoggedInSubject.next(false);
        this.isLoading$.next(false);
        window.location.reload();
        return of(null); // Retorna um observable nulo em caso de erro
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
          if (typeof window !== 'undefined' && window.localStorage) localStorage.setItem('user', JSON.stringify(this.user)); // Salva no localStorage
          return response.accessToken; // Retorna o novo token
        } else {
          console.warn("Nenhum token de acesso retornado na resposta.");
          return null;
        }
      }),
      catchError(error => {
        console.error("Erro ao tentar renovar o token:", error);
        this.logout().subscribe();
        return of(null);
      })
    );
  }


  setAccessToken(newToken: string) {
    if (this.user) {
      this.user.setToken(newToken);
      if (typeof window !== 'undefined' && window.localStorage) localStorage.setItem('user', JSON.stringify(this.user)); // Atualiza o `user` no localStorage
    }
  }

  getAccessToken() {
    return this.user.accessToken;
  }


  public getUser() {
    return this.user;
  }

  forgetPassword(username: string) {
    return this.http.post<{ message: string }>(`${this.apiUrl}/forgot-password`, {username}, {withCredentials: true});
  }
}
