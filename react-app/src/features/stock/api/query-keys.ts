export const stockKeys = {
  all: ['stock'] as const,
  deposits: () => [...stockKeys.all, 'deposits'] as const,
  groups: () => [...stockKeys.all, 'groups'] as const,
  types: () => [...stockKeys.all, 'types'] as const,
  typesSubtype: () => [...stockKeys.all, 'types-subtype'] as const,
  stockists: () => [...stockKeys.all, 'stockists'] as const,
  movementsPending: () => [...stockKeys.all, 'movements', 'pending'] as const,
  movementsApproved: () => [...stockKeys.all, 'movements', 'approved'] as const,
  catalogue: (generic = false) => [...stockKeys.all, 'catalogue', generic] as const,
  materialsBrowse: (depositId: number | undefined, page: number, search: string) =>
    [...stockKeys.all, 'materials-browse', depositId, page, search] as const,
};
