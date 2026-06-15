export type UserActivationStatus = 'PENDING_ACTIVATION' | 'ACTIVE' | 'BLOCKED';

export interface RoleOption {
  selected?: boolean;
  roleId: string;
  roleName: string;
  label: string;
  description: string;
}

export interface ManagedUser {
  userId: string;
  username: string;
  name: string;
  lastname: string;
  email: string;
  cpfCnpj: string;
  dateOfBirth?: string;
  year?: string;
  month?: string;
  day?: string;
  role: RoleOption[];
  status: UserActivationStatus;
  mustChangePassword?: boolean;
  activationExpiresAt?: string | null;
  sel?: boolean;
  show?: boolean;
}

export interface ActivationCodeResponse {
  activationCode: string;
  expiresAt: string;
  message: string;
}

export interface UserUpdatePayload {
  userId: string;
  username: string;
  name: string;
  lastname: string;
  email: string;
  cpfCnpj: string;
  year: string | number;
  month: string | number;
  day: string | number;
  role: string[];
  status: UserActivationStatus;
  sel: boolean;
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
