export const manageKeys = {
  all: ['manage'] as const,
  users: () => [...manageKeys.all, 'users'] as const,
  roles: () => [...manageKeys.all, 'roles'] as const,
  teams: () => [...manageKeys.all, 'teams'] as const,
};
