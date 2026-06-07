import type { ContractFilters } from '@/features/contract/types';

export const contractKeys = {
  all: ['contracts'] as const,
  list: (filters: ContractFilters) => [...contractKeys.all, 'list', filters] as const,
  items: (contractId: number) => [...contractKeys.all, 'items', contractId] as const,
  referenceItems: () => [...contractKeys.all, 'reference-items'] as const,
  referenceItemsBase: () => [...contractKeys.all, 'reference-items-base'] as const,
  referenceLinks: () => [...contractKeys.all, 'reference-links'] as const,
};
