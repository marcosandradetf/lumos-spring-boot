import api from '@/core/auth/api';
import type { ActivationCodeResponse, ManagedUser, RoleOption, UserActivationStatus, UserUpdatePayload } from '@/features/manage/types/manageTypes';

function normalizeStatus(status: unknown): UserActivationStatus {
  if (typeof status === 'boolean') {
    return status ? 'ACTIVE' : 'BLOCKED';
  }

  if (status === 'ACTIVE' || status === 'BLOCKED' || status === 'PENDING_ACTIVATION') {
    return status;
  }

  return 'PENDING_ACTIVATION';
}

function normalizeRole(role: unknown): RoleOption | null {
  const roleObject = typeof role === 'object' && role !== null ? role as Partial<RoleOption> : {};
  const token = typeof role === 'string'
    ? role
    : roleObject.roleName ?? roleObject.roleId ?? roleObject.label;

  if (!token) {
    return null;
  }

  return {
    selected: roleObject.selected ?? false,
    roleId: String(roleObject.roleId ?? token),
    roleName: String(roleObject.roleName ?? token),
    label: String(roleObject.label ?? token),
    description: String(roleObject.description ?? ''),
  };
}

function normalizeUser(user: ManagedUser): ManagedUser {
  return {
    ...user,
    role: (Array.isArray(user.role) ? user.role : [])
      .map((role) => normalizeRole(role))
      .filter((role): role is RoleOption => Boolean(role)),
    status: normalizeStatus(user.status),
  };
}

export const usersApi = {
  async getUsers() {
    const { data } = await api.get<ManagedUser[]>('/api/user/get-users');
    return data.map(normalizeUser);
  },

  async getRoles() {
    const { data } = await api.get<RoleOption[]>('/api/user/get-roles');
    return data;
  },

  async updateUsers(users: UserUpdatePayload[]) {
    const { data } = await api.post<ManagedUser[]>('/api/user/update-users', users);
    return data.map(normalizeUser);
  },

  async generateActivationCode(userId: string) {
    const { data } = await api.post<ActivationCodeResponse>(`/api/user/${userId}/generate-activation-code`, {});
    return data;
  },

  async resetActivation(userId: string) {
    const { data } = await api.post<ActivationCodeResponse>(`/api/user/${userId}/reset-activation`, {});
    return data;
  },
};
