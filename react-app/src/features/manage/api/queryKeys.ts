export const manageKeys = {
  all: ['manage'] as const,
  users: () => [...manageKeys.all, 'users'] as const,
  teams: () => [...manageKeys.all, 'teams'] as const,
};
