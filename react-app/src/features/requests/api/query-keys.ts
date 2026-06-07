export const requestsKeys = {
  all: ['requests'] as const,
  pendingReservesForStockist: () => [...requestsKeys.all, 'pending-reserves-for-stockist'] as const,
  materialsByContractReference: (contractReferenceItemId: number, teamId: number) =>
    [...requestsKeys.all, 'materials-by-contract-reference', contractReferenceItemId, teamId] as const,
  orderHistoryByStatus: (teamId: number, status: string, contractReferenceItemId: number) =>
    [...requestsKeys.all, 'order-history-by-status', teamId, status, contractReferenceItemId] as const,
};
