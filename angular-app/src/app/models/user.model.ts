
export class User {
  public username: string;
  public accessToken: string;
  public birthDate: Date; // Data de nascimento do usuário
  public roles: String; // Perfis de acesso do usuário

  constructor(username: string, accessToken: string, roles: string) {
    this.username = username;
    this.accessToken = accessToken;
    this.birthDate = new Date();
    this.roles = roles;
  }


  // Verifica se o token de acesso está expirado
  isAccessTokenExpired(): boolean {
    if (!this.accessToken) {
      return true; // Considere expirado se não houver token
    }

    try {
      const payload = JSON.parse(atob(this.accessToken.split('.')[1]));
      const expiry = payload.exp * 1000;
      return Date.now() > expiry;
    } catch (error) {
      console.error("Erro ao decodificar o token de acesso:", error);
      return true; // Considere expirado se houver erro na decodificação
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

  // Verifica se o usuário possui um papel específico
  hasRole(role: string): boolean {
    return this.roles.includes(role);
  }

}
