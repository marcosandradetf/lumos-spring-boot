import {Injectable} from '@angular/core';

@Injectable({providedIn: 'root'})
export class User {
  public uuid!: string;
  public username!: string;
  public email!: string;
  public fullName!: string;
  public tenant!: string;
  public accessToken!: string;
  public birthDate!: Date; // Data de nascimento do usuário

  public roles!: string[]; // Perfis de acesso do usuário
  public relatedNotificationTopics!: string[]; // Equipes do usuário

  constructor() {
  }

  initialize(
    uuid: string,
    username: string,
    accessToken: string,
    roles: string[],
    fullName: string,
    tenant: string,
    relatedNotificationTopics: string[],
    email: string) {
    this.uuid = uuid;
    this.username = username;
    this.accessToken = accessToken;
    this.birthDate = new Date();
    this.roles = roles;
    this.fullName = fullName;
    this.tenant = tenant;
    this.relatedNotificationTopics = relatedNotificationTopics;
    this.email = email;
  }


  // Verifica se o token de acesso está expirado
  isAccessTokenExpired(): boolean {
    if (!this.accessToken) {
      return true; // Considera expirado se não houver token
    }

    try {
      // Extrai e decodifica o payload do token
      const payloadBase64 = this.accessToken.split('.')[1];
      const payload = JSON.parse(atob(payloadBase64));

      // Calcula a data de expiração e compara com o horário atual
      const expiry = payload.exp * 1000;
      return Date.now() > expiry;
    } catch (error) {
      console.error("Erro ao decodificar o token de acesso:", error);
      return true; // Considera expirado se houver erro na decodificação
    }
  }

  // Define novos tokens
  setToken(accessToken: string): void {
    this.accessToken = accessToken;
  }

  // Limpa os tokens do usuário
  clearToken(): void {
    this.accessToken = '';
  }

  getRoles(): string[] {
    return this.roles;
  }

  getRelatedNotificationTopics(): string[] {
    return this.relatedNotificationTopics;
  }

}
