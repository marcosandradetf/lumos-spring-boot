import { contractsApi } from '@/features/contract/api/contractsApi';
import { manageApi } from '@/features/manage/api/manageApi';
import { usersApi } from '@/features/manage/api/usersApi';
import { materialApi } from '@/features/stock/api/material-api';
import { stockApi } from '@/features/stock/api/stock-api';
import { stockistApi } from '@/features/stock/api/stockist-api';

function normalizeRoles(userRole: unknown): string[] {
  if (!Array.isArray(userRole)) return [];

  return userRole
    .flatMap((role) => {
      if (!role || typeof role !== 'object') return [];
      const data = role as Record<string, unknown>;
      return [data.label, data.roleId, data.roleName]
        .filter(Boolean)
        .map((value) => String(value).toUpperCase());
    });
}

export async function checkOnboardingState(): Promise<boolean> {
  const [
    referenceContractItems,
    contracts,
    users,
    teams,
    stockists,
    deposits,
    materials,
  ] = await Promise.all([
    contractsApi.getContractReferenceItems(),
    contractsApi.getAllContracts({
      contractor: null,
      startDate: new Date(new Date().setMonth(new Date().getMonth() - 6)),
      endDate: new Date(),
      status: null,
    }),
    usersApi.getUsers(),
    manageApi.getTeams(),
    stockistApi.getStockists(),
    stockApi.getDeposits(),
    materialApi.getCatalogue(),
  ]);

  const operationalUsers = users.filter((user) => {
    const roles = normalizeRoles(user.role);
    return roles.includes('ELETRICISTA') || roles.includes('MOTORISTA');
  }).length;

  const depositsCount = deposits.filter((deposit) => !deposit.isTruck).length;
  const trucksCount = deposits.filter((deposit) => deposit.isTruck).length;

  return (
    referenceContractItems.length === 0
    || contracts.length === 0
    || users.length === 0
    || operationalUsers === 0
    || teams.length === 0
    || stockists.length === 0
    || depositsCount === 0
    || trucksCount === 0
    || materials.length === 0
  );
}
