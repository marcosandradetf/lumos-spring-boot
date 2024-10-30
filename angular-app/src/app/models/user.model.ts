
export class User {
  public username: string;
  public accessToken: string;
  public refreshToken: string;
  public birthDate: Date; // Data de nascimento do usuário
  public roles: String; // Perfis de acesso do usuário

  constructor(username: string, accessToken: string, refreshToken: string, roles: string) {
    this.username = username;
    this.accessToken = accessToken;
    this.refreshToken = refreshToken;
    this.birthDate = new Date();
    this.roles = roles;
  }

  // Verifica se o token de acesso está expirado
  isAccessTokenExpired(): boolean {
    const payload = JSON.parse(atob(this.accessToken.split('.')[1]));
    const expiry = payload.exp * 1000;
    return Date.now() > expiry;
  }

  // Define novos tokens
  setTokens(accessToken: string, refreshToken: string): void {
    this.accessToken = accessToken;
    this.refreshToken = refreshToken;
  }

  // Limpa os tokens do usuário
  clearTokens(): void {
    this.accessToken = '';
    this.refreshToken = '';
  }

  // Verifica se o usuário possui um papel específico
  hasRole(role: string): boolean {
    return this.roles.includes(role);
  }

}
