export type UserActivationStatus = 'PENDING_ACTIVATION' | 'ACTIVE' | 'BLOCKED';

export interface ManagedUser {
  userId: string;
  username: string;
  name: string;
  lastname: string;
  email: string;
  cpfCnpj: string;
  role: Array<{ roleId: string; label: string, roleName: string, description: string }>;
  status: UserActivationStatus;
  mustChangePassword?: boolean;
  activationExpiresAt?: string | null;
}

export interface ActivationCodeResponse {
  activationCode: string;
  expiresAt: string;
  message: string;
}

export interface TeamModel {
  idTeam: string;
  teamName: string;
  memberIds: string[];
  memberNames: string[];
  UFName: string;
  cityName: string;
  regionName: string;
  plate: string;
  depositName: string;
}

export interface TeamFormInput {
  teamName: string;
  memberIds: string[];
  UFName: string;
  cityName: string;
  regionName: string;
  plate: string;
}
